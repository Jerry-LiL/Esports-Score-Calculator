package com.lilranker.tournament.ui

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.lilranker.tournament.R
import com.lilranker.tournament.TournamentApplication
import com.lilranker.tournament.databinding.ActivityMainBinding
import com.lilranker.tournament.ui.viewmodel.ConfigViewModel
import com.lilranker.tournament.ui.viewmodel.TournamentViewModelFactory
import com.lilranker.tournament.util.SessionManager
import com.lilranker.tournament.util.SessionMonitor
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : BaseActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ConfigViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var sessionMonitor: SessionMonitor
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize session management
        sessionManager = SessionManager(this)
        sessionMonitor = SessionMonitor(this)
        
        // Check if session is valid, if not redirect to login
        if (!sessionManager.isSessionValid()) {
            redirectToLogin()
            return
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val repository = (application as TournamentApplication).repository
        val factory = TournamentViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ConfigViewModel::class.java]
        
        // Start session monitoring
        sessionMonitor.startMonitoring()
        
        setupUI()
        checkConfiguration()
    }
    
    override fun onResume() {
        super.onResume()
        
        // Check session validity on resume
        if (!sessionManager.isSessionValid()) {
            redirectToLogin()
            return
        }
        
        // Restart monitoring
        sessionMonitor.startMonitoring()
        
        checkConfiguration()
    }
    
    override fun onPause() {
        super.onPause()
        // Stop monitoring when activity is paused
        sessionMonitor.stopMonitoring()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up
        sessionMonitor.stopMonitoring()
    }
    
    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun setupUI() {
        binding.btnConfigure.setOnClickListener {
            try {
                startActivity(Intent(this, ConfigActivity::class.java))
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error starting ConfigActivity", e)
            }
        }
        
        binding.btnEnterMatch.setOnClickListener {
            try {
                viewModel.getCurrentConfig { config ->
                    if (config != null && config.totalDays > 0 && config.matchesPerDay > 0) {
                        try {
                            startActivity(Intent(this, MatchEntryActivity::class.java))
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Error starting MatchEntryActivity", e)
                        }
                    } else {
                        runOnUiThread {
                            binding.tvConfigWarning.visibility = View.VISIBLE
                            if (config == null) {
                                binding.tvConfigWarning.text = "Please configure tournament settings first"
                            } else if (config.totalDays <= 0) {
                                binding.tvConfigWarning.text = "Please set Total Days to at least 1"
                            } else if (config.matchesPerDay <= 0) {
                                binding.tvConfigWarning.text = "Please set Matches Per Day to at least 1"
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error in btnEnterMatch click", e)
            }
        }
        
        binding.btnLeaderboard.setOnClickListener {
            try {
                viewModel.getCurrentConfig { config ->
                    if (config != null && config.totalDays > 0) {
                        try {
                            startActivity(Intent(this, LeaderboardActivity::class.java))
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Error starting LeaderboardActivity", e)
                        }
                    } else {
                        runOnUiThread {
                            binding.tvConfigWarning.visibility = View.VISIBLE
                            if (config == null) {
                                binding.tvConfigWarning.text = "Please configure tournament settings first"
                            } else if (config.totalDays <= 0) {
                                binding.tvConfigWarning.text = "Please set Total Days to at least 1"
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error in btnLeaderboard click", e)
            }
        }
        
        binding.fabLanguage.setOnClickListener {
            try {
                showLanguageDialog()
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error showing language dialog", e)
            }
        }
    }
    
    private fun showLanguageDialog() {
        val languages = arrayOf(
            getString(R.string.language_english),
            getString(R.string.language_persian)
        )
        val languageCodes = arrayOf("en", "fa")
        
        val currentLang = getLanguagePreference()
        val checkedItem = languageCodes.indexOf(currentLang)
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_language))
            .setSingleChoiceItems(languages, checkedItem) { dialog, which ->
                val selectedLanguage = languageCodes[which]
                if (selectedLanguage != currentLang) {
                    setLanguagePreference(selectedLanguage)
                    // Restart the entire app to apply the language change
                    val intent = intent
                    finish()
                    startActivity(intent)
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }
    
    private fun getLanguagePreference(): String {
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        return prefs.getString("language", "en") ?: "en"
    }
    
    private fun setLanguagePreference(languageCode: String) {
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("language", languageCode).apply()
    }
    
    private fun checkConfiguration() {
        try {
            lifecycleScope.launch {
                try {
                    viewModel.getCurrentConfig { config ->
                        lifecycleScope.launch {
                            try {
                                val hasMatchData = viewModel.hasAnyMatchData()
                                
                                runOnUiThread {
                                    // Check if config exists AND has valid values (totalDays > 0, matchesPerDay > 0)
                                    val isConfigured = config != null && config.totalDays > 0 && config.matchesPerDay > 0
                                    
                                    // Update button visibility based on state
                                    updateButtonVisibility(isConfigured, hasMatchData)
                                    
                                    if (!isConfigured) {
                                        binding.tvConfigWarning.visibility = View.VISIBLE
                                        // Set button to red when not configured properly
                                        binding.btnConfigure.setBackgroundColor(getColor(com.lilranker.tournament.R.color.error_red))
                                        
                                        // Update warning message based on what's missing
                                        if (config == null) {
                                            binding.tvConfigWarning.text = "Please configure tournament settings first"
                                        } else if (config.totalDays <= 0 && config.matchesPerDay <= 0) {
                                            binding.tvConfigWarning.text = "Please set Total Days and Matches Per Day to at least 1"
                                        } else if (config.totalDays <= 0) {
                                            binding.tvConfigWarning.text = "Please set Total Days to at least 1"
                                        } else if (config.matchesPerDay <= 0) {
                                            binding.tvConfigWarning.text = "Please set Matches Per Day to at least 1"
                                        }
                                    } else {
                                        // Config is properly set with valid values
                                        binding.tvConfigWarning.visibility = View.GONE
                                        // Set button to green when configured properly
                                        binding.btnConfigure.setBackgroundColor(getColor(com.lilranker.tournament.R.color.success_green))
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "Error checking match data", e)
                                runOnUiThread {
                                    binding.tvConfigWarning.visibility = View.VISIBLE
                                    binding.tvConfigWarning.text = "Error checking configuration: ${e.message}"
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error getting config", e)
                    runOnUiThread {
                        binding.tvConfigWarning.visibility = View.VISIBLE
                        binding.tvConfigWarning.text = "Error loading configuration"
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in checkConfiguration", e)
        }
    }
    
    /**
     * Update button visibility based on configuration and match data state.
     * 
     * Rules:
     * - Configure Tournament: Always visible
     * - Enter Match Results: Visible only when tournament is configured
     * - View Leaderboard: Visible only when at least 1 match has been entered
     */
    private fun updateButtonVisibility(isConfigured: Boolean, hasMatchData: Boolean) {
        // Configure button is always visible
        binding.btnConfigure.visibility = View.VISIBLE
        
        // Enter Match Results: Show only when configured
        if (isConfigured) {
            binding.btnEnterMatch.visibility = View.VISIBLE
        } else {
            binding.btnEnterMatch.visibility = View.GONE
        }
        
        // View Leaderboard: Show only when at least 1 match has data
        if (hasMatchData) {
            binding.btnLeaderboard.visibility = View.VISIBLE
        } else {
            binding.btnLeaderboard.visibility = View.GONE
        }
    }
}
