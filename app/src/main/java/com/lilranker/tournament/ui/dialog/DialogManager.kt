package com.lilranker.tournament.ui.dialog

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.lilranker.tournament.R
import com.lilranker.tournament.data.model.Penalty

/**
 * Central dialog manager for MatchEntryActivity
 * Handles creation and management of all dialog types
 */
class DialogManager(private val context: Context) {
    
    /**
     * Shows dialog to get total number of ranks for a new match
     */
    fun showTotalRanksDialog(
        maxTeams: Int,
        onStart: (totalRanks: Int) -> Unit,
        onCancel: () -> Unit
    ) {
        try {
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_total_ranks_input, null)
            val dialog = AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create()
            
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            
            val etTotalRanks = dialogView.findViewById<TextInputEditText>(R.id.etTotalRanks)
            val btnCancel = dialogView.findViewById<View>(R.id.btnCancel)
            val btnStart = dialogView.findViewById<View>(R.id.btnStart)
            
            if (etTotalRanks == null || btnCancel == null || btnStart == null) {
                android.util.Log.e("DialogManager", "Dialog views not found")
                onCancel()
                return
            }
            
            btnCancel.setOnClickListener {
                dialog.dismiss()
                onCancel()
            }
            
            btnStart.setOnClickListener {
                val totalRanksText = etTotalRanks.text?.toString()?.trim()
                
                if (totalRanksText.isNullOrEmpty()) {
                    Toast.makeText(context, "Please enter the number of teams", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                val totalRanks = totalRanksText.toIntOrNull()
                if (totalRanks == null || totalRanks < 1 || totalRanks > maxTeams) {
                    Toast.makeText(context, "Please enter a valid number between 1 and $maxTeams", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                dialog.dismiss()
                onStart(totalRanks)
            }
            
            dialog.setOnCancelListener {
                onCancel()
            }
            
            dialog.show()
        } catch (e: Exception) {
            android.util.Log.e("DialogManager", "Error showing total ranks dialog", e)
            onCancel()
        }
    }
    
    /**
     * Shows exit warning dialog when there are unsaved changes
     */
    fun showExitWarningDialog(
        onSaveAndExit: () -> Unit,
        onExitWithoutSaving: () -> Unit
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_exit_warning, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val btnSaveAndExit = dialogView.findViewById<View>(R.id.btnSaveAndExit)
        val btnExitWithoutSaving = dialogView.findViewById<View>(R.id.btnExitWithoutSaving)
        
        btnSaveAndExit.setOnClickListener {
            dialog.dismiss()
            onSaveAndExit()
        }
        
        btnExitWithoutSaving.setOnClickListener {
            dialog.dismiss()
            onExitWithoutSaving()
        }
        
        dialog.show()
    }
    
    /**
     * Shows reset options dialog with 3 options: current day, day range, all data
     */
    fun showResetOptionsDialog(
        onResetCurrentDay: () -> Unit,
        onResetDayRange: () -> Unit,
        onResetAllData: () -> Unit
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_reset_options_simple, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        dialogView.findViewById<View>(R.id.cardResetCurrentDay).setOnClickListener {
            dialog.dismiss()
            onResetCurrentDay()
        }
        
        dialogView.findViewById<View>(R.id.cardResetDayRange).setOnClickListener {
            dialog.dismiss()
            onResetDayRange()
        }
        
        dialogView.findViewById<View>(R.id.cardResetAllData).setOnClickListener {
            dialog.dismiss()
            onResetAllData()
        }
        
        dialogView.findViewById<View>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    /**
     * Shows match selection dialog for resetting specific matches in a day
     */
    fun showMatchSelectionDialog(
        day: Int,
        matchesWithData: List<Int>,
        onReset: (matchesToReset: List<Int>) -> Unit
    ) {
        if (matchesWithData.isEmpty()) {
            Toast.makeText(context, "No data entered for Day $day yet", Toast.LENGTH_SHORT).show()
            return
        }
        
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_select_matches, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val tvDayTitle = dialogView.findViewById<TextView>(R.id.tvDayTitle)
        val checkboxContainer = dialogView.findViewById<LinearLayout>(R.id.checkboxContainer)
        val btnReset = dialogView.findViewById<View>(R.id.btnReset)
        val btnCancel = dialogView.findViewById<View>(R.id.btnCancel)
        
        tvDayTitle.text = "Reset Day $day"
        checkboxContainer.removeAllViews()
        
        val checkBoxes = mutableMapOf<Int, CheckBox>()
        matchesWithData.sorted().forEach { matchNum ->
            val checkBox = CheckBox(context).apply {
                text = "Match $matchNum"
                setTextColor(context.getColor(R.color.text_primary))
                textSize = 16f
                setPadding(12, 12, 12, 12)
                buttonTintList = android.content.res.ColorStateList.valueOf(context.getColor(R.color.neon_blue))
            }
            
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 8 }
            
            checkBox.layoutParams = layoutParams
            checkboxContainer.addView(checkBox)
            checkBoxes[matchNum] = checkBox
        }
        
        btnReset.setOnClickListener {
            val matchesToReset = checkBoxes.filter { it.value.isChecked }.keys.toList()
            
            if (matchesToReset.isEmpty()) {
                Toast.makeText(context, "Please select at least one match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            dialog.dismiss()
            onReset(matchesToReset)
        }
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    /**
     * Shows day range selection dialog with checkboxes for days with data
     */
    fun showDayRangeDialog(
        daysWithData: List<Int>,
        onReset: (selectedDays: List<Int>) -> Unit
    ) {
        if (daysWithData.isEmpty()) {
            Toast.makeText(context, "No data entered yet for any day", Toast.LENGTH_SHORT).show()
            return
        }
        
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_select_day_range, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val tvDaysInfo = dialogView.findViewById<TextView>(R.id.tvDaysInfo)
        val layoutDayCheckboxes = dialogView.findViewById<LinearLayout>(R.id.layoutDayCheckboxes)
        val btnReset = dialogView.findViewById<View>(R.id.btnReset)
        val btnCancel = dialogView.findViewById<View>(R.id.btnCancel)
        
        val sortedDays = daysWithData.sorted()
        tvDaysInfo.text = "Days with entered data: ${sortedDays.joinToString(", ")}"
        
        val dayCheckBoxes = mutableMapOf<Int, CheckBox>()
        sortedDays.forEach { day ->
            val checkBox = CheckBox(context).apply {
                text = "Day $day"
                textSize = 16f
                setTextColor(context.getColor(R.color.text_primary))
                buttonTintList = android.content.res.ColorStateList.valueOf(context.getColor(R.color.neon_blue))
                setPadding(12, 12, 12, 12)
            }
            layoutDayCheckboxes.addView(checkBox)
            dayCheckBoxes[day] = checkBox
        }
        
        btnReset.setOnClickListener {
            val selectedDays = dayCheckBoxes.filter { it.value.isChecked }.keys.toList()
            
            if (selectedDays.isEmpty()) {
                Toast.makeText(context, "Please select at least one day", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            dialog.dismiss()
            onReset(selectedDays)
        }
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    /**
     * Shows confirmation dialog for reset operations
     */
    fun showConfirmationDialog(
        title: String,
        message: String,
        onConfirm: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.yes) { _, _ -> onConfirm() }
            .setNegativeButton(R.string.no, null)
            .show()
    }
    
    /**
     * Shows penalty dialog for applying penalties to teams
     */
    fun showPenaltyDialog(
        currentDay: Int,
        currentMatch: Int?,
        totalDays: Int,
        matchesPerDay: Int,
        totalTeams: Int,
        onApplyPenalty: (penalty: Penalty) -> Unit
    ) {
        try {
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_penalty, null)
            val dialog = AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create()
            
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            
            val etPenaltyDay = dialogView.findViewById<TextInputEditText>(R.id.etPenaltyDay)
            val etPenaltyMatch = dialogView.findViewById<TextInputEditText>(R.id.etPenaltyMatch)
            val etTeamNumber = dialogView.findViewById<TextInputEditText>(R.id.etTeamNumber)
            val etPenaltyPoints = dialogView.findViewById<TextInputEditText>(R.id.etPenaltyPoints)
            val btnCancel = dialogView.findViewById<View>(R.id.btnCancelPenalty)
            val btnApply = dialogView.findViewById<View>(R.id.btnApplyPenalty)
            
            if (etPenaltyDay == null || etPenaltyMatch == null || etTeamNumber == null || 
                etPenaltyPoints == null || btnCancel == null || btnApply == null) {
                android.util.Log.e("DialogManager", "Penalty dialog views not found")
                return
            }
            
            // Pre-fill with current day and match if selected
            etPenaltyDay.setText(currentDay.toString())
            currentMatch?.let { etPenaltyMatch.setText(it.toString()) }
            
            btnCancel.setOnClickListener {
                dialog.dismiss()
            }
            
            btnApply.setOnClickListener {
                val dayText = etPenaltyDay.text?.toString()?.trim()
                val matchText = etPenaltyMatch.text?.toString()?.trim()
                val teamNumberText = etTeamNumber.text?.toString()?.trim()
                val penaltyPointsText = etPenaltyPoints.text?.toString()?.trim()
                
                // Validation
                if (dayText.isNullOrEmpty()) {
                    Toast.makeText(context, "Please enter day number", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                if (matchText.isNullOrEmpty()) {
                    Toast.makeText(context, "Please enter match number", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                if (teamNumberText.isNullOrEmpty()) {
                    Toast.makeText(context, "Please enter team number", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                if (penaltyPointsText.isNullOrEmpty()) {
                    Toast.makeText(context, "Please enter penalty points", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                val day = dayText.toIntOrNull()
                val match = matchText.toIntOrNull()
                val teamNumber = teamNumberText.toIntOrNull()
                val penaltyPoints = penaltyPointsText.toIntOrNull()
                
                if (day == null || day < 1 || day > totalDays) {
                    Toast.makeText(context, "Day number must be between 1 and $totalDays", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                if (match == null || match < 1 || match > matchesPerDay) {
                    Toast.makeText(context, "Match number must be between 1 and $matchesPerDay", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                if (teamNumber == null || teamNumber < 1 || teamNumber > totalTeams) {
                    Toast.makeText(context, "Team number must be between 1 and $totalTeams", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                if (penaltyPoints == null || penaltyPoints < 0) {
                    Toast.makeText(context, "Penalty points must be 0 or greater", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                dialog.dismiss()
                
                val penalty = Penalty(
                    day = day,
                    matchNumber = match,
                    teamNumber = teamNumber,
                    penaltyPoints = penaltyPoints
                )
                
                onApplyPenalty(penalty)
            }
            
            dialog.show()
        } catch (e: Exception) {
            android.util.Log.e("DialogManager", "Error showing penalty dialog", e)
            Toast.makeText(context, "Error showing penalty dialog: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Shows manual score adjustment dialog
     */
    fun showManualScoreDialog(
        currentDay: Int,
        currentMatch: Int?,
        totalDays: Int,
        matchesPerDay: Int,
        totalTeams: Int,
        onApplyScore: (day: Int, match: Int, teamNumber: Int, newScore: Int) -> Unit
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_manual_score, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val etScoreDay = dialogView.findViewById<TextInputEditText>(R.id.etScoreDay)
        val etScoreMatch = dialogView.findViewById<TextInputEditText>(R.id.etScoreMatch)
        val etScoreTeamNumber = dialogView.findViewById<TextInputEditText>(R.id.etScoreTeamNumber)
        val etNewScore = dialogView.findViewById<TextInputEditText>(R.id.etNewScore)
        val btnCancel = dialogView.findViewById<View>(R.id.btnCancelScore)
        val btnApply = dialogView.findViewById<View>(R.id.btnApplyScore)
        
        // Pre-fill with current day and match if selected
        etScoreDay.setText(currentDay.toString())
        currentMatch?.let { etScoreMatch.setText(it.toString()) }
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        btnApply.setOnClickListener {
            val dayText = etScoreDay.text.toString()
            val matchText = etScoreMatch.text.toString()
            val teamNumberText = etScoreTeamNumber.text.toString()
            val newScoreText = etNewScore.text.toString()
            
            // Validation
            if (dayText.isEmpty()) {
                Toast.makeText(context, "Please enter day number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (matchText.isEmpty()) {
                Toast.makeText(context, "Please enter match number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (teamNumberText.isEmpty()) {
                Toast.makeText(context, "Please enter team number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (newScoreText.isEmpty()) {
                Toast.makeText(context, "Please enter new score", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val day = dayText.toIntOrNull()
            val match = matchText.toIntOrNull()
            val teamNumber = teamNumberText.toIntOrNull()
            val newScore = newScoreText.toIntOrNull()
            
            if (day == null || day < 1 || day > totalDays) {
                Toast.makeText(context, "Day number must be between 1 and $totalDays", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (match == null || match < 1 || match > matchesPerDay) {
                Toast.makeText(context, "Match number must be between 1 and $matchesPerDay", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (teamNumber == null || teamNumber < 1 || teamNumber > totalTeams) {
                Toast.makeText(context, "Team number must be between 1 and $totalTeams", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (newScore == null || newScore < 0) {
                Toast.makeText(context, "Score must be 0 or greater", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            dialog.dismiss()
            onApplyScore(day, match, teamNumber, newScore)
        }
        
        dialog.show()
    }
}
