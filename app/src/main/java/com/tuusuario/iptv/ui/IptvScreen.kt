package com.tuusuario.iptv.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.tuusuario.iptv.data.*
import com.tuusuario.iptv.parser.M3uParser
import com.tuusuario.iptv.parser.XmlTvParser
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun IptvScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val db = remember { IptvDatabase.getInstance(context) }
    val dao = remember { db.iptvDao() }

    val savedChannels by dao.observeChannels().collectAsState(initial = emptyList())

    var playlistUrl by remember { mutableStateOf("") }
    var epgUrl by remember { mutableStateOf("") }
    val epgMapState = remember { mutableStateOf>(emptyMap()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    var activeChannel by remember { mutableStateOf<ChannelEntity?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }

    val demoChannels = remember {
        listOf(
            ChannelEntity(
                id = "demo-hls",
                name = "Big Buck Bunny (Demo HLS)",
                url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                groupName = "Demos",
                tvgId = "bbb.demo"
            )
        )
    }

    val displayedList = if (savedChannels.isNotEmpty()) savedChannels else demoChannels

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    LaunchedEffect(Unit) {
        dao.getSettings()?.let { settings ->
            playlistUrl = settings.lastPlaylistUrl
            epgUrl = settings.lastEpgUrl
            if (settings.lastEpgUrl.isNotBlank()) {
                runCatching { epgMapState.value = XmlTvParser.fetchCurrentPrograms(settings.lastEpgUrl) }
            }
        }
    }

    LaunchedEffect(isFullscreen) {
        activity?.let { act ->
            val window = act.window
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (isFullscreen) {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            exoPlayer.release()
        }
    }

    LaunchedEffect(activeChannel?.url) {
        activeChannel?.let { ch ->
            exoPlayer.setMediaItem(MediaItem.fromUri(ch.url))
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    val filteredChannels = remember(displayedList, searchQuery) {
        if (searchQuery.isBlank()) displayedList
        else displayedList.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.groupName.contains(searchQuery, ignoreCase = true)
        }
    }

    if (isFullscreen) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = { isFullscreen = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.FullscreenExit, contentDescription = "Salir de pantalla completa")
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(12.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 4.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                activeChannel?.let { ch ->
                    val currentProg = ch.tvgId?.let { epgMapState.value[it]?.title }
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .fillMaxWidth(0.8f)
                    ) {
                        Text(
                            text = ch.name,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!currentProg.isNullOrBlank()) {
                            Text(
                                text = "Ahora: $currentProg",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                IconButton(
                    onClick = { isFullscreen = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Icon(Icons.Default.Fullscreen, contentDescription = "Pantalla completa")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = playlistUrl,
                onValueChange = { playlistUrl = it },
                placeholder = { Text("URL lista .m3u / .m3u8") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                enabled = !isLoading && playlistUrl.isNotBlank(),
                onClick = {
                    scope.launch {
                        isLoading = true
                        statusMsg = null
                        try {
                            val cleanM3u = playlistUrl.trim()
                            val cleanEpg = epgUrl.trim()
                            val parsed = M3uParser.fromUrl(cleanM3u)
                            if (parsed.isEmpty()) {
                                statusMsg = "Lista vacía o sin canales válidos."
                            } else {
                                val favIds = savedChannels.filter { it.isFavorite }.map { it.id }.toSet()
                                val merged = parsed.map { it.copy(isFavorite = it.id in favIds) }
                                dao.deleteNonFavorites()
                                dao.insertChannels(merged)
                                dao.saveSettings(
                                    AppSettingsEntity(
                                        lastPlaylistUrl = cleanM3u,
                                        lastEpgUrl = cleanEpg
                                    )
                                )
                                if (cleanEpg.isNotBlank()) {
                                    epgMapState.value = XmlTvParser.fetchCurrentPrograms(cleanEpg)
                                }
                                activeChannel = merged.first()
                                statusMsg = "Guardados ${merged.size} canales en Room."
                            }
                        } catch (e: Exception) {
                            statusMsg = "Error: ${e.localizedMessage ?: "revisa la URL"}"
                        } finally {
                            isLoading = false
                        }
                    }
                }
            ) {
                Text(if (isLoading) "..." else "Guardar")
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = epgUrl,
            onValueChange = { epgUrl = it },
            placeholder = { Text("URL guía EPG XMLTV (.xml)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (statusMsg != null) {
            Text(
                text = statusMsg!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar canal o categoría...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filteredChannels, key = { it.id }) { channel ->
                val isSelected = activeChannel?.id == channel.id
                val nowProgram = channel.tvgId?.let { epgMapState.value[it]?.title }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activeChannel = channel },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = channel.name,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (!nowProgram.isNullOrBlank()) {
                                    "${channel.groupName} • $nowProgram"
                                } else {
                                    channel.groupName
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = {
                            scope.launch {
                                dao.setFavorite(channel.id, !channel.isFavorite)
                            }
                        }) {
                            Icon(
                                imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorito"
                            )
                        }
                    }
                }
            }
        }
    }
}
