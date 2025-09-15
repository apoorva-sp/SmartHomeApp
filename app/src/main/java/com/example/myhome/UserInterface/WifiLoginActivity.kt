package com.example.myhome.UserInterface

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.RequestQueue
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
    private lateinit var tvSkip :TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.wifi_login)

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvStatus = findViewById(R.id.tvStatus)
        progressBar = findViewById(R.id.progressBar)
        tvSkip = findViewById(R.id.tvSkip)

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
            tvStatus.text = "mobile connected to hub"

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
            if (password.isEmpty()) {
                tvStatus.text = "Please enter password"
                return@setOnClickListener
            }

            val ip = prefs.getString("gateway_ip", null)
            if (ip != null) {
//                showDeviceCountDialog(ip, username, password) // 👈 Show dialog first
                    loginToDevice(ip,username,password);
            } else {
                tvStatus.text = "Please wait for the app to connect to hub"
            }
        }

        tvSkip.setOnClickListener {
            if(savedIp ==null){
                Toast.makeText(this,"Wait for the device to connect to hub",Toast.LENGTH_SHORT)
            }
            else{

                goToHubStatusScreen()
            }

        }

    }

    private fun loginToDevice(ip: String, username: String, password: String) {
        progressBar.visibility = ProgressBar.VISIBLE
        tvStatus.text = "Logging in..."

        val url = "http://$ip/login"
        val userid = prefs.getInt("userId", -1)

        val jsonBody = JSONObject().apply {
            put("ssid", username)
            put("password", password)
            put ("user_id",userid)
//            put("count",count)
        }

        val request = object : StringRequest(
            Method.POST,
            url,
            { response ->
                progressBar.visibility = ProgressBar.GONE
                val json = JSONObject(response)
                val status = json.optInt("code")
                val message = json.optString("message")
                if (status == 0) {
                    tvStatus.text = "Login successful!"
                    goToHubStatusScreen()
                } else {
                    tvStatus.text = "Login failed: $message"
                }
            },
            { error ->
                progressBar.visibility = ProgressBar.GONE
                val body = error.networkResponse?.data?.toString(Charsets.UTF_8)
                tvStatus.text = "Login failed HTTP ERROR: $body"
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["ssid"] = username
                params["password"] = password
                params["user_id"] = userid.toString()  // if needed
                return params
            }
        }
        requestQueue.add(request)
    }

    private fun showDeviceCountDialog(ip: String, username: String, password: String) {
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = "Enter number of devices (1-15)"
        }

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Number of Devices")
            .setMessage("Please enter how many devices you want to connect (1-15)")
            .setView(input)
            .setPositiveButton("Confirm", null) // We'll override later
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val countText = input.text.toString().trim()
                val count = countText.toIntOrNull()

                if (count == null || count !in 1..15) {
                    input.error = "Enter a valid number between 1 and 15"
                } else {
                    dialog.dismiss()
//                    loginToDevice(ip, username, password, count)
                }
            }
        }

        dialog.show()
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        wifiConnector.handlePermissionsResult(requestCode, grantResults)
    }

}
