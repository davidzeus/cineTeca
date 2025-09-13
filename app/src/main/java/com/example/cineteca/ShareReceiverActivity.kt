package com.example.cineteca

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.cineteca.data.AppDatabase
import com.example.cineteca.data.Movie
import kotlinx.coroutines.launch

class ShareReceiverActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleShareIntent()
    }

    private fun handleShareIntent() {
        val intent = intent
        if (intent?.action == Intent.ACTION_SEND) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                saveMovie(sharedText)
            } else {
                Toast.makeText(this, "No se recibió texto para guardar", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            finish()
        }
    }

    private fun saveMovie(text: String) {
        val movie = Movie(title = text, url = text)
        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch {
            db.movieDao().insert(movie)
            Toast.makeText(this@ShareReceiverActivity, "Película guardada", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
