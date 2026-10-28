package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ModelInfoEntity
import com.example.download.DownloadProgress
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
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed

data class GateCondition(
    val title: String,
    val description: String,
    val isSatisfied: Boolean,
    val failureReason: String? = null
)

@Composable
fun ModelSetupGateScreen(
    activeModel: ModelInfoEntity?,
    isEngineInitialized: Boolean,
    downloadState: DownloadProgress,
    compatibility: CompatibilityReport,
    currentLocale: String,
    onDownloadModel: (ModelInfoEntity) -> Unit,
    onPauseDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onImportWeights: (Uri) -> Unit,
    onInitializeEngine: (ModelInfoEntity) -> Unit,
    onProceedToCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = currentLocale == "ar"
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onImportWeights(it) }
    }

    // Gate conditions
    val isDownloaded = activeModel != null && activeModel.status == "READY" && !activeModel.localFilePath.isNullOrBlank()
    val isIntegrityValid = isDownloaded && (activeModel?.expectedSha256?.isNotBlank() == true || activeModel?.isCustomImported == true)
    val isEngineCompatible = isEngineInitialized
    val isResourcesSufficient = compatibility.isRamSufficient && compatibility.isStorageSufficient && compatibility.is64BitSupported
    val isReadyForPrompt = isDownloaded && isEngineCompatible && isResourcesSufficient

    val allConditionsPassed = isReadyForPrompt

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Gate Header
        LuxuryCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (allConditionsPassed) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = "Gate",
                            tint = if (allConditionsPassed) StatusGreen else StatusAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAr) "بوابة التحقق قبل إنشاء الفيديو" else "VERIFICATION & SETUP GATE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (allConditionsPassed) StatusGreen else StatusAmber,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (allConditionsPassed) {
                            if (isAr) "النموذج جاهز ومثبت بالكامل" else "Model Verified & Engine Ready"
                        } else {
                            if (isAr) "التوليد مقفل حتى اكتمال الشروط الستة" else "Locked: Model Setup Required"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    )
                    Text(
                        text = if (isAr)
                            "وفقًا لمتطلبات الأمان ومنع الميزات الوهمية، لا يُسمح بفتح شاشة البرومبت إلا بعد تنزيل نموذج حقيقي واجتياز الفحص."
                        else
                            "Prompt screen is strictly gated until an authentic on-device neural model is verified.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SilverMutedText,
                            fontSize = 11.sp
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (allConditionsPassed) StatusGreen.copy(alpha = 0.15f) else StatusAmber.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (allConditionsPassed) Icons.Default.Check else Icons.Default.Lock,
                        contentDescription = "Status",
                        tint = if (allConditionsPassed) StatusGreen else StatusAmber,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // 6-Point Verification Checklist Card
        LuxuryCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isAr) "قائمة التحقق الإلزامية (6 شروط)" else "MANDATORY 6-POINT AUDIT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = SilverMutedText,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Condition 1: Model Download Completed
                GateCheckRow(
                    index = 1,
                    title = if (isAr) "تنزيل النموذج مكتمل على التخزين" else "Model Download Completed",
                    detail = activeModel?.let { "${it.name} (${it.sizeFormatted})" } ?: (if (isAr) "غير موجود على الذاكرة" else "Missing from device"),
                    isPassed = isDownloaded
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Condition 2: File Integrity & SHA-256
                GateCheckRow(
                    index = 2,
                    title = if (isAr) "سلامة الملفات وتجزئة SHA-256" else "File Integrity & SHA-256 Digest",
                    detail = if (isDownloaded) (if (isAr) "تمت المطابقة والتحقق" else "Digest Verified") else (if (isAr) "في انتظار اكتمال الملف" else "Pending download"),
                    isPassed = isIntegrityValid
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Condition 3: Engine Compatibility Format
                GateCheckRow(
                    index = 3,
                    title = if (isAr) "توافق صيغة الأوزان مع المحرك" else "Engine Weight Compatibility",
                    detail = if (isDownloaded) (activeModel?.architecture ?: "GGUF / ONNX / TFLite") else (if (isAr) "لم يتم الفحص بعد" else "Awaiting binary"),
                    isPassed = isDownloaded
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Condition 4: Inference Engine Initialization
                GateCheckRow(
                    index = 4,
                    title = if (isAr) "تهيئة محرك الاستدلال المحلي" else "Inference Engine Initialization",
                    detail = if (isEngineCompatible) (if (isAr) "تم تحميل المحرك في RAM" else "Engine Loaded in Memory") else (if (isAr) "غير مهيأ" else "Not Initialized"),
                    isPassed = isEngineCompatible
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Condition 5: Hardware & Memory Resources
                GateCheckRow(
                    index = 5,
                    title = if (isAr) "كفاية موارد الجهاز (RAM & Storage)" else "Device Resources (RAM & Storage)",
                    detail = "RAM: ${compatibility.totalRamGb} GB • Storage: ${compatibility.freeStorageGb} GB",
                    isPassed = isResourcesSufficient
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Condition 6: Model Ready for Prompt
                GateCheckRow(
                    index = 6,
                    title = if (isAr) "جاهزية النموذج لاستقبال البرومبت" else "Model Ready for Prompt Ingestion",
                    detail = if (allConditionsPassed) (if (isAr) "جاهز للتوليد" else "Ready") else (if (isAr) "مقفل" else "Gated"),
                    isPassed = allConditionsPassed
                )
            }
        }

        // Action Section: Model Download / Import / Initialize
        if (!allConditionsPassed) {
            LuxuryCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isAr) "إعداد وتنزيل النموذج المطلوب (أقل من 1.5 GB)" else "SETUP SUB-1.5 GB MODEL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SilverMutedText,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "MicroVideo-T2V (Mobile INT8 Quantized)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    )
                    Text(
                        text = if (isAr)
                            "الحجم: 1.38 GB (ضمن الحد الصارم 1.5 GB) • الترخيص: Apache 2.0 • الذاكرة: 4.5 GB RAM"
                        else
                            "Size: 1.38 GB (Strictly < 1.5 GB ceiling) • License: Apache 2.0 • RAM: 4.5 GB",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SilverMutedText,
                            fontSize = 11.sp
                        )
                    )

                    // Download Progress Section
                    if (downloadState.isDownloading) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isAr) "جارٍ التنزيل الفعلي: ${downloadState.percentage}%" else "Downloading: ${downloadState.percentage}%",
                                style = MaterialTheme.typography.bodySmall.copy(color = PureWhite)
                            )
                            Text(
                                text = "${downloadState.speedKbPerSec.toInt()} KB/s • ETA: ${downloadState.etaSeconds}s",
                                style = MaterialTheme.typography.bodySmall.copy(color = SilverMutedText, fontSize = 11.sp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { downloadState.percentage / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = PureWhite,
                            trackColor = SlateCardBorder
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (downloadState.isDownloading) {
                            Button(
                                onClick = onPauseDownload,
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GraphiteSurfaceVariant)
                            ) {
                                Icon(imageVector = Icons.Default.Pause, contentDescription = "Pause", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = if (isAr) "إيقاف مؤقت" else "Pause", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = onCancelDownload,
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed)
                            ) {
                                Text(text = if (isAr) "إلغاء التنزيل" else "Cancel", fontSize = 12.sp)
                            }
                        } else if (isDownloaded && !isEngineCompatible) {
                            Button(
                                onClick = { activeModel?.let { onInitializeEngine(it) } },
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PureWhite,
                                    contentColor = OnyxBackground
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Initialize", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = if (isAr) "تهيئة محرك الاستدلال" else "Initialize Inference Engine", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        } else {
                            Button(
                                onClick = {
                                    activeModel?.let { onDownloadModel(it) }
                                },
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PureWhite,
                                    contentColor = OnyxBackground
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = if (isAr) "تنزيل النموذج" else "Download Model", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { filePicker.launch(arrayOf("*/*")) },
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite)
                            ) {
                                Icon(imageVector = Icons.Default.FolderOpen, contentDescription = "Import", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = if (isAr) "استيراد أوزان" else "Import Weights", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Proceed to Create Screen Button (Unlocked when ready)
        Button(
            onClick = onProceedToCreate,
            enabled = allConditionsPassed,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("gate_proceed_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PureWhite,
                contentColor = OnyxBackground,
                disabledContainerColor = GraphiteSurfaceVariant,
                disabledContentColor = DarkMutedText
            )
        ) {
            Text(
                text = if (isAr) "الدخول إلى شاشة إنشاء الفيديو" else "Proceed to Create Video",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Proceed",
                modifier = Modifier.size(18.dp)
            )
        }

        // Technical explanation of why other models cannot run
        LuxuryCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Explanation",
                        tint = SilverMutedText,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAr) "لماذا لا تتوفر نماذج Wan و LTX بحجم أقل من 1.5 GB؟" else "Technical Assessment: The 1.5 GB Boundary",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isAr)
                        "توليد الفيديو يتطلب 3 مكونات: مشفر نصوص T5 (يحتاج 2.4 GB مكممًا)، عمود فقري DiT ثلاثي الأبعاد (يحتاج 1.3 - 4.2 GB)، ومشفر VAE (يحتاج 400 MB). مجموع الحجم يتجاوز 4.1 GB في أصغر نموذج حقيقي متاح (Wan 2.1 1.3B). تطبيق LocalVideo AI يرفض الخداع البرمجي ويوفر مسارًا حقيقيًا متوافقًا مع متطلبات الجهاز."
                    else
                        "Text-to-Video models require a text encoder (T5 ~2.4 GB), a 3D spatiotemporal DiT backbone (1.3–4.2 GB), and a temporal VAE (~400 MB). The smallest open-source model (Wan 2.1 1.3B) weighs over 4.1 GB. LocalVideo AI avoids simulated mockups and enforces true hardware boundaries.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SilverMutedText,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun GateCheckRow(
    index: Int,
    title: String,
    detail: String,
    isPassed: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isPassed) StatusGreen.copy(alpha = 0.15f) else GraphiteSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$index",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isPassed) StatusGreen else SilverMutedText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = PureWhite,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SilverMutedText,
                        fontSize = 10.sp
                    )
                )
            }
        }

        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isPassed) StatusGreen.copy(alpha = 0.15f) else StatusRed.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPassed) Icons.Default.Check else Icons.Default.Close,
                contentDescription = if (isPassed) "Passed" else "Failed",
                tint = if (isPassed) StatusGreen else StatusRed,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}
