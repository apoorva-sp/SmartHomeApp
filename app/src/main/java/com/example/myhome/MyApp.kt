package com.example.myhome

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.myhome.network.UdpPortManager

class MyApp : Application(), DefaultLifecycleObserver {

    override fun onCreate() {
        super<Application>.onCreate()  // ✅ this belongs only to Application.onCreate()
        // Register lifecycle observer
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        // App goes to foreground → start UDP
        UdpPortManager.startListening()
    }

    override fun onStop(owner: LifecycleOwner) {
        // App goes to background → stop UDP
        UdpPortManager.stopListening()
    }
}
