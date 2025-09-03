package com.example.myhome.UserInterface

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.myhome.R
import com.example.myhome.network.WifiConnector
import org.json.JSONObject

class WifiLoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var requestQueue: RequestQueue
    private lateinit var wifiConnector: WifiConnector
    private val handler = Handler(Looper.getMainLooper())
    private var pollingRunnable: Runnable? = null
    private var attempt = 0
    private var gatewayIp: String? = null
    private lateinit var prefs: android.content.SharedPreferences // ⭐ cache prefs
    private var savedIp: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.wifi_login)

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvStatus = findViewById(R.id.tvStatus)
        progressBar = findViewById(R.id.progressBar)

        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        requestQueue = Volley.newRequestQueue(this)

        wifiConnector = WifiConnector(
            context = this,
            tvStatus = tvStatus,
            progressBar = progressBar
        ) { gatewayIp ->
            // Save IP for later
            prefs.edit().putString("gateway_ip", gatewayIp).apply()
            savedIp = gatewayIp

        }
        //call the function to connect to esp32 hub
        wifiConnector.checkPermissionsAndConnect()

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty()) {
                tvStatus.text = "Please enter username"
                return@setOnClickListener
            }
            if(password.isEmpty()){
                tvStatus.text = "Please enter password"
                return@setOnClickListener
            }

            val ip = prefs.getString("gateway_ip", null)
            if (ip != null) {
                loginToDevice(ip, username, password)
            } else {
                tvStatus.text = "Please wait for the app to connect to hub"
            }
        }
    }

    private fun loginToDevice(ip: String, username: String, password: String) {
        progressBar.visibility = ProgressBar.VISIBLE
        tvStatus.text = "Logging in..."

        val url = "http://$ip/login"

        val jsonBody = JSONObject().apply {
            put("SSID", username)
            put("password", password)
        }

        val request = JsonObjectRequest(
            Request.Method.POST,
            url,
            jsonBody,
            Response.Listener { response ->
                progressBar.visibility = ProgressBar.GONE
                tvStatus.text = "Login successful!"


                // If ESP32 returns token or status, handle it here
                // Example: response.getString("token")

                goToHubStatusScreen()
            },
            Response.ErrorListener { error ->
                progressBar.visibility = ProgressBar.GONE
                tvStatus.text = "Login failed: ${error.message}"
            }
        )

        requestQueue.add(request)
    }

    private fun goToHubStatusScreen() {
        val intent = Intent(this, HubPollingActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        wifiConnector.handlePermissionsResult(requestCode, grantResults)
    }

}
