package com.lilranker.tournament.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.lilranker.tournament.data.model.MatchResult
import com.lilranker.tournament.data.model.Penalty
import com.lilranker.tournament.data.model.TournamentConfig
import com.lilranker.tournament.data.repository.TournamentRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for match entry operations.
 * 
 * Responsibilities:
 * - Manage match result CRUD operations
 * - Provide UI state for match entry screen
 * - Handle reset operations (match, day, day range, all data)
 * - Manage penalty operations
 * - Track data existence for validation
 */
class MatchEntryViewModel(private val repository: TournamentRepository) : ViewModel() {
    
    // ========================================
    // Observable State
    // ========================================
    
    private val _currentConfig = MutableLiveData<TournamentConfig?>()
    val currentConfig: LiveData<TournamentConfig?> = _currentConfig
    
    private val _matchResultsSaved = MutableLiveData<Boolean>()
    val matchResultsSaved: LiveData<Boolean> = _matchResultsSaved
    
    private val _existingResults = MutableLiveData<List<MatchResult>>()
    val existingResults: LiveData<List<MatchResult>> = _existingResults
    
    private val _resetCompleted = MutableLiveData<String>()
    val resetCompleted: LiveData<String> = _resetCompleted
    
    init {
        // Load initial config
        reloadConfig()
    }
    
    // ========================================
    // Match Result Operations
    // ========================================
    
    /**
     * Load existing results for a specific match.
     * Always posts value, even if empty list.
     */
    fun loadExistingResults(day: Int, matchNumber: Int) {
        viewModelScope.launch {
            try {
                val results = repository.getMatchResultsSync(day, matchNumber)
                _existingResults.postValue(results)
            } catch (e: Exception) {
                android.util.Log.e("MatchEntryViewModel", "Error loading existing results", e)
                _existingResults.postValue(emptyList())
            }
        }
    }
    
    /**
     * Save match results for a specific day and match.
     * Deletes existing results first, then saves new ones.
     * Only participating teams (teamNumber > 0) are saved.
     * 
     * @param day Day number
     * @param matchNumber Match number
     * @param teamData List of (rank, teamNumber, kills) triples
     */
    fun saveMatchResults(day: Int, matchNumber: Int, teamData: List<Triple<Int, Int, Int>>) {
        viewModelScope.launch {
            try {
                val config = repository.getCurrentConfigSync()
                if (config == null) {
                    android.util.Log.e("MatchEntryViewModel", "Cannot save: config is null")
                    _matchResultsSaved.postValue(false)
                    return@launch
                }
                
                // Parse rank points from configuration
                val rankPoints = repository.parseRankPoints(config.rankPoints)
                
                android.util.Log.d("MatchEntryViewModel", "Saving match results - Config: PointsPerKill=${config.pointsPerKill}, RankPoints=${rankPoints}")
                
                // Delete existing results for this match
                repository.deleteMatchResults(day, matchNumber)
                
                // Create and save new results (only for participating teams)
                val results = buildMatchResults(
                    day = day,
                    matchNumber = matchNumber,
                    teamData = teamData,
                    config = config,
                    rankPoints = rankPoints
                )
                
                if (results.isEmpty()) {
                    android.util.Log.w("MatchEntryViewModel", "No results to save")
                    _matchResultsSaved.postValue(false)
                    return@launch
                }
                
                android.util.Log.d("MatchEntryViewModel", "Built ${results.size} match results, first result: ${results.firstOrNull()}")
                
                repository.saveMatchResults(results)
                _matchResultsSaved.postValue(true)
            } catch (e: Exception) {
                android.util.Log.e("MatchEntryViewModel", "Error saving match results", e)
                _matchResultsSaved.postValue(false)
            }
        }
    }
    
    /**
     * Delete match results for a specific match.
     */
    fun deleteMatchResults(day: Int, matchNumber: Int) {
        viewModelScope.launch {
            repository.deleteMatchResults(day, matchNumber)
        }
    }
    
    /**
     * Reset saved state flag.
     */
    fun resetSavedState() {
        _matchResultsSaved.value = false
    }
    
    // ========================================
    // Reset Operations
    // ========================================
    
    /**
     * Reset data for current match (both results and penalties).
     */
    fun resetCurrentMatch(day: Int, matchNumber: Int) {
        viewModelScope.launch {
            try {
                repository.deleteMatchResults(day, matchNumber)
                repository.deletePenaltiesForMatch(day, matchNumber)
                _resetCompleted.postValue("Match $matchNumber on Day $day")
                loadExistingResults(day, matchNumber)
            } catch (e: Exception) {
                android.util.Log.e("MatchEntryViewModel", "Error resetting match", e)
                _resetCompleted.postValue("")
            }
        }
    }
    
    /**
     * Reset all match data for a specific day (both results and penalties).
     */
    fun resetAllMatchesForDay(day: Int) {
        viewModelScope.launch {
            try {
                repository.deleteMatchResultsByDay(day)
                repository.deletePenaltiesByDay(day)
                _resetCompleted.postValue("All matches on Day $day")
            } catch (e: Exception) {
                android.util.Log.e("MatchEntryViewModel", "Error resetting day", e)
                _resetCompleted.postValue("")
            }
        }
    }
    
