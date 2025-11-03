package com.lilranker.tournament.ui.validation

/**
 * Validation result types
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}

/**
 * Centralized validator for match entry data
 * Handles all validation logic for team numbers, ranks, kills, etc.
 */
class MatchDataValidator(
    private val totalTeams: Int,
    private val totalRanksForMatch: Int
) {
    
    /**
     * Validates complete match data before saving
     * Returns ValidationResult with error message if validation fails
     */
    fun validateMatchData(
        teamData: List<Triple<Int, Int, Int>>
    ): ValidationResult {
        try {
            // Filter out empty entries
            val participatingTeams = teamData.filter { (_, teamNumber, kills) ->
                teamNumber > 0 || kills >= 0
            }.filter { (_, teamNumber, kills) ->
                teamNumber > 0 && kills >= 0
            }
            
            // Check if any data exists
            if (participatingTeams.isEmpty()) {
                return ValidationResult.Error("⚠️ No team data entered to save")
            }
            
            // Validate team numbers are within range
            val invalidTeamNumbers = participatingTeams.filter { (_, teamNumber, _) ->
                teamNumber < 1 || teamNumber > totalTeams
            }
            
            if (invalidTeamNumbers.isNotEmpty()) {
                val invalidList = invalidTeamNumbers.map { it.second }.distinct().joinToString(", ")
                return ValidationResult.Error("⚠️ Invalid team numbers: $invalidList (must be 1-$totalTeams)")
            }
            
            // Validate kills are non-negative
            val invalidKills = participatingTeams.filter { (_, _, kills) ->
                kills < 0
            }
            
            if (invalidKills.isNotEmpty()) {
                return ValidationResult.Error("⚠️ Kills cannot be negative")
            }
            
            // Validate team numbers are unique
            val teamNumbers = participatingTeams.map { it.second }
            val duplicateTeams = teamNumbers.groupingBy { it }
                .eachCount()
                .filter { it.value > 1 }
            
            if (duplicateTeams.isNotEmpty()) {
                val duplicateList = duplicateTeams.keys.sorted().joinToString(", ")
                return ValidationResult.Error("⚠️ Duplicate team numbers found: $duplicateList")
            }
            
            // Validate ranks are unique
            val ranks = participatingTeams.map { it.first }
            val duplicateRanks = ranks.groupingBy { it }
                .eachCount()
                .filter { it.value > 1 }
            
            if (duplicateRanks.isNotEmpty()) {
                val duplicateList = duplicateRanks.keys.sorted().joinToString(", ")
                return ValidationResult.Error("⚠️ Duplicate ranks found: $duplicateList")
            }
            
            // Validate ranks are consecutive (no gaps)
            val sortedRanks = ranks.sorted()
            for (i in sortedRanks.indices) {
                if (sortedRanks[i] != i + 1) {
                    return ValidationResult.Error("⚠️ Ranks must be consecutive (1, 2, 3...) with no gaps")
                }
            }
            
            // Allow user to save with fewer teams than total ranks specified
            // This is flexible - they can fill as many or as few as they want
            // Just check that we have at least one participating team
            if (sortedRanks.isEmpty()) {
                return ValidationResult.Error("⚠️ Please enter at least one team")
            }
            
            return ValidationResult.Success
        } catch (e: Exception) {
            android.util.Log.e("MatchDataValidator", "Error validating match data", e)
            return ValidationResult.Error("⚠️ Validation error: ${e.message}")
        }
    }
    
    /**
     * Validates a single team number
     */
    fun validateTeamNumber(teamNumber: Int): ValidationResult {
        return when {
            teamNumber < 1 -> ValidationResult.Error("Team number must be at least 1")
            teamNumber > totalTeams -> ValidationResult.Error("Team number cannot exceed $totalTeams")
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Validates kills count
     */
    fun validateKills(kills: Int): ValidationResult {
        return when {
            kills < 0 -> ValidationResult.Error("Kills cannot be negative")
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Validates total ranks input
     */
    fun validateTotalRanks(totalRanks: Int?): ValidationResult {
        return when {
            totalRanks == null -> ValidationResult.Error("Please enter a valid number")
            totalRanks < 1 -> ValidationResult.Error("Total ranks must be at least 1")
            totalRanks > totalTeams -> ValidationResult.Error("Total ranks cannot exceed $totalTeams teams")
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Validates match number against configured limit
     */
    fun validateMatchNumber(matchNumber: Int, matchesPerDay: Int): ValidationResult {
        return when {
            matchNumber < 1 -> ValidationResult.Error("Match number must be at least 1")
            matchNumber > matchesPerDay -> ValidationResult.Error("⚠️ Match $matchNumber exceeds configured limit of $matchesPerDay matches per day")
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Validates penalty data
     */
    fun validatePenaltyData(
        day: Int?,
        match: Int?,
        teamNumber: Int?,
        penaltyPoints: Int?,
        totalDays: Int,
        matchesPerDay: Int
    ): ValidationResult {
        return when {
            day == null || day < 1 || day > totalDays -> 
                ValidationResult.Error("Day number must be between 1 and $totalDays")
            match == null || match < 1 || match > matchesPerDay -> 
                ValidationResult.Error("Match number must be between 1 and $matchesPerDay")
            teamNumber == null || teamNumber < 1 || teamNumber > totalTeams -> 
                ValidationResult.Error("Team number must be between 1 and $totalTeams")
            penaltyPoints == null || penaltyPoints < 1 -> 
                ValidationResult.Error("Penalty points must be at least 1")
            else -> ValidationResult.Success
        }
    }
}
