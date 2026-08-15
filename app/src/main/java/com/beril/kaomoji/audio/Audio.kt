package com.beril.kaomoji.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beril.kaomoji.storage.FileVault
import java.io.File

class Recorder(private val ctx: Context) {

    private var recorder: MediaRecorder? = null
    private var pfd: ParcelFileDescriptor? = null
    private var startedAt = 0L
    private var pausedTotal = 0L
    private var pausedAt = 0L

    var isRecording by mutableStateOf(false)
        private set
    var isPaused by mutableStateOf(false)
        private set
    var outputUri: String? = null
        private set

    fun elapsedMs(): Long {
        if (!isRecording) return 0L
        val now = System.currentTimeMillis()
        val paused = pausedTotal + if (isPaused) now - pausedAt else 0L
        return now - startedAt - paused
    }

    /** @return true if recording started */
    fun start(vault: FileVault, fileName: String): Boolean {
        stopQuietly()
        return try {
            val target = vault.createAudioFile(fileName) ?: return false
            outputUri = target.toString()

            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                MediaRecorder(ctx) else @Suppress("DEPRECATION") MediaRecorder()

            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(96000)
            r.setAudioSamplingRate(44100)

            if (target.scheme == "content") {
                val d = ctx.contentResolver.openFileDescriptor(target, "w")
                    ?: return false
                pfd = d
                r.setOutputFile(d.fileDescriptor)
            } else {
                r.setOutputFile(target.path)
            }

            r.prepare()
            r.start()
            recorder = r
            startedAt = System.currentTimeMillis()
            pausedTotal = 0L
            isRecording = true
            isPaused = false
            true
        } catch (e: Exception) {
            stopQuietly()
            false
        }
    }

    fun pause() {
        if (!isRecording || isPaused) return
        try {
            recorder?.pause()
            pausedAt = System.currentTimeMillis()
            isPaused = true
        } catch (_: Exception) {
        }
    }

    fun resume() {
        if (!isRecording || !isPaused) return
        try {
            recorder?.resume()
            pausedTotal += System.currentTimeMillis() - pausedAt
            isPaused = false
        } catch (_: Exception) {
        }
    }

    /** @return duration in ms, or 0 on failure */
    fun stop(): Long {
        val dur = elapsedMs()
        try {
            recorder?.stop()
        } catch (_: Exception) {
        }
        stopQuietly()
        return dur
    }

    fun cancel() {
        try { recorder?.stop() } catch (_: Exception) {}
        stopQuietly()
        outputUri = null
    }

    private fun stopQuietly() {
        try { recorder?.release() } catch (_: Exception) {}
        try { pfd?.close() } catch (_: Exception) {}
        recorder = null
        pfd = null
        isRecording = false
        isPaused = false
    }
}

class Player(private val ctx: Context) {

    private var mp: MediaPlayer? = null

    var currentId by mutableStateOf<String?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var positionMs by mutableLongStateOf(0L)
    var durationMs by mutableLongStateOf(0L)
        private set
    var speed by mutableStateOf(1.0f)
        private set

    fun play(id: String, uriString: String, startAt: Long = 0L) {
        if (currentId == id && mp != null) {
            resume(); return
        }
        release()
        try {
            val p = MediaPlayer()
            if (uriString.startsWith("content://")) {
                p.setDataSource(ctx, Uri.parse(uriString))
            } else {
                p.setDataSource(File(Uri.parse(uriString).path ?: uriString).absolutePath)
            }
            p.prepare()
            durationMs = p.duration.toLong()
            if (startAt in 1 until durationMs) p.seekTo(startAt.toInt())
            p.setOnCompletionListener {
                isPlaying = false
                positionMs = 0L
            }
            p.start()
            applySpeed(p)
            mp = p
            currentId = id
            isPlaying = true
        } catch (_: Exception) {
            release()
        }
    }

    fun pause() {
        try { mp?.pause() } catch (_: Exception) {}
        isPlaying = false
    }

    fun resume() {
        try {
            mp?.start(); applySpeed(mp); isPlaying = true
        } catch (_: Exception) {}
    }

    fun toggle(id: String, uri: String, startAt: Long = 0L) {
        if (currentId == id && isPlaying) pause()
        else play(id, uri, if (currentId == id) positionMs else startAt)
    }

    fun seekTo(ms: Long) {
        try {
            mp?.seekTo(ms.toInt()); positionMs = ms
        } catch (_: Exception) {}
    }

    fun setSpeed(s: Float) {
        speed = s
        applySpeed(mp)
    }

    private fun applySpeed(p: MediaPlayer?) {
        try {
            if (p != null && p.isPlaying) {
                p.playbackParams = p.playbackParams.setSpeed(speed)
            }
        } catch (_: Exception) {}
    }

    fun tick() {
        try {
            val p = mp ?: return
            if (p.isPlaying) positionMs = p.currentPosition.toLong()
        } catch (_: Exception) {}
    }

    fun release() {
        try { mp?.release() } catch (_: Exception) {}
        mp = null
        currentId = null
        isPlaying = false
        positionMs = 0L
        durationMs = 0L
    }
}

fun fmtDuration(ms: Long): String {
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    return "%02d:%02d".format(m, s)
}
