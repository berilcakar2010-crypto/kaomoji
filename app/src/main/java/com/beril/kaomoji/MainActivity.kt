package com.beril.kaomoji

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.beril.kaomoji.lab.notification.LabNotifier
import com.beril.kaomoji.ui.LabTheme
import com.beril.kaomoji.ui.lab2.Lab2Root
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val askNotifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* reddedilirse bildirim görünmez, uygulama ve widget çalışmayı sürdürür */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // Kilit ekranı bildirimi için Android 13+ üzerinde açık izin gerekiyor
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            askNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        lifecycleScope.launch { LabNotifier.refresh(applicationContext) }

        setContent {
            LabTheme {
                Lab2Root()
            }
        }
    }
}
