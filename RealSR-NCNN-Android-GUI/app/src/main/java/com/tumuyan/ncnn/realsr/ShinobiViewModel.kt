package com.tumuyan.ncnn.realsr

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Environment
import android.os.IBinder
import android.provider.DocumentsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class Screen { MAIN, SETTINGS, ABOUT }

enum class Phase { PREPARING, IDLE, IMPORTING, RUNNING, DONE, FAILED }

data class UiState(
    val screen: Screen = Screen.MAIN,
    val phase: Phase = Phase.PREPARING,
    val image: ImportedImage? = null,
    val imageVersion: Int = 0,
    val techniques: List<Technique> = emptyList(),
    val category: Category = Category.ANIME,
    val selectedId: String = "",
    /** 0f..1f ; négatif = indéterminé */
    val progress: Float = -1f,
    val elapsedSeconds: Int = 0,
    val log: String = "",
    val resultVersion: Int = 0,
    val beforePath: String? = null,
    val afterPath: String? = null,
    val animatedGifPath: String? = null,
    val useCpu: Boolean = false,
    val extraPath: String = "",
    val tile: Int = 0,
    val message: String? = null,
) {
    val selected: Technique? get() = techniques.firstOrNull { it.id == selectedId }
    val visibleTechniques: List<Technique> get() = techniques.filter { it.category == category }
}

class ShinobiViewModel(app: Application) : AndroidViewModel(app) {

    private val engine = ShinobiEngine(app)
    private val prefs = app.getSharedPreferences("shinobi", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        UiState(
            useCpu = prefs.getBoolean(KEY_CPU, false),
            extraPath = prefs.getString(KEY_EXTRA, "") ?: "",
            tile = prefs.getInt(KEY_TILE, 0),
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val service = MutableStateFlow<ProcessingService?>(null)
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service.value = (binder as ProcessingService.LocalBinder).service
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service.value = null
        }
    }

    @Volatile
    private var cancelled = false
    private var startedAt = 0L
    private var gifFrames = 0
    private var framesDone = 0
    private var lastPercent = 0f
    private val progressLog = ProgressLogHelper()

    init {
        app.bindService(Intent(app, ProcessingService::class.java), connection, Context.BIND_AUTO_CREATE)
        viewModelScope.launch(Dispatchers.IO) {
            engine.prepare()
            refreshTechniques(initial = true)
            _state.update { it.copy(phase = Phase.IDLE) }
        }
    }

    // ------------------------------------------------------------------ Navigation / réglages

    fun go(screen: Screen) = _state.update { it.copy(screen = screen) }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun toast(text: String) = _state.update { it.copy(message = text) }

    fun selectCategory(c: Category) = _state.update { it.copy(category = c) }

    fun selectTechnique(t: Technique) {
        prefs.edit().putString(KEY_SELECTED, t.id).apply()
        _state.update { it.copy(selectedId = t.id, category = t.category) }
    }

    fun setUseCpu(v: Boolean) {
        prefs.edit().putBoolean(KEY_CPU, v).apply()
        _state.update { it.copy(useCpu = v) }
    }

    fun setTile(v: Int) {
        prefs.edit().putInt(KEY_TILE, v).apply()
        _state.update { it.copy(tile = v) }
    }

    fun setExtraPath(path: String) {
        val clean = path.trim()
        prefs.edit().putString(KEY_EXTRA, clean).apply()
        _state.update { it.copy(extraPath = clean) }
        viewModelScope.launch(Dispatchers.IO) { refreshTechniques(initial = false) }
    }

    /** Convertit un dossier choisi via le sélecteur système en chemin utilisable par le moteur. */
    fun setExtraPathFromTree(uri: Uri) {
        val docId = try {
            DocumentsContract.getTreeDocumentId(uri)
        } catch (_: Exception) {
            null
        }
        val parts = docId?.split(":", limit = 2)
        if (parts != null && parts.size == 2 && parts[0].equals("primary", ignoreCase = true)) {
            @Suppress("DEPRECATION")
            val base = Environment.getExternalStorageDirectory().absolutePath
            setExtraPath(File(base, parts[1]).absolutePath)
        } else {
            toast("Ce dossier n'est pas sur le stockage interne : saisis son chemin à la main.")
        }
    }

    private fun refreshTechniques(initial: Boolean) {
        val list = engine.loadTechniques(_state.value.extraPath)
        val saved = prefs.getString(KEY_SELECTED, null)
        val current = _state.value.selectedId
        val defaultCmd = CommandListManager.COMMAND_0[0] // real-esrgan-anime-x4
        val pick = list.firstOrNull { it.id == current }
            ?: list.firstOrNull { it.id == saved }
            ?: list.firstOrNull { it.id == defaultCmd }
            ?: list.firstOrNull()
        _state.update {
            it.copy(
                techniques = list,
                selectedId = pick?.id ?: "",
                category = pick?.category ?: it.category,
            )
        }
        if (initial && pick == null) toast("Aucun modèle trouvé : les assets du moteur sont-ils présents ?")
    }

    // ------------------------------------------------------------------ Parchemin (import)

