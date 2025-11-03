package com.lilranker.tournament.data.model

import androidx.room.Entity

/**
 * Penalty entity.
 * 
 * Stores penalty points applied to teams for rule violations or infractions.
 * Penalties are deducted from team scores in the leaderboard.
 * 
 * Primary key is composite: (day, matchNumber, teamNumber)
 * This ensures each team can only have one penalty per match.
 * 
 * @property day Day number when penalty was incurred
 * @property matchNumber Match number when penalty was incurred
 * @property teamNumber Team receiving the penalty
 * @property penaltyPoints Points to deduct from team's score
 * @property timestamp When the penalty was recorded
 */
@Entity(
    tableName = "penalties",
    primaryKeys = ["day", "matchNumber", "teamNumber"]
)
data class Penalty(
    val day: Int,
    val matchNumber: Int,
    val teamNumber: Int,
    val penaltyPoints: Int,
    val timestamp: Long = System.currentTimeMillis()
)
