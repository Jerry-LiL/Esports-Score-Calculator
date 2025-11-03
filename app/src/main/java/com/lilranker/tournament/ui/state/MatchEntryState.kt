package com.lilranker.tournament.ui.state

/**
 * Represents the various states of the Match Entry screen
 */
sealed class MatchEntryState {
    /**
     * Initial loading state - configuration is being loaded
     */
    object Loading : MatchEntryState()
    
    /**
     * Configuration loaded successfully
     * @param totalDays Total days in tournament
     * @param matchesPerDay Matches per day configured
     * @param totalTeams Total teams in tournament
     */
    data class ConfigLoaded(
        val totalDays: Int,
        val matchesPerDay: Int,
        val totalTeams: Int
    ) : MatchEntryState()
    
    /**
     * A match has been selected and is ready for data entry
     * @param day Selected day
     * @param matchNumber Selected match number
     * @param totalRanks Number of ranks for this match
     * @param hasExistingData Whether this match already has saved data
     */
    data class MatchSelected(
        val day: Int,
        val matchNumber: Int,
        val totalRanks: Int,
        val hasExistingData: Boolean
    ) : MatchEntryState()
    
    /**
     * Data entry is in progress
     * @param hasUnsavedChanges Whether there are unsaved changes
     */
    data class DataEntry(
        val hasUnsavedChanges: Boolean
    ) : MatchEntryState()
    
    /**
     * Data was saved successfully
     * @param message Success message
     */
    data class SaveSuccess(
        val message: String
    ) : MatchEntryState()
    
    /**
     * An error occurred
     * @param message Error message to display
     */
    data class Error(
        val message: String
    ) : MatchEntryState()
    
    /**
     * No configuration found - user needs to configure first
     */
    object NoConfig : MatchEntryState()
}

/**
 * Represents the result of an operation
 */
sealed class OperationResult<out T> {
    data class Success<T>(val data: T) : OperationResult<T>()
    data class Failure(val error: String) : OperationResult<Nothing>()
    object Loading : OperationResult<Nothing>()
}

/**
 * Represents reset operation types
 */
sealed class ResetType {
    data class CurrentMatch(val day: Int, val matchNumber: Int) : ResetType()
    data class SpecificMatches(val day: Int, val matches: List<Int>) : ResetType()
    data class DayRange(val days: List<Int>) : ResetType()
    object AllData : ResetType()
}
