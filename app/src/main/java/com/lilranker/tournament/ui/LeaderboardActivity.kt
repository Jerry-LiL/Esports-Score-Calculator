package com.lilranker.tournament.ui

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.lilranker.tournament.R
import com.lilranker.tournament.TournamentApplication
import com.lilranker.tournament.databinding.ActivityLeaderboardBinding
import com.lilranker.tournament.ui.adapter.LeaderboardAdapter
import com.lilranker.tournament.ui.viewmodel.ConfigViewModel
import com.lilranker.tournament.ui.viewmodel.LeaderboardViewModel
import com.lilranker.tournament.ui.viewmodel.TournamentViewModelFactory
import kotlinx.coroutines.launch

class LeaderboardActivity : BaseActivity() {
    
    private lateinit var binding: ActivityLeaderboardBinding
    private lateinit var viewModel: LeaderboardViewModel
    private lateinit var configViewModel: ConfigViewModel
    private lateinit var adapter: LeaderboardAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLeaderboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val repository = (application as TournamentApplication).repository
        val factory = TournamentViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[LeaderboardViewModel::class.java]
        configViewModel = ViewModelProvider(this, factory)[ConfigViewModel::class.java]
        
        setupRecyclerView()
        setupDayFilter()
        setupSortFilter()
        observeViewModel()
    }
    
    private fun setupRecyclerView() {
        adapter = LeaderboardAdapter()
        binding.recyclerViewLeaderboard.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewLeaderboard.adapter = adapter
    }
    
    private fun setupSortFilter() {
        val sortOptions = listOf(
            getString(R.string.sort_by_points),
            getString(R.string.sort_by_team_number)
        )
        
        val spinnerAdapter = ArrayAdapter(
            this,
            R.layout.spinner_item_day_filter,
            sortOptions
        )
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_day_filter)
        binding.spinnerSortBy.adapter = spinnerAdapter
        
        binding.spinnerSortBy.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.setSortMode(position)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                viewModel.setSortMode(0) // Default to points
            }
        }
    }
    
    private fun setupDayFilter() {
        // Get the tournament configuration to determine total days
        configViewModel.getCurrentConfig { config ->
            runOnUiThread {
                if (config != null) {
                    val totalDays = config.totalDays
                    
                    // Create list: "All Days", "Day 1", "Day 2", ..., "Day N"
                    val dayOptions = mutableListOf<String>()
                    dayOptions.add(getString(R.string.all_days))
                    for (day in 1..totalDays) {
                        dayOptions.add(getString(R.string.day_format, day))
                    }
                    
                    // Setup spinner adapter with custom dark theme layouts
                    val spinnerAdapter = ArrayAdapter(
                        this,
                        R.layout.spinner_item_day_filter,
                        dayOptions
                    )
                    spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_day_filter)
                    binding.spinnerDayFilter.adapter = spinnerAdapter
                    
                    // Handle spinner selection
                    binding.spinnerDayFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            // Position 0 = "All Days" (-1), Position 1 = "Day 1" (1), etc.
                            val selectedDay = if (position == 0) -1 else position
                            viewModel.setDayFilter(selectedDay)
                        }
                        
                        override fun onNothingSelected(parent: AdapterView<*>?) {
                            // Default to "All Days"
                            viewModel.setDayFilter(-1)
                        }
                    }
                }
            }
        }
    }
    
    private fun observeViewModel() {
        viewModel.leaderboard.observe(this) { teamScores ->
            if (teamScores.isEmpty()) {
                binding.tvNoData.visibility = View.VISIBLE
                binding.recyclerViewLeaderboard.visibility = View.GONE
            } else {
                binding.tvNoData.visibility = View.GONE
                binding.recyclerViewLeaderboard.visibility = View.VISIBLE
                adapter.submitList(teamScores)
            }
        }
    }
}
