package com.example.otwin.database

import com.example.otwin.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import java.util.Locale

sealed class AuthResult {
    data class Success(val profile: UserProfile) : AuthResult()
    data class Failure(val messageRes: Int, val detail: String? = null) : AuthResult()
}

/**
 * Firebase Authentication + Realtime Database operations.
 *
 * Firebase Auth only accepts an email as the sign-in identifier. To allow
 * username and phone number login, registration stores two alias indexes:
 *   usernames/{username} -> uid
 *   phones/{phone}       -> uid
 * and a public email lookup:
 *   loginEmails/{uid}    -> email
 * Login resolves alias -> uid -> email, then calls signInWithEmailAndPassword.
 * One Firebase Auth account therefore serves email, username and phone login.
 */
class AuthRepository {

    // Resolved lazily so an unconfigured Firebase project does not crash the Activity.
    private val auth: FirebaseAuth
        get() = FirebaseService.auth

    companion object {
        private val USERNAME_REGEX = Regex("^[a-z0-9_]{3,20}$")

        internal fun normalizeUsername(raw: String): String =
            raw.trim().lowercase(Locale.ROOT)

        internal fun normalizePhone(raw: String): String {
            val digits = raw.filter { it.isDigit() }
            return when {
                digits.startsWith("0") && digits.length > 1 -> "62" + digits.drop(1)
                digits.startsWith("62") -> digits
                else -> digits
            }
        }

        internal fun isPhoneIdentifier(value: String): Boolean {
            val trimmed = value.trim()
            if (trimmed.any { it.isLetter() }) return false
            return trimmed.filter { it.isDigit() }.length >= 8
        }
    }

    /** Accepts an email, a username, or a phone number as [identifier]. */
    fun login(identifier: String, password: String, onResult: (AuthResult) -> Unit) {
        if (!FirebaseService.isConfigured) {
            onResult(AuthResult.Failure(R.string.auth_not_configured))
            return
        }

        val id = identifier.trim()
        if (id.contains("@")) {
            signInWithEmail(id, password, onResult)
            return
        }

        val root = FirebaseService.rootReference()
        val aliasNode = if (isPhoneIdentifier(id)) "phones" else "usernames"
        val aliasKey = if (aliasNode == "phones") normalizePhone(id) else normalizeUsername(id)

        root.child(aliasNode).child(aliasKey).get()
            .addOnSuccessListener { aliasSnapshot ->
                val uid = aliasSnapshot.getValue(String::class.java)
                if (uid.isNullOrBlank()) {
                    onResult(AuthResult.Failure(R.string.auth_identifier_not_found))
                    return@addOnSuccessListener
                }

                root.child("loginEmails").child(uid).get()
                    .addOnSuccessListener { emailSnapshot ->
                        val email = emailSnapshot.getValue(String::class.java)
                        if (email.isNullOrBlank()) {
                            onResult(AuthResult.Failure(R.string.auth_identifier_not_found))
                        } else {
                            signInWithEmail(email, password, onResult)
                        }
                    }
                    .addOnFailureListener {
                        onResult(AuthResult.Failure(R.string.auth_generic_error))
                    }
            }
            .addOnFailureListener {
                onResult(AuthResult.Failure(R.string.auth_generic_error))
            }
    }

    /** Resolve an email/username/phone identifier to the real Firebase Auth email. */
    fun resolveEmail(identifier: String, onResult: (String?) -> Unit) {
        val id = identifier.trim()
        if (id.contains("@")) {
            onResult(id)
            return
        }
        if (!FirebaseService.isConfigured) {
            onResult(null)
            return
        }

        val root = FirebaseService.rootReference()
        val aliasNode = if (isPhoneIdentifier(id)) "phones" else "usernames"
        val aliasKey = if (aliasNode == "phones") normalizePhone(id) else normalizeUsername(id)

        root.child(aliasNode).child(aliasKey).get()
            .addOnSuccessListener { aliasSnapshot ->
                val uid = aliasSnapshot.getValue(String::class.java)
                if (uid.isNullOrBlank()) {
                    onResult(null)
                    return@addOnSuccessListener
                }
                root.child("loginEmails").child(uid).get()
                    .addOnSuccessListener { emailSnapshot ->
                        onResult(emailSnapshot.getValue(String::class.java)?.takeIf { it.isNotBlank() })
                    }
                    .addOnFailureListener { onResult(null) }
            }
            .addOnFailureListener { onResult(null) }
    }

