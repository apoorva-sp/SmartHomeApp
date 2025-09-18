package com.example.myhome.UserInterface

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.myhome.Beans.Device
import com.example.myhome.Keys
import com.example.myhome.R
import com.example.myhome.network.UdpPortManager
import com.example.myhome.utils.ExitUtils
import com.example.myhome.utils.NavigationBarActivity
import org.json.JSONObject

class AppliancesActivity : NavigationBarActivity() {   // ✅ Now extends BaseActivity

    private lateinit var gridLayout: GridLayout
    private lateinit var prefs: android.content.SharedPreferences
    private val devices = mutableListOf<Device>()
    private lateinit var searchBox: EditText
    private lateinit var searchButton: Button
    private var url =""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout into BaseActivity container
        val container = findViewById<android.widget.FrameLayout>(R.id.container)
        layoutInflater.inflate(R.layout.appliances, container, true)

        findViewById<TextView>(R.id.pageTitle).text = "Home"

        gridLayout = findViewById(R.id.gridLayoutDevices)
        searchBox = findViewById(R.id.searchBox)
        searchButton = findViewById(R.id.searchButton)

        url = Keys.BaseURL+"esp32API.php"

        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

//        searchButton.setOnClickListener {
//            val query = searchBox.text.toString().trim()
//            val filteredDevices = devices.filter {
//                it.device_name.contains(query, ignoreCase = true) ||
//                        it.device_type.contains(query, ignoreCase = true)
//            }
//            displayDevices(filteredDevices)
//        }


        searchBox.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                // Filter devices based on name or type
                val filteredDevices = devices.filter {
                    it.device_name.contains(query, ignoreCase = true) ||
                            it.device_type.contains(query, ignoreCase = true)
                }
                displayDevices(filteredDevices)
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })


        fetchDevices()

        // ✅ UDP listener stays here
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
//                runOnUiThread { updateDeviceStatuses() }

                runOnUiThread {
                    updateDeviceStatuses()
                    val query = searchBox.text.toString().trim()
                    val filteredDevices = devices.filter {
                        it.device_name.contains(query, ignoreCase = true) ||
                                it.device_type.contains(query, ignoreCase = true)
                    }
                    displayDevices(filteredDevices)
                }


            } catch (e: Exception) {
                Log.e("UDP", "❌ Error parsing UDP data: ${e.message}")
            }
        }
    }

    /** -------------------------
     *   DEVICE API CALLS
     *  ------------------------ */

    private fun fetchDevices() {


        val requestBody = JSONObject().apply {
            put("serviceID", 2)
            put("user_id", prefs.getInt("userId", -1))
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, requestBody,
            { response ->
                if (response.getInt("code") == 0) {
                    devices.clear()
                    val devicesJson = response.getJSONArray("devices")

                    for (i in 0 until devicesJson.length()) {
                        val obj = devicesJson.getJSONObject(i)
                        devices.add(
                            Device(
                                device_id = obj.getString("device_id"),
                                device_name = obj.getString("device_name"),
                                switch_board_id = obj.getString("switch_board_id"),
                                status = "off", // updated by UDP
                                device_type = obj.optString("device_type", "null")
                            )
                        )
                    }
                    displayDevices(devices)
                } else {
                    Toast.makeText(this, response.getString("message"), Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Volley error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun toggleDevice(
        deviceId: String,
        newStatus: String,
        toggleSwitch: Switch,
        previousState: Boolean
    ) {
        val hub_ip = prefs.getString("hub_ip", null)
        val hub_url = "http://$hub_ip/toggle/$deviceId"

        toggleSwitch.isEnabled = false

        val request = JsonObjectRequest(
            Request.Method.GET, hub_url, null,
            { response ->
                Log.d("ToggleDevice", "Success response: $response")
                devices.find { it.device_id == deviceId }?.status = newStatus
                toggleSwitch.isEnabled = true
            },
            { error ->
                Log.e("ToggleDevice", "Error toggling $deviceId: ${error.message}", error)
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()

                toggleSwitch.setOnCheckedChangeListener(null)
                toggleSwitch.isChecked = previousState
                toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
                    toggleDevice(deviceId, if (isChecked) "on" else "off", toggleSwitch, !isChecked)
                }
                toggleSwitch.isEnabled = true
            }
        )
        Volley.newRequestQueue(this).add(request)
    }

    /** -------------------------
     *   UI HELPERS
     *  ------------------------ */

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

            toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
                toggleDevice(device.device_id, if (isChecked) "on" else "off", toggleSwitch, !isChecked)
            }
            editBtn.setOnClickListener { showEditDialog(device) }

            gridLayout.addView(view)
        }
    }

    private fun updateDeviceStatuses() {
        for (i in 0 until gridLayout.childCount) {
            val view = gridLayout.getChildAt(i)
            val toggleSwitch = view.findViewById<Switch>(R.id.deviceSwitch)
            if (i < devices.size) {
                val device = devices[i]
                toggleSwitch.setOnCheckedChangeListener(null)
                toggleSwitch.isChecked = device.status == "on"
                toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
                    toggleDevice(device.device_id, if (isChecked) "on" else "off", toggleSwitch, !isChecked)
                }
            }
        }
    }

    private fun showEditDialog(device: Device) {
        val dialogView = layoutInflater.inflate(R.layout.edit_device_info, null)
        val editName = dialogView.findViewById<EditText>(R.id.editDeviceName)
        val editType = dialogView.findViewById<EditText>(R.id.editDeviceType)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        val submitBtn = dialogView.findViewById<Button>(R.id.submitButton)

        editName.setText(device.device_name)
        editType.setText(device.device_type)

        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        submitBtn.setOnClickListener {
            val newName = editName.text.toString().trim().ifEmpty { device.device_name }
            val newType = editType.text.toString().trim().ifEmpty { device.device_type }

            progressBar.isVisible = true
            submitBtn.isEnabled = false

            updateDeviceInfo(device.device_id, newName, newType,
                onSuccess = {
                    progressBar.isVisible = false
                    submitBtn.isEnabled = true
                    Toast.makeText(this, "Updated successfully!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    fetchDevices()
                },
                onError = { errorMsg ->
                    progressBar.isVisible = false
                    submitBtn.isEnabled = true
                    Toast.makeText(this, "Error: $errorMsg", Toast.LENGTH_SHORT).show()
                }
            )
        }
        dialog.show()
    }

    private fun updateDeviceInfo(
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


        val request = JsonObjectRequest(
            Request.Method.POST, url, requestBody,
            { response ->
                if (response.getInt("code") == 0) onSuccess()
                else onError(response.getString("message"))
            },
            { error -> onError(error.message ?: "Unknown error") }
        )
        Volley.newRequestQueue(this).add(request)
    }

    /** -------------------------
     *   BACK PRESS HANDLING
     *  ------------------------ */
    override fun onBackPressed() {
        if (isTaskRoot) ExitUtils.showExitDialog(this)
        else super.onBackPressed()
    }
}
