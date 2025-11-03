package com.lilranker.tournament.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lilranker.tournament.data.model.TournamentConfig
import com.lilranker.tournament.data.repository.TournamentRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for tournament configuration management.
 * 
 * Responsibilities:
 * - Manage configuration save/delete operations
 * - Provide UI state for configuration operations
 * - Handle configuration validation and conflicts
 */
class ConfigViewModel(private val repository: TournamentRepository) : ViewModel() {
    
    // ========================================
    // UI State
    // ========================================
    
    private val _configSaved = MutableLiveData<Boolean>()
    val configSaved: LiveData<Boolean> = _configSaved
    
    private val _configError = MutableLiveData<String>()
    val configError: LiveData<String> = _configError
    
    // ========================================
    // Public Methods
    // ========================================
    
    /**
     * Save tournament configuration.
     * Automatically triggers recalculation of match results if scoring changes.
     * 
     * @param totalDays Number of tournament days
     * @param matchesPerDay Number of matches per day
     * @param pointsPerKill Points awarded per kill
     * @param rankPoints Map of rank position to points
     */
    fun saveConfiguration(
        totalDays: Int,
        matchesPerDay: Int,
        pointsPerKill: Int,
        rankPoints: Map<Int, Int>
    ) {
        viewModelScope.launch {
            try {
                android.util.Log.d("CONFIG_SAVE", "Saving: days=$totalDays, matchesPerDay=$matchesPerDay, pointsPerKill=$pointsPerKill")
                
                val config = buildTournamentConfig(
                    totalDays = totalDays,
                    matchesPerDay = matchesPerDay,
                    pointsPerKill = pointsPerKill,
                    rankPoints = rankPoints
                )
                
                repository.saveConfig(config)
                
                // Verify save
                val savedConfig = repository.getCurrentConfigSync()
                android.util.Log.d("CONFIG_SAVE", "Verified: matchesPerDay=${savedConfig?.matchesPerDay}")
                
                _configSaved.value = true
                _configError.value = "" // Clear any previous errors
            } catch (e: Exception) {
                android.util.Log.e("CONFIG_SAVE", "Error: ${e.message}", e)
                _configError.value = e.message ?: "Unknown error occurred"
                _configSaved.value = false
            }
        }
    }
    
    /**
     * Get current configuration asynchronously.
     * 
     * @param callback Callback invoked with configuration (may be null)
     */
    fun getCurrentConfig(callback: (TournamentConfig?) -> Unit) {
        viewModelScope.launch {
            val config = repository.getCurrentConfigSync()
            callback(config)
        }
    }
    
    /**
     * Delete current configuration.
     * Does not affect match results or penalties.
     */
    fun deleteConfiguration() {
        viewModelScope.launch {
            repository.deleteConfiguration()
        }
    }
    
    /**
     * Check if reducing matches per day would hide existing data.
     * 
     * @param newMatchesPerDay The new matches per day value
     * @return True if data exists beyond the new limit
     */
    suspend fun checkMatchesPerDayConflict(newMatchesPerDay: Int): Boolean {
        return repository.hasMatchDataBeyondNumber(newMatchesPerDay)
    }
    
    /**
     * Check if any match data exists.
     * Used to determine if View Leaderboard should be visible.
     * 
     * @return True if at least one match has data entered
     */
    suspend fun hasAnyMatchData(): Boolean {
        return repository.hasAnyMatchData()
    }
    
    // ========================================
    // Team Alias Operations
    // ========================================
    
    /**
     * Get all team aliases.
     */
    suspend fun getAllTeamAliases(): List<com.lilranker.tournament.data.model.TeamAlias> {
        return repository.getAllTeamAliases()
    }
    
    /**
     * Add a team alias.
     */
    suspend fun addTeamAlias(alias: com.lilranker.tournament.data.model.TeamAlias) {
        repository.addTeamAlias(alias)
    }
    
    /**
     * Delete all aliases for a primary team.
     */
    suspend fun deleteAliasesForPrimaryTeam(primaryTeam: Int) {
        repository.deleteAliasesForPrimaryTeam(primaryTeam)
    }
    
    // ========================================
    // Private Helper Methods
    // ========================================
    
    /**
     * Build a TournamentConfig object from parameters.
     */
    private fun buildTournamentConfig(
        totalDays: Int,
        matchesPerDay: Int,
        pointsPerKill: Int,
        rankPoints: Map<Int, Int>
    ): TournamentConfig {
        val rankPointsJson = repository.createRankPointsJson(rankPoints)
        
        return TournamentConfig(
            totalDays = totalDays,
            matchesPerDay = matchesPerDay,
            totalTeams = FIXED_TOTAL_TEAMS,
            pointsPerKill = pointsPerKill,
            rankPoints = rankPointsJson
        )
    }
    
    companion object {
        private const val FIXED_TOTAL_TEAMS = 25
    }
}
