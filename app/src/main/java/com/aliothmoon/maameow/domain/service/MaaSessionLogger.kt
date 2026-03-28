package com.aliothmoon.maameow.domain.service

import com.aliothmoon.maameow.constant.LogConfig
import com.aliothmoon.maameow.data.config.MaaPathConfig
import com.aliothmoon.maameow.data.log.LogEntry
import com.aliothmoon.maameow.data.log.LogFileInfo
import com.aliothmoon.maameow.data.model.LogItem
import com.aliothmoon.maameow.data.model.LogLevel
import com.aliothmoon.maameow.utils.JsonUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicReference

class MaaSessionLogger(private val pathConfig: MaaPathConfig) {

    private companion object {
        private const val TAG = "MaaSessionLogger"
        private const val LOG_PREFIX = "meow_log_"
        private const val LOG_EXTENSION = ".log"
        private val FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    }

    private inner class ActiveSession(private val writer: BufferedWriter) {

        /** 固定轮询，跟随会话生命周期自动启停 */
        private val flushLoop: Job = scope.launch {
            while (true) {
                delay(LogConfig.LOG_FLUSH_INTERVAL_MS)
                flushPending(this@ActiveSession)
            }
        }

        fun writeEntry(logItem: LogItem) {
            try {
                val entry = LogEntry.Log(
                    time = logItem.timestampMillis,
                    level = logItem.level.name,
                    content = logItem.content
                )
                writeLine(json.encodeToString(entry))
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Failed to write log entry")
            }
        }

        fun writeLine(line: String) {
            writer.write(line)
            writer.newLine()
            writer.flush()
        }

        fun close(status: String? = null) {
            flushLoop.cancel()
            flushPending(this)
            if (status != null) {
                try {
                    val footer = LogEntry.Footer(
                        endTime = System.currentTimeMillis(), status = status
                    )
                    writeLine(json.encodeToString(footer))
                } catch (e: Exception) {
                    Timber.e(e, "$TAG: Failed to write footer")
                }
            }
            try {
                writer.close()
            } catch (e: Exception) {
                Timber.w(e, "$TAG: Error closing session writer")
            }
        }
    }

    private val dispatcher = Dispatchers.IO.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    // ---- 运行时日志（内存缓冲）----

    private val _logs = MutableStateFlow<List<LogItem>>(emptyList())
    val logs: StateFlow<List<LogItem>> = _logs.asStateFlow()
    private val pendingLogs = ArrayDeque<LogItem>()

    // ---- 文件持久化 ----

    private val json = JsonUtils.common
    private val sessionRef = AtomicReference<ActiveSession?>(null)

    private val logDir: File
        get() = File(pathConfig.debugDir, "gui").apply {
            if (!exists()) mkdirs()
        }

    // ========== 会话生命周期 ==========

