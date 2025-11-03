package com.lilranker.tournament

import android.app.Application
import com.lilranker.tournament.data.database.TournamentDatabase
import com.lilranker.tournament.data.repository.TournamentRepository
import timber.log.Timber

/**
 * Custom Application class for the tournament management app.
 * 
 * Responsibilities:
 * - Initialize application-wide components (database, repository, logging)
 * - Provide singleton access to shared resources
 * - Configure app-level settings
 */
class TournamentApplication : Application() {
    
    /**
     * Lazily initialized database instance.
     * Thread-safe singleton pattern handled by Room.
     */
    private val database: TournamentDatabase by lazy { 
        TournamentDatabase.getDatabase(this) 
    }
    
    /**
     * Lazily initialized repository instance.
     * Provides unified data access layer to the UI.
     */
    val repository: TournamentRepository by lazy {
        TournamentRepository(
            configDao = database.tournamentConfigDao(),
            matchResultDao = database.matchResultDao(),
            penaltyDao = database.penaltyDao(),
            teamAliasDao = database.teamAliasDao()
        )
    }
    
    override fun onCreate() {
        super.onCreate()
        initializeLogging()
    }
    
    /**
     * Initialize Timber logging framework.
     * Debug tree only planted in debug builds.
     */
    private fun initializeLogging() {
        // Always plant debug tree for now (BuildConfig not available until build)
        Timber.plant(Timber.DebugTree())
        Timber.d("TournamentApplication initialized")
    }
}
