package com.aliothmoon.maameow.domain.service

import com.aliothmoon.maameow.constant.Packages
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.manager.RemoteServiceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber


class GameMuteCoordinator(
    private val appSettingsManager: AppSettingsManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    /** 由持久化标记派生的静音状态，仅供 UI 观察（可能滞后写入瞬间，逻辑判断勿用） */
    val isMuted: StateFlow<Boolean> = appSettingsManager.mutedGamePackage
        .map { it.isNotEmpty() }
        .stateIn(
            scope, SharingStarted.Eagerly,
            appSettingsManager.mutedGamePackage.value.isNotEmpty()
        )

    fun start() {
        // 远端服务每次连接后对账：持久化标记代表用户当前的静音意图，始终重发静音。
        // 不 drop 初始值：若 start() 时服务已连接，该次连接同样需要对账
        scope.launch {
            RemoteServiceManager.state.collect { state ->
                if (state is RemoteServiceManager.ServiceState.Connected) {
                    reconcileOnConnected()
                }
            }
        }
    }

    /** 静音。远端失败时自行还原原始 mode（见 GameAudioMuteController），此处仅回滚标记 */
    suspend fun mute(clientType: String?): Boolean = mutex.withLock { muteLocked(clientType) }

    suspend fun toggle(clientType: String?): Boolean = mutex.withLock {
        val pkg = currentMutedPackage()
        if (pkg.isNotEmpty()) unmuteLocked(pkg) else muteLocked(clientType)
    }

    private suspend fun muteLocked(clientType: String?): Boolean {
        val pkg = clientType?.let { Packages[it] } ?: return false
        if (currentMutedPackage() == pkg) {
            Timber.d("Mute request ignored because %s is already marked muted", pkg)
            return true
        }
        appSettingsManager.setMutedGamePackage(pkg) // write-ahead：先持久化再动系统状态
        val ok = requestRemote(pkg, mute = true)
        if (!ok) {
            appSettingsManager.setMutedGamePackage("")
            Timber.w("Mute %s failed", pkg)
        }
        return ok
    }

    private suspend fun unmuteLocked(pkg: String): Boolean {
        if (pkg.isEmpty()) return true
        val ok = requestRemote(pkg, mute = false)
        if (ok) {
            appSettingsManager.setMutedGamePackage("")
        } else {
            Timber.w("Unmute %s failed, flag kept (system state is still muted)", pkg)
        }
        return ok
    }

    private suspend fun reconcileOnConnected() = mutex.withLock {
        val pkg = currentMutedPackage()
        if (pkg.isEmpty()) return@withLock
        Timber.i("Service connected with persisted mute intent, re-muting %s", pkg)
        requestRemote(pkg, mute = true)
    }

    /** 权威读取：直读 DataStore，绕开派生 StateFlow 的传播延迟 */
    private suspend fun currentMutedPackage(): String =
        appSettingsManager.settings.first().mutedGamePackage

    private suspend fun requestRemote(pkg: String, mute: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                RemoteServiceManager.getInstanceOrNull()
                    ?.setPlayAudioOpAllowed(pkg, !mute) == true
            }.onFailure {
                Timber.w(it, "setPlayAudioOpAllowed(%s, mute=%s) IPC failed", pkg, mute)
            }.getOrDefault(false)
        }
}
