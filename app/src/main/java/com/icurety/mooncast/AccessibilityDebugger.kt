package com.icurety.mooncast

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

object AccessibilityDebugger {
    private const val TAG = "A11yDebugger"
    
    /**
     * Dumps the entire accessibility tree to logs
     */
    fun dumpAccessibilityTree(rootNode: AccessibilityNodeInfo?, prefix: String = "") {
        if (rootNode == null) return
        
        val bounds = Rect()
        rootNode.getBoundsInScreen(bounds)
        
        val nodeInfo = buildString {
            append("${prefix}Node: ")
            append("class=${rootNode.className} ")
            append("text='${rootNode.text}' ")
            append("desc='${rootNode.contentDescription}' ")
            append("bounds=${bounds} ")
            append("clickable=${rootNode.isClickable} ")
            append("enabled=${rootNode.isEnabled} ")
            append("visible=${rootNode.isVisibleToUser} ")
            append("id=${rootNode.viewIdResourceName} ")
            append("children=${rootNode.childCount}")
        }
        
        Log.d(TAG, nodeInfo)
        
        // Recursively dump children
        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i)
            if (child != null) {
                dumpAccessibilityTree(child, "$prefix  ")
                child.recycle()
            }
        }
    }
    
    /**
     * Find all clickable nodes and log their details
     */
    fun findAllClickableNodes(rootNode: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> {
        if (rootNode == null) return emptyList()
        
        val clickableNodes = mutableListOf<AccessibilityNodeInfo>()
        findClickableNodesRecursive(rootNode, clickableNodes)
        
        Log.d(TAG, "Found ${clickableNodes.size} clickable nodes:")
        clickableNodes.forEachIndexed { index, node ->
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            Log.d(TAG, "[$index] Clickable: class=${node.className}, text='${node.text}', desc='${node.contentDescription}', bounds=$bounds, id=${node.viewIdResourceName}")
        }
        
        return clickableNodes
    }
    
    private fun findClickableNodesRecursive(node: AccessibilityNodeInfo, result: MutableList<AccessibilityNodeInfo>) {
        if (node.isClickable) {
            result.add(node)
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                findClickableNodesRecursive(child, result)
                // Don't recycle here as we're keeping references
            }
        }
    }
    
    /**
     * Find nodes by position (useful for finding top-right buttons)
     */
    fun findNodesByPosition(rootNode: AccessibilityNodeInfo?, x: Float, y: Float, tolerance: Float = 50f): List<AccessibilityNodeInfo> {
        if (rootNode == null) return emptyList()
        
        val matchingNodes = mutableListOf<AccessibilityNodeInfo>()
        findNodesByPositionRecursive(rootNode, x, y, tolerance, matchingNodes)
        
        Log.d(TAG, "Found ${matchingNodes.size} nodes near position ($x, $y):")
        matchingNodes.forEachIndexed { index, node ->
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            Log.d(TAG, "[$index] Position match: class=${node.className}, text='${node.text}', center=(${bounds.centerX()}, ${bounds.centerY()})")
        }
        
        return matchingNodes
    }
    
    private fun findNodesByPositionRecursive(node: AccessibilityNodeInfo, x: Float, y: Float, tolerance: Float, result: MutableList<AccessibilityNodeInfo>) {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        
        val distance = kotlin.math.sqrt(
            ((bounds.centerX() - x) * (bounds.centerX() - x) + (bounds.centerY() - y) * (bounds.centerY() - y)).toDouble()
        ).toFloat()
        
        if (distance <= tolerance) {
            result.add(node)
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                findNodesByPositionRecursive(child, x, y, tolerance, result)
            }
        }
    }
    
    /**
     * Find nodes in a specific region (e.g., top-right corner)
     */
    fun findNodesInRegion(rootNode: AccessibilityNodeInfo?, left: Float, top: Float, right: Float, bottom: Float): List<AccessibilityNodeInfo> {
        if (rootNode == null) return emptyList()
        
        val matchingNodes = mutableListOf<AccessibilityNodeInfo>()
        findNodesInRegionRecursive(rootNode, left, top, right, bottom, matchingNodes)
        
        Log.d(TAG, "Found ${matchingNodes.size} nodes in region ($left, $top, $right, $bottom):")
        matchingNodes.forEachIndexed { index, node ->
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            Log.d(TAG, "[$index] Region match: class=${node.className}, text='${node.text}', bounds=$bounds, clickable=${node.isClickable}")
        }
        
        return matchingNodes
    }
    
    private fun findNodesInRegionRecursive(node: AccessibilityNodeInfo, left: Float, top: Float, right: Float, bottom: Float, result: MutableList<AccessibilityNodeInfo>) {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        
        // Check if node's center is in the region
        if (bounds.centerX() >= left && bounds.centerX() <= right && 
            bounds.centerY() >= top && bounds.centerY() <= bottom) {
            result.add(node)
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                findNodesInRegionRecursive(child, left, top, right, bottom, result)
            }
        }
    }
    
    /**
     * Find nodes by content description patterns
     */
    fun findNodesByContentDescription(rootNode: AccessibilityNodeInfo?, patterns: List<String>): List<AccessibilityNodeInfo> {
        if (rootNode == null) return emptyList()
        
        val matchingNodes = mutableListOf<AccessibilityNodeInfo>()
        findNodesByContentDescriptionRecursive(rootNode, patterns, matchingNodes)
        
        Log.d(TAG, "Found ${matchingNodes.size} nodes matching content description patterns: $patterns")
        matchingNodes.forEach { node ->
            Log.d(TAG, "Content desc match: '${node.contentDescription}', class=${node.className}")
        }
        
        return matchingNodes
    }
    
    private fun findNodesByContentDescriptionRecursive(node: AccessibilityNodeInfo, patterns: List<String>, result: MutableList<AccessibilityNodeInfo>) {
        val contentDesc = node.contentDescription?.toString()?.lowercase()
        if (contentDesc != null) {
            for (pattern in patterns) {
                if (contentDesc.contains(pattern.lowercase())) {
                    result.add(node)
                    break
                }
            }
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                findNodesByContentDescriptionRecursive(child, patterns, result)
            }
        }
    }
    
    /**
     * Find small square nodes (often buttons like +)
     */
    fun findSmallSquareNodes(rootNode: AccessibilityNodeInfo?, minSize: Int = 48, maxSize: Int = 200): List<AccessibilityNodeInfo> {
        if (rootNode == null) return emptyList()
        
        val matchingNodes = mutableListOf<AccessibilityNodeInfo>()
        findSmallSquareNodesRecursive(rootNode, minSize, maxSize, matchingNodes)
        
        Log.d(TAG, "Found ${matchingNodes.size} small square nodes (${minSize}x${minSize} to ${maxSize}x${maxSize}):")
        matchingNodes.forEach { node ->
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            Log.d(TAG, "Square node: size=${bounds.width()}x${bounds.height()}, class=${node.className}, clickable=${node.isClickable}")
        }
        
        return matchingNodes
    }
    
    private fun findSmallSquareNodesRecursive(node: AccessibilityNodeInfo, minSize: Int, maxSize: Int, result: MutableList<AccessibilityNodeInfo>) {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        
        val width = bounds.width()
        val height = bounds.height()
        
        // Check if it's roughly square and within size range
        if (width in minSize..maxSize && height in minSize..maxSize) {
            val aspectRatio = width.toFloat() / height.toFloat()
            if (aspectRatio in 0.8f..1.2f) { // Roughly square
                result.add(node)
            }
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                findSmallSquareNodesRecursive(child, minSize, maxSize, result)
            }
        }
    }
} 