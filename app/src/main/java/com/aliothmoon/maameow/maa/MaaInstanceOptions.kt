package com.aliothmoon.maameow.maa

object MaaInstanceOptions {
    const val TOUCH_MODE = 2
    const val ANDROID = "Android"
    const val DEPLOYMENT_WITH_PAUSE = 3

    /**
     * 客户端类型 (v6.9.0+ 新增)
     *
     * 在 AsstConnect 之前调用，让 Core 根据当前服务器渠道自动解析正确的游戏包名。
     * 取值: Official / Bilibili / YoStarJP / YoStarKR / YoStarEN / txwy
     *
     * 对应 WPF: InstanceOptionKey.ClientType = 6
     */
    const val CLIENT_TYPE = 6
}