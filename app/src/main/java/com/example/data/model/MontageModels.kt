package com.example.data.model

import com.squareup.moshi.JsonClass

enum class TransitionType {
    NONE,       // قطع مباشر
    CROSSFADE,  // تلاشي بومض
    SLIDE_LEFT, // انزلاق جانبي
    ZOOM_IN     // تقريب بؤري
}

enum class FilterStyle {
    ORIGINAL,     // المصدر الأصلي
    GRAYSCALE,    // سينمائي كلاسيكي
    SEPIA,        // ذكريات عتيقة
    WARM_SUNSET,  // غروب دافئ
    CYBERPUNK,    // نيون سايبر بانك
    VINTAGE_GRAIN // سينما كلاسيكية تالفة
}

@JsonClass(generateAdapter = true)
data class ClipItem(
    val id: String,
    val title: String,
    val presetType: String,               // NATURE, NEON, VINTAGE, SPACE, CYBERPUNK, OCEAN
    val durationSeconds: Float = 4.0f,
    val filterStyle: FilterStyle = FilterStyle.ORIGINAL,
    val transitionType: TransitionType = TransitionType.NONE,
    val subtitleText: String = "",
    val subtitleStyle: String = "Elegant", // Elegant, Impact, Neon
    val isKenBurnsEnabled: Boolean = true
)

@JsonClass(generateAdapter = true)
data class MusicTrack(
    val id: String,
    val name: String,
    val author: String,
    val category: String, // CINEMATIC, LOFI, SYNTHWAVE, NATURE
    val tempoBpm: Int = 120
)

val PRESET_MUSIC_TRACKS = listOf(
    MusicTrack("m1", "صوت الملحمة السينمائية", "استوديو الألحان", "CINEMATIC", 90),
    MusicTrack("m2", "نبض المستقبل (سايبر)", "دوران رقمي", "SYNTHWAVE", 125),
    MusicTrack("m3", "لحن قهوة الصباح (لوفاي)", "لوفاي الاسترخاء", "LOFI", 80),
    MusicTrack("m4", "أصوات الغابة والرياح", "الطبيعة الأم", "NATURE", 60)
)
