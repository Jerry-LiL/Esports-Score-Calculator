package com.lilranker.tournament.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.lilranker.tournament.data.repository.AuthRepository
import com.lilranker.tournament.databinding.ActivityLoginBinding
import com.lilranker.tournament.util.SessionManager
import kotlinx.coroutines.launch

/**
 * Login activity with key-based authentication
 * This is the entry point of the app
 */
class LoginActivity : BaseActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private lateinit var authRepository: AuthRepository
    private lateinit var sessionManager: SessionManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        authRepository = AuthRepository(this)
        sessionManager = SessionManager(this)
        
        // Check if session is already valid
        if (sessionManager.isSessionValid()) {
            navigateToMainActivity()
            return
        }
        
        setupUI()
    }
    
    private fun setupUI() {
        binding.btnLogin.setOnClickListener {
            val key = binding.etAccessKey.text.toString().trim()
            if (key.isEmpty()) {
                showError("Please enter an access key")
                return@setOnClickListener
            }
            
            // Check if it's the master password
            if (sessionManager.validateMasterPassword(key)) {
                // Master password - instant offline login, never expires
                sessionManager.saveLoginSession("MASTER_ACCESS", isMasterPassword = true)
                Toast.makeText(
                    this@LoginActivity,
                    "🔓 Master access granted!",
                    Toast.LENGTH_SHORT
                ).show()
                navigateToMainActivity()
                return@setOnClickListener
            }
            
            // Otherwise, validate with backend
            validateKey(key)
        }
    }
    
    private fun validateKey(key: String) {
        showLoading(true)
        hideError()
        
        lifecycleScope.launch {
            val result = authRepository.validateKey(key)
            
            result.onSuccess { response ->
                if (response.success) {
                    Toast.makeText(
                        this@LoginActivity,
                        "✅ ${response.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateToMainActivity()
                } else {
                    showError(response.message)
                    showLoading(false)
                }
            }.onFailure { error ->
                showError("Connection error: ${error.message}\n\nTip: Use master password for offline access")
                showLoading(false)
            }
        }
    }
    
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.etAccessKey.isEnabled = !isLoading
    }
    
    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }
    
    private fun hideError() {
        binding.tvError.visibility = View.GONE
    }
    
    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
