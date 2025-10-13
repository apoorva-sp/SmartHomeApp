package com.example.myhome.UserInterface

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myhome.R
import com.example.myhome.network.BroadcastHelper
import com.example.myhome.network.MqttHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

class IpDiscoveryActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var resultText: TextView
    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.broadcast_ip)

        progressBar = findViewById(R.id.progressBar)
        resultText = findViewById(R.id.resultText)
        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        progressBar.visibility = View.VISIBLE
        resultText.text = "🔍 Searching for hub..."

        val hub_ip = prefs.getString("hub_ip", null)

        lifecycleScope.launch {
            if (hub_ip == null) {
                // No saved hub, discover via broadcast
                val ip = discoverHubWithRetries()
                handleDiscoveryResult(ip)

            } else {
                // Try unicast first
                val isAlive = verifyHubWithRetries(hub_ip)
                if (isAlive) {
                    progressBar.visibility = View.GONE
                    resultText.text = "✅ Hub is alive at: $hub_ip"
                    prefs.edit()
                        .putBoolean("LAN", true).apply()
                    OpenAppliancePage()
                } else {
                    Log.w("IpDiscoveryActivity", "Unicast verification failed, falling back to broadcast")
                    val ip = discoverHubWithRetries()
                    handleDiscoveryResult(ip)
                    if (ip.isNullOrEmpty()) {
                        connectToMqtt()
                    }
                }
            }
        }
    }

    private suspend fun discoverHubWithRetries(): String? {
        var reply: String? = null
        repeat(3) { attempt ->
            reply = BroadcastHelper.discoverDevice()
            if (reply != null) {
                Log.d("IpDiscoveryActivity", "Got reply on attempt ${attempt + 1}")
                return reply
            } else {
                Log.w("IpDiscoveryActivity", "No reply, attempt ${attempt + 1}")
                delay(1000)
            }
        }
        return reply
    }

    private suspend fun verifyHubWithRetries(ip: String): Boolean {
        repeat(3) { attempt ->
            val isAlive = BroadcastHelper.verifyHubIp(ip)
            if (isAlive) {
                Log.d("IpDiscoveryActivity", "Hub alive on attempt ${attempt + 1}")
                return true
            } else {
                Log.w("IpDiscoveryActivity", "Hub not responding, attempt ${attempt + 1}")
                delay(500)
            }
        }
        return false
    }

    private fun handleDiscoveryResult(reply: String?) {
        progressBar.visibility = View.GONE
        if (reply != null) {
            try {
                val json = JSONObject(reply)
                val ip = json.getString("ip")

                with(prefs.edit()) {
                    putString("hub_ip", ip)
                    apply()
                }
                prefs.edit()
                    .putBoolean("LAN", true).apply()

                resultText.text = "✅ Hub found at: $ip"
                Log.d("broadcasthub", "Response: '$ip'")
                OpenAppliancePage()
            } catch (e: Exception) {
                Log.e("IpDiscoveryActivity", "JSON parse error: ${e.message}\nReply: $reply")
                resultText.text = "⚠️ Invalid response"
            }
        } else {
            resultText.text = "❌ No reply after 3 attempts"
            Toast.makeText(
                this@IpDiscoveryActivity,
                "Hub not found in network",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun connectToMqtt() {
        MqttHelper.init(this@IpDiscoveryActivity)
        MqttHelper.connect(
            onConnected = {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    resultText.text = "✅ Connected to hub via MQTT"
                    prefs.edit()
                        .putBoolean("LAN", false).apply()
                    val json = JSONObject().apply {
                        put("type", 1)
                    }
                    MqttHelper.publish(json.toString(), qos = 1)//say hello
                    OpenAppliancePage()
                }
            },
            onFailure = { error ->
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    resultText.text = "❌ MQTT connection failed: ${error?.message}"
                }
            }
        )
    }


    private fun OpenAppliancePage() {
        val intent = Intent(this, AppliancesActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
