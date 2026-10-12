package com.tumuyan.ncnn.realsr

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.media.MediaScannerConnection
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Catégories affichées dans la liste des « Techniques ». */
enum class Category { ANIME, PHOTO, CLASSIC }

/** Une technique = une ligne de commande du moteur (realsr-ncnn, realcugan-ncnn, …). */
data class Technique(
    val command: String,
    val label: String,
    val category: Category,
) {
    val id: String get() = command
}

/** Image importée dans le dossier de travail du moteur. */
data class ImportedImage(
    val displayName: String,
    val width: Int,
    val height: Int,
    val isAnimatedGif: Boolean,
    val frameCount: Int,
    val gifDelay: Int,
)

/**
 * Pont entre l'interface Compose et le moteur existant.
 *
 * Le mécanisme d'origine est conservé tel quel :
 *  - les assets `realsr/` sont copiés dans `cacheDir/realsr` (AssetsCopyer) ;
 *  - une commande shell (`./realsr-ncnn -i input.png -o output.png -m models-…`) est exécutée
 *    dans ce dossier par ImageProcessor / ProcessingService, avec LD_LIBRARY_PATH positionné ;
 *  - la progression est lue sur la sortie standard (lignes `NN.NN%`, ProgressLogHelper) ;
 *  - le résultat est le fichier `output.png` (ou le dossier `output.png/` pour un GIF animé).
 */
class ShinobiEngine(private val context: Context) {

    val root: String = context.cacheDir.absolutePath
    val dir: String = "$root/realsr"
    val inputFile = File(dir, "input.png")
    val outputFile = File(dir, "output.png")
    val outputGif = File(dir, "output.gif")

    @Volatile
    private var prepared = false

