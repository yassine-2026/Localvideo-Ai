package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ModelRepository
import com.example.data.VideoRepository
import com.example.data.db.GeneratedVideoEntity
import com.example.data.db.ModelInfoEntity
import com.example.download.DownloadProgress
import com.example.download.FileIntegrityVerifier
import com.example.download.ModelDownloadManager
import com.example.engine.DefaultLocalVideoGenerationEngine
import com.example.engine.GenerationConfig
import com.example.engine.GenerationPhase
import com.example.engine.LocalVideoGenerationEngine
import com.example.engine.PromptEnricher
import com.example.hardware.CompatibilityReport
import com.example.hardware.DeviceCompatibilityManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class GeneratorUiState(
    val prompt: String = "",
    val durationSeconds: Double = 2.0,
    val resolutionWidth: Int = 256,
    val resolutionHeight: Int = 256,
    val fps: Int = 12,
    val aspectRatio: String = "1:1",
    val selectedModel: ModelInfoEntity? = null,
    val isGenerating: Boolean = false,
    val currentPhase: GenerationPhase? = null,
    val lastGeneratedVideo: GeneratedVideoEntity? = null,
    val userNotice: String? = null,
    val currentLocale: String = "en" // "en" or "ar"
)

class VideoGeneratorViewModel(application: Application) : AndroidViewModel(application) {

    private val modelRepo = ModelRepository(application)
    private val videoRepo = VideoRepository(application)
    private val downloadManager = ModelDownloadManager(application)
    private val generationEngine: LocalVideoGenerationEngine = DefaultLocalVideoGenerationEngine()

    val compatibilityReport: StateFlow<CompatibilityReport> = MutableStateFlow(
        DeviceCompatibilityManager.checkCompatibility(application)
    ).asStateFlow()

    val modelsList: StateFlow<List<ModelInfoEntity>> = modelRepo.allModels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videosList: StateFlow<List<GeneratedVideoEntity>> = videoRepo.allVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadState: StateFlow<DownloadProgress> = downloadManager.downloadState

    private val _uiState = MutableStateFlow(GeneratorUiState())
    val uiState: StateFlow<GeneratorUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<String>()
    val eventFlow: SharedFlow<String> = _eventFlow.asSharedFlow()

    init {
        viewModelScope.launch {
            modelRepo.initializeCatalogIfEmpty()
            val active = modelRepo.getActiveModel()
            if (active != null) {
                _uiState.value = _uiState.value.copy(selectedModel = active)
            }
        }
    }

    fun updatePrompt(newPrompt: String) {
        _uiState.value = _uiState.value.copy(prompt = newPrompt)
    }

    fun enrichPromptLocally() {
        val current = _uiState.value.prompt
        if (current.isNotBlank()) {
            val enriched = PromptEnricher.enrichPrompt(current)
            _uiState.value = _uiState.value.copy(prompt = enriched)
        }
    }

    fun updateConfig(
        duration: Double = _uiState.value.durationSeconds,
        width: Int = _uiState.value.resolutionWidth,
        height: Int = _uiState.value.resolutionHeight,
        fps: Int = _uiState.value.fps,
        aspectRatio: String = _uiState.value.aspectRatio
    ) {
        _uiState.value = _uiState.value.copy(
            durationSeconds = duration,
            resolutionWidth = width,
            resolutionHeight = height,
            fps = fps,
            aspectRatio = aspectRatio
        )
    }

    fun selectModel(model: ModelInfoEntity) {
        _uiState.value = _uiState.value.copy(selectedModel = model)
    }

    fun toggleLanguage() {
        val next = if (_uiState.value.currentLocale == "en") "ar" else "en"
        _uiState.value = _uiState.value.copy(currentLocale = next)
    }

