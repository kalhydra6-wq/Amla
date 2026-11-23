package com.amala.app

import android.content.Context
import java.io.File

/** Stores the last uncaught exception so it can be shown inside the app. */
object CrashLogger {
    private const val FILE_NAME = "last_crash.txt"

    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                File(context.filesDir, FILE_NAME).writeText(
                    buildString {
                        appendLine("Thread: ${thread.name}")
                        appendLine("Error: ${throwable.javaClass.name}: ${throwable.message}")
                        appendLine()
                        throwable.stackTrace.take(25).forEach { appendLine(it.toString()) }
                    },
                )
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun read(context: Context): String? = runCatching {
        File(context.filesDir, FILE_NAME).takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() }
    }.getOrNull()

    fun clear(context: Context) {
        runCatching { File(context.filesDir, FILE_NAME).delete() }
    }
}
