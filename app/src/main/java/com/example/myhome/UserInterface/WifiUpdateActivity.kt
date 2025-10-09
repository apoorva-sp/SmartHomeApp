package com.example.myhome.UserInterface

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.myhome.R
import org.json.JSONObject
import android.util.Log
import com.example.myhome.utils.NavigationBarActivity

class WifiUpdateActivity : NavigationBarActivity() {

    private val TAG = "WifiUpdateActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = findViewById<android.widget.FrameLayout>(R.id.container)
        layoutInflater.inflate(R.layout.activity_update_wifi, container, true)

        findViewById<TextView>(R.id.pageTitle).text = "Wifi Update"

        val ssidField = findViewById<EditText>(R.id.editSsid)
        val passwordField = findViewById<EditText>(R.id.editWifiPassword)
        val btnUpdate = findViewById<Button>(R.id.btnUpdateWifi)

        btnUpdate.setOnClickListener {
            val ssid = ssidField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (ssid.isEmpty() || password.length < 8) {
                Toast.makeText(this, "Please enter valid Wi-Fi details.", Toast.LENGTH_SHORT).show()
            } else {
                sendWifiCredentials(ssid, password)
            }
        }
    }

    private fun sendWifiCredentials(ssid: String, password: String) {
        // Get hub IP from SharedPreferences
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val hubIp = prefs.getString("hub_ip", null)

        if (hubIp.isNullOrEmpty()) {
            Toast.makeText(this, "Hub IP not found in preferences.", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "http://$hubIp/login"

        val jsonBody = JSONObject().apply {
            put("ssid", ssid)
            put("password", password)
        }

        val requestQueue = Volley.newRequestQueue(this)

        val request = JsonObjectRequest(
            Request.Method.POST,
            url,
            jsonBody,
            { response ->
                val code = response.optInt("code", -1)
                val message = response.optString("message", "Unknown error")
                if (code == 0) {

                    Toast.makeText(this, "Wi-Fi updated successfully!", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Wi-Fi update response: $response")
                } else {
                    Toast.makeText(this, "Failed: $message", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Wi-Fi update failed: $response")
                }
            },
            { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Volley error: ${error.message}", error)
            }
        ).apply {
            retryPolicy = DefaultRetryPolicy(5000, 1, 1.0f)
        }

        // Show feedback
        Toast.makeText(this, "Sending Wi-Fi credentials...", Toast.LENGTH_SHORT).show()
        requestQueue.add(request)
    }
}
