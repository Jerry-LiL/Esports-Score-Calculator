package com.lilranker.tournament.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.map
import com.lilranker.tournament.data.model.TeamScore
import com.lilranker.tournament.data.repository.TournamentRepository

/**
 * ViewModel for leaderboard display and filtering.
 * 
 * Responsibilities:
 * - Manage day filtering (all days vs specific day)
 * - Manage sort mode (by points vs by team number)
 * - Provide reactive leaderboard data with filters applied
 */
class LeaderboardViewModel(private val repository: TournamentRepository) : ViewModel() {
    
    // ========================================
    // Filter State
    // ========================================
    
    /**
     * Selected day filter.
     * -1 = All Days, otherwise specific day number.
     */
    private val _selectedDay = MutableLiveData<Int>(ALL_DAYS)
    val selectedDay: LiveData<Int> = _selectedDay
    
    /**
     * Sort mode.
     * 0 = by points (descending), 1 = by team number (ascending)
     */
    private val _sortMode = MutableLiveData<Int>(SORT_BY_POINTS)
    val sortMode: LiveData<Int> = _sortMode
    
    // ========================================
    // Leaderboard Data
    // ========================================
    
    /**
     * Raw leaderboard data that responds to day filter changes.
     */
    private val rawLeaderboard: LiveData<List<TeamScore>> = _selectedDay.switchMap { day ->
        if (day == ALL_DAYS) {
            repository.getLeaderboard().asLiveData()
        } else {
            repository.getLeaderboardByDay(day).asLiveData()
        }
    }
    
    /**
     * Leaderboard with sorting applied.
     * Automatically updates when raw data or sort mode changes.
     */
    val leaderboard: LiveData<List<TeamScore>> = rawLeaderboard.switchMap { scores ->
        _sortMode.map { mode ->
            applySorting(scores, mode)
        }
    }
    
    // ========================================
    // Public Methods
    // ========================================
    
    /**
     * Set day filter.
     * 
     * @param day Day number, or -1 for all days
     */
    fun setDayFilter(day: Int) {
        _selectedDay.value = day
    }
    
    /**
     * Set sort mode.
     * 
     * @param mode 0 for points sorting, 1 for team number sorting
     */
    fun setSortMode(mode: Int) {
        _sortMode.value = mode
    }
    
    // ========================================
    // Private Helper Methods
    // ========================================
    
    /**
     * Apply sorting to team scores based on mode.
     */
    private fun applySorting(scores: List<TeamScore>, mode: Int): List<TeamScore> {
        return when (mode) {
            SORT_BY_TEAM_NUMBER -> scores.sortedBy { it.teamNumber }
            else -> scores.sortedWith(
                compareByDescending<TeamScore> { it.totalPoints }
                    .thenByDescending { it.totalKills }
                    .thenBy { it.teamNumber }
            )
        }
    }
    
    companion object {
        const val ALL_DAYS = -1
        const val SORT_BY_POINTS = 0
        const val SORT_BY_TEAM_NUMBER = 1
    }
}
