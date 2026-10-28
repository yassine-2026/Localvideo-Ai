package com.example.ui.screens

import android.content.Context
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ModelInfoEntity
import com.example.data.model.ModelCatalog
import com.example.data.model.ResearchModelSpec
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

@Composable
fun HomeScreen(
    installedModel: ModelInfoEntity?,
    isEngineInitialized: Boolean,
    currentLocale: String,
    onImportFiles: (List<Uri>) -> Unit,
    onCopyDirectUrl: (label: String, url: String) -> Unit,
    onNavigateToGate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = currentLocale == "ar"
    val context = LocalContext.current

    // SAF multi-document picker for model weights & components
    val multiDocPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            onImportFiles(uris)
        }
    }

    // State for expanded component breakdowns per model card
    val expandedMap = remember {
        mutableStateMapOf<String, Boolean>().apply {
            // Default first model expanded
            put("zeroscope-v2", true)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))

            // App Identity & Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LocalVideo AI",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = PureWhite,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Text(
                        text = if (isAr) "توليد الفيديو محلياً بنماذج مفتوحة المصدر حقيقية" else "Native On-Device Video Generation Engine",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SilverMutedText,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }

        // Current Installed Model Status Card
        item {
            LuxuryCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isAr) "حالة النموذج المثبت حالياً" else "CURRENT INSTALLED MODEL STATUS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = SilverMutedText,
                                letterSpacing = 1.sp
                            )
                        )

                        if (installedModel != null && installedModel.status == "READY") {
                            StatusBadge(status = "READY")
                        } else {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(StatusAmber.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(StatusAmber)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isAr) "غير مثبت" else "Not Installed",
                                    color = StatusAmber,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (installedModel != null && installedModel.status == "READY") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Active",
                                tint = StatusGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = installedModel.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PureWhite
                                    )
                                )
                                Text(
                                    text = "${installedModel.sizeFormatted} • ${if (isEngineInitialized) (if (isAr) "محرك الاستدلال مهيأ" else "Engine Initialized") else (if (isAr) "بانتظار التهيئة" else "Awaiting Init")}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = SilverMutedText,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = SilverMutedText,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isAr) "لا يوجد نموذج متوافق مثبت حالياً" else "No Compatible Model Installed",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PureWhite
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isAr)
                                        "يتطلب التطبيق استيراد نموذج ذكاء اصطناعي محلي لتوليد الفيديو. يُمنع التوليد الوهمي بالرسوم الهندسية وفق شروط المشروع."
                                    else
                                        "Video generation requires an authentic verified model bundle. Synthetic procedural mockups are strictly prohibited.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = SilverMutedText,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Prominent Import Button via SAF (Multi-file selection)
        item {
            Button(
                onClick = { multiDocPicker.launch(arrayOf("*/*")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("home_import_model_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PureWhite,
                    contentColor = OnyxBackground
                )
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = "Import",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isAr) "استيراد نموذج من الجهاز" else "Import Model from Device",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                )
            }
        }

        // Section Title: "اختر نموذج توليد الفيديو"
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isAr) "اختر نموذج توليد الفيديو" else "Choose Video Generation Model",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                )
            }
            Text(
                text = if (isAr)
                    "أربعة نماذج بحثية عالمية حقيقية مرتبة من الأخف إلى الأثقل، مع تفصيل كامل للملفات والمتطلبات:"
                else
                    "Four authentic global research models ordered from lightest to heaviest, with full component breakdown:",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = SilverMutedText,
                    fontSize = 11.sp
                )
            )
        }

        // 4 Model Cards Ordered from Lightest to Heaviest
        items(ModelCatalog.RESEARCH_MODELS, key = { it.id }) { modelSpec ->
            val isExpanded = expandedMap[modelSpec.id] == true

            ModelResearchCard(
                modelSpec = modelSpec,
                isExpanded = isExpanded,
                isAr = isAr,
                context = context,
                onToggleExpand = {
                    expandedMap[modelSpec.id] = !isExpanded
                },
                onCopyDirectUrl = { label, url ->
                    onCopyDirectUrl(label, url)
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun ModelResearchCard(
    modelSpec: ResearchModelSpec,
    isExpanded: Boolean,
    isAr: Boolean,
    context: Context,
    onToggleExpand: () -> Unit,
    onCopyDirectUrl: (String, String) -> Unit
) {
    LuxuryCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateCardBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Order badge + Name + Task
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(GraphiteSurfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "#${modelSpec.orderIndex} ${if (modelSpec.orderIndex == 1) (if (isAr) "الأخف" else "Lightest") else if (modelSpec.orderIndex == 4) (if (isAr) "الأثقل" else "Heaviest") else ""}".trim(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite,
                                    fontSize = 10.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = modelSpec.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = if (isAr) modelSpec.taskAr else modelSpec.taskEn,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SilverMutedText,
                            fontSize = 11.sp
                        )
                    )
                }

                // Compatibility Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(StatusRed.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isAr) "يتجاوز 1.5 GB" else "Exceeds 1.5 GB",
                        color = StatusRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Specs Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(GraphiteSurfaceVariant)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isAr) "الحجم الحقيقي الكامل" else "True Total Size",
                        style = MaterialTheme.typography.labelSmall.copy(color = DarkMutedText, fontSize = 10.sp)
                    )
                    Text(
                        text = modelSpec.totalSizeFormatted,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = StatusRed,
                            fontSize = 11.sp
                        )
                    )
                }

                Column {
                    Text(
                        text = if (isAr) "صيغة الملفات" else "File Formats",
                        style = MaterialTheme.typography.labelSmall.copy(color = DarkMutedText, fontSize = 10.sp)
                    )
                    Text(
                        text = modelSpec.fileFormats,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = PureWhite,
                            fontSize = 11.sp
                        )
                    )
                }

                Column {
                    Text(
                        text = if (isAr) "متطلبات الذاكرة" else "Hardware RAM",
                        style = MaterialTheme.typography.labelSmall.copy(color = DarkMutedText, fontSize = 10.sp)
                    )
                    Text(
                        text = "${modelSpec.minRamGb.toInt()} GB RAM",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = PureWhite,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hardware & Compatibility details
            Text(
                text = "${if (isAr) "متطلبات التشغيل: " else "Requirements: "}${if (isAr) modelSpec.hardwareRequirementsAr else modelSpec.hardwareRequirementsEn}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = SilverMutedText,
                    fontSize = 11.sp
                )
            )
            Text(
                text = "${if (isAr) "حالة التوافق: " else "Compatibility: "}${if (isAr) modelSpec.compatibilityStatusAr else modelSpec.compatibilityStatusEn}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = StatusRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Component Breakdown Toggle Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CharcoalSurface)
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isAr) "تفصيل ملفات النموذج ومكوناته (${modelSpec.components.size} ملفات إلزامية)" else "Component Files Breakdown (${modelSpec.components.size} files)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                        fontSize = 11.sp
                    )
                )

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle",
                    tint = SilverMutedText,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Expanded Component Files List
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    modelSpec.components.forEach { comp ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(OnyxBackground)
                                .border(1.dp, SlateCardBorder, RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = comp.fileName,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PureWhite,
                                        fontSize = 11.sp
                                    )
                                )
                                Text(
                                    text = if (isAr) comp.roleAr else comp.roleEn,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = SilverMutedText,
                                        fontSize = 10.sp
                                    )
                                )
                                Text(
                                    text = if (isAr) comp.descriptionAr else comp.descriptionEn,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = DarkMutedText,
                                        fontSize = 9.sp
                                    )
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = comp.sizeFormatted,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PureWhite,
                                        fontSize = 11.sp
                                    )
                                )
                                Text(
                                    text = comp.format,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = DarkMutedText,
                                        fontSize = 9.sp
                                    )
                                )
                            }
                        }
                    }

                    Text(
                        text = if (isAr) modelSpec.notesAr else modelSpec.notesEn,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SilverMutedText,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row:
            // 1. «فتح صفحة التنزيل» (Hugging Face)
            // 2. «نسخ رابط التنزيل المباشر» (Direct file URL, only when verified)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Open Download Page
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(modelSpec.huggingFaceRepoUrl))
                        try { context.startActivity(intent) } catch (_: Exception) {}
                    },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GraphiteSurfaceVariant,
                        contentColor = PureWhite
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAr) "فتح صفحة التنزيل" else "Download Page",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Copy Direct Download URL (Shown only when verified direct URL exists)
                if (!modelSpec.directDownloadUrl.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = {
                            onCopyDirectUrl(
                                "${modelSpec.name} Direct URL",
                                modelSpec.directDownloadUrl
                            )
                        },
                        modifier = Modifier.weight(1.2f).height(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = CharcoalSurface,
                            contentColor = PureWhite
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SlateCardBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAr) "نسخ رابط التنزيل المباشر" else "Copy Direct URL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
