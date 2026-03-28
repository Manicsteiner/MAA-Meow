package com.aliothmoon.maameow.data.model;

import androidx.compose.ui.graphics.Color

/**
 * 日志级别
 *
 *
 * 参考 MaaWPFGUI 的 UiLogColor 设计
 */
enum class LogLevel(val displayName: String, val color: Color, val severity: LogSeverity) {
    /** 普通消息 - 灰色 */
    MESSAGE("MSG", Color.Black, LogSeverity.MESSAGE),

    /** 信息 - 蓝色 */
    INFO("INFO", Color(0xFF409EFF), LogSeverity.INFO),

    /** 成功 - 绿色 */
    SUCCESS("SUCCESS", Color(0xFF67C23A), LogSeverity.INFO),

    /** 警告 - 橙色 */
    WARNING("WRN", Color(0xFFE6A23C), LogSeverity.WARNING),

    /** 错误 - 红色 */
    ERROR("ERR", Color(0xFFF56C6C), LogSeverity.ERROR),

    /** 追踪 - 浅灰色 */
    TRACE("TRACE", Color(0xFF909399), LogSeverity.TRACE),

    /** 1星干员 - 黑色 */
    RECRUIT_STAR_1("1星", Color(0xFF333333), LogSeverity.INFO),

    /** 2星干员 - 黄绿色 */
    RECRUIT_STAR_2("2星", Color(0xFF99CC33), LogSeverity.INFO),

    /** 3星干员 - 蓝色 */
    RECRUIT_STAR_3("3星", Color(0xFF3399FF), LogSeverity.INFO),

    /** 4星干员 - 紫色 */
    RECRUIT_STAR_4("4星", Color(0xFF9966FF), LogSeverity.INFO),

    /** 5星干员 - 橙色 */
    RECRUIT_STAR_5("5星", Color(0xFFFFAA33), LogSeverity.INFO),

    /** 6星干员 - 深橙色 */
    RECRUIT_STAR_6("6星", Color(0xFFFF8C00), LogSeverity.INFO),

    /** 支援机械 - 深灰色 */
    RECRUIT_ROBOT("机械", Color(0xFF666666), LogSeverity.INFO),

    // 肉鸽专用颜色
    /** 肉鸽战斗成功 - 深绿色 */
    ROGUELIKE_SUCCESS("战斗成功", Color(0xFF52C41A), LogSeverity.INFO),

    /** 普通作战节点 - 蓝色 */
    ROGUELIKE_COMBAT("作战", Color(0xFF1890FF), LogSeverity.INFO),

    /** 紧急作战节点 - 橙色 */
    ROGUELIKE_EMERGENCY("紧急", Color(0xFFFA8C16), LogSeverity.INFO),

    /** BOSS节点 - 红色 */
    ROGUELIKE_BOSS("领袖", Color(0xFFEB2F96), LogSeverity.INFO),

    /** 放弃探索 - 灰色 */
    ROGUELIKE_ABANDON("放弃", Color(0xFF8C8C8C), LogSeverity.INFO),

    /** 稀有干员/物品 - 金色 */
    RARE("稀有", Color(0xFFFFAA00), LogSeverity.INFO);

    companion object {
        /**
         * 根据公招星级获取对应的日志级别
         */
        fun forRecruitStar(star: Int): LogLevel {
            return when (star) {
                1 -> RECRUIT_STAR_1
                2 -> RECRUIT_STAR_2
                3 -> RECRUIT_STAR_3
                4 -> RECRUIT_STAR_4
                5 -> RECRUIT_STAR_5
                6 -> RECRUIT_STAR_6
                else -> MESSAGE
            }
        }
    }
}
