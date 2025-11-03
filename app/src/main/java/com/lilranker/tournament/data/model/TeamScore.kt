package com.lilranker.tournament.data.model

/**
 * Team score data class for leaderboard display.
 * 
 * Represents aggregated performance data for a team,
 * calculated from match results with penalties applied.
 * 
 * This is not a Room entity - it's derived from MatchResult and Penalty entities.
 * 
 * Calculation:
 * - totalKills: Sum of all kills across matches
 * - totalPoints: Sum of all match points minus total penalties
 * - matchesPlayed: Count of matches the team participated in
 * 
 * @property teamNumber Team identifier
 * @property totalKills Total kills across all matches
 * @property totalPoints Total points (with penalties deducted)
 * @property matchesPlayed Number of matches team participated in
 */
data class TeamScore(
    val teamNumber: Int,
    val totalKills: Int,
    val totalPoints: Int,
    val matchesPlayed: Int
)
