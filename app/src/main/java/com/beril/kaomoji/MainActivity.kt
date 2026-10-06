package com.beril.kaomoji

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.beril.kaomoji.ui.LabTheme
import com.beril.kaomoji.ui.lab2.Lab2Root

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            LabTheme {
                Lab2Root()
            }
        }
    }
}