    fun startVideoGeneration() {
        val state = _uiState.value
        val model = state.selectedModel

        if (model == null) {
            viewModelScope.launch { _eventFlow.emit("Please select a verified model first.") }
            return
        }

        if (model.status == "EXCEEDS_LIMIT") {
            viewModelScope.launch {
                _eventFlow.emit("Model ${model.name} exceeds the 1.5 GB mobile limit and cannot be run locally.")
            }
            return
        }

        if (state.prompt.trim().length < 3) {
            viewModelScope.launch { _eventFlow.emit("Please enter a descriptive prompt.") }
            return
        }

        val config = GenerationConfig(
            prompt = state.prompt.trim(),
            durationSeconds = state.durationSeconds,
            fps = state.fps,
            resolutionWidth = state.resolutionWidth,
            resolutionHeight = state.resolutionHeight
        )

        _uiState.value = state.copy(isGenerating = true, currentPhase = GenerationPhase.Validating("Initializing pipeline…"))

        viewModelScope.launch {
            generationEngine.generateVideo(getApplication(), model, config).collect { phase ->
                _uiState.value = _uiState.value.copy(currentPhase = phase)

                when (phase) {
                    is GenerationPhase.Success -> {
                        // Persist into database
                        val saved = videoRepo.saveGeneratedVideo(
                            title = config.prompt.take(40),
                            prompt = config.prompt,
                            modelName = model.name,
                            modelVersion = model.version,
                            resolution = "${config.resolutionWidth}x${config.resolutionHeight}",
                            durationSeconds = phase.durationSec,
                            fps = config.fps,
                            frameCount = phase.frameCount,
                            file = phase.videoFile
                        )
                        _uiState.value = _uiState.value.copy(
                            isGenerating = false,
                            lastGeneratedVideo = saved
                        )
                        _eventFlow.emit("Video generated successfully!")
                    }
                    is GenerationPhase.Failure -> {
                        _uiState.value = _uiState.value.copy(isGenerating = false)
                        _eventFlow.emit("Generation stopped: ${phase.error}")
                    }
                    else -> {}
                }
            }
        }
    }

    fun cancelGeneration() {
        generationEngine.cancelActiveGeneration()
        _uiState.value = _uiState.value.copy(isGenerating = false)
    }

    fun downloadModel(model: ModelInfoEntity) {
        if (model.status == "EXCEEDS_LIMIT") {
            viewModelScope.launch {
                _eventFlow.emit("Cannot download: Model ${model.name} exceeds the 1.5 GB ceiling.")
            }
            return
        }

        viewModelScope.launch {
            val fileName = "${model.id}.bin"
            val result = downloadManager.startDownload(
                modelId = model.id,
                urlString = model.downloadUrl,
                fileName = fileName,
                expectedSizeBytes = model.sizeBytes,
                expectedSha256 = model.expectedSha256
            )

            if (result.isSuccess) {
                val file = result.getOrThrow()
                val updated = model.copy(
                    status = "READY",
                    localFilePath = file.absolutePath,
                    downloadedBytes = file.length()
                )
                modelRepo.initializeCatalogIfEmpty()
                _eventFlow.emit("Model downloaded and verified!")
            } else {
                _eventFlow.emit("Download failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun pauseDownload() {
        downloadManager.pauseDownload()
    }

    fun cancelDownload() {
        downloadManager.cancelDownload()
    }

    fun deleteModel(model: ModelInfoEntity) {
        viewModelScope.launch {
            modelRepo.deleteModel(model)
            _eventFlow.emit("Model deleted.")
        }
    }

    fun importCustomModel(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw Exception("Could not open file stream")

                val tempFile = File(context.cacheDir, "imported_weights_${System.currentTimeMillis()}.bin")
                FileOutputStream(tempFile).use { out ->
                    inputStream.copyTo(out)
                }

                // Check size limit: 1.5 GB
                val maxAllowed = 1_610_612_736L
                if (tempFile.length() > maxAllowed) {
                    tempFile.delete()
                    _eventFlow.emit("Error: File size (${tempFile.length() / (1024 * 1024)} MB) strictly exceeds the 1.5 GB maximum limit.")
                    return@launch
                }

                val sha256 = FileIntegrityVerifier.calculateSha256(tempFile)
                val destFile = File(downloadManager.getModelsDir(), tempFile.name)
                tempFile.renameTo(destFile)

                val result = modelRepo.registerCustomImport(
                    name = "Imported Weights",
                    file = destFile,
                    sha256 = sha256
                )

                if (result.isSuccess) {
                    val imported = result.getOrThrow()
                    _uiState.value = _uiState.value.copy(selectedModel = imported)
                    _eventFlow.emit("Custom weights imported and verified successfully!")
                } else {
                    _eventFlow.emit("Import failed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _eventFlow.emit("Import error: ${e.message}")
            }
        }
    }

    fun deleteVideo(video: GeneratedVideoEntity) {
        viewModelScope.launch {
            videoRepo.deleteVideo(video)
            _eventFlow.emit("Video deleted.")
        }
    }

    fun exportVideo(video: GeneratedVideoEntity) {
        viewModelScope.launch {
            val result = videoRepo.exportVideoToGallery(video)
            if (result.isSuccess) {
                _eventFlow.emit("Saved to device Movies/LocalVideoAI folder!")
            } else {
                _eventFlow.emit("Export failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun getShareIntent(video: GeneratedVideoEntity) = videoRepo.createShareIntent(video)
}
