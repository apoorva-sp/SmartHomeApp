package com.example.myhome.network

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.*
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class WifiConnector(
    private val context: Context,
    private val tvStatus: TextView,
    private val progressBar: ProgressBar,
    private val onSuccess: (gatewayIp: String) -> Unit
) {
    private val PERMISSIONS_REQUEST_CODE = 100

    fun checkPermissionsAndConnect() {
        progressBar.visibility = View.VISIBLE
        tvStatus.text = "Checking permissions..."

        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE
        )

        val hasPermissions = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        if (!hasPermissions) {
            ActivityCompat.requestPermissions(
                context as android.app.Activity,
                permissions,
                PERMISSIONS_REQUEST_CODE
            )
        } else {
            suggestNetwork()
        }
    }

    fun handlePermissionsResult(
        requestCode: Int,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                suggestNetwork()
            } else {
                progressBar.visibility = View.GONE
                Toast.makeText(context, "Permissions required to connect to Wi-Fi", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun suggestNetwork() {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val networkSSID = "Apoorva5g" // TODO: Change to your SSID
        val networkPass = "apoorva2003sp@" // TODO: Change to your password


        tvStatus.text = "Adding Wi-Fi suggestion..."

        val suggestion = WifiNetworkSuggestion.Builder()
            .setSsid(networkSSID)
            .setWpa2Passphrase(networkPass)
            .build()

        val status = wifiManager.addNetworkSuggestions(listOf(suggestion))

        if (status != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
            tvStatus.text = "Failed to add suggestion. Status: $status"
            progressBar.visibility = View.GONE
            return
        }

        tvStatus.text = "Suggestion added. Waiting for connection..."

        val connectivityFilter = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeNetwork = cm.activeNetwork
                val capabilities = cm.getNetworkCapabilities(activeNetwork)

                if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    val gatewayIp = getGatewayIpAddress()
                    markWifiSetupDone()///////////////////to be removed at the end
                    progressBar.visibility = View.GONE
                    context.unregisterReceiver(this)

                    // Pass gateway to MainActivity callback
                    onSuccess(gatewayIp)
                }
            }
        }, connectivityFilter)
    }

    fun getGatewayIpAddress(): String {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcpInfo = wifiManager.dhcpInfo
        val gateway = dhcpInfo.gateway
        return String.format(
            "%d.%d.%d.%d",
            gateway and 0xff,
            gateway shr 8 and 0xff,
            gateway shr 16 and 0xff,
            gateway shr 24 and 0xff
        )
    }

    private fun markWifiSetupDone() {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("wifi_connected_once", true).apply()
    }
}
