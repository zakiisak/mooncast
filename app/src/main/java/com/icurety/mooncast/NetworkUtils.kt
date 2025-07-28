package com.icurety.mooncast

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.text.format.Formatter
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*

object NetworkUtils {
    
    fun getWiFiIPAddress(context: Context): String? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        return if (wifiManager != null && wifiManager.isWifiEnabled) {
            val wifiInfo = wifiManager.connectionInfo
            if (wifiInfo != null && wifiInfo.ipAddress != 0) {
                Formatter.formatIpAddress(wifiInfo.ipAddress)
            } else {
                getIPAddressFromNetworkInterface()
            }
        } else {
            getIPAddressFromNetworkInterface()
        }
    }
    
    private fun getIPAddressFromNetworkInterface(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (networkInterface in interfaces) {
                // Skip loopback and non-WiFi interfaces
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                
                val addresses = Collections.list(networkInterface.inetAddresses)
                for (address in addresses) {
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        val hostAddress = address.hostAddress
                        if (hostAddress != null && isPrivateIP(hostAddress)) {
                            return hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
    
    private fun isPrivateIP(ip: String): Boolean {
        return ip.startsWith("192.168.") || 
               ip.startsWith("10.") || 
               (ip.startsWith("172.") && 
                ip.split(".").getOrNull(1)?.toIntOrNull()?.let { it in 16..31 } == true)
    }
    
    fun isConnectedToWiFi(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        return if (connectivityManager != null) {
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        } else {
            false
        }
    }
    
    fun getNetworkName(context: Context): String? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        return if (wifiManager != null && wifiManager.isWifiEnabled) {
            val wifiInfo = wifiManager.connectionInfo
            wifiInfo?.ssid?.removeSurrounding("\"")
        } else {
            null
        }
    }
} 