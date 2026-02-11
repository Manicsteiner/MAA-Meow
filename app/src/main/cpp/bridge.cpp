#include "bridge.h"
#include <string>
#include <android/log.h>
#include <android/hardware_buffer.h>
#include <android/hardware_buffer_jni.h>
#include <android/bitmap.h>
#include <cinttypes>
#include <cstring>
#include <cstdlib>
#include <cstdio>
#include <thread>
#include <chrono>
#include <atomic>
#include <cstdint>

#define LOG_TAG "LibBridge"

#ifdef NDEBUG
#define LOGD(...) ((void)0)
#else
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#endif

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static jstring ping(JNIEnv *env, jclass clazz);

static void nativeInitFrameBuffers(JNIEnv *env, jclass clazz, jint width, jint height);

static jlong nativeCopyFrameFromHardwareBuffer(JNIEnv *env, jclass clazz, jobject hardwareBuffer);

static void nativeReleaseFrameBuffers(JNIEnv *env, jclass clazz);

static jobject nativeGetFrameBufferBitmap(JNIEnv *env, jclass clazz);

static JNINativeMethod gMethods[] = {
        {"ping",                        "()Ljava/lang/String;",                 (void *) ping},
        {"initFrameBuffers",            "(II)V",                                (void *) nativeInitFrameBuffers},
        {"copyFrameFromHardwareBuffer", "(Landroid/hardware/HardwareBuffer;)J", (void *) nativeCopyFrameFromHardwareBuffer},
        {"releaseFrameBuffers",         "()V",                                  (void *) nativeReleaseFrameBuffers},
        {"getFrameBufferBitmap",        "()Landroid/graphics/Bitmap;",          (void *) nativeGetFrameBufferBitmap},
};

static JavaVM *g_jvm = nullptr;

static jclass g_driver_class = nullptr;

static jmethodID g_touch_down_method = nullptr;
static jmethodID g_touch_move_method = nullptr;
static jmethodID g_touch_up_method = nullptr;
static jmethodID g_key_down_method = nullptr;
static jmethodID g_key_up_method = nullptr;
static jmethodID g_start_app_method = nullptr;

static FrameBuffer g_buffers[FRAME_BUFFER_COUNT] = {
        {nullptr, 0, 0, 0, 0, 0, 0},
        {nullptr, 0, 0, 0, 0, 0, 0},
        {nullptr, 0, 0, 0, 0, 0, 0}
};
// FREE=0, READING=1, WRITING=2
static std::atomic<int> g_bufferStates[FRAME_BUFFER_COUNT] = {
        FRAME_STATE_FREE, FRAME_STATE_FREE, FRAME_STATE_FREE
};
static std::atomic<int> g_readerCounts[FRAME_BUFFER_COUNT] = {0, 0, 0};
// available buffer
static std::atomic<FrameBuffer *> g_readBuffer{nullptr};
static std::atomic<int64_t> g_frameCount{0};
static bool g_frameBuffersInitialized = false;

