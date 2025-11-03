package com.lilranker.tournament.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.lilranker.tournament.ui.LoginActivity

/**
 * Session monitor to track and handle session expiry
 * Runs a timer that checks session validity periodically
 */
class SessionMonitor(private val context: Context) {
    
    private val sessionManager = SessionManager(context)
    private val handler = Handler(Looper.getMainLooper())
    private var isMonitoring = false
    
    // Check every 5 seconds
    private val checkInterval = 5000L
    
    private val sessionCheckRunnable = object : Runnable {
        override fun run() {
            if (isMonitoring) {
                checkSession()
                handler.postDelayed(this, checkInterval)
            }
        }
    }
    
    /**
     * Start monitoring session
     */
    fun startMonitoring() {
        if (!isMonitoring) {
            isMonitoring = true
            handler.post(sessionCheckRunnable)
        }
    }
    
    /**
     * Stop monitoring session
     */
    fun stopMonitoring() {
        isMonitoring = false
        handler.removeCallbacks(sessionCheckRunnable)
    }
    
    /**
     * Check if session is still valid
     */
    private fun checkSession() {
        if (!sessionManager.isSessionValid()) {
            // Session expired - logout and redirect to login
            onSessionExpired()
        }
    }
    
    /**
     * Handle session expiry
     */
    private fun onSessionExpired() {
        stopMonitoring()
        
        // Clear session
        sessionManager.clearSession()
        
        // Show toast
        (context as? Activity)?.runOnUiThread {
            Toast.makeText(
                context,
                "⏰ Session expired. Please login again.",
                Toast.LENGTH_LONG
            ).show()
        }
        
        // Redirect to login
        val intent = Intent(context, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
        
        // Finish current activity
        (context as? Activity)?.finish()
    }
    
    /**
     * Get formatted remaining time
     */
    fun getFormattedRemainingTime(): String {
        val remaining = sessionManager.getRemainingSessionTime()
        val seconds = (remaining / 1000) % 60
        val minutes = (remaining / (1000 * 60)) % 60
        val hours = (remaining / (1000 * 60 * 60)) % 24
        val days = remaining / (1000 * 60 * 60 * 24)
        
        return when {
            days > 0 -> "${days}d ${hours}h remaining"
            hours > 0 -> "${hours}h ${minutes}m remaining"
            minutes > 0 -> "${minutes}m ${seconds}s remaining"
            else -> "${seconds}s remaining"
        }
    }
}
