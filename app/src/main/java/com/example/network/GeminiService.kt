package com.example.network

import com.example.BuildConfig
import com.example.data.model.ClipItem
import com.example.data.model.FilterStyle
import com.example.data.model.TransitionType
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class AiClipSuggestion(
    val title: String,
    val presetType: String,               // NATURE, NEON, VINTAGE, SPACE, CYBERPUNK, OCEAN
    val durationSeconds: Float = 4.0f,
    val filterStyle: String = "ORIGINAL", // ORIGINAL, GRAYSCALE, SEPIA, etc.
    val transitionType: String = "NONE",  // NONE, CROSSFADE, SLIDE_LEFT, ZOOM_IN
    val subtitleText: String = "",
    val subtitleStyle: String = "Elegant" // Elegant, Impact, Neon
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = "application/json",
    val temperature: Float? = 0.8f
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class PartResponse(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class ContentResponse(
    val parts: List<PartResponse>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: ContentResponse? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }

    val aiSuggestionAdapter = moshi.adapter<List<AiClipSuggestion>>(
        Types.newParameterizedType(java.util.List::class.java, AiClipSuggestion::class.java)
    )
}

object GeminiStoryboardRepository {
    suspend fun generateStoryboard(theme: String, musicType: String): List<ClipItem> {
        val apiKey = BuildConfig.GEMINI_API_KEY
        // Check if api key is mock/placeholder
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("placeholder", ignoreCase = true)) {
            // Fallback immediately to high-quality procedural storyboard generator
            return generateProceduralFallback(theme, musicType)
        }

        val promptJsonSchema = """
        [
            {
                "title": "scene descriptive short title",
                "presetType": "NATURE|NEON|VINTAGE|SPACE|CYBERPUNK|OCEAN",
                "durationSeconds": 4.0,
                "filterStyle": "ORIGINAL|GRAYSCALE|SEPIA|WARM_SUNSET|CYBERPUNK|VINTAGE_GRAIN",
                "transitionType": "NONE|CROSSFADE|SLIDE_LEFT|ZOOM_IN",
                "subtitleText": "elegant expressive subtitles/caption text in Arabic",
                "subtitleStyle": "Elegant|Impact|Neon"
            }
        ]
        """.trimIndent()

        val prompt = """
            You are a professional cinematographic storyboard director.
            Generate a sequence of 3 to 5 scenes for a montage.
            Theme of the montage: "$theme"
            Audio Soundtrack chosen by user: "$musicType"
            
            Return a JSON array of scenes matching this exact schema:
            $promptJsonSchema
            
            CRITICAL RULES:
            - The "subtitleText" MUST be in poetic, beautiful, and engaging Arabic language suited for social media status/reel.
            - Ensure diverse presetType and custom fitting filters matching the mood of the theme.
            - The JSON syntax must be 100% valid. Return ONLY the JSON array. Put nothing else in your response.
        """.trimIndent()

        try {
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                systemInstruction = Content(parts = listOf(Part(text = "You are a professional media editing AI assistant. You output raw valid JSON as requested. Do not wrap in markdown tags like ```json."))),
                generationConfig = GenerationConfig()
            )

            val apiResponse = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = apiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("No response text")

            // Clean markdown blocks if returned by model despite instructions
            val cleanedJson = jsonText.substringAfter("```json")
                .substringBefore("```")
                .trim()

            val suggestions = RetrofitClient.aiSuggestionAdapter.fromJson(cleanedJson) ?: emptyList()
            return suggestions.mapIndexed { index, item ->
                ClipItem(
                    id = "ai_${index}_${System.currentTimeMillis()}",
                    title = item.title,
                    presetType = when (item.presetType.uppercase()) {
                        "NATURE" -> "NATURE"
                        "NEON" -> "NEON"
                        "VINTAGE" -> "VINTAGE"
                        "SPACE" -> "SPACE"
                        "CYBERPUNK" -> "CYBERPUNK"
                        "OCEAN" -> "OCEAN"
                        else -> "NATURE"
                    },
                    durationSeconds = if (item.durationSeconds in 1.0f..15.0f) item.durationSeconds else 4.0f,
                    filterStyle = try {
                        FilterStyle.valueOf(item.filterStyle.uppercase())
                    } catch (e: Exception) {
                        FilterStyle.ORIGINAL
                    },
                    transitionType = try {
                        TransitionType.valueOf(item.transitionType.uppercase())
                    } catch (e: Exception) {
                        TransitionType.NONE
                    },
                    subtitleText = item.subtitleText,
                    subtitleStyle = if (item.subtitleStyle in listOf("Elegant", "Impact", "Neon")) item.subtitleStyle else "Elegant"
                )
            }
        } catch (e: Exception) {
            // Procedural fallback
            return generateProceduralFallback(theme, musicType)
        }
    }

    private fun generateProceduralFallback(theme: String, musicType: String): List<ClipItem> {
        val tId = System.currentTimeMillis()
        val suffix = if (theme.isNotBlank()) " - $theme" else ""
        return listOf(
            ClipItem(
                id = "f1_$tId",
                title = "المشهد الافتتاحي$suffix",
                presetType = "NATURE",
                durationSeconds = 4.0f,
                filterStyle = FilterStyle.WARM_SUNSET,
                transitionType = TransitionType.CROSSFADE,
                subtitleText = "أهلاً بك في فصول الحكاية الملهمة",
                subtitleStyle = "Elegant"
            ),
            ClipItem(
                id = "f2_$tId",
                title = "ممر المستقبل ومقاطع النور",
                presetType = "NEON",
                durationSeconds = 3.5f,
                filterStyle = FilterStyle.CYBERPUNK,
                transitionType = TransitionType.SLIDE_LEFT,
                subtitleText = "حيث تلتقي التفاصيل العميقة بالأضواء الساحرة",
                subtitleStyle = "Neon"
            ),
            ClipItem(
                id = "f3_$tId",
                title = "المشهد الختامي العتيق",
                presetType = "VINTAGE",
                durationSeconds = 5.0f,
                filterStyle = FilterStyle.VINTAGE_GRAIN,
                transitionType = TransitionType.ZOOM_IN,
                subtitleText = "وتبقى الذكريات منقوشة في ثنايا القلوب",
                subtitleStyle = "Elegant"
            )
        )
    }
}
