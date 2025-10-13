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

    // The client is now a HiveMQ Mqtt5AsyncClient
    private var client: Mqtt5AsyncClient? = null

    // Configuration loaded from SharedPreferences
    private var pub: String = ""
    private var sub: String = ""

    // This logic remains the same: a list to hold callbacks from different parts of your app
    private val listeners = mutableListOf<(String) -> Unit>()

    /**
     * Initializes the helper by reading user-specific topics from SharedPreferences.
     * The client itself is now created during the connect() call.
     */
    fun init(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val userId = prefs.getInt("userId", -1)
        pub = "hub/$userId/cmd"
        sub = "hub/$userId/data"
    }

    /**
     * Connects to the MQTT broker.
     * Creates the client on the first connection attempt.
     */
    // In MqttHelper.kt

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

        // Add .cleanStart(true) to the connect options
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

    /**
     * Registers a new listener to receive messages.
     * This allows multiple parts of your app (e.g., different Fragments) to listen.
     */
    fun subscribe(onMessageReceived: (String) -> Unit) {
        if (!listeners.contains(onMessageReceived)) {
            listeners.add(onMessageReceived)
        }
        // If already connected, ensure the subscription is active.
        if (isConnected()) {
            subscribeToTopic()
        }
    }

    /**
     * The actual MQTT subscription. This is called once to listen on the topic.
     * The received message is then distributed to all registered listeners.
     */
    private fun subscribeToTopic(qos: Int = 1) {
        if (!isConnected()) return

        client?.subscribeWith()
            ?.topicFilter(sub)
            ?.qos(MqttQos.fromCode(qos) ?: MqttQos.AT_LEAST_ONCE)
            ?.callback { publish ->
                // This is the new `messageArrived`.
                // It forwards the message to all registered listeners.
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
     * Publishes a message to the default command topic.
     */
    fun publish(message: String, qos: Int = 1) {
        if (!isConnected()) {
            Log.e(TAG, "MQTT not connected. Cannot publish.")
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
                } else {
                    // This is the equivalent of Paho's `deliveryComplete`
                    Log.d(TAG, "📤 Published to [$pub]: $message")
                }
            }
    }

    /**
     * Disconnects the client safely.
     */
    fun disconnect() {
        client?.disconnect()
        Log.d(TAG, "MQTT disconnected.")
    }

    /**
     * Checks the connection state directly from the client.
     */
    fun isConnected(): Boolean = client?.state?.isConnected == true
}