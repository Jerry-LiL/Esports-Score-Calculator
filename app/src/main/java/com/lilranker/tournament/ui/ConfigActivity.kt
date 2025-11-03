package com.lilranker.tournament.ui

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.lilranker.tournament.R
import com.lilranker.tournament.TournamentApplication
import com.lilranker.tournament.databinding.ActivityConfigBinding
import com.lilranker.tournament.ui.viewmodel.ConfigViewModel
import com.lilranker.tournament.ui.viewmodel.TournamentViewModelFactory
import kotlinx.coroutines.launch

class ConfigActivity : BaseActivity() {
    
    private lateinit var binding: ActivityConfigBinding
    private lateinit var viewModel: ConfigViewModel
    private val rankPointsMap = mutableMapOf<Int, Int>()
    private var hasUnsavedChanges = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val repository = (application as TournamentApplication).repository
        val factory = TournamentViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ConfigViewModel::class.java]
        
        setupUI()
        loadExistingConfig()
        observeViewModel()
    }
    
    private fun setupUI() {
        // Start with empty rank points - user must add manually
        
        // Add text watchers to track unsaved changes
        binding.etTotalDays.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                hasUnsavedChanges = true
                updateTotalMatchesDisplay()
            }
        })
        
        binding.etMatchesPerDay.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                hasUnsavedChanges = true
                updateTotalMatchesDisplay()
            }
        })
        
        binding.etPointsPerKill.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                hasUnsavedChanges = true
            }
        })
        
        binding.btnAddRank.setOnClickListener {
            // Find the next rank number by checking existing rank entries
            val nextRank = getNextRankNumber()
            
            // Validate: Maximum totalTeams (25) ranks
            if (nextRank > 25) {
                Toast.makeText(this, getString(R.string.maximum_ranks_allowed), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            addRankEntry(nextRank, null) // null means empty field
        }
        
        binding.btnResetConfig.setOnClickListener {
            showResetConfirmationDialog()
        }
        
        binding.btnTeamGrouping.setOnClickListener {
            showTeamGroupingDialog()
        }
        
        binding.btnSaveConfig.setOnClickListener {
            saveConfiguration()
        }
    }
    
    private fun updateTotalMatchesDisplay() {
        try {
            val totalDaysText = binding.etTotalDays.text?.toString()?.trim()
            val matchesPerDayText = binding.etMatchesPerDay.text?.toString()?.trim()
            
            val totalDays = totalDaysText?.toIntOrNull()
            val matchesPerDay = matchesPerDayText?.toIntOrNull()
            
            // Total matches calculation hidden per user request
            binding.tvTotalMatches.visibility = View.GONE
        } catch (e: Exception) {
            android.util.Log.e("ConfigActivity", "Error updating total matches display", e)
            binding.tvTotalMatches.visibility = View.GONE
        }
    }
    
    private fun showResetConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.reset_configuration))
            .setMessage(getString(R.string.reset_config_confirmation))
            .setPositiveButton(getString(R.string.btn_reset)) { _, _ ->
                resetConfiguration()
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }
    
    private fun resetConfiguration() {
        // Clear Total Days
        binding.etTotalDays.setText("")
        
        // Clear Matches Per Day
        binding.etMatchesPerDay.setText("")
        
        // Clear Points Per Kill
        binding.etPointsPerKill.setText("")
        
        // Remove all rank entries
        binding.rankPointsContainer.removeAllViews()
        
        // Clear the map
        rankPointsMap.clear()
        
        // Delete config from database
        viewModel.deleteConfiguration()
        
        Toast.makeText(this, getString(R.string.config_reset_success), Toast.LENGTH_SHORT).show()
        
        // Reset unsaved changes flag
        hasUnsavedChanges = false
    }
    
    private fun getNextRankNumber(): Int {
        try {
            // Check all existing rank entries in the container
            var maxRank = 0
            for (i in 0 until binding.rankPointsContainer.childCount) {
                val itemView = binding.rankPointsContainer.getChildAt(i)
                val etRankPosition = itemView?.findViewById<TextInputEditText>(R.id.etRankPosition)
                val rankText = etRankPosition?.text?.toString()?.trim()
                val rank = rankText?.toIntOrNull() ?: 0
                if (rank > maxRank) {
                    maxRank = rank
                }
            }
            return maxRank + 1
        } catch (e: Exception) {
            android.util.Log.e("ConfigActivity", "Error getting next rank number", e)
            return 1
        }
    }
    
    private fun addRankEntry(rank: Int, points: Int?) {
        try {
            val itemView = LayoutInflater.from(this).inflate(
                R.layout.item_rank_point,
                binding.rankPointsContainer,
                false
            )
            
            val etRankPosition = itemView?.findViewById<TextInputEditText>(R.id.etRankPosition)
            val etRankPoints = itemView?.findViewById<TextInputEditText>(R.id.etRankPoints)
            val btnDeleteRank = itemView?.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDeleteRank)
            
            if (etRankPosition == null || etRankPoints == null || btnDeleteRank == null) {
                android.util.Log.e("ConfigActivity", "Failed to find views in rank point item")
                return
            }
            
            etRankPosition.setText(rank.toString())
            // Only set points if not null, otherwise leave empty
            if (points != null) {
                etRankPoints.setText(points.toString())
            }
            
            // Add text watcher for rank position to prevent duplicates
            etRankPosition.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    try {
                        val newRankText = s?.toString()?.trim()
                        val newRank = newRankText?.toIntOrNull() ?: return
                        
                        // Check if this rank already exists in another entry
                        val childCount = binding.rankPointsContainer.childCount
                        for (i in 0 until childCount) {
                            val otherView = binding.rankPointsContainer.getChildAt(i)
                            if (otherView == itemView) continue // Skip self
                            
                            val otherRankField = otherView?.findViewById<TextInputEditText>(R.id.etRankPosition)
                            val otherRankText = otherRankField?.text?.toString()?.trim()
                            val otherRank = otherRankText?.toIntOrNull()
                            
                            if (otherRank == newRank) {
                                // Duplicate found! Show error and clear the field
                                etRankPosition.error = getString(R.string.rank_already_configured, newRank)
                                Toast.makeText(this@ConfigActivity, getString(R.string.rank_already_configured, newRank), Toast.LENGTH_SHORT).show()
                                etRankPosition.setText("")
                                return
                            }
                        }
                        
                        etRankPosition.error = null
                        hasUnsavedChanges = true
                    } catch (e: Exception) {
                        android.util.Log.e("ConfigActivity", "Error in rank position watcher", e)
                    }
                }
            })
            
            // Add text watcher for rank points to mark unsaved changes
            etRankPoints.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    hasUnsavedChanges = true
                }
            })
            
            // Handle delete button click
            btnDeleteRank.setOnClickListener {
                binding.rankPointsContainer.removeView(itemView)
                hasUnsavedChanges = true
            }
            
            binding.rankPointsContainer.addView(itemView)
            
            // Scroll to the newly added rank entry
            binding.scrollView.post {
                binding.scrollView.smoothScrollTo(0, binding.rankPointsContainer.bottom)
            }
        } catch (e: Exception) {
            android.util.Log.e("ConfigActivity", "Error adding rank entry", e)
            Toast.makeText(this, getString(R.string.error_adding_rank, e.message), Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadExistingConfig() {
        viewModel.getCurrentConfig { config ->
            runOnUiThread {
                if (config != null) {
                    android.util.Log.d("ConfigActivity", "Loading existing config - Days: ${config.totalDays}, Matches/Day: ${config.matchesPerDay}, Points/Kill: ${config.pointsPerKill}")
                    
                    binding.etTotalDays.setText(config.totalDays.toString())
                    binding.etMatchesPerDay.setText(config.matchesPerDay.toString())
                    binding.etPointsPerKill.setText(config.pointsPerKill.toString())
                    
                    // Load existing rank points
                    val repository = (application as TournamentApplication).repository
                    val existingRankPoints = repository.parseRankPoints(config.rankPoints)
                    
                    binding.rankPointsContainer.removeAllViews()
                    existingRankPoints.toSortedMap().forEach { (rank, points) ->
                        addRankEntry(rank, points)
                    }
                    
                    // No unsaved changes after loading
                    hasUnsavedChanges = false
                    
                    // Update total matches display
                    updateTotalMatchesDisplay()
                } else {
                    android.util.Log.w("ConfigActivity", "No existing configuration found")
                }
            }
        }
    }
    
    private fun saveConfiguration() {
        try {
            val totalDaysText = binding.etTotalDays.text?.toString()?.trim()
            val matchesPerDayText = binding.etMatchesPerDay.text?.toString()?.trim()
            val pointsPerKillText = binding.etPointsPerKill.text?.toString()?.trim()
            
            if (totalDaysText.isNullOrEmpty() || matchesPerDayText.isNullOrEmpty() || pointsPerKillText.isNullOrEmpty()) {
                Toast.makeText(this, getString(R.string.fill_required_fields), Toast.LENGTH_LONG).show()
                return
            }
            
            val totalDays = totalDaysText.toIntOrNull()
            val matchesPerDay = matchesPerDayText.toIntOrNull()
            val pointsPerKill = pointsPerKillText.toIntOrNull()
            
            if (totalDays == null || totalDays < 1) {
                Toast.makeText(this, getString(R.string.total_days_minimum), Toast.LENGTH_SHORT).show()
                return
            }
            
            if (matchesPerDay == null || matchesPerDay < 1) {
                Toast.makeText(this, getString(R.string.matches_per_day_minimum), Toast.LENGTH_SHORT).show()
                return
            }
            
            if (pointsPerKill == null || pointsPerKill < 0) {
                Toast.makeText(this, getString(R.string.points_per_kill_minimum), Toast.LENGTH_SHORT).show()
                return
            }
            
            if (matchesPerDay > 10) {
                // Warn user about setting too many matches
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(getString(R.string.high_matches_per_day))
                    .setMessage(getString(R.string.high_matches_confirmation, matchesPerDay))
                    .setPositiveButton(getString(R.string.yes_continue)) { _, _ ->
                        proceedWithValidation(totalDays, matchesPerDay, pointsPerKill)
                    }
                    .setNegativeButton(getString(R.string.btn_cancel), null)
                    .show()
                return
            }
            
            proceedWithValidation(totalDays, matchesPerDay, pointsPerKill)
        } catch (e: Exception) {
            android.util.Log.e("ConfigActivity", "Error saving configuration", e)
            Toast.makeText(this, getString(R.string.error_saving_config, e.message), Toast.LENGTH_LONG).show()
        }
    }
    
    private fun proceedWithValidation(totalDays: Int, matchesPerDay: Int, pointsPerKill: Int) {
        // Check if reducing matches per day would orphan data
        lifecycleScope.launch {
            val repository = (application as TournamentApplication).repository
            val currentConfig = repository.getCurrentConfigSync()
            
            if (currentConfig != null && matchesPerDay < currentConfig.matchesPerDay) {
                // User is reducing matches per day - check for orphaned data
                val hasOrphanedData = repository.hasMatchDataBeyondNumber(matchesPerDay)
                
                if (hasOrphanedData) {
                    runOnUiThread {
                        showOrphanedDataWarning(matchesPerDay, currentConfig.matchesPerDay) {
                            // User confirmed - proceed with save
                            proceedWithSave(totalDays, matchesPerDay, pointsPerKill)
                        }
                    }
                    return@launch
                }
            }
            
            // No orphaned data issue - proceed with save
            runOnUiThread {
                proceedWithSave(totalDays, matchesPerDay, pointsPerKill)
            }
        }
    }
    
    private fun showOrphanedDataWarning(newMatchCount: Int, oldMatchCount: Int, onConfirm: () -> Unit) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.data_will_be_hidden))
            .setMessage(getString(R.string.reducing_matches_warning, oldMatchCount, newMatchCount) +
                    "You have existing data for matches beyond Match $newMatchCount. " +
                    "This data will be HIDDEN (not deleted) and won't appear in the leaderboard.\n\n" +
                    "Do you want to continue?")
            .setPositiveButton(getString(R.string.yes_continue)) { _, _ ->
                onConfirm()
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }
    
    private fun proceedWithSave(totalDays: Int, matchesPerDay: Int, pointsPerKill: Int) {
        try {
            // Collect rank points from all entries
            rankPointsMap.clear()
            for (i in 0 until binding.rankPointsContainer.childCount) {
                val itemView = binding.rankPointsContainer.getChildAt(i)
                if (itemView == null) continue
                
                val etRankPosition = itemView.findViewById<TextInputEditText>(R.id.etRankPosition)
                val etRankPoints = itemView.findViewById<TextInputEditText>(R.id.etRankPoints)
                
                val rankText = etRankPosition?.text?.toString()?.trim()
                val pointsText = etRankPoints?.text?.toString()?.trim()
                
                val rank = rankText?.toIntOrNull()
                if (rank == null || rank < 1) {
                    Toast.makeText(this, getString(R.string.invalid_rank_value), Toast.LENGTH_SHORT).show()
                    return
                }
                
                val points = pointsText?.toIntOrNull() ?: 0
                if (points < 0) {
                    Toast.makeText(this, getString(R.string.rank_points_must_be_positive), Toast.LENGTH_SHORT).show()
                    return
                }
                
                rankPointsMap[rank] = points
            }
            
            android.util.Log.d("ConfigActivity", "Saving config - Days: $totalDays, Matches/Day: $matchesPerDay, Points/Kill: $pointsPerKill, Ranks: ${rankPointsMap.size}")
            viewModel.saveConfiguration(totalDays, matchesPerDay, pointsPerKill, rankPointsMap)
            hasUnsavedChanges = false
        } catch (e: Exception) {
            android.util.Log.e("ConfigActivity", "Error in proceedWithSave", e)
            Toast.makeText(this, getString(R.string.error_saving, e.message), Toast.LENGTH_LONG).show()
        }
    }
    
    private fun observeViewModel() {
        // Observe config saved event for user feedback
        viewModel.configSaved.observe(this) { saved ->
            if (saved) {
                Toast.makeText(this, getString(R.string.config_saved_success), Toast.LENGTH_SHORT).show()
            }
        }
        
        // Observe config errors
        viewModel.configError.observe(this) { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(this, getString(R.string.error_message, error), Toast.LENGTH_LONG).show()
            }
        }
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (hasUnsavedChanges) {
            showExitWarningDialog()
        } else {
            super.onBackPressed()
        }
    }
    
    private fun showExitWarningDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_exit_warning, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val btnSaveAndExit = dialogView.findViewById<View>(R.id.btnSaveAndExit)
        val btnExitWithoutSaving = dialogView.findViewById<View>(R.id.btnExitWithoutSaving)
        
        btnSaveAndExit.setOnClickListener {
            saveConfiguration()
            dialog.dismiss()
            finish()
        }
        
        btnExitWithoutSaving.setOnClickListener {
            dialog.dismiss()
            hasUnsavedChanges = false
            finish()
        }
        
        dialog.show()
    }
    
    private fun showTeamGroupingDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_team_grouping, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val etTeamNumbers = dialogView.findViewById<TextInputEditText>(R.id.etTeamNumbers)
        val etMainTeam = dialogView.findViewById<TextInputEditText>(R.id.etMainTeam)
        val recyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewGroups)
        val btnCancel = dialogView.findViewById<View>(R.id.btnCancel)
        val btnAddGroup = dialogView.findViewById<View>(R.id.btnAddGroup)
        
        // Setup RecyclerView for existing groups
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        loadExistingGroups(recyclerView)
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        btnAddGroup.setOnClickListener {
            val teamNumbersText = etTeamNumbers.text.toString().trim()
            val mainTeamText = etMainTeam.text.toString().trim()
            
            // Validation
            if (teamNumbersText.isEmpty()) {
                Toast.makeText(this, getString(R.string.enter_team_numbers), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (mainTeamText.isEmpty()) {
                Toast.makeText(this, getString(R.string.enter_main_team_number), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val mainTeam = mainTeamText.toIntOrNull()
            if (mainTeam == null || mainTeam < 1 || mainTeam > 25) {
                Toast.makeText(this, getString(R.string.main_team_range), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Parse team numbers (e.g., "7, 19" or "7,19")
            val teamNumbers = teamNumbersText.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it in 1..25 && it != mainTeam }
            
            if (teamNumbers.isEmpty()) {
                Toast.makeText(this, getString(R.string.enter_valid_team_numbers), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Check if main team is in the list
            if (!teamNumbers.contains(mainTeam) && teamNumbers.isNotEmpty()) {
                // Add aliases for all teams except main team
                lifecycleScope.launch {
                    try {
                        teamNumbers.forEach { aliasTeam ->
                            val alias = com.lilranker.tournament.data.model.TeamAlias(
                                primaryTeamNumber = mainTeam,
                                aliasTeamNumber = aliasTeam,
                                groupName = "Group $mainTeam"
                            )
                            viewModel.addTeamAlias(alias)
                        }
                        
                        runOnUiThread {
                            Toast.makeText(
                                this@ConfigActivity,
                                getString(R.string.team_group_created, mainTeam, teamNumbers.joinToString(", ")),
                                Toast.LENGTH_LONG
                            ).show()
                            loadExistingGroups(recyclerView)
                            etTeamNumbers.text?.clear()
                            etMainTeam.text?.clear()
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(
                                this@ConfigActivity,
                                getString(R.string.error_creating_group, e.message),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, getString(R.string.main_team_not_in_alias), Toast.LENGTH_SHORT).show()
            }
        }
        
        dialog.show()
    }
    
    private fun loadExistingGroups(recyclerView: androidx.recyclerview.widget.RecyclerView) {
        lifecycleScope.launch {
            try {
                val allAliases = viewModel.getAllTeamAliases()
                
                // Group by primary team
                val groupedAliases = allAliases.groupBy { it.primaryTeamNumber }
                
                runOnUiThread {
                    val adapter = TeamGroupAdapter(groupedAliases) { primaryTeam ->
                        // Delete group
                        lifecycleScope.launch {
                            viewModel.deleteAliasesForPrimaryTeam(primaryTeam)
                            loadExistingGroups(recyclerView)
                            Toast.makeText(
                                this@ConfigActivity,
                                "Group deleted",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    recyclerView.adapter = adapter
                }
            } catch (e: Exception) {
                android.util.Log.e("ConfigActivity", "Error loading groups", e)
            }
        }
    }
    
    // Simple adapter for team groups
    private class TeamGroupAdapter(
        private val groups: Map<Int, List<com.lilranker.tournament.data.model.TeamAlias>>,
        private val onDelete: (Int) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<TeamGroupAdapter.ViewHolder>() {
        
        private val groupList = groups.toList()
        
        class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            val tvGroupInfo: android.widget.TextView = view.findViewById(R.id.tvGroupInfo)
            val btnDelete: com.google.android.material.button.MaterialButton = view.findViewById(R.id.btnDelete)
        }
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_team_group, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val (primaryTeam, aliases) = groupList[position]
            val aliasNumbers = aliases.map { it.aliasTeamNumber }.joinToString(", ")
            holder.tvGroupInfo.text = "Team $primaryTeam ← [$aliasNumbers]"
            holder.btnDelete.setOnClickListener {
                onDelete(primaryTeam)
            }
        }
        
        override fun getItemCount() = groupList.size
    }
}