    suspend fun startSession(taskNames: List<String>): Boolean = withContext(dispatcher) {
        try {
            sessionRef.getAndSet(null)?.close()
            val startTime = System.currentTimeMillis()
            val fileName = "${LOG_PREFIX}${
                Instant.ofEpochMilli(startTime).atZone(ZoneId.systemDefault())
                    .format(FILE_DATE_FORMAT)
            }_${taskNames.size}$LOG_EXTENSION"
            val file = File(logDir, fileName)
            val session = ActiveSession(BufferedWriter(FileWriter(file, true)))
            val header = LogEntry.Header(startTime = startTime, tasks = taskNames)
            session.writeLine(json.encodeToString(header))
            sessionRef.set(session)
            true
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Failed to start session")
            false
        }
    }

    fun endSession(status: String = "COMPLETED") {
        scope.launch { endSessionLocked(status) }
    }

    suspend fun endSessionAndWait(status: String = "COMPLETED") {
        withContext(dispatcher) { endSessionLocked(status) }
    }

    fun completeSession(
        status: String,
        finalLog: String? = null,
        level: LogLevel = LogLevel.ERROR
    ) {
        scope.launch { completeSessionLocked(status, finalLog, level) }
    }

    suspend fun completeSessionAndWait(
        status: String,
        finalLog: String? = null,
        level: LogLevel = LogLevel.ERROR
    ) {
        withContext(dispatcher) { completeSessionLocked(status, finalLog, level) }
    }

    // ========== 写入日志 ==========

    fun append(content: String, level: LogLevel = LogLevel.MESSAGE, tooltip: String? = null) {
        append(LogItem(content = content, level = level, tooltip = tooltip))
    }

    fun append(logItem: LogItem) {
        scope.launch {
            pendingLogs.addLast(logItem)
        }
    }

    suspend fun appendAndWait(
        content: String,
        level: LogLevel = LogLevel.MESSAGE,
        tooltip: String? = null
    ) {
        appendAndWait(LogItem(content = content, level = level, tooltip = tooltip))
    }

    suspend fun appendAndWait(logItem: LogItem) {
        withContext(dispatcher) {
            sessionRef.get()?.let { flushPending(it) }
            appendDirectLocked(logItem)
        }
    }

    /** 仅写入日志文件，不在运行时 UI 中展示 */
    fun appendToFileOnly(content: String, level: LogLevel = LogLevel.TRACE) {
        val logItem = LogItem(content = content, level = level)
        scope.launch {
            sessionRef.get()?.writeEntry(logItem)
        }
    }


    fun clearRuntimeLogs() {
        scope.launch {
            sessionRef.get()?.let { flushPending(it) }
            _logs.value = emptyList()
        }
    }


    suspend fun getLogFiles(): List<LogFileInfo> = withContext(Dispatchers.IO) {
        try {
            logDir.listFiles { file ->
                file.isFile && file.name.startsWith(LOG_PREFIX) && file.name.endsWith(LOG_EXTENSION)
            }?.map { file ->
                doParseLogFileInfo(file)
            }?.sortedByDescending { it.startTime } ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Failed to get log files")
            emptyList()
        }
    }

    suspend fun readLogFile(fileName: String): List<LogEntry> = withContext(Dispatchers.IO) {
        try {
            val file = File(logDir, fileName)
            if (!file.exists()) return@withContext emptyList()

            file.readLines().mapNotNull { line ->
                try {
                    if (line.isBlank()) return@mapNotNull null
                    json.decodeFromString<LogEntry>(line)
                } catch (e: Exception) {
                    Timber.w("$TAG: Failed to parse line: $line")
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Failed to read log file")
            emptyList()
        }
    }

    suspend fun deleteLogFile(fileName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(logDir, fileName)
            if (file.exists()) {
                file.delete().also {
                    Timber.i("$TAG: Deleted log file: $fileName, result: $it")
                }
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Failed to delete log file")
            false
        }
    }

    suspend fun cleanupOldLogs(daysToKeep: Int = LogConfig.MAX_TASK_LOG_DAYS): Int =
        withContext(Dispatchers.IO) {
            try {
                val cutoffTime = System.currentTimeMillis() - daysToKeep * 24 * 60 * 60 * 1000L
                var deletedCount = 0

                logDir.listFiles { file ->
                    file.isFile && file.name.startsWith(LOG_PREFIX) && file.name.endsWith(
                        LOG_EXTENSION
                    )
                }?.forEach { file ->
                    val info = doParseLogFileInfo(file)
                    if (info.startTime < cutoffTime) {
                        if (file.delete()) deletedCount++
                    }
                }

                Timber.i("$TAG: Cleaned up $deletedCount old log files")
                deletedCount
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Failed to cleanup old logs")
                0
            }
        }

    // ========== 内部实现 ==========

    /** 排空 pending → 更新 UI 缓冲 + 委托 Session 落盘 */
    private fun flushPending(session: ActiveSession) {
        if (pendingLogs.isEmpty()) return
        val batch = pendingLogs.toList()
        pendingLogs.clear()
        emitToUI(batch)
        batch.forEach { session.writeEntry(it) }
    }

    /** 批量合并到 _logs StateFlow */
    private fun emitToUI(batch: List<LogItem>) {
        val current = _logs.value
        val merged = if (current.isEmpty()) batch else current + batch
        _logs.value = if (merged.size > LogConfig.MAX_RUNTIME_LOG_COUNT) {
            merged.takeLast(LogConfig.MAX_RUNTIME_LOG_COUNT)
        } else {
            merged
        }
    }

    /** 直接追加单条到 UI + 文件，绕过 pending 缓冲 */
    private fun appendDirectLocked(logItem: LogItem) {
        emitToUI(listOf(logItem))
        sessionRef.get()?.writeEntry(logItem)
    }

    private suspend fun completeSessionLocked(
        status: String,
        finalLog: String?,
        level: LogLevel
    ) {
        if (sessionRef.get() == null) return
        finalLog?.let {
            appendDirectLocked(LogItem(content = it, level = level))
        }
        endSessionLocked(status)
    }

    private suspend fun endSessionLocked(status: String) {
        val s = sessionRef.getAndSet(null) ?: return
        s.close(status)
        Timber.i("$TAG: Session ended with status: $status")
    }

    // ---- 文件名解析 ----

    /**
     * 解析文件名获取 LogFileInfo
     * 文件名格式：meow_log_20260121_143052_3.log
     */
    private fun doParseLogFileInfo(file: File): LogFileInfo {
        val fileName = file.name
        var startTime = file.lastModified()

        val taskCount = try {
            val name = fileName.removePrefix(LOG_PREFIX).removeSuffix(LOG_EXTENSION)
            val parts = name.split("_")
            if (parts.size >= 3) {
                val dateStr = "${parts[0]}_${parts[1]}"
                startTime =
                    LocalDateTime.parse(dateStr, FILE_DATE_FORMAT)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                parts.getOrNull(2)?.toIntOrNull() ?: 0
            } else {
                0
            }
        } catch (e: Exception) {
            Timber.w(e, "$TAG: Failed to parse filename: $fileName")
            0
        }

        return LogFileInfo(
            fileName = fileName,
            filePath = file.absolutePath,
            startTime = startTime,
            fileSize = file.length(),
            taskCount = taskCount
        )
    }
}
