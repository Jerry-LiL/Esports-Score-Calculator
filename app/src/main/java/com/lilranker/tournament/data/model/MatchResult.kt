package com.lilranker.tournament.data.model

import androidx.room.Entity

/**
 * Match result entity.
 * 
 * Stores a team's performance in a specific match.
 * 
 * Primary key is composite: (day, matchNumber, teamNumber)
 * This ensures each team can only have one result per match.
 * 
 * Total points calculation:
 * totalPoints = (kills × pointsPerKill) + rankPoints[rank]
 * 
 * @property day Day number of the match
 * @property matchNumber Match number within the day
 * @property teamNumber Team identifier
 * @property kills Number of kills achieved by the team
 * @property rank Final rank/position in the match (1 = first place)
 * @property totalPoints Calculated total points for the match
 * @property timestamp When the result was recorded
 */
@Entity(
    tableName = "match_results",
    primaryKeys = ["day", "matchNumber", "teamNumber"]
)
data class MatchResult(
    val day: Int,
    val matchNumber: Int,
    val teamNumber: Int,
    val kills: Int,
    val rank: Int,
    val totalPoints: Int,
    val timestamp: Long = System.currentTimeMillis()
)
