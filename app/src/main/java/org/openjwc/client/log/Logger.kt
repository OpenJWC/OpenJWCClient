package org.openjwc.client.log

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

object Logger {
    private val counter = AtomicLong(0L)
    enum class Level {
        NONE,
        DEBUG,
        INFO,
        WARNING,
        ERROR,
        VERBOSE,
    }

    @Immutable
    data class LogEntry(
        val id: Long,
        val timestamp: Long,
        val level: Level,
        val tag: String,
        val message: String
    )

    private var isDebug = true
    private var maxEntries = 500

    val logHistory = mutableStateListOf<LogEntry>()

    fun init(debug: Boolean, maxEntries: Int) {
        this.isDebug = debug
        this.maxEntries = maxEntries
    }

    fun d(tag: String, message: String) {
        android.util.Log.d(tag, message)
        if (isDebug) append(Level.DEBUG, tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        android.util.Log.e(tag, message, throwable)
        append(Level.ERROR, tag, "$message ${throwable?.localizedMessage ?: ""}")
    }

    fun i(tag: String, message: String) {
        android.util.Log.i(tag, message)
        append(Level.INFO, tag, message)
    }

    fun w(tag: String, message: String) {
        android.util.Log.w(tag, message)
        append(Level.WARNING, tag, message)
    }

    fun v(tag: String, message: String) {
        android.util.Log.v(tag, message)
//        append(Level.INFO, tag, message)
    }
    fun log(tag: String, message: String, level: Level) {
        when (level) {
            Level.NONE -> {}
            Level.DEBUG -> d(tag, message)
            Level.ERROR -> e(tag, message)
            Level.INFO -> i(tag, message)
            Level.VERBOSE -> v(tag, message)
            Level.WARNING -> w(tag, message)
        }

    }


    private fun append(level: Level, tag: String, message: String) {
        val entry = LogEntry(
            id = counter.getAndIncrement(),
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message
        )

        kotlinx.coroutines.MainScope().launch {
            logHistory.add(0, entry)
            if (logHistory.size > maxEntries) {
                logHistory.removeAt(logHistory.size - 1)
            }
        }
    }

    fun clear(){
        logHistory.clear()
    }
}

