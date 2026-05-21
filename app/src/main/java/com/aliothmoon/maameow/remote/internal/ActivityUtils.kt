package com.aliothmoon.maameow.remote.internal

import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import com.aliothmoon.maameow.third.FakeContext
import com.aliothmoon.maameow.third.Ln
import com.aliothmoon.maameow.third.wrappers.ServiceManager

object ActivityUtils {
    /**
     * 以 shell 身份启动指定 Intent 的 Activity，绕过 BAL 限制。
     */
    @JvmStatic
    @JvmOverloads
    fun startActivity(intent: Intent, displayId: Int = 0): Boolean {
        val am = ServiceManager.getActivityManager()
        try {
            val launchOptions = ActivityOptions.makeBasic()
            if (displayId != 0) {
                launchOptions.setLaunchDisplayId(displayId)
            }
            val ret = try {
                am.startActivity(intent, launchOptions.toBundle())
            } catch (e: Exception) {
                Ln.w("startActivity failed, returning -1", e)
                -1
            }
            if (ret < 0) {
                Ln.w("startActivity returned error code $ret, fallback to am command")
                return startViaAmCommand(intent, displayId)
            }
            return true
        } catch (e: Exception) {
            Ln.w("startActivity failed, fallback to am command", e)
            return startViaAmCommand(intent, displayId)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun startApp(
        packageName: String,
        displayId: Int,
        forceStop: Boolean = true,
        excludeFromRecents: Boolean = true
    ): Boolean {
        val pm = FakeContext.get().packageManager

        val intent = pm.getLaunchIntentForPackage(packageName) ?: run {
            pm.getLeanbackLaunchIntentForPackage(packageName)
        }

        if (intent == null) {
            Ln.w("Cannot create launch intent for app $packageName")
            return false
        }

        var flag = Intent.FLAG_ACTIVITY_NEW_TASK
        if (excludeFromRecents) {
            flag = flag or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        }
        intent.addFlags(flag)

        if (forceStop) {
            ServiceManager.getActivityManager().forceStopPackage(packageName)
        }
        Ln.i("startApp ${intent.component?.flattenToShortString()}")

        return startActivity(intent, displayId)
    }

    /**
     * 检查指定包名的应用是否有活动 task 运行在给定 displayId 上。
     * API 28 无 TaskInfo.displayId 字段（@hide），宽松返回 true（不拦截）。
     * 任何异常也宽松返回 true，避免误伤。
     */
    fun isAppOnDisplay(packageName: String, targetDisplayId: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
        return runCatching {
            val am = FakeContext.get().getSystemService(ActivityManager::class.java)
                ?: return@runCatching true
            @Suppress("DEPRECATION")
            am.getRunningTasks(100).any { task ->
                task.topActivity?.packageName == packageName
                    && getTaskDisplayId(task) == targetDisplayId
            }
        }.getOrDefault(true)
    }

    private val taskDisplayIdField by lazy {
        runCatching {
            var cls: Class<*>? = ActivityManager.RunningTaskInfo::class.java
            var field: java.lang.reflect.Field? = null
            while (cls != null && field == null) {
                field = runCatching { cls.getDeclaredField("displayId") }.getOrNull()
                cls = cls.superclass
            }
            field?.also { it.isAccessible = true }
        }.onFailure { Ln.w("taskDisplayIdField: reflection failed", it) }.getOrNull()
    }

    private fun getTaskDisplayId(task: ActivityManager.RunningTaskInfo): Int =
        runCatching { taskDisplayIdField?.getInt(task) ?: -1 }.getOrDefault(-1)

    private fun startViaAmCommand(intent: Intent, displayId: Int): Boolean {
        try {
            val intentUri = intent.toUri(Intent.URI_INTENT_SCHEME)
            val args = arrayOf("am", "start", "--display", displayId.toString(), intentUri)
            Ln.d("Executing: am start --display $displayId <intent-uri>")
            val process = Runtime.getRuntime().exec(args)
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                Ln.w("am command exited with code $exitCode")
                return false
            }
            return true
        } catch (e: Exception) {
            Ln.e("am command fallback also failed", e)
            return false
        }
    }
}
