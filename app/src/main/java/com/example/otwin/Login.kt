package com.example.otwin

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.otwin.database.AuthRepository
import com.example.otwin.database.AuthResult
import com.example.otwin.database.FirebaseService
import com.example.otwin.database.GoogleAuthRepository
import com.example.otwin.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuthInvalidUserException

@Suppress("DEPRECATION")
class Login : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authRepository: AuthRepository
    private lateinit var googleAuthRepository: GoogleAuthRepository

    companion object {
        private const val RC_GOOGLE_SIGN_IN = 9001
        private const val PREFS_SESSION = "otwin_session"
        private const val KEY_REMEMBER = "remember_me"
        private val USERNAME_REGEX = Regex("^[a-z0-9_]{3,20}$")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FirebaseService.initialize(applicationContext)
        authRepository = AuthRepository()
        googleAuthRepository = GoogleAuthRepository(this)

        // Firebase Auth menyimpan sesi lokal. Kalau masih ada, langsung masuk
        // ke halaman utama tanpa meminta login lagi setelah app ditutup.
        if (restoreExistingSession()) return

        setupListeners()
    }

    /** Returns true when the Activity has been handed off to MainActivity. */
    private fun restoreExistingSession(): Boolean {
        val remember = sessionPreferences().getBoolean(KEY_REMEMBER, true)
        binding.checkboxRemember.isChecked = remember

        val auth = FirebaseService.auth
        if (auth.currentUser == null) return false

        if (!remember) {
            // Pilihan "Ingat Saya" tidak dicentris: sesi lama tidak dipertahankan.
            auth.signOut()
            return false
        }

        // Tunda satu frame supaya onCreate selesai sebelum pindah task.
        binding.root.post { goToTarget() }
        return true
    }

    private fun sessionPreferences() =
        getSharedPreferences(PREFS_SESSION, MODE_PRIVATE)

    /**
     * Firebase Auth Android menyimpan sesi secara lokal secara default.
     * Preferensi ini dipakai saat aplikasi dibuka lagi: kalau tidak dicentris,
     * sesi lama dihapus di [restoreExistingSession].
     */
    private fun saveRememberPreference() {
        sessionPreferences()
            .edit()
            .putBoolean(KEY_REMEMBER, binding.checkboxRemember.isChecked)
            .apply()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener { attemptLogin() }

        binding.btnGoogle.setOnClickListener { launchGoogleSignIn() }

        binding.tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
        }

        binding.tvForgotPassword.setOnClickListener { attemptPasswordReset() }
    }

    /** Send a Firebase password-reset link using the identifier in the login field. */
    private fun attemptPasswordReset() {
        val identifier = binding.txtEmail.text.toString().trim()
        binding.tilEmail.error = null

        if (identifier.isEmpty()) {
            binding.tilEmail.error = getString(R.string.auth_forgot_required)
            return
        }
        if (identifier.contains("@") && !Patterns.EMAIL_ADDRESS.matcher(identifier).matches()) {
            binding.tilEmail.error = getString(R.string.auth_invalid_email)
            return
        }

        authRepository.resolveEmail(identifier) { email ->
            if (email.isNullOrBlank()) {
                binding.tilEmail.error = getString(R.string.auth_identifier_not_found)
                return@resolveEmail
            }
            FirebaseService.auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Toast.makeText(
                        this,
                        getString(R.string.auth_forgot_sent, email),
                        Toast.LENGTH_LONG
                    ).show()
                }
                .addOnFailureListener { error ->
                    val messageRes = if (error is FirebaseAuthInvalidUserException) {
                        R.string.auth_identifier_not_found
                    } else {
                        R.string.auth_generic_error
                    }
                    Toast.makeText(this, messageRes, Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun attemptLogin() {
        val identifier = binding.txtEmail.text.toString().trim()
        val password = binding.txtPassword.text.toString().trim()

        binding.tilEmail.error = null
        binding.tilPassword.error = null

        if (identifier.isEmpty()) {
            binding.tilEmail.error = getString(R.string.auth_login_required)
            return
        }
        if (identifier.contains("@") && !Patterns.EMAIL_ADDRESS.matcher(identifier).matches()) {
            binding.tilEmail.error = getString(R.string.auth_invalid_email)
            return
        }
        if (!identifier.contains("@")) {
            val phoneLike = identifier.none { it.isLetter() } &&
                identifier.filter { it.isDigit() }.length >= 8
            if (phoneLike && identifier.filter { it.isDigit() }.length < 10) {
                binding.tilEmail.error = getString(R.string.auth_phone_invalid)
                return
            }
            if (!phoneLike && !USERNAME_REGEX.matches(identifier.lowercase())) {
                binding.tilEmail.error = getString(R.string.auth_username_invalid)
                return
            }
        }
        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.auth_password_required)
            return
        }
        if (password.length < 6) {
            binding.tilPassword.error = getString(R.string.auth_password_short)
            return
        }

        saveRememberPreference()
        setLoading(true)
        authRepository.login(identifier, password) { result ->
            setLoading(false)
            when (result) {
                is AuthResult.Success -> {
                    Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show()
                    goToTarget()
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
        saveRememberPreference()
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
                    goToTarget()
                }

                is AuthResult.Failure -> {
                    showAuthFailure(result)
                }
            }
        }
    }

    /**
     * Admin masuk ke panel admin, user biasa ke Beranda. Peran dibaca dari
     * `admins/{uid}` setelah Firebase Auth menyimpan sesi login.
     */
    private fun goToTarget() {
        val user = FirebaseService.auth.currentUser
        if (user == null || !FirebaseService.isDatabaseConfigured) {
            openCleared(MainActivity::class.java)
            return
        }

        VehicleRepository.isAdmin(user.uid) { isAdmin ->
            if (isFinishing || isDestroyed) return@isAdmin
            val target = if (isAdmin) AdminActivity::class.java else MainActivity::class.java
            openCleared(target)
        }
    }

    private fun openCleared(target: Class<out AppCompatActivity>) {
        val intent = Intent(this, target)
        // Hapus Login dari back stack supaya tombol back tidak kembali ke form.
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.btnLogin.isEnabled = !loading
        binding.btnGoogle.isEnabled = !loading
        binding.checkboxRemember.isEnabled = !loading
        binding.btnLogin.text = getString(
            if (loading) R.string.auth_processing else R.string.login_submit
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
