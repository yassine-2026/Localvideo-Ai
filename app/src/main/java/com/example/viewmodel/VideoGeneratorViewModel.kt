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
import com.example.engine.DefaultLocalInferenceEngine
import com.example.engine.InferenceConfig
import com.example.engine.InferenceState
import com.example.engine.LocalInferenceEngine
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
    val isEngineInitialized: Boolean = false,
    val isGenerating: Boolean = false,
    val currentInferenceState: InferenceState? = null,
    val lastGeneratedVideo: GeneratedVideoEntity? = null,
    val currentLocale: String = "en"
)

class VideoGeneratorViewModel(application: Application) : AndroidViewModel(application) {

    private val modelRepo = ModelRepository(application)
    private val videoRepo = VideoRepository(application)
    private val downloadManager = ModelDownloadManager(application)
    val zeroScopeManager = com.example.data.ZeroScopeModelManager(application)
    val inferenceEngine: LocalInferenceEngine = DefaultLocalInferenceEngine()

    val zeroScopeState: StateFlow<com.example.data.ZeroScopeScreenState> = zeroScopeManager.state

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

    private val _importReport = MutableStateFlow<com.example.data.model.ImportInspectionReport?>(null)
    val importReport: StateFlow<com.example.data.model.ImportInspectionReport?> = _importReport.asStateFlow()

    private val _eventFlow = MutableSharedFlow<String>()
    val eventFlow: SharedFlow<String> = _eventFlow.asSharedFlow()

