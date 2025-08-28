package com.example.myhome

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var wifiConnector: WifiConnector
    private lateinit var btnRetry: Button
    private lateinit var btnLogin: Button
    private var browserOpened = false

    private lateinit var requestQueue: RequestQueue
    private val handler = Handler(Looper.getMainLooper())
    private var pollingRunnable: Runnable? = null
    private var attempt = 0
    private var savedIp: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.textViewStatus)
        progressBar = findViewById(R.id.progressBar)
        btnRetry = findViewById(R.id.btnRetry)
        btnLogin = findViewById(R.id.btnLogin)

        requestQueue = Volley.newRequestQueue(this)
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        wifiConnector = WifiConnector(
            context = this,
            tvStatus = tvStatus,
            progressBar = progressBar
        ) { gatewayIp ->
            // Save IP for later
            prefs.edit().putString("gateway_ip", gatewayIp).apply()
            savedIp = gatewayIp
            startPolling(gatewayIp)
        }
        //call the function to connect to esp32 hub
        wifiConnector.checkPermissionsAndConnect()

        btnRetry.setOnClickListener {
            tvStatus.text = "Retrying polling..."
            val ip = prefs.getString("gateway_ip", null)  // local immutable val
            savedIp = ip
            if (ip != null) {
                startPolling(ip)
            } else {
                tvStatus.text = "No saved IP, reconnecting..."
                wifiConnector.checkPermissionsAndConnect()
            }
        }


        btnLogin.setOnClickListener {

            goToWifiLoginPage()
        }
    }

    private fun startPolling(ip: String) {
        stopPolling() // clear any old runnables
        attempt = 0
        progressBar.visibility = View.VISIBLE
        tvStatus.text = "Starting polling..."

        pollingRunnable = object : Runnable {
            override fun run() {
                if (attempt >= 12) { // 12 * 5s = 1 min
                    tvStatus.text = "Polling finished."
                    progressBar.visibility = View.GONE
                    return
                }

                val url = "http://$ip/"
                val request = StringRequest(
                    Request.Method.GET, url,
                    { response ->
                        tvStatus.text = "Attempt ${attempt + 1}: $response"
                    },
                    { error ->
                        tvStatus.text = "Error: ${error.message}"
                    }
                )

                requestQueue.add(request)

                attempt++
                handler.postDelayed(this, 5000) // repeat after 5s
            }
        }

        handler.post(pollingRunnable!!)
    }

    private fun stopPolling() {
        pollingRunnable?.let { handler.removeCallbacks(it) }
        progressBar.visibility = View.GONE
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        wifiConnector.handlePermissionsResult(requestCode, grantResults)
    }

    private fun goToWifiLoginPage() {
        val intent = Intent(this, WifiLogin::class.java)
        savedIp?.let { intent.putExtra("gatewayIp", it) } // ✅ only if not null
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPolling()
        requestQueue.cancelAll { true }
    }

}