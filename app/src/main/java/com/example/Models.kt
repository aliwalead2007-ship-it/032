package com.example

import androidx.compose.runtime.Immutable

@Immutable
data class IdeaAnalysis(
    val summary: String,
    val suggestedStyle: String = "",
    val goal: String = "",
    val targetAudience: String = "",
    val tone: String = "",
    val keywords: List<String> = emptyList(),
    val proposedScenes: List<String> = emptyList(),
    val viralityScore: Int = 80,
    val hookSuggestions: List<String> = emptyList(),
    val ctaSuggestions: List<String> = emptyList(),
    val confidence: Double = 1.0,
    val themes: List<String> = emptyList()
)

@Immutable
data class VideoStyleAnalysis(
    val detectedStyle: String = "",
    val dominantColors: String = "",
    val transitionSpeed: String = "",
    val movementPatterns: String = "",
    val overallRhythm: String = "",
    val audioStyle: String = "",
    val typographyStyle: String = "",
    val contentTone: String = "",
    val targetAudience: String = "",
    val keywords: List<String> = emptyList()
)

@Immutable
data class TrendingIdea(
    val title: String,
    val description: String,
    val viralityScore: Int = 80,
    val tags: List<String> = emptyList()
)

@Immutable
data class Template(
    val id: String = "",
    val name: String = "",
    val parameters: Map<String, String> = emptyMap(),
    val colors: String = "",
    val transitions: String = "",
    val textAnimation: String = ""
)

