package com.beril.kaomoji.ui.lab2

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Lab 2.0'ın kendi iç navigasyonu — ana `Screen` sealed class'ına (Root.kt) yeni bir dal
 * eklemek yerine burada izole tutuluyor. Bu kademeli yaklaşımın bir parçası: Lab 2.0 kendi
 * büyüdükçe ana uygulamanın navigasyon dosyasını her seferinde değiştirmek zorunda kalmıyoruz.
 */
private sealed class Lab2Screen {
    data object Home : Lab2Screen()
    data class Session(val conceptId: String, val conceptTitle: String) : Lab2Screen()
}

@Composable
fun Lab2Root(onExit: () -> Unit) {
    var screen by remember { mutableStateOf<Lab2Screen>(Lab2Screen.Home) }
    val session = screen as? Lab2Screen.Session

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = breakpointFor(maxWidth.value.toInt()) == Lab2Breakpoint.TABLET_LANDSCAPE

        if (wide) {
            // Genişlik yetiyorsa (§38) bir oturum açmak ana listeyi GİZLEMEZ — ikisi yan
            // yana durur, "nereden geldin" her zaman görünür kalır.
            Lab2NavShell(
                secondaryPanel = session?.let { s ->
                    { LearningSessionScreen(s.conceptId, s.conceptTitle, onBack = { screen = Lab2Screen.Home }) }
                },
            ) {
                Lab2HomeScreen(onBack = onExit, onOpenConcept = { id, title -> screen = Lab2Screen.Session(id, title) })
            }
        } else {
            Lab2NavShell {
                when (val s = screen) {
                    is Lab2Screen.Home -> Lab2HomeScreen(
                        onBack = onExit,
                        onOpenConcept = { id, title -> screen = Lab2Screen.Session(id, title) },
                    )
                    is Lab2Screen.Session -> LearningSessionScreen(
                        conceptId = s.conceptId,
                        conceptTitle = s.conceptTitle,
                        onBack = { screen = Lab2Screen.Home },
                    )
                }
            }
        }
    }
}
