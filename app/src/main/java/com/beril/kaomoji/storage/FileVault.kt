package com.beril.kaomoji.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

/**
 * User files (audio, exports, backups) live wherever the user chooses via SAF.
 * App data (state.json) stays in internal storage. If no folder is chosen we
 * fall back to app-private external files so recording always works.
 */
class FileVault(private val ctx: Context, private val treeUriString: String?) {

    companion object {
        const val ROOT = "(≧▽≦)"
        val FOLDERS = listOf("Audio", "Transcripts", "Exports", "Backups", "Generated", "Projects")
    }

    private val tree: DocumentFile?
        get() = treeUriString?.let {
            try {
                DocumentFile.fromTreeUri(ctx, Uri.parse(it))
            } catch (_: Exception) {
                null
            }
        }

    val isExternal: Boolean get() = tree?.canWrite() == true

    fun displayPath(): String {
        val t = tree ?: return "Uygulama klasörü (varsayılan)"
        val name = t.name ?: "seçilen klasör"
        return "$name / $ROOT"
    }

    /** Creates (≧▽≦)/ and its subfolders inside the chosen tree. */
    fun ensureStructure(): Boolean {
        val t = tree ?: return false
        return try {
            val root = t.findFile(ROOT)?.takeIf { it.isDirectory } ?: t.createDirectory(ROOT)
            ?: return false
            FOLDERS.forEach { f ->
                if (root.findFile(f)?.isDirectory != true) root.createDirectory(f)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun folder(name: String): DocumentFile? {
        val t = tree ?: return null
        val root = t.findFile(ROOT)?.takeIf { it.isDirectory } ?: t.createDirectory(ROOT)
        ?: return null
        return root.findFile(name)?.takeIf { it.isDirectory } ?: root.createDirectory(name)
    }

    private fun localDir(name: String): File {
        val d = File(ctx.getExternalFilesDir(null) ?: ctx.filesDir, "$ROOT/$name")
        if (!d.exists()) d.mkdirs()
        return d
    }

    fun createAudioFile(fileName: String): Uri? {
        folder("Audio")?.let { dir ->
            val f = dir.createFile("audio/mp4", fileName)
            if (f != null) return f.uri
        }
        val f = File(localDir("Audio"), "$fileName.m4a")
        return try {
            f.createNewFile(); Uri.fromFile(f)
        } catch (_: Exception) {
            null
        }
    }

    fun writeText(sub: String, fileName: String, mime: String, content: String): Uri? {
        folder(sub)?.let { dir ->
            try {
                val existing = dir.findFile(fileName)
                existing?.delete()
                val f = dir.createFile(mime, fileName)
                if (f != null) {
                    ctx.contentResolver.openOutputStream(f.uri)?.use {
                        it.write(content.toByteArray())
                    }
                    return f.uri
                }
            } catch (_: Exception) {
            }
        }
        return try {
            val f = File(localDir(sub), fileName)
            f.writeText(content)
            Uri.fromFile(f)
        } catch (_: Exception) {
            null
        }
    }

    fun readText(uri: Uri): String? = try {
        if (uri.scheme == "content")
            ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        else File(uri.path!!).readText()
    } catch (_: Exception) {
        null
    }

    fun deleteFile(uriString: String): Boolean = try {
        val u = Uri.parse(uriString)
        if (u.scheme == "content") DocumentFile.fromSingleUri(ctx, u)?.delete() ?: false
        else File(u.path!!).delete()
    } catch (_: Exception) {
        false
    }

    /** bytes per folder */
    fun usage(): Map<String, Long> {
        val out = linkedMapOf<String, Long>()
        FOLDERS.forEach { name ->
            var sum = 0L
            folder(name)?.listFiles()?.forEach { sum += it.length() }
            val local = File(ctx.getExternalFilesDir(null) ?: ctx.filesDir, "$ROOT/$name")
            if (local.exists()) local.listFiles()?.forEach { sum += it.length() }
            out[name] = sum
        }
        return out
    }
}

fun humanSize(bytes: Long): String = when {
    bytes <= 0 -> "—"
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
}
