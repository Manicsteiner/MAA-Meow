package com.aliothmoon.maameow.remote.internal

import com.aliothmoon.maameow.third.Ln

object AppOpsHelper {
    private const val TAG = "AppOpsHelper"

    fun setPlayAudioOpAllowed(packageName: String?, isAllowed: Boolean): Boolean {
        if (packageName.isNullOrBlank()) return false
        val op = if (isAllowed) "allow" else "deny"
        try {
            val process = Runtime.getRuntime()
                .exec(arrayOf("sh", "-c", "appops set $packageName PLAY_AUDIO $op"))
            val exitCode = process.waitFor()
            Ln.i("$TAG: appops set $packageName PLAY_AUDIO $op -> exitCode=$exitCode")
            return exitCode == 0
        } catch (e: Exception) {
            Ln.e("$TAG: setPlayAudioOpAllowed failed: ${e.message}")
            return false
        }
    }
}
