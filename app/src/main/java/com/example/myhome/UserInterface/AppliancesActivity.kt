package com.example.myhome.UserInterface

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.myhome.Beans.Device
import com.example.myhome.R
import com.example.myhome.network.UdpPortManager
import androidx.core.view.GravityCompat
import com.example.myhome.utils.ExitUtils

import com.google.android.material.navigation.NavigationView
import org.json.JSONObject

class AppliancesActivity : AppCompatActivity() {

    private lateinit var gridLayout: GridLayout
    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var menuButton: ImageButton
    private val devices = mutableListOf<Device>()

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var drawerToggle: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.appliances)

        gridLayout = findViewById(R.id.gridLayoutDevices)
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        drawerToggle = findViewById(R.id.drawerToggle)

        fetchDevices()
        setupDrawer()

        // UDP listener
        UdpPortManager.messages.observe(this) { (msg, sender) ->
            Log.d("UDP", "📩 Raw UDP message: '$msg' from ${sender.hostAddress}")

            try {
                val json = JSONObject(msg)
                val states = json.getJSONArray("States")

                var deviceIndex = 0
                for (i in 0 until states.length()) {
                    val board = states.getJSONArray(i)
                    for (j in 0 until board.length()) {
                        if (deviceIndex < devices.size) {
                            devices[deviceIndex].status =
                                if (board.getInt(j) == 1) "on" else "off"
                        }
                        deviceIndex++
                    }
                }

                runOnUiThread { updateDeviceStatuses() }

            } catch (e: Exception) {
                Log.e("UDP", "❌ Error parsing UDP data: ${e.message}")
            }
        }

    }

    // fetch device info once
    private fun fetchDevices() {
        val url = "https://capstone.pivotpt.in/esp32API.php"

        val requestBody = JSONObject()
        requestBody.put("serviceID", 2)
        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val userId = prefs.getInt("userId",-1)

        requestBody.put("user_id", userId)

        val request = JsonObjectRequest(
            Request.Method.POST, url, requestBody,
            { response ->
                if (response.getInt("code") == 0) {
                    devices.clear()
                    val devicesJson = response.getJSONArray("devices")

                    for (i in 0 until devicesJson.length()) {
                        val obj = devicesJson.getJSONObject(i)
                        val device = Device(
                            device_id = obj.getString("device_id"),
                            device_name = obj.getString("device_name"),
                            switch_board_id = obj.getString("switch_board_id"),
                            status = "off", // will be updated by UDP
                            device_type = obj.optString("device_type", "null")
                        )
                        devices.add(device)
                    }

                    displayDevices(devices)
                } else {
                    Toast.makeText(
                        this,
                        "Error: ${response.getString("message")}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            { error ->
                Toast.makeText(this, "Volley error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    // update only statuses from UDP
    private fun updateDeviceStatuses() {
        for (i in 0 until gridLayout.childCount) {
            val view = gridLayout.getChildAt(i)
            val toggleSwitch = view.findViewById<Switch>(R.id.deviceSwitch)

            if (i < devices.size) {
                val device = devices[i]

                // detach listener before update
                toggleSwitch.setOnCheckedChangeListener(null)
                toggleSwitch.isChecked = device.status == "on"

                // reattach listener for user action
                toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
                    val newStatus = if (isChecked) "on" else "off"
                    toggleDevice(device.device_id, newStatus, toggleSwitch, !isChecked)
                }
            }
        }
    }

    // create device views
    private fun displayDevices(devices: List<Device>) {
        gridLayout.removeAllViews()

        for (device in devices) {
            val view = layoutInflater.inflate(R.layout.device_list, gridLayout, false)

            val nameText = view.findViewById<TextView>(R.id.deviceName)
            val typeText = view.findViewById<TextView>(R.id.deviceType)
            val toggleSwitch = view.findViewById<Switch>(R.id.deviceSwitch)
            val editBtn = view.findViewById<ImageButton>(R.id.editButton)

            nameText.text = device.device_name
            typeText.text = device.device_type
            toggleSwitch.isChecked = device.status == "on"

            // attach listener for user action
            toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
                val newStatus = if (isChecked) "on" else "off"
                toggleDevice(device.device_id, newStatus, toggleSwitch, !isChecked)
            }

            editBtn.setOnClickListener {
                SetDeviceInfo(device)
            }

            gridLayout.addView(view)
        }
    }

    // API call for toggle (user only)
    private fun toggleDevice(
        deviceId: String,
        newStatus: String,
        toggleSwitch: Switch,
        previousState: Boolean
    ) {
        val hub_ip = prefs.getString("hub_ip", null)
        val url = "http://$hub_ip/toggle/$deviceId"

        toggleSwitch.isEnabled = false // disable until response

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                // ✅ Log success response
                Log.d("ToggleDevice", "Success response: $response")

                Toast.makeText(
                    this,
                    "Device $deviceId turned $newStatus",
                    Toast.LENGTH_SHORT
                ).show()

                // update local device status
                val device = devices.find { it.device_id == deviceId }
                device?.status = newStatus

                toggleSwitch.isEnabled = true
            },
            { error ->
                // ❌ Log error response
                Log.e("ToggleDevice", "Error toggling device $deviceId, Error: ${error.message}", error)

                Toast.makeText(
                    this,
                    "Error: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()

                toggleSwitch.setOnCheckedChangeListener(null)
                toggleSwitch.isChecked = previousState
                toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
                    val retryStatus = if (isChecked) "on" else "off"
                    toggleDevice(deviceId, retryStatus, toggleSwitch, !isChecked)
                }

                toggleSwitch.isEnabled = true
            }
        )

        Log.d("ToggleDevice", "Sending request to: $url with newStatus=$newStatus")
        Volley.newRequestQueue(this).add(request)
    }

    // menu
    private fun setupMenuButton() {
        menuButton.setOnClickListener {
            val popup = PopupMenu(this, menuButton)
            popup.menuInflater.inflate(R.menu.top_menu, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_signout -> {
                        Toast.makeText(this, "Signing out...", Toast.LENGTH_SHORT).show()
                        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                        prefs.edit()
                            .remove("phone")
                            .remove("password")
                            .remove("is_logged_in")
                            .apply()

                        val intent = Intent(this, LoginSignupActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)

                        finish()
                        true
                    }

                    else -> false
                }
            }
            popup.show()
        }
    }


    private fun SetDeviceInfo(device: Device) {
        val dialogView = layoutInflater.inflate(R.layout.edit_device_info, null)
        val editName = dialogView.findViewById<EditText>(R.id.editDeviceName)
        val editType = dialogView.findViewById<EditText>(R.id.editDeviceType)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        val submitBtn = dialogView.findViewById<Button>(R.id.submitButton)

        // Prefill current values
        editName.setText(device.device_name)
        editType.setText(device.device_type) // 👈 show current type instead of id

        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        submitBtn.setOnClickListener {
            val newName = editName.text.toString().trim()
            val newType = editType.text.toString().trim()

            if (newName.isEmpty() && newType.isEmpty()) {
                Toast.makeText(this, "At least one field is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Keep old values if empty
            val finalName = if (newName.isEmpty()) device.device_name else newName
            val finalType = if (newType.isEmpty()) device.device_type else newType

            // Show progress
            progressBar.visibility = android.view.View.VISIBLE
            submitBtn.isEnabled = false

            // Pass device_id along with new values
            SetDeviceInfoAPICall(
                deviceId = device.device_id,
                newName = finalName,
                newType = finalType,
                onSuccess = {
                    progressBar.visibility = android.view.View.GONE
                    submitBtn.isEnabled = true
                    Toast.makeText(this, "Updated successfully!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    fetchDevices()
                },
                onError = { errorMsg ->
                    progressBar.visibility = android.view.View.GONE
                    submitBtn.isEnabled = true
                    Toast.makeText(this, "Error: $errorMsg", Toast.LENGTH_SHORT).show()
                }
            )
        }

        dialog.show()
    }

    private fun SetDeviceInfoAPICall(
        deviceId: String,
        newName: String,
        newType: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val requestBody = JSONObject().apply {
            put("serviceID", 3)
            put("device_id", deviceId)
            put("device_name", newName)
            put("device_type", newType)
        }

        val url = "https://capstone.pivotpt.in/esp32API.php"
        val request = JsonObjectRequest(
            Request.Method.POST, url, requestBody,
            { response ->
                if (response.getInt("code") == 0) {
                    onSuccess()
                } else {
                    onError(response.getString("message"))
                }
            },
            { error ->
                onError(error.message ?: "Unknown error")
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun setupDrawer() {
        // open drawer when hamburger clicked
        drawerToggle.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        // Access header view inside NavigationView
        val headerView = navigationView.getHeaderView(0)
        val tvUsername = headerView.findViewById<TextView>(R.id.profileName)

        // Load from SharedPreferences
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        var username = prefs.getString("username", "Guest") // fallback = Guest
        if(username ==""|| username == null){
            username = "Guest"
        }
        tvUsername.text = username
        // handle navigation menu clicks
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, AppliancesActivity::class.java)
                    // Clear back stack so Home becomes the root
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    true
                }

//                R.id.nav_profile -> {
//                    val intent = Intent(this, ProfileActivity::class.java)
//                    startActivity(intent)
//                    true
//                }

//                R.id.add_device -> {
//                    val intent = Intent(this, AddDeviceActivity::class.java)
//                    startActivity(intent)
//                    true
//                }

//                R.id.update_wifi_cred -> {
//                    val intent = Intent(this, WifiActivity::class.java)
//                    startActivity(intent)
//                    true
//                }

                R.id.nav_logout -> {
                    handleLogout()
                    true
                }

                else -> false
            }.also {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
        }

    }

    private fun handleLogout() {
        Toast.makeText(this, "Signing out...", Toast.LENGTH_SHORT).show()
        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .remove("phone")
            .remove("password")
            .remove("is_logged_in")
            .apply()

        val intent = Intent(this, LoginSignupActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        if (isTaskRoot) {
            ExitUtils.showExitDialog(this)
        } else {
            super.onBackPressed()
        }
    }





}
