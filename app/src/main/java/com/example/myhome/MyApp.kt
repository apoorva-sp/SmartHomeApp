package com.example.myhome

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.myhome.UserInterface.IpDiscoveryActivity
import com.example.myhome.UserInterface.LoginSignupActivity
import com.example.myhome.network.UdpPortManager

class MyApp : Application(), DefaultLifecycleObserver {
    private lateinit var prefs: android.content.SharedPreferences // ⭐ cache prefs


    override fun onCreate() {
        super<Application>.onCreate()  // ✅ this belongs only to Application.onCreate()
        // Register lifecycle observer
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        // App goes to foreground → start UDP
        UdpPortManager.startListening()
        // 🔹 Force open your chosen activity
        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
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

    override fun onStop(owner: LifecycleOwner) {
        // App goes to background → stop UDP
        UdpPortManager.stopListening(applicationContext)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        UdpPortManager.stopListening(applicationContext)
    }

    override fun onPause(owner: LifecycleOwner) {
        UdpPortManager.stopListening(applicationContext)
    }

    override fun onResume(owner: LifecycleOwner) {
        // Always restart UDP listening
        UdpPortManager.startListening()

        // 🔹 Force open your chosen activity
        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
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

    }
