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
    data class Graph(val conceptId: String, val conceptTitle: String) : Lab2Screen()
    data class Session(val conceptId: String, val conceptTitle: String) : Lab2Screen()
    data object Search : Lab2Screen()
    data object Evaluation : Lab2Screen()
}

@Composable
fun Lab2Root(onExit: () -> Unit) {
    var screen by remember { mutableStateOf<Lab2Screen>(Lab2Screen.Home) }
    val currentDetail = screen

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = breakpointFor(maxWidth.value.toInt()) == Lab2Breakpoint.TABLET_LANDSCAPE
        val showSecondary = wide && currentDetail !is Lab2Screen.Home

        if (wide) {
            // Genişlik yetiyorsa (§38) bir detay (graf/oturum/arama/değerlendirme) açmak ana
            // listeyi GİZLEMEZ — ikisi yan yana durur, "nereden geldin" her zaman görünür kalır.
            Lab2NavShell(
                secondaryPanel = if (showSecondary) {
                    { Lab2DetailScreen(currentDetail, onBack = { screen = Lab2Screen.Home }, onNavigate = { next -> screen = next }) }
                } else null,
            ) {
                Lab2HomeScreen(
                    onBack = onExit,
                    onOpenConcept = { id, title -> screen = Lab2Screen.Graph(id, title) },
                    onOpenSearch = { screen = Lab2Screen.Search },
                    onOpenEvaluation = { screen = Lab2Screen.Evaluation },
                )
            }
        } else {
            Lab2NavShell {
                when (val s = screen) {
                    is Lab2Screen.Home -> Lab2HomeScreen(
                        onBack = onExit,
                        onOpenConcept = { id, title -> screen = Lab2Screen.Graph(id, title) },
                        onOpenSearch = { screen = Lab2Screen.Search },
                        onOpenEvaluation = { screen = Lab2Screen.Evaluation },
                    )
                    else -> Lab2DetailScreen(s, onBack = { screen = Lab2Screen.Home }, onNavigate = { next -> screen = next })
                }
            }
        }
    }
}

@Composable
private fun Lab2DetailScreen(screen: Lab2Screen, onBack: () -> Unit, onNavigate: (Lab2Screen) -> Unit) {
    when (screen) {
        is Lab2Screen.Graph -> ConceptGraphScreen(
            conceptId = screen.conceptId,
            conceptTitle = screen.conceptTitle,
            onBack = onBack,
            onOpenConcept = { id, title -> onNavigate(Lab2Screen.Graph(id, title)) },
            onStartSession = { id, title -> onNavigate(Lab2Screen.Session(id, title)) },
        )
        is Lab2Screen.Session -> LearningSessionScreen(
            conceptId = screen.conceptId,
            conceptTitle = screen.conceptTitle,
            onBack = onBack,
        )
        is Lab2Screen.Search -> LabSearchScreen(
            onBack = onBack,
            onOpenConcept = { id, title -> onNavigate(Lab2Screen.Graph(id, title)) },
        )
        is Lab2Screen.Evaluation -> LabEvaluationScreen(onBack = onBack)
        is Lab2Screen.Home -> Unit
    }
}
