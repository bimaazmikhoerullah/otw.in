package com.example.otwin

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.ImageView
import coil.load

/** Menampilkan gambar armada dari URL http(s) maupun data URI hasil unggahan admin. */
object VehicleImageLoader {

    fun load(imageView: ImageView, source: String?) {
        val value = source?.trim().orEmpty()
        when {
            value.startsWith("http://", ignoreCase = true) ||
                value.startsWith("https://", ignoreCase = true) ->
                imageView.load(value) {
                    placeholder(R.drawable.ic_car)
                    error(R.drawable.ic_car)
                    crossfade(true)
                }

            value.startsWith("data:image", ignoreCase = true) -> {
                val base64 = value.substringAfter(",", "")
                val bitmap = try {
                    val bytes = Base64.decode(base64, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } catch (_: IllegalArgumentException) {
                    null
                }
                if (bitmap != null) imageView.setImageBitmap(bitmap)
                else imageView.setImageResource(R.drawable.ic_car)
            }

            else -> imageView.setImageResource(R.drawable.ic_car)
        }
    }

    /** Kompres gambar yang dipilih admin menjadi data URI agar disimpan di RTDB. */
    fun encode(selected: Bitmap, maxDimension: Int = 640, quality: Int = 70): String? {
        val ratio = minOf(
            maxDimension.toFloat() / selected.width,
            maxDimension.toFloat() / selected.height,
            1f
        )
        val width = (selected.width * ratio).toInt().coerceAtLeast(1)
        val height = (selected.height * ratio).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(selected, width, height, true)
        val stream = java.io.ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        val encoded = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        return "data:image/jpeg;base64,$encoded"
    }
}
