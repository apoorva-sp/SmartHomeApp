package com.example.myhome.UserInterface

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myhome.R
import com.example.myhome.network.BroadcastHelper
import kotlinx.coroutines.launch

class IpDiscoveryActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var sendButton: Button
    private lateinit var resultText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.broadcast_ip)

        progressBar = findViewById(R.id.progressBar)
        sendButton = findViewById(R.id.sendButton)
        resultText = findViewById(R.id.resultText)

        sendButton.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            resultText.text = "Searching for hub..."

            lifecycleScope.launch {
                val hubIp = BroadcastHelper.discoverEsp32Hub()
                progressBar.visibility = View.GONE
                resultText.text = hubIp ?: "❌ No hub found"
            }
        }
    }
}