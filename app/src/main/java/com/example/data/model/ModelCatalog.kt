package com.example.data.model

data class ModelFileComponent(
    val fileName: String,
    val roleAr: String,
    val roleEn: String,
    val sizeBytes: Long,
    val sizeFormatted: String,
    val format: String,
    val descriptionAr: String,
    val descriptionEn: String
)

data class ResearchModelSpec(
    val id: String,
    val orderIndex: Int,
    val name: String,
    val taskAr: String,
    val taskEn: String,
    val totalSizeBytes: Long,
    val totalSizeFormatted: String,
    val fileFormats: String,
    val minRamGb: Double,
    val hardwareRequirementsAr: String,
    val hardwareRequirementsEn: String,
    val compatibilityStatusAr: String,
    val compatibilityStatusEn: String,
    val isCompatibleWithDeviceLimit: Boolean,
    val huggingFaceRepoUrl: String,
    val directDownloadUrl: String?,
    val directDownloadFileName: String?,
    val components: List<ModelFileComponent>,
    val notesAr: String,
    val notesEn: String
)

object ModelCatalog {

    val RESEARCH_MODELS: List<ResearchModelSpec> = listOf(
        // Model 1: ZeroScope v2 (576w) - Lightest (~3.76 GB)
        ResearchModelSpec(
            id = "zeroscope-v2",
            orderIndex = 1,
            name = "ZeroScope v2 (576w)",
            taskAr = "توليد فيديو من النص (Text-to-Video)",
            taskEn = "Text-to-Video Generation",
            totalSizeBytes = 3_765_000_000L,
            totalSizeFormatted = "3.76 GB",
            fileFormats = "SafeTensors (FP16)",
            minRamGb = 8.0,
            hardwareRequirementsAr = "8 GB VRAM / ذاكرة عشوائية موحدة",
            hardwareRequirementsEn = "8 GB VRAM / Unified RAM",
            compatibilityStatusAr = "يتجاوز حد 1.5 GB الصارم للهاتف",
            compatibilityStatusEn = "Exceeds strict 1.5 GB device limit",
            isCompatibleWithDeviceLimit = false,
            huggingFaceRepoUrl = "https://huggingface.co/cerspense/zeroscope_v2_576w",
            directDownloadUrl = "https://huggingface.co/cerspense/zeroscope_v2_576w/resolve/main/unet/diffusion_pytorch_model.safetensors",
            directDownloadFileName = "diffusion_pytorch_model.safetensors",
            components = listOf(
                ModelFileComponent(
                    fileName = "unet/diffusion_pytorch_model.safetensors",
                    roleAr = "أوزان توليد الحركة (UNet Backbone)",
                    roleEn = "UNet Denoising Backbone",
                    sizeBytes = 2_750_000_000L,
                    sizeFormatted = "2.75 GB",
                    format = "SafeTensors",
                    descriptionAr = "الشبكة الزمنية المسؤولة عن إزالة الضجيج وتوليد تتابع الإطارات",
                    descriptionEn = "Spatiotemporal backbone responsible for multi-frame denoising"
                ),
                ModelFileComponent(
                    fileName = "text_encoder/model.safetensors",
                    roleAr = "مشفر النصوص (OpenCLIP ViT-H/14)",
                    roleEn = "Text Encoder (OpenCLIP ViT-H/14)",
                    sizeBytes = 680_000_000L,
                    sizeFormatted = "680 MB",
                    format = "SafeTensors",
                    descriptionAr = "تحويل وصف البرومبت النصي إلى متجهات معاني سياقية",
                    descriptionEn = "Transforms prompt text into high-dimensional semantic embeddings"
                ),
                ModelFileComponent(
                    fileName = "vae/diffusion_pytorch_model.safetensors",
                    roleAr = "مفكك التشفير الصوري (3D VAE Decoder)",
                    roleEn = "3D VAE Decoder",
                    sizeBytes = 335_000_000L,
                    sizeFormatted = "335 MB",
                    format = "SafeTensors",
                    descriptionAr = "فك ضغط المتجهات الكامنة إلى بكسلات فيديو RGB حقيقية",
                    descriptionEn = "Decodes latent tensor space into actual RGB video frames"
                )
            ),
            notesAr = "أخف نموذج مفتوح المصدر تم بحثه. يتطلب ملفات UNet وCLIP وVAE معاً ليعمل، ومجموعها 3.76 GB.",
            notesEn = "Lightest open-source research model. Requires UNet, CLIP, and VAE components totaling 3.76 GB."
        ),

        // Model 2: Wan 2.1 (1.3B) (~4.10 - 5.6 GB)
        ResearchModelSpec(
            id = "wan-2.1-1.3b",
            orderIndex = 2,
            name = "Wan 2.1 (1.3B)",
            taskAr = "توليد فيديو من النص (Text-to-Video)",
            taskEn = "Text-to-Video Generation",
            totalSizeBytes = 4_100_000_000L,
            totalSizeFormatted = "4.10 GB – 5.6 GB",
            fileFormats = "SafeTensors / PyTorch (.pth)",
            minRamGb = 10.0,
            hardwareRequirementsAr = "10 GB RAM / بطاقة تسريع رسومي متقدمة",
            hardwareRequirementsEn = "10 GB RAM / Dedicated GPU",
            compatibilityStatusAr = "يتجاوز حد 1.5 GB الصارم للهاتف",
            compatibilityStatusEn = "Exceeds strict 1.5 GB device limit",
            isCompatibleWithDeviceLimit = false,
            huggingFaceRepoUrl = "https://huggingface.co/Wan-AI/Wan2.1-T2V-1.3B",
            directDownloadUrl = "https://huggingface.co/Wan-AI/Wan2.1-T2V-1.3B/resolve/main/diffusion_pytorch_model.safetensors",
            directDownloadFileName = "diffusion_pytorch_model.safetensors",
            components = listOf(
                ModelFileComponent(
                    fileName = "diffusion_pytorch_model.safetensors",
                    roleAr = "محول التدفق الزمكاني (Spatiotemporal DiT 1.3B)",
                    roleEn = "Spatiotemporal Flow DiT (1.3B)",
                    sizeBytes = 1_300_000_000L,
                    sizeFormatted = "1.3 GB (INT8) / 2.8 GB (FP16)",
                    format = "SafeTensors",
                    descriptionAr = "معمارية التدفق الهندسي الحديثة لمحاكاة الحركة الفيزيائية للفيلم",
                    descriptionEn = "Spatiotemporal flow transformer driving physical visual motion"
                ),
                ModelFileComponent(
                    fileName = "models_t5_umt5-xxl-enc-fp8.safetensors",
                    roleAr = "مشفر نصوص ضخم مكمم (UMT5-XXL FP8)",
                    roleEn = "Text Encoder (UMT5-XXL FP8)",
                    sizeBytes = 2_400_000_000L,
                    sizeFormatted = "2.40 GB",
                    format = "SafeTensors",
                    descriptionAr = "مشفر لغوي فائق الدقة لفهم التفاصيل السينمائية المعقدة",
                    descriptionEn = "Ultra-large linguistic encoder for nuanced prompt comprehension"
                ),
                ModelFileComponent(
                    fileName = "Wan2.1_VAE.pth",
                    roleAr = "مفكك VAE ثلاثي الأبعاد للفيديو",
                    roleEn = "3D Causal Video VAE",
                    sizeBytes = 400_000_000L,
                    sizeFormatted = "400 MB",
                    format = "PyTorch (.pth)",
                    descriptionAr = "إعادة بناء المقاطع بتوقيت زمني متماسك بدقة عالية",
                    descriptionEn = "Temporal-spatial reconstruction of latent tokens to video"
                )
            ),
            notesAr = "أصغر معمارية DiT حقيقية في العالم حتى الآن. لا يمكن تشغيل الـ 1.3B بدون مشفر UMT5 الذي يبلغ 2.4 GB وحده.",
            notesEn = "Smallest real-world DiT architecture. The 1.3B backbone cannot operate without the 2.4 GB UMT5 encoder."
        ),

        // Model 3: CogVideoX-2B (~4.84 - 9.6 GB)
        ResearchModelSpec(
            id = "cogvideox-2b",
            orderIndex = 3,
            name = "CogVideoX-2B",
            taskAr = "توليد فيديو سينمائي من النص (Text-to-Video)",
            taskEn = "Cinematic Text-to-Video Generation",
            totalSizeBytes = 4_840_000_000L,
            totalSizeFormatted = "4.84 GB (INT4) – 9.6 GB (FP16)",
            fileFormats = "SafeTensors",
            minRamGb = 12.0,
            hardwareRequirementsAr = "12 - 14 GB RAM مع مسرع كودا أو NPU مكتبي",
            hardwareRequirementsEn = "12 - 14 GB RAM with Desktop NPU/CUDA",
            compatibilityStatusAr = "يتجاوز حد 1.5 GB الصارم للهاتف",
            compatibilityStatusEn = "Exceeds strict 1.5 GB device limit",
            isCompatibleWithDeviceLimit = false,
            huggingFaceRepoUrl = "https://huggingface.co/THUDM/CogVideoX-2b",
            directDownloadUrl = "https://huggingface.co/THUDM/CogVideoX-2b/resolve/main/transformer/diffusion_pytorch_model.safetensors",
            directDownloadFileName = "diffusion_pytorch_model.safetensors",
            components = listOf(
                ModelFileComponent(
                    fileName = "transformer/diffusion_pytorch_model.safetensors",
                    roleAr = "خبير محول الانتشار ثلاثي الأبعاد (Expert DiT)",
                    roleEn = "3D Expert Diffusion Transformer",
                    sizeBytes = 2_200_000_000L,
                    sizeFormatted = "2.2 GB (INT4) / 4.4 GB (FP16)",
                    format = "SafeTensors",
                    descriptionAr = "محول انتشار ثلاثي الأبعاد مخصص للقطات السينمائية والإضاءة الواقعية",
                    descriptionEn = "3D diffusion transformer specialized in cinematic lighting and depth"
                ),
                ModelFileComponent(
                    fileName = "text_encoder/model.safetensors",
                    roleAr = "مشفر نصوص كامل (T5-XXL)",
                    roleEn = "Text Encoder (T5-XXL)",
                    sizeBytes = 4_800_000_000L,
                    sizeFormatted = "4.80 GB",
                    format = "SafeTensors",
                    descriptionAr = "استيعاب السياق السردي والأوصاف الطويلة بدقة عالية",
                    descriptionEn = "Full-precision narrative context processor"
                ),
                ModelFileComponent(
                    fileName = "vae/diffusion_pytorch_model.safetensors",
                    roleAr = "مفكك ترميز الفيديو السببي ثلاثي الأبعاد (3D Causal VAE)",
                    roleEn = "3D Causal VAE Decoder",
                    sizeBytes = 400_000_000L,
                    sizeFormatted = "400 MB",
                    format = "SafeTensors",
                    descriptionAr = "فك التشفير الزمكاني للإطارات دون فقدان التفاصيل الدقيقة",
                    descriptionEn = "Causal spatiotemporal decoding preventing frame artifacts"
                )
            ),
            notesAr = "نموذج صيني متقدم من جامعة تسينغهوا وZhipu AI. يتجاوز إمكانيات الهواتف الذكية بالكامل.",
            notesEn = "Advanced research model by Tsinghua & Zhipu AI. Substantially exceeds mobile device capabilities."
        ),

        // Model 4: LTX-Video (2B) - Heaviest (~6.94 GB)
        ResearchModelSpec(
            id = "ltx-video-2b",
            orderIndex = 4,
            name = "LTX-Video (2B)",
            taskAr = "توليد وتحويل صور ونصوص إلى فيديو (T2V & I2V)",
            taskEn = "Text-to-Video & Image-to-Video (T2V & I2V)",
            totalSizeBytes = 6_940_000_000L,
            totalSizeFormatted = "6.94 GB",
            fileFormats = "SafeTensors (FP8)",
            minRamGb = 12.0,
            hardwareRequirementsAr = "12 GB VRAM / معالج محطة عمل فائقة",
            hardwareRequirementsEn = "12 GB VRAM / High-End Workstation",
            compatibilityStatusAr = "يتجاوز حد 1.5 GB الصارم للهاتف",
            compatibilityStatusEn = "Exceeds strict 1.5 GB device limit",
            isCompatibleWithDeviceLimit = false,
            huggingFaceRepoUrl = "https://huggingface.co/Lightricks/LTX-Video",
            directDownloadUrl = "https://huggingface.co/Lightricks/LTX-Video/resolve/main/ltx-video-2b-v0.9.1.safetensors",
            directDownloadFileName = "ltx-video-2b-v0.9.1.safetensors",
            components = listOf(
                ModelFileComponent(
                    fileName = "ltx-video-2b-v0.9.1.safetensors",
                    roleAr = "محول الانتشار الفوري (Diffusion Transformer DiT 2B)",
                    roleEn = "Fast DiT Diffusion Transformer (2B)",
                    sizeBytes = 4_190_000_000L,
                    sizeFormatted = "4.19 GB (FP8)",
                    format = "SafeTensors",
                    descriptionAr = "محرك توليد سريع يدعم معدل إطارات سينمائي 24 FPS",
                    descriptionEn = "High-speed real-time DiT supporting 24 FPS cinematic output"
                ),
                ModelFileComponent(
                    fileName = "text_encoder/model.safetensors",
                    roleAr = "مشفر نصوص مكمم (T5-XXL FP8)",
                    roleEn = "Text Encoder (T5-XXL FP8)",
                    sizeBytes = 2_400_000_000L,
                    sizeFormatted = "2.40 GB",
                    format = "SafeTensors",
                    descriptionAr = "مشفر لغوي متكامل لضبط حركة الكاميرا والأشخاص",
                    descriptionEn = "Linguistic encoder tuned for camera motion and entity steering"
                ),
                ModelFileComponent(
                    fileName = "vae/diffusion_pytorch_model.safetensors",
                    roleAr = "مفكك فيديو عالي الدقة (Spatial-Temporal VAE)",
                    roleEn = "Spatial-Temporal VAE",
                    sizeBytes = 350_000_000L,
                    sizeFormatted = "350 MB",
                    format = "SafeTensors",
                    descriptionAr = "فك التشفير فائق السرعة بدقة 768x512",
                    descriptionEn = "Ultra-fast high-res spatial-temporal decoder"
                )
            ),
            notesAr = "أسرع نموذج DiT من أبحاث Lightricks. يبلغ إجمالي ملفاته 6.94 GB ويتطلب 12 GB ذاكرة رسومية.",
            notesEn = "Fast DiT model by Lightricks Research. Component bundle totals 6.94 GB requiring 12 GB VRAM."
        )
    )

    fun getSpecById(id: String): ResearchModelSpec? {
        return RESEARCH_MODELS.find { it.id == id }
    }
}
