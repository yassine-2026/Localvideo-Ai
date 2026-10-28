package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hardware.CompatibilityReport
import com.example.ui.components.LuxuryCard
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
fun HardwareScreen(
    report: CompatibilityReport,
    currentLocale: String,
    modifier: Modifier = Modifier
) {
    val isAr = currentLocale == "ar"
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Overall Compatibility Score Card
        LuxuryCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isAr) "تقييم التوافق الشامل" else "HARDWARE COMPATIBILITY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SilverMutedText,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (report.isOverallCompatible) {
                            if (isAr) "الجهاز جاهز للاستدلال المحلي" else "Device Ready for On-Device AI"
                        } else {
                            if (isAr) "قيود عتادية على التوليد" else "Hardware Limitations Detected"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (report.isOverallCompatible) StatusGreen else StatusAmber
                        )
                    )
                    Text(
                        text = if (isAr)
                            "فحص شامل لبنية المعالج وذاكرة RAM ومرمز H.264 بالعتاد"
                        else
                            "Verified ABI, RAM constraints, and hardware video encoder",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SilverMutedText,
                            fontSize = 11.sp
                        )
                    )
                }

                // Score Circle
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(GraphiteSurfaceVariant)
                        .border(
                            2.dp,
                            if (report.score >= 70) StatusGreen else if (report.score >= 50) StatusAmber else StatusRed,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${report.score}%",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = PureWhite,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }
        }

        // Hardware Diagnostics Checklist
        LuxuryCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isAr) "فحص متطلبات النظام" else "SYSTEM TELEMETRY AUDIT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = SilverMutedText,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 1. Android OS
                HardwareRow(
                    icon = Icons.Default.Speed,
                    title = if (isAr) "نظام التشغيل" else "Android OS",
                    detail = report.androidVersion,
                    isPass = report.isAndroidVersionSupported
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. CPU Architecture
                HardwareRow(
                    icon = Icons.Default.DeveloperBoard,
                    title = if (isAr) "بنية المعالج (ABI)" else "Processor Architecture",
                    detail = "${report.cpuAbi} (${if (report.is64BitSupported) "64-bit ARM" else "32-bit Unsupported"})",
                    isPass = report.is64BitSupported
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. RAM Memory
                HardwareRow(
                    icon = Icons.Default.Memory,
                    title = if (isAr) "الذاكرة العشوائية (RAM)" else "Physical RAM",
                    detail = "${report.totalRamGb} GB (Available: ${report.availableRamGb} GB)",
                    isPass = report.isRamSufficient
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Free Storage
                HardwareRow(
                    icon = Icons.Default.SdStorage,
                    title = if (isAr) "المساحة الحرة" else "Available Storage",
                    detail = "${report.freeStorageGb} GB (Required >= 3.0 GB)",
                    isPass = report.isStorageSufficient
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 5. Hardware H.264 Encoder
                HardwareRow(
                    icon = Icons.Default.Videocam,
                    title = if (isAr) "مرمز الفيديو العتادي" else "MediaCodec H.264 Encoder",
                    detail = "${report.encoderName} (Max: ${report.maxSupportedResolution})",
                    isPass = report.hasHardwareH264Encoder
                )
            }
        }

        // Technical Issues & Recommendations
        if (report.issues.isNotEmpty() || report.recommendations.isNotEmpty()) {
            LuxuryCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isAr) "الملاحظات الفنية والتوصيات" else "TECHNICAL FINDINGS & ADVICE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SilverMutedText,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    report.issues.forEach { issue ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Issue",
                                tint = StatusRed,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = issue,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = PlatinumText,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    report.recommendations.forEach { rec ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Advice",
                                tint = StatusAmber,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = rec,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = SilverMutedText,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun HardwareRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    detail: String,
    isPass: Boolean
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
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(GraphiteSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = SilverMutedText,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = PureWhite,
                        fontSize = 12.sp
                    )
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SilverMutedText,
                        fontSize = 11.sp
                    )
                )
            }
        }

        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isPass) StatusGreen.copy(alpha = 0.15f) else StatusRed.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPass) Icons.Default.Check else Icons.Default.Close,
                contentDescription = if (isPass) "Passed" else "Failed",
                tint = if (isPass) StatusGreen else StatusRed,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