    /** Copie les binaires et les modèles depuis les assets (première utilisation : plusieurs secondes). */
    @Synchronized
    fun prepare() {
        if (prepared && File(dir, "realsr-ncnn").exists()) return
        val prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)
        val version = prefs.getInt("version", 0)
        AssetsCopyer.releaseAssets(context, "realsr", root, version == BuildConfig.VERSION_CODE)
        prefs.edit().putInt("version", BuildConfig.VERSION_CODE).apply()
        if (version != BuildConfig.VERSION_CODE) {
            shell("cd $dir;$CMD_CP_LIB_OPENCL; chmod +x *")
        } else {
            shell("chmod +x $dir -R")
        }
        prepared = true
    }

    /** Exécute une commande et attend la fin (équivalent de MainActivity.run_command). */
    fun shell(command: String): String {
        val out = StringBuilder()
        try {
            val p = ProcessBuilder("sh").redirectErrorStream(true).start()
            p.outputStream.use {
                it.write("cd $dir; export LD_LIBRARY_PATH=$dir\n".toByteArray())
                it.write("$command\n".toByteArray())
                it.write("exit\n".toByteArray())
                it.flush()
            }
            p.inputStream.bufferedReader().useLines { lines -> lines.forEach { out.appendLine(it) } }
            p.waitFor()
        } catch (e: Exception) {
            Log.e(TAG, "shell failed: $command", e)
        }
        return out.toString()
    }

    // ------------------------------------------------------------------ Techniques

    /** Construit la liste des techniques disponibles (modèles réellement présents). */
    fun loadTechniques(extraPath: String): List<Technique> {
        val res = context.resources
        val mgr = CommandListManager(
            res.getStringArray(R.array.style_array),
            extraPath.trim(),
            "",
            res.getString(R.string.default_classical_filters).trim().split(Regex("\\s+")).toTypedArray(),
            res.getString(R.string.default_magick_filters).trim().split(Regex("\\s+")).toTypedArray(),
        )
        val commands = mgr.commandList
        val labels = mgr.getDisplayLabels(false)
        val list = ArrayList<Technique>()
        for (i in commands.indices) {
            val cmd = commands[i]
            if (!isAvailable(cmd)) continue
            list += Technique(cmd, labels.getOrElse(i) { cmd }, categorize(cmd, labels.getOrElse(i) { "" }))
        }
        return list
    }

    /** Une technique est proposée seulement si son modèle existe (assets ou dossier perso). */
    private fun isAvailable(cmd: String): Boolean {
        val program = cmd.substringBefore(' ')
        if (!File(dir, program.removePrefix("./")).exists()) return false
        val model = Regex("\\s-m\\s+(\\S+)").find(cmd)?.groupValues?.get(1) ?: return true
        if (cmd.startsWith("./resize-ncnn") || cmd.startsWith("./magick")) return true
        val f = if (model.startsWith("/")) File(model) else File(dir, model)
        return f.exists()
    }

    private fun categorize(cmd: String, label: String): Category {
        if (cmd.startsWith("./resize-ncnn") || cmd.startsWith("./magick")) return Category.CLASSIC
        val s = (cmd + " " + label).lowercase(Locale.ROOT)
        val animeHints = listOf("anime", "cugan", "illustration", "manga", "waifu2x", "cunet", "upconv", "moesr", "sourcebook")
        val photoHints = listOf("photo", "realsr-", "df2k", "nomos", "general", "photo")
        return when {
            animeHints.any { s.contains(it) } && !s.contains("general") -> Category.ANIME
            photoHints.any { s.contains(it) } -> Category.PHOTO
            s.contains("real-esrgan") -> Category.PHOTO
            else -> Category.PHOTO
        }
    }

    /** Ajoute les options GPU/CPU et tuile, comme le faisait le bouton « Run » d'origine. */
    fun buildCommand(t: Technique, useCpu: Boolean, tile: Int): String {
        val head = t.command
        val cmd = StringBuilder(head)
        if (head.matches(Regex("./(realsr|srmd|waifu2x|realcugan|mnnsr)-ncnn.+"))) {
            if (tile > 0 && !head.contains(" -t ")) cmd.append(" -t ").append(tile)
            if (useCpu && !head.startsWith("./srmd") && !head.startsWith("./mnnsr") && !head.contains(" -g "))
                cmd.append(" -g -1")
            if (head.startsWith("./mnnsr") && !head.contains(" -b "))
                cmd.append(" -b ").append(if (useCpu) 0 else 3)
        }
        return cmd.toString()
    }

    // ------------------------------------------------------------------ Import

    fun queryName(uri: Uri): String {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) return c.getString(idx) ?: "image"
            }
        } catch (_: Exception) {
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "image"
    }

    /** Copie l'image choisie vers `input.png` (ou le dossier `input.png/` pour un GIF animé). */
    fun importImage(uri: Uri): ImportedImage? {
        prepare()
        deleteRecursive(inputFile)
        deleteRecursive(outputFile)
        deleteRecursive(outputGif)
        val tmp = File(dir, "tmp")
        tmp.delete()
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tmp).use { input.copyTo(it) }
            } ?: return null
        } catch (e: Exception) {
            Log.e(TAG, "import failed", e)
            return null
        }
        val head = ByteArray(4112)
        val n = tmp.inputStream().use { it.read(head) }
        if (n < 10) return null
        val match = PreprocessToPng.match(head.copyOf(n))
        val name = queryName(uri).substringBeforeLast('.')

        var animated = false
        var delay = 0
        var frames = 1
        when {
            match < 0 -> tmp.renameTo(inputFile)
            PreprocessToPng.isHeif(match) || PreprocessToPng.isAVIF(match) -> {
                val bmp = BitmapFactory.decodeFile(tmp.absolutePath) ?: return null
                FileOutputStream(inputFile).use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                bmp.recycle()
            }
            PreprocessToPng.isGIF(match) -> {
                animated = GifUntils.isGifAnimation(tmp.absolutePath)
                if (animated) {
                    delay = gifDelay(tmp)
                    animated = delay > 0
                }
                if (animated) {
                    inputFile.mkdirs()
                    shell("./magick tmp -coalesce -delay 0 input.png/%04d.png")
                    frames = inputFile.listFiles()?.size ?: 0
                    if (frames < 2) animated = false
                }
                if (!animated) {
                    deleteRecursive(inputFile)
                    shell("./magick tmp[0] input.png")
                }
            }
            else -> shell("./magick tmp ${ShellUtils.escapeShellArgument(inputFile.absolutePath)}")
        }
        tmp.delete()

        val sample = if (animated) inputFile.listFiles()?.minByOrNull { it.name } else inputFile
        if (sample == null || !sample.exists()) return null
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(sample.absolutePath, opts)
        if (opts.outWidth <= 0) return null
        return ImportedImage(name, opts.outWidth, opts.outHeight, animated, frames, delay)
    }

    /** Délai moyen d'une frame (centièmes de seconde), via `magick identify`, comme l'app d'origine. */
    private fun gifDelay(gif: File): Int {
        val out = shell("./magick identify -format \"%T \" ${ShellUtils.escapeShellArgument(gif.absolutePath)}")
        val values = out.trim().split(Regex("\\s+")).mapNotNull { it.toIntOrNull() }
        if (values.isEmpty()) return 0
        return values.average().toInt().coerceAtLeast(2)
    }

    // ------------------------------------------------------------------ Sauvegarde

    /** Enregistre le résultat dans la galerie (Pictures/ShinobiUpscaler). Retourne l'URI ou null. */
    fun saveToGallery(baseName: String, animated: Boolean): Uri? {
        val src = if (animated) outputGif else outputFile
        if (!src.isFile) return null
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val ext = if (animated) "gif" else "png"
        val mime = if (animated) "image/gif" else "image/png"
        val fileName = "${baseName}_shinobi_$stamp.$ext"
        val resolver = context.contentResolver
        return try {
            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, mime)
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/ShinobiUpscaler")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
                resolver.openOutputStream(uri)?.use { out -> src.inputStream().use { it.copyTo(out) } }
                    ?: return null
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri
            } else {
                @Suppress("DEPRECATION")
                val folder = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "ShinobiUpscaler",
                )
                folder.mkdirs()
                val target = File(folder, fileName)
                src.inputStream().use { i -> FileOutputStream(target).use { i.copyTo(it) } }
                MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf(mime), null)
                Uri.fromFile(target)
            }
        } catch (e: Exception) {
            Log.e(TAG, "save failed", e)
            null
        }
    }

    companion object {
        private const val TAG = "ShinobiEngine"

        /** Code identique à l'application d'origine (copie des bibliothèques OpenCL du système). */
        const val CMD_CP_LIB_OPENCL =
            " if [ -e /system/vendor/lib64/libOpenCL.so ]; then cp /system/vendor/lib64/libOpenCL.so ./; elif [ -e /system/lib64/libOpenCL.so ]; then cp /system/lib64/libOpenCL.so ./; elif  [ -e /system/vendor/lib/libOpenCL.so ]; then cp /system/vendor/lib/libOpenCL.so ./; elif [ -e /system/lib/libOpenCL.so ]; then cp /system/lib/libOpenCL.so ./; else echo \"[warning]libOpenCL.so not find\"; fi; if [ -e /system/vendor/lib/egl/libGLES_mali.so ]; then cp /system/vendor/lib/egl/libGLES_mali.so ./; elif [ -e /system/lib/egl/libGLES_mali.so ]; then cp /system/lib/egl/libGLES_mali.so ./; else echo \"[warning]libGLES_mali.so not find\"; fi"

        fun deleteRecursive(f: File) {
            if (f.isDirectory) f.listFiles()?.forEach { deleteRecursive(it) }
            f.delete()
        }
    }
}
