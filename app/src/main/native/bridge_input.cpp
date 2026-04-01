#include <unistd.h>
#include "bridge_input.h"

static JavaVM *g_jvm = nullptr;
static jclass g_driverClass = nullptr;
static jmethodID g_touchDownMethod = nullptr;
static jmethodID g_touchMoveMethod = nullptr;
static jmethodID g_touchUpMethod = nullptr;
static jmethodID g_keyDownMethod = nullptr;
static jmethodID g_keyUpMethod = nullptr;
static jmethodID g_startAppMethod = nullptr;

static int
UpcallInputControl(JNIEnv *env, MethodType method, int x, int y, int keyCode, int displayId) {
    if (!env || !g_driverClass) {
        return -1;
    }

    switch (method) {
        case TOUCH_DOWN:
            return env->CallStaticBooleanMethod(g_driverClass, g_touchDownMethod, x, y, displayId)
                   ? 0 : -1;
        case TOUCH_MOVE:
            return env->CallStaticBooleanMethod(g_driverClass, g_touchMoveMethod, x, y, displayId)
                   ? 0 : -1;
        case TOUCH_UP:
            return env->CallStaticBooleanMethod(g_driverClass, g_touchUpMethod, x, y, displayId)
                   ? 0 : -1;
        case KEY_DOWN:
            return env->CallStaticBooleanMethod(g_driverClass, g_keyDownMethod, keyCode, displayId)
                   ? 0 : -1;
        case KEY_UP:
            return env->CallStaticBooleanMethod(g_driverClass, g_keyUpMethod, keyCode, displayId)
                   ? 0 : -1;
        default:
            return -1;
    }
}

static int UpcallStartApp(JNIEnv *env, const char *packageName, int displayId, bool forceStop) {
    if (!env || !packageName || !g_driverClass || !g_startAppMethod) {
        return -1;
    }

    jstring jPackageName = env->NewStringUTF(packageName);
    jboolean result = env->CallStaticBooleanMethod(g_driverClass, g_startAppMethod, jPackageName,
                                                   displayId, static_cast<jboolean>(forceStop));
    env->DeleteLocalRef(jPackageName);
    return result ? 0 : -1;
}

bool InitInputBridge(JavaVM *vm, JNIEnv *env, const char *driverClassName) {
    g_jvm = vm;
    if (!env || !driverClassName) {
        return false;
    }

    jclass driverClass = env->FindClass(driverClassName);
    if (!driverClass || CheckJNIException(env, "FindClass(driverClassName)")) {
        return false;
    }

    g_driverClass = static_cast<jclass>(env->NewGlobalRef(driverClass));
    env->DeleteLocalRef(driverClass);
    if (!g_driverClass) {
        return false;
    }

    g_touchDownMethod = env->GetStaticMethodID(g_driverClass, "touchDown", "(III)Z");
    g_touchMoveMethod = env->GetStaticMethodID(g_driverClass, "touchMove", "(III)Z");
    g_touchUpMethod = env->GetStaticMethodID(g_driverClass, "touchUp", "(III)Z");
    g_keyDownMethod = env->GetStaticMethodID(g_driverClass, "keyDown", "(II)Z");
    g_keyUpMethod = env->GetStaticMethodID(g_driverClass, "keyUp", "(II)Z");
    g_startAppMethod = env->GetStaticMethodID(g_driverClass, "startApp", "(Ljava/lang/String;IZ)Z");

    if (CheckJNIException(env, "GetStaticMethodID(DriverClass)") ||
        !g_touchDownMethod || !g_touchMoveMethod || !g_touchUpMethod ||
        !g_keyDownMethod || !g_keyUpMethod || !g_startAppMethod) {
        ReleaseInputBridge(env);
        return false;
    }

    return true;
}

void ReleaseInputBridge(JNIEnv *env) {
    g_touchDownMethod = nullptr;
    g_touchMoveMethod = nullptr;
    g_touchUpMethod = nullptr;
    g_keyDownMethod = nullptr;
    g_keyUpMethod = nullptr;
    g_startAppMethod = nullptr;

    if (g_driverClass && env) {
        env->DeleteGlobalRef(g_driverClass);
    }
    g_driverClass = nullptr;
    g_jvm = nullptr;
}

struct JniThreadAttacher {
    JNIEnv *env = nullptr;
    bool needs_detach = false;

    JniThreadAttacher() {
        if (!g_jvm) return;
        if (g_jvm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
            if (g_jvm->AttachCurrentThreadAsDaemon(&env, nullptr) == JNI_OK) {
                needs_detach = true;
                LOGI("JniThreadAttacher: attached thread %d", gettid());
            } else {
                LOGE("JniThreadAttacher: attach failed for thread %d", gettid());
            }
        }
    }

    ~JniThreadAttacher() {
        if (needs_detach && g_jvm) {
            LOGI("JniThreadAttacher: detaching thread %d", gettid());
            g_jvm->DetachCurrentThread();
        }
    }
};

static JNIEnv *GetJNIEnv() {
    thread_local JniThreadAttacher attacher;
    return attacher.env;
}

BRIDGE_API int DispatchInputMessage(MethodParam param) {
    LOGD("DispatchInputMessage: method=%d display_id=%d", param.method, param.display_id);

    auto *env = GetJNIEnv();
    if (!env) {
        return -1;
    }

    switch (param.method) {
        case TOUCH_DOWN:
            return UpcallInputControl(env, TOUCH_DOWN, param.args.touch.p.x, param.args.touch.p.y,
                                      0, param.display_id);
        case TOUCH_MOVE:
            return UpcallInputControl(env, TOUCH_MOVE, param.args.touch.p.x, param.args.touch.p.y,
                                      0, param.display_id);
        case TOUCH_UP:
            return UpcallInputControl(env, TOUCH_UP, param.args.touch.p.x, param.args.touch.p.y, 0,
                                      param.display_id);
        case KEY_DOWN:
            return UpcallInputControl(env, KEY_DOWN, 0, 0, param.args.key.key_code,
                                      param.display_id);
        case KEY_UP:
            return UpcallInputControl(env, KEY_UP, 0, 0, param.args.key.key_code, param.display_id);
        case START_GAME:
            return UpcallStartApp(env, param.args.start_game.package_name, param.display_id,
                                  param.args.start_game.force_stop != 0);
        default:
            return 0;
    }
}
