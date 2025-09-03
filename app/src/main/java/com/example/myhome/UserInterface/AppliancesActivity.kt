package com.example.myhome.UserInterface

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.myhome.Beans.Device
import com.example.myhome.R
import com.example.myhome.network.UdpPortManager
import org.json.JSONObject

class AppliancesActivity : AppCompatActivity() {

    private lateinit var gridLayout: GridLayout
    private lateinit var backtohome: TextView
    private lateinit var prefs: android.content.SharedPreferences // ⭐ cache prefs
    private lateinit var menuButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.appliances)

        gridLayout = findViewById(R.id.gridLayoutDevices)
        backtohome = findViewById(R.id.backtohome)
        menuButton = findViewById(R.id.menuButton)

        UdpPortManager.messages.observe(this) { (msg, sender) ->
            // Here you get each incoming UDP message

            fetchDevices()//API call and display devices

            Log.d("LoginSignupActivity", "Message: $msg from $sender")

        }

        setupMenuButton()

        backtohome.setOnClickListener {
            finish()
        }
    }

    private fun fetchDevices() {
        val url = "https://capstone.pivotpt.in/esp32API.php" // replace with your endpoint

        val requestBody = JSONObject()
        requestBody.put("serviceID", 2)
        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val userId = prefs.getInt("userId", -1)

        requestBody.put("user_id", userId)

        val request = JsonObjectRequest(
            Request.Method.POST, url, requestBody,
            { response ->
                if (response.getInt("code") == 0) {
                    val devicesJson = response.getJSONArray("devices")
                    val devices = mutableListOf<Device>()

                    for (i in 0 until devicesJson.length()) {
                        val obj = devicesJson.getJSONObject(i)
                        val device = Device(
                            device_id = obj.getString("device_id"),
                            device_name = obj.getString("device_name"),
                            switch_board_id = obj.getString("switch_board_id"),
                            status = obj.getString("device_status"),
                            device_type = obj.optString("device_type", "null")
                        )
                        devices.add(device)
                    }

                    displayDevices(devices)
                } else {
                    Toast.makeText(this, "Error: ${response.getString("message")}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Volley error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

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
                val newStatus = if (isChecked) "on" else "off"
                OnOrOffDevice(device, newStatus)
            }

            editBtn.setOnClickListener{
                //display a form that will take device inputs
                SetDeviceInfo(device)
            }

            gridLayout.addView(view)
        }
    }

    private fun setupMenuButton() {
        menuButton.setOnClickListener {
            val popup = PopupMenu(this, menuButton)
            popup.menuInflater.inflate(R.menu.top_menu, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_signout -> {
                        Toast.makeText(this, "Signing out...", Toast.LENGTH_SHORT).show()

                        // Remove only specific keys
                        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                        prefs.edit()
                            .remove("phone")
                            .remove("password")
                            .remove("is_logged_in")
                            .apply() // ✅ don't forget apply()

                        // Go to login page
                        val intent = Intent(this, LoginSignupActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)

                        finish() // close current activity
                        true
                    }

                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun OnOrOffDevice(device: Device, newStatus: String) {
        val url = "https://capstone.pivotpt.in/esp32API.php"

        val requestBody = JSONObject().apply {
            put("serviceID", 4)
            put("device_id", device.device_id)
            put("state", newStatus)
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, requestBody,
            { response ->
                if (response.getInt("code") == 0) {
                    Toast.makeText(this, "${device.device_name} turned $newStatus", Toast.LENGTH_SHORT).show()

                } else {
                    Toast.makeText(this, "Error: ${response.getString("message")}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Volley error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(this).add(request)
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


}