    /**
     * Reset data for a range of days (both results and penalties).
     */
    fun resetDayRange(startDay: Int, endDay: Int) {
        viewModelScope.launch {
            try {
                repository.deleteMatchResultsByDayRange(startDay, endDay)
                repository.deletePenaltiesByDayRange(startDay, endDay)
                _resetCompleted.postValue("Days $startDay to $endDay")
            } catch (e: Exception) {
                android.util.Log.e("MatchEntryViewModel", "Error resetting day range", e)
                _resetCompleted.postValue("")
            }
        }
    }
    
    /**
     * Reset all tournament data (all results and penalties).
     */
    fun resetAllTournamentData() {
        viewModelScope.launch {
            try {
                repository.deleteAllResults()
                repository.deleteAllPenalties()
                _resetCompleted.postValue("All tournament data")
            } catch (e: Exception) {
                android.util.Log.e("MatchEntryViewModel", "Error resetting all data", e)
                _resetCompleted.postValue("")
            }
        }
    }
    
    // ========================================
    // Data Query Operations
    // ========================================
    
    /**
     * Get list of days that have data entered.
     */
    suspend fun getDaysWithData(): List<Int> {
        return repository.getDaysWithData()
    }
    
    /**
     * Get list of matches with data for a specific day.
     */
    suspend fun getMatchesWithDataForDay(day: Int): List<Int> {
        return repository.getMatchesWithDataForDay(day)
    }
    
    /**
     * Get count of results for a specific match.
     */
    suspend fun getMatchResultsCount(day: Int, matchNumber: Int): Int {
        val results = repository.getMatchResultsSync(day, matchNumber)
        return results.size
    }
    
    /**
     * Get the maximum rank from existing results for a match.
     * Returns 0 if no results exist.
     */
    suspend fun getMaxRankForMatch(day: Int, matchNumber: Int): Int {
        val results = repository.getMatchResultsSync(day, matchNumber)
        return results.maxOfOrNull { it.rank } ?: 0
    }
    
    /**
     * Get match results synchronously.
     */
    suspend fun getMatchResultsSync(day: Int, matchNumber: Int): List<MatchResult> {
        return repository.getMatchResultsSync(day, matchNumber)
    }
    
    /**
     * Get the last day that has match data.
     * Returns null if no data exists.
     */
    suspend fun getLastDayWithData(): Int? {
        return repository.getLastDayWithData()
    }
    
    /**
     * Get the last match number for a specific day that has data.
     * Returns null if no data exists for that day.
     */
    suspend fun getLastMatchWithDataForDay(day: Int): Int? {
        return repository.getLastMatchWithDataForDay(day)
    }
    
    // ========================================
    // Penalty Operations
    // ========================================
    
    /**
     * Save a penalty for a team.
     */
    suspend fun savePenalty(penalty: Penalty) {
        repository.savePenalty(penalty)
    }
    
    /**
     * Manually update a team's score for a specific match.
     * Replaces the existing total points for that team.
     */
    suspend fun updateManualScore(day: Int, matchNumber: Int, teamNumber: Int, newScore: Int) {
        repository.updateTeamScore(day, matchNumber, teamNumber, newScore)
    }
    
    // ========================================
    // Configuration Operations
    // ========================================
    
    /**
     * Reload configuration from database.
     * Used when returning to activity after config changes.
     * Always emits value to ensure UI updates even if config values are same.
     */
    fun reloadConfig() {
        viewModelScope.launch {
            val config = repository.getCurrentConfigSync()
            
            // Use postValue to update from background thread
            // Create a new copy to ensure reference change and trigger observers
            val configCopy = config?.copy()
            _currentConfig.postValue(configCopy)
        }
    }
    
    // ========================================
    // Private Helper Methods
    // ========================================
    
    /**
     * Build list of MatchResult objects from team data.
     * Filters out non-participating teams (teamNumber = 0).
     */
    private fun buildMatchResults(
        day: Int,
        matchNumber: Int,
        teamData: List<Triple<Int, Int, Int>>,
        config: TournamentConfig,
        rankPoints: Map<Int, Int>
    ): List<MatchResult> {
        return try {
            android.util.Log.d("MatchEntryViewModel", "buildMatchResults - PointsPerKill: ${config.pointsPerKill}, RankPoints: $rankPoints")
            
            val results = teamData
                .filter { (_, teamNumber, _) -> teamNumber > 0 }
                .map { (rank, teamNumber, kills) ->
                    val killPoints = kills * config.pointsPerKill
                    val positionPoints = rankPoints[rank] ?: 0
                    val totalPoints = (killPoints + positionPoints).coerceAtLeast(0)
                    
                    android.util.Log.d("MatchEntryViewModel", 
                        "Team $teamNumber: Rank=$rank, Kills=$kills -> " +
                        "KillPoints=$killPoints, PositionPoints=$positionPoints, Total=$totalPoints")
                    
                    MatchResult(
                        day = day,
                        matchNumber = matchNumber,
                        teamNumber = teamNumber,
                        kills = kills.coerceAtLeast(0),
                        rank = rank,
                        totalPoints = totalPoints
                    )
                }
            
            results
        } catch (e: Exception) {
            android.util.Log.e("MatchEntryViewModel", "Error building match results", e)
            emptyList()
        }
    }
}
