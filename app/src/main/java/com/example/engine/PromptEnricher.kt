package com.example.engine

object PromptEnricher {

    private val cinematicLighting = listOf(
        "volumetric golden hour lighting",
        "dramatic cinematic rim lighting",
        "soft atmospheric mist with diffused ambient light",
        "hyper-detailed neon reflections with subtle lens flare",
        "moody chiaroscuro lighting with deep directional shadows"
    )

    private val cameraMotions = listOf(
        "smooth slow-motion tracking shot",
        "subtle cinematic push-in camera motion",
        "dynamic orbital panning shot",
        "fluid handheld aesthetic with gentle parallax",
        "low-angle dolly shot with depth of field"
    )

    private val visualFidelity = listOf(
        "photorealistic textures, 8k crisp details",
        "masterpiece quality, high dynamic range",
        "intricate surface details, cinematic motion blur"
    )

    /**
     * Refines and enriches a user's prompt by adding descriptive cinematic descriptors,
     * camera trajectories, and lighting conditions locally.
     * Note: This refines the prompt phrasing; it does not alter the underlying model weights.
     */
    fun enrichPrompt(basePrompt: String): String {
        val clean = basePrompt.trim().removeSuffix(".")
        if (clean.isBlank()) return basePrompt

        val lighting = cinematicLighting.random()
        val motion = cameraMotions.random()
        val fidelity = visualFidelity.random()

        return "$clean, $motion, $lighting, $fidelity"
    }
}
