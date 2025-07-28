package com.icurety.mooncast

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.icurety.mooncast.ui.theme.MooncastTheme
import kotlinx.coroutines.delay
import android.util.Log
import android.os.Handler
import android.os.Looper
import android.view.View

class MainActivity : ComponentActivity() {
    
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var accessibilityPermissionLauncher: ActivityResultLauncher<Intent>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Test logging at app startup
        Log.e("MainActivity", "🔴 ERROR LEVEL LOG - MainActivity onCreate called")
        Log.w("MainActivity", "🟡 WARN LEVEL LOG - MainActivity onCreate called")
        Log.i("MainActivity", "🔵 INFO LEVEL LOG - MainActivity onCreate called") 
        Log.d("MainActivity", "🟢 DEBUG LEVEL LOG - MainActivity onCreate called")
        Log.v("MainActivity", "⚪ VERBOSE LEVEL LOG - MainActivity onCreate called")
        System.out.println("SYSTEM.OUT: MainActivity onCreate called")
        println("PRINTLN: MainActivity onCreate called")
        
        // Enable fullscreen mode
        enableFullscreenMode()
        
        enableEdgeToEdge()
        
        // Initialize wake lock
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Mooncast::ScreenWakeLock"
        )
        
        // Initialize accessibility permission launcher
        accessibilityPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            // Check if accessibility service is enabled after returning from settings
            checkAccessibilityPermission()
        }
        
        setContent {
            MooncastTheme {
                MooncastApp(
                    onRequestBatteryOptimization = { requestBatteryOptimizationExemption() },
                    onRequestAccessibilityPermission = { requestAccessibilityPermission() },
                    onTestAccessibilityService = { testAccessibilityService() },
                    onCheckServiceStatus = { checkServiceStatusOnly() }
                )
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Re-enable fullscreen mode when returning to app
        enableFullscreenMode()
        
        // Acquire wake lock to keep screen on
        if (!wakeLock.isHeld) {
            wakeLock.acquire()
        }
        
        // Start HTTP server service
        val serviceIntent = Intent(this, HttpServerService::class.java)
        startService(serviceIntent)
        
        // Check permissions
        checkBatteryOptimization()
        checkAccessibilityPermission()
        
        // Schedule periodic accessibility service check
        scheduleAccessibilityCheck()
    }
    
    private var accessibilityCheckHandler: Handler? = null
    
    private fun scheduleAccessibilityCheck() {
        // Cancel any existing checks
        accessibilityCheckHandler?.removeCallbacksAndMessages(null)
        
        // Create new handler for periodic checks
        accessibilityCheckHandler = Handler(Looper.getMainLooper())
        
        val checkRunnable = object : Runnable {
            override fun run() {
                if (!isFinishing && !isDestroyed) {
                    // Check if accessibility service is still enabled
                    if (!isAccessibilityServiceEnabled()) {
                        Log.w("MainActivity", "⚠️ Accessibility service was disabled - auto-prompting re-enable")
                        // Auto-prompt user to re-enable (more aggressive approach)
                        requestAccessibilityPermission()
                    } else {
                        Log.d("MainActivity", "✅ Accessibility service is still enabled")
                    }
                    
                    // Schedule next check in 30 seconds (more frequent)
                    accessibilityCheckHandler?.postDelayed(this, 30000)
                }
            }
        }
        
        // Start the periodic check in 10 seconds (faster initial check)
        accessibilityCheckHandler?.postDelayed(checkRunnable, 10000)
        Log.d("MainActivity", "⏰ Scheduled periodic accessibility service check (every 30s)")
    }
    
    override fun onPause() {
        super.onPause()
        
        // Release wake lock when app goes to background
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Cancel periodic accessibility checks
        accessibilityCheckHandler?.removeCallbacksAndMessages(null)
        
        // Release wake lock
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        
        // Stop HTTP server service
        val serviceIntent = Intent(this, HttpServerService::class.java)
        stopService(serviceIntent)
    }
    
    private fun checkBatteryOptimization() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            Toast.makeText(this, "Please disable battery optimization for Mooncast", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun checkAccessibilityPermission() {
        val isEnabled = isAccessibilityServiceEnabled()
        Log.d("MainActivity", "Accessibility service enabled: $isEnabled")
        if (!isEnabled) {
            Toast.makeText(this, "Please enable Mooncast accessibility service", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "✓ Accessibility service is enabled", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun requestBatteryOptimizationExemption() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open battery optimization settings", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun requestAccessibilityPermission() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            accessibilityPermissionLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open accessibility settings", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun testAccessibilityService() {
        Log.d("MainActivity", "=== ACCESSIBILITY SERVICE TEST ===")
        
        // First, let's check if the service is even enabled
        val isEnabled = isAccessibilityServiceEnabled()
        Log.d("MainActivity", "Service enabled: $isEnabled")
        
        // Check what accessibility services are actually running
        checkRunningAccessibilityServices()
        
        if (isEnabled) {
            Log.d("MainActivity", "Accessibility service appears to be enabled")
            
            // First, let's try sending a broadcast without any complex operations
            Log.d("MainActivity", "Sending simple test broadcast...")
            val intent = Intent("com.icurety.mooncast.ACTION_STOP_CAST")
            intent.setPackage(packageName)
            
            Log.d("MainActivity", "Broadcast details:")
            Log.d("MainActivity", "  Action: ${intent.action}")
            Log.d("MainActivity", "  Package: ${intent.`package`}")
            Log.d("MainActivity", "  App package: $packageName")
            
            try {
                sendBroadcast(intent)
                Log.d("MainActivity", "✓ Test STOP broadcast sent successfully")
                
                // Wait a moment to see if we get any logs from the service
                Handler(Looper.getMainLooper()).postDelayed({
                    Log.d("MainActivity", "Checking if service responded...")
                    Log.d("MainActivity", "If you don't see MoonlightA11yService logs above, the service isn't receiving broadcasts")
                    Toast.makeText(this, "Test broadcast sent - check logs for service response", Toast.LENGTH_LONG).show()
                }, 2000)
                
            } catch (e: Exception) {
                Log.e("MainActivity", "❌ Error sending broadcast", e)
                Toast.makeText(this, "Error sending broadcast: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Log.e("MainActivity", "❌ Accessibility service not enabled")
            Log.e("MainActivity", "Go to: Settings → Accessibility → Mooncast → Turn ON")
            Toast.makeText(this, "❌ Accessibility service not enabled", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun checkServiceStatusOnly() {
        Log.d("MainActivity", "=== CHECKING SERVICE STATUS ONLY ===")
        
        // Check what accessibility services are actually running
        checkRunningAccessibilityServices()
        
        val isEnabled = isAccessibilityServiceEnabled()
        if (isEnabled) {
            Log.d("MainActivity", "✅ Service appears enabled in settings")
            
            // Additional debugging for why service might not be running
            troubleshootAccessibilityService()
            
            Toast.makeText(this, "✓ Service appears to be enabled", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "❌ Service not enabled", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun troubleshootAccessibilityService() {
        Log.d("MainActivity", "=== TROUBLESHOOTING ACCESSIBILITY SERVICE ===")
        
        try {
            // Check if the service class can be instantiated
            val serviceClass = MoonlightAccessibilityService::class.java
            Log.d("MainActivity", "✅ Service class exists: ${serviceClass.name}")
            
            // Check the manifest declaration
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SERVICES)
            val services = packageInfo.services
            
            if (services != null) {
                Log.d("MainActivity", "📋 Services declared in manifest:")
                services.forEach { service ->
                    Log.d("MainActivity", "  - ${service.name}")
                    if (service.name.contains("MoonlightAccessibilityService")) {
                        Log.d("MainActivity", "    ✅ Found accessibility service in manifest")
                        Log.d("MainActivity", "    Permission: ${service.permission}")
                        Log.d("MainActivity", "    Exported: ${service.exported}")
                    }
                }
            }
            
            // Check if we can access the accessibility service configuration
            try {
                val resources = resources
                val configId = resources.getIdentifier("accessibility_service_config", "xml", packageName)
                Log.d("MainActivity", "Config resource ID: $configId")
                if (configId != 0) {
                    Log.d("MainActivity", "✅ Accessibility service config found")
                } else {
                    Log.e("MainActivity", "❌ Accessibility service config not found")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "❌ Error checking config", e)
            }
            
            // Suggest troubleshooting steps
            Log.d("MainActivity", "=== TROUBLESHOOTING STEPS ===")
            Log.d("MainActivity", "If you see NO MoonlightA11yService logs:")
            Log.d("MainActivity", "1. Go to Settings → Accessibility → Mooncast")
            Log.d("MainActivity", "2. Turn OFF the service")
            Log.d("MainActivity", "3. Wait 2 seconds")
            Log.d("MainActivity", "4. Turn ON the service")
            Log.d("MainActivity", "5. Look for MoonlightA11yService logs")
            Log.d("MainActivity", "6. If still no logs, restart the device")
            
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in troubleshooting", e)
        }
    }
    
    fun checkRunningAccessibilityServices(): String {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return "Running services: ${enabledServices ?: "none"}"
    }
    
    private fun enableFullscreenMode() {
        try {
            // Hide system UI for immersive fullscreen experience
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
            
            Log.i("MainActivity", "🖥️ Fullscreen mode enabled")
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ Error enabling fullscreen mode", e)
        }
    }
    
    fun isAccessibilityServiceEnabled(): Boolean {
        val serviceName = "$packageName/${MoonlightAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        val isEnabled = enabledServices?.contains(serviceName) == true
        
        Log.d("MainActivity", "=== ACCESSIBILITY SERVICE CHECK ===")
        Log.d("MainActivity", "Service name: $serviceName")
        Log.d("MainActivity", "Enabled services: $enabledServices")
        Log.d("MainActivity", "Is enabled: $isEnabled")
        
        return isEnabled
    }
}

@Composable
fun MooncastApp(
    onRequestBatteryOptimization: () -> Unit = {},
    onRequestAccessibilityPermission: () -> Unit = {},
    onTestAccessibilityService: () -> Unit = {},
    onCheckServiceStatus: () -> Unit = {}
) {
    val context = LocalContext.current
    var ipAddress by remember { mutableStateOf<String?>(null) }
    var networkName by remember { mutableStateOf<String?>(null) }
    var isWifiConnected by remember { mutableStateOf(false) }
    
    // Update network info periodically
    LaunchedEffect(Unit) {
        while (true) {
            ipAddress = NetworkUtils.getWiFiIPAddress(context)
            networkName = NetworkUtils.getNetworkName(context)
            isWifiConnected = NetworkUtils.isConnectedToWiFi(context)
            delay(5000) // Update every 5 seconds
        }
    }
    
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Large IP Address Display (Main Focus)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isWifiConnected) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.errorContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🌙 MOONCAST READY",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isWifiConnected) 
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else 
                            MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = ipAddress ?: "Getting IP...",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isWifiConnected) 
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else 
                            MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "📶 ${networkName ?: "Unknown Network"}",
                        fontSize = 16.sp,
                        color = if (isWifiConnected) 
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else 
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                    
                    if (!isWifiConnected) {
                        Text(
                            text = "⚠️ WiFi not connected",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Network Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isWifiConnected) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isWifiConnected) "📶 Connected" else "❌ No WiFi",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isWifiConnected) 
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else 
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                    
                    if (isWifiConnected) {
                        networkName?.let { name ->
    Text(
                                text = "Network: $name",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Device IP Address:",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Text(
                            text = ipAddress ?: "Getting IP...",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        
                        Text(
                            text = "Server Port: 8080",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
            
            // Instructions Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "📋 Instructions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "• Install Moonlight app if not already installed\n" +
                              "• Enable accessibility service below\n" +
                              "• Disable battery optimization below\n" +
                              "• Host PC can send commands to:\n" +
                              "  POST ${ipAddress ?: "[IP]"}:8080/cast\n" +
                              "  POST ${ipAddress ?: "[IP]"}:8080/stop",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // Permissions Section
            Text(
                text = "⚙️ Setup Required",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Battery Optimization Button
            Button(
                onClick = onRequestBatteryOptimization,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Disable Battery Optimization")
            }
            
            // Accessibility Permission Button
            Button(
                onClick = onRequestAccessibilityPermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enable Accessibility Service")
            }
            
            // Debug Test Button
            Button(
                onClick = {
                    // Test broadcast functionality
                    val intent = Intent("com.icurety.mooncast.ACTION_START_CAST")
                    intent.setPackage(context.packageName)
                    intent.putExtra("host_ip", "192.168.1.100")
                    intent.putExtra("host_name", "TestPC")
                    context.sendBroadcast(intent)
                    Log.d("MainActivity", "Test broadcast sent")
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("🧪 Test Cast (Debug)")
            }
            
            // Accessibility Service Test Button
            Button(
                onClick = onTestAccessibilityService,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text("🔧 Test Accessibility Service")
            }
            
            // Minimal Service Check Button (no broadcasts)
            Button(
                onClick = onCheckServiceStatus,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline)
            ) {
                Text("📋 Check Service Status Only")
            }
            
            // Logging Test Button
            Button(
                onClick = {
                    Log.e("LOGGING_TEST", "🔴 ERROR: If you see this, ERROR logs work")
                    Log.w("LOGGING_TEST", "🟡 WARN: If you see this, WARN logs work") 
                    Log.i("LOGGING_TEST", "🔵 INFO: If you see this, INFO logs work")
                    Log.d("LOGGING_TEST", "🟢 DEBUG: If you see this, DEBUG logs work")
                    System.out.println("SYSTEM.OUT: If you see this, System.out works")
                    println("PRINTLN: If you see this, println works")
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("🔍 Test Logging")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Status
            Text(
                text = "Keep this app running to receive cast commands",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MooncastAppPreview() {
    MooncastTheme {
        MooncastApp()
    }
}