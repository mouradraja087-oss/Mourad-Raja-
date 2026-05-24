package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.MontageProject
import com.example.data.model.ClipItem
import com.example.data.model.FilterStyle
import com.example.data.model.TransitionType
import com.example.network.GeminiStoryboardRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MontageViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.montageDao()

    // 1. Projects State from Database
    val allProjects: StateFlow<List<MontageProject>> = dao.getAllProjects()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 2. Editing Board State
    private val _activeProject = MutableStateFlow<MontageProject?>(null)
    val activeProject: StateFlow<MontageProject?> = _activeProject.asStateFlow()

    // Screen State: "DASHBOARD" or "EDITOR"
    private val _currentScreen = MutableStateFlow("DASHBOARD")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Playback state
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTimelineSeconds = MutableStateFlow(0f)
    val currentTimelineSeconds: StateFlow<Float> = _currentTimelineSeconds.asStateFlow()

    // Audio frequency visual waves
    private val _equalizerWaves = MutableStateFlow(List(45) { 0.15f })
    val equalizerWaves: StateFlow<List<Float>> = _equalizerWaves.asStateFlow()

    // Selected Clip for Inspector editing
    private val _selectedClipId = MutableStateFlow<String?>(null)
    val selectedClipId: StateFlow<String?> = _selectedClipId.asStateFlow()

    // Gemini Loading State
    private val _isGeminiLoading = MutableStateFlow(false)
    val isGeminiLoading: StateFlow<Boolean> = _isGeminiLoading.asStateFlow()

    private val _geminiError = MutableStateFlow<String?>(null)
    val geminiError: StateFlow<String?> = _geminiError.asStateFlow()

    // Exporting simulation state
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportProgress = MutableStateFlow(0)
    val exportProgress: StateFlow<Int> = _exportProgress.asStateFlow()

    private val _exportLog = MutableStateFlow<List<String>>(emptyList())
    val exportLog: StateFlow<List<String>> = _exportLog.asStateFlow()

    private val _exportFinishedVideo = MutableStateFlow<String?>(null) // Video properties if exported
    val exportFinishedVideo: StateFlow<String?> = _exportFinishedVideo.asStateFlow()

    private var playJob: Job? = null

    init {
        // Run wave heartbeat
        viewModelScope.launch {
            while (isActive) {
                delay(120)
                if (_isPlaying.value) {
                    _equalizerWaves.value = List(45) {
                        (0.15f + Math.sin(_currentTimelineSeconds.value.toDouble() * 9.0 + it).toFloat() * 0.35f + (Math.random().toFloat() * 0.5f)).coerceIn(0.1f, 1.0f)
                    }
                } else {
                    _equalizerWaves.value = _equalizerWaves.value.map { (it * 0.9f).coerceAtLeast(0.1f) }
                }
            }
        }
    }

    // Navigation and Routing
    fun navigateToEditor(project: MontageProject) {
        _activeProject.value = project
        _currentScreen.value = "EDITOR"
        _currentTimelineSeconds.value = 0f
        _selectedClipId.value = project.clips.firstOrNull()?.id
        stopPlayback()
    }

    fun navigateToDashboard() {
        _currentScreen.value = "DASHBOARD"
        _activeProject.value = null
        stopPlayback()
    }

    // Playback loop controller
    fun togglePlay() {
        val nextPlay = !_isPlaying.value
        _isPlaying.value = nextPlay
        if (nextPlay) {
            startPlayLoop()
        } else {
            stopPlayback()
        }
    }

    private fun startPlayLoop() {
        playJob?.cancel()
        playJob = viewModelScope.launch {
            var lastTime = System.currentTimeMillis()
            while (isActive) {
                delay(16) // ~60 FPS
                if (!_isPlaying.value) break
                val now = System.currentTimeMillis()
                val delta = (now - lastTime) / 1000f
                lastTime = now

                val total = getTotalDuration()
                if (total > 0f) {
                    val updatedTime = _currentTimelineSeconds.value + delta
                    if (updatedTime >= total) {
                        _currentTimelineSeconds.value = 0f // Loop back
                    } else {
                        _currentTimelineSeconds.value = updatedTime
                    }
                } else {
                    _currentTimelineSeconds.value = 0f
                }
            }
        }
    }

    fun stopPlayback() {
        _isPlaying.value = false
        playJob?.cancel()
    }

    fun seekToPosition(positionSeconds: Float) {
        val total = getTotalDuration()
        _currentTimelineSeconds.value = positionSeconds.coerceIn(0f, total)
    }

    fun getTotalDuration(): Float {
        return _activeProject.value?.clips?.sumOf { it.durationSeconds.toDouble() }?.toFloat() ?: 0f
    }

    // Get current clip and transition state based on total timeline timeline seconds progress
    fun getTimelineClipRenderingState(): RenderingClipState {
        val project = _activeProject.value ?: return RenderingClipState(null, null, 0f, 0f, TransitionType.NONE)
        val targetTime = _currentTimelineSeconds.value
        val clips = project.clips
        if (clips.isEmpty()) return RenderingClipState(null, null, 0f, 0f, TransitionType.NONE)

        var accumulatedTime = 0f
        var currentClipIndex = -1

        for (i in clips.indices) {
            val clipDuration = clips[i].durationSeconds
            if (targetTime >= accumulatedTime && targetTime <= accumulatedTime + clipDuration) {
                currentClipIndex = i
                break
            }
            accumulatedTime += clipDuration
        }

        if (currentClipIndex == -1) {
            currentClipIndex = clips.lastIndex
            accumulatedTime = clips.sumOf { it.durationSeconds.toDouble() }.toFloat() - clips.last().durationSeconds
        }

        val primaryClip = clips[currentClipIndex]
        val clipStartTimelineTime = accumulatedTime
        val clipProgressSeconds = targetTime - clipStartTimelineTime
        val clipDuration = primaryClip.durationSeconds
        val primaryPercent = if (clipDuration > 0) clipProgressSeconds / clipDuration else 0f

        // Transition window checks: look at end of current clip transitioning to next
        val transitionDuration = 0.8f // 0.8 second transition zone
        val timeRemainingInClip = clipDuration - clipProgressSeconds

        if (timeRemainingInClip <= transitionDuration && currentClipIndex < clips.lastIndex) {
            val incomingClip = clips[currentClipIndex + 1]
            val transProgress = (transitionDuration - timeRemainingInClip) / transitionDuration
            return RenderingClipState(
                currentClip = primaryClip,
                nextClip = incomingClip,
                currentClipProgressPercent = primaryPercent,
                transitionProgressPercent = transProgress,
                transitionStyle = primaryClip.transitionType
            )
        }

        return RenderingClipState(
            currentClip = primaryClip,
            nextClip = null,
            currentClipProgressPercent = primaryPercent,
            transitionProgressPercent = 0f,
            transitionStyle = TransitionType.NONE
        )
    }

    // Projects CRUD
    fun createAndLoadNewProject(name: String) {
        viewModelScope.launch {
            val initialClips = listOf(
                ClipItem(
                    id = "cl1_${System.currentTimeMillis()}",
                    title = "لقطة البداية العريضة",
                    presetType = "NATURE",
                    durationSeconds = 4.0f,
                    filterStyle = FilterStyle.WARM_SUNSET,
                    transitionType = TransitionType.CROSSFADE,
                    subtitleText = "رحلة في ثنايا الجمال الفريد",
                    subtitleStyle = "Elegant"
                ),
                ClipItem(
                    id = "cl2_${System.currentTimeMillis()}",
                    title = "شارع الأضواء الكثيفة",
                    presetType = "NEON",
                    durationSeconds = 4.0f,
                    filterStyle = FilterStyle.CYBERPUNK,
                    transitionType = TransitionType.SLIDE_LEFT,
                    subtitleText = "ألوان المستقبل المتوهج",
                    subtitleStyle = "Neon"
                ),
                ClipItem(
                    id = "cl3_${System.currentTimeMillis()}",
                    title = "مشهد كلاسيكي دافئ",
                    presetType = "VINTAGE",
                    durationSeconds = 5.0f,
                    filterStyle = FilterStyle.VINTAGE_GRAIN,
                    transitionType = TransitionType.NONE,
                    subtitleText = "لحظة تروى بألف حكاية وحكاية",
                    subtitleStyle = "Elegant"
                )
            )

            val draft = MontageProject(
                name = name,
                musicTrackId = "m2",
                clips = initialClips
            )

            val newId = dao.insertProject(draft)
            val insertedProject = dao.getProjectById(newId.toInt())
            if (insertedProject != null) {
                navigateToEditor(insertedProject)
            }
        }
    }

    fun selectClip(id: String) {
        _selectedClipId.value = id
    }

    fun addClipToActive(presetType: String) {
        val project = _activeProject.value ?: return
        val newClip = ClipItem(
            id = "cl_${System.currentTimeMillis()}",
            title = when(presetType) {
                "NATURE" -> "طبيعة ساحرة جديدة"
                "NEON" -> "إضاءة نيون ونبض جرافيكي"
                "VINTAGE" -> "مشهد ذكريات سينمي"
                "SPACE" -> "التحليق بين الكواكب"
                "CYBERPUNK" -> "إيقاع سيبراني مستقبلي"
                "OCEAN" -> "محيط دافئ هادئ"
                else -> "لقطة جديدة مضافة"
            },
            presetType = presetType,
            durationSeconds = 4.0f
        )
        val updatedClips = project.clips + newClip
        val updatedProject = project.copy(clips = updatedClips)
        _activeProject.value = updatedProject
        _selectedClipId.value = newClip.id
        saveProjectToDb(updatedProject)
    }

    fun updateClipInActive(updatedClip: ClipItem) {
        val project = _activeProject.value ?: return
        val updatedClips = project.clips.map { if (it.id == updatedClip.id) updatedClip else it }
        val updatedProject = project.copy(clips = updatedClips)
        _activeProject.value = updatedProject
        saveProjectToDb(updatedProject)
    }

    fun deleteClipFromActive(clipId: String) {
        val project = _activeProject.value ?: return
        val updatedClips = project.clips.filter { it.id != clipId }
        val updatedProject = project.copy(clips = updatedClips)
        _activeProject.value = updatedProject
        if (_selectedClipId.value == clipId) {
            _selectedClipId.value = updatedClips.firstOrNull()?.id
        }
        _currentTimelineSeconds.value = 0f
        saveProjectToDb(updatedProject)
    }

    fun reorderClips(clips: List<ClipItem>) {
        val project = _activeProject.value ?: return
        val updatedProject = project.copy(clips = clips)
        _activeProject.value = updatedProject
        saveProjectToDb(updatedProject)
    }

    fun updateProjectSettings(name: String, trackId: String, volume: Float, aspectRatio: String) {
        val project = _activeProject.value ?: return
        val updatedProject = project.copy(
            name = name,
            musicTrackId = trackId,
            musicVolume = volume,
            aspectRatio = aspectRatio
        )
        _activeProject.value = updatedProject
        saveProjectToDb(updatedProject)
    }

    fun deleteProject(projectId: Int) {
        viewModelScope.launch {
            dao.deleteProjectById(projectId)
            if (_activeProject.value?.id == projectId) {
                navigateToDashboard()
            }
        }
    }

    private fun saveProjectToDb(project: MontageProject) {
        viewModelScope.launch {
            dao.updateProject(project)
        }
    }

    // AI smart script generation
    fun generateAiStoryboard(theme: String, musicType: String) {
        _isGeminiLoading.value = true
        _geminiError.value = null
        viewModelScope.launch {
            try {
                val generatedClips = GeminiStoryboardRepository.generateStoryboard(theme, musicType)
                if (generatedClips.isNotEmpty()) {
                    val newProject = MontageProject(
                        name = if (theme.isNotBlank()) "سيناريو ذكي - $theme" else "سيناريو ذكاء اصطناعي",
                        musicTrackId = when (musicType.uppercase()) {
                            "CINEMATIC" -> "m1"
                            "SYNTHWAVE" -> "m2"
                            "LOFI" -> "m3"
                            "NATURE" -> "m4"
                            else -> "m2"
                        },
                        clips = generatedClips
                    )
                    val insertedId = dao.insertProject(newProject)
                    val loadedProject = dao.getProjectById(insertedId.toInt())
                    if (loadedProject != null) {
                        navigateToEditor(loadedProject)
                    }
                } else {
                    _geminiError.value = "لم يتم إنشاء لقطات للمونتاج"
                }
            } catch (e: Exception) {
                _geminiError.value = "حدث خطأ أثناء التصميم الذكي: ${e.message}"
            } finally {
                _isGeminiLoading.value = false
            }
        }
    }

    // High performance compilation simulator
    fun startExportSimulation(resolution: String, format: String, frameRate: String) {
        _isExporting.value = true
        _exportProgress.value = 0
        _exportFinishedVideo.value = null
        
        val logs = mutableListOf<String>()
        _exportLog.value = logs

        viewModelScope.launch {
            stopPlayback()
            val steps = listOf(
                Pair(10, "البدء: جاري فك حزم أصول الفيديو الثابتة..."),
                Pair(25, "فحص المسارات: جاري قراءة خط كود الزمن البصري..."),
                Pair(40, "فلاتر الألوان: تخليق طبقات فلاتر الرندر المتشعبة..."),
                Pair(60, "الانتقالات: محاذاة تنقلات $format وتعديل الإطارات العالية..."),
                Pair(75, "هندسة الصوت: تركيب المقطع وتطبيق خوارزميات الضغط الديناميكي..."),
                Pair(90, "الترميز: ترميز الفيديو بدقة $resolution تحت معدل إطارات $frameRate..."),
                Pair(100, "اكتمال رائع: كبس حزم الفيديو بنجاح! تم الحفظ في المعرض الموقّت.")
            )

            for (step in steps) {
                delay(800)
                _exportProgress.value = step.first
                logs.add(0, step.second) // Add to top of list
                _exportLog.value = logs.toList()
            }

            delay(650)
            val activeProj = _activeProject.value
            if (activeProj != null) {
                val finishedProj = activeProj.copy(isExported = true)
                _activeProject.value = finishedProj
                dao.updateProject(finishedProj)
            }
            
            _exportFinishedVideo.value = "v_${System.currentTimeMillis()}_$resolution.$format"
        }
    }

    fun dismissExport() {
        _isExporting.value = false
        _exportProgress.value = 0
        _exportLog.value = emptyList()
        _exportFinishedVideo.value = null
    }
}

// Represent active state in playback rendering
data class RenderingClipState(
    val currentClip: ClipItem?,
    val nextClip: ClipItem?,
    val currentClipProgressPercent: Float,
    val transitionProgressPercent: Float,
    val transitionStyle: TransitionType
)
