#ifndef BRIDGE_FRAME_BUFFER_H
#define BRIDGE_FRAME_BUFFER_H

#include "bridge_internal.h"

#include <android/hardware_buffer.h>

typedef enum {
    FRAME_STATE_FREE = 0,
    FRAME_STATE_WRITING = 2
} FrameBufferState;

#define FRAME_BUFFER_COUNT 3

typedef struct {
    uint8_t *data;
    uint8_t *bgr_data;
    int width;
    int height;
    int stride;
    size_t size;
    size_t bgr_size;
    int64_t timestamp;
    int64_t frameCount;
} FrameBuffer;

void InitFrameBuffers(int width, int height);
void ReleaseFrameBuffers();
bool WriteHardwareBufferToFrame(AHardwareBuffer *buffer, int64_t timestampNs);
jobject CreateFrameBufferBitmap(JNIEnv *env);

#endif // BRIDGE_FRAME_BUFFER_H
