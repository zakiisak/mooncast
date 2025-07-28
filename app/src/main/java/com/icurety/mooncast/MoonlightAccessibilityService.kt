package com.icurety.mooncast

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityNodeInfo
import com.icurety.mooncast.HostStorage
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.accessibilityservice.AccessibilityService.GestureResultCallback

class MoonlightAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "MoonlightA11yService"
    }
    
    // Connection state management
    private enum class ConnectionStep {
        LAUNCH_APP,
        FIND_PLUS_BUTTON, 
        ENTER_IP,
        FIND_HOST_BUTTON,
        SELECT_DESKTOP,
        COMPLETED
    }
    
    private var currentStep = ConnectionStep.LAUNCH_APP
    private var targetIP: String? = null
    private var targetHostName: String? = null
    private var heartbeatHandler: Handler? = null
    private var heartbeatRunnable: Runnable? = null
    private var desktopClickRetries = 0
    private val maxDesktopRetries = 3
    
    private val simpleBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.e(TAG, "📻 BROADCAST RECEIVED! Action: ${intent?.action}")
            Log.i(TAG, "📻 Broadcast extras: ${intent?.extras}")
            Log.d(TAG, "📻 Context: $context")
            
            when (intent?.action) {
                "com.icurety.mooncast.ACTION_START_CAST" -> {
                    Log.i(TAG, "📻 Received START_CAST broadcast")
                    val hostIP = intent.getStringExtra("host_ip")
                    val hostName = intent.getStringExtra("host_name")
                    Log.i(TAG, "📻 Host IP: $hostIP, Host Name: $hostName")
                    
                    // Start the automation process
                    if (hostIP != null) {
                        Log.i(TAG, "🚀 Starting Moonlight automation for IP: $hostIP")
                        startMoonlightConnection(hostIP, hostName)
                    } else {
                        Log.e(TAG, "❌ No host IP provided in broadcast")
                    }
                }
                "com.icurety.mooncast.ACTION_STOP_CAST" -> {
                    Log.i(TAG, "📻 Received STOP_CAST broadcast")
                    Log.i(TAG, "🛑 Starting Moonlight stop process")
                    stopMoonlightConnection()
                }
                else -> {
                    Log.w(TAG, "📻 Unknown broadcast action: ${intent?.action}")
                }
            }
            
            Log.d(TAG, "📻 Broadcast processing completed")
        }
    }

    override fun onCreate() {
        // Test logging with multiple levels
        Log.e(TAG, "🔴 ERROR LEVEL LOG - Simple accessibility service onCreate called")
        Log.w(TAG, "🟡 WARN LEVEL LOG - Simple accessibility service onCreate called")
        Log.i(TAG, "🔵 INFO LEVEL LOG - Simple accessibility service onCreate called")
        Log.d(TAG, "🟢 DEBUG LEVEL LOG - Simple accessibility service onCreate called")
        System.out.println("SYSTEM.OUT: MoonlightAccessibilityService onCreate called")
        println("PRINTLN: MoonlightAccessibilityService onCreate called")
        
        Log.d(TAG, "🟢 STEP 1: Starting simple service initialization...")
        
        try {
            Log.d(TAG, "🟢 STEP 2: Calling super.onCreate()...")
            super.onCreate()
            Log.d(TAG, "🟢 STEP 3: super.onCreate() completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ STEP 3 FAILED: super.onCreate() failed", e)
            return
        }
        
        val filter: IntentFilter
        try {
            Log.d(TAG, "🟢 STEP 4: Creating IntentFilter...")
            filter = IntentFilter()
            Log.d(TAG, "🟢 STEP 5: Adding ACTION_START_CAST to filter...")
            filter.addAction("com.icurety.mooncast.ACTION_START_CAST")
            Log.d(TAG, "🟢 STEP 6: Adding ACTION_STOP_CAST to filter...")
            filter.addAction("com.icurety.mooncast.ACTION_STOP_CAST")
            Log.d(TAG, "🟢 STEP 7: Filter created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ STEP 7 FAILED: IntentFilter creation failed", e)
            return
        }
        
        try {
            Log.d(TAG, "🟢 STEP 8: About to register BroadcastReceiver...")
            registerReceiver(simpleBroadcastReceiver, filter)
            Log.d(TAG, "🟢 STEP 9: BroadcastReceiver registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ STEP 9 FAILED: BroadcastReceiver registration failed", e)
            return
        }
        
        Log.d(TAG, "✅ STEP 10: SIMPLE SERVICE STARTED SUCCESSFULLY - All initialization complete")
        
        // Start heartbeat to monitor service lifecycle
        try {
            Log.d(TAG, "🟢 STEP 11: Starting heartbeat monitor...")
            heartbeatHandler = Handler(Looper.getMainLooper())
            heartbeatRunnable = object : Runnable {
                override fun run() {
                    Log.i(TAG, "💓 HEARTBEAT - Service is alive and running")
                    heartbeatHandler?.postDelayed(this, 10000) // Every 10 seconds
                }
            }
            heartbeatHandler?.post(heartbeatRunnable!!)
            Log.d(TAG, "🟢 STEP 12: Heartbeat monitor started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ STEP 12 FAILED: Heartbeat monitor failed to start", e)
        }
        
        Log.d(TAG, "✅ FINAL: All service initialization completed successfully!")
    }

    // Main automation methods
    private fun startMoonlightConnection(hostIP: String, hostName: String?) {
        try {
            Log.i(TAG, "🚀 Starting Moonlight connection to $hostIP")
            targetIP = hostIP
            targetHostName = hostName
            currentStep = ConnectionStep.LAUNCH_APP
            desktopClickRetries = 0 // Reset retry counter for new connection
            
            // Save host mapping for future reference
            if (hostName != null) {
                HostStorage.saveHostMapping(this, hostIP, hostName)
                Log.i(TAG, "💾 Saved host mapping: $hostIP -> $hostName")
            }
            
            // Launch Moonlight app
            val moonlightIntent = packageManager.getLaunchIntentForPackage("com.limelight")
            if (moonlightIntent != null) {
                moonlightIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(moonlightIntent)
                Log.i(TAG, "📱 Moonlight app launched successfully")
                currentStep = ConnectionStep.FIND_PLUS_BUTTON
            } else {
                Log.e(TAG, "❌ Moonlight app not found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error starting Moonlight connection", e)
        }
    }
    
    private fun stopMoonlightConnection() {
        try {
            Log.i(TAG, "🛑 Stopping Moonlight connection")
            currentStep = ConnectionStep.COMPLETED
            
            // Try to close Moonlight and return to Mooncast
            val mooncastIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (mooncastIntent != null) {
                mooncastIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(mooncastIntent)
                Log.i(TAG, "🏠 Returned to Mooncast app")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error stopping Moonlight connection", e)
        }
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (event?.packageName == "com.limelight" && currentStep != ConnectionStep.COMPLETED) {
                Log.d(TAG, "🔍 Moonlight UI event detected: ${event.eventType}")
                handleMoonlightWindow()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in onAccessibilityEvent", e)
        }
    }
    
    private fun handleMoonlightWindow() {
        try {
            val rootNode = rootInActiveWindow
            if (rootNode == null) {
                Log.w(TAG, "⚠️ No active window found")
                return
            }
            
            Log.i(TAG, "🎯 Current step: $currentStep")
            
            when (currentStep) {
                ConnectionStep.FIND_PLUS_BUTTON -> {
                    Log.i(TAG, "🔍 Looking for plus button...")
                    findAndClickPlusButton(rootNode)
                }
                ConnectionStep.ENTER_IP -> {
                    Log.i(TAG, "⌨️ Entering IP address...")
                    enterIPAddress(rootNode)
                }
                ConnectionStep.FIND_HOST_BUTTON -> {
                    Log.i(TAG, "🖥️ Looking for host button...")
                    findAndClickHostButton(rootNode)
                }
                ConnectionStep.SELECT_DESKTOP -> {
                    Log.i(TAG, "🖥️ Selecting Desktop option...")
                    selectDesktopOption(rootNode)
                }
                else -> {
                    Log.d(TAG, "⏸️ No action needed for current step")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling Moonlight window", e)
        }
    }
    
    private fun findAndClickPlusButton(rootNode: AccessibilityNodeInfo) {
        try {
            // Get all clickable nodes
            val clickableNodes = getAllClickableNodes(rootNode)
            Log.i(TAG, "🔍 Found ${clickableNodes.size} clickable nodes")
            
            // Try the 3rd clickable node (index 2) as specified by user
            if (clickableNodes.size >= 3) {
                val thirdClickable = clickableNodes[2]
                Log.i(TAG, "🎯 Attempting to click 3rd clickable node: ${thirdClickable.contentDescription}")
                
                if (thirdClickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    Log.i(TAG, "✅ Successfully clicked plus button (3rd clickable)")
                    currentStep = ConnectionStep.ENTER_IP
                    
                    // Wait for dialog to open
                    Handler(Looper.getMainLooper()).postDelayed({
                        handleMoonlightWindow()
                    }, 1500)
                } else {
                    Log.w(TAG, "⚠️ Failed to click 3rd clickable node")
                }
            } else {
                Log.w(TAG, "⚠️ Not enough clickable nodes found (need at least 3)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error finding plus button", e)
        }
    }
    
    private fun enterIPAddress(rootNode: AccessibilityNodeInfo) {
        try {
            targetIP?.let { ip ->
                Log.i(TAG, "⌨️ Attempting to enter IP: $ip")
                
                // Get fresh root node
                val freshRootNode = rootInActiveWindow
                if (freshRootNode == null) {
                    Log.w(TAG, "⚠️ No fresh root node available")
                    return
                }
                
                // Find edit text field
                val editTextNode = findEditTextNode(freshRootNode)
                if (editTextNode != null) {
                    Log.i(TAG, "📝 Found edit text field")
                    
                    // Focus the field first
                    editTextNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                    
                    // Try to set the text
                    val arguments = android.os.Bundle()
                    arguments.putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, ip)
                    
                    if (editTextNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)) {
                        Log.i(TAG, "✅ Successfully entered IP address")
                        
                        // Look for OK button and click it
                        Handler(Looper.getMainLooper()).postDelayed({
                            findAndClickOKButton()
                        }, 1000)
                    } else {
                        Log.w(TAG, "⚠️ Failed to set text, trying alternative method")
                        // Alternative: clear and try again
                        editTextNode.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, arguments.apply {
                            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
                            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, editTextNode.text?.length ?: 0)
                        })
                        
                        Handler(Looper.getMainLooper()).postDelayed({
                            val freshRoot2 = rootInActiveWindow
                            freshRoot2?.let { root ->
                                val editField = findEditTextNode(root)
                                editField?.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                            }
                        }, 500)
                    }
                } else {
                    Log.w(TAG, "⚠️ Edit text field not found")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error entering IP address", e)
        }
    }
    
    private fun findAndClickOKButton() {
        try {
            val freshRootNode = rootInActiveWindow
            if (freshRootNode == null) {
                Log.w(TAG, "⚠️ No fresh root node for OK button")
                return
            }
            
            val okButton = findNodeByText(freshRootNode, "OK") ?: 
                          findNodeByText(freshRootNode, "Add") ?: 
                          findNodeByText(freshRootNode, "Connect")
            
            if (okButton != null) {
                Log.i(TAG, "🎯 Found OK button")
                if (okButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    Log.i(TAG, "✅ Successfully clicked OK button")
                    currentStep = ConnectionStep.FIND_HOST_BUTTON
                    
                    // Wait for host list to load
                    Handler(Looper.getMainLooper()).postDelayed({
                        handleMoonlightWindow()
                    }, 2000)
                }
            } else {
                Log.w(TAG, "⚠️ OK button not found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error clicking OK button", e)
        }
    }
    
    private fun findAndClickHostButton(rootNode: AccessibilityNodeInfo) {
        try {
            val hostName = targetHostName ?: HostStorage.getHostName(this, targetIP ?: "")
            
            if (hostName != null) {
                Log.i(TAG, "🔍 Looking for host button with name: $hostName")
                val hostButton = findNodeByText(rootNode, hostName)
                
                if (hostButton != null) {
                    Log.i(TAG, "🎯 Found host button")
                    Log.d(TAG, "🔍 Host button details: className=${hostButton.className}, isClickable=${hostButton.isClickable}, isEnabled=${hostButton.isEnabled}")
                    Log.d(TAG, "🔍 Host button text: '${hostButton.text}', contentDesc: '${hostButton.contentDescription}'")
                    
                    // Try multiple click methods
                    var clickSuccess = false
                    
                    // Method 1: Standard click
                    Log.d(TAG, "🔄 Attempting standard ACTION_CLICK...")
                    if (hostButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        Log.i(TAG, "✅ Successfully clicked host button with standard click")
                        clickSuccess = true
                    } else {
                        Log.w(TAG, "⚠️ Standard click failed, trying alternatives...")
                        
                        // Method 2: Try clicking the parent
                        val parent = hostButton.parent
                        if (parent != null && parent.isClickable) {
                            Log.d(TAG, "🔄 Attempting to click parent node...")
                            if (parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                                Log.i(TAG, "✅ Successfully clicked host button via parent")
                                clickSuccess = true
                            } else {
                                Log.w(TAG, "⚠️ Parent click also failed")
                            }
                        }
                        
                        // Method 3: Try focusing first then clicking
                        if (!clickSuccess) {
                            Log.d(TAG, "🔄 Attempting focus + click...")
                            hostButton.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                            Handler(Looper.getMainLooper()).postDelayed({
                                if (hostButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                                    Log.i(TAG, "✅ Successfully clicked host button after focus")
                                    currentStep = ConnectionStep.SELECT_DESKTOP
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        handleMoonlightWindow()
                                    }, 1500)
                                } else {
                                    Log.e(TAG, "❌ All click methods failed for host button")
                                }
                            }, 500)
                            return // Exit early since we're handling async
                        }
                    }
                    
                    if (clickSuccess) {
                        Log.i(TAG, "🎉 Host button clicked successfully - proceeding to desktop selection")
                        currentStep = ConnectionStep.SELECT_DESKTOP
                        
                        // Wait for desktop options to appear
                        Handler(Looper.getMainLooper()).postDelayed({
                            handleMoonlightWindow()
                        }, 1500)
                    }
                } else {
                    Log.w(TAG, "⚠️ Host button not found for: $hostName")
                    // Let's also try finding all buttons for debugging
                    Log.d(TAG, "🔍 Available buttons:")
                    findAllButtons(rootNode).forEachIndexed { index, button ->
                        Log.d(TAG, "  Button $index: text='${button.text}', desc='${button.contentDescription}'")
                    }
                }
            } else {
                Log.w(TAG, "⚠️ No host name available")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error finding host button", e)
        }
    }
    
    private fun selectDesktopOption(rootNode: AccessibilityNodeInfo) {
        try {
            // Prevent excessive retries
            if (desktopClickRetries >= maxDesktopRetries) {
                Log.e(TAG, "❌ Maximum Desktop click retries reached ($maxDesktopRetries), giving up")
                currentStep = ConnectionStep.COMPLETED
                return
            }
            
            desktopClickRetries++
            Log.i(TAG, "🔍 Looking for Desktop button... (Attempt $desktopClickRetries/$maxDesktopRetries)")
            Log.d(TAG, "🔍 Root node details: $rootNode")
            
            // First, let's see ALL text nodes on screen
            Log.d(TAG, "🔍 === SCANNING ALL NODES FOR DESKTOP ===")
            scanAllNodesForText(rootNode, "Desktop")
            scanAllNodesForText(rootNode, "DESKTOP")
            Log.d(TAG, "🔍 === END DESKTOP SCAN ===")
            
            // Look for Desktop button with more specific criteria
            val desktopButton = findSpecificDesktopButton(rootNode)
            
            if (desktopButton != null) {
                Log.i(TAG, "🎯 Found Desktop button")
                Log.e(TAG, "🔍 DESKTOP BUTTON ANALYSIS:")
                Log.e(TAG, "  - Button object: $desktopButton")
                Log.e(TAG, "  - className: ${desktopButton.className}")
                Log.e(TAG, "  - isClickable: ${desktopButton.isClickable}")
                Log.e(TAG, "  - isEnabled: ${desktopButton.isEnabled}")
                Log.e(TAG, "  - isFocusable: ${desktopButton.isFocusable}")
                Log.e(TAG, "  - isVisibleToUser: ${desktopButton.isVisibleToUser}")
                Log.e(TAG, "  - text: '${desktopButton.text}'")
                Log.e(TAG, "  - contentDescription: '${desktopButton.contentDescription}'")
                Log.e(TAG, "  - parent: ${desktopButton.parent}")
                Log.e(TAG, "  - childCount: ${desktopButton.childCount}")
                
                // Get button bounds for gesture clicking
                val bounds = Rect()
                try {
                    desktopButton.getBoundsInScreen(bounds)
                    Log.e(TAG, "  - bounds: $bounds")
                    Log.e(TAG, "  - center: (${bounds.centerX()}, ${bounds.centerY()})")
                    Log.e(TAG, "  - width: ${bounds.width()}, height: ${bounds.height()}")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error getting button bounds", e)
                }
                
                // Check parent details too
                try {
                    val parent = desktopButton.parent
                    if (parent != null) {
                        Log.e(TAG, "🔍 PARENT ANALYSIS:")
                        Log.e(TAG, "  - Parent className: ${parent.className}")
                        Log.e(TAG, "  - Parent isClickable: ${parent.isClickable}")
                        Log.e(TAG, "  - Parent isEnabled: ${parent.isEnabled}")
                        Log.e(TAG, "  - Parent text: '${parent.text}'")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error analyzing parent", e)
                }
                
                // Try multiple click methods with detailed logging
                var clickSuccess = false
                
                // Method 1: Standard click
                Log.d(TAG, "🔄 Attempting standard ACTION_CLICK on Desktop...")
                try {
                    val clickResult = desktopButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Log.e(TAG, "🔄 Standard click result: $clickResult")
                    if (clickResult) {
                        Log.i(TAG, "✅ Successfully clicked Desktop button with standard click")
                        clickSuccess = true
                    } else {
                        Log.w(TAG, "⚠️ Standard click failed on Desktop, trying alternatives...")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Exception during standard click", e)
                }
                
                // Method 2: Try clicking the parent
                if (!clickSuccess) {
                    try {
                        val parent = desktopButton.parent
                        if (parent != null && parent.isClickable) {
                            Log.d(TAG, "🔄 Attempting to click Desktop parent node...")
                            if (parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                                Log.i(TAG, "✅ Successfully clicked Desktop button via parent")
                                clickSuccess = true
                            } else {
                                Log.w(TAG, "⚠️ Desktop parent click also failed")
                            }
                        } else {
                            Log.d(TAG, "🔍 Parent is null or not clickable")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Exception during parent click", e)
                    }
                }
                
                // Method 3: Try focusing first then clicking
                if (!clickSuccess) {
                    Log.d(TAG, "🔄 Attempting focus + click on Desktop...")
                    try {
                        desktopButton.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                if (desktopButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                                    Log.i(TAG, "✅ Successfully clicked Desktop button after focus")
                                    Log.i(TAG, "🎉 Desktop button clicked successfully!")
                                    
                                    // Wait a moment for potential dialog to appear
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        checkForSessionDialog()
                                    }, 1500)
                                } else {
                                    Log.w(TAG, "⚠️ Focus + click also failed, trying simple coordinate simulation...")
                                    // Instead of complex gesture, try a simple approach
                                    trySimpleDesktopClick(bounds)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Exception during focus + click", e)
                                trySimpleDesktopClick(bounds)
                            }
                        }, 500)
                        return // Exit early since we're handling async
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Exception during focus attempt", e)
                    }
                }
                
                if (clickSuccess) {
                    Log.i(TAG, "🎉 Desktop button clicked successfully!")
                    
                    // Wait a moment for potential dialog to appear
                    Handler(Looper.getMainLooper()).postDelayed({
                        checkForSessionDialog()
                    }, 1500)
                } else {
                    // Method 4: Try ImageButtons by position (Desktop is likely the first one)
                    Log.d(TAG, "🔄 Attempting gesture-based click on Desktop...")
                    trySimpleDesktopClick(bounds)
                    return
                }
            } else {
                Log.w(TAG, "⚠️ Desktop button not found")
                // Debug: Show all available options
                Log.d(TAG, "🔍 Available clickable options:")
                try {
                    findAllButtons(rootNode).forEachIndexed { index, button ->
                        Log.d(TAG, "  Option $index: text='${button.text}', desc='${button.contentDescription}', class='${button.className}'")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error listing buttons", e)
                }
                
                // Try to find "DESKTOP" text differently
                Log.d(TAG, "🔍 Trying alternative Desktop search...")
                val desktopAlt = findNodeByText(rootNode, "DESKTOP") // Try uppercase
                if (desktopAlt != null) {
                    Log.i(TAG, "🎯 Found DESKTOP (uppercase) button")
                    try {
                        if (desktopAlt.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                            Log.i(TAG, "✅ Successfully clicked DESKTOP button")
                            currentStep = ConnectionStep.COMPLETED
                            desktopClickRetries = 0
                            return
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Exception clicking DESKTOP button", e)
                    }
                }
                
                // Wait and retry
                if (desktopClickRetries < maxDesktopRetries) {
                    Log.d(TAG, "🔄 Retrying Desktop selection in 2 seconds...")
                    Handler(Looper.getMainLooper()).postDelayed({
                        handleMoonlightWindow()
                    }, 2000)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error selecting desktop option", e)
        }
    }
    
    private fun trySimpleDesktopClick(bounds: Rect) {
        try {
            Log.d(TAG, "🔄 Trying simple approach - looking for any 'Desktop' clickable node...")
            
            // Get fresh root and try one more simple click attempt
            val freshRoot = rootInActiveWindow
            if (freshRoot != null) {
                val simpleDesktop = findNodeByText(freshRoot, "Desktop")
                if (simpleDesktop?.isClickable == true) {
                    Log.d(TAG, "🔄 One final simple click attempt...")
                    if (simpleDesktop.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        Log.i(TAG, "✅ Simple Desktop click succeeded!")
                        
                        // Wait for potential session dialog
                        Handler(Looper.getMainLooper()).postDelayed({
                            checkForSessionDialog()
                        }, 1500)
                        desktopClickRetries = 0
                        return
                    }
                }
            }
            
            Log.w(TAG, "⚠️ All Desktop click methods exhausted")
            if (desktopClickRetries < maxDesktopRetries) {
                Handler(Looper.getMainLooper()).postDelayed({
                    handleMoonlightWindow()
                }, 2000)
            } else {
                Log.e(TAG, "❌ Giving up on Desktop button after all attempts")
                currentStep = ConnectionStep.COMPLETED
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in simple desktop click", e)
        }
    }
    
    // Helper methods
    private fun getAllClickableNodes(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val clickableNodes = mutableListOf<AccessibilityNodeInfo>()
        
        if (node.isClickable) {
            clickableNodes.add(node)
        }
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                clickableNodes.addAll(getAllClickableNodes(child))
            }
        }
        
        return clickableNodes
    }
    
    private fun findEditTextNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.className == "android.widget.EditText") {
            return node
        }
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                val result = findEditTextNode(child)
                if (result != null) return result
            }
        }
        
        return null
    }
    
    private fun findNodeByText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        if (node.text?.toString()?.contains(text, ignoreCase = true) == true ||
            node.contentDescription?.toString()?.contains(text, ignoreCase = true) == true) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                val result = findNodeByText(child, text)
                if (result != null) return result
            }
        }
        
        return null
    }

    private fun findAllButtons(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val buttons = mutableListOf<AccessibilityNodeInfo>()
        
        // Check if this node is a clickable element (button, view, etc.)
        if (node.isClickable || node.className?.contains("Button") == true) {
            buttons.add(node)
        }
        
        // Recursively check children
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                buttons.addAll(findAllButtons(child))
            }
        }
        
        return buttons
    }

    private fun scanAllNodesForText(node: AccessibilityNodeInfo, text: String) {
        if (node.text?.toString()?.contains(text, ignoreCase = true) == true ||
            node.contentDescription?.toString()?.contains(text, ignoreCase = true) == true) {
            Log.d(TAG, "🔍 Found text node: ${node.text} (Desc: ${node.contentDescription})")
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                scanAllNodesForText(child, text)
            }
        }
    }

    private fun findSpecificDesktopButton(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        Log.d(TAG, "🔍 === SEARCHING FOR SPECIFIC DESKTOP BUTTON ===")
        
        // First, let's see ALL clickable elements on screen
        val allClickable = getAllClickableNodes(rootNode)
        Log.e(TAG, "🔍 ALL CLICKABLE ELEMENTS ON SCREEN:")
        allClickable.forEachIndexed { index, node ->
            Log.e(TAG, "  Clickable $index: text='${node.text}', desc='${node.contentDescription}', class='${node.className}'")
        }
        
        // Method 1: Look for exact "Desktop" text (not part of PC name)
        Log.d(TAG, "🔍 Method 1: Looking for exact 'Desktop' text...")
        for (node in allClickable) {
            val text = node.text?.toString() ?: ""
            val desc = node.contentDescription?.toString() ?: ""
            
            // Exact match for "Desktop" (case insensitive)
            if (text.equals("Desktop", ignoreCase = true) || desc.equals("Desktop", ignoreCase = true)) {
                Log.i(TAG, "✅ Found exact Desktop match: text='$text', desc='$desc'")
                return node
            }
        }
        
        // Method 2: Look for "DESKTOP" standalone (not as part of hostname)
        Log.d(TAG, "🔍 Method 2: Looking for standalone 'DESKTOP' text...")
        for (node in allClickable) {
            val text = node.text?.toString() ?: ""
            val desc = node.contentDescription?.toString() ?: ""
            
            if (text.equals("DESKTOP", ignoreCase = true) || desc.equals("DESKTOP", ignoreCase = true)) {
                Log.i(TAG, "✅ Found standalone DESKTOP match: text='$text', desc='$desc'")
                return node
            }
        }
        
        // Method 3: Look in ALL nodes (not just clickable) for exact matches
        Log.d(TAG, "🔍 Method 3: Scanning all nodes for Desktop options...")
        val allDesktopNodes = findAllDesktopNodes(rootNode)
        Log.e(TAG, "🔍 ALL NODES CONTAINING 'DESKTOP':")
        allDesktopNodes.forEachIndexed { index, node ->
            Log.e(TAG, "  Desktop Node $index: text='${node.text}', desc='${node.contentDescription}', clickable=${node.isClickable}, class='${node.className}'")
        }
        
        // Try to find the clickable parent of a Desktop text node
        for (node in allDesktopNodes) {
            val text = node.text?.toString() ?: ""
            if (text.equals("Desktop", ignoreCase = true) || text.equals("DESKTOP", ignoreCase = true)) {
                // Check if this node or its parents are clickable
                var current: AccessibilityNodeInfo? = node
                var depth = 0
                while (current != null && depth < 5) { // Check up to 5 levels up
                    if (current.isClickable) {
                        Log.i(TAG, "✅ Found clickable parent at depth $depth for Desktop text")
                        return current
                    }
                    current = current.parent
                    depth++
                }
            }
        }
        
        // Method 4: Try ImageButtons by position (Desktop is likely the first one)
        Log.d(TAG, "🔍 Method 4: Trying ImageButtons by position...")
        val imageButtons = allClickable.filter { it.className == "android.widget.ImageButton" }
        Log.e(TAG, "🔍 Found ${imageButtons.size} ImageButtons")
        
        if (imageButtons.isNotEmpty()) {
            Log.i(TAG, "🎯 Attempting first ImageButton (likely Desktop based on layout)")
            return imageButtons[0] // Desktop is on the left in the screenshot
        }
        
        // Method 5: Try RelativeLayouts (Desktop might be wrapped in a layout)
        Log.d(TAG, "🔍 Method 5: Trying RelativeLayouts...")
        val relativeLayouts = allClickable.filter { it.className == "android.widget.RelativeLayout" }
        Log.e(TAG, "🔍 Found ${relativeLayouts.size} RelativeLayouts")
        
        if (relativeLayouts.isNotEmpty()) {
            Log.i(TAG, "🎯 Attempting first RelativeLayout (might contain Desktop)")
            return relativeLayouts[0]
        }
        
        Log.w(TAG, "❌ No specific Desktop button found")
        return null
    }
    
    private fun findAllDesktopNodes(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val desktopNodes = mutableListOf<AccessibilityNodeInfo>()
        
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        
        if (text.contains("desktop", ignoreCase = true) || desc.contains("desktop", ignoreCase = true)) {
            desktopNodes.add(node)
        }
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                desktopNodes.addAll(findAllDesktopNodes(child))
            }
        }
        
        return desktopNodes
    }
    
    private fun checkForSessionDialog() {
        try {
            Log.i(TAG, "🔍 Checking for session dialog after Desktop click...")
            
            val rootNode = rootInActiveWindow
            if (rootNode == null) {
                Log.w(TAG, "⚠️ No active window found for session dialog check")
                // Assume streaming started directly (new session)
                Log.i(TAG, "🎉 MOONLIGHT CONNECTION COMPLETED! (No dialog - direct start)")
                currentStep = ConnectionStep.COMPLETED
                desktopClickRetries = 0
                return
            }
            
            // Look for session dialog options
            Log.d(TAG, "🔍 Scanning for session dialog options...")
            
            // Check for "Resume Session" button
            val resumeButton = findNodeByText(rootNode, "Resume Session")
            if (resumeButton != null) {
                Log.i(TAG, "🎯 Found 'Resume Session' button - attempting enhanced click methods")
                
                // Use the same enhanced clicking approach as Desktop button
                if (performEnhancedClick(resumeButton, "Resume Session")) {
                    Log.i(TAG, "✅ Successfully clicked Resume Session")
                    Log.i(TAG, "🎉 MOONLIGHT SESSION RESUMED! Streaming should start now.")
                    currentStep = ConnectionStep.COMPLETED
                    desktopClickRetries = 0
                    return
                } else {
                    Log.w(TAG, "⚠️ Failed to click Resume Session button with all methods")
                }
            }
            
            // Check for "Start Session" or similar (new session)
            val startButton = findNodeByText(rootNode, "Start Session") ?: 
                             findNodeByText(rootNode, "Start") ?: 
                             findNodeByText(rootNode, "Connect")
            
            if (startButton != null) {
                Log.i(TAG, "🎯 Found start session button - attempting enhanced click methods")
                if (performEnhancedClick(startButton, "Start Session")) {
                    Log.i(TAG, "✅ Successfully clicked start session button")
                    Log.i(TAG, "🎉 MOONLIGHT NEW SESSION STARTED! Streaming should start now.")
                    currentStep = ConnectionStep.COMPLETED
                    desktopClickRetries = 0
                    return
                } else {
                    Log.w(TAG, "⚠️ Failed to click start session button with all methods")
                }
            }
            
            // If no dialog found, assume direct connection (some setups start immediately)
            Log.d(TAG, "🔍 No session dialog found - checking if connection started directly")
            
            // Wait a bit more and check if we're in streaming mode
            Handler(Looper.getMainLooper()).postDelayed({
                // Check if the current window is different (indicating streaming started)
                val currentWindow = rootInActiveWindow
                if (currentWindow?.packageName != "com.limelight") {
                    Log.i(TAG, "🎉 MOONLIGHT CONNECTION COMPLETED! (Direct connection - no dialog)")
                    currentStep = ConnectionStep.COMPLETED
                    desktopClickRetries = 0
                } else {
                    // Still in Moonlight app, might need more investigation
                    Log.w(TAG, "⚠️ Still in Moonlight app - session dialog might not have been handled")
                    showAvailableOptions(currentWindow)
                    
                    // Mark as completed anyway to prevent infinite loops
                    currentStep = ConnectionStep.COMPLETED
                    desktopClickRetries = 0
                }
            }, 2000)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking for session dialog", e)
            // Mark as completed to prevent infinite loops
            currentStep = ConnectionStep.COMPLETED
            desktopClickRetries = 0
        }
    }
    
    private fun showAvailableOptions(rootNode: AccessibilityNodeInfo?) {
        try {
            if (rootNode == null) return
            
            Log.d(TAG, "🔍 === AVAILABLE OPTIONS ON SCREEN ===")
            val allClickable = getAllClickableNodes(rootNode)
            allClickable.forEachIndexed { index, node ->
                Log.d(TAG, "  Option $index: text='${node.text}', desc='${node.contentDescription}', class='${node.className}'")
            }
            
            val allText = findAllTextNodes(rootNode)
            Log.d(TAG, "🔍 === ALL TEXT ON SCREEN ===")
            allText.forEachIndexed { index, node ->
                Log.d(TAG, "  Text $index: '${node.text}', desc='${node.contentDescription}'")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error showing available options", e)
        }
    }
    
    private fun findAllTextNodes(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val textNodes = mutableListOf<AccessibilityNodeInfo>()
        
        if (!node.text.isNullOrEmpty() || !node.contentDescription.isNullOrEmpty()) {
            textNodes.add(node)
        }
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                textNodes.addAll(findAllTextNodes(child))
            }
        }
        
        return textNodes
    }
    
    private fun performEnhancedClick(button: AccessibilityNodeInfo, buttonName: String): Boolean {
        try {
            Log.e(TAG, "🔍 ENHANCED CLICK ANALYSIS FOR $buttonName:")
            Log.e(TAG, "  - Button object: $button")
            Log.e(TAG, "  - className: ${button.className}")
            Log.e(TAG, "  - isClickable: ${button.isClickable}")
            Log.e(TAG, "  - isEnabled: ${button.isEnabled}")
            Log.e(TAG, "  - isFocusable: ${button.isFocusable}")
            Log.e(TAG, "  - isVisibleToUser: ${button.isVisibleToUser}")
            Log.e(TAG, "  - text: '${button.text}'")
            Log.e(TAG, "  - contentDescription: '${button.contentDescription}'")
            
            // Get button bounds
            val bounds = Rect()
            try {
                button.getBoundsInScreen(bounds)
                Log.e(TAG, "  - bounds: $bounds")
                Log.e(TAG, "  - center: (${bounds.centerX()}, ${bounds.centerY()})")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error getting button bounds", e)
            }
            
            // Check parent details
            try {
                val parent = button.parent
                if (parent != null) {
                    Log.e(TAG, "🔍 PARENT ANALYSIS FOR $buttonName:")
                    Log.e(TAG, "  - Parent className: ${parent.className}")
                    Log.e(TAG, "  - Parent isClickable: ${parent.isClickable}")
                    Log.e(TAG, "  - Parent isEnabled: ${parent.isEnabled}")
                    Log.e(TAG, "  - Parent text: '${parent.text}'")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error analyzing parent", e)
            }
            
            // Method 1: Standard click
            Log.d(TAG, "🔄 Method 1: Attempting standard ACTION_CLICK on $buttonName...")
            try {
                val clickResult = button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.e(TAG, "🔄 Standard click result: $clickResult")
                if (clickResult) {
                    Log.i(TAG, "✅ Successfully clicked $buttonName with standard click")
                    return true
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during standard click", e)
            }
            
            // Method 2: Try clicking the parent
            Log.d(TAG, "🔄 Method 2: Attempting parent click for $buttonName...")
            try {
                val parent = button.parent
                if (parent != null && parent.isClickable) {
                    Log.d(TAG, "🔄 Parent is clickable, attempting parent click...")
                    if (parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        Log.i(TAG, "✅ Successfully clicked $buttonName via parent")
                        return true
                    } else {
                        Log.w(TAG, "⚠️ Parent click failed for $buttonName")
                    }
                } else {
                    Log.d(TAG, "🔍 Parent is null or not clickable for $buttonName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during parent click", e)
            }
            
            // Method 3: Focus + click
            Log.d(TAG, "🔄 Method 3: Attempting focus + click for $buttonName...")
            try {
                button.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                Thread.sleep(500) // Brief wait for focus
                if (button.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    Log.i(TAG, "✅ Successfully clicked $buttonName after focus")
                    return true
                } else {
                    Log.w(TAG, "⚠️ Focus + click failed for $buttonName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during focus + click", e)
            }
            
            // Method 4: Try position-based clicking for session buttons
            Log.d(TAG, "🔄 Method 4: Attempting position-based click for $buttonName...")
            try {
                val rootNode = rootInActiveWindow
                if (rootNode != null) {
                    val allClickable = getAllClickableNodes(rootNode)
                    Log.e(TAG, "🔍 ALL CLICKABLE ELEMENTS FOR SESSION DIALOG:")
                    allClickable.forEachIndexed { index, node ->
                        Log.e(TAG, "  Clickable $index: text='${node.text}', desc='${node.contentDescription}', class='${node.className}'")
                    }
                    
                    // For Resume Session, try the first clickable element
                    if (buttonName.contains("Resume") && allClickable.isNotEmpty()) {
                        Log.i(TAG, "🎯 Attempting first clickable element for Resume Session")
                        if (allClickable[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                            Log.i(TAG, "✅ Successfully clicked $buttonName via position")
                            return true
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during position-based click", e)
            }
            
            Log.w(TAG, "❌ All enhanced click methods failed for $buttonName")
            return false
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in performEnhancedClick for $buttonName", e)
            return false
        }
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "⏸️ Service interrupted")
    }
    
    override fun onServiceConnected() {
        Log.e(TAG, "🔗 onServiceConnected called")
        super.onServiceConnected()
        Log.d(TAG, "🔗 onServiceConnected completed")
    }

    override fun onDestroy() {
        Log.e(TAG, "💀 onDestroy called - Service is being destroyed")
        
        // Stop heartbeat
        try {
            Log.d(TAG, "💀 Stopping heartbeat monitor...")
            heartbeatRunnable?.let { runnable ->
                heartbeatHandler?.removeCallbacks(runnable)
            }
            heartbeatHandler = null
            heartbeatRunnable = null
            Log.d(TAG, "💀 Heartbeat monitor stopped successfully")
        } catch (e: Exception) {
            Log.e(TAG, "💀 Error stopping heartbeat monitor", e)
        }
        
        // Unregister broadcast receiver
        try {
            Log.d(TAG, "💀 Unregistering BroadcastReceiver...")
            unregisterReceiver(simpleBroadcastReceiver)
            Log.d(TAG, "💀 BroadcastReceiver unregistered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "💀 Error unregistering BroadcastReceiver", e)
        }
        
        super.onDestroy()
        Log.d(TAG, "💀 onDestroy completed - Service fully destroyed")
    }
} 