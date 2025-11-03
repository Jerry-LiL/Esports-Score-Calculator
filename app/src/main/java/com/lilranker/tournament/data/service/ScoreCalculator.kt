package com.lilranker.tournament.data.service

import com.lilranker.tournament.util.Logger

/**
 * Service for calculating match scores and points.
 * 
 * Centralizes all scoring logic to ensure consistency
 * across the application.
 */
object ScoreCalculator {
    
    private const val TAG = "ScoreCalculator"
    
    /**
     * Calculate total points for a match result.
     * 
     * Formula: totalPoints = (kills × pointsPerKill) + rankPoints[rank]
     * 
     * @param kills Number of kills
     * @param rank Final rank/position (1 = first place)
     * @param pointsPerKill Points awarded per kill
     * @param rankPoints Map of rank to points
     * @return Total calculated points
     */
    fun calculateTotalPoints(
        kills: Int,
        rank: Int,
        pointsPerKill: Int,
        rankPoints: Map<Int, Int>
    ): Int {
        require(kills >= 0) { "Kills cannot be negative: $kills" }
        require(rank > 0) { "Rank must be positive: $rank" }
        require(pointsPerKill >= 0) { "Points per kill cannot be negative: $pointsPerKill" }
        
        val killPoints = kills * pointsPerKill
        val positionPoints = rankPoints[rank] ?: 0
        val totalPoints = killPoints + positionPoints
        
        Logger.d(TAG, 
            "Calculated points - Kills: $kills, Rank: $rank, " +
            "Kill Points: $killPoints, Position Points: $positionPoints, " +
            "Total: $totalPoints"
        )
        
        return totalPoints
    }
    
    /**
     * Validate match result data.
     * 
     * @param kills Number of kills
     * @param rank Final rank
     * @param totalTeams Total number of teams in tournament
     * @return List of validation errors, empty if valid
     */
    fun validateMatchResult(
        kills: Int,
        rank: Int,
        totalTeams: Int
    ): List<String> {
        val errors = mutableListOf<String>()
        
        when {
            kills < 0 -> errors.add("Kills cannot be negative: $kills")
            kills > 999 -> errors.add("Kills value too high: $kills")
        }
        
        when {
            rank <= 0 -> errors.add("Rank must be positive: $rank")
            rank > totalTeams -> errors.add("Rank $rank exceeds total teams $totalTeams")
        }
        
        return errors
    }
}
