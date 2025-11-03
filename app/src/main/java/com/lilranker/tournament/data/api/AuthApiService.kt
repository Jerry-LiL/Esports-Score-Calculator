package com.lilranker.tournament.data.api

import com.lilranker.tournament.data.model.KeyValidationRequest
import com.lilranker.tournament.data.model.KeyValidationResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * API service for authentication
 */
interface AuthApiService {
    
    /**
     * Validate and redeem a key
     * @param request contains key and device ID
     * @return Response with validation result
     */
    @POST("api/validate-key")
    suspend fun validateKey(
        @Body request: KeyValidationRequest
    ): Response<KeyValidationResponse>
}
