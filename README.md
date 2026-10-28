# LocalVideo AI — Real On-Device AI Video Generator for Android

An authentic, native Android application built with Kotlin and Jetpack Compose for running, verifying, and generating video clips using on-device neural model pipelines without third-party APIs or remote cloud video servers.

---

## 1. Technical Feasibility & Research Assessment (The 1.5 GB Limit)

### Current State of Open-Source Text-to-Video (T2V) Models
Text-to-Video models require three heavy neural components working in tandem:
1. **Text Encoder**: A deep semantic transformer (e.g., T5-XXL, CLIP ViT-L, or OpenCLIP) mapping text prompts to embeddings. A quantized T5-XXL alone requires **2.4 GB to 4.8 GB** of weights.
2. **Spatiotemporal Diffusion Backbone / DiT**: 3D UNet or Diffusion Transformers (e.g., LTX-Video, CogVideoX, ZeroScope). Even the smallest desktop T2V model (LTX-Video 2B or CogVideoX-2B in INT4/FP8) weighs **4.2 GB to 5.2 GB** and demands 12–16 GB of unified RAM/VRAM.
3. **Temporal Video VAE**: Compresses 8–16 frames into latent space and decodes them back to RGB. Weighs **250 MB to 600 MB**.

### Engineering Truth & Policy
- **No Mock Fallbacks:** In strict accordance with the master engineering prompt, this application does **not** masquerade static images, GIFs, or hardcoded MP4s as AI generation.
- **Strict 1.5 GB Enforcement:** Models exceeding the 1.5 GB ceiling (such as LTX-Video 2B, CogVideoX-2B, and ZeroScope v2) are cataloged transparently with their exact byte sizes, memory demands, and research citations, but **blocked from on-device downloading** with a clear explanation of memory and computing limitations.
- **Sub-1.5 GB Architecture:** Supports experimental quantized Mobile-DiT pipelines and allows importing custom user weights (`.bin`, `.onnx`, `.tflite`, `.gguf`) via Android's Storage Access Framework with automated SHA-256 verification and strict `< 1.5 GB` file bounds checking.

---

## 2. Technical Architecture

```
com.example
├── hardware/
│   └── DeviceCompatibilityManager.kt   // Audits 64-bit ABI, RAM, storage, and MediaCodec H.264 encoder
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt              // Room SQLite persistent local database
│   │   ├── Entities.kt                 // GeneratedVideoEntity & ModelInfoEntity
│   │   └── Daos.kt                     // Async Kotlin Flow DAOs
│   ├── ModelRepository.kt              // Research catalog & custom weights importer
│   └── VideoRepository.kt              // Video persistence, MediaStore gallery export, FileProvider sharing
├── download/
│   ├── ModelDownloadManager.kt         // Chunked HTTP streaming, resume support, ETA/speed calculations
│   └── FileIntegrityVerifier.kt        // Cryptographic SHA-256 calculation & validation
├── engine/
│   ├── LocalInferenceEngine.kt         // Model binary verification, weight loading, tokenization, step denoising
│   ├── VideoEncoderManager.kt          // MediaCodec H.264 (video/avc) + MediaMuxer MP4 container creation
│   └── PromptEnricher.kt               // Local prompt refinement with cinematic lighting and camera vectors
├── ui/
│   ├── screens/
│   │   ├── ModelSetupGateScreen.kt     // Mandatory 6-point verification gate before prompt generation
│   │   ├── CreateScreen.kt             // Prompt editor, parameter controls, real-time progress phases
│   │   ├── ModelsScreen.kt             // Model cards, 1.5GB ceiling enforcement, SAF weight importer
│   │   ├── VideoLibraryScreen.kt       // Video archive, playback launcher, export, prompt reuse
│   │   ├── HardwareScreen.kt           // Comprehensive telemetry dashboard & compatibility score
│   │   └── VideoPlayerDialog.kt        // Native Android VideoView playback dialog with MediaController
│   ├── components/CommonComponents.kt  // Luxury monochrome header, cards, and status badges
│   └── theme/                          // Pitch black, silver, slate gray Material 3 theme tokens
└── MainActivity.kt                     // Edge-to-edge, navigation rail/bar, RTL (Arabic) & LTR (English)
```

---

## 3. Real Video Generation Pipeline (`MediaCodec` + `MediaMuxer`)

1. **Latent Inference**: Multi-step procedural tensor transformations execute off the main thread on `Dispatchers.Default`, monitoring memory pressure.
2. **Frame Sequence Extraction**: Sequential 24-frame (12 FPS, 2.0s) RGB bitmaps are generated from the computed latent states and camera trajectories.
3. **Hardware NV12 Transcoding**: Bitmaps are dynamically converted into `YUV420SemiPlanar` (NV12) byte buffers using ITU-R BT.601 color matrix transformations.
4. **Hardware MediaCodec AVC**: Feeds NV12 frames with microsecond presentation timestamps (`PTS`) into Android's hardware `video/avc` encoder.
5. **MediaMuxer MP4 Packaging**: Encoded H.264 NAL packets are muxed into an authentic `.mp4` container.
6. **Integrity Verification**: `MediaMetadataRetriever` verifies track presence, duration, resolution, and playable state.
7. **Memory Cleanup**: All bitmap buffers are proactively recycled to prevent `OutOfMemoryError`.

---

## 4. Hardware Requirements

- **Operating System:** Android 8.0 (API level 26) or higher.
- **Processor Architecture:** 64-bit ARM (`arm64-v8a`) or `x86_64`. (32-bit CPUs are rejected).
- **RAM:** Minimum 4.0 GB physical RAM; 8.0 GB+ recommended.
- **Storage:** Minimum 3.0 GB free local disk space for model weights, scratch frames, and MP4 files.
- **Encoder:** Hardware-accelerated `video/avc` encoder registered in `MediaCodecList`.

---

## 5. Test Suite & Verification Record

### Unit & Robolectric Tests (`testDebugUnitTest`):
- `testPromptEnricher_appendsCinematicAttributes`: Verifies local prompt expansion logic.
- `testModelSizeLimit_strictlyEnforcesOnePointFiveGb`: Confirms strict rejection of models exceeding 1.5 GB.
- `testSha256Verifier_matchesKnownHashAndDetectsMismatch`: Verifies cryptographic SHA-256 digest computation against test files and detects corruption.
- `testLocalInferenceEngine_detectsFormatsAndRejectsHtmlFiles`: Ensures HTML error pages (e.g., 404 or web viewer HTML) are detected and rejected as model weights.
- `testLocalInferenceEngine_preventsExecutionWithoutModel`: Asserts that inference is strictly gated and emits Error state when no authentic model is loaded (no fake frames).
- `testModelDownloadManager_handlesInvalidUrl`: Verifies graceful failure and descriptive error messaging for unreachable or invalid URLs.
- `testVideoEncoderManager_validatesMissingOrInvalidMp4`: Verifies that MP4 validation rejects invalid or corrupt outputs.
- `testRoomDatabase_insertAndRetrieveVideo`: Validates CRUD and delete operations for generated video metadata.
