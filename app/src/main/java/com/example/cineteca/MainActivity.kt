package com.example.cineteca

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import com.example.cineteca.data.AppDatabase
import com.example.cineteca.data.Movie
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF121212),
                    surface = Color(0xFF1E1E1E),
                    primary = Color(0xFF4F8EF7),
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                MovieListScreen()
            }
        }
    }

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    @Composable
    fun MovieListScreen() {
        val context = this
        val localContext = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        var movies by remember { mutableStateOf(listOf<Movie>()) }
        var editingMovieId by remember { mutableStateOf<Int?>(null) }
        var tempTitle by remember { mutableStateOf("") }
        
        fun openUrl(url: String) {
            try {
                val uri = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    "https://$url"
                } else {
                    url
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                localContext.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(localContext, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show()
            }
        }
        
        fun updateMovieTitle(movieId: Int, newTitle: String) {
            coroutineScope.launch {
                val db = AppDatabase.getDatabase(context)
                val updatedMovie = movies.find { it.id == movieId }?.copy(title = newTitle)
                updatedMovie?.let { 
                    db.movieDao().update(it)
                    Toast.makeText(localContext, "Título actualizado", Toast.LENGTH_SHORT).show()
                }
            }
        }

        LaunchedEffect(Unit) {
            val db = AppDatabase.getDatabase(context)
            db.movieDao().getAllMovies().collectLatest {
                movies = it
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "🎬 CineTeca",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ) 
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1E1E1E)
                    )
                )
            },
            containerColor = Color(0xFF121212)
        ) { padding ->
            if (movies.isEmpty()) {
                // Estado vacío
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(32.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "🍿",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No hay películas guardadas",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Comparte enlaces desde otras apps para agregarlas a tu lista",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding() + 8.dp,
                        bottom = padding.calculateBottomPadding() + 8.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(movies) { movie ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { 
                                    movie.url?.let { url -> openUrl(url) } 
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF2A2A2A)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (editingMovieId == movie.id) {
                                        BasicTextField(
                                            value = tempTitle,
                                            onValueChange = { tempTitle = it },
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(
                                                    Color(0xFF3A3A3A),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(8.dp),
                                            textStyle = TextStyle(
                                                color = Color.White,
                                                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            cursorBrush = SolidColor(Color.White),
                                            singleLine = true
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = {
                                                if (tempTitle.isNotBlank()) {
                                                    updateMovieTitle(movie.id, tempTitle)
                                                    editingMovieId = null
                                                }
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Guardar",
                                                tint = Color(0xFF4F8EF7)
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = movie.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                editingMovieId = movie.id
                                                tempTitle = movie.title
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Editar título",
                                                tint = Color.Gray
                                            )
                                        }
                                    }
                                }
                                movie.url?.let { url ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = url,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF4F8EF7),
                                        modifier = Modifier.clickable { openUrl(url) }
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Agregada ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(movie.addedAt))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                    if (movie.url != null) {
                                        Text(
                                            text = "👆 Toca para abrir",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF4F8EF7)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
