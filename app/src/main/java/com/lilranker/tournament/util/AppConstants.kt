package com.lilranker.tournament.util

/**
 * Application-wide constants.
 * Centralized location for all constant values used throughout the app.
 */
object AppConstants {
    
    // ========================================
    // Configuration Constants
    // ========================================
    
    /** Fixed total number of teams in the tournament */
    const val TOTAL_TEAMS = 25
    
    /** Minimum number of days for a tournament */
    const val MIN_DAYS = 1
    
    /** Minimum number of matches per day */
    const val MIN_MATCHES_PER_DAY = 1
    
    /** Minimum points per kill (can be 0) */
    const val MIN_POINTS_PER_KILL = 0
    
    /** Maximum number of ranks that can be configured */
    const val MAX_RANKS = TOTAL_TEAMS
    
    // ========================================
    // Day Filter Constants
    // ========================================
    
    /** Value representing "All Days" in day filter */
    const val ALL_DAYS = -1
    
    // ========================================
    // Sort Mode Constants
    // ========================================
    
    /** Sort leaderboard by points (descending) */
    const val SORT_BY_POINTS = 0
    
    /** Sort leaderboard by team number (ascending) */
    const val SORT_BY_TEAM_NUMBER = 1
    
    // ========================================
    // UI Constants
    // ========================================
    
    /** Delay in milliseconds for scroll animations */
    const val SCROLL_DELAY_MS = 100L
    
    /** Invalid match number indicator */
    const val INVALID_MATCH_NUMBER = -1
    
    // ========================================
    // Validation Messages
    // ========================================
    
    object ValidationMessages {
        const val EMPTY_DATA = "⚠️ No team data entered to save"
        const val INVALID_TEAM_NUMBERS = "⚠️ Invalid team numbers detected (must be 1-$TOTAL_TEAMS)"
        const val DUPLICATE_TEAM_NUMBERS = "⚠️ Duplicate team numbers found: %s"
        const val DUPLICATE_RANKS = "⚠️ Duplicate ranks found: %s"
        const val RANKS_NOT_CONSECUTIVE = "⚠️ Ranks must be consecutive (1, 2, 3...) with no gaps"
        const val FILL_ALL_RANKS = "⚠️ Please fill all %d ranks"
        const val TEAM_NUMBER_MIN = "Team number must be at least 1"
        const val TEAM_NUMBER_MAX = "Team number cannot exceed %d"
        const val KILLS_NEGATIVE = "Kills cannot be negative"
        const val INVALID_TOTAL_RANKS = "Please enter a valid number"
        const val TOTAL_RANKS_MIN = "Total ranks must be at least 1"
        const val TOTAL_RANKS_MAX = "Total ranks cannot exceed %d teams"
        const val MATCH_NUMBER_MIN = "Match number must be at least 1"
        const val MATCH_NUMBER_MAX = "⚠️ Match %d exceeds configured limit of %d matches per day"
        const val DAY_NUMBER_RANGE = "Day number must be between 1 and %d"
        const val MATCH_NUMBER_RANGE = "Match number must be between 1 and %d"
        const val PENALTY_POINTS_MIN = "Penalty points must be at least 1"
    }
    
    // ========================================
    // Toast Messages
    // ========================================
    
    object ToastMessages {
        const val NO_MATCH_SELECTED = "⚠️ Please select a match first"
        const val SAVE_SUCCESS = "✅ Match results saved successfully!"
        const val NO_DATA_FOR_DAY = "No data entered for Day %d yet"
        const val NO_DATA_ANY_DAY = "No data entered yet for any day"
        const val SELECT_AT_LEAST_ONE_MATCH = "Please select at least one match"
        const val SELECT_AT_LEAST_ONE_DAY = "Please select at least one day"
        const val ENTER_TEAM_COUNT = "Enter team numbers and kills for %d ranks"
        const val PENALTY_SUCCESS = "✓ Penalty of %d points applied to Team %d for Day %d Match %d"
        const val TEAM_NOT_PARTICIPATED = "⚠️ Team %d did not participate in Day %d Match %d. Cannot apply penalty."
        const val CONFIG_SAVED = "✅ Configuration saved successfully!"
        const val CONFIG_ERROR = "❌ Error: %s"
        const val CONFIG_RESET_SUCCESS = "Configuration reset successfully"
        const val MAX_RANKS_REACHED = "Maximum 25 ranks allowed"
        const val NO_CONFIG_WARNING = "⚠️ Please configure tournament settings first"
    }
    
    // ========================================
    // Dialog Messages
    // ========================================
    
    object DialogMessages {
        const val RESET_ALL_CONFIRM = "⚠️ This will permanently delete ALL match results for ALL days.\n\nAre you absolutely sure?"
        const val RESET_DAY_MATCHES_CONFIRM = "Reset %s for Day %d?\n\nThis will clear all entered data."
        const val RESET_DAY_RANGE_CONFIRM = "Reset data for Days: %s?\n\nThis will clear all entered data for these days."
        const val RESET_SUCCESS = "✅ Successfully reset: %s"
        const val UNSAVED_CHANGES = "You have unsaved changes for Day %d. Save before switching to Day %d?"
        const val CONFIG_RESET_CONFIRM = "Are you sure you want to reset all configuration settings? This will clear:\n\n• Total Days\n• Points Per Kill\n• All Rank Points\n\nThis will NOT delete match results."
        const val ORPHANED_DATA_WARNING = "You are reducing matches per day from %d to %d.\n\nYou have existing data for matches beyond Match %d. This data will be HIDDEN (not deleted) and won't appear in the leaderboard.\n\nDo you want to continue?"
        const val EXIT_WITHOUT_SAVING = "Exit without saving changes?"
    }
    
    // ========================================
    // Field Requirements
    // ========================================
    
    object FieldRequirements {
        const val ALL_FIELDS_REQUIRED = "⚠️ Please fill all required fields (Total Days, Matches Per Day, Points Per Kill)"
        const val TOTAL_DAYS_MIN = "⚠️ Total Days must be at least 1"
        const val MATCHES_PER_DAY_MIN = "⚠️ Matches Per Day must be at least 1"
        const val POINTS_PER_KILL_MIN = "⚠️ Points Per Kill must be 0 or greater"
        const val INVALID_RANK_VALUE = "⚠️ Invalid rank value in rank points"
        const val RANK_POINTS_MIN = "⚠️ Rank points must be 0 or greater"
        const val ENTER_DAY_NUMBER = "Please enter day number"
        const val ENTER_MATCH_NUMBER = "Please enter match number"
        const val ENTER_TEAM_NUMBER = "Please enter team number"
        const val ENTER_PENALTY_POINTS = "Please enter penalty points"
        const val ENTER_NUMBER_OF_TEAMS = "Please enter the number of teams"
        const val VALID_NUMBER_RANGE = "Please enter a valid number between 1 and %d"
        const val PENALTY_POINTS_RANGE = "Penalty points must be 0 or greater"
    }
}
