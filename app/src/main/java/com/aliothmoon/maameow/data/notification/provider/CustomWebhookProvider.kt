package com.aliothmoon.maameow.data.notification.provider

import com.aliothmoon.maameow.data.api.HttpClientHelper
import com.aliothmoon.maameow.data.notification.NotificationSettingsManager
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CustomWebhookProvider(
    private val httpClient: HttpClientHelper,
    private val settingsManager: NotificationSettingsManager
) : NotificationProvider {

    override val id = "CustomWebhook"

    override suspend fun send(title: String, content: String): Boolean {
        val settings = settingsManager.settings.first()
        val url = settings.customWebhookUrl.takeIf { it.isNotEmpty() } ?: return false
        val bodyTemplate = settings.customWebhookBody.takeIf { it.isNotEmpty() } ?: return false

        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val body = bodyTemplate
            .replace("{title}", title.replace("\r", "").replace("\n", ""))
            .replace("{content}", content.replace("\r", "").replace("\n", "\\n"))
            .replace("{time}", now)

        val headers = settings.customWebhookHeaders
            .replace("\r", "")
            .split("\n")
            .filter { it.contains(":") }
            .mapNotNull { line ->
                val idx = line.indexOf(':')
                if (idx <= 0) null
                else line.substring(0, idx).trim() to line.substring(idx + 1).trim()
            }
            .toMap()

        return runCatching {
            httpClient.post(url, body, headers = headers).use { response ->
                response.isSuccessful
            }
        }.getOrElse {
            Timber.e(it, "CustomWebhook send failed")
            false
        }
    }
}