static constexpr auto DRIVE_CLAZZ = "com/aliothmoon/maameow/maa/DriverClass";
static constexpr auto NATIVE_BRIDGE_CLAZZ = "com/aliothmoon/maameow/bridge/NativeBridgeLib";

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    LOGI("JNI_OnLoad called - registering native methods");

    g_jvm = vm;

    JNIEnv *env = nullptr;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE("Failed to get JNI environment");
        return JNI_ERR;
    }

    jclass nativeLibClass = env->FindClass(NATIVE_BRIDGE_CLAZZ);
    if (nativeLibClass == nullptr) {
        LOGE("Failed to find NativeLib class");
        return JNI_ERR;
    }

    if (env->RegisterNatives(nativeLibClass, gMethods, std::size(gMethods)) < 0) {
        LOGE("Failed to register native methods");
        return JNI_ERR;
    }

    LOGI("Successfully registered %d native methods",
         (int) (sizeof(gMethods) / sizeof(gMethods[0])));

    jclass driverClass = env->FindClass(DRIVE_CLAZZ);
    if (driverClass) {
        g_driver_class = (jclass) env->NewGlobalRef(driverClass);

        g_touch_down_method = env->GetStaticMethodID(g_driver_class, "touchDown", "(III)Z");
        g_touch_move_method = env->GetStaticMethodID(g_driver_class, "touchMove", "(III)Z");
        g_touch_up_method = env->GetStaticMethodID(g_driver_class, "touchUp", "(III)Z");
        g_key_down_method = env->GetStaticMethodID(g_driver_class, "keyDown", "(II)Z");
        g_key_up_method = env->GetStaticMethodID(g_driver_class, "keyUp", "(II)Z");
        g_start_app_method = env->GetStaticMethodID(g_driver_class, "startApp",
                                                    "(Ljava/lang/String;IZ)Z");

        env->DeleteLocalRef(driverClass);

        if (g_touch_down_method && g_touch_move_method && g_touch_up_method &&
            g_key_down_method && g_key_up_method && g_start_app_method) {
            LOGI("Successfully cached DriverClass and all methods");
        } else {
            LOGW("Failed to cache some methods");
        }
    } else {
        LOGW("Failed to find DriverClass, will look up at runtime");
    }

    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    LOGI("JNI_OnUnload called - cleaning up resources");

    JNIEnv *env = nullptr;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) == JNI_OK) {
        if (g_driver_class) {
            env->DeleteGlobalRef(g_driver_class);
            g_driver_class = nullptr;
            LOGI("Cleaned up DriverClass global reference");
        }
    }
    g_touch_down_method = nullptr;
    g_touch_move_method = nullptr;
    g_touch_up_method = nullptr;
    g_key_down_method = nullptr;
    g_key_up_method = nullptr;
    g_start_app_method = nullptr;
}


int UpcallInputControl(JNIEnv *env, MethodType method, int x, int y, int keyCode, int displayId) {
    if (!env) {
        LOGE("JNIEnv is null");
        return -1;
    }

    jclass inputControlClass = g_driver_class;
    jmethodID targetMethod = nullptr;
    bool needCleanup = false;

    switch (method) {
        case TOUCH_DOWN:
            targetMethod = g_touch_down_method;
            break;
        case TOUCH_MOVE:
            targetMethod = g_touch_move_method;
            break;
        case TOUCH_UP:
            targetMethod = g_touch_up_method;
            break;
        case KEY_DOWN:
            targetMethod = g_key_down_method;
            break;
        case KEY_UP:
            targetMethod = g_key_up_method;
            break;
        default:
            LOGE("Unsupported method type: %d", method);
            return -1;
    }

    if (!inputControlClass || !targetMethod) {
        LOGD("Using runtime lookup for DriverClass input control methods");
        inputControlClass = env->FindClass(DRIVE_CLAZZ);
        if (!inputControlClass) {
            LOGE("Failed to find DriverClass");
            return -1;
        }

        const char *methodName;
        const char *methodSig;
        switch (method) {
            case TOUCH_DOWN:
                methodName = "touchDown";
                methodSig = "(III)Z";
                break;
            case TOUCH_MOVE:
                methodName = "touchMove";
                methodSig = "(III)Z";
                break;
            case TOUCH_UP:
                methodName = "touchUp";
                methodSig = "(III)Z";
                break;
            case KEY_DOWN:
                methodName = "keyDown";
                methodSig = "(II)Z";
                break;
            case KEY_UP:
                methodName = "keyUp";
                methodSig = "(II)Z";
                break;
            default:
                env->DeleteLocalRef(inputControlClass);
                return -1;
        }

        targetMethod = env->GetStaticMethodID(inputControlClass, methodName, methodSig);
        if (!targetMethod) {
            LOGE("Failed to find %s method", methodName);
            env->DeleteLocalRef(inputControlClass);
            return -1;
        }
        needCleanup = true;
    }

    jboolean result = JNI_FALSE;
    if (method == KEY_DOWN || method == KEY_UP) {
        result = env->CallStaticBooleanMethod(inputControlClass, targetMethod, keyCode, displayId);
    } else {
        result = env->CallStaticBooleanMethod(inputControlClass, targetMethod, x, y, displayId);
    }

    if (CheckJNIException(env, "InputControlUtils method call")) {
        if (needCleanup) env->DeleteLocalRef(inputControlClass);
        return -1;
    }

    if (needCleanup) {
        env->DeleteLocalRef(inputControlClass);
    }

    return result ? 0 : -1;
}

