package com.lilranker.tournament.data.dao

import androidx.room.*
import com.lilranker.tournament.data.model.TeamAlias

/**
 * DAO for team alias/grouping operations.
 */
@Dao
interface TeamAliasDao {
    
    /**
     * Get all team aliases.
     */
    @Query("SELECT * FROM team_aliases")
    suspend fun getAllAliases(): List<TeamAlias>
    
    /**
     * Get all alias teams for a primary team.
     */
    @Query("SELECT * FROM team_aliases WHERE primaryTeamNumber = :primaryTeam")
    suspend fun getAliasesForPrimaryTeam(primaryTeam: Int): List<TeamAlias>
    
    /**
     * Get primary team for an alias team number.
     * Returns null if the team is not an alias.
     */
    @Query("SELECT * FROM team_aliases WHERE aliasTeamNumber = :teamNumber LIMIT 1")
    suspend fun getPrimaryForAlias(teamNumber: Int): TeamAlias?
    
    /**
     * Check if a team number is used as primary or alias.
     */
    @Query("SELECT COUNT(*) FROM team_aliases WHERE primaryTeamNumber = :teamNumber OR aliasTeamNumber = :teamNumber")
    suspend fun isTeamInGroup(teamNumber: Int): Int
    
    /**
     * Insert a new team alias.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlias(alias: TeamAlias)
    
    /**
     * Delete a team alias.
     */
    @Delete
    suspend fun deleteAlias(alias: TeamAlias)
    
    /**
     * Delete all aliases for a primary team.
     */
    @Query("DELETE FROM team_aliases WHERE primaryTeamNumber = :primaryTeam")
    suspend fun deleteAliasesForPrimaryTeam(primaryTeam: Int)
    
    /**
     * Delete all team aliases.
     */
    @Query("DELETE FROM team_aliases")
    suspend fun deleteAllAliases()
}
