package com.lilranker.tournament.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tournament configuration entity.
 * 
 * Stores all tournament settings including:
 * - Tournament duration and structure
 * - Scoring parameters
 * - Team configuration
 * 
 * Note: Only one configuration should exist at a time.
 * The most recent configuration (by createdAt) is considered current.
 * 
 * @property id Auto-generated primary key
 * @property totalDays Number of tournament days
 * @property matchesPerDay Number of matches played each day
 * @property totalTeams Total number of teams in tournament (fixed at 25)
 * @property pointsPerKill Points awarded for each kill
 * @property rankPoints JSON string mapping rank position to points (e.g., {"1":10,"2":6,"3":4})
 * @property createdAt Timestamp when configuration was created
 */
@Entity(tableName = "tournament_config")
data class TournamentConfig(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val totalDays: Int,
    val matchesPerDay: Int,
    val totalTeams: Int = 25,
    val pointsPerKill: Int,
    val rankPoints: String,
    val createdAt: Long = System.currentTimeMillis()
)