int UpcallStartApp(JNIEnv *env, const char *packageName, int displayId, bool forceStop) {
    if (!env || !packageName) {
        LOGE("UpcallStartApp: invalid params");
        return -1;
    }

    jclass driverClass = g_driver_class;
    jmethodID startAppMethod = g_start_app_method;
    bool needCleanup = false;

    if (!driverClass || !startAppMethod) {
        LOGD("Using runtime lookup for startApp method");
        driverClass = env->FindClass(DRIVE_CLAZZ);
        if (!driverClass) {
            LOGE("Failed to find DriverClass");
            return -1;
        }
        startAppMethod = env->GetStaticMethodID(driverClass, "startApp", "(Ljava/lang/String;IZ)Z");
        if (!startAppMethod) {
            LOGE("Failed to find startApp method");
            env->DeleteLocalRef(driverClass);
            return -1;
        }
        needCleanup = true;
    }

    jstring jPackageName = env->NewStringUTF(packageName);
    if (!jPackageName) {
        LOGE("Failed to create jstring for packageName");
        if (needCleanup) env->DeleteLocalRef(driverClass);
        return -1;
    }

    jboolean result = env->CallStaticBooleanMethod(driverClass, startAppMethod,
                                                   jPackageName, displayId, (jboolean) forceStop);

    env->DeleteLocalRef(jPackageName);
    if (needCleanup) env->DeleteLocalRef(driverClass);

    if (CheckJNIException(env, "startApp call")) {
        return -1;
    }

    LOGI("UpcallStartApp: package=%s, displayId=%d, forceStop=%d, result=%d",
         packageName, displayId, forceStop, result);
    return result ? 0 : -1;
}

int DispatchInputMessage(MethodParam param) {
    LOGI("DispatchInputMessage start method: %d, displayId: %d", param.method, param.display_id);

    auto *env = (JNIEnv *) AttachThread();
    if (!env) {
        LOGE("Thread attach failed");
        return -1;
    }

    int result = -1;
    int displayId = param.display_id;

    switch (param.method) {
        case TOUCH_DOWN:
            result = UpcallInputControl(env, TOUCH_DOWN, param.args.touch.p.x, param.args.touch.p.y,
                                        0, displayId);
            break;
        case TOUCH_MOVE:
            result = UpcallInputControl(env, TOUCH_MOVE, param.args.touch.p.x, param.args.touch.p.y,
                                        0, displayId);
            break;
        case TOUCH_UP:
            result = UpcallInputControl(env, TOUCH_UP, param.args.touch.p.x, param.args.touch.p.y,
                                        0, displayId);
            break;
        case KEY_DOWN:
            result = UpcallInputControl(env, KEY_DOWN, 0, 0, param.args.key.key_code, displayId);
            break;
        case KEY_UP:
            result = UpcallInputControl(env, KEY_UP, 0, 0, param.args.key.key_code, displayId);
            break;
        case START_GAME:
            result = UpcallStartApp(env, param.args.start_game.package_name,
                                    displayId, param.args.start_game.force_stop != 0);
            break;
        case STOP_GAME:
            result = 0;
            break;
        case INPUT:
            result = 0;
            break;
        default:
            LOGW("Unsupported method type: %d", param.method);
            result = -1;
            break;
    }

    LOGD("DispatchInputMessage completed result: %d", result);
    return result;
}

void *AttachThread() {
    if (!g_jvm) {
        LOGE("JavaVM is null, cannot attach thread");
        return nullptr;
    }

    JNIEnv *env = nullptr;
    jint result = g_jvm->GetEnv((void **) &env, JNI_VERSION_1_6);

    if (result == JNI_OK) {
        return (void *) env;
    } else if (result == JNI_EDETACHED) {
        LOGI("Thread not attached, attaching...");

        result = g_jvm->AttachCurrentThreadAsDaemon(&env, nullptr);
        if (result == JNI_OK) {
            LOGI("Thread attached successfully, returning void*: %p", (void *) env);
            return (void *) env;
        } else {
            LOGE("Thread attach failed, error code: %d", result);
            return nullptr;
        }
    } else {
        LOGE("GetEnv failed, error code: %d", result);
        return nullptr;
    }
}

