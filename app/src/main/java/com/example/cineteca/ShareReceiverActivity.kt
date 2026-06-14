package com.example.cineteca

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.cineteca.data.AppDatabase
import com.example.cineteca.data.Movie
import com.example.cineteca.utils.MetadataFetcher
import com.example.cineteca.utils.PlatformDetector
import kotlinx.coroutines.launch

class ShareReceiverActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleShareIntent()
    }

    private fun handleShareIntent() {
        val sharedText = intent?.takeIf { it.action == Intent.ACTION_SEND }
            ?.getStringExtra(Intent.EXTRA_TEXT)

        if (sharedText.isNullOrBlank()) {
            Toast.makeText(this, "No se recibió texto para guardar", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@ShareReceiverActivity)

            // Guarda inmediatamente con la URL como título
            val isUrl = sharedText.startsWith("http://") || sharedText.startsWith("https://")
            val initialMovie = Movie(
                title = sharedText,
                url = if (isUrl) sharedText else null,
                platform = if (isUrl) PlatformDetector.detect(sharedText)?.name else null
            )
            val rowId = db.movieDao().insertAndGetId(initialMovie)

            Toast.makeText(this@ShareReceiverActivity, "Guardado, obteniendo info...", Toast.LENGTH_SHORT).show()
            finish()

            // En background: busca metadata y actualiza el registro
            if (isUrl && rowId > 0) {
                val meta = MetadataFetcher.fetch(sharedText)
                val updated = initialMovie.copy(
                    id = rowId.toInt(),
                    title = meta.title ?: sharedText,
                    thumbnailUrl = meta.thumbnailUrl,
                    description = meta.description,
                    platform = meta.platform ?: initialMovie.platform
                )
                db.movieDao().update(updated)
            }
        }
    }
}
