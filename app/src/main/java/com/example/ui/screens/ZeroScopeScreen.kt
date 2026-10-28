package com.example.ui.screens

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ComponentVerificationState
import com.example.data.ZeroScopeScreenState
import com.example.data.ZeroScopeSlotData
import com.example.data.ZeroScopeSlotId
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ZeroScopeScreen(
    state: ZeroScopeScreenState,
    currentLocale: String,
    onSelectSlotFile: (ZeroScopeSlotId, Uri) -> Unit,
    onRemoveSlotFile: (ZeroScopeSlotId) -> Unit,
    onActivateZeroScope: () -> Unit,
    onDismissActivationResult: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToHardware: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = currentLocale == "ar"
    var showRamDetail by remember { mutableStateOf(false) }

    // SAF file pickers for each independent slot
    val unetPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onSelectSlotFile(ZeroScopeSlotId.UNET, it) }
    }

    val textEncoderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onSelectSlotFile(ZeroScopeSlotId.TEXT_ENCODER, it) }
    }

    val vaePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onSelectSlotFile(ZeroScopeSlotId.VAE, it) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(OnyxBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Status Bar: Connection & Ranking
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Connection Status
                    Row(
                        modifier = Modifier
                            .background(CharcoalSurface, RoundedCornerShape(20.dp))
                            .border(1.dp, SlateCardBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(StatusGreen)
                        )
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "Connection",
                            tint = StatusGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isAr) "اتصال: إنترنت" else "Connection: Internet",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = PlatinumText
                        )
                    }

                    // Model Type Badge
                    Row(
                        modifier = Modifier
                            .background(GraphiteSurfaceVariant, RoundedCornerShape(20.dp))
                            .border(1.dp, SlateCardBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = SilverMutedText,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isAr) "توليد فيديو من النص (Text-to-Video)" else "Text-to-Video Generation",
                            fontSize = 11.sp,
                            color = SilverMutedText
                        )
                    }
                }

                // Model Main Title Header
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "ZeroScope v2 (576w)",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )

                    // Ranking clarification badge (Per prompt: not general claim, but ranked #1 among verified list)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CharcoalSurface, RoundedCornerShape(8.dp))
                            .border(1.dp, SlateCardBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = PlatinumText,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isAr) {
                                "الترتيب: #1 الأخف ضمن قائمة النماذج التي تم التحقق منها، وليس ادعاءً عامًا بأنه الأخف عالميًا."
                            } else {
                                "Rank: #1 Lightest among verified research models (not a global claim)."
                            },
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = SilverMutedText
                        )
                    }
                }

                // Information & Metrics Grid Cards
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 2
                ) {
                    // Card 1: True Total Size (dynamic from actual files)
                    LuxuryCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isAr) "الحجم الإجمالي الحقيقي" else "True Total Size",
                                fontSize = 11.sp,
                                color = SilverMutedText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.totalRealSizeFormatted,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (state.isExceedingLimit) StatusRed else PureWhite
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isAr) "التقديري: ~${state.estimatedTotalSizeFormatted}" else "Est: ~${state.estimatedTotalSizeFormatted}",
                                fontSize = 10.sp,
                                color = DarkMutedText
                            )
                        }
                    }

                    // Card 2: Permitted Limit
                    LuxuryCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isAr) "الحد المسموح للهاتف" else "Device Permitted Limit",
                                fontSize = 11.sp,
                                color = SilverMutedText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "1.5 GB",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = PlatinumText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isAr) "حد أمان سعة التخزين والذاكرة" else "Strict memory & storage threshold",
                                fontSize = 10.sp,
                                color = DarkMutedText
                            )
                        }
                    }

                    // Card 3: Size Status (Warning if > 1.5 GB)
                    LuxuryCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isAr) "حالة الحجم" else "Size Status",
                                fontSize = 11.sp,
                                color = SilverMutedText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (state.isExceedingLimit) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = StatusRed, modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = if (isAr) state.sizeStatusLabelAr else state.sizeStatusLabelEn,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.isExceedingLimit) StatusRed else StatusGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (state.isExceedingLimit) {
                                    if (isAr) "يتطلب تكميم لتشغيله محلياً" else "Requires quantization for mobile"
                                } else {
                                    if (isAr) "ضمن السعة المحددة" else "Within safe range"
                                },
                                fontSize = 10.sp,
                                color = DarkMutedText
                            )
                        }
                    }

                    // Card 4: RAM Requirements
                    LuxuryCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showRamDetail = !showRamDetail }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isAr) "متطلبات الرام" else "RAM Requirements",
                                    fontSize = 11.sp,
                                    color = SilverMutedText
                                )
                                Icon(
                                    imageVector = if (showRamDetail) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = SilverMutedText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "~8 GB RAM (تقديرية)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusAmber
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isAr) "اضغط للتفاصيل الهندسية" else "Tap for engineering notes",
                                fontSize = 10.sp,
                                color = DarkMutedText
                            )
                        }
                    }

                    // Card 5: File Format
                    LuxuryCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isAr) "صيغة الملفات" else "File Format",
                                fontSize = 11.sp,
                                color = SilverMutedText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "SafeTensors",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isAr) "أوزان آمنة مشفرة ثنائياً" else "Zero-copy binary tensor format",
                                fontSize = 10.sp,
                                color = DarkMutedText
                            )
                        }
                    }

                    // Card 6: Compatibility & Activation Status
                    LuxuryCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isAr) "حالة التوافق والتفعيل" else "Compatibility & Activation",
                                fontSize = 11.sp,
                                color = SilverMutedText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isAr) state.compatibilityStatusAr else state.compatibilityStatusEn,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (state.isCompatible) StatusGreen else StatusAmber
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isAr) {
                                    if (state.isActivated) "حالة التفعيل: جاهز" else "حالة التفعيل: غير مفعّل"
                                } else {
                                    if (state.isActivated) "Activation: Ready" else "Activation: Inactive"
                                },
                                fontSize = 10.sp,
                                color = DarkMutedText
                            )
                        }
                    }
                }

                // Expandable RAM Explanation Card
                AnimatedVisibility(visible = showRamDetail) {
                    LuxuryCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Memory, contentDescription = null, tint = StatusAmber, modifier = Modifier.size(18.dp))
                                Text(
                                    text = if (isAr) "توضيح هندسي للذاكرة العشوائية (RAM)" else "Engineering RAM Note",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = PureWhite
                                )
                            }
                            Text(
                                text = if (isAr) {
                                    "حجم ملفات النموذج على القرص يختلف تماماً عن الرام المستهلك أثناء المعالجة. عند تشغيل ZeroScope v2 في بيئة PyTorch/Diffusers يتم فك طبقات الانتباه المكانية والزمنية (3D Spatiotemporal Attention) وتخزين المتجهات اللاتنت، مما يستهلك قرابة 8 GB من الرام/VRAM. الهواتف ذات الرام الأقل لا تستطيع تشغيل هذا النموذج بدون تكميم حقيقي (4-bit/8-bit)."
                                } else {
                                    "File size on disk differs from runtime RAM consumption. During diffusion, 3D spatio-temporal attention layers and latent activations expand to ~8 GB RAM/VRAM. Devices with lower RAM cannot run this model without native quantization."
                                },
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = SilverMutedText
                            )
                        }
                    }
                }

                // Credibility Note (MANDATORY)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CharcoalSurface, RoundedCornerShape(10.dp))
                        .border(1.dp, SlateCardBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = StatusAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isAr) {
                            "ملاحظة الأمان والمصداقية: الأرقام التقديرية (UNet ~2.75 GB، Text Encoder ~680 MB، VAE ~335 MB، الإجمالي ~3.76 GB) هي بيانات أولية للتوجيه وليست حقائق مؤكدة حتى يتم فحص الملفات الفعلية المحددة من قبلك."
                        } else {
                            "Safety & Credibility Note: Preliminary figures (UNet ~2.75 GB, Text Encoder ~680 MB, VAE ~335 MB, Total ~3.76 GB) are baseline guidelines and not confirmed facts until actual files are inspected on your device."
                        },
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = PlatinumText
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Section Header: The 3 Independent Component Blocks
                Text(
                    text = if (isAr) "المكونات الثلاثة المطلوبة للاستيراد" else "Required 3 Model Components",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                )

                // Block 1: UNet
                ComponentSlotCard(
                    slot = state.unetSlot,
                    isAr = isAr,
                    onPickFile = { unetPickerLauncher.launch(arrayOf("*/*")) },
                    onRemoveFile = { onRemoveSlotFile(ZeroScopeSlotId.UNET) },
                    testTagPrefix = "unet"
                )

                // Block 2: Text Encoder
                ComponentSlotCard(
                    slot = state.textEncoderSlot,
                    isAr = isAr,
                    onPickFile = { textEncoderPickerLauncher.launch(arrayOf("*/*")) },
                    onRemoveFile = { onRemoveSlotFile(ZeroScopeSlotId.TEXT_ENCODER) },
                    testTagPrefix = "text_encoder"
                )

                // Block 3: VAE
                ComponentSlotCard(
                    slot = state.vaeSlot,
                    isAr = isAr,
                    onPickFile = { vaePickerLauncher.launch(arrayOf("*/*")) },
                    onRemoveFile = { onRemoveSlotFile(ZeroScopeSlotId.VAE) },
                    testTagPrefix = "vae"
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Activation Section: Button «تفعيل ZeroScope v2»
                LuxuryCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isAr) "تفعيل النموذج ومطابقة المحرك" else "Model Activation & Engine Check",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )

                        Text(
                            text = if (isAr) {
                                "يشترط اكتمال فحص الملفات الثلاثة بنجاح، ووجود محرك استدلال محلي متوافق على الجهاز."
                            } else {
                                "Requires successful verification of all 3 files and a compatible on-device engine."
                            },
                            fontSize = 11.sp,
                            color = SilverMutedText
                        )

                        Button(
                            onClick = onActivateZeroScope,
                            enabled = state.isActivationReady,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("zeroscope_activate_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PureWhite,
                                contentColor = OnyxBackground,
                                disabledContainerColor = CharcoalSurface,
                                disabledContentColor = DarkMutedText
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.isActivationReady) Icons.Default.CheckCircle else Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isAr) "تفعيل ZeroScope v2" else "Activate ZeroScope v2",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (!state.isActivationReady) {
                            Text(
                                text = if (isAr) {
                                    "الزر معطل حتى يتم اختيار وفحص الملفات الثلاثة (UNet و Text Encoder و VAE) بنجاح."
                                } else {
                                    "Activation disabled until UNet, Text Encoder, and VAE are successfully verified."
                                },
                                fontSize = 11.sp,
                                color = DarkMutedText,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Activation Result Honest Dialog
    state.activationResult?.let { result ->
        AlertDialog(
            onDismissRequest = onDismissActivationResult,
            containerColor = CharcoalSurface,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (result.isActivated) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (result.isActivated) StatusGreen else StatusAmber,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (isAr) result.titleAr else result.titleEn,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isAr) result.messageAr else result.messageEn,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = PlatinumText
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(OnyxBackground, RoundedCornerShape(8.dp))
                            .border(1.dp, SlateCardBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = result.technicalDetails,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = SilverMutedText
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onDismissActivationResult,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureWhite,
                        contentColor = OnyxBackground
                    ),
                    modifier = Modifier.testTag("activation_dialog_confirm")
                ) {
                    Text(if (isAr) "حسناً، فهمت" else "Understood", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onNavigateToHardware) {
                    Text(if (isAr) "فحص عتاد الهاتف" else "Check Hardware", color = SilverMutedText)
                }
            }
        )
    }
}

