package com.lilranker.tournament.ui.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lilranker.tournament.R

class RankEntryAdapter(
    private val totalRanks: Int
) : RecyclerView.Adapter<RankEntryAdapter.RankEntryViewHolder>() {

    // Store data: rank -> (teamNumber, kills)
    private val rankData = mutableMapOf<Int, Pair<Int?, Int?>>()

    init {
        // Initialize all ranks with empty data
        for (i in 1..totalRanks) {
            rankData[i] = Pair(null, null)
        }
    }

    fun getAllData(): Map<Int, Pair<Int?, Int?>> {
        return rankData.toMap()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RankEntryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rank_entry_row, parent, false)
        return RankEntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: RankEntryViewHolder, position: Int) {
        val rank = position + 1
        holder.bind(rank)
    }

    override fun getItemCount(): Int = totalRanks
    
    override fun onViewRecycled(holder: RankEntryViewHolder) {
        super.onViewRecycled(holder)
        holder.cleanup()
    }

    inner class RankEntryViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val tvRank: TextView = itemView.findViewById(R.id.tvRank)
        private val etTeamNumber: EditText = itemView.findViewById(R.id.etTeamNumber)
        private val etKills: EditText = itemView.findViewById(R.id.etKills)

        private var teamNumberWatcher: TextWatcher? = null
        private var killsWatcher: TextWatcher? = null
        private var currentRank: Int = -1

        fun bind(rank: Int) {
            // Clean up previous watchers before binding new data
            cleanup()
            
            currentRank = rank
            tvRank.text = rank.toString()

            // Set current data with null safety
            val currentData = rankData[rank]
            etTeamNumber.setText(currentData?.first?.toString() ?: "")
            etKills.setText(currentData?.second?.toString() ?: "")

            // Create new watchers with null safety and rank verification
            teamNumberWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    try {
                        // Verify this is still the correct rank (avoid stale callbacks)
                        if (currentRank != rank) return
                        
                        val teamNumber = s?.toString()?.trim()?.toIntOrNull()
                        val kills = rankData[rank]?.second
                        rankData[rank] = Pair(teamNumber, kills)
                    } catch (e: Exception) {
                        android.util.Log.e("RankEntryAdapter", "Error in team number watcher", e)
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
                        
                        val kills = s?.toString()?.trim()?.toIntOrNull()
                        val teamNumber = rankData[rank]?.first
                        rankData[rank] = Pair(teamNumber, kills)
                    } catch (e: Exception) {
                        android.util.Log.e("RankEntryAdapter", "Error in kills watcher", e)
                    }
                }
            }

            etTeamNumber.addTextChangedListener(teamNumberWatcher)
            etKills.addTextChangedListener(killsWatcher)
        }
        
        fun cleanup() {
            currentRank = -1
            teamNumberWatcher?.let {
                etTeamNumber.removeTextChangedListener(it)
                teamNumberWatcher = null
            }
            killsWatcher?.let {
                etKills.removeTextChangedListener(it)
                killsWatcher = null
            }
        }
    }
}
