package com.lilranker.tournament.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lilranker.tournament.data.repository.TournamentRepository

class TournamentViewModelFactory(
    private val repository: TournamentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConfigViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ConfigViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(MatchEntryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MatchEntryViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(LeaderboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LeaderboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