int DetachThread(void *env) {
    if (!g_jvm) {
        LOGE("JavaVM is null, cannot detach thread");
        return -1;
    }

    JNIEnv *currentEnv = nullptr;
    jint result = g_jvm->GetEnv((void **) &currentEnv, JNI_VERSION_1_6);

    if (result == JNI_EDETACHED) {
        LOGI("Thread already detached, no action needed");
        return 0;
    } else if (result != JNI_OK) {
        LOGE("GetEnv failed, error code: %d", result);
        return -1;
    }

    if (env && (JNIEnv *) env != currentEnv) {
        LOGW("DetachThread: provided env(%p) doesn't match current thread env(%p)", env,
             (void *) currentEnv);
    }

    result = g_jvm->DetachCurrentThread();
    if (result == JNI_OK) {
        return 0;
    } else {
        LOGE("Thread detach failed, error code: %d", result);
        return -1;
    }
}

void InitFrameBuffers(int width, int height) {
    if (g_frameBuffersInitialized) {
        ReleaseFrameBuffers();
    }

    size_t size = (size_t) width * height * 4;
    // RGBA
    int stride = width * 4;

    for (int i = 0; i < FRAME_BUFFER_COUNT; ++i) {
        FrameBuffer &buf = g_buffers[i];
        buf.data = (uint8_t *) malloc(size);
        buf.width = width;
        buf.height = height;
        buf.stride = stride;
        buf.size = size;
        buf.timestamp = 0;
        buf.frameCount = 0;
        g_bufferStates[i].store(FRAME_STATE_FREE);
        g_readerCounts[i].store(0);
    }

    g_readBuffer.store(nullptr);
    g_frameCount.store(0);
    g_frameBuffersInitialized = true;

    LOGI("InitFrameBuffers: %dx%d, size=%zu bytes, %d buffers",
         width, height, size, FRAME_BUFFER_COUNT);
}

void ReleaseFrameBuffers(void) {
    // 等待所有缓冲区释放
    for (int i = 0; i < FRAME_BUFFER_COUNT; ++i) {
        // 自旋等待 WRITING 状态和读者计数归零
        while (g_bufferStates[i].load() == FRAME_STATE_WRITING ||
               g_readerCounts[i].load() > 0) {
            std::this_thread::yield();
        }
        if (g_buffers[i].data) {
            free(g_buffers[i].data);
            g_buffers[i].data = nullptr;
        }
        g_bufferStates[i].store(FRAME_STATE_FREE);
        g_readerCounts[i].store(0);
    }
    g_readBuffer.store(nullptr);
    g_frameBuffersInitialized = false;
    LOGI("ReleaseFrameBuffers completed");
}

static int GetBufferIndex(FrameBuffer *buf) {
    for (int i = 0; i < FRAME_BUFFER_COUNT; ++i) {
        if (&g_buffers[i] == buf) return i;
    }
    return -1;
}

static void CommitWriteBuffer(FrameBuffer *buf) {
    int idx = GetBufferIndex(buf);
    if (idx >= 0) {
        // 先发布指针，再解开状态锁 
        // 这样读线程一旦看到 g_readBuffer 更新，尝试去读时，会发现状态暂时还是 WRITING，
        // 从而进入自旋等待，直到下面这行代码执行 这是最安全的交接方式 
        g_readBuffer.store(buf, std::memory_order_release);
        g_bufferStates[idx].store(FRAME_STATE_FREE, std::memory_order_release);
    }
}

static FrameBuffer *AcquireWriteBuffer() {
    // 获取当前读线程正在用的帧（写线程绝对不能动这个帧）
    FrameBuffer *currentReadBuf = g_readBuffer.load(std::memory_order_acquire);

    for (int i = 0; i < FRAME_BUFFER_COUNT; ++i) {
        FrameBuffer *candidate = &g_buffers[i];

        // 1. 保护当前最新的可读帧
        // 即使没有读者在读，这个帧也代表了屏幕上最新的画面，不能被覆盖，除非有更新的帧产生 
        if (candidate == currentReadBuf) continue;

        // 2. 检查是否有读线程正在读取 (原子读取)
        if (g_readerCounts[i].load(std::memory_order_acquire) > 0) continue;

        // 3. 尝试获取写权限
        // 即使只有一个写线程，这里依然建议用 CAS (compare_exchange)
        // 因为它能提供必要的内存屏障，防止编译器指令重排 
        int expected = FRAME_STATE_FREE;
        if (g_bufferStates[i].compare_exchange_strong(expected, FRAME_STATE_WRITING,
                                                      std::memory_order_acq_rel)) {

            // [关键双重检查]：针对多读线程场景
            // 在我们检查 count=0 到 设置状态=WRITING 的微小瞬间，
            // 可能有一个读线程刚好拿到旧指针并增加了计数 
            if (g_readerCounts[i].load(std::memory_order_acquire) > 0) {
                // 发生冲突，读线程赢了 写线程放弃此块，回退状态 
                g_bufferStates[i].store(FRAME_STATE_FREE, std::memory_order_release);
                continue;
            }

            // 再次检查 currentReadBuf，确保在操作期间它没变（单写线程其实这里一般不会变，除非逻辑错乱）
            if (g_readBuffer.load(std::memory_order_acquire) == candidate) {
                g_bufferStates[i].store(FRAME_STATE_FREE, std::memory_order_release);
                continue;
            }

            return candidate;
        }
    }

    // 所有缓冲区都忙（丢帧）
    return nullptr;
}

