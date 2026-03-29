package com.aliothmoon.maameow.domain.usecase

import com.aliothmoon.maameow.data.model.FightConfig
import com.aliothmoon.maameow.data.model.MallConfig
import com.aliothmoon.maameow.data.model.TaskChainNode
import com.aliothmoon.maameow.data.model.WakeUpConfig
import com.aliothmoon.maameow.data.preferences.TaskChainState
import com.aliothmoon.maameow.data.resource.ServerTimezone
import com.aliothmoon.maameow.domain.models.MallCreditFightAvailability
import com.aliothmoon.maameow.domain.models.resolveMallCreditFightAvailability
import com.aliothmoon.maameow.maa.task.MaaTaskParams
import timber.log.Timber
import java.time.DayOfWeek

class BuildTaskParamsUseCase(private val chainState: TaskChainState) {
    operator fun invoke(): List<MaaTaskParams> {
        return buildFrom(chainState.chain.value)
    }

    /** 校验任务链配置，返回错误信息或 null 表示通过 */
    fun validate(chain: List<TaskChainNode>): String? {
        val nodes = chain.filter { it.enabled }
        if (nodes.isEmpty()) return "请先选择要执行的任务"
        return validateClientTypeConsistency(nodes)
    }

    /** 多个开始唤醒节点的客户端类型必须一致 */
    private fun validateClientTypeConsistency(nodes: List<TaskChainNode>): String? {
        val clientTypes = nodes
            .mapNotNull { (it.config as? WakeUpConfig)?.clientType }
            .distinct()
        if (clientTypes.size > 1) {
            return "任务链中存在多个不同的客户端类型（${clientTypes.joinToString("、")}），请保持一致"
        }
        return null
    }

    fun buildFrom(chain: List<TaskChainNode>): List<MaaTaskParams> {
        val enabledNodes = chain.filter { it.enabled }.sortedBy { it.order }
        val clientType = resolveClientType(enabledNodes)
        val creditFightAvailability = resolveMallCreditFightAvailability(enabledNodes)
        val serverDayOfWeek = ServerTimezone.getYjDayOfWeek(clientType)

        logCreditFightWarning(enabledNodes, creditFightAvailability)

        return enabledNodes.mapNotNull { node ->
            if (isSkippedByWeeklySchedule(node, serverDayOfWeek)) return@mapNotNull null
            buildNodeParams(node, creditFightAvailability, clientType)
        }
    }

    /** 从任务链中提取客户端类型（取第一个 WakeUpConfig 的 clientType） */
    private fun resolveClientType(nodes: List<TaskChainNode>): String {
        return nodes.firstNotNullOfOrNull { (it.config as? WakeUpConfig)?.clientType } ?: "Official"
    }

    /** 周计划过滤：启用周计划且今天被禁用 → 跳过 */
    private fun isSkippedByWeeklySchedule(node: TaskChainNode, serverDayOfWeek: DayOfWeek): Boolean {
        val config = node.config
        if (config is FightConfig && config.useWeeklySchedule) {
            if (config.weeklySchedule[serverDayOfWeek.name] == false) {
                Timber.d("WeeklySchedule: skip node '%s' on %s", node.name, serverDayOfWeek)
                return true
            }
        }
        return false
    }

    /** 将单个节点转换为 MaaCore 任务参数 */
    private fun buildNodeParams(
        node: TaskChainNode,
        creditFightAvailability: MallCreditFightAvailability,
        clientType: String,
    ): MaaTaskParams {
        return when (val config = node.config) {
            is MallConfig -> config.toTaskParams(
                creditFightEnabled = config.creditFight && creditFightAvailability.isAvailable,
                clientType = clientType,
            )
            else -> config.toTaskParams()
        }
    }

    /** 信用战不可用时输出警告日志 */
    private fun logCreditFightWarning(
        nodes: List<TaskChainNode>,
        availability: MallCreditFightAvailability,
    ) {
        if (!availability.isAvailable && nodes.any { (it.config as? MallConfig)?.creditFight == true }) {
            Timber.w(
                "Credit fight disabled because a fight task has no resolvable active stage. task=%s order=%d",
                availability.blockingTaskName ?: "unknown",
                availability.blockingTaskOrder ?: -1,
            )
        }
    }
}
