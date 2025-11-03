package com.lilranker.tournament.ui

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.lilranker.tournament.R
import com.lilranker.tournament.TournamentApplication
import com.lilranker.tournament.data.model.Penalty
import com.lilranker.tournament.databinding.ActivityMatchEntryBinding
import com.lilranker.tournament.ui.adapter.TeamEntryAdapter
import com.lilranker.tournament.ui.dialog.DialogManager
import com.lilranker.tournament.ui.helper.MatchEntryConstants
import com.lilranker.tournament.ui.helper.MatchSelectionHelper
import com.lilranker.tournament.ui.helper.ScrollHelper
import com.lilranker.tournament.ui.validation.MatchDataValidator
import com.lilranker.tournament.ui.validation.ValidationResult
import com.lilranker.tournament.ui.viewmodel.MatchEntryViewModel
import com.lilranker.tournament.ui.viewmodel.TournamentViewModelFactory
import com.lilranker.tournament.util.MatchEntryPreferences
import kotlinx.coroutines.launch

/**
 * Match Entry Activity - Refactored for better maintainability
 * 
 * Responsibilities:
 * - Coordinates UI components and user interactions
 * - Delegates dialog management to DialogManager
 * - Delegates validation to MatchDataValidator
 * - Manages state through ViewModel
 * 
 * Architecture Pattern: MVVM with Repository
 */
class MatchEntryActivity : BaseActivity() {
    
    // View Binding
    private lateinit var binding: ActivityMatchEntryBinding
    
    // ViewModel
    private lateinit var viewModel: MatchEntryViewModel
    
    // Adapter
    private lateinit var adapter: TeamEntryAdapter
    
    // Helpers
    private lateinit var dialogManager: DialogManager
    private lateinit var matchSelectionHelper: MatchSelectionHelper
    private lateinit var scrollHelper: ScrollHelper
    private lateinit var validator: MatchDataValidator
    private lateinit var matchPreferences: MatchEntryPreferences
    
    // State
    private var selectedDay = 1
    private var selectedMatch: Int? = null
    private var totalDays = 7
    private var matchesPerDay = 0
    private var totalTeams = 25
    private var totalRanksForCurrentMatch = 0
    private var hasUnsavedChanges = false
    private var selectedMatchBox: TextView? = null
    private val matchBoxes = mutableMapOf<Int, TextView>() // Track all match boxes
    private val matchCompletionStatus = mutableMapOf<Int, Boolean>() // Track if match data is complete
    
    // Store unsaved match data temporarily when switching between matches
    // Key: "day_matchNumber" -> Map of rank to (teamNumber, kills)
    private val unsavedMatchData = mutableMapOf<String, Pair<Map<Int, Pair<Int, Int>>, Int>>()
    
