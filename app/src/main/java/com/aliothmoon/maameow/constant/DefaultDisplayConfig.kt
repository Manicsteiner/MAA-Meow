package com.aliothmoon.maameow.constant

/**
 * 默认允许的一些宽高配置
 */
object DefaultDisplayConfig {
    const val VD_NAME = "MAA_VD"
    const val DISPLAY_NONE = -1

    // 720p (默认，适用于大多数客户端)
    const val WIDTH = 1280
    const val HEIGHT = 720
    const val DPI = 160

    const val ASPECT_RATIO_WIDTH = 16
    const val ASPECT_RATIO_HEIGHT = 9

    /** 16:9 宽高比 */
    val ASPECT_RATIO: Float get() = WIDTH.toFloat() / HEIGHT

    const val FRAME_INTERVAL_MS = 16L

    data class Resolution(val width: Int, val height: Int, val dpi: Int)

    /**
     * YoStarEN → 1080p，其他 → 720p
     */
    fun resolveResolution(clientType: String): Resolution = when (clientType) {
        "YoStarEN" -> Resolution(1920, 1080, 240)
        else -> Resolution(WIDTH, HEIGHT, DPI)
    }
}
