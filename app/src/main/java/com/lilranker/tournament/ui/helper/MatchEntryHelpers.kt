package com.lilranker.tournament.ui.helper

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import com.google.android.material.radiobutton.MaterialRadioButton
import com.lilranker.tournament.R

/**
 * Helper class to manage match selection UI components
 */
class MatchSelectionHelper(private val context: Context) {
    
    /**
     * Creates radio buttons dynamically for match selection
     * @param radioGroup The RadioGroup to add buttons to
     * @param matchesPerDay Number of matches to create buttons for
     * @return Map of match number to radio button ID
     */
    fun createMatchRadioButtons(
        radioGroup: android.widget.RadioGroup,
        matchesPerDay: Int
    ): Map<Int, Int> {
        radioGroup.removeAllViews()
        
        val matchIdMap = mutableMapOf<Int, Int>()
        
        for (i in 1..matchesPerDay) {
            val radioButton = MaterialRadioButton(context).apply {
                text = "Match $i"
                textSize = 16f
                setTextColor(context.getColor(R.color.text_primary))
                buttonTintList = android.content.res.ColorStateList.valueOf(
                    context.getColor(R.color.neon_blue)
                )
                id = View.generateViewId()
            }
            
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
            
            radioButton.layoutParams = params
            radioGroup.addView(radioButton)
            
            matchIdMap[i] = radioButton.id
        }
        
        return matchIdMap
    }
    
    /**
     * Gets the match number from a checked radio button ID
     * @param radioGroup The RadioGroup containing the buttons
     * @param checkedId The ID of the checked button
     * @return Match number (1-indexed), or null if not found
     */
    fun getMatchNumberFromCheckedId(
        radioGroup: android.widget.RadioGroup,
        checkedId: Int
    ): Int? {
        for (i in 0 until radioGroup.childCount) {
            val radioButton = radioGroup.getChildAt(i)
            if (radioButton.id == checkedId) {
                return i + 1
            }
        }
        return null
    }
}

/**
 * Helper class for scroll operations
 */
class ScrollHelper {
    /**
     * Smoothly scrolls to a view
     */
    fun smoothScrollToView(
        scrollView: androidx.core.widget.NestedScrollView,
        targetView: View
    ) {
        scrollView.post {
            scrollView.smoothScrollTo(0, targetView.top)
        }
    }
}

/**
 * Constants for Match Entry Activity
 */
object MatchEntryConstants {
    // Toast messages
    const val MSG_NO_MATCH_SELECTED = "⚠️ Please select a match first"
    const val MSG_NO_DATA_TO_SAVE = "⚠️ No team data entered to save"
    const val MSG_SAVE_SUCCESS = "✅ Match results saved successfully!"
    const val MSG_NO_DATA_FOR_DAY = "No data entered for Day %d yet"
    const val MSG_NO_DATA_ANY_DAY = "No data entered yet for any day"
    const val MSG_SELECT_AT_LEAST_ONE_MATCH = "Please select at least one match"
    const val MSG_SELECT_AT_LEAST_ONE_DAY = "Please select at least one day"
    const val MSG_ENTER_TEAM_COUNT = "Enter team numbers and kills for %d ranks"
    const val MSG_MATCH_EXCEEDS_LIMIT = "⚠️ Match %d exceeds configured limit of %d matches per day"
    const val MSG_PENALTY_SUCCESS = "✓ Penalty of %d points applied to Team %d for Day %d Match %d"
    const val MSG_TEAM_NOT_PARTICIPATED = "⚠️ Team %d did not participate in Day %d Match %d. Cannot apply penalty."
    
    // Dialog messages
    const val MSG_RESET_ALL_CONFIRM = "⚠️ This will permanently delete ALL match results for ALL days.\n\nAre you absolutely sure?"
    const val MSG_RESET_DAY_MATCHES_CONFIRM = "Reset %s for Day %d?\n\nThis will clear all entered data."
    const val MSG_RESET_DAY_RANGE_CONFIRM = "Reset data for Days: %s?\n\nThis will clear all entered data for these days."
    
    // Success messages
    const val MSG_RESET_SUCCESS = "✅ Successfully reset: %s"
    
    // View visibility constants
    const val INVALID_MATCH_NUMBER = -1
    
    // Scroll delay
    const val SCROLL_DELAY_MS = 100L
}