    // Track loaded state to prevent multiple initializations
    private var isInitialLoadComplete = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMatchEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initializeViewModel()
        initializeHelpers()
        setupRecyclerView()
        observeViewModel()
        setupUI()
    }
    
    override fun onResume() {
        super.onResume()
        // Reload configuration when returning to this activity
        // This ensures changes made in ConfigActivity are reflected
        viewModel.reloadConfig()
    }
    
    // ========================================
    // Initialization Methods
    // ========================================
    
    private fun initializeViewModel() {
        val repository = (application as TournamentApplication).repository
        val factory = TournamentViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MatchEntryViewModel::class.java]
    }
    
    private fun initializeHelpers() {
        dialogManager = DialogManager(this)
        matchSelectionHelper = MatchSelectionHelper(this)
        scrollHelper = ScrollHelper()
        matchPreferences = MatchEntryPreferences(this)
        // Validator will be initialized once we have config loaded
    }
    
    private fun setupRecyclerView() {
        adapter = TeamEntryAdapter(totalTeams) { _, _, _ ->
            hasUnsavedChanges = true
        }
        
        binding.recyclerViewTeams.apply {
            layoutManager = LinearLayoutManager(this@MatchEntryActivity)
            adapter = this@MatchEntryActivity.adapter
        }
    }
    
    private fun setupUI() {
        setupMatchSelectionListener()
        setupButtonListeners()
    }
    
    // ========================================
    // UI Setup Methods
    // ========================================
    
    private fun setupMatchSelectionListener() {
        // Match selection is now handled by individual box click listeners
    }
    
    private fun setupButtonListeners() {
        binding.btnReset.setOnClickListener { handleResetClick() }
        binding.btnSave.setOnClickListener { handleSaveClick() }
        binding.btnMenu.setOnClickListener { showMenuPopup() }
    }
    
    private fun showMenuPopup() {
        val popup = android.widget.PopupMenu(this, binding.btnMenu)
        popup.menuInflater.inflate(R.menu.menu_match_entry, popup.menu)
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_manual_score -> {
                    handleManualScoreClick()
                    true
                }
                R.id.action_penalty -> {
                    handlePenaltyClick()
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }
    
    // ========================================
    // ViewModel Observers
    // ========================================
    
    private fun observeViewModel() {
        observeConfig()
        observeExistingResults()
        observeResetCompletion()
    }
    private fun observeConfig() {
        viewModel.currentConfig.observe(this) { config ->
            if (config != null) {
                totalDays = config.totalDays
                matchesPerDay = config.matchesPerDay
                totalTeams = config.totalTeams
                
                // Initialize validator with loaded config
                validator = MatchDataValidator(totalTeams, totalRanksForCurrentMatch)
                
                // Update adapter's total teams instead of recreating it
                adapter.updateTotalTeams(totalTeams)
                
                setupDaySpinner()
                setupMatchRadioButtons()
                
                // Auto-load last match only on initial load
                if (!isInitialLoadComplete) {
                    isInitialLoadComplete = true
                    autoLoadLastMatch()
                }
            } else {
                Toast.makeText(this, R.string.no_config_warning, Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
    
    /**
     * Auto-load the last day/match that was being edited.
     * Priority:
     * 1. Last saved preference (day/match user was viewing)
     * 2. Last day/match with actual saved data in database
     * 3. Day 1, no match selected
     */
    private fun autoLoadLastMatch() {
        lifecycleScope.launch {
            try {
                var dayToLoad = 1
                var matchToLoad: Int? = null
                
                // Check if we have saved preferences from last session
                if (matchPreferences.hasSavedSelection()) {
                    val prefDay = matchPreferences.getLastSelectedDay()
                    val prefMatch = matchPreferences.getLastSelectedMatch()
                    
                    // Validate that the saved day/match are within bounds
                    if (prefDay in 1..totalDays && prefMatch in 1..matchesPerDay) {
                        // Check if this match actually has data (in temp storage or database)
                        val key = "${prefDay}_${prefMatch}"
                        val hasTempData = unsavedMatchData.containsKey(key)
                        val hasDbData = viewModel.getMatchResultsCount(prefDay, prefMatch) > 0
                        
                        if (hasTempData || hasDbData) {
                            // Only auto-load if match has data
                            dayToLoad = prefDay
                            matchToLoad = prefMatch
                            android.util.Log.d("MatchEntryActivity", "Loading from preferences: Day $dayToLoad, Match $matchToLoad")
                        } else {
                            // Preference points to empty match, find last match with actual data
                            android.util.Log.d("MatchEntryActivity", "Preference has no data, looking for last match with data")
                            val lastDay = viewModel.getLastDayWithData()
                            if (lastDay != null && lastDay in 1..totalDays) {
                                dayToLoad = lastDay
                                val lastMatch = viewModel.getLastMatchWithDataForDay(lastDay)
                                if (lastMatch != null && lastMatch in 1..matchesPerDay) {
                                    matchToLoad = lastMatch
                                    android.util.Log.d("MatchEntryActivity", "Found data at Day $dayToLoad, Match $matchToLoad")
                                }
                            }
                        }
                    }
                } else {
                    // No saved preference, find last day/match with data
                    val lastDay = viewModel.getLastDayWithData()
                    if (lastDay != null && lastDay in 1..totalDays) {
                        dayToLoad = lastDay
                        val lastMatch = viewModel.getLastMatchWithDataForDay(lastDay)
                        if (lastMatch != null && lastMatch in 1..matchesPerDay) {
                            matchToLoad = lastMatch
                            android.util.Log.d("MatchEntryActivity", "Loading from database: Day $dayToLoad, Match $matchToLoad")
                        }
                    }
                }
                
                // Update UI on main thread
                runOnUiThread {
                    // Set day spinner
                    if (dayToLoad != selectedDay) {
                        selectedDay = dayToLoad
                        binding.spinnerDay.setSelection(dayToLoad - 1, false)
                    }
                    
                    // Auto-select match ONLY if we found one with data
                    if (matchToLoad != null && matchToLoad in 1..matchesPerDay) {
                        val matchBox = matchBoxes[matchToLoad]
                        if (matchBox != null) {
                            // Simulate click to load the match
                            updateMatchBoxSelection(matchBox)
                            proceedWithMatchSelection(matchToLoad)
                        }
                    } else {
                        // No match with data found, don't auto-select anything
                        android.util.Log.d("MatchEntryActivity", "No match with data found, leaving selection empty")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MatchEntryActivity", "Error auto-loading last match", e)
            }
        }
    }
    
    private fun observeExistingResults() {
        viewModel.existingResults.observe(this) { results ->
            // Check if we have unsaved temporary data for this match
            val match = selectedMatch ?: return@observe
            val key = "${selectedDay}_${match}"
            val tempData = unsavedMatchData[key]
            
            android.util.Log.d("MatchEntryActivity", "observeExistingResults - Day: $selectedDay, Match: $match, " +
                "TempData: ${tempData != null}, DBResults: ${results.size}, TotalRanks: $totalRanksForCurrentMatch")
            
            if (tempData != null) {
                // Load from temporary storage (preserves unsaved changes)
                val (dataMap, numRanks) = tempData
                android.util.Log.d("MatchEntryActivity", "Loading from temp storage: $numRanks ranks")
                adapter.setExistingData(dataMap, numRanks)
                // Don't reset hasUnsavedChanges here - temp data means unsaved changes
            } else if (results.isNotEmpty()) {
                // Load from database (no unsaved changes)
                val dataMap = results.associate { result ->
                    Pair(result.rank, Pair(result.teamNumber, result.kills))
                }
                // Use the max rank from results if totalRanksForCurrentMatch is not set
                val numRanks = if (totalRanksForCurrentMatch > 0) {
                    totalRanksForCurrentMatch
                } else {
                    results.maxOfOrNull { it.rank } ?: 0
                }
                android.util.Log.d("MatchEntryActivity", "Loading from database: ${results.size} results, $numRanks ranks")
                adapter.setExistingData(dataMap, numRanks)
                // Reset unsaved changes since we're loading from database
                hasUnsavedChanges = false
            } else {
                // No data at all - clear if needed
                android.util.Log.d("MatchEntryActivity", "No data found, clearing adapter")
                if (totalRanksForCurrentMatch == 0) {
                    adapter.clearAllData()
                    hasUnsavedChanges = false
                }
            }
        }
    }
    
    private fun observeResetCompletion() {
        viewModel.resetCompleted.observe(this) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(
                    this,
                    getString(R.string.reset_success, message),
                    Toast.LENGTH_SHORT
                ).show()
                
                lifecycleScope.launch {
                    val matchesForCurrentDay = viewModel.getMatchesWithDataForDay(selectedDay)
                    if (matchesForCurrentDay.isEmpty()) {
                        runOnUiThread {
                            clearMatchSelection()
                        }
                    } else {
                        loadExistingResults()
                    }
                }
            }
        }
    }
    
    // ========================================
    // Match Selection Logic
    // ========================================
    
    private fun handleMatchSelection(matchNumber: Int, clickedBox: TextView) {
        try {
            // If clicking the same match, do nothing
            if (selectedMatch == matchNumber) {
                return
            }
            
            // No warning when switching between matches - just save to temp storage
            updateMatchBoxSelection(clickedBox)
            proceedWithMatchSelection(matchNumber)
        } catch (e: Exception) {
            android.util.Log.e("MatchEntryActivity", "Error handling match selection", e)
            Toast.makeText(this, getString(R.string.error_selecting_match, e.message), Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateMatchBoxSelection(newSelectedBox: TextView) {
        // Deselect previous box and update its appearance
        selectedMatchBox?.let { prevBox ->
            val prevMatchNum = matchBoxes.entries.find { it.value == prevBox }?.key
            prevMatchNum?.let { updateMatchBoxAppearance(it) }
        }
        
        // Select new box
        newSelectedBox.setBackgroundResource(R.drawable.match_box_selector)
        newSelectedBox.isSelected = true
        selectedMatchBox = newSelectedBox
    }
    
    private fun proceedWithMatchSelection(matchNumber: Int) {
        // Save current match data to temporary storage before switching
        selectedMatch?.let { prevMatch ->
            saveCurrentMatchToTempStorage(prevMatch)
            val isComplete = checkMatchDataComplete()
            matchCompletionStatus[prevMatch] = isComplete
            updateMatchBoxAppearance(prevMatch)
        }
        
        // Validate match number
        val validationResult = validator.validateMatchNumber(matchNumber, matchesPerDay)
        if (validationResult is ValidationResult.Error) {
            Toast.makeText(this, validationResult.message, Toast.LENGTH_LONG).show()
            return
        }
        
        // Save preference for this selection
        matchPreferences.saveLastSelection(selectedDay, matchNumber)
        
        // Check if match has existing data or temporary data
        lifecycleScope.launch {
            val key = "${selectedDay}_${matchNumber}"
            val hasTempData = unsavedMatchData.containsKey(key)
            
            if (hasTempData) {
                // Load from temporary storage - don't show popup
                runOnUiThread {
                    loadMatchFromTempStorage(matchNumber)
                }
            } else {
                val existingResults = viewModel.getMatchResultsCount(selectedDay, matchNumber)
                if (existingResults > 0) {
                    // Load existing match - don't show popup
                    runOnUiThread {
                        loadExistingMatch(matchNumber)
                    }
                } else {
                    // Only show total teams popup for NEW matches
                    runOnUiThread {
                        startNewMatch(matchNumber)
                    }
                }
            }
        }
    }
    
    /**
     * Save current adapter data to temporary storage when switching matches.
     * This preserves unsaved changes when user switches between matches.
     */
    private fun saveCurrentMatchToTempStorage(matchNumber: Int) {
        if (totalRanksForCurrentMatch > 0) {
            val currentData = adapter.getAllTeamData()
            val dataMap = currentData.associate { (rank, teamNumber, kills) ->
                rank to Pair(teamNumber, kills)
            }
            val key = "${selectedDay}_${matchNumber}"
            unsavedMatchData[key] = Pair(dataMap, totalRanksForCurrentMatch)
        }
    }
    
    /**
     * Load match data from temporary storage.
     */
    private fun loadMatchFromTempStorage(matchNumber: Int) {
        val key = "${selectedDay}_${matchNumber}"
        val tempData = unsavedMatchData[key]
        
        if (tempData != null) {
            selectedMatch = matchNumber
            val (dataMap, numRanks) = tempData
            totalRanksForCurrentMatch = numRanks
            
            // Update validator with stored total ranks
            validator = MatchDataValidator(totalTeams, totalRanksForCurrentMatch)
            
            adapter.initializeForMatch(numRanks)
            adapter.setExistingData(dataMap, numRanks)
            binding.cardTeamEntry.visibility = View.VISIBLE
            
            // Reset unsaved changes flag since we're loading existing data
            hasUnsavedChanges = false
            
            scrollHelper.smoothScrollToView(binding.scrollView, binding.cardTeamEntry)
        }
    }
    
    /**
     * Clear all temporary data for a specific day.
     */
    private fun clearTempDataForDay(day: Int) {
        unsavedMatchData.keys.removeAll { it.startsWith("${day}_") }
    }
    
    private fun checkMatchDataComplete(): Boolean {
        // Check if all required fields are filled
        val teamData = adapter.getAllTeamData()
        if (teamData.isEmpty()) return false
        
        // Check if at least one team has valid data
        val hasValidData = teamData.any { (_, teamNumber, kills) ->
            teamNumber > 0 && kills >= 0
        }
        
        // Check if all participating teams have valid ranks
        val participatingTeams = teamData.filter { (_, teamNumber, kills) ->
            teamNumber > 0 && kills >= 0
        }
        
        return hasValidData && participatingTeams.size == totalRanksForCurrentMatch
    }
    
    private fun updateMatchBoxAppearance(matchNumber: Int) {
        try {
            val matchBox = matchBoxes[matchNumber]
            if (matchBox == null) {
                android.util.Log.w("MatchEntryActivity", "Match box not found for match $matchNumber")
                return
            }
            
            val isComplete = matchCompletionStatus[matchNumber] ?: false
            
            if (matchBox == selectedMatchBox) {
                // Keep selected appearance
                matchBox.setBackgroundResource(R.drawable.match_box_selector)
                matchBox.isSelected = true
            } else {
                // Update based on completion status
                when {
                    isComplete -> matchBox.setBackgroundResource(R.drawable.match_box_complete)
                    matchCompletionStatus.containsKey(matchNumber) && !isComplete -> 
                        matchBox.setBackgroundResource(R.drawable.match_box_incomplete)
                    else -> matchBox.setBackgroundResource(R.drawable.match_box_default)
                }
                matchBox.isSelected = false
            }
        } catch (e: Exception) {
            android.util.Log.e("MatchEntryActivity", "Error updating match box appearance", e)
        }
    }
    
    private fun loadExistingMatch(matchNumber: Int) {
        lifecycleScope.launch {
            android.util.Log.d("MatchEntryActivity", "Loading existing match: Day $selectedDay, Match $matchNumber")
            
            selectedMatch = matchNumber
            val maxRank = viewModel.getMaxRankForMatch(selectedDay, matchNumber)
            totalRanksForCurrentMatch = maxRank
            
            android.util.Log.d("MatchEntryActivity", "Max rank from DB: $maxRank")
            
            // Update validator with new total ranks
            validator = MatchDataValidator(totalTeams, totalRanksForCurrentMatch)
            
            // Initialize adapter with correct number of ranks
            runOnUiThread {
                adapter.initializeForMatch(maxRank)
                binding.cardTeamEntry.visibility = View.VISIBLE
                
                // Load the actual data - this will trigger observeExistingResults
                loadExistingResults()
                
                // Reset unsaved changes flag since we're loading existing data
                hasUnsavedChanges = false
                
                scrollHelper.smoothScrollToView(binding.scrollView, binding.cardTeamEntry)
            }
        }
    }
    
    private fun startNewMatch(matchNumber: Int) {
        dialogManager.showTotalRanksDialog(
            maxTeams = totalTeams,
            onStart = { totalRanks ->
                selectedMatch = matchNumber
                totalRanksForCurrentMatch = totalRanks
                
                // Update validator with new total ranks
                validator = MatchDataValidator(totalTeams, totalRanksForCurrentMatch)
                
                adapter.initializeForMatch(totalRanks)
                binding.cardTeamEntry.visibility = View.VISIBLE
                loadExistingResults()
                
                scrollHelper.smoothScrollToView(binding.scrollView, binding.cardTeamEntry)
                
                Toast.makeText(
                    this,
                    MatchEntryConstants.MSG_ENTER_TEAM_COUNT.format(totalRanks),
                    Toast.LENGTH_SHORT
                ).show()
            },
            onCancel = {
                clearMatchSelection()
                selectedMatchBox?.isSelected = false
                selectedMatchBox = null
            }
        )
    }
    
    private fun clearMatchSelection() {
        selectedMatch = null
        selectedMatchBox?.isSelected = false
        selectedMatchBox = null
        binding.cardTeamEntry.visibility = View.GONE
        adapter.clearAllData()
    }
    
    // ========================================
    // Button Click Handlers
    // ========================================
    
    private fun handleResetClick() {
        dialogManager.showResetOptionsDialog(
            onResetCurrentDay = { handleResetCurrentDay() },
            onResetDayRange = { handleResetDayRange() },
            onResetAllData = { handleResetAllData() }
        )
    }
    
    private fun handlePenaltyClick() {
        dialogManager.showPenaltyDialog(
            currentDay = selectedDay,
            currentMatch = selectedMatch,
            totalDays = totalDays,
            matchesPerDay = matchesPerDay,
            totalTeams = totalTeams,
            onApplyPenalty = { penalty ->
                applyPenalty(penalty)
            }
        )
    }
    
    private fun handleManualScoreClick() {
        dialogManager.showManualScoreDialog(
            currentDay = selectedDay,
            currentMatch = selectedMatch,
            totalDays = totalDays,
            matchesPerDay = matchesPerDay,
            totalTeams = totalTeams,
            onApplyScore = { day, match, teamNumber, newScore ->
                applyManualScore(day, match, teamNumber, newScore)
            }
        )
    }
    
    private fun handleSaveClick() {
        saveMatchData()
    }
    
    // ========================================
    // Data Operations
    // ========================================
    
    private fun loadExistingResults() {
        val match = selectedMatch ?: return
        viewModel.loadExistingResults(selectedDay, match)
        hasUnsavedChanges = false
    }
    
    private fun saveMatchData() {
        try {
            // First, save current match to temporary storage
            selectedMatch?.let { match ->
                saveCurrentMatchToTempStorage(match)
            }
            
            // Now save ALL unsaved matches from temporary storage
            if (unsavedMatchData.isEmpty()) {
                Toast.makeText(this, getString(R.string.no_unsaved_data), Toast.LENGTH_SHORT).show()
                return
            }
            
            android.util.Log.d("MatchEntryActivity", "=== SAVE ALL MATCHES START ===")
            android.util.Log.d("MatchEntryActivity", "Total unsaved matches: ${unsavedMatchData.size}")
            
            lifecycleScope.launch {
                val config = viewModel.currentConfig.value
                if (config == null) {
                    Toast.makeText(this@MatchEntryActivity, getString(R.string.configuration_not_loaded), Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                var savedCount = 0
                var errorCount = 0
                
                // Iterate through all unsaved matches
                unsavedMatchData.forEach { (key, tempData) ->
                    try {
                        val (dataMap, numRanks) = tempData
                        val parts = key.split("_")
                        val day = parts[0].toInt()
                        val matchNum = parts[1].toInt()
                        
                        // Convert dataMap to list of triples
                        val teamData = dataMap.map { (rank, pair) ->
                            Triple(rank, pair.first, pair.second)
                        }
                        
                        android.util.Log.d("MatchEntryActivity", "Saving Day $day, Match $matchNum: ${teamData.size} entries")
                        
                        // Validate data
                        val validator = MatchDataValidator(totalTeams, numRanks)
                        val validationResult = validator.validateMatchData(teamData)
                        
                        if (validationResult is ValidationResult.Success) {
                            // Filter out empty entries
                            val participatingTeams = teamData.filter { (_, teamNumber, kills) ->
                                teamNumber > 0 && kills >= 0
                            }
                            
                            if (participatingTeams.isNotEmpty()) {
                                viewModel.saveMatchResults(day, matchNum, participatingTeams)
                                savedCount++
                                
                                // Mark as complete
                                matchCompletionStatus[matchNum] = true
                                
                                android.util.Log.d("MatchEntryActivity", "✓ Saved Day $day, Match $matchNum with ${participatingTeams.size} teams")
                            }
                        } else if (validationResult is ValidationResult.Error) {
                            android.util.Log.e("MatchEntryActivity", "✗ Validation failed for Day $day, Match $matchNum: ${validationResult.message}")
                            errorCount++
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MatchEntryActivity", "Error saving match $key", e)
                        errorCount++
                    }
                }
                
                // Clear temporary storage after successful save
                unsavedMatchData.clear()
                hasUnsavedChanges = false
                
                // Update match box appearances
                runOnUiThread {
                    matchBoxes.keys.forEach { matchNum ->
                        updateMatchBoxAppearance(matchNum)
                    }
                    
                    val message = when {
                        errorCount > 0 -> "Saved $savedCount match(es), $errorCount failed"
                        savedCount > 0 -> "✅ Successfully saved $savedCount match(es)"
                        else -> "No valid data to save"
                    }
                    
                    Toast.makeText(this@MatchEntryActivity, message, Toast.LENGTH_LONG).show()
                    android.util.Log.d("MatchEntryActivity", "=== SAVE ALL MATCHES COMPLETE: $message ===")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MatchEntryActivity", "Error saving match data", e)
            Toast.makeText(this, getString(R.string.error_saving_data, e.message), Toast.LENGTH_LONG).show()
        }
    }
    
    // ========================================
    // Reset Operations
    // ========================================
    
    private fun handleResetCurrentDay() {
        lifecycleScope.launch {
            val matchesWithData = viewModel.getMatchesWithDataForDay(selectedDay).toMutableList()
            
            if (selectedDay == selectedDay && adapter.hasAnyData()) {
                selectedMatch?.let { match ->
                    if (match !in matchesWithData) {
                        matchesWithData.add(match)
                    }
                }
            }
            
            runOnUiThread {
                dialogManager.showMatchSelectionDialog(
                    day = selectedDay,
                    matchesWithData = matchesWithData,
                    onReset = { matchesToReset ->
                        confirmAndResetMatches(selectedDay, matchesToReset)
                    }
                )
            }
        }
    }
    
    private fun handleResetDayRange() {
        lifecycleScope.launch {
            val daysWithData = viewModel.getDaysWithData().toMutableSet()
            
            if (adapter.hasAnyData() && selectedMatch != null && selectedDay !in daysWithData) {
                daysWithData.add(selectedDay)
            }
            
            runOnUiThread {
                dialogManager.showDayRangeDialog(
                    daysWithData = daysWithData.toList(),
                    onReset = { selectedDays ->
                        confirmAndResetDays(selectedDays)
                    }
                )
            }
        }
    }
    
    private fun handleResetAllData() {
        dialogManager.showConfirmationDialog(
            title = getString(R.string.reset_title),
            message = MatchEntryConstants.MSG_RESET_ALL_CONFIRM,
            onConfirm = {
                viewModel.resetAllTournamentData()
                // Clear all temporary data
                unsavedMatchData.clear()
                // Clear preferences
                matchPreferences.clearAll()
                clearMatchSelection()
            }
        )
    }
    
    private fun confirmAndResetMatches(day: Int, matches: List<Int>) {
        val matchText = matches.joinToString(" and ") { "Match $it" }
        dialogManager.showConfirmationDialog(
            title = getString(R.string.reset_title),
            message = MatchEntryConstants.MSG_RESET_DAY_MATCHES_CONFIRM.format(matchText, day),
            onConfirm = {
                matches.forEach { matchNum ->
                    viewModel.resetCurrentMatch(day, matchNum)
                    // Clear temporary data for reset matches
                    val key = "${day}_${matchNum}"
                    unsavedMatchData.remove(key)
                    
                    // Clear match completion status
                    matchCompletionStatus.remove(matchNum)
                    
                    // Update match box appearance
                    if (day == selectedDay) {
                        updateMatchBoxAppearance(matchNum)
                    }
                }
                // Clear selection if current match was reset
                if (selectedMatch in matches && day == selectedDay) {
                    clearMatchSelection()
                }
            }
        )
    }
    
    private fun confirmAndResetDays(days: List<Int>) {
        val daysList = days.sorted().joinToString(", ")
        dialogManager.showConfirmationDialog(
            title = getString(R.string.reset_title),
            message = MatchEntryConstants.MSG_RESET_DAY_RANGE_CONFIRM.format(daysList),
            onConfirm = {
                days.forEach { day ->
                    viewModel.resetAllMatchesForDay(day)
                    // Clear temporary data for reset days
                    clearTempDataForDay(day)
                }
                // Clear selection if current day was reset
                if (selectedDay in days) {
                    clearMatchSelection()
                }
            }
        )
    }
    
    // ========================================
    // Penalty Operations
    // ========================================
    
    private fun applyPenalty(penalty: Penalty) {
        lifecycleScope.launch {
            val matchResults = viewModel.getMatchResultsSync(penalty.day, penalty.matchNumber)
            val teamParticipated = matchResults.any { it.teamNumber == penalty.teamNumber }
            
            if (!teamParticipated) {
                runOnUiThread {
                    Toast.makeText(
                        this@MatchEntryActivity,
                        MatchEntryConstants.MSG_TEAM_NOT_PARTICIPATED.format(
                            penalty.teamNumber,
                            penalty.day,
                            penalty.matchNumber
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
                return@launch
            }
            
            viewModel.savePenalty(penalty)
            runOnUiThread {
                Toast.makeText(
                    this@MatchEntryActivity,
                    MatchEntryConstants.MSG_PENALTY_SUCCESS.format(
                        penalty.penaltyPoints,
                        penalty.teamNumber,
                        penalty.day,
                        penalty.matchNumber
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun applyManualScore(day: Int, match: Int, teamNumber: Int, newScore: Int) {
        lifecycleScope.launch {
            val matchResults = viewModel.getMatchResultsSync(day, match)
            val teamParticipated = matchResults.any { it.teamNumber == teamNumber }
            
            if (!teamParticipated) {
                runOnUiThread {
                    Toast.makeText(
                        this@MatchEntryActivity,
                        "Team #$teamNumber did not participate in Day $day, Match $match. Please save match data first.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return@launch
            }
            
            viewModel.updateManualScore(day, match, teamNumber, newScore)
            
            // Reload results if this is the currently selected match
            if (day == selectedDay && match == selectedMatch) {
                loadExistingResults()
            }
            
            runOnUiThread {
                Toast.makeText(
                    this@MatchEntryActivity,
                    "✅ Updated Team #$teamNumber score to $newScore points for Day $day, Match $match",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    // ========================================
    // Day Spinner Setup
    // ========================================
    
    private fun setupDaySpinner() {
        val days = (1..totalDays).map { "Day $it" }
        val spinnerAdapter = ArrayAdapter(
            this,
            R.layout.spinner_item_day_filter,
            days
        )
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_day_filter)
        binding.spinnerDay.adapter = spinnerAdapter
        
        binding.spinnerDay.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newDay = position + 1
                
                // Check if we're changing days and have unsaved changes
                if (newDay != selectedDay && hasUnsavedChanges) {
                    // Show warning and revert spinner selection
                    showDayChangeWarning(newDay)
                    // Revert to current day (selectedDay is 1-based, position is 0-based)
                    binding.spinnerDay.setSelection(selectedDay - 1)
                } else if (newDay != selectedDay) {
                    // Save current match to temp storage before changing days
                    selectedMatch?.let { match ->
                        saveCurrentMatchToTempStorage(match)
                    }
                    
                    selectedDay = newDay
                    
                    // Clear current selection when changing days
                    clearMatchSelection()
                    
                    // Don't auto-load any match when user changes day
                    // Let them manually select which match they want to work on
                    android.util.Log.d("MatchEntryActivity", "Day changed to $selectedDay, cleared selection")
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun showDayChangeWarning(newDay: Int) {
        dialogManager.showConfirmationDialog(
            title = "Unsaved Changes",
            message = "You have unsaved changes for Day $selectedDay. Save before switching to Day $newDay?",
            onConfirm = {
                // Save current data
                saveMatchData()
                // Now switch to new day
                selectedDay = newDay
                binding.spinnerDay.setSelection(newDay - 1)
                loadExistingResults()
            }
        )
    }
    
    // ========================================
    // Match Radio Buttons Setup
    // ========================================
    
    private fun setupMatchRadioButtons() {
        binding.matchButtonContainer.removeAllViews()
        matchBoxes.clear()
        matchCompletionStatus.clear()
        
        if (matchesPerDay <= 0) {
            binding.tvSelectMatch.visibility = View.GONE
            binding.matchButtonContainer.visibility = View.GONE
            binding.cardTeamEntry.visibility = View.GONE
            Toast.makeText(
                this,
                "⚠️ Please configure 'Matches Per Day' in tournament settings",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        
        binding.tvSelectMatch.visibility = View.VISIBLE
        binding.matchButtonContainer.visibility = View.VISIBLE
        
        // Create styled match boxes in grid (2 columns)
        for (i in 1..matchesPerDay) {
            val matchBox = TextView(this).apply {
                text = "Match $i"
                textSize = 16f
                setTextColor(getColor(R.color.text_primary))
                gravity = android.view.Gravity.CENTER
                setPadding(24, 16, 24, 16)
                setBackgroundResource(R.drawable.match_box_default)
                minHeight = 80
                
                isClickable = true
                isFocusable = true
                
                setOnClickListener {
                    // Handle match selection (which includes warning check)
                    handleMatchSelection(i, this)
                }
            }
            
            // Add to GridLayout - let it auto-arrange in 2 columns
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(6, 6, 6, 6)
            }
            matchBox.layoutParams = params
            
            matchBoxes[i] = matchBox
            binding.matchButtonContainer.addView(matchBox)
        }
        
        binding.cardTeamEntry.visibility = View.GONE
    }
    
    // ========================================
    // Back Press Handling
    // ========================================
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Only check for unsaved changes if user has modified data in current session
        // hasUnsavedChanges tracks changes made in the current match
        if (hasUnsavedChanges) {
            // User has made changes in current match
            // Create custom dialog with Save/Don't Save/Cancel options
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.unsaved_changes))
                .setMessage(getString(R.string.unsaved_changes_message))
                .setPositiveButton(getString(R.string.save_and_exit)) { _, _ ->
                    // Save all data
                    saveMatchData()
                    // Wait a bit for save to complete, then exit
                    lifecycleScope.launch {
                        kotlinx.coroutines.delay(500)
                        finish()
                    }
                }
                .setNegativeButton(getString(R.string.dont_save)) { _, _ ->
                    // Confirm exit without saving
                    dialogManager.showConfirmationDialog(
                        title = getString(R.string.exit_without_saving),
                        message = getString(R.string.all_unsaved_changes_lost),
                        onConfirm = {
                            hasUnsavedChanges = false
                            unsavedMatchData.clear()
                            finish()
                        }
                    )
                }
                .setNeutralButton(getString(R.string.btn_cancel), null)
                .show()
        } else {
            super.onBackPressed()
        }
    }
}
