package com.tuusuario.iptv.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.tuusuario.iptv.data.AppSettingsEntity
import com.tuusuario.iptv.data.ChannelEntity
import com.tuusuario.iptv.data.EpgProgram
import com.tuusuario.iptv.data.IptvDatabase
import com.tuusuario.iptv.parser.M3uParser
import com.tuusuario.iptv.parser.XmlTvParser
import kotlinx.coroutines.launch

@Composable
@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
fun IptvScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val db = remember { IptvDatabase.getInstance(context) }
    val dao = remember { db.iptvDao() }

    val savedChannels by dao.observeChannels().collectAsState(initial = emptyList())

    var playlistUrl by remember { mutableStateOf("") }
    var epgUrl by remember { mutableStateOf("") }
    val epgPrograms = remember { mutableStateOf(emptyMap<String, EpgProgram>()) }

    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("TODOS") }
    var onlyFavorites by remember { mutableStateOf(false) }
    var darkTheme by remember { mutableStateOf(true) }

    var showAddListDialog by remember { mutableStateOf(false) }
    var showListsDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    var activeChannel by remember { mutableStateOf<ChannelEntity?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }

    val headerBlue = Color(0xFF0277BD)
    val darkHeaderBlue = Color(0xFF01579B)

    val demoChannels = remember {
        listOf(
            ChannelEntity(
                id = "demo-hls",
                name = "Big Buck Bunny HLS",
                url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                groupName = "CANALES HD",
                tvgId = "bbb.demo"
            ),
            ChannelEntity(
                id = "demo-dash",
                name = "Tears of Steel Demo",
                url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                groupName = "CINE Y SERIES",
                tvgId = "tos.demo"
            )
        )
    }

    val baseChannels: List<ChannelEntity> =
        if (savedChannels.isNotEmpty()) savedChannels else demoChannels

    val categories: List<String> = remember(baseChannels) {
        val dynamicGroups = baseChannels
            .map { it.groupName.trim().uppercase() }
            .filter { it.isNotBlank() && it != "GENERAL" }
            .distinct()
        val defaults = listOf(
            "TODOS",
            "CANALES HD",
            "CINE Y SERIES",
            "DEPORTES",
            "ANIME"
        )
        (defaults + dynamicGroups).distinct()
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    suspend fun reloadPlaylistAndEpg(m3u: String, epg: String) {
        isLoading = true
        statusMsg = null
        try {
            val cleanM3u = m3u.trim()
            val cleanEpg = epg.trim()
            val parsed = M3uParser.fromUrl(cleanM3u)
            if (parsed.isEmpty()) {
                statusMsg = "Lista vacía o sin canales válidos."
            } else {
                val favIds = savedChannels
                    .filter { it.isFavorite }
                    .map { it.id }
                    .toSet()
                val merged = parsed.map { ch ->
                    ch.copy(isFavorite = ch.id in favIds)
                }
                dao.deleteNonFavorites()
                dao.insertChannels(merged)
                dao.saveSettings(
                    AppSettingsEntity(
                        lastPlaylistUrl = cleanM3u,
                        lastEpgUrl = cleanEpg
                    )
                )
                if (cleanEpg.isNotBlank()) {
                    epgPrograms.value = XmlTvParser.fetchCurrentPrograms(cleanEpg)
                }
                statusMsg = "Cargados ${merged.size} canales."
            }
        } catch (e: Exception) {
            statusMsg = "Error: ${e.localizedMessage ?: "revisa la URL"}"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        val settings = dao.getSettings()
        if (settings != null) {
            playlistUrl = settings.lastPlaylistUrl
            epgUrl = settings.lastEpgUrl
            if (settings.lastEpgUrl.isNotBlank()) {
                runCatching {
                    epgPrograms.value = XmlTvParser.fetchCurrentPrograms(settings.lastEpgUrl)
                }
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

    BackHandler(enabled = isFullscreen || showSearchBar) {
        if (isFullscreen) {
            isFullscreen = false
        } else if (showSearchBar) {
            showSearchBar = false
            searchQuery = ""
        }
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

    val filteredChannels: List<ChannelEntity> = remember(
        baseChannels,
        selectedTab,
        onlyFavorites,
        searchQuery
    ) {
        baseChannels.filter { ch ->
            val matchesFav = !onlyFavorites || ch.isFavorite
            val matchesGroup = selectedTab == "TODOS" ||
                ch.groupName.equals(selectedTab, ignoreCase = true) ||
                ch.groupName.contains(selectedTab, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                ch.name.contains(searchQuery, ignoreCase = true) ||
                ch.groupName.contains(searchQuery, ignoreCase = true)
            matchesFav && matchesGroup && matchesSearch
        }
    }

    if (isFullscreen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
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
                Icon(
                    imageVector = Icons.Default.FullscreenExit,
                    contentDescription = "Salir de pantalla completa",
                    tint = Color.White
                )
            }
        }
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(290.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(darkHeaderBlue)
                        .padding(horizontal = 20.dp, vertical = 28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "NOVA IPTV",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Reproductor sin anuncios",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                NavigationDrawerItem(
                    label = { Text("Inicio / Canales") },
                    selected = !onlyFavorites && selectedTab == "TODOS",
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    onClick = {
                        onlyFavorites = false
                        selectedTab = "TODOS"
                        scope.launch { drawerState.close() }
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Favoritos") },
                    selected = onlyFavorites,
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                    onClick = {
                        onlyFavorites = !onlyFavorites
                        scope.launch { drawerState.close() }
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Agregar lista") },
                    selected = false,
                    icon = { Icon(Icons.Default.AddBox, contentDescription = null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        showAddListDialog = true
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Ver listas") },
                    selected = false,
                    icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        showListsDialog = true
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Eliminar listas") },
                    selected = false,
                    icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        showDeleteDialog = true
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Modo oscuro",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = darkTheme,
                        onCheckedChange = { darkTheme = it }
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = headerBlue,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menú")
                        }
                    },
                    title = {
                        Text(
                            text = if (onlyFavorites) "Favoritos" else "Nova IPTV",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (playlistUrl.isNotBlank()) {
                                    scope.launch { reloadPlaylistAndEpg(playlistUrl, epgUrl) }
                                } else {
                                    showAddListDialog = true
                                }
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Actualizar lista")
                        }
                        IconButton(
                            onClick = {
                                showSearchBar = !showSearchBar
                                if (!showSearchBar) searchQuery = ""
                            }
                        ) {
                            Icon(
                                imageVector = if (showSearchBar) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Buscar"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (activeChannel != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .background(Color.Black)
                    ) {
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
                        val ch = activeChannel!!
                        val prog: EpgProgram? = ch.tvgId?.let { id -> epgPrograms.value[id] }
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .fillMaxWidth(0.75f)
                        ) {
                            Text(
                                text = ch.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (prog != null && prog.title.isNotBlank()) {
                                Text(
                                    text = "Ahora: ${prog.title}",
                                    color = Color.White.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(
                            onClick = { isFullscreen = true },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Pantalla completa",
                                tint = Color.White
                            )
                        }
                    }
                }

                if (showSearchBar) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar canal o categoría...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        val isChipSelected = selectedTab == category && !onlyFavorites
                        Surface(
                            modifier = Modifier.clickable {
                                onlyFavorites = false
                                selectedTab = category
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isChipSelected) headerBlue else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isChipSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Text(
                                text = category,
                                maxLines = 1,
                                softWrap = false,
                                fontWeight = if (isChipSelected) FontWeight.Bold else FontWeight.Medium,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                val currentStatus = statusMsg
                if (currentStatus != null) {
                    Text(
                        text = currentStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = headerBlue,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
                    )
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredChannels, key = { it.id }) { channel ->
                        val isSelected = activeChannel?.id == channel.id
                        val nowProg: EpgProgram? = channel.tvgId?.let { id -> epgPrograms.value[id] }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { activeChannel = channel },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 10f)
                                        .background(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LiveTv,
                                        contentDescription = null,
                                        tint = headerBlue,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .align(Alignment.Center)
                                    )
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                dao.setFavorite(channel.id, !channel.isFavorite)
                                            }
                                        },
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Icon(
                                            imageVector = if (channel.isFavorite) {
                                                Icons.Default.Favorite
                                            } else {
                                                Icons.Default.FavoriteBorder
                                            },
                                            contentDescription = "Favorito",
                                            tint = if (channel.isFavorite) Color.Red else Color.Gray
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = channel.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                val subtitle = if (nowProg != null && nowProg.title.isNotBlank()) {
                                    nowProg.title
                                } else {
                                    channel.groupName
                                }

                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddListDialog) {
        AlertDialog(
            onDismissRequest = { showAddListDialog = false },
            title = { Text("Agregar lista M3U / EPG") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = playlistUrl,
                        onValueChange = { playlistUrl = it },
                        placeholder = { Text("URL lista .m3u / .m3u8") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = epgUrl,
                        onValueChange = { epgUrl = it },
                        placeholder = { Text("URL guía EPG XMLTV (.xml)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !isLoading && playlistUrl.isNotBlank(),
                    onClick = {
                        showAddListDialog = false
                        scope.launch { reloadPlaylistAndEpg(playlistUrl, epgUrl) }
                    }
                ) {
                    Text(if (isLoading) "..." else "Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddListDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showListsDialog) {
        AlertDialog(
            onDismissRequest = { showListsDialog = false },
            title = { Text("Listas configuradas") },
            text = {
                Text(
                    text = if (playlistUrl.isBlank()) {
                        "No hay lista remota guardada aún. Usa 'Agregar lista' para cargar tu enlace M3U."
                    } else {
                        "Lista activa:\n$playlistUrl"
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { showListsDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar lista") },
            text = {
                Text("¿Deseas borrar la lista guardada y la URL actual?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        scope.launch {
                            dao.deleteNonFavorites()
                            playlistUrl = ""
                            epgUrl = ""
                            dao.saveSettings(AppSettingsEntity(lastPlaylistUrl = "", lastEpgUrl = ""))
                            statusMsg = "Lista eliminada."
                        }
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