    init {
        viewModelScope.launch {
            modelRepo.initializeCatalogIfEmpty()
            val active = modelRepo.getActiveModel()
            if (active != null) {
                _uiState.value = _uiState.value.copy(selectedModel = active)
                initializeEngine(active)
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
        if (model.status == "READY" && !model.localFilePath.isNullOrBlank()) {
            initializeEngine(model)
        } else {
            _uiState.value = _uiState.value.copy(isEngineInitialized = false)
        }
    }

    fun initializeEngine(model: ModelInfoEntity) {
        viewModelScope.launch {
            val result = inferenceEngine.loadAndInitializeModel(model)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isEngineInitialized = true)
                _eventFlow.emit("LocalInferenceEngine initialized with ${model.name}")
            } else {
                _uiState.value = _uiState.value.copy(isEngineInitialized = false)
                _eventFlow.emit("Engine initialization failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun toggleLanguage() {
        val next = if (_uiState.value.currentLocale == "en") "ar" else "en"
        _uiState.value = _uiState.value.copy(currentLocale = next)
    }

    fun startVideoGeneration() {
        val state = _uiState.value
        val model = state.selectedModel

        if (model == null || !state.isEngineInitialized) {
            viewModelScope.launch {
                _eventFlow.emit("Model is not initialized. Please verify and load model first.")
            }
            return
        }

        if (state.prompt.trim().length < 3) {
            viewModelScope.launch { _eventFlow.emit("Please enter a descriptive prompt.") }
            return
        }

        val config = InferenceConfig(
            prompt = state.prompt.trim(),
            durationSeconds = state.durationSeconds,
            fps = state.fps,
            width = state.resolutionWidth,
            height = state.resolutionHeight
        )

        _uiState.value = state.copy(
            isGenerating = true,
            currentInferenceState = InferenceState.VerifyingModel("Auditing model binary…")
        )

        viewModelScope.launch {
            inferenceEngine.executeInference(getApplication(), config).collect { step ->
                _uiState.value = _uiState.value.copy(currentInferenceState = step)

                when (step) {
                    is InferenceState.Success -> {
                        val saved = videoRepo.saveGeneratedVideo(
                            title = config.prompt.take(40),
                            prompt = config.prompt,
                            modelName = model.name,
                            modelVersion = model.version,
                            resolution = "${config.width}x${config.height}",
                            durationSeconds = step.durationSec,
                            fps = config.fps,
                            frameCount = step.totalFrames,
                            file = step.outputFile
                        )
                        _uiState.value = _uiState.value.copy(
                            isGenerating = false,
                            lastGeneratedVideo = saved
                        )
                        _eventFlow.emit("Video generated and verified successfully!")
                    }
                    is InferenceState.Error -> {
                        _uiState.value = _uiState.value.copy(isGenerating = false)
                        _eventFlow.emit("Inference stopped: ${step.reason}")
                    }
                    else -> {}
                }
            }
        }
    }

    fun cancelGeneration() {
        inferenceEngine.cancelInference()
        _uiState.value = _uiState.value.copy(isGenerating = false)
    }

    fun downloadModel(model: ModelInfoEntity) {
        if (model.status == "EXCEEDS_LIMIT") {
            viewModelScope.launch {
                _eventFlow.emit("Model ${model.name} exceeds the 1.5 GB limit and cannot be downloaded.")
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
                modelRepo.updateModel(updated)
                _uiState.value = _uiState.value.copy(selectedModel = updated)
                initializeEngine(updated)
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
            if (_uiState.value.selectedModel?.id == model.id) {
                _uiState.value = _uiState.value.copy(selectedModel = null, isEngineInitialized = false)
            }
            _eventFlow.emit("Model deleted.")
        }
    }

    fun inspectImportUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val report = com.example.data.model.ModelImportInspector.inspectUris(getApplication(), uris)
        _importReport.value = report
    }

    fun dismissImportReport() {
        _importReport.value = null
    }

    fun confirmImport(report: com.example.data.model.ImportInspectionReport) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val modelsDir = downloadManager.getModelsDir()
                val result = com.example.data.model.ModelImportInspector.executeImport(context, report, modelsDir)

                if (result.isSuccess) {
                    val primaryFile = result.getOrThrow()
                    val sha256 = FileIntegrityVerifier.calculateSha256(primaryFile)
                    val modelName = primaryFile.parentFile?.name ?: primaryFile.nameWithoutExtension

                    val registerResult = modelRepo.registerCustomImport(
                        name = "Custom Bundle ($modelName)",
                        file = primaryFile,
                        sha256 = sha256
                    )

                    if (registerResult.isSuccess) {
                        val imported = registerResult.getOrThrow()
                        _uiState.value = _uiState.value.copy(selectedModel = imported)
                        initializeEngine(imported)
                        _importReport.value = null
                        _eventFlow.emit("تم استيراد حزمة النموذج والتحقق منها بنجاح!")
                    } else {
                        _eventFlow.emit("فشل تسجيل النموذج: ${registerResult.exceptionOrNull()?.message}")
                    }
                } else {
                    _eventFlow.emit("فشل الاستيراد: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _eventFlow.emit("خطأ أثناء الاستيراد: ${e.message}")
            }
        }
    }

    fun copyTextToClipboard(label: String, text: String) {
        try {
            val context = getApplication<Application>()
            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            viewModelScope.launch {
                _eventFlow.emit(if (_uiState.value.currentLocale == "ar") "تم نسخ الرابط المباشر إلى الحافظة" else "Direct link copied to clipboard")
            }
        } catch (e: Exception) {
            viewModelScope.launch {
                _eventFlow.emit("Failed to copy: ${e.message}")
            }
        }
    }

    fun importCustomModel(uri: Uri) {
        inspectImportUris(listOf(uri))
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

    fun selectZeroScopeSlot(slotId: com.example.data.ZeroScopeSlotId, uri: Uri) {
        viewModelScope.launch {
            zeroScopeManager.selectAndVerifySlot(slotId, uri)
        }
    }

    fun removeZeroScopeSlot(slotId: com.example.data.ZeroScopeSlotId) {
        zeroScopeManager.removeSlot(slotId)
    }

    fun activateZeroScope() {
        val result = zeroScopeManager.activateZeroScope()
        if (result.isActivated) {
            viewModelScope.launch {
                _eventFlow.emit("تم تفعيل ZeroScope v2 بنجاح")
            }
        }
    }

    fun dismissZeroScopeActivation() {
        zeroScopeManager.dismissActivationResult()
    }

    fun getShareIntent(video: GeneratedVideoEntity) = videoRepo.createShareIntent(video)
}
