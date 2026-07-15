package com.aliothmoon.maameow

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #181 防回归 tripwire：以源码文本断言约束静音语义，不是行为测试。
 * 背景：RemoteServiceManager 是 object 单例无法注入，行为测试需先抽接口；
 * 在此之前用文本断言兜底「任务结束不得触碰静音状态、连接对账只重发静音」两条契约。
 */
class GameMuteCoordinatorContractTest {

    @Test
    fun taskCompletion_doesNotDriveGameMuteState() {
        val source = coordinatorSource()

        assertFalse(source.contains("compositionService.state"))
        assertFalse(source.contains("MaaExecutionState"))
        assertFalse(source.contains("suspend fun unmute()"))
        assertFalse(backgroundTaskViewModelSource().contains("gameMuteCoordinator.unmute()"))
    }

    @Test
    fun reconnect_reappliesPersistedMuteIntent() {
        val source = coordinatorSource()
        val startAnchor = "private suspend fun reconcileOnConnected()"
        val endAnchor = "private suspend fun currentMutedPackage"
        assertTrue("anchor missing: $startAnchor", source.contains(startAnchor))
        assertTrue("anchor missing: $endAnchor", source.contains(endAnchor))
        val reconcileBody = source
            .substringAfter(startAnchor)
            .substringBefore(endAnchor)

        assertTrue(reconcileBody.contains("requestRemote(pkg, mute = true)"))
        assertFalse(reconcileBody.contains("requestRemote(pkg, mute = false)"))
        assertFalse(reconcileBody.contains("setMutedGamePackage(\"\")"))
    }

    private fun coordinatorSource(): String = resolveSourceFile(
        "src/main/java/com/aliothmoon/maameow/domain/service/GameMuteCoordinator.kt"
    ).readText()

    private fun backgroundTaskViewModelSource(): String = resolveSourceFile(
        "src/main/java/com/aliothmoon/maameow/presentation/viewmodel/BackgroundTaskViewModel.kt"
    ).readText()

    private fun resolveSourceFile(relativePath: String): File {
        val candidates = listOf(
            File(relativePath),
            File("app/$relativePath"),
            File("../app/$relativePath"),
        )
        val file = candidates.firstOrNull { it.isFile }
        checkNotNull(file) { "Source file not found for test: $relativePath" }
        return file
    }
}
