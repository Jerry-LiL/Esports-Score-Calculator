package com.lilranker.tournament.data.repository

import android.content.Context
import com.lilranker.tournament.data.api.RetrofitClient
import com.lilranker.tournament.data.model.KeyValidationRequest
import com.lilranker.tournament.data.model.KeyValidationResponse
import com.lilranker.tournament.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for authentication operations
 */
class AuthRepository(private val context: Context) {
    
    private val apiService = RetrofitClient.authApiService
    private val sessionManager = SessionManager(context)
    
    /**
     * Validate a key with the backend
     */
    suspend fun validateKey(key: String): Result<KeyValidationResponse> = withContext(Dispatchers.IO) {
        try {
            val deviceId = sessionManager.getDeviceId(context)
            val request = KeyValidationRequest(key, deviceId)
            val response = apiService.validateKey(request)
            
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    // Save session on successful validation
                    sessionManager.saveLoginSession(key)
                }
                Result.success(body)
            } else {
                Result.failure(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if session is valid
     */
    fun isSessionValid(): Boolean {
        return sessionManager.isSessionValid()
    }
    
    /**
     * Get remaining session time
     */
    fun getRemainingSessionTime(): Long {
        return sessionManager.getRemainingSessionTime()
    }
    
    /**
     * Logout user
     */
    fun logout() {
        sessionManager.clearSession()
    }
}
