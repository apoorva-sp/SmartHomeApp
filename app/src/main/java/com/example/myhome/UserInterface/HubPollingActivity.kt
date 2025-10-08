package com.example.myhome.UserInterface

import android.content.Context
import android.content.Intent
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
import com.example.myhome.R

class HubPollingActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnRetry: Button
    private lateinit var btnLogin: Button

    private lateinit var requestQueue: RequestQueue
    private val handler = Handler(Looper.getMainLooper())
    private var pollingRunnable: Runnable? = null
    private var attempt = 0
    private lateinit var prefs: android.content.SharedPreferences // ⭐ cache prefs


    private var gatewayIp: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hub_polling)

        progressBar = findViewById(R.id.progressBar)
        tvStatus = findViewById(R.id.textViewStatus)
        btnLogin = findViewById(R.id.btnLogin)
        btnRetry = findViewById(R.id.btnRetry)

        requestQueue = Volley.newRequestQueue(this)

        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        gatewayIp = prefs.getString("gateway_ip", null)

        btnRetry.setOnClickListener {
            tvStatus.text = "Retrying polling..."
            gatewayIp?.let { ip -> startPolling(ip) }
                ?: run { tvStatus.text = "❌ No saved hub IP found" }
        }

        btnLogin.setOnClickListener {
            showNodeConfirmationDialog()
        }

        // Optionally: auto-start polling when activity loads
        gatewayIp?.let { startPolling(it) }
    }

    private fun showNodeConfirmationDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Confirm")
        builder.setMessage("Are all nodes connected?")

        builder.setPositiveButton("Yes") { dialog, _ ->
            dialog.dismiss()
            prefs.edit()
                .putBoolean("is_esp32_setup_done", true)
                .apply()
            OpenIpdiscoveryPage()  // ✅ go to next intent
        }

        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
            // ✅ do nothing, stay on the same page
        }

        builder.create().show()
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

                        // ✅ Example: stop polling early if response looks good
                        if (response.contains("ready", ignoreCase = true)) {
                            stopPolling()
                            tvStatus.text = "✅ Hub is ready"
                        }
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

    private fun OpenIpdiscoveryPage() {
        val intent = Intent(this, IpDiscoveryActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPolling()
        requestQueue.cancelAll { true }
    }
}
