package com.lilranker.tournament.data.dao

import androidx.room.*
import com.lilranker.tournament.data.model.MatchResult
import kotlinx.coroutines.flow.Flow

/**
 * DAO for match result operations.
 * 
 * Match results store team performance data for each match.
 * Primary key: (day, matchNumber, teamNumber) - one result per team per match.
 */
@Dao
interface MatchResultDao {
    
    // ========================================
    // Query Operations
    // ========================================
    
    /**
     * Get match results for a specific day and match as a Flow.
     * Results are sorted by team number.
     */
    @Query("SELECT * FROM match_results WHERE day = :day AND matchNumber = :matchNumber ORDER BY teamNumber ASC")
    fun getMatchResults(day: Int, matchNumber: Int): Flow<List<MatchResult>>
    
    /**
     * Get match results for a specific day and match synchronously.
     * Results are sorted by team number.
     */
    @Query("SELECT * FROM match_results WHERE day = :day AND matchNumber = :matchNumber ORDER BY teamNumber ASC")
    suspend fun getMatchResultsSync(day: Int, matchNumber: Int): List<MatchResult>
    
    /**
     * Get a specific match result for a team.
     * Used for manual score updates.
     */
    @Query("SELECT * FROM match_results WHERE day = :day AND matchNumber = :matchNumber AND teamNumber = :teamNumber LIMIT 1")
    suspend fun getMatchResultForTeam(day: Int, matchNumber: Int, teamNumber: Int): MatchResult?
    
    /**
     * Get all match results as a Flow.
     * Results are sorted by day, match number, and team number.
     */
    @Query("SELECT * FROM match_results ORDER BY day, matchNumber, teamNumber")
    fun getAllResults(): Flow<List<MatchResult>>
    
    /**
     * Get all match results synchronously.
     * Used for batch operations like recalculation.
     */
    @Query("SELECT * FROM match_results ORDER BY day, matchNumber, teamNumber")
    suspend fun getAllResultsSync(): List<MatchResult>
    
    // ========================================
    // Insert/Update Operations
    // ========================================
    
    /**
     * Insert or replace a single match result.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: MatchResult)
    
    /**
     * Insert or replace a single match result (alias for consistency).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatchResult(result: MatchResult)
    
    /**
     * Insert or replace multiple match results.
     * More efficient than multiple single inserts.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(results: List<MatchResult>)
    
    // ========================================
    // Delete Operations
    // ========================================
    
    /**
     * Delete all results for a specific match.
     */
    @Query("DELETE FROM match_results WHERE day = :day AND matchNumber = :matchNumber")
    suspend fun deleteMatchResults(day: Int, matchNumber: Int)
    
    /**
     * Delete all results for a specific day.
     */
    @Query("DELETE FROM match_results WHERE day = :day")
    suspend fun deleteMatchResultsByDay(day: Int)
    
    /**
     * Delete results for a range of days (inclusive).
     */
    @Query("DELETE FROM match_results WHERE day >= :startDay AND day <= :endDay")
    suspend fun deleteMatchResultsByDayRange(startDay: Int, endDay: Int)
    
    /**
     * Delete all match results.
     */
    @Query("DELETE FROM match_results")
    suspend fun deleteAllResults()
    
    // ========================================
    // Leaderboard Queries
    // ========================================
    
    /**
     * Get raw leaderboard data for all days.
     * Aggregates team performance across all matches.
     * Sorted by total points (DESC), then total kills (DESC).
     */
    @Query("""
        SELECT teamNumber, 
               SUM(kills) as totalKills, 
               SUM(totalPoints) as totalPoints,
               COUNT(*) as matchesPlayed
        FROM match_results 
        GROUP BY teamNumber 
        ORDER BY totalPoints DESC, totalKills DESC
    """)
    fun getLeaderboardRaw(): Flow<List<LeaderboardEntry>>
    
    /**
     * Get raw leaderboard data for a specific day.
     * Only includes results from the specified day.
     * Sorted by total points (DESC), then total kills (DESC).
     */
    @Query("""
        SELECT teamNumber, 
               SUM(kills) as totalKills, 
               SUM(totalPoints) as totalPoints,
               COUNT(*) as matchesPlayed
        FROM match_results 
        WHERE day = :day
        GROUP BY teamNumber 
        ORDER BY totalPoints DESC, totalKills DESC
    """)
    fun getLeaderboardByDayRaw(day: Int): Flow<List<LeaderboardEntry>>
    
    // ========================================
    // Data Existence Queries
    // ========================================
    
    /**
     * Get distinct days that have any data entered.
     * Only includes days where kills > 0 or rank > 0.
     */
    @Query("""
        SELECT DISTINCT day 
        FROM match_results 
        WHERE kills > 0 OR rank > 0 
        ORDER BY day ASC
    """)
    suspend fun getDaysWithData(): List<Int>
    
    /**
     * Get match numbers for a specific day that have data entered.
     */
    @Query("""
        SELECT DISTINCT matchNumber 
        FROM match_results 
        WHERE day = :day AND (kills > 0 OR rank > 0)
        ORDER BY matchNumber ASC
    """)
    suspend fun getMatchesWithDataForDay(day: Int): List<Int>
    
    /**
     * Check if a specific match has data entered.
     */
    @Query("""
        SELECT COUNT(*) > 0 
        FROM match_results 
        WHERE day = :day AND matchNumber = :matchNumber AND (kills > 0 OR rank > 0)
    """)
    suspend fun hasMatchData(day: Int, matchNumber: Int): Boolean
    
    /**
     * Check if any match data exists beyond a certain match number.
     * Used to validate matches per day configuration changes.
     */
    @Query("""
        SELECT COUNT(*) > 0 
        FROM match_results 
        WHERE matchNumber > :matchNumber
    """)
    suspend fun hasMatchDataBeyondNumber(matchNumber: Int): Boolean
    
    /**
     * Check if any match data exists at all.
     * Used to determine if View Leaderboard should be enabled.
     */
    @Query("""
        SELECT COUNT(*) > 0 
        FROM match_results 
        WHERE kills > 0 OR rank > 0
    """)
    suspend fun hasAnyMatchData(): Boolean
    
    /**
     * Get the last day that has match data.
     * Returns the highest day number with data, or null if no data exists.
     */
    @Query("""
        SELECT MAX(day) 
        FROM match_results 
        WHERE kills > 0 OR rank > 0
    """)
    suspend fun getLastDayWithData(): Int?
    
    /**
     * Get the last match number for a specific day that has data.
     * Returns the highest match number with data for that day, or null if no data exists.
     */
    @Query("""
        SELECT MAX(matchNumber) 
        FROM match_results 
        WHERE day = :day AND (kills > 0 OR rank > 0)
    """)
    suspend fun getLastMatchWithDataForDay(day: Int): Int?
    
    /**
     * Data class for raw leaderboard aggregation.
     * Used before penalties are applied.
     */
    data class LeaderboardEntry(
        val teamNumber: Int,
        val totalKills: Int,
        val totalPoints: Int,
        val matchesPlayed: Int
    )
}
