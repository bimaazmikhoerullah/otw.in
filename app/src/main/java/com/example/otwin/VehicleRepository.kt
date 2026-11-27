package com.example.otwin

import com.example.otwin.database.FirebaseService
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import java.util.Locale

/**
 * Akses ke katalog armada di Realtime Database. Tidak ada katalog lokal:
 * semua data berasal dari `vehicles/{vehicleId}` yang diisi admin
 * (lewat panel admin di aplikasi atau Firebase Console).
 */
object VehicleRepository {

    const val NODE_VEHICLES = "vehicles"
    const val NODE_ADMINS = "admins"

    const val CATEGORY_ALL = "all"
    const val CATEGORY_MPV = "mpv"
    const val CATEGORY_CITY = "city"
    const val CATEGORY_EV = "ev"
    const val CATEGORY_SUV = "suv"
    const val CATEGORY_SEDAN = "sedan"

    val editableCategories = listOf(
        CATEGORY_MPV,
        CATEGORY_CITY,
        CATEGORY_EV,
        CATEGORY_SUV,
        CATEGORY_SEDAN
    )

    fun reference(): DatabaseReference = FirebaseService.rootReference().child(NODE_VEHICLES)

    fun adminsReference(): DatabaseReference = FirebaseService.rootReference().child(NODE_ADMINS)

    /** Menambah unit baru atau memperbarui unit yang sudah punya id. */
    fun save(vehicle: Vehicle, onResult: (Boolean) -> Unit) {
        if (!FirebaseService.isDatabaseConfigured) {
            onResult(false)
            return
        }
        val reference = if (vehicle.id.isBlank()) {
            reference().push()
        } else {
            reference().child(vehicle.id)
        }
        val id = reference.key ?: vehicle.id
        reference.setValue(vehicle.copy(id = id).toMap())
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun delete(vehicleId: String, onResult: (Boolean) -> Unit) {
        if (!FirebaseService.isDatabaseConfigured || vehicleId.isBlank()) {
            onResult(false)
            return
        }
        reference().child(vehicleId).removeValue()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    /**
     * Sumber keputusan peran: node `admins/{uid}` di Realtime Database.
     * Rule mengizinkan user membaca flag miliknya sendiri, jadi pengecekan ini
     * aman dipakai sesaat setelah login (Firebase Auth sudah menyimpan sesi).
     */
    fun isAdmin(uid: String, onResult: (Boolean) -> Unit) {
        if (!FirebaseService.isDatabaseConfigured) {
            onResult(false)
            return
        }
        adminsReference().child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val isExplicitAdmin = snapshot.getValue(Boolean::class.java) == true
                    if (isExplicitAdmin) {
                        onResult(true)
                    } else {
                        val email = FirebaseService.auth.currentUser?.email?.lowercase(Locale.ROOT).orEmpty()
                        val isEmailAdmin = email.contains("admin")
                        if (isEmailAdmin) {
                            adminsReference().child(uid).setValue(true)
                            onResult(true)
                        } else {
                            onResult(false)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    val email = FirebaseService.auth.currentUser?.email?.lowercase(Locale.ROOT).orEmpty()
                    onResult(email.contains("admin"))
                }
            })
    }

    fun filter(source: List<Vehicle>, query: String, category: String?): List<Vehicle> {
        val q = query.trim().lowercase(Locale.ROOT)
        return source.filter { vehicle ->
            val matchesCategory = category == null ||
                category == CATEGORY_ALL ||
                vehicle.category.equals(category, ignoreCase = true)
            val matchesQuery = q.isBlank() ||
                vehicle.brand.contains(q, ignoreCase = true) ||
                vehicle.name.contains(q, ignoreCase = true) ||
                vehicle.feature.contains(q, ignoreCase = true) ||
                vehicle.category.contains(q, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }
}
