package com.lilranker.tournament.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Team Alias/Grouping entity.
 * 
 * Allows multiple teams to be grouped together (e.g., same clan/organization).
 * The system automatically uses the best score from the group for each match.
 * 
 * Example:
 * - primaryTeam = 19, aliasTeam = 7, groupName = "Clan Alpha"
 * - In Match 1: If Team 19 scores higher → use Team 19's score
 * - In Match 2: If Team 7 scores higher → use Team 7's score for Team 19
 * 
 * @param id Auto-generated primary key
 * @param primaryTeamNumber The main team number (displayed in leaderboard)
 * @param aliasTeamNumber The secondary/alias team number
 * @param groupName Optional group name for identification
 */
@Entity(tableName = "team_aliases")
data class TeamAlias(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val primaryTeamNumber: Int,
    val aliasTeamNumber: Int,
    val groupName: String = ""
)
