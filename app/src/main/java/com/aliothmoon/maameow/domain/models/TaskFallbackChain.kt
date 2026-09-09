package com.aliothmoon.maameow.domain.models

import com.aliothmoon.maameow.data.model.LogLevel
import com.aliothmoon.maameow.utils.i18n.UiText

/** 一个任务位的后备链 */
data class TaskFallbackChain(
    /** 按序尝试，首个 append 成功即停 */
    val candidates: List<TaskCandidate> = emptyList(),
    /**
     * 末尾被跳过的计划日志，只在全部候选都失败时补打。
     * 上游成功后 break 不会评估后面的计划，全失败才会走到列表末尾——这里对齐它
     */
    val logsWhenExhausted: List<Pair<UiText, LogLevel>> = emptyList(),
) {
    val isEmpty: Boolean get() = candidates.isEmpty() && logsWhenExhausted.isEmpty()
}

/**
 * 按序尝试候选，首个 append 成功即停；返回命中的 taskId 与候选，全失败返回 null
 *
 * 副作用全部经参数注入，便于直接测「core 连续拒绝、后备成功」这条路径
 */
inline fun TaskFallbackChain.appendFirstSuccessful(
    log: (UiText, LogLevel) -> Unit,
    append: (TaskCandidate) -> Int,
    onAppendFailed: (TaskCandidate) -> Unit,
): Pair<Int, TaskCandidate>? {
    for (c in candidates) {
        c.logsBefore.forEach { (text, level) -> log(text, level) }
        val id = append(c)
        if (id <= 0) {
            onAppendFailed(c)
            continue
        }
        c.logOnSuccess?.let { (text, level) -> log(text, level) }
        return id to c
    }
    logsWhenExhausted.forEach { (text, level) -> log(text, level) }
    return null
}
