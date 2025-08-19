package com.example.myhome

import android.content.Intent
import android.os.Bundle
import android.widget.GridLayout
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.Toast
import org.xml.sax.Locator


class Appliances : AppCompatActivity() {

    private lateinit var gridLayout: GridLayout
    private lateinit var backtohome: TextView
    private lateinit var tvHouseName :TextView
    private lateinit var tvLocation : TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.appliances)

        gridLayout = findViewById(R.id.gridLayoutDevices)
        backtohome = findViewById(R.id.backtohome)
        tvHouseName = findViewById(R.id.tvHouseName)
        tvLocation = findViewById(R.id.tvLocation)

        // Sample devices
        val devices = listOf(
            Device("Main Light", "Bulb", "💡", false),
            Device("Table Lamp", "Bulb", "💡", false),
            Device("Smart TV", "Tv", "📺", false),
            Device("AC Unit", "Ac", "🌡", false)
        )

        tvHouseName.text = intent.getStringExtra("housename")
        tvLocation.text = intent.getStringExtra("location")
        displayDevices(devices)
        backtohome.setOnClickListener {
            finish()
        }

    }

    private fun displayDevices(devices: List<Device>) {
        gridLayout.removeAllViews()

        for (device in devices) {
            val view = layoutInflater.inflate(R.layout.device_list, gridLayout, false)

            val nameText = view.findViewById<TextView>(R.id.deviceName)
            val typeText = view.findViewById<TextView>(R.id.deviceType)
            val toggleSwitch = view.findViewById<Switch>(R.id.deviceSwitch)

            nameText.text = "${device.icon} ${device.name}"
            typeText.text = device.type
            toggleSwitch.isChecked = device.isOn

            toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
                Toast.makeText(this, "${device.name} turned ${if (isChecked) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
            }

            gridLayout.addView(view)
        }
    }
}
