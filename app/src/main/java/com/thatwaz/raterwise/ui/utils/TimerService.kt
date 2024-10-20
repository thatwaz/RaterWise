package com.thatwaz.raterwise.ui.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// File: utils/TimerService.kt


class TimerService : Service() {

    interface TimerCallback {
        fun onTimeUpdate(elapsedTime: Long, clockInTime: String)
        fun onTaskTimeUpdate(taskSeconds: Long)
    }

    private val binder = TimerBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var elapsedTime = 0L
    private var taskSeconds = 0L // Track task seconds
    private var clockInTime: String? = null
    private var isTimerRunning = false
    private var isTaskTimerRunning = false // Track task timer state

    var timerCallback: TimerCallback? = null

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent?): IBinder = binder


    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, getNotification("Tracking time..."))
    }

    // Start regular timer
    fun startTimer(clockInTime: String) {
        if (isTimerRunning) return
        this.clockInTime = clockInTime
        isTimerRunning = true

        serviceScope.launch {
            while (isTimerRunning) {
                delay(1000L) // Update every second
                elapsedTime++
                timerCallback?.onTimeUpdate(elapsedTime, clockInTime) // Notify callback
            }
        }
    }

    // Start task-specific timer
    fun startTaskTimer() {
        if (isTaskTimerRunning) return
        isTaskTimerRunning = true

        serviceScope.launch {
            while (isTaskTimerRunning) {
                delay(1000L) // Update task timer every second
                taskSeconds++
                timerCallback?.onTaskTimeUpdate(taskSeconds) // Notify callback
            }
        }
    }

    fun stopTaskTimer() {
        isTaskTimerRunning = false
    }

    fun stopTimer() {
        isTimerRunning = false
        elapsedTime = 0L
        timerCallback?.onTimeUpdate(elapsedTime, clockInTime ?: "") // Notify callback
        stopForeground(true)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "timer_channel",
                "Time Tracking Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun getNotification(text: String): Notification {
        return NotificationCompat.Builder(this, "timer_channel")
            .setContentTitle("Time Tracking Service")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}



//class TimerService : Service() {
//
//    interface TimerCallback {
//        fun onTimeUpdate(elapsedTime: Long, clockInTime: String)
//    }
//
//
//    private val binder = TimerBinder()
//    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
//
//    private var elapsedTime = 0L
//    private var clockInTime: String? = null
//    private var isTimerRunning = false
//
//    var timerCallback: TimerCallback? = null
//
//    inner class TimerBinder : Binder() {
//        fun getService(): TimerService = this@TimerService
//    }
//
//    override fun onBind(intent: Intent?): IBinder = binder
//
//    override fun onCreate() {
//        super.onCreate()
//        createNotificationChannel()
//        startForeground(1, getNotification("Tracking time..."))
//    }
//
//    fun startTimer(clockInTime: String) {
//        if (isTimerRunning) return
//        this.clockInTime = clockInTime
//        isTimerRunning = true
//
//        serviceScope.launch {
//            while (isTimerRunning) {
//                delay(1000L) // Update every second
//                elapsedTime++
//                timerCallback?.onTimeUpdate(elapsedTime, clockInTime) // Notify callback
//            }
//        }
//    }
//
//    fun stopTimer() {
//        isTimerRunning = false
//        elapsedTime = 0L
//        timerCallback?.onTimeUpdate(elapsedTime, clockInTime ?: "") // Notify callback
//        stopForeground(true)
//    }
//
//    private fun createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                "timer_channel",
//                "Time Tracking Service",
//                NotificationManager.IMPORTANCE_LOW
//            )
//            val manager = getSystemService(NotificationManager::class.java)
//            manager?.createNotificationChannel(channel)
//        }
//    }
//
//    private fun getNotification(text: String): Notification {
//        return NotificationCompat.Builder(this, "timer_channel")
//            .setContentTitle("Time Tracking Service")
//            .setContentText(text)
//            .setSmallIcon(android.R.drawable.ic_notification_overlay)
//            .build()
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        serviceScope.cancel()
//    }
//}



//class TimerService : Service() {
//
//    private val binder = TimerBinder()
//    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
//
//    private var elapsedTime = 0L // Time in seconds
//    private var isTimerRunning = false
//    private var clockInTime: String = ""
//
//    interface TimerCallback {
//        fun onTimeUpdate(elapsedTime: Long, clockInTime: String)
//    }
//
//    var timerCallback: TimerCallback? = null
//
//    inner class TimerBinder : Binder() {
//        fun getService(): TimerService = this@TimerService
//    }
//
//    override fun onBind(intent: Intent?): IBinder {
//        return binder
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//        createNotificationChannel()
//        startForeground(1, getNotification("Tracking time..."))
//    }
//
//    fun startTimer(clockInTime: String) {
//        if (isTimerRunning) return
//        isTimerRunning = true
//        this.clockInTime = clockInTime
//
//        serviceScope.launch {
//            while (isTimerRunning) {
//                delay(1000L)
//                elapsedTime++
//                timerCallback?.onTimeUpdate(elapsedTime, clockInTime)
//            }
//        }
//    }
//
//    fun stopTimer() {
//        isTimerRunning = false
//        elapsedTime = 0L
//        timerCallback?.onTimeUpdate(elapsedTime, "")
//        stopForeground(true)
//    }
//
//    fun isServiceRunning(): Boolean {
//        return isTimerRunning
//    }
//
//    fun getElapsedTime(): Long {
//        return elapsedTime
//    }
//
//    fun getClockInTime(): String {
//        return clockInTime
//    }
//
//    private fun getNotification(text: String): Notification {
//        return NotificationCompat.Builder(this, "timer_channel")
//            .setContentTitle("Time Tracking Service")
//            .setContentText(text)
//            .setSmallIcon(android.R.drawable.ic_notification_overlay)
//            .build()
//    }
//
//    private fun createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                "timer_channel",
//                "Time Tracking Service",
//                NotificationManager.IMPORTANCE_LOW
//            )
//            val manager = getSystemService(NotificationManager::class.java)
//            manager?.createNotificationChannel(channel)
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        serviceScope.cancel()
//    }
//}



//class TimerService : Service() {
//    private val binder = TimerBinder()
//
//    // A simple binder class for the service
//    inner class TimerBinder : Binder() {
//        fun getService(): TimerService = this@TimerService
//    }
//
//    override fun onBind(intent: Intent?): IBinder? {
//        return binder
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        startForegroundService()
//        return START_NOT_STICKY
//    }
//
//    private fun startForegroundService() {
//        val notification = createNotification()
//        startForeground(1, notification)
//    }
//
//    private fun createNotification(): Notification {
//        val channelId = "TimerServiceChannel"
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                channelId,
//                "Timer Service Channel",
//                NotificationManager.IMPORTANCE_DEFAULT
//            )
//            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
//        }
//
//        return NotificationCompat.Builder(this, channelId)
//            .setContentTitle("RaterWise Timer Active")
//            .setContentText("Your task timer is running.")
//            .setSmallIcon(R.drawable.ic_clock)
//            .build()
//    }
//}
