package com.beril.kaomoji

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.beril.kaomoji.audio.Player
import com.beril.kaomoji.audio.Recorder
import com.beril.kaomoji.data.Store
import com.beril.kaomoji.storage.FileVault
import com.beril.kaomoji.ui.KaomojiTheme
import com.beril.kaomoji.ui.J
import com.beril.kaomoji.ui.Root
import com.beril.kaomoji.ui.storeCtx

class MainActivity : ComponentActivity() {

    private lateinit var store: Store
    private lateinit var recorder: Recorder
    private lateinit var player: Player

    private var permissionState by mutableStateOf(false)

    private val askPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> permissionState = granted }

    private val pickFolder = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            store.setStorage(uri.toString())
            FileVault(this, uri.toString()).ensureStructure()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        storeCtx = applicationContext
        store = Store(applicationContext)
        recorder = Recorder(applicationContext)
        player = Player(applicationContext)

        permissionState = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        setContent {
            KaomojiTheme {
                BoxWithConstraints(
                    Modifier
                        .fillMaxSize()
                        .background(J.paper)
                ) {
                    val w = maxWidth.value.toInt()
                    Root(
                        store = store,
                        recorder = recorder,
                        player = player,
                        hasAudioPermission = permissionState,
                        onRequestPermission = {
                            askPermission.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onPickFolder = { pickFolder.launch(null) },
                        widthDp = w
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        store.save()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
        recorder.cancel()
    }
}
