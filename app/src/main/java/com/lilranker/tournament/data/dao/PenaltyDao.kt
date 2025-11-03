package com.lilranker.tournament.data.dao

import androidx.room.*
import com.lilranker.tournament.data.model.Penalty
import kotlinx.coroutines.flow.Flow

/**
 * DAO for penalty operations.
 * 
 * Penalties are applied to teams for rule violations or other infractions.
 * Primary key: (day, matchNumber, teamNumber) - one penalty per team per match.
 */
@Dao
interface PenaltyDao {
    
    // ========================================
    // Query Operations
    // ========================================
    
    /**
     * Get penalties for a specific match as a Flow.
     * Results are sorted by team number.
     */
    @Query("SELECT * FROM penalties WHERE day = :day AND matchNumber = :matchNumber ORDER BY teamNumber ASC")
    fun getPenaltiesForMatch(day: Int, matchNumber: Int): Flow<List<Penalty>>
    
    /**
     * Get penalties for a specific match synchronously.
     * Results are sorted by team number.
     */
    @Query("SELECT * FROM penalties WHERE day = :day AND matchNumber = :matchNumber ORDER BY teamNumber ASC")
    suspend fun getPenaltiesForMatchSync(day: Int, matchNumber: Int): List<Penalty>
    
    /**
     * Get all penalties as a Flow.
     * Results are sorted by day, match number, and team number.
     */
    @Query("SELECT * FROM penalties ORDER BY day, matchNumber, teamNumber")
    fun getAllPenalties(): Flow<List<Penalty>>
    
    /**
     * Get all penalties synchronously.
     * Results are sorted by day, match number, and team number.
     */
    @Query("SELECT * FROM penalties ORDER BY day, matchNumber, teamNumber")
    suspend fun getAllPenaltiesSync(): List<Penalty>
    
    // ========================================
    // Insert/Update Operations
    // ========================================
    
    /**
     * Insert or replace a penalty.
     * If a penalty already exists for the same day/match/team, it's replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPenalty(penalty: Penalty)
    
    // ========================================
    // Delete Operations
    // ========================================
    
    /**
     * Delete a specific penalty for a team in a match.
     */
    @Query("DELETE FROM penalties WHERE day = :day AND matchNumber = :matchNumber AND teamNumber = :teamNumber")
    suspend fun deletePenalty(day: Int, matchNumber: Int, teamNumber: Int)
    
    /**
     * Delete all penalties for a specific match.
     */
    @Query("DELETE FROM penalties WHERE day = :day AND matchNumber = :matchNumber")
    suspend fun deletePenaltiesForMatch(day: Int, matchNumber: Int)
    
    /**
     * Delete all penalties for a specific day.
     */
    @Query("DELETE FROM penalties WHERE day = :day")
    suspend fun deletePenaltiesByDay(day: Int)
    
    /**
     * Delete penalties for a range of days (inclusive).
     */
    @Query("DELETE FROM penalties WHERE day >= :startDay AND day <= :endDay")
    suspend fun deletePenaltiesByDayRange(startDay: Int, endDay: Int)
    
    /**
     * Delete all penalties.
     */
    @Query("DELETE FROM penalties")
    suspend fun deleteAllPenalties()
    
    // ========================================
    // Aggregate Queries
    // ========================================
    
    /**
     * Get total penalty points for a team across all matches.
     * 
     * @return Total penalty points, null if no penalties exist
     */
    @Query("""
        SELECT SUM(penaltyPoints) as totalPenalty
        FROM penalties 
        WHERE teamNumber = :teamNumber
    """)
    suspend fun getTotalPenaltyForTeam(teamNumber: Int): Int?
    
    /**
     * Get total penalty points for a team on a specific day.
     * 
     * @return Total penalty points for the day, null if no penalties exist
     */
    @Query("""
        SELECT SUM(penaltyPoints) as totalPenalty
        FROM penalties 
        WHERE teamNumber = :teamNumber AND day = :day
    """)
    suspend fun getTotalPenaltyForTeamOnDay(teamNumber: Int, day: Int): Int?
}
