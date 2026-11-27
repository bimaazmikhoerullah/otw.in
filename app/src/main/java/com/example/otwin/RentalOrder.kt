package com.example.otwin

/** Pesanan sewa yang disimpan di Realtime Database: orders/{uid}/{orderId}. */
data class RentalOrder(
    val id: String = "",
    val vehicleId: String = "",
    val vehicleName: String = "",
    val brand: String = "",
    val imageUrl: String = "",
    val customerName: String = "",
    val customerUsername: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val durationDays: Int = 0,
    val mode: String = "",
    val total: Long = 0L,
    val status: String = "Diproses",
    val createdAt: Long = 0L
)
