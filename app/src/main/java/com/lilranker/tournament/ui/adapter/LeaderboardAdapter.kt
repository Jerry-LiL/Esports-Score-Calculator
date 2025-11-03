package com.lilranker.tournament.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.lilranker.tournament.data.model.TeamScore
import com.lilranker.tournament.databinding.ItemLeaderboardBinding

/**
 * Adapter for displaying leaderboard team scores.
 * Uses DiffUtil for efficient list updates.
 */
class LeaderboardAdapter : RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder>() {
    
    private var teamScores = listOf<TeamScore>()
    
    /**
     * Submit a new list of team scores.
     * Uses DiffUtil to calculate and dispatch minimal updates.
     */
    fun submitList(scores: List<TeamScore>) {
        val diffCallback = TeamScoreDiffCallback(teamScores, scores)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        
        teamScores = scores
        diffResult.dispatchUpdatesTo(this)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        val binding = ItemLeaderboardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LeaderboardViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
        holder.bind(teamScores[position])
    }
    
    override fun getItemCount(): Int = teamScores.size
    
    /**
     * ViewHolder for leaderboard item.
     */
    class LeaderboardViewHolder(
        private val binding: ItemLeaderboardBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(teamScore: TeamScore) {
            binding.apply {
                tvTeamNumber.text = "Team ${teamScore.teamNumber}"
                tvKills.text = teamScore.totalKills.toString()
                tvPoints.text = teamScore.totalPoints.toString()
            }
        }
    }
    
    /**
     * DiffUtil callback for efficient list updates.
     */
    private class TeamScoreDiffCallback(
        private val oldList: List<TeamScore>,
        private val newList: List<TeamScore>
    ) : DiffUtil.Callback() {
        
        override fun getOldListSize(): Int = oldList.size
        
        override fun getNewListSize(): Int = newList.size
        
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].teamNumber == newList[newItemPosition].teamNumber
        }
        
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
