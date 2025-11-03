package com.lilranker.tournament.data.repository

/**
 * Base exception for repository layer errors.
 * 
 * Encapsulates data access errors with descriptive messages
 * and optional underlying causes.
 * 
 * @param message Descriptive error message
 * @param cause Optional underlying exception that caused this error
 */
sealed class RepositoryException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {
    
    /**
     * Configuration-related errors.
     * Thrown when configuration operations fail.
     */
    class ConfigurationException(
        message: String,
        cause: Throwable? = null
    ) : RepositoryException(message, cause)
    
    /**
     * Match result operation errors.
     * Thrown when match result operations fail.
     */
    class MatchResultException(
        message: String,
        cause: Throwable? = null
    ) : RepositoryException(message, cause)
    
    /**
     * Penalty operation errors.
     * Thrown when penalty operations fail.
     */
    class PenaltyException(
        message: String,
        cause: Throwable? = null
    ) : RepositoryException(message, cause)
    
    /**
     * Team alias operation errors.
     * Thrown when team alias operations fail.
     */
    class TeamAliasException(
        message: String,
        cause: Throwable? = null
    ) : RepositoryException(message, cause)
    
    /**
     * JSON parsing errors.
     * Thrown when rank points JSON cannot be parsed.
     */
    class JsonParsingException(
        message: String,
        cause: Throwable? = null
    ) : RepositoryException(message, cause)
    
    /**
     * Data validation errors.
     * Thrown when data fails validation rules.
     */
    class ValidationException(
        message: String,
        cause: Throwable? = null
    ) : RepositoryException(message, cause)
}
