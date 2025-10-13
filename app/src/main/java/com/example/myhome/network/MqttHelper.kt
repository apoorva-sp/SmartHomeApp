package com.example.myhome.network

import android.content.Context
import android.util.Log
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5Connect
import java.util.UUID

object MqttHelper {

    private const val TAG = "MqttHelper"
    private const val MQTT_BROKER_URI = "13.204.47.177"
    private const val MQTT_BROKER_PORT = 1883
    private val CLIENT_ID = UUID.randomUUID().toString()
    private const val USERNAME = "capstone"
    private const val PASSWORD = "capstone"

    private var client: Mqtt5AsyncClient? = null
    private var pub: String = ""
    private var sub: String = ""
    private val listeners = mutableListOf<(String) -> Unit>()

    fun init(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val userId = prefs.getInt("userId", -1)
        pub = "hub/$userId/cmd"
        sub = "hub/$userId/data"
    }

    fun connect(onConnected: (() -> Unit)? = null, onFailure: ((Throwable?) -> Unit)? = null) {
        if (isConnected()) {
            Log.d(TAG, "Already connected.")
            onConnected?.invoke()
            return
        }

        client = MqttClient.builder()
            .useMqttVersion5()
            .identifier(CLIENT_ID)
            .serverHost(MQTT_BROKER_URI)
            .serverPort(MQTT_BROKER_PORT)
            .simpleAuth()
            .username(USERNAME)
            .password(PASSWORD.toByteArray())
            .applySimpleAuth()
            .automaticReconnectWithDefaultConfig()
            .addDisconnectedListener { context ->
                Log.e(TAG, "MQTT connection lost: ${context.cause.message}")
            }
            .buildAsync()

        client?.connect(
            Mqtt5Connect.builder()
                .cleanStart(true)
                .build()
        )
            ?.whenComplete { _, throwable ->
                if (throwable != null) {
                    Log.e(TAG, "❌ MQTT connection failed: ${throwable.message}")
                    onFailure?.invoke(throwable)
                } else {
                    Log.d(TAG, "✅ MQTT connected successfully")
                    onConnected?.invoke()
                    subscribeToTopic()
                }
            }
    }

    fun subscribe(onMessageReceived: (String) -> Unit) {
        if (!listeners.contains(onMessageReceived)) {
            listeners.add(onMessageReceived)
        }
        if (isConnected()) {
            subscribeToTopic()
        }
    }

    private fun subscribeToTopic(qos: Int = 1) {
        if (!isConnected()) return

        client?.subscribeWith()
            ?.topicFilter(sub)
            ?.qos(MqttQos.fromCode(qos) ?: MqttQos.AT_LEAST_ONCE)
            ?.callback { publish ->
                val msg = String(publish.payloadAsBytes)
                listeners.forEach { it(msg) }
                Log.d(TAG, "📩 Message arrived on ${publish.topic}: $msg")
            }
            ?.send()
            ?.whenComplete { _, throwable ->
                if (throwable != null) {
                    Log.e(TAG, "❌ Failed to subscribe to $sub: ${throwable.message}")
                } else {
                    Log.d(TAG, "✅ Subscribed to topic: $sub")
                }
            }
    }

    /**
     * Publishes a message to the default command topic with callbacks for success and error.
     */
    fun publish(
        message: String,
        qos: Int = 1,
        onSuccess: () -> Unit,
        onError: (Throwable?) -> Unit
    ) {
        if (!isConnected()) {
            val error = IllegalStateException("MQTT client is not connected.")
            Log.e(TAG, "Cannot publish. ${error.message}")
            onError(error)
            return
        }

        client?.publishWith()
            ?.topic(pub)
            ?.payload(message.toByteArray())
            ?.qos(MqttQos.fromCode(qos) ?: MqttQos.AT_LEAST_ONCE)
            ?.send()
            ?.whenComplete { _, throwable ->
                if (throwable != null) {
                    Log.e(TAG, "Publish failed to [$pub]: ${throwable.message}")
                    onError(throwable) // Report error via callback
                } else {
                    Log.d(TAG, "📤 Published to [$pub]: $message")
                    onSuccess() // Report success via callback
                }
            }
    }

    fun disconnect() {
        client?.disconnect()
        Log.d(TAG, "MQTT disconnected.")
    }

    fun isConnected(): Boolean = client?.state?.isConnected == true
}