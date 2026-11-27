package com.example.otwin

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.otwin.database.AuthRepository
import com.example.otwin.database.AuthResult
import com.example.otwin.database.FirebaseService
import com.example.otwin.database.GoogleAuthRepository
import com.example.otwin.databinding.ActivityRegisterBinding

@Suppress("DEPRECATION")
class Register : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var authRepository: AuthRepository
    private lateinit var googleAuthRepository: GoogleAuthRepository

    companion object {
        private const val RC_GOOGLE_SIGN_IN = 9002
        private val USERNAME_REGEX = Regex("^[a-z0-9_]{3,20}$")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FirebaseService.initialize(applicationContext)
        authRepository = AuthRepository()
        googleAuthRepository = GoogleAuthRepository(this)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnRegister.setOnClickListener { attemptRegister() }
        binding.btnGoogle.setOnClickListener { launchGoogleSignIn() }

        binding.tvLoginLink.setOnClickListener { goToLogin() }

        binding.tvForgotPassword.setOnClickListener { goToLogin() }

        binding.txtPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                updatePasswordStrength(s?.toString() ?: "")
            }
        })
        updatePasswordStrength("")
    }

    /** Updates the three strength bars and label as the user types. */
    private fun updatePasswordStrength(password: String) {
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        val hasSymbol = password.any { !it.isLetterOrDigit() }
        val hasMixedCase = password.any { it.isUpperCase() } && password.any { it.isLowerCase() }

        // 0 = empty, 1 = weak, 2 = medium, 3 = strong
        val level = when {
            password.isEmpty() -> 0
            password.length < 8 || !hasLetter || !hasDigit -> 1
            hasSymbol || hasMixedCase || password.length >= 12 -> 3
            else -> 2
        }

        val labelRes = when (level) {
            0 -> R.string.strength_empty
            1 -> R.string.strength_weak
            2 -> R.string.strength_medium
            else -> R.string.strength_strong
        }
        val colorRes = when (level) {
            1 -> R.color.error_red
            2 -> R.color.warning_amber
            3 -> R.color.secondary_green
            else -> R.color.text_secondary
        }

        val color = ContextCompat.getColor(this, colorRes)
        binding.tvStrengthValue.text = getString(labelRes)
        binding.tvStrengthValue.setTextColor(color)

        val activeTint = ColorStateList.valueOf(color)
        val inactiveTint = ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.disabled_background)
        )
        listOf(binding.strengthBar1, binding.strengthBar2, binding.strengthBar3)
            .forEachIndexed { index, bar ->
                bar.backgroundTintList = if (index < level) activeTint else inactiveTint
            }
    }

    private fun attemptRegister() {
        val name = binding.txtFullName.text.toString().trim()
        val username = binding.txtUsername.text.toString().trim()
        val email = binding.txtEmail.text.toString().trim()
        val phone = binding.txtPhone.text.toString().trim()
        val password = binding.txtPassword.text.toString()
        val confirmPassword = binding.txtConfirmPassword.text.toString()

        binding.tilName.error = null
        binding.tilUsername.error = null
        binding.tilEmail.error = null
        binding.tilPhone.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null

        if (name.isEmpty()) {
            binding.tilName.error = getString(R.string.auth_name_required)
            return
        }
        if (username.isEmpty()) {
            binding.tilUsername.error = getString(R.string.auth_username_required)
            return
        }
        if (!USERNAME_REGEX.matches(username.lowercase())) {
            binding.tilUsername.error = getString(R.string.auth_username_invalid)
            return
        }
        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.auth_email_required)
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.auth_invalid_email)
            return
        }
        if (phone.isEmpty()) {
            binding.tilPhone.error = getString(R.string.auth_phone_required)
            return
        }
        if (phone.filter { it.isDigit() }.length < 10) {
            binding.tilPhone.error = getString(R.string.auth_phone_invalid)
            return
        }
        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.auth_password_required)
            return
        }
        if (password.length < 8 ||
            !password.any { it.isLetter() } ||
            !password.any { it.isDigit() }
        ) {
            binding.tilPassword.error = getString(R.string.auth_weak_password)
            return
        }
        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = getString(R.string.auth_confirm_required)
            return
        }
        if (password != confirmPassword) {
            binding.tilConfirmPassword.error = getString(R.string.auth_password_mismatch)
            return
        }
        if (!binding.checkboxTerms.isChecked) {
            Toast.makeText(this, R.string.auth_terms_required, Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        authRepository.register(name, username, email, phone, password) { result ->
            setLoading(false)
            when (result) {
                is AuthResult.Success -> {
                    // Dedicated success screen with the registered account summary.
                    startActivity(RegisterSuccess.createIntent(this, result.profile))
                }

                is AuthResult.Failure -> {
                    showAuthFailure(result)
                }
            }
        }
    }

    private fun launchGoogleSignIn() {
        if (!googleAuthRepository.isConfigured) {
            Toast.makeText(this, R.string.auth_google_not_configured, Toast.LENGTH_LONG).show()
            return
        }
        setLoading(true)
        startActivityForResult(googleAuthRepository.getSignInIntent(), RC_GOOGLE_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != RC_GOOGLE_SIGN_IN) return

        googleAuthRepository.handleResult(data) { result ->
            setLoading(false)
            when (result) {
                is AuthResult.Success -> {
                    Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show()
                    goToMain()
                }

                is AuthResult.Failure -> {
                    showAuthFailure(result)
                }
            }
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun goToLogin() {
        val intent = Intent(this, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.btnRegister.isEnabled = !loading
        binding.btnGoogle.isEnabled = !loading
        binding.checkboxTerms.isEnabled = !loading
        binding.btnRegister.text = getString(
            if (loading) R.string.auth_processing else R.string.register_submit
        )
    }

    private fun showAuthFailure(failure: AuthResult.Failure) {
        val base = getString(failure.messageRes)
        val message = if (failure.detail.isNullOrBlank()) {
            base
        } else {
            "$base (${failure.detail})"
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
