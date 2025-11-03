package com.lilranker.tournament.data.model

/**
 * Data model for authentication key
 * Represents a key in the MongoDB database
 */
data class AuthKey(
    val key: String,
    val isUsed: Boolean = false,
    val usedBy: String? = null,
    val usedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Response from key validation API
 */
data class KeyValidationResponse(
    val success: Boolean,
    val message: String,
    val data: AuthKey? = null
)

/**
 * Request body for key validation
 */
data class KeyValidationRequest(
    val key: String,
    val deviceId: String
)
