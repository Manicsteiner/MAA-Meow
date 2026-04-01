#include "bridge_frame_buffer.h"

#include <android/bitmap.h>

#include <atomic>
#include <cstdlib>
#include <thread>

#if defined(__ARM_NEON)
#include <arm_neon.h>
#endif

static FrameBuffer g_buffers[FRAME_BUFFER_COUNT] = {};
static std::atomic<int> g_bufferStates[FRAME_BUFFER_COUNT] = {
        FRAME_STATE_FREE, FRAME_STATE_FREE, FRAME_STATE_FREE
};
static std::atomic<int> g_readerCounts[FRAME_BUFFER_COUNT] = {0, 0, 0};
static std::atomic<FrameBuffer *> g_readBuffer{nullptr};
static std::atomic<int64_t> g_frameCount{0};
static std::atomic<bool> g_frameBuffersInitialized{false};

static void ProcessFrameDataV2(
        const uint8_t *__restrict src,
        uint8_t *__restrict dstBGR,
        int width,
        int height,
        int srcStride) {
    for (int y = 0; y < height; ++y) {
        const uint8_t *s = src + static_cast<size_t>(y) * srcStride;
        uint8_t *d3 = dstBGR + y * width * 3;
        int x = 0;

#if defined(__ARM_NEON)
        for (; x <= width - 16; x += 16) {
            uint8x16x4_t rgba = vld4q_u8(s);
            s += 64;
            uint8x16x3_t bgr;
            bgr.val[0] = rgba.val[2];
            bgr.val[1] = rgba.val[1];
            bgr.val[2] = rgba.val[0];
            vst3q_u8(d3, bgr);
            d3 += 48;
        }
        for (; x <= width - 8; x += 8) {
            uint8x8x4_t rgba = vld4_u8(s);
            s += 32;
            uint8x8x3_t bgr;
            bgr.val[0] = rgba.val[2];
            bgr.val[1] = rgba.val[1];
            bgr.val[2] = rgba.val[0];
            vst3_u8(d3, bgr);
            d3 += 24;
        }
#endif
        for (; x < width; ++x) {
            d3[0] = s[2];
            d3[1] = s[1];
            d3[2] = s[0];
            s += 4;
            d3 += 3;
        }
    }
}

static int GetBufferIndex(FrameBuffer *buf) {
    for (int i = 0; i < FRAME_BUFFER_COUNT; ++i) {
        if (&g_buffers[i] == buf) {
            return i;
        }
    }
    return -1;
}

static void ReleaseBuffer(FrameBuffer *buf) {
    if (!buf) {
        return;
    }
    if (buf->bgr_data) {
        free(buf->bgr_data);
        buf->bgr_data = nullptr;
    }
    buf->data = nullptr;
    buf->width = 0;
    buf->height = 0;
    buf->stride = 0;
    buf->size = 0;
    buf->bgr_size = 0;
    buf->timestamp = 0;
    buf->frameCount = 0;
}

static void MarkBufferFree(FrameBuffer *buf) {
    int idx = GetBufferIndex(buf);
    if (idx >= 0) {
        g_bufferStates[idx].store(FRAME_STATE_FREE, std::memory_order_release);
    }
}

static void CommitWriteBuffer(FrameBuffer *buf) {
    int idx = GetBufferIndex(buf);
    if (idx < 0) {
        return;
    }
    g_bufferStates[idx].store(FRAME_STATE_FREE, std::memory_order_release);
    if (g_frameBuffersInitialized.load(std::memory_order_acquire)) {
        g_readBuffer.store(buf, std::memory_order_release);
    }
}

static FrameBuffer *AcquireWriteBuffer() {
    if (!g_frameBuffersInitialized.load(std::memory_order_acquire)) {
        return nullptr;
    }

    FrameBuffer *currentReadBuffer = g_readBuffer.load(std::memory_order_acquire);
    for (int i = 0; i < FRAME_BUFFER_COUNT; ++i) {
        FrameBuffer *candidate = &g_buffers[i];
        if (candidate == currentReadBuffer ||
            g_readerCounts[i].load(std::memory_order_acquire) > 0) {
            continue;
        }

        int expected = FRAME_STATE_FREE;
        if (!g_bufferStates[i].compare_exchange_strong(expected, FRAME_STATE_WRITING,
                                                       std::memory_order_acq_rel)) {
            continue;
        }

        if (g_readerCounts[i].load(std::memory_order_acquire) > 0 ||
            g_readBuffer.load(std::memory_order_acquire) == candidate ||
            !g_frameBuffersInitialized.load(std::memory_order_acquire)) {
            g_bufferStates[i].store(FRAME_STATE_FREE, std::memory_order_release);
            continue;
        }
        return candidate;
    }
    return nullptr;
}

