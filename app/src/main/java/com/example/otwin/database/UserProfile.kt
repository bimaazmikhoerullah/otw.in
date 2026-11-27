package com.example.otwin.database

/**
 * Data profil yang disimpan di Realtime Database: users/{uid}.
 *
 * email  = identitas asli Firebase Authentication (dipakai signInWithEmailAndPassword)
 * username / phone = alias yang dipetakan ke uid agar bisa dipakai sebagai pengganti email
 */
data class UserProfile(
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val phone: String = "",
    val createdAt: Long = 0L
)
