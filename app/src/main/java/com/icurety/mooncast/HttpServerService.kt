package com.icurety.mooncast

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import java.io.*
import java.net.*
import java.util.concurrent.Executors
import java.net.InetAddress
import org.json.JSONObject

class HttpServerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    
    companion object {
        private const val TAG = "HttpServerService"
        private const val PORT = 8080
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "HttpServerService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            startHttpServer()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopHttpServer()
        scope.cancel()
    }

    private fun startHttpServer() {
        scope.launch {
            try {
                serverSocket = ServerSocket(PORT)
                isRunning = true
                Log.d(TAG, "HTTP Server started on port $PORT")

                while (isRunning && !serverSocket!!.isClosed) {
                    try {
                        val clientSocket = serverSocket!!.accept()
                        launch { handleClient(clientSocket) }
                    } catch (e: Exception) {
                        if (isRunning) {
                            Log.e(TAG, "Error accepting client connection", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting HTTP server", e)
            }
        }
    }

    private fun stopHttpServer() {
        isRunning = false
        try {
            serverSocket?.close()
            Log.d(TAG, "HTTP Server stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping HTTP server", e)
        }
    }

    private suspend fun handleClient(clientSocket: Socket) {
        withContext(Dispatchers.IO) {
            try {
                val input = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                val output = PrintWriter(clientSocket.getOutputStream(), true)

                val requestLine = input.readLine()
                if (requestLine != null) {
                    Log.d(TAG, "Received request: $requestLine")
                    val parts = requestLine.split(" ")
                    if (parts.size >= 2) {
                        val method = parts[0]
                        val path = parts[1]
                        
                        // Read headers
                        var line: String?
                        val headers = mutableMapOf<String, String>()
                        while (input.readLine().also { line = it } != null && line!!.isNotEmpty()) {
                            val headerParts = line!!.split(": ", limit = 2)
                            if (headerParts.size == 2) {
                                headers[headerParts[0]] = headerParts[1]
                            }
                        }
                        
                        // Read POST body if present
                        var postBody = ""
                        if (method == "POST" && headers["Content-Length"] != null) {
                            val contentLength = headers["Content-Length"]?.toIntOrNull() ?: 0
                            if (contentLength > 0) {
                                val bodyChars = CharArray(contentLength)
                                input.read(bodyChars, 0, contentLength)
                                postBody = String(bodyChars)
                            }
                        }
                        
                        val clientIP = clientSocket.inetAddress.hostAddress ?: "unknown"
                        val hostname = extractHostname(clientSocket, headers, postBody)
                        
                        // Save IP to hostname mapping
                        if (hostname.isNotEmpty()) {
                            HostStorage.saveHostMapping(this@HttpServerService, clientIP, hostname)
                        }
                        
                        handleRequest(method, path, clientIP, hostname, postBody, output)
                    }
                }
                
                clientSocket.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error handling client", e)
                try {
                    clientSocket.close()
                } catch (closeEx: Exception) {
                    Log.e(TAG, "Error closing client socket", closeEx)
                }
            }
        }
    }

    private fun extractHostname(clientSocket: Socket, headers: Map<String, String>, postBody: String): String {
        // 1. Try to get hostname from POST body (if explicitly sent)
        if (postBody.isNotEmpty()) {
            try {
                val json = JSONObject(postBody)
                if (json.has("pc_name")) {
                    val pcName = json.getString("pc_name")
                    Log.d(TAG, "Got PC name from POST body: $pcName")
                    return pcName
                }
            } catch (e: Exception) {
                // Not JSON, try simple key-value format
                if (postBody.contains("pc_name=")) {
                    val pcName = postBody.substringAfter("pc_name=").substringBefore("&")
                    Log.d(TAG, "Got PC name from form data: $pcName")
                    return pcName
                }
            }
        }
        
        // 2. Try to get from Host header
        headers["Host"]?.let { host ->
            val hostname = host.split(":")[0] // Remove port if present
            if (hostname.isNotEmpty() && hostname != "localhost" && !hostname.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))) {
                Log.d(TAG, "Got hostname from Host header: $hostname")
                return hostname
            }
        }
        
        // 3. Try reverse DNS lookup
        try {
            val address = clientSocket.inetAddress
            val hostname = address.canonicalHostName
            if (hostname != address.hostAddress && hostname.isNotEmpty()) {
                Log.d(TAG, "Got hostname from reverse DNS: $hostname")
                return hostname
            }
        } catch (e: Exception) {
            Log.d(TAG, "Reverse DNS lookup failed: ${e.message}")
        }
        
        // 4. Try User-Agent header for some info
        headers["User-Agent"]?.let { userAgent ->
            if (userAgent.contains("Sunshine")) {
                // Extract computer name from Sunshine user agent if available
                val parts = userAgent.split(" ")
                for (part in parts) {
                    if (part.contains("-") && !part.contains("Mozilla") && !part.contains("Version")) {
                        Log.d(TAG, "Got hostname from User-Agent: $part")
                        return part
                    }
                }
            }
        }
        
        Log.d(TAG, "Could not determine hostname, using IP")
        return "" // Will use IP address as fallback
    }

    private fun handleRequest(method: String, path: String, clientIP: String, hostname: String, postBody: String, output: PrintWriter) {
        when {
            method == "POST" && path == "/cast" -> {
                Log.d(TAG, "Cast request from $clientIP (${hostname.ifEmpty { "unknown" }})")
                val success = handleCastScreen(clientIP, hostname)
                sendResponse(output, if (success) 200 else 500, if (success) "Cast started" else "Cast failed")
            }
            method == "POST" && path == "/stop" -> {
                Log.d(TAG, "Stop cast request from $clientIP")
                val success = handleStopCast()
                sendResponse(output, if (success) 200 else 500, if (success) "Cast stopped" else "Stop failed")
            }
            method == "GET" && path == "/" -> {
                Log.d(TAG, "Status request from $clientIP")
                sendResponse(output, 200, "Mooncast server running")
            }
            else -> {
                Log.d(TAG, "Unknown request: $method $path from $clientIP")
                sendResponse(output, 404, "Not found")
            }
        }
    }

    private fun sendResponse(output: PrintWriter, statusCode: Int, message: String) {
        val statusText = when (statusCode) {
            200 -> "OK"
            404 -> "Not Found"
            500 -> "Internal Server Error"
            else -> "Unknown"
        }
        
        output.println("HTTP/1.1 $statusCode $statusText")
        output.println("Content-Type: text/plain")
        output.println("Content-Length: ${message.length}")
        output.println()
        output.println(message)
    }

    private fun handleCastScreen(hostIP: String, hostname: String): Boolean {
        return try {
            Log.d(TAG, "=== STARTING CAST SCREEN ===")
            Log.d(TAG, "Host IP: $hostIP")
            Log.d(TAG, "Host Name: $hostname")
            
            // Create explicit intent targeting our app package
            val intent = Intent("com.icurety.mooncast.ACTION_START_CAST")
            intent.setPackage(packageName) // Explicit package targeting
            intent.putExtra("host_ip", hostIP)
            intent.putExtra("host_name", hostname.ifEmpty { hostIP })
            
            Log.d(TAG, "Sending broadcast intent: ${intent.action}")
            Log.d(TAG, "Intent package: ${intent.`package`}")
            Log.d(TAG, "Intent extras: host_ip=$hostIP, host_name=${hostname.ifEmpty { hostIP }}")
            
            sendBroadcast(intent)
            
            // Also try to launch Moonlight directly as fallback
            Log.d(TAG, "Also attempting direct Moonlight launch as fallback...")
            launchMoonlightDirectly(hostIP, hostname)
            
            Log.d(TAG, "Cast screen broadcast sent successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting cast", e)
            false
        }
    }

    private fun handleStopCast(): Boolean {
        return try {
            Log.d(TAG, "=== STOPPING CAST ===")
            
            val intent = Intent("com.icurety.mooncast.ACTION_STOP_CAST")
            intent.setPackage(packageName) // Explicit package targeting
            
            Log.d(TAG, "Sending stop broadcast intent: ${intent.action}")
            sendBroadcast(intent)
            
            Log.d(TAG, "Stop cast broadcast sent successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping cast", e)
            false
        }
    }

    private fun launchMoonlightDirectly(hostIP: String, hostname: String) {
        try {
            // Try to launch Moonlight directly if accessibility service isn't available
            val packageManager = this.packageManager
            val launchIntent = packageManager.getLaunchIntentForPackage("com.limelight")
            
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // Store the connection info for the accessibility service to pick up later
                HostStorage.saveHostMapping(this, hostIP, hostname.ifEmpty { hostIP })
                
                Log.d(TAG, "Launching Moonlight app directly")
                startActivity(launchIntent)
            } else {
                Log.w(TAG, "Moonlight app not found for direct launch")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching Moonlight directly", e)
        }
    }
} 