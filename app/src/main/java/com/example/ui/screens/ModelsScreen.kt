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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
fun ModelsScreen(
    models: List<ModelInfoEntity>,
    selectedModel: ModelInfoEntity?,
    downloadState: DownloadProgress,
    currentLocale: String,
    onSelectModel: (ModelInfoEntity) -> Unit,
    onDownloadModel: (ModelInfoEntity) -> Unit,
    onPauseDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDeleteModel: (ModelInfoEntity) -> Unit,
    onImportCustomWeights: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = currentLocale == "ar"
    val context = LocalContext.current

    // SAF Document Picker for custom weights
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onImportCustomWeights(it) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))

            // Limit Notice Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(GraphiteSurfaceVariant)
                    .border(1.dp, SlateCardBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Policy",
                            tint = PureWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAr) "معيار الحد الصارم لحجم النموذج: 1.5 GB" else "STRICT 1.5 GB MODEL SIZE STANDARD",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = PureWhite,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isAr)
                            "وفقًا لمتطلبات المشروع، يُمنع تنزيل نماذج تتجاوز 1.5 GB على ذاكرة الهاتف لتفادي انهيار النظام. النماذج الأكبر (مثل CogVideoX وLTX) موضحة أدناه لتوثيق القيود التقنية فقط ولا يمكن تشغيلها محليًا."
                        else
                            "In strict compliance with on-device memory guidelines, models exceeding 1.5 GB binary budget cannot be downloaded for local inference. They are cataloged below to document technical boundaries.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SilverMutedText,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    )
                }
            }
        }

        // Custom Model Import Button
        item {
            OutlinedButton(
                onClick = { filePicker.launch(arrayOf("*/*")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("import_model_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = CharcoalSurface,
                    contentColor = PureWhite
                )
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = "Import",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isAr) "استيراد أوزان مخصصة من الهاتف (حد أقصى 1.5 GB)" else "Import Local Weights (Max 1.5 GB)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }

        items(models, key = { it.id }) { model ->
            val isSelected = selectedModel?.id == model.id
            val isCurrentlyDownloading = downloadState.isDownloading && downloadState.modelId == model.id

            LuxuryCard(
                modifier = Modifier
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) PureWhite else SlateCardBorder,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        if (model.status != "EXCEEDS_LIMIT") {
                            onSelectModel(model)
                        }
                    }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = model.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PureWhite
                                    )
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = PureWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${model.version} • ${model.provider}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = SilverMutedText,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        StatusBadge(status = model.status)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Specs Grid
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(GraphiteSurfaceVariant)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isAr) "الحجم الفعلي" else "Actual Size",
                                style = MaterialTheme.typography.labelSmall.copy(color = DarkMutedText, fontSize = 10.sp)
                            )
                            Text(
                                text = model.sizeFormatted,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (model.status == "EXCEEDS_LIMIT") StatusRed else PureWhite,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Column {
                            Text(
                                text = if (isAr) "الذاكرة المطلوبة" else "Min RAM",
                                style = MaterialTheme.typography.labelSmall.copy(color = DarkMutedText, fontSize = 10.sp)
                            )
                            Text(
                                text = "${model.minRamGb} GB",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Column {
                            Text(
                                text = if (isAr) "الترخيص" else "License",
                                style = MaterialTheme.typography.labelSmall.copy(color = DarkMutedText, fontSize = 10.sp)
                            )
                            Text(
                                text = model.license,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = SilverMutedText,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Verification notes
                    if (model.verificationNotes.isNotBlank()) {
                        Text(
                            text = model.verificationNotes,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (model.status == "EXCEEDS_LIMIT") StatusRed else DarkMutedText,
                                fontSize = 11.sp
                            )
                        )
                    }

                    // Download Progress Section
                    AnimatedVisibility(visible = isCurrentlyDownloading) {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isAr) "جارٍ التنزيل: ${downloadState.percentage}%" else "Downloading: ${downloadState.percentage}%",
                                    style = MaterialTheme.typography.bodySmall.copy(color = PureWhite)
                                )
                                Text(
                                    text = "${downloadState.speedKbPerSec.toInt()} KB/s • ETA: ${downloadState.etaSeconds}s",
                                    style = MaterialTheme.typography.bodySmall.copy(color = SilverMutedText, fontSize = 10.sp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { downloadState.percentage / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = PureWhite,
                                trackColor = SlateCardBorder
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Official link
                        Row(
                            modifier = Modifier
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(model.downloadUrl))
                                    try { context.startActivity(intent) } catch (_: Exception) {}
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isAr) "المصدر الرسمي" else "Source",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = SilverMutedText,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Source",
                                tint = SilverMutedText,
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        // Right actions
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (model.status == "EXCEEDS_LIMIT") {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(StatusRed.copy(alpha = 0.1f))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = "Blocked",
                                        tint = StatusRed,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isAr) "غير قابل للتنزيل" else "Not Downloadable",
                                        color = StatusRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else if (model.status == "READY") {
                                Button(
                                    onClick = { onSelectModel(model) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) GraphiteSurfaceVariant else PureWhite,
                                        contentColor = if (isSelected) PureWhite else OnyxBackground
                                    )
                                ) {
                                    Text(
                                        text = if (isSelected) (if (isAr) "محدد حالياً" else "Active") else (if (isAr) "اختيار النموذج" else "Use Model"),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (model.isCustomImported) {
                                    IconButton(
                                        onClick = { onDeleteModel(model) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = StatusRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            } else if (isCurrentlyDownloading) {
                                Button(
                                    onClick = onPauseDownload,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GraphiteSurfaceVariant)
                                ) {
                                    Icon(imageVector = Icons.Default.Pause, contentDescription = "Pause", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = if (isAr) "إيقاف مؤقت" else "Pause", fontSize = 11.sp)
                                }
                            } else {
                                Button(
                                    onClick = { onDownloadModel(model) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PureWhite,
                                        contentColor = OnyxBackground
                                    )
                                ) {
                                    Icon(imageVector = Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = if (isAr) "تنزيل النموذج" else "Download", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
