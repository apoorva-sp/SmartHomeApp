package com.example.myhome

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var wifiConnector: WifiConnector
    private var browserOpened = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.textViewStatus)
        progressBar = findViewById(R.id.progressBar)

        wifiConnector = WifiConnector(
            context = this,
            tvStatus = tvStatus,
            progressBar = progressBar
        ) { gatewayIp ->
            // Open browser after connection
            openBrowser(gatewayIp)
        }

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val alreadyConnected = prefs.getBoolean("wifi_connected_once", false)

        if (!alreadyConnected) {
            wifiConnector.checkPermissionsAndConnect()
        } else {
            goToLoginPage()
        }
    }

    private fun openBrowser(gatewayIp: String) {
        val loginUrl = "http://$gatewayIp/login"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(loginUrl))
        startActivity(intent)
        browserOpened = true
    }

    private fun goToLoginPage() {
        val intent = Intent(this, LoginSignup::class.java)
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

    override fun onResume() {
        super.onResume()
        if (browserOpened) {
            goToLoginPage()
            browserOpened = false
        }
    }
}