int64_t CopyFrameFromHardwareBuffer(void *env_ptr, void *hardwareBufferObj) {
    auto start = std::chrono::high_resolution_clock::now();

    auto *env = (JNIEnv *) env_ptr;
    if (!env || !hardwareBufferObj) {
        LOGE("CopyFrameFromHardwareBuffer: invalid params");
        return -1;
    }

    if (!g_frameBuffersInitialized) {
        LOGE("CopyFrameFromHardwareBuffer: frame buffers not initialized");
        return -1;
    }

    // 获取可写入的缓冲区
    FrameBuffer *target = AcquireWriteBuffer();
    if (!target) {
        // 所有缓冲区忙，丢帧
        return -1;
    }

    AHardwareBuffer *buffer = AHardwareBuffer_fromHardwareBuffer(env, (jobject) hardwareBufferObj);
    if (!buffer) {
        LOGE("AHardwareBuffer_fromHardwareBuffer failed");
        // 释放写入状态
        int idx = GetBufferIndex(target);
        if (idx >= 0) g_bufferStates[idx].store(FRAME_STATE_FREE);
        return -1;
    }

    AHardwareBuffer_Desc desc;
    AHardwareBuffer_describe(buffer, &desc);

    void *srcAddr = nullptr;
    if (AHardwareBuffer_lock(buffer, AHARDWAREBUFFER_USAGE_CPU_READ_OFTEN, -1, nullptr, &srcAddr) !=
        0) {
        LOGE("AHardwareBuffer_lock failed");
        int idx = GetBufferIndex(target);
        if (idx >= 0) g_bufferStates[idx].store(FRAME_STATE_FREE);
        return -1;
    }

    if ((int) desc.width != target->width || (int) desc.height != target->height) {
        LOGW("Frame size mismatch: HW=%dx%d, buffer=%dx%d",
             desc.width, desc.height, target->width, target->height);
        AHardwareBuffer_unlock(buffer, nullptr);
        int idx = GetBufferIndex(target);
        if (idx >= 0) g_bufferStates[idx].store(FRAME_STATE_FREE);
        return -1;
    }

    int srcStride = desc.stride * 4;
    if (srcStride == target->stride) {
        memcpy(target->data, srcAddr, target->size);
    } else {
        int rowBytes = target->width * 4;
        for (int y = 0; y < target->height; ++y) {
            memcpy(target->data + y * target->stride,
                   (uint8_t *) srcAddr + y * srcStride,
                   rowBytes);
        }
    }

    AHardwareBuffer_unlock(buffer, nullptr);

    target->timestamp = std::chrono::duration_cast<std::chrono::nanoseconds>(
            std::chrono::high_resolution_clock::now().time_since_epoch()).count();
    target->frameCount = g_frameCount.fetch_add(1) + 1;

    // 提交缓冲区，设置为可读
    CommitWriteBuffer(target);

    auto end = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::microseconds>(end - start).count();
//    LOGD("CopyFrameFromHardwareBuffer: %lld us, frame #%lld", (long long) duration,
//         (long long) target->frameCount);

    return target->frameCount;
}

