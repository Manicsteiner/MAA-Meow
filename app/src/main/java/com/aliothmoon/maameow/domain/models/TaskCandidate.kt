package com.aliothmoon.maameow.domain.models

import com.aliothmoon.maameow.data.model.LogLevel
import com.aliothmoon.maameow.maa.task.MaaTaskType
import com.aliothmoon.maameow.utils.i18n.UiText

/**
 * 一个任务位的后备候选：主任务 append 被 core 拒绝时按序尝试，首个成功即停
 *
 * 用 core 的 append 结果当权威，而不是在 App 侧复刻 core 的参数校验规则
 * —— 后者住在独立钉版本的 libMaaCore.so 里，抄一份必然漂移
 */
data class TaskCandidate(
    val type: MaaTaskType,
    val params: String,
    val logName: UiText,
    /** 命中后才 stage，否则候选之间会互相覆盖同一任务位 */
    val dropTarget: DropTarget,
    /** 尝试本候选前补打：上游失败后继续评估时输出的、被跳过的计划 */
    val logsBefore: List<Pair<UiText, LogLevel>> = emptyList(),
    /**
     * append 成功后才打。仅后备候选有值：主任务的同类日志在预检阶段就已产出，
     * 那是有意的——预检回放发生在建显示器/连接之前，挪到 append 时会被几十秒的
     * 连接过程与相邻的计划日志隔开。别"对齐"回去
     */
    val logOnSuccess: Pair<UiText, LogLevel>? = null,
)
