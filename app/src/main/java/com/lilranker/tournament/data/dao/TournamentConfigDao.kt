package com.lilranker.tournament.data.dao

import androidx.room.*
import com.lilranker.tournament.data.model.TournamentConfig
import kotlinx.coroutines.flow.Flow

/**
 * DAO for tournament configuration operations.
 * 
 * Note: Only one configuration should exist at a time.
 * The most recent configuration (by createdAt) is considered current.
 */
@Dao
interface TournamentConfigDao {
    
    /**
     * Get current configuration as a Flow.
     * Automatically emits new values when configuration changes.
     * 
     * @return Flow of current config, null if none exists
     */
    @Query("SELECT * FROM tournament_config ORDER BY createdAt DESC LIMIT 1")
    fun getCurrentConfig(): Flow<TournamentConfig?>
    
    /**
     * Get current configuration synchronously.
     * 
     * @return Current config, null if none exists
     */
    @Query("SELECT * FROM tournament_config ORDER BY createdAt DESC LIMIT 1")
    suspend fun getCurrentConfigSync(): TournamentConfig?
    
    /**
     * Insert or replace a configuration.
     * 
     * @param config Configuration to save
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: TournamentConfig)
    
    /**
     * Delete all configurations.
     * Used before inserting new configuration to ensure only one exists.
     */
    @Query("DELETE FROM tournament_config")
    suspend fun deleteAllConfigs()
}
