package com.example.otwin

import java.text.NumberFormat
import java.util.Locale

/**
 * Satu unit armada. Semua field berasal dari Realtime Database node `vehicles/{id}`,
 * jadi tidak ada data armada yang ditanam di aplikasi.
 *
 * `imageUrl` bisa berupa URL http(s) atau data URI hasil unggahan admin dari aplikasi.
 */
data class Vehicle(
    val id: String = "",
    val brand: String = "",
    val name: String = "",
    val category: String = VehicleRepository.CATEGORY_MPV,
    val seats: String = "",
    val transmission: String = "",
    val feature: String = "",
    val pricePerDay: Long = 0L,
    val rating: String = "",
    val reviews: String = "",
    val imageUrl: String = "",
    val badge: String = "",
    val badgeType: String = "blue",
    val active: Boolean = true,
    val createdAt: Long = 0L
) {
    /** Map untuk ditulis ke Realtime Database (masih kompatibel dengan rules). */
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "brand" to brand,
        "name" to name,
        "category" to category,
        "seats" to seats,
        "transmission" to transmission,
        "feature" to feature,
        "pricePerDay" to pricePerDay,
        "rating" to rating,
        "reviews" to reviews,
        "imageUrl" to imageUrl,
        "badge" to badge,
        "badgeType" to badgeType,
        "active" to active,
        "createdAt" to createdAt
    )
}

fun formatRupiah(amount: Long): String =
    "Rp " + NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(amount)
