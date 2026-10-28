package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.GeneratedVideoEntity
import com.example.engine.InferenceState
import com.example.hardware.CompatibilityReport
import com.example.ui.components.LuxuryCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.DarkMutedText
import com.example.ui.theme.GraphiteSurfaceVariant
import com.example.ui.theme.OnyxBackground
import com.example.ui.theme.PlatinumText
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SilverMutedText
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.viewmodel.GeneratorUiState

@Composable
fun CreateScreen(
    uiState: GeneratorUiState,
    compatibility: CompatibilityReport,
    onPromptChange: (String) -> Unit,
    onEnrichPrompt: () -> Unit,
    onUpdateConfig: (Double, Int, Int, Int, String) -> Unit,
    onStartGeneration: () -> Unit,
    onCancelGeneration: () -> Unit,
    onPlayVideo: (GeneratedVideoEntity) -> Unit,
    onShareVideo: (GeneratedVideoEntity) -> Unit,
    onExportVideo: (GeneratedVideoEntity) -> Unit,
    onNavigateToGate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val isAr = uiState.currentLocale == "ar"
    var showAdvancedSettings by remember { mutableStateOf(false) }

    val isModelReady = uiState.isEngineInitialized && uiState.selectedModel?.status == "READY"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Active Model Status Header Card
        LuxuryCard(modifier = Modifier.testTag("model_status_card")) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isAr) "محرك الاستدلال المحلي:" else "ON-DEVICE INFERENCE ENGINE:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SilverMutedText,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = uiState.selectedModel?.name ?: (if (isAr) "لم يتم تنزيل نموذج بعد" else "No Model Verified"),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    )
                    Text(
                        text = if (isModelReady) {
                            if (isAr) "المحرك محمل في الذاكرة وجاهز للتوليد" else "Loaded in memory & verified"
                        } else {
                            if (isAr) "مغلق: يتطلب تنزيل نموذج حقيقي وتهيئة المحرك" else "Gated: Setup & initialization required"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isModelReady) StatusGreen else SilverMutedText,
                            fontSize = 11.sp
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    StatusBadge(status = if (isModelReady) "READY" else (uiState.selectedModel?.status ?: "NOT_DOWNLOADED"))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isAr) "بوابة الفحص" else "Gate Setup",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = PureWhite,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GraphiteSurfaceVariant)
                            .clickable { onNavigateToGate() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Gate Warning if not initialized
        if (!isModelReady) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CharcoalSurface)
                    .border(1.dp, SlateCardBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isAr) "بوابة التحقق مفعلة" else "Setup Gate Enforced",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        )
                        Text(
                            text = if (isAr)
                                "يجب تنزيل نموذج حقيقي وتهيئة محرك الاستدلال قبل السماح بإنشاء الفيديو."
                            else
                                "You must download and initialize an authentic on-device model first.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SilverMutedText,
                                fontSize = 11.sp
                            )
                        )
                    }
                    Button(
                        onClick = onNavigateToGate,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PureWhite,
                            contentColor = OnyxBackground
                        )
                    ) {
                        Text(text = if (isAr) "فتح البوابة" else "Open Gate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Large Prompt Input Card
        LuxuryCard(modifier = Modifier.testTag("prompt_input_card")) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAr) "وصف المشهد (البرومبت)" else "SCENE DESCRIPTION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SilverMutedText,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "${uiState.prompt.length} chars",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = DarkMutedText,
                            fontSize = 11.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = uiState.prompt,
                    onValueChange = onPromptChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .testTag("prompt_text_field"),
                    placeholder = {
                        Text(
                            text = if (isAr)
                                "اكتب وصفًا سينمائيًا مفصلًا للمشهد (مثال: نسر فضي يحلق فوق قمم جبال مثلجة مع أشعة شمس ذهبية وانعكاسات درامية)…"
                            else
                                "Describe your video scene in detail (e.g. A chrome eagle soaring above mist-shrouded glacial peaks in cinematic sunset rays)…",
                            color = DarkMutedText,
                            fontSize = 13.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = GraphiteSurfaceVariant,
                        unfocusedContainerColor = GraphiteSurfaceVariant,
                        focusedBorderColor = PureWhite,
                        unfocusedBorderColor = SlateCardBorder,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PlatinumText
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Prompt Refiner Button (local enhancement)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(GraphiteSurfaceVariant)
                            .border(1.dp, SlateCardBorder, RoundedCornerShape(8.dp))
                            .clickable { onEnrichPrompt() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("enrich_prompt_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Enhance Prompt",
                            tint = PureWhite,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAr) "تحسين الصياغة محلياً" else "Refine Prompt (Local)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = PureWhite,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Text(
                        text = if (isAr) "*يحسن الصياغة فقط ولا يعدل الأوزان" else "*Refines text wording locally",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = DarkMutedText,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Video Configuration & Options (Duration, Resolution, FPS)
        LuxuryCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdvancedSettings = !showAdvancedSettings },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Settings",
                            tint = PureWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAr) "إعدادات التوليد والترميز" else "GENERATION PARAMETERS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = SilverMutedText,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    Text(
                        text = "${uiState.resolutionWidth}p • ${uiState.durationSeconds}s • ${uiState.fps} FPS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = PureWhite,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                if (showAdvancedSettings) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Duration Selector
                    Text(
                        text = if (isAr) "المدة الزمنية:" else "Duration:",
                        style = MaterialTheme.typography.bodySmall.copy(color = SilverMutedText)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1.5, 2.0, 3.0).forEach { dur ->
                            val selected = uiState.durationSeconds == dur
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) PureWhite else GraphiteSurfaceVariant)
                                    .border(1.dp, if (selected) PureWhite else SlateCardBorder, RoundedCornerShape(8.dp))
                                    .clickable { onUpdateConfig(dur, uiState.resolutionWidth, uiState.resolutionHeight, uiState.fps, uiState.aspectRatio) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${dur}s",
                                    color = if (selected) OnyxBackground else PlatinumText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Resolution Selector
                    Text(
                        text = if (isAr) "الدقة (متوافقة مع ذاكرة الهاتف):" else "Resolution (On-Device Memory Budget):",
                        style = MaterialTheme.typography.bodySmall.copy(color = SilverMutedText)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(256 to 256, 384 to 384).forEach { (w, h) ->
                            val selected = uiState.resolutionWidth == w
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) PureWhite else GraphiteSurfaceVariant)
                                    .border(1.dp, if (selected) PureWhite else SlateCardBorder, RoundedCornerShape(8.dp))
                                    .clickable { onUpdateConfig(uiState.durationSeconds, w, h, uiState.fps, uiState.aspectRatio) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${w}x${h}",
                                    color = if (selected) OnyxBackground else PlatinumText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // FPS Selector
                    Text(
                        text = if (isAr) "معدل الإطارات (FPS):" else "Frame Rate (FPS):",
                        style = MaterialTheme.typography.bodySmall.copy(color = SilverMutedText)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10, 12).forEach { f ->
                            val selected = uiState.fps == f
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) PureWhite else GraphiteSurfaceVariant)
                                    .border(1.dp, if (selected) PureWhite else SlateCardBorder, RoundedCornerShape(8.dp))
                                    .clickable { onUpdateConfig(uiState.durationSeconds, uiState.resolutionWidth, uiState.resolutionHeight, f, uiState.aspectRatio) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$f fps",
                                    color = if (selected) OnyxBackground else PlatinumText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hardware advisory notice
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.BatteryAlert,
                contentDescription = "Advisory",
                tint = DarkMutedText,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isAr)
                    "تنبيه: التوليد على المعالج يستهلك طاقة البطارية ويستغرق من 30 إلى 90 ثانية."
                else
                    "Advisory: On-device generation is compute-intensive and consumes battery power.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = DarkMutedText,
                    fontSize = 11.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Real-Time Progress Phase Card (when active)
        AnimatedVisibility(visible = uiState.isGenerating) {
            LuxuryCard(modifier = Modifier.padding(bottom = 16.dp)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isAr) "جارٍ الاستدلال العصبي الحقيقي على الهاتف" else "NEURAL INFERENCE RUNNING",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = StatusGreen,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(StatusRed.copy(alpha = 0.15f))
                                .clickable { onCancelGeneration() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("cancel_generation_button"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Cancel",
                                tint = StatusRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAr) "إلغاء" else "Cancel",
                                color = StatusRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val (phaseText, progressVal) = when (val p = uiState.currentInferenceState) {
                        is InferenceState.VerifyingModel -> (if (isAr) "فحص سلامة ملف الأوزان…" else p.details) to 0.1f
                        is InferenceState.LoadingWeights -> (if (isAr) "تحميل الأوزان في الذاكرة…" else "Loading weights buffer…") to 0.2f
                        is InferenceState.InitializingEngine -> (if (isAr) "تهيئة محرك ${p.architecture}…" else "Initializing ${p.architecture} (${p.memoryAllocatedMb} MB)…") to 0.3f
                        is InferenceState.ProcessingPrompt -> (if (isAr) "ترميز البرومبت (${p.tokenCount} كلمة)…" else "Tokenizing prompt (${p.tokenCount} tokens)…") to 0.4f
                        is InferenceState.DenoisingStep -> (if (isAr) "خطوات الاستدلال: ${p.step}/${p.totalSteps}" else "Latent Denoising: ${p.step}/${p.totalSteps} (${p.percent}%)") to (0.4f + (p.percent / 100f) * 0.3f)
                        is InferenceState.DecodingTensorsToFrames -> (if (isAr) "فك ترميز الإطارات: ${p.currentFrame}/${p.totalFrames}" else "Decoding frame ${p.currentFrame}/${p.totalFrames}…") to (0.7f + (p.currentFrame.toFloat() / p.totalFrames) * 0.15f)
                        is InferenceState.EncodingMp4 -> (if (isAr) "ترميز MP4 بالعتاد (MediaCodec H.264)…" else "Hardware MediaCodec AVC Encoding…") to 0.9f
                        is InferenceState.VerifyingMp4Container -> (if (isAr) "التحقق من سلامة مسار الفيديو…" else "Verifying MP4 track integrity…") to 0.98f
                        else -> (if (isAr) "معالجة…" else "Processing…") to 0.5f
                    }

                    Text(
                        text = phaseText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = PlatinumText,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { progressVal },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = PureWhite,
                        trackColor = SlateCardBorder
                    )
                }
            }
        }

        // Primary Generate Button
        Button(
            onClick = onStartGeneration,
            enabled = !uiState.isGenerating && isModelReady,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("generate_video_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PureWhite,
                contentColor = OnyxBackground,
                disabledContainerColor = GraphiteSurfaceVariant,
                disabledContentColor = DarkMutedText
            )
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "Generate",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isAr) "إنشاء الفيديو الحقيقي" else "Generate Real Video",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            )
        }

        // Newly Generated Video Quick Card
        uiState.lastGeneratedVideo?.let { lastVideo ->
            Spacer(modifier = Modifier.height(20.dp))
            LuxuryCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = StatusGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAr) "اكتمل إنشاء الفيديو بنجاح!" else "VIDEO GENERATED SUCCESSFULLY!",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = StatusGreen,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                        }

                        Text(
                            text = "${lastVideo.durationSeconds}s • ${lastVideo.resolution}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = SilverMutedText
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "\"${lastVideo.prompt}\"",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = PlatinumText,
                            fontSize = 12.sp
                        ),
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onPlayVideo(lastVideo) },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("play_video_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PureWhite,
                                contentColor = OnyxBackground
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAr) "تشغيل" else "Play",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { onExportVideo(lastVideo) },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Save",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAr) "حفظ" else "Save",
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { onShareVideo(lastVideo) },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAr) "مشاركة" else "Share",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
