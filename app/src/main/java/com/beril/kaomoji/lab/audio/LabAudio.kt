package com.beril.kaomoji.lab.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.util.UUID

/**
 * Lab'ın kendi ses kaydı — Aşama 15'te silinen eski `audio/Audio.kt`'nin (Recorder/Player)
 * yerini almıyor, sıfırdan yazıldı; eskisi hiçbir zaman geri getirilmedi. Dosyalar uygulamaya
 * özel depoda (`filesDir/recordings`) tutulur — SAF'a gerek yok, bunlar paylaşılan kullanıcı
 * dosyaları değil, uygulamanın kendi ürettiği ve sadece kendisinin okuduğu ses kayıtları.
 * `ExplanationPayload.audioFilePath` bu dosyanın mutlak yolunu tutar.
 */
class LabRecorder(private val ctx: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    /** Kayda başlar, dosyayı oluşturur. Çağıran taraf RECORD_AUDIO izninin verildiğinden
     *  sorumludur — bu sınıf izin kontrolü yapmaz. */
    fun start(): File {
        val dir = File(ctx.filesDir, "recordings").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.m4a")
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(ctx)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setOutputFile(file.absolutePath)
        r.prepare()
        r.start()
        recorder = r
        outputFile = file
        return file
    }

    /** Kaydı durdurur ve dosyayı döner — null ise hiçbir kayıt sürmüyordu. */
    fun stop(): File? {
        val f = outputFile
        try {
            recorder?.stop()
        } catch (_: Exception) {
            // Çok kısa bir kayıt (örn. hemen durdurulmuş) bazı cihazlarda IllegalStateException
            // fırlatabilir — dosya zaten geçersiz olacağından sessizce geç.
        }
        recorder?.release()
        recorder = null
        outputFile = null
        return f
    }

    /** Kaydı iptal eder, dosyayı siler — kullanıcı vazgeçerse çağrılır. */
    fun cancel() {
        try {
            recorder?.stop()
        } catch (_: Exception) {
        }
        recorder?.release()
        recorder = null
        outputFile?.delete()
        outputFile = null
    }
}

class LabPlayer {
    private var player: MediaPlayer? = null

    fun play(path: String, onComplete: () -> Unit = {}) {
        stop()
        val p = MediaPlayer()
        p.setDataSource(path)
        p.setOnCompletionListener {
            onComplete()
            stop()
        }
        p.prepare()
        p.start()
        player = p
    }

    fun stop() {
        player?.let {
            try {
                it.stop()
            } catch (_: Exception) {
            }
            it.release()
        }
        player = null
    }
}
