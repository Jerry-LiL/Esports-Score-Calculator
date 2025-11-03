package com.lilranker.tournament.util

import timber.log.Timber

/**
 * Centralized logging utility for the application.
 * 
 * Wraps Timber to provide consistent logging interface.
 * Makes it easier to switch logging implementations if needed.
 */
object Logger {
    
    /**
     * Log debug message.
     */
    fun d(tag: String, message: String) {
        Timber.tag(tag).d(message)
    }
    
    /**
     * Log info message.
     */
    fun i(tag: String, message: String) {
        Timber.tag(tag).i(message)
    }
    
    /**
     * Log warning message.
     */
    fun w(tag: String, message: String) {
        Timber.tag(tag).w(message)
    }
    
    /**
     * Log error message.
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Timber.tag(tag).e(throwable, message)
        } else {
            Timber.tag(tag).e(message)
        }
    }
    
    /**
     * Log verbose message.
     */
    fun v(tag: String, message: String) {
        Timber.tag(tag).v(message)
    }
}
