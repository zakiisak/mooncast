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
    private var sessionDialogCheckInProgress = false
    
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
                    
                    // Wait longer for screen transition and host list to fully load
                    Log.i(TAG, "⏳ Waiting for host list screen to load completely...")
                    Handler(Looper.getMainLooper()).postDelayed({
                        // Verify we're actually on the host list screen before proceeding
                        validateHostListScreenAndProceed()
                    }, 4000) // Increased from 2000 to 4000ms
                }
            } else {
                Log.w(TAG, "⚠️ OK button not found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error clicking OK button", e)
        }
    }
    
    private fun validateHostListScreenAndProceed() {
        try {
            Log.i(TAG, "🔍 Validating that we're on the host list screen...")
            val rootNode = rootInActiveWindow
            
            if (rootNode == null) {
                Log.w(TAG, "⚠️ No root node - retrying in 2 seconds...")
                Handler(Looper.getMainLooper()).postDelayed({
                    validateHostListScreenAndProceed()
                }, 2000)
                return
            }
            
            // Check for indicators that we're on the host list screen
            val allClickable = getAllClickableNodes(rootNode)
            Log.e(TAG, "🔍 Screen validation - Found ${allClickable.size} clickable elements")
            
            // Ensure we're still in Moonlight app (not Chrome or other apps)
            val packageName = rootNode.packageName?.toString() ?: ""
            val isInMoonlight = packageName == "com.limelight"
            
            if (!isInMoonlight) {
                Log.w(TAG, "⚠️ Not in Moonlight app (package: $packageName) - waiting to return...")
                Handler(Looper.getMainLooper()).postDelayed({
                    validateHostListScreenAndProceed()
                }, 3000)
                return
            }
            
            // Look for GridView (hosts container) or PC name text
            val hasGridView = allClickable.any { it.className?.contains("GridView") == true }
            val hasHostText = findNodeByText(rootNode, targetHostName ?: "DESKTOP") != null
            val hasSettingsButton = allClickable.any { 
                it.className?.contains("ImageButton") == true && 
                it.toString().contains("settingsButton")
            }
            
            // More strict validation - require minimum elements AND proper content
            val hasMinimumElements = allClickable.size >= 3
            val hasProperContent = hasGridView && hasHostText
            
            Log.e(TAG, "🔍 Screen indicators: hasGridView=$hasGridView, hasHostText=$hasHostText, hasSettingsButton=$hasSettingsButton")
            Log.e(TAG, "🔍 Validation: hasMinimumElements=$hasMinimumElements, hasProperContent=$hasProperContent, isInMoonlight=$isInMoonlight")
            
            if (hasProperContent && hasMinimumElements) {
                Log.i(TAG, "✅ Confirmed on host list screen with proper content - proceeding with host button search")
                handleMoonlightWindow()
            } else if (hasSettingsButton) {
                Log.w(TAG, "⚠️ Still seeing settings button - screen hasn't transitioned yet, waiting longer...")
                Handler(Looper.getMainLooper()).postDelayed({
                    validateHostListScreenAndProceed()
                }, 3000)
            } else if (!hasMinimumElements) {
                Log.w(TAG, "⚠️ Too few clickable elements (${allClickable.size}) - screen still loading...")
                Handler(Looper.getMainLooper()).postDelayed({
                    validateHostListScreenAndProceed()
                }, 2000)
            } else if (!hasProperContent) {
                Log.w(TAG, "⚠️ Missing GridView or host text - content not fully loaded...")
                Handler(Looper.getMainLooper()).postDelayed({
                    validateHostListScreenAndProceed()
                }, 2000)
            } else {
                Log.i(TAG, "🤔 Uncertain screen state but trying anyway...")
                handleMoonlightWindow()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error validating screen state", e)
            // Fallback to original behavior
            handleMoonlightWindow()
        }
    }
    
    private fun findAndClickHostButton(rootNode: AccessibilityNodeInfo) {
        try {
            val hostName = targetHostName ?: HostStorage.getHostName(this, targetIP ?: "")
            
            if (hostName != null) {
                Log.i(TAG, "🔍 Looking for host button with name: $hostName")
                
                // Add comprehensive debugging to understand the current screen
                Log.e(TAG, "=== FULL SCREEN ANALYSIS ===")
                dumpFullScreenContents(rootNode)
                Log.e(TAG, "=== END SCREEN ANALYSIS ===")
                
                val hostButton = findHostButtonSpecific(rootNode, hostName)
                
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
                    
                    // Try a more aggressive approach - just click the GridView or RelativeLayout
                    Log.e(TAG, "🔄 FALLBACK: Trying to click containers directly...")
                    val allClickable = getAllClickableNodes(rootNode)
                    
                    // Check if we're possibly on the wrong screen (settings screen) 
                    val hasSettingsButton = allClickable.any { 
                        it.toString().contains("settingsButton", ignoreCase = true) 
                    }
                    
                    if (hasSettingsButton) {
                        Log.w(TAG, "⚠️ Detected settings button - we might be on wrong screen, waiting longer...")
                        Handler(Looper.getMainLooper()).postDelayed({
                            // Try to go back or wait for correct screen
                            Log.i(TAG, "🔄 Retrying host button search after settings screen delay...")
                            handleMoonlightWindow()
                        }, 3000)
                        return
                    }
                    
                    // Try GridView first (common container for hosts)
                    val gridView = allClickable.find { it.className?.contains("GridView") == true }
                    if (gridView != null) {
                        Log.e(TAG, "🎯 Found GridView, attempting click...")
                        if (performEnhancedClick(gridView, "GridView Container")) {
                            Log.i(TAG, "✅ Successfully clicked GridView container")
                            currentStep = ConnectionStep.SELECT_DESKTOP
                            Handler(Looper.getMainLooper()).postDelayed({
                                handleMoonlightWindow()
                            }, 1500)
                            return
                        }
                    }
                    
                    // Try RelativeLayout second
                    val relativeLayout = allClickable.find { it.className?.contains("RelativeLayout") == true }
                    if (relativeLayout != null) {
                        Log.e(TAG, "🎯 Found RelativeLayout, attempting click...")
                        if (performEnhancedClick(relativeLayout, "RelativeLayout Container")) {
                            Log.i(TAG, "✅ Successfully clicked RelativeLayout container")
                            currentStep = ConnectionStep.SELECT_DESKTOP
                            Handler(Looper.getMainLooper()).postDelayed({
                                handleMoonlightWindow()
                            }, 1500)
                            return
                        }
                    }
                    
                    // Try clicking any non-ImageButton element as last resort
                    val nonImageButton = allClickable.find { 
                        !(it.className?.contains("ImageButton") == true)
                    }
                    if (nonImageButton != null) {
                        Log.e(TAG, "🎯 Trying non-ImageButton as last resort...")
                        if (performEnhancedClick(nonImageButton, "Last Resort Button")) {
                            Log.i(TAG, "✅ Successfully clicked last resort button")
                            currentStep = ConnectionStep.SELECT_DESKTOP
                            Handler(Looper.getMainLooper()).postDelayed({
                                handleMoonlightWindow()
                            }, 1500)
                            return
                        }
                    }
                    
                    Log.e(TAG, "❌ All fallback methods failed - might need to check if we're on the right screen")
                    
                    // Don't proceed to desktop selection if we have very few elements or wrong screen
                    if (allClickable.size < 2) {
                        Log.w(TAG, "⚠️ Too few clickable elements (${allClickable.size}) - screen not ready, waiting longer...")
                        Handler(Looper.getMainLooper()).postDelayed({
                            Log.i(TAG, "🔄 Retrying host button search after element loading delay...")
                            handleMoonlightWindow()
                        }, 3000)
                        return
                    }
                    
                    // Check if we're possibly on the wrong screen or if the host was already added
                    val packageName = rootNode.packageName?.toString() ?: ""
                    if (packageName != "com.limelight") {
                        Log.w(TAG, "⚠️ Not in Moonlight app (package: $packageName) - waiting to return...")
                        Handler(Looper.getMainLooper()).postDelayed({
                            Log.i(TAG, "🔄 Retrying host button search after returning to Moonlight...")
                            handleMoonlightWindow()
                        }, 3000)
                        return
                    }
                    
                    Log.e(TAG, "🔍 Proceeding to Desktop selection as last resort...")
                    currentStep = ConnectionStep.SELECT_DESKTOP
                    Handler(Looper.getMainLooper()).postDelayed({
                        handleMoonlightWindow()
                    }, 2000)
                }
            } else {
                Log.w(TAG, "⚠️ No host name available - trying to proceed anyway")
                // Even without host name, try to proceed
                currentStep = ConnectionStep.SELECT_DESKTOP
                Handler(Looper.getMainLooper()).postDelayed({
                    handleMoonlightWindow()
                }, 2000)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error finding host button", e)
        }
    }
    
    private fun selectDesktopOption(rootNode: AccessibilityNodeInfo) {
        try {
            // Ensure we're still in Moonlight app (not Chrome or other apps)
            val packageName = rootNode.packageName?.toString() ?: ""
            if (packageName != "com.limelight") {
                Log.w(TAG, "⚠️ Not in Moonlight app during Desktop selection (package: $packageName) - waiting to return...")
                Handler(Looper.getMainLooper()).postDelayed({
                    Log.i(TAG, "🔄 Retrying Desktop selection after returning to Moonlight...")
                    handleMoonlightWindow()
                }, 3000)
                return
            }
            
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
    
    private fun findHostButtonSpecific(node: AccessibilityNodeInfo, hostName: String): AccessibilityNodeInfo? {
        // More specific host button detection to avoid settings buttons
        val allClickable = getAllClickableNodes(node)
        
        Log.e(TAG, "🔍 SEARCHING FOR HOST BUTTON: '$hostName'")
        Log.e(TAG, "🔍 Total clickable elements: ${allClickable.size}")
        
        // First, look for direct text matches
        val hostCandidates = mutableListOf<AccessibilityNodeInfo>()
        
        // Check each clickable element and its children for host name
        allClickable.forEachIndexed { index, candidate ->
            val text = candidate.text?.toString() ?: ""
            val desc = candidate.contentDescription?.toString() ?: ""
            val className = candidate.className?.toString() ?: ""
            
            // Get all text from children (host name might be in child TextView)
            val childTexts = getAllTextFromChildren(candidate)
            val allTexts = childTexts.joinToString(" ")
            
            Log.e(TAG, "  📋 Candidate $index: text='$text', desc='$desc', class='$className'")
            Log.e(TAG, "    📝 Child texts: '$allTexts'")
            
            // Check if this element or its children contain the host name
            val directMatch = text.contains(hostName, ignoreCase = true) || 
                             desc.contains(hostName, ignoreCase = true)
            
            val childMatch = allTexts.contains(hostName, ignoreCase = true)
            
            val containsHostName = directMatch || childMatch
            
            val isNotSettings = !text.contains("setting", ignoreCase = true) &&
                              !text.contains("option", ignoreCase = true) &&
                              !text.contains("menu", ignoreCase = true) &&
                              !desc.contains("setting", ignoreCase = true) &&
                              !desc.contains("option", ignoreCase = true) &&
                              !desc.contains("menu", ignoreCase = true) &&
                              !allTexts.contains("setting", ignoreCase = true) &&
                              !allTexts.contains("menu", ignoreCase = true)
            
            val isRelevantClass = className.contains("Button") || 
                                className.contains("TextView") ||
                                className.contains("RelativeLayout") ||
                                className.contains("LinearLayout") ||
                                className.contains("GridView")
            
            if (containsHostName) {
                Log.e(TAG, "    🎯 CONTAINS HOST NAME! directMatch=$directMatch, childMatch=$childMatch")
                Log.e(TAG, "    🎯 isNotSettings=$isNotSettings, isRelevantClass=$isRelevantClass")
                if (isNotSettings && isRelevantClass) {
                    Log.e(TAG, "    ✅ ADDING TO CANDIDATES!")
                    hostCandidates.add(candidate)
                } else {
                    Log.e(TAG, "    ❌ FILTERED OUT")
                }
            }
        }
        
        Log.e(TAG, "🔍 Found ${hostCandidates.size} potential host buttons after text analysis")
        
        // If we found candidates with text matching, return the best one
        if (hostCandidates.isNotEmpty()) {
            // Prefer exact matches first
            val exactMatch = hostCandidates.firstOrNull { candidate ->
                val allTexts = getAllTextFromChildren(candidate)
                allTexts.any { it.equals(hostName, ignoreCase = true) }
            }
            
            if (exactMatch != null) {
                Log.e(TAG, "✅ Found exact text match for host button")
                return exactMatch
            }
            
            Log.e(TAG, "✅ Using first partial match for host button")
            return hostCandidates.first()
        }
        
        // Fallback: If no text matches found, try position-based approach
        Log.e(TAG, "⚠️ No text-based matches found, trying position-based fallback...")
        
        // In Moonlight, host buttons are often in a grid/list after the "+" button
        // Try RelativeLayout and GridView elements (common containers for host buttons)
        val containerCandidates = allClickable.filter { candidate ->
            val className = candidate.className?.toString() ?: ""
            className.contains("RelativeLayout") || className.contains("GridView")
        }
        
        Log.e(TAG, "🔍 Found ${containerCandidates.size} container candidates for fallback")
        
        if (containerCandidates.isNotEmpty()) {
            // Try the first container that might contain host buttons
            Log.e(TAG, "🎯 Using first container as fallback host button")
            return containerCandidates.first()
        }
        
        // Last resort: Try any clickable element that's not an ImageButton
        val nonImageButtons = allClickable.filter { candidate ->
            val className = candidate.className?.toString() ?: ""
            !className.contains("ImageButton")
        }
        
        if (nonImageButtons.isNotEmpty()) {
            Log.e(TAG, "🎯 Using first non-ImageButton as last resort")
            return nonImageButtons.first()
        }
        
        Log.e(TAG, "❌ No suitable host button candidates found")
        return null
    }
    
    private fun getAllTextFromChildren(node: AccessibilityNodeInfo): List<String> {
        val texts = mutableListOf<String>()
        
        // Add this node's text if it exists
        node.text?.toString()?.let { if (it.isNotEmpty()) texts.add(it) }
        node.contentDescription?.toString()?.let { if (it.isNotEmpty()) texts.add(it) }
        
        // Recursively get text from all children
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                texts.addAll(getAllTextFromChildren(child))
            }
        }
        
        return texts
    }
    
    private fun dumpFullScreenContents(node: AccessibilityNodeInfo, depth: Int = 0) {
        try {
            val indent = "  ".repeat(depth)
            val text = node.text?.toString() ?: ""
            val desc = node.contentDescription?.toString() ?: ""
            val className = node.className?.toString() ?: ""
            val clickable = node.isClickable
            val enabled = node.isEnabled
            val visible = node.isVisibleToUser
            
            Log.e(TAG, "${indent}📍 Node[d$depth]: class='$className'")
            if (text.isNotEmpty()) Log.e(TAG, "${indent}   📝 text='$text'")
            if (desc.isNotEmpty()) Log.e(TAG, "${indent}   📋 desc='$desc'")
            Log.e(TAG, "${indent}   🎯 clickable=$clickable, enabled=$enabled, visible=$visible")
            Log.e(TAG, "${indent}   👶 children=${node.childCount}")
            
            // Show bounds for clickable elements
            if (clickable) {
                try {
                    val bounds = Rect()
                    node.getBoundsInScreen(bounds)
                    Log.e(TAG, "${indent}   📐 bounds=$bounds")
                } catch (e: Exception) {
                    Log.e(TAG, "${indent}   📐 bounds=ERROR")
                }
            }
            
            // Recursively dump children (but limit depth to avoid spam)
            if (depth < 4) {
                for (i in 0 until node.childCount) {
                    node.getChild(i)?.let { child ->
                        dumpFullScreenContents(child, depth + 1)
                    }
                }
            } else if (node.childCount > 0) {
                Log.e(TAG, "${indent}   ... ${node.childCount} more children (depth limit reached)")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error dumping node at depth $depth", e)
        }
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
        // Method 5: Try RelativeLayouts (Desktop might be wrapped in a layout)
        Log.d(TAG, "🔍 Method 1: Trying RelativeLayouts...")
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
    
    private fun checkForSessionDialog(retryCount: Int = 0) {
        try {
            // Prevent multiple simultaneous calls
            if (sessionDialogCheckInProgress) {
                Log.w(TAG, "⚠️ Session dialog check already in progress - skipping")
                return
            }
            
            sessionDialogCheckInProgress = true
            Log.i(TAG, "🔍 Checking for session dialog after Desktop click... (attempt ${retryCount + 1}/3)")
            
            val rootNode = rootInActiveWindow
            if (rootNode == null) {
                if (retryCount < 2) {
                    Log.w(TAG, "⚠️ No active window found - retrying in 2 seconds...")
                    sessionDialogCheckInProgress = false // Clear flag before retry
                    Handler(Looper.getMainLooper()).postDelayed({
                        checkForSessionDialog(retryCount + 1)
                    }, 2000)
                    return
                } else {
                    Log.w(TAG, "⚠️ No active window found after retries - assuming direct start")
                    Log.i(TAG, "🎉 MOONLIGHT CONNECTION COMPLETED! (No dialog - direct start)")
                    currentStep = ConnectionStep.COMPLETED
                    desktopClickRetries = 0
                    sessionDialogCheckInProgress = false
                    return
                }
            }
            
            // Ensure we're still in Moonlight app
            val packageName = rootNode.packageName?.toString() ?: ""
            if (packageName != "com.limelight") {
                if (retryCount < 2) {
                    Log.w(TAG, "⚠️ Not in Moonlight app (package: $packageName) - waiting for dialog...")
                    sessionDialogCheckInProgress = false // Clear flag before retry
                    Handler(Looper.getMainLooper()).postDelayed({
                        checkForSessionDialog(retryCount + 1)
                    }, 2000)
                    return
                } else {
                    Log.i(TAG, "🎉 MOONLIGHT CONNECTION COMPLETED! (Switched to streaming app)")
                    currentStep = ConnectionStep.COMPLETED
                    desktopClickRetries = 0
                    sessionDialogCheckInProgress = false
                    return
                }
            }
            
            // Look for session dialog options with enhanced detection
            Log.d(TAG, "🔍 Scanning for session dialog options...")
            
            // Get all clickable elements for session dialog analysis
            val allClickable = getAllClickableNodes(rootNode)
            Log.e(TAG, "🔍 SESSION DIALOG ANALYSIS - Found ${allClickable.size} clickable elements:")
            allClickable.forEachIndexed { index, node ->
                val text = node.text?.toString() ?: ""
                val desc = node.contentDescription?.toString() ?: ""
                val className = node.className?.toString() ?: ""
                
                // Get all text from children for debugging
                val childTexts = getAllTextFromChildren(node)
                val allTexts = childTexts.joinToString(" | ")
                
                Log.e(TAG, "  [$index] text='$text', desc='$desc', class='$className'")
                Log.e(TAG, "        childTexts: '$allTexts'")
            }
            
            // Look for session dialog buttons with multiple approaches
            var sessionButtonFound = false
            
            // Method 1: Look for "Resume Session" button (prioritize specific buttons over containers)
            val resumeCandidates = allClickable.filter { node ->
                val text = node.text?.toString() ?: ""
                val desc = node.contentDescription?.toString() ?: ""
                val className = node.className?.toString() ?: ""
                
                // Get all text from children (button text might be in child TextView)
                val childTexts = getAllTextFromChildren(node)
                val allTexts = childTexts.joinToString(" ")
                
                Log.d(TAG, "🔍 Checking resume candidate: text='$text', desc='$desc', class='$className', childTexts='$allTexts'")
                
                text.contains("resume", ignoreCase = true) || 
                desc.contains("resume", ignoreCase = true) ||
                text.contains("Resume Session", ignoreCase = true) ||
                desc.contains("Resume Session", ignoreCase = true) ||
                allTexts.contains("resume", ignoreCase = true) ||
                allTexts.contains("Resume Session", ignoreCase = true)
            }
            
            // Prioritize specific buttons over containers and those with exact text match
            val resumeButtons = resumeCandidates.sortedWith(compareBy<AccessibilityNodeInfo> { node ->
                val className = node.className?.toString() ?: ""
                val childTexts = getAllTextFromChildren(node)
                val hasExactResumeText = childTexts.any { it.equals("Resume Session", ignoreCase = true) }
                val isContainer = className.contains("ListView")
                
                when {
                    hasExactResumeText && className.contains("LinearLayout") -> 0    // Best: exact text + LinearLayout
                    hasExactResumeText && !isContainer -> 1                          // Good: exact text, not container
                    className.contains("LinearLayout") && !isContainer -> 2          // OK: LinearLayout, not container  
                    className.contains("Button") -> 3                                // OK: explicit button
                    !isContainer -> 4                                               // OK: not a container
                    else -> 9                                                        // Last: containers (ListView etc)
                }
            })
            
            Log.e(TAG, "🎯 Found ${resumeButtons.size} potential Resume buttons")
            resumeButtons.forEachIndexed { index, resumeButton ->
                val className = resumeButton.className?.toString() ?: ""
                Log.i(TAG, "🎯 Attempting Resume Session button $index (class: $className) - enhanced click methods")
                
                if (performEnhancedClick(resumeButton, "Resume Session")) {
                    Log.i(TAG, "✅ Successfully clicked Resume Session button $index")
                    
                    // Wait and verify the dialog was actually dismissed
                    Handler(Looper.getMainLooper()).postDelayed({
                        val newRootNode = rootInActiveWindow
                        if (newRootNode != null) {
                            val newClickable = getAllClickableNodes(newRootNode)
                            val stillHasDialog = newClickable.any { node ->
                                val childTexts = getAllTextFromChildren(node)
                                childTexts.any { it.contains("Resume Session", ignoreCase = true) }
                            }
                            
                                                         if (!stillHasDialog) {
                                 Log.i(TAG, "🎉 MOONLIGHT SESSION RESUMED! Dialog dismissed successfully.")
                                 currentStep = ConnectionStep.COMPLETED
                                 desktopClickRetries = 0
                                 sessionDialogCheckInProgress = false
                                 return@postDelayed
                                                         } else {
                                 Log.w(TAG, "⚠️ Resume Session button clicked but dialog still present - continuing search...")
                                 // Don't clear the flag yet, let the method continue with other buttons/methods
                             }
                        }
                    }, 1000)
                    
                    // Return early - let the handler validation determine success
                    return
                } else {
                    Log.w(TAG, "⚠️ Failed to click Resume Session button $index, trying next...")
                }
            }
            
            // Method 2: Look for "Start Session" or similar (new session)
            val startCandidates = allClickable.filter { node ->
                val text = node.text?.toString() ?: ""
                val desc = node.contentDescription?.toString() ?: ""
                
                // Get all text from children (button text might be in child TextView)
                val childTexts = getAllTextFromChildren(node)
                val allTexts = childTexts.joinToString(" ")
                
                Log.d(TAG, "🔍 Checking start candidate: text='$text', desc='$desc', childTexts='$allTexts'")
                
                text.contains("start", ignoreCase = true) || 
                desc.contains("start", ignoreCase = true) ||
                text.contains("connect", ignoreCase = true) ||
                desc.contains("connect", ignoreCase = true) ||
                text.contains("ok", ignoreCase = true) ||
                desc.contains("ok", ignoreCase = true) ||
                allTexts.contains("start", ignoreCase = true) ||
                allTexts.contains("connect", ignoreCase = true) ||
                allTexts.contains("ok", ignoreCase = true) ||
                allTexts.contains("Start Session", ignoreCase = true) ||
                allTexts.contains("New Session", ignoreCase = true)
            }
            
            // Prioritize specific buttons over containers
            val startButtons = startCandidates.sortedWith(compareBy<AccessibilityNodeInfo> { node ->
                val className = node.className?.toString() ?: ""
                val childTexts = getAllTextFromChildren(node)
                val hasExactStartText = childTexts.any { 
                    it.equals("Start Session", ignoreCase = true) || 
                    it.equals("Start", ignoreCase = true) ||
                    it.equals("Connect", ignoreCase = true)
                }
                val isContainer = className.contains("ListView")
                
                when {
                    hasExactStartText && className.contains("LinearLayout") -> 0    // Best: exact text + LinearLayout
                    hasExactStartText && !isContainer -> 1                          // Good: exact text, not container
                    className.contains("LinearLayout") && !isContainer -> 2         // OK: LinearLayout, not container  
                    className.contains("Button") -> 3                               // OK: explicit button
                    !isContainer -> 4                                               // OK: not a container
                    else -> 9                                                       // Last: containers (ListView etc)
                }
            })
            
            Log.e(TAG, "🎯 Found ${startButtons.size} potential Start/Connect buttons")
            startButtons.forEachIndexed { index, startButton ->
                val className = startButton.className?.toString() ?: ""
                Log.i(TAG, "🎯 Attempting Start/Connect button $index (class: $className) - enhanced click methods")
                
                if (performEnhancedClick(startButton, "Start Session")) {
                    Log.i(TAG, "✅ Successfully clicked start session button $index")
                    
                    // Wait and verify the dialog was actually dismissed
                    Handler(Looper.getMainLooper()).postDelayed({
                        val newRootNode = rootInActiveWindow
                        if (newRootNode != null) {
                            val newClickable = getAllClickableNodes(newRootNode)
                            val stillHasDialog = newClickable.any { node ->
                                val childTexts = getAllTextFromChildren(node)
                                childTexts.any { it.contains("Start Session", ignoreCase = true) || 
                                               it.contains("Resume Session", ignoreCase = true) }
                            }
                            
                            if (!stillHasDialog) {
                                Log.i(TAG, "🎉 MOONLIGHT NEW SESSION STARTED! Dialog dismissed successfully.")
                                currentStep = ConnectionStep.COMPLETED
                                desktopClickRetries = 0
                                sessionDialogCheckInProgress = false
                                return@postDelayed
                                                         } else {
                                 Log.w(TAG, "⚠️ Start Session button clicked but dialog still present - continuing search...")
                                 // Don't clear the flag yet, let the method continue with other buttons/methods
                             }
                        }
                    }, 1000)
                    
                    // Return early - let the handler validation determine success
                    return
                } else {
                    Log.w(TAG, "⚠️ Failed to click start session button $index, trying next...")
                }
            }
            
            // Method 3: Enhanced text-based search for common session patterns
            Log.e(TAG, "🔄 METHOD 3: Enhanced pattern matching for session buttons")
            val sessionPatterns = listOf(
                "resume", "Resume", "RESUME",
                "start", "Start", "START", 
                "continue", "Continue", "CONTINUE",
                "play", "Play", "PLAY",
                "connect", "Connect", "CONNECT",
                "ok", "OK", "Ok",
                "yes", "YES", "Yes",
                "proceed", "Proceed", "PROCEED"
            )
            
                         for (pattern in sessionPatterns) {
                 Log.d(TAG, "🔍 Searching for pattern: '$pattern'")
                 val patternButtons = allClickable.filter { node ->
                     val childTexts = getAllTextFromChildren(node)
                     val nodeInfo = node.toString()
                     
                     // Check both child text and node properties
                     val hasTextPattern = childTexts.any { it.contains(pattern, ignoreCase = false) }
                     val hasIdPattern = nodeInfo.contains(pattern, ignoreCase = true)
                     
                     hasTextPattern || hasIdPattern
                 }
                 
                 if (patternButtons.isNotEmpty()) {
                     Log.e(TAG, "🎯 Found ${patternButtons.size} buttons with pattern '$pattern'")
                     for (patternButton in patternButtons) {
                         Log.i(TAG, "🎯 Trying pattern-matched button for '$pattern'")
                         if (performEnhancedClick(patternButton, "Pattern Matched Session Button ($pattern)")) {
                             Log.i(TAG, "✅ Successfully clicked session button with pattern '$pattern'")
                             Log.i(TAG, "🎉 MOONLIGHT SESSION STARTED! (Pattern matching)")
                             currentStep = ConnectionStep.COMPLETED
                             desktopClickRetries = 0
                             return
                         }
                     }
                 }
             }
             
             // Method 3.5: Look for Android dialog buttons (common button classes/IDs)
             Log.e(TAG, "🔄 METHOD 3.5: Looking for Android dialog buttons")
             val dialogButtons = allClickable.filter { node ->
                 val nodeInfo = node.toString()
                 val className = node.className?.toString() ?: ""
                 
                 // Common Android dialog button patterns
                 className.contains("Button") || 
                 nodeInfo.contains("android:id/button", ignoreCase = true) ||
                 nodeInfo.contains("positiveButton", ignoreCase = true) ||
                 nodeInfo.contains("negativeButton", ignoreCase = true) ||
                 nodeInfo.contains("neutralButton", ignoreCase = true)
             }
             
             Log.e(TAG, "🎯 Found ${dialogButtons.size} potential dialog buttons")
             for (dialogButton in dialogButtons) {
                 Log.i(TAG, "🎯 Trying dialog button")
                 if (performEnhancedClick(dialogButton, "Dialog Button")) {
                     Log.i(TAG, "✅ Successfully clicked dialog button")
                     Log.i(TAG, "🎉 MOONLIGHT SESSION STARTED! (Dialog button)")
                     currentStep = ConnectionStep.COMPLETED
                     desktopClickRetries = 0
                     return
                 }
             }
            
            // Method 4: Try clicking the first few clickable elements as a fallback
            Log.e(TAG, "🔄 FALLBACK: Trying first 3 clickable elements as potential session buttons")
            for (i in 0 until minOf(3, allClickable.size)) {
                val fallbackButton = allClickable[i]
                Log.i(TAG, "🎯 Trying fallback button $i")
                if (performEnhancedClick(fallbackButton, "Fallback Session Button $i")) {
                    Log.i(TAG, "✅ Successfully clicked fallback session button $i")
                    Log.i(TAG, "🎉 MOONLIGHT SESSION STARTED! (Fallback method)")
                    currentStep = ConnectionStep.COMPLETED
                    desktopClickRetries = 0
                    return
                }
            }
            
            // If no dialog buttons worked, retry or give up
            if (retryCount < 2) {
                Log.w(TAG, "⚠️ No session dialog buttons worked - retrying in 3 seconds... (attempt ${retryCount + 1}/3)")
                sessionDialogCheckInProgress = false // Clear flag before retry
                Handler(Looper.getMainLooper()).postDelayed({
                    checkForSessionDialog(retryCount + 1)
                }, 3000)
                return
            }
            
            // Final attempt: assume direct connection (some setups start immediately)
            Log.d(TAG, "🔍 No session dialog buttons worked after retries - checking if connection started directly")
            
            // Wait a bit more and check if we're in streaming mode
            Handler(Looper.getMainLooper()).postDelayed({
                // Check if the current window is different (indicating streaming started)
                val currentWindow = rootInActiveWindow
                if (currentWindow?.packageName != "com.limelight") {
                    Log.i(TAG, "🎉 MOONLIGHT CONNECTION COMPLETED! (Direct connection - no dialog)")
                    currentStep = ConnectionStep.COMPLETED
                    desktopClickRetries = 0
                    sessionDialogCheckInProgress = false
                } else {
                    // Still in Moonlight app, might need more investigation
                    Log.w(TAG, "⚠️ Still in Moonlight app after all attempts - session dialog might not have been handled")
                    showAvailableOptions(currentWindow)
                    
                    // Mark as completed anyway to prevent infinite loops
                    Log.i(TAG, "🔄 Marking as completed to prevent infinite loops")
                    currentStep = ConnectionStep.COMPLETED
                    desktopClickRetries = 0
                    sessionDialogCheckInProgress = false
                }
            }, 2000)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking for session dialog", e)
            // Mark as completed to prevent infinite loops
            currentStep = ConnectionStep.COMPLETED
            desktopClickRetries = 0
            sessionDialogCheckInProgress = false
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
                    
                    // For session dialogs, try the first few clickable elements
                    if (buttonName.contains("Resume") || buttonName.contains("Start") || buttonName.contains("Session")) {
                        for (i in 0 until minOf(3, allClickable.size)) {
                            Log.i(TAG, "🎯 Attempting clickable element $i for $buttonName")
                            if (allClickable[i].performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                                Log.i(TAG, "✅ Successfully clicked $buttonName via position $i")
                                return true
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during position-based click", e)
            }
            
            // Method 5: Try delayed click (sometimes UI needs time)
            Log.d(TAG, "🔄 Method 5: Attempting delayed click for $buttonName...")
            try {
                Thread.sleep(1000) // Wait 1 second
                val clickResult = button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.e(TAG, "🔄 Delayed click result: $clickResult")
                if (clickResult) {
                    Log.i(TAG, "✅ Successfully clicked $buttonName with delayed click")
                    return true
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during delayed click", e)
            }
            
            // Method 6: Try clicking grandparent (sometimes button is nested deeply)
            Log.d(TAG, "🔄 Method 6: Attempting grandparent click for $buttonName...")
            try {
                val parent = button.parent
                val grandparent = parent?.parent
                if (grandparent != null && grandparent.isClickable) {
                    Log.d(TAG, "🔄 Grandparent is clickable, attempting grandparent click...")
                    if (grandparent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        Log.i(TAG, "✅ Successfully clicked $buttonName via grandparent")
                        return true
                    } else {
                        Log.w(TAG, "⚠️ Grandparent click failed for $buttonName")
                    }
                } else {
                    Log.d(TAG, "🔍 Grandparent is null or not clickable for $buttonName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during grandparent click", e)
            }
            
            // Method 7: Try all siblings (button might be in a container with other clickable siblings)
            Log.d(TAG, "🔄 Method 7: Attempting sibling clicks for $buttonName...")
            try {
                val parent = button.parent
                if (parent != null) {
                    for (i in 0 until parent.childCount) {
                        val sibling = parent.getChild(i)
                        if (sibling != null && sibling.isClickable && sibling != button) {
                            Log.d(TAG, "🔄 Trying clickable sibling $i...")
                            if (sibling.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                                Log.i(TAG, "✅ Successfully clicked $buttonName via sibling $i")
                                return true
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during sibling click", e)
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