    fun importImage(uri: Uri) {
        if (_state.value.phase == Phase.RUNNING) return
        _state.update { it.copy(phase = Phase.IMPORTING, log = "", progress = -1f) }
        viewModelScope.launch(Dispatchers.IO) {
            val img = engine.importImage(uri)
            if (img == null) {
                _state.update {
                    it.copy(phase = Phase.IDLE, image = null, message = "Impossible de lire ce parchemin (image).")
                }
            } else {
                val before = firstFrame(engine.inputFile)
                _state.update {
                    it.copy(
                        phase = Phase.IDLE,
                        image = img,
                        imageVersion = it.imageVersion + 1,
                        beforePath = before,
                        afterPath = null,
                        animatedGifPath = null,
                        progress = -1f,
                    )
                }
            }
        }
    }

    private fun firstFrame(f: File): String? =
        if (f.isDirectory) f.listFiles()?.filter { it.isFile }?.minByOrNull { it.name }?.absolutePath
        else if (f.exists()) f.absolutePath else null

    // ------------------------------------------------------------------ Lancer le jutsu

    fun launchJutsu() {
        val s = _state.value
        val img = s.image ?: return toast("Choisis d'abord un parchemin.")
        val tech = s.selected ?: return toast("Choisis une technique.")
        if (s.phase == Phase.RUNNING) return

        cancelled = false
        gifFrames = if (img.isAnimatedGif) img.frameCount else 0
        framesDone = 0
        lastPercent = 0f
        startedAt = System.currentTimeMillis()
        progressLog.reset()
        _state.update {
            it.copy(
                phase = Phase.RUNNING, progress = -1f, elapsedSeconds = 0, log = "",
                afterPath = null, animatedGifPath = null,
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            ShinobiEngine.deleteRecursive(engine.outputFile)
            ShinobiEngine.deleteRecursive(engine.outputGif)
            if (img.isAnimatedGif) engine.outputFile.mkdirs()

            var cmd = engine.buildCommand(tech, s.useCpu, s.tile)
            if (img.isAnimatedGif) {
                cmd += " && ./magick -delay ${img.gifDelay} output.png/* -loop 0 output.gif"
            }

            val app = getApplication<Application>()
            app.startService(Intent(app, ProcessingService::class.java))
            val svc = service.filterNotNull().first()

            svc.startTask(cmd, engine.dir, 2, object : ImageProcessor.ProcessCallback {
                override fun onProgress(line: String) {
                    progressLog.appendLine(line)
                    val elapsed = ((System.currentTimeMillis() - startedAt) / 1000).toInt()
                    if (ProgressLogHelper.isProgressLine(line)) {
                        val pct = line.trim().substringBefore('%').toFloatOrNull()?.div(100f)
                        if (pct != null) {
                            // En mode GIF, le moteur recommence à 0 % à chaque image.
                            if (gifFrames > 1 && lastPercent > 0.9f && pct < 0.5f) framesDone++
                            lastPercent = pct
                            val overall = if (gifFrames > 1) (framesDone + pct) / gifFrames else pct
                            _state.update {
                                it.copy(progress = overall.coerceIn(0f, 1f), elapsedSeconds = elapsed)
                            }
                            return
                        }
                    }
                    _state.update { it.copy(log = progressLog.fullLog, elapsedSeconds = elapsed) }
                }

                override fun onCompleted(result: String, success: Boolean) {
                    val ok = success && hasOutput(img.isAnimatedGif)
                    if (ok) {
                        val after = firstFrame(engine.outputFile)
                        val gif = if (img.isAnimatedGif && engine.outputGif.isFile) engine.outputGif.absolutePath else null
                        _state.update {
                            it.copy(
                                phase = Phase.DONE, progress = 1f, log = progressLog.fullLog,
                                resultVersion = it.resultVersion + 1, afterPath = after, animatedGifPath = gif,
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                phase = Phase.FAILED, progress = -1f, log = progressLog.fullLog,
                                message = "Le jutsu a échoué. Essaie le mode CPU dans les réglages.",
                            )
                        }
                    }
                }

                override fun onError(error: String?) {
                    if (cancelled) {
                        _state.update { it.copy(phase = Phase.IDLE, progress = -1f, message = "Jutsu annulé.") }
                    } else {
                        _state.update {
                            it.copy(phase = Phase.FAILED, progress = -1f, message = "Erreur : ${error ?: "inconnue"}")
                        }
                    }
                }
            })
        }
    }

    private fun hasOutput(animated: Boolean): Boolean {
        val o = engine.outputFile
        return if (animated) o.isDirectory && (o.listFiles()?.isNotEmpty() == true) && engine.outputGif.isFile
        else o.isFile && o.length() > 0
    }

    fun cancelJutsu() {
        cancelled = true
        service.value?.cancelTask()
        _state.update { it.copy(phase = Phase.IDLE, progress = -1f, message = "Jutsu annulé.") }
    }

    // ------------------------------------------------------------------ Enregistrer

    fun saveToGallery() {
        val s = _state.value
        val img = s.image ?: return
        if (s.phase != Phase.DONE) return toast("Rien à enregistrer pour l'instant.")
        viewModelScope.launch {
            val uri = withContext(Dispatchers.IO) { engine.saveToGallery(img.displayName, img.isAnimatedGif) }
            toast(
                if (uri != null) "Enregistré dans la galerie (Pictures/ShinobiUpscaler)."
                else "Échec de l'enregistrement."
            )
        }
    }

    override fun onCleared() {
        try {
            getApplication<Application>().unbindService(connection)
        } catch (_: Exception) {
        }
        super.onCleared()
    }

    companion object {
        private const val KEY_CPU = "useCpu"
        private const val KEY_EXTRA = "extraPath"
        private const val KEY_TILE = "tile"
        private const val KEY_SELECTED = "selected"
    }
}
