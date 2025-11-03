package com.lilranker.tournament.ui.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lilranker.tournament.databinding.ItemTeamEntryBinding

/**
 * Adapter for match entry team data input.
 * 
 * Features:
 * - Dynamic rank entry based on match configuration
 * - Real-time validation of team numbers and kills
 * - Duplicate team number detection
 * - Automatic data change notification
 * - Proper ViewHolder recycling with cleanup
 * 
 * @param maxTeams Maximum allowed team number (from config)
 * @param onDataChanged Callback invoked when data changes (rank, teamNumber, kills)
 */
class TeamEntryAdapter(
    private var maxTeams: Int,
    private val onDataChanged: (Int, Int, Int) -> Unit
) : RecyclerView.Adapter<TeamEntryAdapter.TeamEntryViewHolder>() {
    
    // ========================================
    // Data Management
    // ========================================
    
    /** Map of rank position to (teamNumber, kills) */
    private val rankData = mutableMapOf<Int, Pair<Int, Int>>()
    
    /** Total number of ranks for current match */
    private var totalRanks = 0
    
    /**
     * Update the maximum teams value (when config is loaded).
     * This is called instead of recreating the adapter.
     */
    fun updateTotalTeams(newMaxTeams: Int) {
        maxTeams = newMaxTeams
    }
    
    /**
     * Initialize adapter for a new match with specified number of ranks.
     * Clears existing data and sets up empty entries.
     */
    fun initializeForMatch(numRanks: Int) {
        totalRanks = numRanks
        rankData.clear()
        
        // Initialize all ranks with empty data (0 = not filled)
        for (i in 1..numRanks) {
            rankData[i] = Pair(0, 0)
        }
        
        notifyDataSetChanged()
    }
    
    /**
     * Set existing data for a match being edited.
     * 
     * @param data Map of rank to (teamNumber, kills)
     * @param numRanks Total number of ranks
     */
    fun setExistingData(data: Map<Int, Pair<Int, Int>>, numRanks: Int) {
        totalRanks = numRanks
        rankData.clear()
        
        // Initialize all ranks
        for (i in 1..numRanks) {
            rankData[i] = Pair(0, 0)
        }
        
        // Overwrite with existing data
        rankData.putAll(data)
        notifyDataSetChanged()
    }
    
    /**
     * Clear all data from adapter.
     */
    fun clearAllData() {
        rankData.clear()
        totalRanks = 0
        notifyDataSetChanged()
    }
    
    /**
     * Get all team data as a list of triples.
     * 
     * @return List of (rank, teamNumber, kills) sorted by rank
     */
    fun getAllTeamData(): List<Triple<Int, Int, Int>> {
        return rankData.map { (rank, data) ->
            Triple(rank, data.first, data.second)
        }.sortedBy { it.first }
    }
    
    /**
     * Check if adapter has any non-zero data.
     */
    fun hasAnyData(): Boolean {
        return rankData.values.any { (teamNumber, kills) ->
            teamNumber > 0 || kills > 0
        }
    }
    
    // ========================================
    // RecyclerView Overrides
    // ========================================
    
    /**
     * Find if a team number is already used in a different rank.
     * 
     * @param teamNumber Team number to check
     * @param excludeRank Rank to exclude from check (current rank)
     * @return Rank number if duplicate found, null otherwise
     */
    private fun findDuplicateTeamNumber(teamNumber: Int, excludeRank: Int): Int? {
        if (teamNumber <= 0) return null
        
        return try {
            rankData.entries
                .firstOrNull { (rank, data) ->
                    rank != excludeRank && data.first == teamNumber
                }?.key
        } catch (e: Exception) {
            android.util.Log.e("TeamEntryAdapter", "Error finding duplicate team number", e)
            null
        }
    }
    
    override fun onViewRecycled(holder: TeamEntryViewHolder) {
        super.onViewRecycled(holder)
        holder.cleanup()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamEntryViewHolder {
        val binding = ItemTeamEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TeamEntryViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: TeamEntryViewHolder, position: Int) {
        val rank = position + 1
        holder.bind(rank)
    }
    
    override fun getItemCount(): Int = totalRanks
    
    // ========================================
    // ViewHolder
    // ========================================
    
    inner class TeamEntryViewHolder(
        private val binding: ItemTeamEntryBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private var teamNumberWatcher: TextWatcher? = null
        private var killsWatcher: TextWatcher? = null
        private var currentRank: Int = -1
        
        fun bind(rank: Int) {
            // Clean up previous watchers before binding new data
            cleanup()
            
            currentRank = rank
            
            // Show rank number
            binding.tvRank.text = rank.toString()
            
            // Set values from data with null safety
            val currentData = rankData[rank]
            val teamNumber = currentData?.first ?: 0
            val kills = currentData?.second ?: 0
            
            binding.etTeamNumber.setText(if (teamNumber > 0) teamNumber.toString() else "")
            binding.etKills.setText(if (kills > 0) kills.toString() else "")
            
            // Create and add new watchers
            teamNumberWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    try {
                        // Verify this is still the correct rank (avoid stale callbacks)
                        if (currentRank != rank) return
                        
                        val teamNumberText = s?.toString()?.trim() ?: ""
                        val teamNumber = teamNumberText.toIntOrNull() ?: 0
                        
                        // Validate: team number must be 1-maxTeams (0 means empty/not filled)
                        if (teamNumberText.isNotEmpty() && teamNumber < 1) {
                            binding.etTeamNumber.error = "Team # must be at least 1"
                            binding.etTeamNumber.setText("")
                            return
                        }
                        
                        if (teamNumber > maxTeams) {
                            binding.etTeamNumber.error = "Team # must be 1-$maxTeams"
                            return
                        }
                        
                        // Check for duplicates in real-time
                        if (teamNumber > 0) {
                            val duplicateRank = findDuplicateTeamNumber(teamNumber, rank)
                            if (duplicateRank != null) {
                                binding.etTeamNumber.error = "Team $teamNumber already at Rank $duplicateRank"
                                return
                            }
                        }
                        
                        binding.etTeamNumber.error = null
                        val kills = rankData[rank]?.second ?: 0
                        rankData[rank] = Pair(teamNumber, kills)
                        onDataChanged(rank, teamNumber, kills)
                    } catch (e: Exception) {
                        android.util.Log.e("TeamEntryAdapter", "Error in team number watcher", e)
                    }
                }
            }
            
            killsWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    try {
                        // Verify this is still the correct rank (avoid stale callbacks)
                        if (currentRank != rank) return
                        
                        val killsText = s?.toString()?.trim() ?: ""
                        val kills = killsText.toIntOrNull() ?: 0
                        
                        // Validate: kills cannot be negative
                        if (kills < 0) {
                            binding.etKills.error = "Kills cannot be negative"
                            binding.etKills.setText("")
                            return
                        }
                        
                        binding.etKills.error = null
                        val teamNumber = rankData[rank]?.first ?: 0
                        rankData[rank] = Pair(teamNumber, kills)
                        onDataChanged(rank, teamNumber, kills)
                    } catch (e: Exception) {
                        android.util.Log.e("TeamEntryAdapter", "Error in kills watcher", e)
                    }
                }
            }
            
            binding.etTeamNumber.addTextChangedListener(teamNumberWatcher)
            binding.etKills.addTextChangedListener(killsWatcher)
            
            // Add focus listeners for immediate save when user leaves field
            binding.etTeamNumber.setOnFocusChangeListener { _, hasFocus ->
                // Check if view is still attached and rank matches to avoid recycled view issues
                if (!hasFocus && currentRank == rank && binding.root.isAttachedToWindow) {
                    // Field lost focus - trigger immediate save
                    onDataChanged(rank, rankData[rank]?.first ?: 0, rankData[rank]?.second ?: 0)
                }
            }
            
            binding.etKills.setOnFocusChangeListener { _, hasFocus ->
                // Check if view is still attached and rank matches to avoid recycled view issues
                if (!hasFocus && currentRank == rank && binding.root.isAttachedToWindow) {
                    // Field lost focus - trigger immediate save
                    onDataChanged(rank, rankData[rank]?.first ?: 0, rankData[rank]?.second ?: 0)
                }
            }
        }
        
        fun cleanup() {
            currentRank = -1
            binding.etTeamNumber.setOnFocusChangeListener(null)
            binding.etKills.setOnFocusChangeListener(null)
            teamNumberWatcher?.let {
                binding.etTeamNumber.removeTextChangedListener(it)
                teamNumberWatcher = null
            }
            killsWatcher?.let {
                binding.etKills.removeTextChangedListener(it)
                killsWatcher = null
            }
        }
    }
}
