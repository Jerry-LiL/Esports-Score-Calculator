package com.lilranker.tournament.data.service

import com.lilranker.tournament.data.repository.RepositoryException
import com.lilranker.tournament.util.Logger
import org.json.JSONException
import org.json.JSONObject

/**
 * Service for serializing and deserializing rank points configuration.
 * 
 * Handles conversion between Map<Int, Int> and JSON string format.
 * Provides robust error handling for malformed JSON.
 * 
 * JSON Format: {"1":10,"2":6,"3":4,"4":2,"5":1}
 * Map Format: {1=10, 2=6, 3=4, 4=2, 5=1}
 */
object RankPointsSerializer {
    
    private const val TAG = "RankPointsSerializer"
    
    /**
     * Parse rank points JSON string to Map.
     * 
     * @param rankPointsJson JSON string like {"1":10,"2":6,"3":4}
     * @return Map of rank to points, empty map if parsing fails or input is blank
     * @throws RepositoryException.JsonParsingException if JSON is malformed
     */
    fun parseRankPoints(rankPointsJson: String): Map<Int, Int> {
        if (rankPointsJson.isBlank()) {
            Logger.d(TAG, "Blank rank points JSON provided, returning empty map")
            return emptyMap()
        }
        
        return try {
            val jsonObject = JSONObject(rankPointsJson)
            buildMap<Int, Int> {
                jsonObject.keys().forEach { key ->
                    val rank = key.toIntOrNull()
                    val points = jsonObject.optInt(key, 0)
                    
                    when {
                        rank == null -> {
                            Logger.w(TAG, "Invalid rank key: $key (not a valid integer)")
                        }
                        rank <= 0 -> {
                            Logger.w(TAG, "Invalid rank value: $rank (must be positive)")
                        }
                        points < 0 -> {
                            Logger.w(TAG, "Invalid points for rank $rank: $points (must be non-negative)")
                        }
                        else -> {
                            this[rank] = points
                        }
                    }
                }
            }.also {
                Logger.d(TAG, "Parsed rank points: $it")
            }
        } catch (e: JSONException) {
            Logger.e(TAG, "Error parsing rank points JSON: $rankPointsJson", e)
            throw RepositoryException.JsonParsingException(
                "Invalid rank points JSON format: ${e.message}",
                e
            )
        } catch (e: Exception) {
            Logger.e(TAG, "Unexpected error parsing rank points", e)
            emptyMap()
        }
    }
    
    /**
     * Create JSON string from rank points Map.
     * 
     * @param rankPoints Map of rank to points
     * @return JSON string like {"1":10,"2":6,"3":4}
     * @throws RepositoryException.JsonParsingException if JSON creation fails
     */
    fun createRankPointsJson(rankPoints: Map<Int, Int>): String {
        if (rankPoints.isEmpty()) {
            Logger.d(TAG, "Empty rank points map, returning empty JSON object")
            return "{}"
        }
        
        return try {
            val jsonObject = JSONObject()
            rankPoints
                .filter { (rank, _) -> rank > 0 }
                .forEach { (rank, points) ->
                    jsonObject.put(rank.toString(), points)
                }
            
            jsonObject.toString().also {
                Logger.d(TAG, "Created rank points JSON: $it")
            }
        } catch (e: JSONException) {
            Logger.e(TAG, "Error creating rank points JSON", e)
            throw RepositoryException.JsonParsingException(
                "Failed to create rank points JSON: ${e.message}",
                e
            )
        } catch (e: Exception) {
            Logger.e(TAG, "Unexpected error creating rank points JSON", e)
            "{}"
        }
    }
    
    /**
     * Validate rank points configuration.
     * 
     * @param rankPoints Map to validate
     * @return List of validation errors, empty if valid
     */
    fun validateRankPoints(rankPoints: Map<Int, Int>): List<String> {
        val errors = mutableListOf<String>()
        
        rankPoints.forEach { (rank, points) ->
            when {
                rank <= 0 -> errors.add("Rank $rank is invalid (must be positive)")
                rank > 25 -> errors.add("Rank $rank exceeds maximum (25)")
                points < 0 -> errors.add("Points for rank $rank is negative")
            }
        }
        
        return errors
    }
}
