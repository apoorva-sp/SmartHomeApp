package com.example.myhome

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class WifiLogin : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var requestQueue: RequestQueue

    private var gatewayIp: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.wifi_login)

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvStatus = findViewById(R.id.tvStatus)
        progressBar = findViewById(R.id.progressBar)

        requestQueue = Volley.newRequestQueue(this)
        gatewayIp = intent.getStringExtra("gatewayIp")

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

            gatewayIp?.let { ip ->
                loginToDevice(ip, username, password)
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

                goToHomeScreen()
            },
            Response.ErrorListener { error ->
                progressBar.visibility = ProgressBar.GONE
                tvStatus.text = "Login failed: ${error.message}"
            }
        )

        requestQueue.add(request)
    }

    private fun goToHomeScreen() {
        val intent = Intent(this, Appliances::class.java)
        startActivity(intent)
        finish()
    }
}
