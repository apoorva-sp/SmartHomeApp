package com.example.myhome.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object BroadcastHelper {

    private const val PORT = 5000
    private const val TIMEOUT_DISCOVERY = 5000
    private const val TIMEOUT_VERIFY = 2000
    private const val BROADCAST_IP = "192.168.29.255" // ⚠️ make configurable if needed

    /**
     * Send DISCOVER_HUB broadcast and wait for a reply.
     */
    suspend fun discoverEsp32Hub(): String? = withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket().apply {
                broadcast = true
                soTimeout = TIMEOUT_DISCOVERY
            }

            val message = "DISCOVER_HUB".toByteArray()
            val sendPacket = DatagramPacket(message, message.size, InetAddress.getByName(BROADCAST_IP), PORT)
            socket.send(sendPacket)

            val buffer = ByteArray(1024)
            val receivePacket = DatagramPacket(buffer, buffer.size)
            socket.receive(receivePacket)

            receivePacket.address.hostAddress
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
            socket.soTimeout = TIMEOUT_VERIFY

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