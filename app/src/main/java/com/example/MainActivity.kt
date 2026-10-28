package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.GeneratedVideoEntity
import com.example.ui.components.AppHeader
import com.example.ui.screens.CreateScreen
import com.example.ui.screens.HardwareScreen
import com.example.ui.screens.ModelSetupGateScreen
import com.example.ui.screens.ModelsScreen
import com.example.ui.screens.VideoLibraryScreen
import com.example.ui.screens.VideoPlayerDialog
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.DarkMutedText
import com.example.ui.theme.LocalVideoTheme
import com.example.ui.theme.OnyxBackground
import com.example.ui.theme.PlatinumText
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SilverMutedText
import com.example.ui.theme.SlateCardBorder
import com.example.viewmodel.VideoGeneratorViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class ScreenTab {
    GATE,
    CREATE,
    MODELS,
    VIDEOS,
    HARDWARE
}

class MainActivity : ComponentActivity() {

    private val viewModel: VideoGeneratorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LocalVideoTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val compatibility by viewModel.compatibilityReport.collectAsStateWithLifecycle()
                val models by viewModel.modelsList.collectAsStateWithLifecycle()
                val videos by viewModel.videosList.collectAsStateWithLifecycle()
                val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()

                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                
                // On launch, if model is not ready, start at GATE screen
                var currentTab by remember {
                    mutableStateOf(if (uiState.isEngineInitialized) ScreenTab.CREATE else ScreenTab.GATE)
                }
                var activePlayingVideo by remember { mutableStateOf<GeneratedVideoEntity?>(null) }

                LaunchedEffect(Unit) {
                    viewModel.eventFlow.collectLatest { message ->
                        scope.launch {
                            snackbarHostState.showSnackbar(message)
                        }
                    }
                }

                val layoutDirection = if (uiState.currentLocale == "ar") {
                    LayoutDirection.Rtl
                } else {
                    LayoutDirection.Ltr
                }

                BackHandler(enabled = currentTab != ScreenTab.GATE || activePlayingVideo != null) {
                    if (activePlayingVideo != null) {
                        activePlayingVideo = null
                    } else {
                        currentTab = ScreenTab.GATE
                    }
                }

                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(OnyxBackground),
                        containerColor = OnyxBackground,
                        topBar = {
                            AppHeader(
                                currentLocale = uiState.currentLocale,
                                onToggleLanguage = { viewModel.toggleLanguage() },
                                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                modifier = Modifier
                                    .border(1.dp, SlateCardBorder)
                                    .windowInsetsPadding(WindowInsets.navigationBars),
                                containerColor = CharcoalSurface,
                                contentColor = PlatinumText
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == ScreenTab.GATE,
                                    onClick = { currentTab = ScreenTab.GATE },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Gate",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = if (uiState.currentLocale == "ar") "البوابة" else "Gate",
                                            fontSize = 11.sp,
                                            fontWeight = if (currentTab == ScreenTab.GATE) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = OnyxBackground,
                                        selectedTextColor = PureWhite,
                                        indicatorColor = PureWhite,
                                        unselectedIconColor = SilverMutedText,
                                        unselectedTextColor = DarkMutedText
                                    ),
                                    modifier = Modifier.testTag("nav_tab_gate")
                                )

