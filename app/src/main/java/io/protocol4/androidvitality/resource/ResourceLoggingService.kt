package io.protocol4.androidvitality.resource

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class ResourceLoggingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var collector: ResourceCollector

    override fun onCreate() {
        super.onCreate()
        collector = ResourceCollector(this)
        createNotificationChannel()
        startForeground(1, createNotification())
        startLogging()
    }

    private fun startLogging() {
        serviceScope.launch {
            val hardware = collector.getHardwareInfo()
            Log.d("ResourceLoggingService", "Device: ${hardware.manufacturer} ${hardware.model}, Android ${hardware.androidVersion}")
            while (isActive) {
                val cpu = collector.getCpuUsage()
                val mem = collector.getMemoryInfo()
                Log.d("ResourceLoggingService", "CPU: ${cpu.totalUsage}%, Mem: ${mem.availableMemory} bytes avail")
                delay(60000)
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "resource_monitor",
            "Resource Monitor",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "resource_monitor")
            .setContentTitle("Resource Monitor Active")
            .setContentText("Logging system resources in background")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
