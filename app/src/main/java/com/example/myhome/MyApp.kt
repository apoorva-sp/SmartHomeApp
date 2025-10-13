package com.example.myhome

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.myhome.UserInterface.IpDiscoveryActivity
import com.example.myhome.UserInterface.LoginSignupActivity
import com.example.myhome.network.MqttHelper
import com.example.myhome.network.MqttShutdownService
import com.example.myhome.network.UdpPortManager
import org.json.JSONObject

class MyApp : Application(), DefaultLifecycleObserver {
    private lateinit var prefs: android.content.SharedPreferences // ⭐ cache prefs
    // ✅ ADD THIS FLAG
    private var isAppInBackground = false


    override fun onCreate() {
        super<Application>.onCreate()  // ✅ this belongs only to Application.onCreate()
        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        // Register lifecycle observer
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        helperToStart()

    }

    override fun onStop(owner: LifecycleOwner) {
        // App goes to background → stop UDP
        helperToStop()

    }

    override fun onDestroy(owner: LifecycleOwner) {
        helperToStop()
    }

    override fun onPause(owner: LifecycleOwner) {
        helperToStop()
    }

    override fun onResume(owner: LifecycleOwner) {
        helperToStart()

    }
    fun helperToStart(){
        // Always restart UDP listening
        UdpPortManager.startListening()
        isAppInBackground = false

        // 🔹 Force open your chosen activity
        val isSetupDone = prefs.getBoolean("is_esp32_setup_done", false)

        val intent = if (isSetupDone) {
            Intent(this, IpDiscoveryActivity::class.java)
        } else {
            Intent(this, LoginSignupActivity::class.java)
        }

        // Ensure no old activities remain in the stack
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        applicationContext.startActivity(intent)
    }

    fun helperToStop(){
        if (isAppInBackground) {
            return // Shutdown logic has already been triggered
        }
        isAppInBackground = true
        val lanEnabled = prefs.getBoolean("LAN", false)
        if (lanEnabled) {
            UdpPortManager.stopListening(applicationContext)
        } else {
            Log.d("MyApp", "App stopping, starting MqttShutdownService.")
            val intent = Intent(this, MqttShutdownService::class.java)
            startService(intent)

        }
    }

    }
