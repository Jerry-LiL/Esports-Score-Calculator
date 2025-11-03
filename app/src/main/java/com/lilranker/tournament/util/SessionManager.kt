package com.lilranker.tournament.util

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings

/**
 * Session manager to handle authentication state and session expiry
 * Uses SharedPreferences for offline session tracking
 */
class SessionManager(context: Context) {
    
    companion object {
        private const val PREF_NAME = "LiLRankerAuthPrefs"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_LOGIN_TIMESTAMP = "loginTimestamp"
        private const val KEY_DEVICE_ID = "deviceId"
        private const val KEY_VALIDATED_KEY = "validatedKey"
        private const val KEY_IS_MASTER_LOGIN = "isMasterLogin"
        
        // Master password for offline access (never expires)
        private const val MASTER_PASSWORD = "1@2@3@4@5@"
        
        // Session duration in milliseconds (2 minutes for testing)
        // Change to 30 days: 30L * 24 * 60 * 60 * 1000
        const val SESSION_DURATION = 2L * 60 * 1000 // 2 minutes
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = prefs.edit()
    
    /**
     * Get unique device ID
     */
    fun getDeviceId(context: Context): String {
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        if (deviceId == null) {
            deviceId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "unknown_device"
            editor.putString(KEY_DEVICE_ID, deviceId).apply()
        }
        return deviceId ?: "unknown_device"
    }
    
    /**
     * Save login session
     */
    fun saveLoginSession(key: String, isMasterPassword: Boolean = false) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putLong(KEY_LOGIN_TIMESTAMP, System.currentTimeMillis())
        editor.putString(KEY_VALIDATED_KEY, key)
        editor.putBoolean(KEY_IS_MASTER_LOGIN, isMasterPassword)
        editor.apply()
    }
    
    /**
     * Validate master password
     */
    fun validateMasterPassword(password: String): Boolean {
        return password == MASTER_PASSWORD
    }
    
    /**
     * Check if current session is master login
     */
    fun isMasterLogin(): Boolean {
        return prefs.getBoolean(KEY_IS_MASTER_LOGIN, false)
    }
    
    /**
     * Check if user is logged in AND session is still valid
     */
    fun isSessionValid(): Boolean {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (!isLoggedIn) return false
        
        // Master password login never expires
        if (isMasterLogin()) return true
        
        val loginTimestamp = prefs.getLong(KEY_LOGIN_TIMESTAMP, 0)
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - loginTimestamp
        
        return elapsedTime < SESSION_DURATION
    }
    
    /**
     * Get remaining session time in milliseconds
     */
    fun getRemainingSessionTime(): Long {
        if (!prefs.getBoolean(KEY_IS_LOGGED_IN, false)) return 0
        
        // Master login never expires
        if (isMasterLogin()) return Long.MAX_VALUE
        
        val loginTimestamp = prefs.getLong(KEY_LOGIN_TIMESTAMP, 0)
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - loginTimestamp
        val remaining = SESSION_DURATION - elapsedTime
        
        return if (remaining > 0) remaining else 0
    }
    
    /**
     * Clear login session (logout)
     */
    fun clearSession() {
        editor.remove(KEY_IS_LOGGED_IN)
        editor.remove(KEY_LOGIN_TIMESTAMP)
        editor.remove(KEY_VALIDATED_KEY)
        editor.remove(KEY_IS_MASTER_LOGIN)
        editor.remove(KEY_VALIDATED_KEY)
        editor.apply()
    }
    
    /**
     * Get the validated key
     */
    fun getValidatedKey(): String? {
        return prefs.getString(KEY_VALIDATED_KEY, null)
    }
    
    /**
     * Check if session is about to expire (within 10 seconds)
     */
    fun isSessionAboutToExpire(): Boolean {
        val remaining = getRemainingSessionTime()
        return remaining > 0 && remaining < 10000 // Less than 10 seconds
    }
}
