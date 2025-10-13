package com.example.myhome.network

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.myhome.R
import org.json.JSONObject

class MqttShutdownService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = "MqttShutdownChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "MQTT Shutdown Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("MyHome")
            .setContentText("Finalizing session...")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your app's icon
            .build()

        startForeground(1, notification)

        // --- MODIFIED LOGIC ---

        if (MqttHelper.isConnected()) {
            val json = JSONObject().apply { put("type", 5) }

            // Use the new publish method with callbacks
            MqttHelper.publish(
                message = json.toString(),
                onSuccess = {
                    Log.d("MqttShutdownService", "✅ Shutdown message published successfully.")
                    // Now that the task is done, disconnect and stop the service.
                    MqttHelper.disconnect()
                    stopSelf()
                },
                onError = { error ->
                    Log.e("MqttShutdownService", "❌ Failed to publish shutdown message: ${error?.message}")
                    // Even on error, disconnect and stop the service.
                    MqttHelper.disconnect()
                    stopSelf()
                }
            )
        } else {
            Log.w("MqttShutdownService", "MQTT not connected, cannot publish. Stopping service.")
            // If not connected, there's nothing to do, so just stop.
            stopSelf()
        }

        // The service will continue running until stopSelf() is called inside a callback.
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Not a bound service
    }
}