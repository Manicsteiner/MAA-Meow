package com.aliothmoon.maameow.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.domain.models.RunMode
import com.aliothmoon.maameow.schedule.model.ScheduleStrategy
import com.aliothmoon.maameow.schedule.model.ScheduleType
import com.aliothmoon.maameow.schedule.service.ScheduleAlarmManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleAlarmManagerTest {
    private val platformAlarms = mockk<AlarmManager>(relaxed = true)
    private val context = mockk<Context> {
        every { getSystemService(Context.ALARM_SERVICE) } returns platformAlarms
    }
    private val settings = mockk<AppSettingsManager> {
        every { runMode } returns MutableStateFlow(RunMode.BACKGROUND)
    }
    private val alarms = spyk(ScheduleAlarmManager(context, settings))
    private val strategy = ScheduleStrategy(
        id = "daily", name = "Daily", profileId = "profile-1",
        scheduleType = ScheduleType.INTERVAL,
        startTimeMs = System.currentTimeMillis() + 3_600_000L,
        intervalMinutes = 60,
    )

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun missingPermissionDoesNotAttemptAlarmClockFallback() {
        every { alarms.canScheduleExact() } returns false
        assertFalse(alarms.scheduleNext(strategy))
        assertFalse(alarms.scheduleRetry(strategy.id, 123L))
        verify(exactly = 0) { platformAlarms.setExactAndAllowWhileIdle(any(), any(), any()) }
        verify(exactly = 0) { platformAlarms.setAlarmClock(any(), any()) }
    }

    @Test
    fun revokedPermissionDuringRegistrationDoesNotCrash_andGrantAllowsRescheduling() {
        every { alarms.canScheduleExact() } returns true
        mockkConstructor(Intent::class)
        every { anyConstructed<Intent>().setClassName(any<Context>(), any()) } answers { self as Intent }
        every { anyConstructed<Intent>().putExtra(any<String>(), any<String>()) } answers { self as Intent }
        every { anyConstructed<Intent>().putExtra(any<String>(), any<Long>()) } answers { self as Intent }
        every { anyConstructed<Intent>().putExtra(any<String>(), any<Int>()) } answers { self as Intent }
        mockkStatic(PendingIntent::class)
        every { PendingIntent.getBroadcast(any(), any(), any(), any()) } returns mockk()
        every { platformAlarms.setExactAndAllowWhileIdle(any(), any(), any()) } throws SecurityException("revoked")
        assertFalse(alarms.scheduleNext(strategy))
        every { platformAlarms.setExactAndAllowWhileIdle(any(), any(), any()) } returns Unit
        assertTrue(alarms.scheduleNext(strategy))
        verify(exactly = 0) { platformAlarms.setAlarmClock(any(), any()) }
    }

    @Test
    fun retryIsCappedAndCarriesIncrementedCount() {
        every { alarms.canScheduleExact() } returns true
        mockkConstructor(Intent::class)
        every { anyConstructed<Intent>().setClassName(any<Context>(), any()) } answers { self as Intent }
        every { anyConstructed<Intent>().putExtra(any<String>(), any<String>()) } answers { self as Intent }
        every { anyConstructed<Intent>().putExtra(any<String>(), any<Long>()) } answers { self as Intent }
        every { anyConstructed<Intent>().putExtra(any<String>(), any<Int>()) } answers { self as Intent }
        mockkStatic(PendingIntent::class)
        every { PendingIntent.getBroadcast(any(), any(), any(), any()) } returns mockk()

        assertTrue(alarms.scheduleRetry(strategy.id, 123L, retryCount = 0))
        verify(exactly = 1) {
            anyConstructed<Intent>().putExtra(ScheduleAlarmManager.EXTRA_RETRY_COUNT, 1)
        }
        assertTrue(alarms.scheduleRetry(strategy.id, 123L, retryCount = ScheduleAlarmManager.MAX_RETRY_COUNT - 1))
        assertFalse(alarms.scheduleRetry(strategy.id, 123L, retryCount = ScheduleAlarmManager.MAX_RETRY_COUNT))
        verify(exactly = 2) { platformAlarms.setExactAndAllowWhileIdle(any(), any(), any()) }
    }
}
