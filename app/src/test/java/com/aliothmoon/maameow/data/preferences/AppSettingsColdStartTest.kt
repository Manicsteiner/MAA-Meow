package com.aliothmoon.maameow.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.aliothmoon.maameow.data.preferences.AppSettingsManager.Companion.dataStore
import com.aliothmoon.maameow.data.repository.FakePreferencesDataStore
import com.aliothmoon.maameow.domain.models.AppSettingsSchema
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.coroutines.CoroutineContext

class AppSettingsColdStartTest {
    @Test
    fun persistedUnlockTypeIsAvailableBeforeSettingsCollectorsRun() = runBlocking {
        mockkObject(AppSettingsManager.Companion)
        try {
            for ((stored, expected) in listOf("pin" to "pin", "gesture" to "gesture", "invalid" to "swipe")) {
                val store = FakePreferencesDataStore()
                store.edit {
                    it[AppSettingsSchema.wakeUnlockType] = stored
                    it[AppSettingsSchema.wakeCredential] = "1234"
                }
                val context = mockk<Context>(relaxed = true)
                every { context.dataStore } returns store
                val pending = ArrayDeque<Runnable>()
                val dispatcher = object : CoroutineDispatcher() {
                    override fun dispatch(context: CoroutineContext, block: Runnable) {
                        pending.addLast(block)
                    }
                }
                val scope = CoroutineScope(SupervisorJob() + dispatcher)
                try {
                    val settings = AppSettingsManager(context, mockk(relaxed = true), scope)
                    assertEquals(expected, settings.wakeUnlockType.value)
                    assertEquals("1234", settings.wakeCredential.value)
                } finally {
                    scope.cancel()
                    while (pending.isNotEmpty()) pending.removeFirst().run()
                }
            }
        } finally {
            unmockkObject(AppSettingsManager.Companion)
        }
    }
}