static const FrameBuffer *LockCurrentFrame() {
    if (!g_frameBuffersInitialized.load(std::memory_order_acquire)) {
        return nullptr;
    }

    for (int attempt = 0; attempt < 3; ++attempt) {
        FrameBuffer *frame = g_readBuffer.load(std::memory_order_acquire);
        if (!frame || frame->frameCount == 0) {
            return nullptr;
        }

        int idx = GetBufferIndex(frame);
        if (idx < 0) {
            return nullptr;
        }

        g_readerCounts[idx].fetch_add(1, std::memory_order_acquire);
        if (g_readBuffer.load(std::memory_order_acquire) != frame ||
            !g_frameBuffersInitialized.load(std::memory_order_acquire)) {
            g_readerCounts[idx].fetch_sub(1, std::memory_order_release);
            continue;
        }

        if (g_bufferStates[idx].load(std::memory_order_acquire) == FRAME_STATE_WRITING) {
            bool ready = false;
            for (int spin = 0; spin < 500; ++spin) {
                if (g_bufferStates[idx].load(std::memory_order_acquire) != FRAME_STATE_WRITING) {
                    ready = true;
                    break;
                }
            }
            if (!ready) {
                g_readerCounts[idx].fetch_sub(1, std::memory_order_release);
                return nullptr;
            }
        }

        return frame;
    }
    return nullptr;
}

static void UnlockFrame(const FrameBuffer *frame) {
    if (!frame) {
        return;
    }

    int idx = GetBufferIndex(const_cast<FrameBuffer *>(frame));
    if (idx >= 0) {
        g_readerCounts[idx].fetch_sub(1, std::memory_order_release);
    }
}

void InitFrameBuffers(int width, int height) {
    if (g_frameBuffersInitialized.load(std::memory_order_acquire)) {
        ReleaseFrameBuffers();
    }

    const size_t bgrSize = static_cast<size_t>(width) * height * 3;
    for (int i = 0; i < FRAME_BUFFER_COUNT; ++i) {
        FrameBuffer &buf = g_buffers[i];
        ReleaseBuffer(&buf);
        if (posix_memalign(reinterpret_cast<void **>(&buf.bgr_data), 64, bgrSize) != 0) {
            LOGE("InitFrameBuffers: posix_memalign failed at index=%d", i);
            for (int j = 0; j <= i; ++j) {
                ReleaseBuffer(&g_buffers[j]);
                g_bufferStates[j].store(FRAME_STATE_FREE, std::memory_order_release);
                g_readerCounts[j].store(0, std::memory_order_release);
            }
            g_readBuffer.store(nullptr, std::memory_order_release);
            g_frameCount.store(0, std::memory_order_release);
            return;
        }

        buf.data = nullptr;
        buf.width = width;
        buf.height = height;
        buf.stride = width * 4;
        buf.size = 0;
        buf.bgr_size = bgrSize;
        buf.timestamp = 0;
        buf.frameCount = 0;
        g_bufferStates[i].store(FRAME_STATE_FREE, std::memory_order_release);
        g_readerCounts[i].store(0, std::memory_order_release);
    }

    g_readBuffer.store(nullptr, std::memory_order_release);
    g_frameCount.store(0, std::memory_order_release);
    g_frameBuffersInitialized.store(true, std::memory_order_release);
    LOGI("InitFrameBuffers: Success %dx%d", width, height);
}

void ReleaseFrameBuffers() {
    g_frameBuffersInitialized.store(false, std::memory_order_release);
    g_readBuffer.store(nullptr, std::memory_order_release);

    for (int i = 0; i < FRAME_BUFFER_COUNT; ++i) {
        while (g_bufferStates[i].load(std::memory_order_acquire) == FRAME_STATE_WRITING ||
               g_readerCounts[i].load(std::memory_order_acquire) > 0) {
            std::this_thread::yield();
        }

        ReleaseBuffer(&g_buffers[i]);
        g_bufferStates[i].store(FRAME_STATE_FREE, std::memory_order_release);
        g_readerCounts[i].store(0, std::memory_order_release);
    }

    g_readBuffer.store(nullptr, std::memory_order_release);
    g_frameCount.store(0, std::memory_order_release);
}

