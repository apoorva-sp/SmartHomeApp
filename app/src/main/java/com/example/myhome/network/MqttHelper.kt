package com.example.myhome.network

import android.content.Context
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

object MqttHelper {

    private const val TAG = "MqttHelper"
    private const val MQTT_BROKER_URI = "tcp://broker.hivemq.com:1883" // example public broker
    private const val CLIENT_ID = "MyHomeAppClient"
    private const val TOPIC = "myhome/hub/discovery"

    fun publishMessage(context: Context, message: String) {
        val client = MqttAndroidClient(context, MQTT_BROKER_URI, CLIENT_ID)
        val options = MqttConnectOptions()
        options.isCleanSession = true

        try {
            client.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "MQTT Connected")
                    val mqttMessage = MqttMessage(message.toByteArray())
                    mqttMessage.qos = 1
                    client.publish(TOPIC, mqttMessage)
                    Log.d(TAG, "Message published to $TOPIC: $message")
                    client.disconnect()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "MQTT Connection failed: ${exception?.message}")
                }
            })
        } catch (e: MqttException) {
            Log.e(TAG, "Error publishing MQTT message: ${e.message}")
        }
    }
}
