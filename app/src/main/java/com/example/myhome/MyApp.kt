// File: app/src/main/java/com/example/myhome/MyApp.kt
package com.example.myhome

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.myhome.network.UdpPortManager

class MyApp : Application(), DefaultLifecycleObserver {
    override fun onCreate() {
        super.onCreate()
        // Attach observer to the process lifecycle
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        UdpPortManager.startListening()
    }

    override fun onStop(owner: LifecycleOwner) {
        UdpPortManager.stopListening()
    }
}

