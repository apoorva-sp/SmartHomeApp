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

        if (hub_ip == null) {
            lifecycleScope.launch {
                var reply: String? = null

                // Try discovery 3 times
                repeat(3) { attempt ->
                    reply = BroadcastHelper.discoverDevice()
                    if (reply != null) {
                        Log.d("IpDiscoveryActivity", "Got reply on attempt ${attempt + 1}")
                        return@repeat
                    } else {
                        Log.w("IpDiscoveryActivity", "No reply, attempt ${attempt + 1}")
                        delay(1000) // wait 1s before retry
                    }
                }

                if (reply != null) {
                    try {
                        val json = JSONObject(reply)
                        val ip = json.getString("ip")

                        with(prefs.edit()) {
                            putString("hub_ip", ip)
                            apply()
                        }

                        resultText.text = "✅ Hub found at: $ip"
                        Log.d("broadcasthub", "Response: '$ip'")
                        OpenAppliancePage()
                    } catch (e: Exception) {
                        Log.e("IpDiscoveryActivity", "JSON parse error: ${e.message}\nReply: $reply")
                        resultText.text = "⚠️ Invalid response"
                    }
                } else {
                    resultText.text = "❌ No reply after 3 attempts"
                }

                progressBar.visibility = View.GONE
            }
        } else {
            lifecycleScope.launch {
                var success = false
                for (attempt in 1..3) {
                    val isAlive = BroadcastHelper.verifyHubIp(hub_ip)
                    if (isAlive) {
                        success = true
                        break
                    }
                }

                progressBar.visibility = View.GONE

                if (success) {
                    resultText.text = "✅ Hub is alive at: $hub_ip"
                    OpenAppliancePage()
                } else {
                    resultText.text = "❌ Hub not responding, you are connected to a different network"
                    Toast.makeText(
                        this@IpDiscoveryActivity,
                        "Hub is in a different network",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun OpenAppliancePage() {
        val intent = Intent(this, AppliancesActivity::class.java)
        startActivity(intent)
        finish()
    }
}
