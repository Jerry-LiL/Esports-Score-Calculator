package com.lilranker.tournament.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lilranker.tournament.data.dao.MatchResultDao
import com.lilranker.tournament.data.dao.PenaltyDao
import com.lilranker.tournament.data.dao.TeamAliasDao
import com.lilranker.tournament.data.dao.TournamentConfigDao
import com.lilranker.tournament.data.model.MatchResult
import com.lilranker.tournament.data.model.Penalty
import com.lilranker.tournament.data.model.TeamAlias
import com.lilranker.tournament.data.model.TournamentConfig
import timber.log.Timber

/**
 * Room database for tournament data persistence.
 * 
 * Database structure:
 * - TournamentConfig: Scoring rules and tournament structure
 * - MatchResult: Team performance in each match
 * - Penalty: Rule violations and point deductions
 * - TeamAlias: Team grouping for score consolidation
 * 
 * Version: 4
 * Migration strategy: Destructive (data is cleared on schema change)
 * 
 * @see TournamentConfigDao
 * @see MatchResultDao
 * @see PenaltyDao
 * @see TeamAliasDao
 */
@Database(
    entities = [
        TournamentConfig::class,
        MatchResult::class,
        Penalty::class,
        TeamAlias::class
    ],
    version = 4,
    exportSchema = false
)
abstract class TournamentDatabase : RoomDatabase() {
    
    /**
     * DAO for tournament configuration operations.
     */
    abstract fun tournamentConfigDao(): TournamentConfigDao
    
    /**
     * DAO for match result operations.
     */
    abstract fun matchResultDao(): MatchResultDao
    
    /**
     * DAO for penalty operations.
     */
    abstract fun penaltyDao(): PenaltyDao
    
    /**
     * DAO for team alias operations.
     */
    abstract fun teamAliasDao(): TeamAliasDao
    
    companion object {
        
        private const val DATABASE_NAME = "tournament_database"
        
        @Volatile
        private var INSTANCE: TournamentDatabase? = null
        
        /**
         * Get database instance using double-checked locking pattern.
         * Thread-safe singleton that creates database only once.
         * 
         * @param context Application context (not activity context to avoid leaks)
         * @return Singleton database instance
         */
        fun getDatabase(context: Context): TournamentDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { 
                    INSTANCE = it
                    Timber.d("Tournament database instance created")
                }
            }
        }
        
        /**
         * Build the Room database instance with configuration.
         */
        private fun buildDatabase(context: Context): TournamentDatabase {
            return Room.databaseBuilder(
                context,
                TournamentDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration() // Clear data on schema change
                .build()
        }
        
        /**
         * Clear database instance (for testing purposes).
         * Should not be used in production code.
         */
        @Synchronized
        fun clearInstance() {
            INSTANCE = null
            Timber.w("Database instance cleared (testing only)")
        }
    }
}