@Composable
private fun ComponentSlotCard(
    slot: ZeroScopeSlotData,
    isAr: Boolean,
    onPickFile: () -> Unit,
    onRemoveFile: () -> Unit,
    testTagPrefix: String
) {
    LuxuryCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Title and Role
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = slot.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                )
                Text(
                    text = if (isAr) slot.descriptionAr else slot.descriptionEn,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = SilverMutedText
                )
            }

            // Current State Block
            when (val vState = slot.verificationState) {
                is ComponentVerificationState.NotSelected -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GraphiteSurfaceVariant, RoundedCornerShape(8.dp))
                            .border(1.dp, SlateCardBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isAr) "لم يتم الاختيار بعد" else "Not selected yet",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = DarkMutedText
                            )
                            Text(
                                text = if (isAr) "الحجم التقديري: ~${slot.estimatedSizeFormatted}" else "Estimated size: ~${slot.estimatedSizeFormatted}",
                                fontSize = 10.sp,
                                color = DarkMutedText
                            )
                        }

                        Button(
                            onClick = onPickFile,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PureWhite,
                                contentColor = OnyxBackground
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_pick_$testTagPrefix")
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isAr) "اختيار ملف" else "Choose File", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is ComponentVerificationState.Checking -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GraphiteSurfaceVariant, RoundedCornerShape(8.dp))
                            .border(1.dp, SlateCardBorder, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = PureWhite
                        )
                        Text(
                            text = if (isAr) vState.stageAr else vState.stageEn,
                            fontSize = 12.sp,
                            color = PlatinumText
                        )
                    }
                }

                is ComponentVerificationState.Valid -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GraphiteSurfaceVariant, RoundedCornerShape(8.dp))
                            .border(1.dp, StatusGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // File Name & Calculated Size
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = vState.fileName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite,
                                maxLines = 1
                            )
                            Text(
                                text = vState.sizeFormatted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusGreen
                            )
                        }

                        // Inspection badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(14.dp))
                            Text(
                                text = if (isAr) {
                                    "سليم ومطابق: ترويسة SafeTensors (${vState.tensorCount} تنسور) | SHA-256: ${vState.sha256Prefix}"
                                } else {
                                    "Verified SafeTensors (${vState.tensorCount} tensors) | SHA-256: ${vState.sha256Prefix}"
                                },
                                fontSize = 10.sp,
                                color = PlatinumText
                            )
                        }

                        // Action Buttons: Change & Remove
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onPickFile,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.testTag("btn_change_$testTagPrefix")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = PlatinumText)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isAr) "تغيير الملف" else "Change", fontSize = 11.sp, color = PlatinumText)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = onRemoveFile,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.testTag("btn_remove_$testTagPrefix")
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp), tint = StatusRed)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isAr) "إزالة الملف" else "Remove", fontSize = 11.sp, color = StatusRed)
                            }
                        }
                    }
                }

                is ComponentVerificationState.Invalid -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GraphiteSurfaceVariant, RoundedCornerShape(8.dp))
                            .border(1.dp, StatusRed.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = vState.fileName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusRed,
                                maxLines = 1
                            )
                            Text(
                                text = vState.sizeFormatted,
                                fontSize = 11.sp,
                                color = SilverMutedText
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = StatusRed, modifier = Modifier.size(15.dp))
                            Text(
                                text = if (isAr) {
                                    "غير مطابق: ${vState.failureReasonAr}"
                                } else {
                                    "Mismatched: ${vState.failureReasonEn}"
                                },
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                color = StatusRed
                            )
                        }

                        // Action Buttons: Change & Remove
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onPickFile,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.testTag("btn_change_$testTagPrefix")
                            ) {
                                Text(if (isAr) "اختيار ملف آخر" else "Choose Another", fontSize = 11.sp, color = PlatinumText)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = onRemoveFile,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.testTag("btn_remove_$testTagPrefix")
                            ) {
                                Text(if (isAr) "إزالة" else "Remove", fontSize = 11.sp, color = StatusRed)
                            }
                        }
                    }
                }
            }
        }
    }
}
