package com.example.myhome.network // Or your preferred package

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.myhome.R // Make sure to import your R file
import org.json.JSONObject

class MqttShutdownService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Create a notification channel (required for Android 8+)
        val channelId = "MqttShutdownChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "MQTT Shutdown Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        // Create the notification for the foreground service
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("MyHome")
            .setContentText("Finalizing session...")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your app's icon
            .build()

        // Start the service in the foreground
        startForeground(1, notification)

        // Perform the MQTT publish operation
        if (MqttHelper.isConnected()) {
            val json = JSONObject().apply { put("type", 5) }
            MqttHelper.publish(json.toString())
            Log.d("MqttShutdownService", "Shutdown message published.")
        } else {
            Log.e("MqttShutdownService", "MQTT not connected, cannot publish shutdown message.")
        }

        // Stop the service once the task is done
        stopSelf()

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Not a bound service
    }
}