package com.lilranker.tournament.data.repository

import com.lilranker.tournament.data.dao.MatchResultDao
import com.lilranker.tournament.data.dao.PenaltyDao
import com.lilranker.tournament.data.dao.TeamAliasDao
import com.lilranker.tournament.data.dao.TournamentConfigDao
import com.lilranker.tournament.data.model.MatchResult
import com.lilranker.tournament.data.model.Penalty
import com.lilranker.tournament.data.model.TeamAlias
import com.lilranker.tournament.data.model.TournamentConfig
import com.lilranker.tournament.data.model.TeamScore
import com.lilranker.tournament.data.service.RankPointsSerializer
import com.lilranker.tournament.data.service.ScoreCalculator
import com.lilranker.tournament.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Repository for managing tournament data operations.
 * 
 * Responsibilities:
 * - Coordinate data access through DAOs
 * - Handle complex business logic for tournaments
 * - Manage configuration changes and their side effects
 * - Calculate leaderboards with penalties applied
 * - Manage team aliases/grouping
 * 
 * Business Logic Delegation:
 * - Score calculation: [ScoreCalculator]
 * - JSON serialization: [RankPointsSerializer]
 * 
 * @property configDao DAO for tournament configuration
 * @property matchResultDao DAO for match results
 * @property penaltyDao DAO for penalties
 * @property teamAliasDao DAO for team aliases
 */