    private fun signInWithEmail(
        email: String,
        password: String,
        onResult: (AuthResult) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { task ->
                val uid = task.user?.uid
                if (uid == null) {
                    onResult(AuthResult.Failure(R.string.auth_generic_error))
                    return@addOnSuccessListener
                }

                FirebaseService.usersReference().child(uid).get()
                    .addOnSuccessListener { snapshot ->
                        val profile = snapshot.getValue(UserProfile::class.java)
                            ?: UserProfile(email = email)
                        onResult(AuthResult.Success(profile))
                    }
                    .addOnFailureListener {
                        onResult(AuthResult.Failure(R.string.auth_generic_error))
                    }
            }
            .addOnFailureListener { error ->
                onResult(AuthResult.Failure(errorRes(error)))
            }
    }

    fun register(
        name: String,
        username: String,
        email: String,
        phone: String,
        password: String,
        onResult: (AuthResult) -> Unit
    ) {
        if (!FirebaseService.isConfigured) {
            onResult(AuthResult.Failure(R.string.auth_not_configured))
            return
        }

        val normalizedUsername = normalizeUsername(username)
        val normalizedPhone = normalizePhone(phone)

        if (!USERNAME_REGEX.matches(normalizedUsername)) {
            onResult(AuthResult.Failure(R.string.auth_username_invalid))
            return
        }
        if (normalizedPhone.length < 10) {
            onResult(AuthResult.Failure(R.string.auth_phone_invalid))
            return
        }

        val root = FirebaseService.rootReference()
        root.child("usernames").child(normalizedUsername).get()
            .addOnSuccessListener { usernameSnapshot ->
                if (usernameSnapshot.exists()) {
                    onResult(AuthResult.Failure(R.string.auth_username_taken))
                    return@addOnSuccessListener
                }

                root.child("phones").child(normalizedPhone).get()
                    .addOnSuccessListener { phoneSnapshot ->
                        if (phoneSnapshot.exists()) {
                            onResult(AuthResult.Failure(R.string.auth_phone_taken))
                            return@addOnSuccessListener
                        }
                        createAccount(
                            root,
                            name,
                            normalizedUsername,
                            email,
                            normalizedPhone,
                            password,
                            onResult
                        )
                    }
                    .addOnFailureListener {
                        onResult(AuthResult.Failure(R.string.auth_generic_error))
                    }
            }
            .addOnFailureListener {
                onResult(AuthResult.Failure(R.string.auth_generic_error))
            }
    }

    private fun createAccount(
        root: com.google.firebase.database.DatabaseReference,
        name: String,
        username: String,
        email: String,
        phone: String,
        password: String,
        onResult: (AuthResult) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { task ->
                val uid = task.user?.uid
                if (uid == null) {
                    onResult(AuthResult.Failure(R.string.auth_generic_error))
                    return@addOnSuccessListener
                }

                val profile = UserProfile(
                    name = name,
                    username = username,
                    email = email,
                    phone = phone,
                    createdAt = System.currentTimeMillis()
                )

                // Multi-path write: profile + both aliases + public email lookup.
                val updates = mapOf(
                    "/users/$uid" to profile,
                    "/loginEmails/$uid" to email,
                    "/usernames/$username" to uid,
                    "/phones/$phone" to uid
                )

                root.updateChildren(updates)
                    .addOnSuccessListener {
                        // createUserWithEmailAndPassword signs the new user in automatically.
                        // Registration must leave the user at the login screen so another
                        // account can be created without being redirected to MainActivity.
                        auth.signOut()
                        onResult(AuthResult.Success(profile))
                    }
                    .addOnFailureListener {
                        // Remove the auth account so the user can retry with another alias.
                        auth.currentUser?.delete()
                        onResult(AuthResult.Failure(R.string.auth_generic_error))
                    }
            }
            .addOnFailureListener { error ->
                onResult(AuthResult.Failure(errorRes(error)))
            }
    }

    private fun errorRes(error: Exception): Int = when (error) {
        is FirebaseAuthInvalidUserException,
        is FirebaseAuthInvalidCredentialsException -> R.string.auth_invalid_credentials

        is FirebaseAuthUserCollisionException -> R.string.auth_email_in_use
        is FirebaseAuthWeakPasswordException -> R.string.auth_weak_password
        else -> R.string.auth_generic_error
    }
}