static const FrameBuffer *LockCurrentFrame() {
    for (int attempt = 0; attempt < 3; attempt++) {
        // 1. 获取当前发布的最新帧指针
        FrameBuffer *frame = g_readBuffer.load(std::memory_order_acquire);
        if (!frame || frame->frameCount == 0) return nullptr;

        int idx = GetBufferIndex(frame);
        if (idx < 0) return nullptr;

        // 2. 乐观锁：先增加引用计数
        g_readerCounts[idx].fetch_add(1, std::memory_order_acquire);

        // 3. 重验证
        // 在我们执行第1步和第2步之间，写线程可能极快的完成了一次Commit，
        // 导致 g_readBuffer 已经指向了别的缓冲区 
        // 如果这里不检查，读线程就会锁定一个过期的的缓冲区 
        if (g_readBuffer.load(std::memory_order_acquire) != frame) {
            // 指针已过期，撤销计数，重试
            g_readerCounts[idx].fetch_sub(1, std::memory_order_release);
            continue;
        }

        // 4. 检查写入状态
        // 配合 CommitWriteBuffer 的顺序（先发布后解锁），这里可能会短暂读到 WRITING 
        if (g_bufferStates[idx].load(std::memory_order_acquire) == FRAME_STATE_WRITING) {
            // 自旋等待写线程释放锁（通常只需要几纳秒）
            bool ready = false;
            for (int spin = 0; spin < 500; spin++) {
                if (g_bufferStates[idx].load(std::memory_order_acquire) != FRAME_STATE_WRITING) {
                    ready = true;
                    break;
                }
            }

            if (!ready) {
                // 超时，为了不卡死读线程，放弃
                g_readerCounts[idx].fetch_sub(1, std::memory_order_release);
                return nullptr; // 或者 continue 重试
            }
        }

        return frame;
    }
    return nullptr;
}

static void UnlockFrame(const FrameBuffer *frame) {
    if (!frame) return;

    int idx = GetBufferIndex(const_cast<FrameBuffer *>(frame));
    if (idx >= 0) {
        int prev = g_readerCounts[idx].fetch_sub(1);
        if (prev <= 0) {
            // 不应该发生，说明有 bug
            LOGE("UnlockFrame: reader count underflow on buffer %d", idx);
            g_readerCounts[idx].store(0);
        }
    }
}

const FrameBuffer *GetCurrentFrame(void) {
    return LockCurrentFrame();
}


FrameInfo GetLockedPixels() {
    LOGD("GetLockedPixels start");
    FrameInfo result = {0};

    if (!g_frameBuffersInitialized) {
        LOGE("GetLockedPixels: frame buffers not initialized");
        return result;
    }

    const FrameBuffer *frame = GetCurrentFrame();
    if (!frame || !frame->data || frame->frameCount == 0) {
        LOGD("GetLockedPixels: no valid frame available");
        return result;
    }

    result.width = frame->width;
    result.height = frame->height;
    result.stride = frame->stride;
    result.length = frame->size;
    result.data = frame->data;
    result.frame_ref = const_cast<FrameBuffer *>(frame);

    LOGD("GetLockedPixels: %dx%d, frame #%lld",
         result.width, result.height, (long long) frame->frameCount);
    return result;
}

int UnlockPixels(FrameInfo info) {
    if (!info.frame_ref) {
        LOGW("UnlockPixels: frame_ref is null");
        return -1;
    }
    UnlockFrame(reinterpret_cast<const FrameBuffer *>(info.frame_ref));
    LOGD("UnlockPixels: frame unlocked");
    return 0;
}


static jstring ping(JNIEnv *env, jclass clazz) {
    std::string val = "BridgeLib Ping";
    return env->NewStringUTF(val.c_str());
}

static void nativeInitFrameBuffers(JNIEnv *env, jclass clazz, jint width, jint height) {
    InitFrameBuffers(width, height);
}

static jlong nativeCopyFrameFromHardwareBuffer(JNIEnv *env, jclass clazz, jobject hardwareBuffer) {
    if (!hardwareBuffer) return -1;
    return CopyFrameFromHardwareBuffer(env, hardwareBuffer);
}

static void nativeReleaseFrameBuffers(JNIEnv *env, jclass clazz) {
    ReleaseFrameBuffers();
}

