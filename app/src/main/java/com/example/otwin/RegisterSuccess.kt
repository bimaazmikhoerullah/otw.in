package com.example.otwin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.otwin.database.UserProfile
import com.example.otwin.databinding.ActivityRegisterSuccessBinding

/** Shown after a successful email/password registration. */
class RegisterSuccess : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterSuccessBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvName.text = intent.getStringExtra(EXTRA_NAME).orEmpty()
        binding.tvUsername.text = intent.getStringExtra(EXTRA_USERNAME).orEmpty()
        binding.tvEmail.text = intent.getStringExtra(EXTRA_EMAIL).orEmpty()

        binding.btnBack.setOnClickListener { goToLogin() }
        binding.btnGoToLogin.setOnClickListener { goToLogin() }
        binding.tvLoginLink.setOnClickListener { goToLogin() }
    }

    private fun goToLogin() {
        val intent = Intent(this, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    companion object {
        private const val EXTRA_NAME = "register_success_name"
        private const val EXTRA_USERNAME = "register_success_username"
        private const val EXTRA_EMAIL = "register_success_email"

        fun createIntent(context: Context, profile: UserProfile): Intent =
            Intent(context, RegisterSuccess::class.java).apply {
                putExtra(EXTRA_NAME, profile.name)
                putExtra(EXTRA_USERNAME, profile.username)
                putExtra(EXTRA_EMAIL, profile.email)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            }
    }
}
