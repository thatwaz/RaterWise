package com.thatwaz.raterwise.ui.utils

class Timer {
    private var isRunning = false

    fun start(onTick: () -> Unit) {
        isRunning = true
        Thread {
            while (isRunning) {
                Thread.sleep(60000) // 1 minute
                if (isRunning) onTick()
            }
        }.start()
    }

    fun cancel() {
        isRunning = false
    }
}

fun getCurrentTimeFormatted(): String {
    val currentTime = System.currentTimeMillis()
    val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return formatter.format(currentTime)
}