                                NavigationBarItem(
                                    selected = currentTab == ScreenTab.CREATE,
                                    onClick = { currentTab = ScreenTab.CREATE },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Videocam,
                                            contentDescription = "Create",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = if (uiState.currentLocale == "ar") "الإنشاء" else "Create",
                                            fontSize = 11.sp,
                                            fontWeight = if (currentTab == ScreenTab.CREATE) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = OnyxBackground,
                                        selectedTextColor = PureWhite,
                                        indicatorColor = PureWhite,
                                        unselectedIconColor = SilverMutedText,
                                        unselectedTextColor = DarkMutedText
                                    ),
                                    modifier = Modifier.testTag("nav_tab_create")
                                )

                                NavigationBarItem(
                                    selected = currentTab == ScreenTab.MODELS,
                                    onClick = { currentTab = ScreenTab.MODELS },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Layers,
                                            contentDescription = "Models",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = if (uiState.currentLocale == "ar") "النماذج" else "Models",
                                            fontSize = 11.sp,
                                            fontWeight = if (currentTab == ScreenTab.MODELS) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = OnyxBackground,
                                        selectedTextColor = PureWhite,
                                        indicatorColor = PureWhite,
                                        unselectedIconColor = SilverMutedText,
                                        unselectedTextColor = DarkMutedText
                                    ),
                                    modifier = Modifier.testTag("nav_tab_models")
                                )

                                NavigationBarItem(
                                    selected = currentTab == ScreenTab.VIDEOS,
                                    onClick = { currentTab = ScreenTab.VIDEOS },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Movie,
                                            contentDescription = "Videos",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = if (uiState.currentLocale == "ar") "فيديوهاتي" else "My Videos",
                                            fontSize = 11.sp,
                                            fontWeight = if (currentTab == ScreenTab.VIDEOS) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = OnyxBackground,
                                        selectedTextColor = PureWhite,
                                        indicatorColor = PureWhite,
                                        unselectedIconColor = SilverMutedText,
                                        unselectedTextColor = DarkMutedText
                                    ),
                                    modifier = Modifier.testTag("nav_tab_videos")
                                )

                                NavigationBarItem(
                                    selected = currentTab == ScreenTab.HARDWARE,
                                    onClick = { currentTab = ScreenTab.HARDWARE },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Memory,
                                            contentDescription = "Hardware",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = if (uiState.currentLocale == "ar") "العتاد" else "Hardware",
                                            fontSize = 11.sp,
                                            fontWeight = if (currentTab == ScreenTab.HARDWARE) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = OnyxBackground,
                                        selectedTextColor = PureWhite,
                                        indicatorColor = PureWhite,
                                        unselectedIconColor = SilverMutedText,
                                        unselectedTextColor = DarkMutedText
                                    ),
                                    modifier = Modifier.testTag("nav_tab_hardware")
                                )
                            }
                        },
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentTab) {
                                ScreenTab.GATE -> {
                                    ModelSetupGateScreen(
                                        activeModel = uiState.selectedModel ?: models.firstOrNull { it.id == "microvideo-quantized" },
                                        isEngineInitialized = uiState.isEngineInitialized,
                                        downloadState = downloadState,
                                        compatibility = compatibility,
                                        currentLocale = uiState.currentLocale,
                                        onDownloadModel = { viewModel.downloadModel(it) },
                                        onPauseDownload = { viewModel.pauseDownload() },
                                        onCancelDownload = { viewModel.cancelDownload() },
                                        onImportWeights = { viewModel.importCustomModel(it) },
                                        onInitializeEngine = { viewModel.initializeEngine(it) },
                                        onProceedToCreate = { currentTab = ScreenTab.CREATE }
                                    )
                                }
                                ScreenTab.CREATE -> {
                                    CreateScreen(
                                        uiState = uiState,
                                        compatibility = compatibility,
                                        onPromptChange = { viewModel.updatePrompt(it) },
                                        onEnrichPrompt = { viewModel.enrichPromptLocally() },
                                        onUpdateConfig = { dur, w, h, f, r ->
                                            viewModel.updateConfig(dur, w, h, f, r)
                                        },
                                        onStartGeneration = { viewModel.startVideoGeneration() },
                                        onCancelGeneration = { viewModel.cancelGeneration() },
                                        onPlayVideo = { activePlayingVideo = it },
                                        onShareVideo = { video ->
                                            val intent = viewModel.getShareIntent(video)
                                            startActivity(Intent.createChooser(intent, "Share Video"))
                                        },
                                        onExportVideo = { viewModel.exportVideo(it) },
                                        onNavigateToGate = { currentTab = ScreenTab.GATE }
                                    )
                                }
                                ScreenTab.MODELS -> {
                                    ModelsScreen(
                                        models = models,
                                        selectedModel = uiState.selectedModel,
                                        downloadState = downloadState,
                                        currentLocale = uiState.currentLocale,
                                        onSelectModel = { viewModel.selectModel(it) },
                                        onDownloadModel = { viewModel.downloadModel(it) },
                                        onPauseDownload = { viewModel.pauseDownload() },
                                        onCancelDownload = { viewModel.cancelDownload() },
                                        onDeleteModel = { viewModel.deleteModel(it) },
                                        onImportCustomWeights = { viewModel.importCustomModel(it) }
                                    )
                                }
                                ScreenTab.VIDEOS -> {
                                    VideoLibraryScreen(
                                        videos = videos,
                                        currentLocale = uiState.currentLocale,
                                        onPlayVideo = { activePlayingVideo = it },
                                        onShareVideo = { video ->
                                            val intent = viewModel.getShareIntent(video)
                                            startActivity(Intent.createChooser(intent, "Share Video"))
                                        },
                                        onExportVideo = { viewModel.exportVideo(it) },
                                        onReusePrompt = { prompt ->
                                            viewModel.updatePrompt(prompt)
                                            currentTab = ScreenTab.CREATE
                                        },
                                        onDeleteVideo = { viewModel.deleteVideo(it) },
                                        onNavigateToCreate = { currentTab = ScreenTab.CREATE }
                                    )
                                }
                                ScreenTab.HARDWARE -> {
                                    HardwareScreen(
                                        report = compatibility,
                                        currentLocale = uiState.currentLocale
                                    )
                                }
                            }

                            activePlayingVideo?.let { playingVideo ->
                                VideoPlayerDialog(
                                    video = playingVideo,
                                    currentLocale = uiState.currentLocale,
                                    onDismiss = { activePlayingVideo = null },
                                    onShare = {
                                        val intent = viewModel.getShareIntent(playingVideo)
                                        startActivity(Intent.createChooser(intent, "Share Video"))
                                    },
                                    onExport = {
                                        viewModel.exportVideo(playingVideo)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
