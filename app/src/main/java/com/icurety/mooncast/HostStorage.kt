package com.icurety.mooncast

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

object HostStorage {
    private const val PREFS_NAME = "mooncast_hosts"
    private const val TAG = "HostStorage"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun saveHostMapping(context: Context, ip: String, pcName: String) {
        try {
            getPrefs(context).edit().putString(ip, pcName).apply()
            Log.d(TAG, "Saved mapping: $ip -> $pcName")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving host mapping", e)
        }
    }
    
    fun getHostName(context: Context, ip: String): String? {
        return try {
            val result = getPrefs(context).getString(ip, null)
            Log.d(TAG, "Retrieved mapping for $ip: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error getting host name", e)
            null
        }
    }
    
    fun getAllHosts(context: Context): Map<String, String> {
        return try {
            val allPrefs = getPrefs(context).all
            val result = allPrefs.mapValues { it.value as String }
            Log.d(TAG, "Retrieved all hosts: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all hosts", e)
            emptyMap()
        }
    }
    
    fun removeHost(context: Context, ip: String) {
        try {
            getPrefs(context).edit().remove(ip).apply()
            Log.d(TAG, "Removed mapping for: $ip")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing host", e)
        }
    }
} 