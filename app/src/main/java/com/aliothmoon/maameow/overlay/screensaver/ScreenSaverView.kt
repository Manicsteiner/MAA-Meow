package com.aliothmoon.maameow.overlay.screensaver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliothmoon.maameow.domain.service.MaaSessionLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
fun ScreenSaverView(
    sessionLogger: MaaSessionLogger,
    onUnlock: () -> Unit
) {
    val logs by sessionLogger.logs.collectAsStateWithLifecycle()
    val latestLog = logs.lastOrNull()?.content ?: "等待任务开始..."

    val batteryState = rememberBatteryState()
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    // 限制左右移动幅度（最大屏幕宽度的 1/8）
    val maxOffsetX = configuration.screenWidthDp / 8
    // 上下移动幅度较大（最大屏幕高度的 1/4）
    val maxOffsetY = configuration.screenHeightDp / 4

    var burnInOffsetX by remember { mutableIntStateOf(0) }
    var burnInOffsetY by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            while (true) {
                currentTime = LocalTime.now()
                delay(30_000L) // 首次显示居中，30秒后才开始漂移并更新时间
                // 水平漂移极小，垂直漂移较大
                burnInOffsetX = if (maxOffsetX > 0) (-maxOffsetX..maxOffsetX).random() else 0
                burnInOffsetY = if (maxOffsetY > 0) (-maxOffsetY..maxOffsetY).random() else 0
            }
        }
    }

    val density = LocalDensity.current
    val dragThresholdPx = with(density) { (screenHeight / 4).toPx() }
    val offsetY = remember { Animatable(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // 纯黑背景防止OLED发光
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (abs(offsetY.value) > dragThresholdPx) {
                            onUnlock()
                        } else {
                            scope.launch { offsetY.animateTo(0f) }
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            val newY = (offsetY.value + dragAmount).coerceAtMost(0f)
                            offsetY.snapTo(newY)
                        }
                    }
                )
            }
            .offset { IntOffset(0, offsetY.value.toInt()) }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset { IntOffset(burnInOffsetX, burnInOffsetY) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = currentTime.format(timeFormatter),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Light
                ),
                color = Color.White.copy(alpha = 0.5f)
            )

            Text(
                text = "${if (batteryState.isCharging) "⚡" else "🔋"} ${batteryState.level}%",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.4f)
            )

            Text(
                text = latestLog,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }

        Text(
            text = "向上滑动解锁",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.2f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        )
    }
}

data class BatteryState(val level: Int = 100, val isCharging: Boolean = false)

@Composable
fun rememberBatteryState(): BatteryState {
    val context = LocalContext.current
    var state by remember { mutableStateOf(BatteryState()) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL
                    if (scale > 0) {
                        state = BatteryState(level * 100 / scale, isCharging)
                    }
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        // Initial fetch
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (intent != null) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            if (scale > 0) {
                state = BatteryState(level * 100 / scale, isCharging)
            }
        }

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }
    return state
}