static jobject nativeGetFrameBufferBitmap(JNIEnv *env, jclass clazz) {
    if (!g_frameBuffersInitialized) {
        LOGE("getFrameBufferBitmap: frame buffers not initialized");
        return nullptr;
    }

    const FrameBuffer *frame = LockCurrentFrame();
    if (!frame || !frame->data || frame->frameCount == 0) {
        LOGE("getFrameBufferBitmap: no valid frame available");
        return nullptr;
    }

    jclass bitmapClass = env->FindClass("android/graphics/Bitmap");
    if (!bitmapClass) {
        LOGE("getFrameBufferBitmap: failed to find Bitmap class");
        UnlockFrame(frame);
        return nullptr;
    }

    jclass configClass = env->FindClass("android/graphics/Bitmap$Config");
    if (!configClass) {
        LOGE("getFrameBufferBitmap: failed to find Bitmap$Config class");
        env->DeleteLocalRef(bitmapClass);
        UnlockFrame(frame);
        return nullptr;
    }

    jfieldID argb8888Field = env->GetStaticFieldID(configClass, "ARGB_8888",
                                                   "Landroid/graphics/Bitmap$Config;");
    if (!argb8888Field) {
        LOGE("getFrameBufferBitmap: failed to find ARGB_8888 field");
        env->DeleteLocalRef(configClass);
        env->DeleteLocalRef(bitmapClass);
        UnlockFrame(frame);
        return nullptr;
    }

    jobject argb8888Config = env->GetStaticObjectField(configClass, argb8888Field);
    if (!argb8888Config) {
        LOGE("getFrameBufferBitmap: failed to get ARGB_8888 config");
        env->DeleteLocalRef(configClass);
        env->DeleteLocalRef(bitmapClass);
        UnlockFrame(frame);
        return nullptr;
    }

    jmethodID createBitmapMethod = env->GetStaticMethodID(bitmapClass, "createBitmap",
                                                          "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    if (!createBitmapMethod) {
        LOGE("getFrameBufferBitmap: failed to find createBitmap method");
        env->DeleteLocalRef(argb8888Config);
        env->DeleteLocalRef(configClass);
        env->DeleteLocalRef(bitmapClass);
        UnlockFrame(frame);
        return nullptr;
    }

    jobject bitmap = env->CallStaticObjectMethod(bitmapClass, createBitmapMethod,
                                                 frame->width, frame->height, argb8888Config);
    if (!bitmap || env->ExceptionCheck()) {
        LOGE("getFrameBufferBitmap: failed to create bitmap");
        env->ExceptionClear();
        env->DeleteLocalRef(argb8888Config);
        env->DeleteLocalRef(configClass);
        env->DeleteLocalRef(bitmapClass);
        UnlockFrame(frame);
        return nullptr;
    }

    void *bitmapPixels = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &bitmapPixels) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("getFrameBufferBitmap: failed to lock bitmap pixels");
        env->DeleteLocalRef(bitmap);
        env->DeleteLocalRef(argb8888Config);
        env->DeleteLocalRef(configClass);
        env->DeleteLocalRef(bitmapClass);
        UnlockFrame(frame);
        return nullptr;
    }

    AndroidBitmapInfo bitmapInfo;
    if (AndroidBitmap_getInfo(env, bitmap, &bitmapInfo) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("getFrameBufferBitmap: failed to get bitmap info");
        AndroidBitmap_unlockPixels(env, bitmap);
        env->DeleteLocalRef(bitmap);
        env->DeleteLocalRef(argb8888Config);
        env->DeleteLocalRef(configClass);
        env->DeleteLocalRef(bitmapClass);
        UnlockFrame(frame);
        return nullptr;
    }

    if (bitmapInfo.stride == (uint32_t) frame->stride) {
        memcpy(bitmapPixels, frame->data, frame->size);
    } else {
        int rowBytes = frame->width * 4;
        for (int y = 0; y < frame->height; ++y) {
            memcpy((uint8_t *) bitmapPixels + y * bitmapInfo.stride,
                   frame->data + y * frame->stride,
                   rowBytes);
        }
    }

    AndroidBitmap_unlockPixels(env, bitmap);

    env->DeleteLocalRef(argb8888Config);
    env->DeleteLocalRef(configClass);
    env->DeleteLocalRef(bitmapClass);

    UnlockFrame(frame);

    LOGI("getFrameBufferBitmap: created %dx%d bitmap from frame #%lld",
         frame->width, frame->height, (long long) frame->frameCount);

    return bitmap;
}
