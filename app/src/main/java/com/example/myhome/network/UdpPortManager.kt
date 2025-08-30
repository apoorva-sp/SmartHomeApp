// File: app/src/main/java/com/example/myhome/network/UdpPortManager.kt
package com.example.myhome.network

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object UdpPortManager {
    private const val PORT = 5000
    private var socket: DatagramSocket? = null
    private var listenJob: Job? = null
    private val TAG = "UdpPortManager"

    // 🔹 LiveData to expose received messages globally
    private val _messages = MutableLiveData<Pair<String, InetAddress>>()
    val messages: LiveData<Pair<String, InetAddress>> get() = _messages

    fun startListening() {
        if (listenJob?.isActive == true) {
            Log.d(TAG, "Already listening on UDP port $PORT")
            return
        }

        listenJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                socket = DatagramSocket(PORT, InetAddress.getByName("0.0.0.0"))
                Log.d(TAG, "UDP socket bound to port $PORT")

                val buffer = ByteArray(1024)
                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket?.receive(packet)

                    val msg = String(packet.data, 0, packet.length)
                    Log.d(TAG, "Received: $msg from ${packet.address.hostAddress}")

                    // Post to LiveData (thread-safe)
                    _messages.postValue(msg to packet.address)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in UDP listener: ${e.message}", e)
            } finally {
                socket?.close()
                socket = null
                Log.d(TAG, "UDP socket closed")
            }
        }
    }

    fun stopListening() {
        listenJob?.cancel()
        listenJob = null
        socket?.close()
        socket = null
        Log.d(TAG, "Stopped UDP listener on port $PORT")
    }

//    UdpPortManager.messages.observe(this) { (msg, addr) ->
//        // Update UI or trigger logic
//        textView.text = "📩 $msg from ${addr.hostAddress}"
//    }
}