bool WriteHardwareBufferToFrame(AHardwareBuffer *buffer, int64_t timestampNs) {
    if (!buffer || !g_frameBuffersInitialized.load(std::memory_order_acquire)) {
        return false;
    }

    FrameBuffer *target = AcquireWriteBuffer();
    if (!target) {
        return false;
    }

    void *srcAddr = nullptr;
    if (AHardwareBuffer_lock(buffer, AHARDWAREBUFFER_USAGE_CPU_READ_OFTEN, -1, nullptr,
                             &srcAddr) != 0) {
        MarkBufferFree(target);
        return false;
    }

    AHardwareBuffer_Desc desc;
    AHardwareBuffer_describe(buffer, &desc);
    ProcessFrameDataV2(static_cast<uint8_t *>(srcAddr), target->bgr_data, target->width,
                       target->height, static_cast<int>(desc.stride) * 4);
    AHardwareBuffer_unlock(buffer, nullptr);

    target->timestamp = timestampNs;
    target->frameCount = g_frameCount.fetch_add(1, std::memory_order_acq_rel) + 1;
    CommitWriteBuffer(target);
    return true;
}

BRIDGE_API FrameInfo GetLockedPixels() {
    FrameInfo result = {0};
    const FrameBuffer *frame = LockCurrentFrame();
    if (!frame) {
        return result;
    }

    if (!frame->bgr_data) {
        UnlockFrame(frame);
        return result;
    }

    result.width = frame->width;
    result.height = frame->height;
    result.stride = frame->width * 3;
    result.length = static_cast<uint32_t>(frame->bgr_size);
    result.data = frame->bgr_data;
    result.frame_ref = const_cast<FrameBuffer *>(frame);
    return result;
}

BRIDGE_API int UnlockPixels(FrameInfo info) {
    if (info.frame_ref) {
        UnlockFrame(reinterpret_cast<const FrameBuffer *>(info.frame_ref));
    }
    return 0;
}

jobject CreateFrameBufferBitmap(JNIEnv *env) {
    const FrameBuffer *frame = LockCurrentFrame();
    if (!frame) {
        return nullptr;
    }

    if (!frame->bgr_data) {
        UnlockFrame(frame);
        return nullptr;
    }

    jclass bitmapClass = env->FindClass("android/graphics/Bitmap");
    jclass configClass = env->FindClass("android/graphics/Bitmap$Config");
    jfieldID argb8888Field = env->GetStaticFieldID(
            configClass, "ARGB_8888", "Landroid/graphics/Bitmap$Config;");
    jobject argb8888Config = env->GetStaticObjectField(configClass, argb8888Field);
    jmethodID createBitmapMethod = env->GetStaticMethodID(
            bitmapClass, "createBitmap",
            "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    jobject bitmap = env->CallStaticObjectMethod(bitmapClass, createBitmapMethod, frame->width,
                                                 frame->height, argb8888Config);

    void *pixels = nullptr;
    if (bitmap &&
        AndroidBitmap_lockPixels(env, bitmap, &pixels) == ANDROID_BITMAP_RESULT_SUCCESS) {
        AndroidBitmapInfo info;
        AndroidBitmap_getInfo(env, bitmap, &info);

        uint8_t *dst = static_cast<uint8_t *>(pixels);
        uint8_t *src = frame->bgr_data;
        for (int y = 0; y < frame->height; ++y) {
            for (int x = 0; x < frame->width; ++x) {
                dst[x * 4 + 0] = src[x * 3 + 2];
                dst[x * 4 + 1] = src[x * 3 + 1];
                dst[x * 4 + 2] = src[x * 3 + 0];
                dst[x * 4 + 3] = 255;
            }
            dst += info.stride;
            src += frame->width * 3;
        }
        AndroidBitmap_unlockPixels(env, bitmap);
    }

    UnlockFrame(frame);
    return bitmap;
}
