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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

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
            val isUrl = sharedText.startsWith("http://") || sharedText.startsWith("https://")

            // Espera la metadata antes de cerrar — el lifecycleScope se cancela con finish()
            val meta = if (isUrl) {
                try {
                    withTimeout(9000) { MetadataFetcher.fetch(sharedText) }
                } catch (e: TimeoutCancellationException) {
                    MetadataFetcher.Metadata()
                }
            } else null

            val movie = Movie(
                title = meta?.title?.takeIf { it.isNotBlank() } ?: sharedText,
                url = if (isUrl) sharedText else null,
                thumbnailUrl = meta?.thumbnailUrl,
                description = meta?.description,
                platform = meta?.platform
                    ?: if (isUrl) PlatformDetector.detect(sharedText)?.name else null
            )
            db.movieDao().insert(movie)

            Toast.makeText(this@ShareReceiverActivity, "¡Guardado!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
