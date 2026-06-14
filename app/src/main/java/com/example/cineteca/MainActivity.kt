package com.example.cineteca

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.cineteca.R
import com.example.cineteca.data.AppDatabase
import com.example.cineteca.data.Movie
import com.example.cineteca.ui.theme.CineTecaTheme
import com.example.cineteca.utils.PlatformDetector
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CineTecaTheme {
                MovieListScreen()
            }
        }
    }
}

enum class MovieFilter { ALL, UNWATCHED, WATCHED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieListScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }

    var movies by remember { mutableStateOf(listOf<Movie>()) }
    var activeFilter by remember { mutableStateOf(MovieFilter.ALL) }
    var editingId by remember { mutableStateOf<Int?>(null) }
    var editTitle by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.movieDao().getAllMovies().collectLatest { movies = it }
    }

    val filtered = remember(movies, activeFilter) {
        when (activeFilter) {
            MovieFilter.ALL -> movies
            MovieFilter.UNWATCHED -> movies.filter { !it.isWatched }
            MovieFilter.WATCHED -> movies.filter { it.isWatched }
        }
    }

    fun openUrl(url: String) {
        try {
            val uri = Uri.parse(if (!url.startsWith("http")) "https://$url" else url)
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_cineteca),
                            contentDescription = "CineTeca logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Text(
                            "CineTeca",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = activeFilter == MovieFilter.ALL,
                        onClick = { activeFilter = MovieFilter.ALL },
                        label = { Text("Todas (${movies.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = activeFilter == MovieFilter.UNWATCHED,
                        onClick = { activeFilter = MovieFilter.UNWATCHED },
                        label = { Text("Por ver (${movies.count { !it.isWatched }})") }
                    )
                }
                item {
                    FilterChip(
                        selected = activeFilter == MovieFilter.WATCHED,
                        onClick = { activeFilter = MovieFilter.WATCHED },
                        label = { Text("Vistas (${movies.count { it.isWatched }})") }
                    )
                }
            }

            if (filtered.isEmpty()) {
                EmptyState(activeFilter, Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { movie ->
                        val swipeState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                when (value) {
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        scope.launch { db.movieDao().delete(movie) }
                                        true
                                    }
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        scope.launch {
                                            db.movieDao().update(movie.copy(isWatched = !movie.isWatched))
                                        }
                                        false
                                    }
                                    else -> false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = swipeState,
                            backgroundContent = {
                                val (bgColor, icon, label, align) = when (swipeState.dismissDirection) {
                                    SwipeToDismissBoxValue.EndToStart -> Quad(
                                        MaterialTheme.colorScheme.errorContainer,
                                        Icons.Default.Delete, "Eliminar", Alignment.CenterEnd
                                    )
                                    SwipeToDismissBoxValue.StartToEnd -> if (movie.isWatched) Quad(
                                        MaterialTheme.colorScheme.secondaryContainer,
                                        Icons.Default.Close, "No vista", Alignment.CenterStart
                                    ) else Quad(
                                        MaterialTheme.colorScheme.tertiaryContainer,
                                        Icons.Default.Check, "Vista", Alignment.CenterStart
                                    )
                                    else -> Quad(Color.Transparent, null, "", Alignment.Center)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(bgColor, RoundedCornerShape(16.dp))
                                        .padding(horizontal = 24.dp),
                                    contentAlignment = align
                                ) {
                                    if (icon != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(icon, label, tint = MaterialTheme.colorScheme.onSurface)
                                            Text(label, fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                        ) {
                            MovieCard(
                                movie = movie,
                                isEditing = editingId == movie.id,
                                editTitleValue = editTitle,
                                onEditTitleChange = { editTitle = it },
                                onStartEdit = { editingId = movie.id; editTitle = movie.title },
                                onSaveEdit = {
                                    if (editTitle.isNotBlank()) {
                                        scope.launch {
                                            db.movieDao().update(movie.copy(title = editTitle))
                                        }
                                        editingId = null
                                    }
                                },
                                onCancelEdit = { editingId = null },
                                onToggleWatched = {
                                    scope.launch {
                                        db.movieDao().update(movie.copy(isWatched = !movie.isWatched))
                                    }
                                },
                                onOpenUrl = { movie.url?.let { openUrl(it) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MovieCard(
    movie: Movie,
    isEditing: Boolean,
    editTitleValue: String,
    onEditTitleChange: (String) -> Unit,
    onStartEdit: () -> Unit,
    onSaveEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onToggleWatched: () -> Unit,
    onOpenUrl: () -> Unit
) {
    val context = LocalContext.current
    val platformColor = movie.platform?.let { PlatformDetector.colorFor(it) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isEditing && movie.url != null) { onOpenUrl() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (movie.isWatched)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (movie.isWatched) 1.dp else 3.dp
        )
    ) {
        // Thumbnail en la parte superior si existe
        if (movie.thumbnailUrl != null && !isEditing) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(movie.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            )
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Badge de estado (solo si no hay thumbnail)
                if (movie.thumbnailUrl == null) {
                    Surface(
                        shape = CircleShape,
                        color = if (movie.isWatched)
                            MaterialTheme.colorScheme.tertiaryContainer
                        else
                            MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (movie.isWatched) {
                                Icon(
                                    Icons.Default.Check, "Vista",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text("🎬", fontSize = 16.sp)
                            }
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    if (isEditing) {
                        OutlinedTextField(
                            value = editTitleValue,
                            onValueChange = onEditTitleChange,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Título") },
                            textStyle = MaterialTheme.typography.bodyLarge
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onCancelEdit) { Text("Cancelar") }
                            TextButton(onClick = onSaveEdit) { Text("Guardar") }
                        }
                    } else {
                        Text(
                            text = movie.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (movie.isWatched)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Descripción
                        if (movie.description != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = movie.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (!isEditing) {
                    IconButton(onClick = onStartEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Edit, "Editar",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!isEditing) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Badge de plataforma
                        if (movie.platform != null) {
                            val badgeColor = platformColor?.let { Color(it) }
                                ?: MaterialTheme.colorScheme.secondaryContainer
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = badgeColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = movie.platform,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                .format(Date(movie.addedAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    AssistChip(
                        onClick = onToggleWatched,
                        label = {
                            Text(
                                if (movie.isWatched) "No vista" else "Marcar vista",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (movie.isWatched) Icons.Default.Refresh else Icons.Default.Check,
                                null, modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(filter: MovieFilter, modifier: Modifier = Modifier) {
    val (emoji, title, subtitle) = when (filter) {
        MovieFilter.ALL -> Triple(
            "🎬", "No hay nada guardado",
            "Comparte un enlace desde Instagram, YouTube u otra app y aparecerá aquí"
        )
        MovieFilter.UNWATCHED -> Triple(
            "✅", "¡Todo visto!",
            "No tienes películas pendientes por ver"
        )
        MovieFilter.WATCHED -> Triple(
            "🍿", "Nada marcado como visto",
            "Desliza a la derecha una película para marcarla como vista"
        )
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(emoji, style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(16.dp))
            Text(
                title, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                subtitle, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
