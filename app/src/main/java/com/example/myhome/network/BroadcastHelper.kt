package com.example.myhome.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException


object BroadcastHelper {

    private const val PORT = 5000
    private const val BROADCAST_IP = "192.168.1.255" // ⚠️ make configurable if needed
    private const val TIMEOUT = 5000 // ms

    suspend fun discoverDevice(message: String = "DISCOVER_HUB"): String? = withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        return@withContext try {
            socket = DatagramSocket().apply {
                broadcast = true
                soTimeout = TIMEOUT
            }

            // Send broadcast
            val data = message.toByteArray()
            val sendPacket = DatagramPacket(data, data.size, InetAddress.getByName(BROADCAST_IP), PORT)
            socket.send(sendPacket)

            // Receive reply
            val buffer = ByteArray(1024)
            val receivePacket = DatagramPacket(buffer, buffer.size)
            socket.receive(receivePacket)

            val reply = String(receivePacket.data, 0, receivePacket.length).trim()
            reply

        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            socket?.close()
        }
    }

    /**
     * Verify that the saved hub IP is alive with a PING → ACK check.
     */
    suspend fun verifyHubIp(savedIp: String): Boolean = withContext(Dispatchers.IO) {
        DatagramSocket().use { socket ->
            socket.soTimeout = TIMEOUT

            return@withContext try {
                val hubAddr = InetAddress.getByName(savedIp)
                val message = "PING".toByteArray()
                val sendPacket = DatagramPacket(message, message.size, hubAddr, PORT)
                socket.send(sendPacket)

                val buffer = ByteArray(1024)
                val receivePacket = DatagramPacket(buffer, buffer.size)
                socket.receive(receivePacket)

                val response = String(receivePacket.data, 0, receivePacket.length).trim()
                response == "ACK"
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}