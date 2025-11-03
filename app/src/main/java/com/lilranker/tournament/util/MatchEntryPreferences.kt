package com.lilranker.tournament.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages persistent preferences for Match Entry Activity.
 * Stores the last viewed day and match to restore state when app is reopened.
 */
class MatchEntryPreferences(context: Context) {
    
    companion object {
        private const val PREF_NAME = "match_entry_prefs"
        private const val KEY_LAST_SELECTED_DAY = "last_selected_day"
        private const val KEY_LAST_SELECTED_MATCH = "last_selected_match"
        private const val KEY_LAST_INTERACTED_TIMESTAMP = "last_interacted_timestamp"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = prefs.edit()
    
    /**
     * Save the last selected day and match.
     * Called whenever user selects a different day or match.
     */
    fun saveLastSelection(day: Int, matchNumber: Int) {
        editor.apply {
            putInt(KEY_LAST_SELECTED_DAY, day)
            putInt(KEY_LAST_SELECTED_MATCH, matchNumber)
            putLong(KEY_LAST_INTERACTED_TIMESTAMP, System.currentTimeMillis())
            apply()
        }
    }
    
    /**
     * Get the last selected day.
     * Returns 1 if no previous selection exists.
     */
    fun getLastSelectedDay(): Int {
        return prefs.getInt(KEY_LAST_SELECTED_DAY, 1)
    }
    
    /**
     * Get the last selected match.
     * Returns -1 if no previous selection exists.
     */
    fun getLastSelectedMatch(): Int {
        return prefs.getInt(KEY_LAST_SELECTED_MATCH, -1)
    }
    
    /**
     * Clear all saved preferences.
     * Useful when resetting all tournament data.
     */
    fun clearAll() {
        editor.clear().apply()
    }
    
    /**
     * Check if there is a saved selection.
     */
    fun hasSavedSelection(): Boolean {
        return prefs.contains(KEY_LAST_SELECTED_DAY) && prefs.contains(KEY_LAST_SELECTED_MATCH)
    }
}
