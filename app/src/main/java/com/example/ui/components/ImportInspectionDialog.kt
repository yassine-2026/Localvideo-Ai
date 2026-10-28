package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ComponentRole
import com.example.data.model.ImportInspectionReport
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
fun ImportInspectionDialog(
    report: ImportInspectionReport,
    currentLocale: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val isAr = currentLocale == "ar"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(20.dp),
            color = CharcoalSurface,
            tonalElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isAr) "تقرير فحص واستيراد النموذج" else "Model Import Inspection Report",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        )
                        Text(
                            text = if (isAr) "تحليل المعمارية وسقف الذاكرة الصارم 1.5 GB" else "Architecture breakdown & 1.5 GB memory audit",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SilverMutedText,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (report.isValidAndImportable) StatusGreen.copy(alpha = 0.15f) else StatusRed.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (report.isValidAndImportable) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                            contentDescription = "Status",
                            tint = if (report.isValidAndImportable) StatusGreen else StatusRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Verdict Banner
                val bannerBg = if (report.isValidAndImportable) StatusGreen.copy(alpha = 0.12f) else StatusRed.copy(alpha = 0.12f)
                val bannerBorder = if (report.isValidAndImportable) StatusGreen.copy(alpha = 0.35f) else StatusRed.copy(alpha = 0.35f)
                val bannerColor = if (report.isValidAndImportable) StatusGreen else StatusRed

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(bannerBg)
                        .border(1.dp, bannerBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (report.isValidAndImportable) Icons.Default.Check else Icons.Default.Warning,
                                contentDescription = null,
                                tint = bannerColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAr) report.statusTitleAr else report.statusTitleEn,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = bannerColor,
                                    fontSize = 12.sp
                                )
                            )
                        }
                        val reason = if (isAr) report.rejectionReasonAr else report.rejectionReasonEn
                        if (!reason.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = PlatinumText,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Size & Architecture Summary Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(GraphiteSurfaceVariant)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isAr) "الحجم الإجمالي المقاس" else "Total Measured Size",
                            style = MaterialTheme.typography.labelSmall.copy(color = DarkMutedText, fontSize = 10.sp)
                        )
                        Text(
                            text = report.totalSizeFormatted,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (report.isExceedingLimit) StatusRed else PureWhite,
                                fontSize = 12.sp
                            )
                        )
                    }

                    Column {
                        Text(
                            text = if (isAr) "الحد الصارم للجهاز" else "Strict Device Limit",
                            style = MaterialTheme.typography.labelSmall.copy(color = DarkMutedText, fontSize = 10.sp)
                        )
                        Text(
                            text = "1.50 GB (1536 MB)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = PureWhite,
                                fontSize = 12.sp
                            )
                        )
                    }

                    Column {
                        Text(
                            text = if (isAr) "عدد الملفات" else "Files Count",
                            style = MaterialTheme.typography.labelSmall.copy(color = DarkMutedText, fontSize = 10.sp)
                        )
                        Text(
                            text = "${report.files.size} ${if (isAr) "ملفات" else "files"}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = SilverMutedText,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Files Breakdown Section
                Text(
                    text = if (isAr) "الملفات المكتشفة وتصنيفاتها:" else "Detected Files & Classifications:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                        fontSize = 11.sp
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(report.files) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(OnyxBackground)
                                .border(1.dp, SlateCardBorder, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.fileName,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PureWhite,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isAr) item.roleLabelAr else item.roleLabelEn,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = when (item.role) {
                                            ComponentRole.BACKBONE_DIT_UNET, ComponentRole.STANDALONE_NEURAL_GRAPH -> PureWhite
                                            ComponentRole.TEXT_ENCODER -> StatusAmber
                                            ComponentRole.VAE_DECODER -> SilverMutedText
                                            else -> DarkMutedText
                                        },
                                        fontSize = 10.sp
                                    )
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = item.sizeFormatted,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = PureWhite,
                                        fontSize = 11.sp
                                    )
                                )
                                Text(
                                    text = item.format,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = SilverMutedText,
                                        fontSize = 9.sp
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = GraphiteSurfaceVariant,
                            contentColor = PureWhite
                        )
                    ) {
                        Text(text = if (isAr) "إلغاء" else "Cancel", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onConfirm,
                        enabled = report.isValidAndImportable,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(46.dp)
                            .testTag("confirm_import_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PureWhite,
                            contentColor = OnyxBackground,
                            disabledContainerColor = GraphiteSurfaceVariant,
                            disabledContentColor = DarkMutedText
                        )
                    ) {
                        Text(
                            text = if (isAr) "تأكيد واستيراد الحزمة" else "Confirm & Install",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
