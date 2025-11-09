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
import com.android.volley.toolbox.StringRequest
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
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val hubIp = prefs.getString("hub_ip", null)

        if (hubIp.isNullOrEmpty()) {
            Toast.makeText(this, "Hub IP not found in preferences.", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "http://$hubIp/updateWifiCreds"

        // Send as application/x-www-form-urlencoded (NOT JSON)
        val requestQueue = Volley.newRequestQueue(this)

        val request = object : StringRequest(
            Method.POST, url,
            { response ->
                Log.d(TAG, "Wi-Fi update response: $response")
                Toast.makeText(this, "Wi-Fi updated successfully!", Toast.LENGTH_SHORT).show()
                finish()
            },
            { error ->
                Log.e(TAG, "Volley error: ${error.message}", error)
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "ssid" to ssid,
                    "password" to password
                )
            }

            override fun getBodyContentType(): String {
                return "application/x-www-form-urlencoded; charset=UTF-8"
            }
        }.apply {
            retryPolicy = DefaultRetryPolicy(5000, 1, 1.0f)
        }

        Toast.makeText(this, "Sending Wi-Fi credentials...", Toast.LENGTH_SHORT).show()
        requestQueue.add(request)
    }
}
