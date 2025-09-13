package com.example.cineteca

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.example.cineteca.data.AppDatabase
import com.example.cineteca.data.Movie
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MovieListScreen()
            }
        }
    }

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    @Composable
    fun MovieListScreen() {
        val context = this
        var movies by remember { mutableStateOf(listOf<Movie>()) }

        LaunchedEffect(Unit) {
            val db = AppDatabase.getDatabase(context)
            db.movieDao().getAllMovies().collectLatest {
                movies = it
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Películas por ver") })
            }
        ) { padding ->
            LazyColumn(contentPadding = padding) {
                items(movies) { movie ->
                    ListItem(
                        headlineContent = { Text(movie.title) },
                        supportingContent = { movie.url?.let { Text(it) } }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