class TournamentRepository(
    private val configDao: TournamentConfigDao,
    private val matchResultDao: MatchResultDao,
    private val penaltyDao: PenaltyDao,
    private val teamAliasDao: TeamAliasDao
) {
    
    companion object {
        private const val TAG = "TournamentRepository"
    }
    
    // ========================================
    // Configuration Management
    // ========================================
    
    /**
     * Get current tournament configuration as a Flow.
     * Updates automatically when configuration changes.
     */
    fun getCurrentConfig(): Flow<TournamentConfig?> = configDao.getCurrentConfig()
    
    /**
     * Get current tournament configuration synchronously.
     * Use when Flow observation is not needed.
     */
    suspend fun getCurrentConfigSync(): TournamentConfig? = configDao.getCurrentConfigSync()
    
    /**
     * Save tournament configuration.
     * If scoring parameters change, automatically recalculates all match results.
     * 
     * @param config The configuration to save
     * @throws RepositoryException.ConfigurationException if save or recalculation fails
     */
    suspend fun saveConfig(config: TournamentConfig) {
        try {
            val oldConfig = configDao.getCurrentConfigSync()
            
            // Replace configuration (only one config allowed at a time)
            configDao.deleteAllConfigs()
            configDao.insertConfig(config)
            
            Logger.d(TAG, "Configuration saved: $config")
            
            // Recalculate match results if scoring parameters changed
            if (oldConfig != null && hasScoringChanges(oldConfig, config)) {
                Logger.i(TAG, "Scoring parameters changed, recalculating all match results")
                recalculateAllMatchResults(config)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to save configuration", e)
            throw RepositoryException.ConfigurationException(
                "Failed to save configuration: ${e.message}",
                e
            )
        }
    }
    
    /**
     * Delete tournament configuration.
     * Does not affect match results or penalties.
     */
    suspend fun deleteConfiguration() {
        configDao.deleteAllConfigs()
    }
    
    /**
     * Check if scoring parameters changed between configurations.
     */
    private fun hasScoringChanges(oldConfig: TournamentConfig, newConfig: TournamentConfig): Boolean {
        return oldConfig.pointsPerKill != newConfig.pointsPerKill ||
               oldConfig.rankPoints != newConfig.rankPoints
    }
    
    /**
     * Recalculate points for all existing match results based on new configuration.
     * Updates totalPoints for each result using new scoring parameters.
     * 
     * @param config The new configuration with updated scoring
     * @throws RepositoryException.ConfigurationException if recalculation fails
     */
    private suspend fun recalculateAllMatchResults(config: TournamentConfig) {
        try {
            val allResults = matchResultDao.getAllResultsSync()
            if (allResults.isEmpty()) {
                Logger.d(TAG, "No match results to recalculate")
                return
            }
            
            val rankPoints = RankPointsSerializer.parseRankPoints(config.rankPoints)
            
            val updatedResults = allResults.map { result ->
                val newTotalPoints = ScoreCalculator.calculateTotalPoints(
                    kills = result.kills,
                    rank = result.rank,
                    pointsPerKill = config.pointsPerKill,
                    rankPoints = rankPoints
                )
                result.copy(totalPoints = newTotalPoints)
            }
            
            matchResultDao.insertResults(updatedResults)
            Logger.i(TAG, "Successfully recalculated ${updatedResults.size} match results")
        } catch (e: Exception) {
            Logger.e(TAG, "Error during recalculation", e)
            throw RepositoryException.ConfigurationException(
                "Error during recalculation: ${e.message}",
                e
            )
        }
    }
    
    /**
     * Calculate total points for a match result using ScoreCalculator.
     * 
     * @deprecated Use [ScoreCalculator.calculateTotalPoints] directly
     */
    @Deprecated(
        message = "Use ScoreCalculator.calculateTotalPoints() instead",
        replaceWith = ReplaceWith(
            "ScoreCalculator.calculateTotalPoints(kills, rank, pointsPerKill, rankPoints)",
            "com.lilranker.tournament.data.service.ScoreCalculator"
        )
    )
    private fun calculateTotalPoints(
        kills: Int,
        rank: Int,
        pointsPerKill: Int,
        rankPoints: Map<Int, Int>
    ): Int {
        return ScoreCalculator.calculateTotalPoints(kills, rank, pointsPerKill, rankPoints)
    }
    
    // ========================================
    // Match Results Management
    // ========================================
    
    /**
     * Save multiple match results.
     * Uses REPLACE strategy - overwrites existing results with same primary key.
     * 
     * @param results List of match results to save
     * @throws RepositoryException.MatchResultException if save fails
     */
    suspend fun saveMatchResults(results: List<MatchResult>) {
        try {
            Logger.d(TAG, "Saving ${results.size} match results")
            results.forEach { result ->
                Logger.v(TAG, 
                    "Result: Day=${result.day}, Match=${result.matchNumber}, " +
                    "Team=${result.teamNumber}, Kills=${result.kills}, " +
                    "Rank=${result.rank}, Points=${result.totalPoints}"
                )
            }
            
            matchResultDao.insertResults(results)
            
            // Apply team alias score swapping after saving
            if (results.isNotEmpty()) {
                val day = results.first().day
                val matchNumber = results.first().matchNumber
                applyTeamAliasScoreSwapping(day, matchNumber)
            }
            
            Logger.i(TAG, "Successfully saved ${results.size} match results")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to save match results", e)
            throw RepositoryException.MatchResultException(
                "Failed to save match results: ${e.message}",
                e
            )
        }
    }
    
    /**
     * Apply score swapping for team aliases in a specific match.
     * The main team always gets the highest score from its group.
     * Other teams get the remaining scores in descending order.
     * 
     * Example: If Team 19 (main) has aliases [7, 12]
     * Match scores: Team 7=15, Team 12=20, Team 19=10
     * After swap: Team 19=20, Team 12=15, Team 7=10
     * 
     * @param day Day number
     * @param matchNumber Match number
     */
    private suspend fun applyTeamAliasScoreSwapping(day: Int, matchNumber: Int) {
        try {
            // Get all team aliases
            val allAliases = teamAliasDao.getAllAliases()
            if (allAliases.isEmpty()) {
                Logger.d(TAG, "No team aliases configured, skipping score swapping")
                return
            }
            
            // Group by primary team
            val groupedAliases = allAliases.groupBy { it.primaryTeamNumber }
            Logger.d(TAG, "Processing ${groupedAliases.size} team groups for score swapping")
            
            // Get all results for this match
            val matchResults = matchResultDao.getMatchResultsSync(day, matchNumber)
            
            // Process each group
            groupedAliases.forEach { (primaryTeam, aliases) ->
                processTeamGroupScoreSwapping(
                    primaryTeam = primaryTeam,
                    aliases = aliases,
                    matchResults = matchResults,
                    day = day,
                    matchNumber = matchNumber
                )
            }
            
            Logger.i(TAG, "Score swapping completed for day $day, match $matchNumber")
        } catch (e: Exception) {
            Logger.e(TAG, "Error applying team alias score swapping", e)
            // Don't throw - score swapping failure shouldn't block match saving
        }
    }
    
    /**
     * Process score swapping for a single team group.
     */
    private suspend fun processTeamGroupScoreSwapping(
        primaryTeam: Int,
        aliases: List<TeamAlias>,
        matchResults: List<MatchResult>,
        day: Int,
        matchNumber: Int
    ) {
        // Get all team numbers in this group (primary + aliases)
        val groupTeamNumbers = buildSet {
            add(primaryTeam)
            addAll(aliases.map { it.aliasTeamNumber })
        }
        
        // Get results for teams in this group
        val groupResults = matchResults.filter { it.teamNumber in groupTeamNumbers }
        
        if (groupResults.size <= 1) {
            Logger.d(TAG, "Group $primaryTeam has only ${groupResults.size} results, skipping")
            return
        }
        
        // Sort by total points descending (best score first)
        val sortedResults = groupResults.sortedByDescending { it.totalPoints }
        
        // Create mapping of team to new score
        val teamScoreMap = mutableMapOf<Int, MatchResult>()
        
        // Main team gets the best score
        teamScoreMap[primaryTeam] = sortedResults.first()
        
        // Distribute remaining scores to alias teams
        val aliasTeams = groupTeamNumbers.filter { it != primaryTeam }.toList()
        val remainingScores = sortedResults.drop(1)
        
        aliasTeams.forEachIndexed { index, aliasTeam ->
            if (index < remainingScores.size) {
                teamScoreMap[aliasTeam] = remainingScores[index]
            }
        }
        
        // Update database with swapped scores
        groupResults.forEach { originalResult ->
            val newScoreData = teamScoreMap[originalResult.teamNumber]
            if (newScoreData != null && newScoreData.teamNumber != originalResult.teamNumber) {
                // Swap: keep original team number but use new score data
                val swappedResult = originalResult.copy(
                    kills = newScoreData.kills,
                    rank = newScoreData.rank,
                    totalPoints = newScoreData.totalPoints
                )
                matchResultDao.insertResult(swappedResult)
                Logger.v(TAG, 
                    "Swapped: Team ${originalResult.teamNumber} now has " +
                    "score from Team ${newScoreData.teamNumber}"
                )
            }
        }
    }
    
    /**
     * Save a single match result.
     */
    suspend fun saveMatchResult(result: MatchResult) {
        matchResultDao.insertResult(result)
    }
    
    /**
     * Get match results for a specific day and match as a Flow.
     * Results are automatically sorted by team number.
     */
    fun getMatchResults(day: Int, matchNumber: Int): Flow<List<MatchResult>> =
        matchResultDao.getMatchResults(day, matchNumber)
    
    /**
     * Get match results synchronously for a specific day and match.
     */
    suspend fun getMatchResultsSync(day: Int, matchNumber: Int): List<MatchResult> =
        matchResultDao.getMatchResultsSync(day, matchNumber)
    
    /**
     * Get all match results as a Flow.
     */
    fun getAllResults(): Flow<List<MatchResult>> = matchResultDao.getAllResults()
    
    /**
     * Get all match results synchronously.
     */
    suspend fun getAllMatchResultsSync(): List<MatchResult> =
        matchResultDao.getAllResultsSync()
    
    /**
     * Delete all results for a specific match.
     */
    suspend fun deleteMatchResults(day: Int, matchNumber: Int) {
        matchResultDao.deleteMatchResults(day, matchNumber)
    }
    
    /**
     * Delete all results for a specific day.
     */
    suspend fun deleteMatchResultsByDay(day: Int) {
        matchResultDao.deleteMatchResultsByDay(day)
    }
    
    /**
     * Delete results for a range of days (inclusive).
     */
    suspend fun deleteMatchResultsByDayRange(startDay: Int, endDay: Int) {
        matchResultDao.deleteMatchResultsByDayRange(startDay, endDay)
    }
    
    /**
     * Delete all match results.
     */
    suspend fun deleteAllResults() {
        matchResultDao.deleteAllResults()
    }
    
    /**
     * Get list of days that have data entered.
     * Only includes days with actual kill or rank data.
     */
    suspend fun getDaysWithData(): List<Int> {
        return matchResultDao.getDaysWithData()
    }
    
    /**
     * Get list of match numbers that have data for a specific day.
     */
    suspend fun getMatchesWithDataForDay(day: Int): List<Int> {
        return matchResultDao.getMatchesWithDataForDay(day)
    }
    
    /**
     * Check if match data exists beyond a certain match number.
     * Used for validation when reducing matches per day configuration.
     */
    suspend fun hasMatchDataBeyondNumber(matchNumber: Int): Boolean {
        return matchResultDao.hasMatchDataBeyondNumber(matchNumber)
    }
    
    /**
     * Check if any match data exists at all.
     * Used to determine if View Leaderboard should be enabled.
     */
    suspend fun hasAnyMatchData(): Boolean {
        return matchResultDao.hasAnyMatchData()
    }
    
    /**
     * Get the last day that has match data.
     * Returns null if no data exists.
     */
    suspend fun getLastDayWithData(): Int? {
        return matchResultDao.getLastDayWithData()
    }
    
    /**
     * Get the last match number for a specific day that has data.
     * Returns null if no data exists for that day.
     */
    suspend fun getLastMatchWithDataForDay(day: Int): Int? {
        return matchResultDao.getLastMatchWithDataForDay(day)
    }
    
    // ========================================
    // Leaderboard Calculation
    // ========================================
    
    /**
     * Get overall leaderboard with penalties applied.
     * Combines match results and penalties from all days.
     * Teams are sorted by total points (DESC) and total kills (DESC).
     */
    /**
     * Get overall leaderboard with penalties applied.
     * All teams are shown with their (potentially swapped) scores.
     */
    fun getLeaderboard(): Flow<List<TeamScore>> {
        return combine(
            matchResultDao.getLeaderboardRaw(),
            penaltyDao.getAllPenalties()
        ) { leaderboardEntries, penalties ->
            applyPenaltiesToLeaderboard(leaderboardEntries, penalties)
        }
    }
    
    /**
     * Get leaderboard for a specific day with that day's penalties applied.
     * Only includes data from the specified day.
     */
    fun getLeaderboardByDay(day: Int): Flow<List<TeamScore>> {
        return combine(
            matchResultDao.getLeaderboardByDayRaw(day),
            penaltyDao.getAllPenalties()
        ) { leaderboardEntries, penalties ->
            // Filter penalties to only include those from the specified day
            val dayPenalties = penalties.filter { it.day == day }
            applyPenaltiesToLeaderboard(leaderboardEntries, dayPenalties)
        }
    }
    
    /**
     * Apply penalties to leaderboard entries and convert to TeamScore.
     * 
     * @param entries Raw leaderboard entries from database
     * @param penalties List of penalties to apply
     * @return List of TeamScore with penalties deducted
     */
    private fun applyPenaltiesToLeaderboard(
        entries: List<MatchResultDao.LeaderboardEntry>,
        penalties: List<Penalty>
    ): List<TeamScore> {
        return try {
            Logger.d(TAG, "Applying penalties to ${entries.size} leaderboard entries")
            
            // Build penalty map for efficient lookup
            val penaltyMap = penalties
                .groupBy { it.teamNumber }
                .mapValues { (_, teamPenalties) ->
                    teamPenalties.sumOf { it.penaltyPoints }
                }
            
            if (penaltyMap.isNotEmpty()) {
                Logger.d(TAG, "Penalty map: $penaltyMap")
            }
            
            // Apply penalties to each entry
            val result = entries.map { entry ->
                val totalPenalty = penaltyMap[entry.teamNumber] ?: 0
                val finalPoints = (entry.totalPoints - totalPenalty).coerceAtLeast(0)
                
                if (entry.totalPoints > 0 || entry.totalKills > 0) {
                    Logger.v(TAG, 
                        "Team ${entry.teamNumber}: Kills=${entry.totalKills}, " +
                        "Points=${entry.totalPoints}, Penalty=$totalPenalty, " +
                        "Final=$finalPoints, Matches=${entry.matchesPlayed}"
                    )
                }
                
                TeamScore(
                    teamNumber = entry.teamNumber,
                    totalKills = entry.totalKills,
                    totalPoints = finalPoints,
                    matchesPlayed = entry.matchesPlayed
                )
            }
            
            Logger.d(TAG, "Created ${result.size} team scores")
            result
        } catch (e: Exception) {
            Logger.e(TAG, "Error applying penalties to leaderboard", e)
            // Fallback: return entries without penalties
            entries.map { entry ->
                TeamScore(
                    teamNumber = entry.teamNumber,
                    totalKills = entry.totalKills,
                    totalPoints = entry.totalPoints.coerceAtLeast(0),
                    matchesPlayed = entry.matchesPlayed
                )
            }
        }
    }
    
    // ========================================
    // Rank Points Serialization
    // ========================================
    
    /**
     * Parse rank points JSON string to Map.
     * Delegates to [RankPointsSerializer].
     * 
     * @param rankPointsJson JSON string like {"1":10,"2":6,"3":4}
     * @return Map of rank to points, empty map if parsing fails
     */
    fun parseRankPoints(rankPointsJson: String): Map<Int, Int> {
        return RankPointsSerializer.parseRankPoints(rankPointsJson)
    }
    
    /**
     * Create JSON string from rank points Map.
     * Delegates to [RankPointsSerializer].
     * 
     * @param rankPoints Map of rank to points
     * @return JSON string like {"1":10,"2":6,"3":4}
     */
    fun createRankPointsJson(rankPoints: Map<Int, Int>): String {
        return RankPointsSerializer.createRankPointsJson(rankPoints)
    }
    
    // ========================================
    // Penalty Management
    // ========================================
    
    /**
     * Save a penalty for a team.
     * Uses REPLACE strategy - overwrites existing penalty with same primary key.
     */
    suspend fun savePenalty(penalty: Penalty) {
        penaltyDao.insertPenalty(penalty)
    }
    
    /**
     * Get penalties for a specific match as a Flow.
     */
    fun getPenaltiesForMatch(day: Int, matchNumber: Int): Flow<List<Penalty>> =
        penaltyDao.getPenaltiesForMatch(day, matchNumber)
    
    /**
     * Get penalties for a specific match synchronously.
     */
    suspend fun getPenaltiesForMatchSync(day: Int, matchNumber: Int): List<Penalty> =
        penaltyDao.getPenaltiesForMatchSync(day, matchNumber)
    
    /**
     * Get all penalties as a Flow.
     */
    fun getAllPenalties(): Flow<List<Penalty>> = penaltyDao.getAllPenalties()
    
    /**
     * Get all penalties synchronously.
     */
    suspend fun getAllPenaltiesSync(): List<Penalty> =
        penaltyDao.getAllPenaltiesSync()
    
    /**
     * Delete a specific penalty for a team in a match.
     */
    suspend fun deletePenalty(day: Int, matchNumber: Int, teamNumber: Int) {
        penaltyDao.deletePenalty(day, matchNumber, teamNumber)
    }
    
    /**
     * Delete all penalties for a specific match.
     */
    suspend fun deletePenaltiesForMatch(day: Int, matchNumber: Int) {
        penaltyDao.deletePenaltiesForMatch(day, matchNumber)
    }
    
    /**
     * Delete all penalties for a specific day.
     */
    suspend fun deletePenaltiesByDay(day: Int) {
        penaltyDao.deletePenaltiesByDay(day)
    }
    
    /**
     * Delete penalties for a range of days (inclusive).
     */
    suspend fun deletePenaltiesByDayRange(startDay: Int, endDay: Int) {
        penaltyDao.deletePenaltiesByDayRange(startDay, endDay)
    }
    
    /**
     * Delete all penalties.
     */
    suspend fun deleteAllPenalties() {
        penaltyDao.deleteAllPenalties()
    }
    
    /**
     * Get total penalty points for a team across all matches.
     */
    suspend fun getTotalPenaltyForTeam(teamNumber: Int): Int {
        return penaltyDao.getTotalPenaltyForTeam(teamNumber) ?: 0
    }
    
    /**
     * Get total penalty points for a team on a specific day.
     */
    suspend fun getTotalPenaltyForTeamOnDay(teamNumber: Int, day: Int): Int {
        return penaltyDao.getTotalPenaltyForTeamOnDay(teamNumber, day) ?: 0
    }
    
    /**
     * Manually update a team's total score for a specific match.
     * Finds the existing match result and updates its totalPoints.
     */
    suspend fun updateTeamScore(day: Int, matchNumber: Int, teamNumber: Int, newScore: Int) {
        val existingResult = matchResultDao.getMatchResultForTeam(day, matchNumber, teamNumber)
        if (existingResult != null) {
            val updatedResult = existingResult.copy(totalPoints = newScore)
            matchResultDao.insertMatchResult(updatedResult)
        }
    }
    
    // ========================================
    // Team Alias/Grouping Operations
    // ========================================
    
    /**
     * Get all team aliases.
     */
    suspend fun getAllTeamAliases(): List<TeamAlias> {
        return teamAliasDao.getAllAliases()
    }
    
    /**
     * Get all alias teams for a primary team.
     */
    suspend fun getAliasesForPrimaryTeam(primaryTeam: Int): List<TeamAlias> {
        return teamAliasDao.getAliasesForPrimaryTeam(primaryTeam)
    }
    
    /**
     * Get primary team for an alias team.
     * Returns null if the team is not an alias.
     */
    suspend fun getPrimaryForAlias(teamNumber: Int): TeamAlias? {
        return teamAliasDao.getPrimaryForAlias(teamNumber)
    }
    
    /**
     * Check if a team is in any group (primary or alias).
     */
    suspend fun isTeamInGroup(teamNumber: Int): Boolean {
        return teamAliasDao.isTeamInGroup(teamNumber) > 0
    }
    
    /**
     * Add a team alias.
     */
    suspend fun addTeamAlias(alias: TeamAlias) {
        teamAliasDao.insertAlias(alias)
    }
    
    /**
     * Delete a team alias.
     */
    suspend fun deleteTeamAlias(alias: TeamAlias) {
        teamAliasDao.deleteAlias(alias)
    }
    
    /**
     * Delete all aliases for a primary team.
     */
    suspend fun deleteAliasesForPrimaryTeam(primaryTeam: Int) {
        teamAliasDao.deleteAliasesForPrimaryTeam(primaryTeam)
    }
    
    /**
     * Delete all team aliases.
     */
    suspend fun deleteAllTeamAliases() {
        teamAliasDao.deleteAllAliases()
    }
}
