package com.beril.kaomoji.ui.lab2

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Uygulamanın tek giriş noktası ve kendi iç navigasyonu — `MainActivity` doğrudan bunu açar.
 */
private sealed class Lab2Screen {
    data object Home : Lab2Screen()
    data class Graph(val conceptId: String, val conceptTitle: String) : Lab2Screen()
    data class Session(val conceptId: String, val conceptTitle: String) : Lab2Screen()
    data object Search : Lab2Screen()
    data object Evaluation : Lab2Screen()
    data object Mistakes : Lab2Screen()
    data object Flashcards : Lab2Screen()
    data object Data : Lab2Screen()
    data object Projects : Lab2Screen()
    data object Exams : Lab2Screen()
    data object Settings : Lab2Screen()
}

@Composable
fun Lab2Root() {
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
                    {
                        // Detaylar arası (örn. bir kavramdan diğerine) sessiz bir çapraz
                        // geçişle değişir — §11/§38'in istediği "küçük, tatmin edici hareket".
                        AnimatedContent(
                            targetState = currentDetail,
                            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                            label = "lab2-secondary-panel",
                        ) { detail ->
                            Lab2DetailScreen(detail, onBack = { screen = Lab2Screen.Home }, onNavigate = { next -> screen = next })
                        }
                    }
                } else null,
            ) {
                Lab2HomeScreen(
                    onOpenConcept = { id, title -> screen = Lab2Screen.Graph(id, title) },
                    onOpenSearch = { screen = Lab2Screen.Search },
                    onOpenEvaluation = { screen = Lab2Screen.Evaluation },
                    onOpenMistakes = { screen = Lab2Screen.Mistakes },
                    onOpenFlashcards = { screen = Lab2Screen.Flashcards },
                    onOpenData = { screen = Lab2Screen.Data },
                    onOpenProjects = { screen = Lab2Screen.Projects },
                    onOpenExams = { screen = Lab2Screen.Exams },
                    onOpenSettings = { screen = Lab2Screen.Settings },
                )
            }
        } else {
            Lab2NavShell {
                // Ana sayfadan bir detaya gidiş sağdan kayarak girer (ileri gidiş hissi),
                // geri dönüş soldan — klasik, tahmin edilebilir bir yön dili. Şıklık için değil,
                // §38'in "nereden geldiğini hissettir" ilkesi için (§11 — küçük, tatmin edici).
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        if (targetState !is Lab2Screen.Home) {
                            (slideInHorizontally(tween(220)) { it / 4 } + fadeIn(tween(220))) togetherWith
                                fadeOut(tween(140))
                        } else {
                            fadeIn(tween(200)) togetherWith
                                (slideOutHorizontally(tween(200)) { it / 4 } + fadeOut(tween(140)))
                        }
                    },
                    label = "lab2-screen-transition",
                ) { s ->
                    when (s) {
                        is Lab2Screen.Home -> Lab2HomeScreen(
                            onOpenConcept = { id, title -> screen = Lab2Screen.Graph(id, title) },
                            onOpenSearch = { screen = Lab2Screen.Search },
                            onOpenEvaluation = { screen = Lab2Screen.Evaluation },
                            onOpenMistakes = { screen = Lab2Screen.Mistakes },
                            onOpenFlashcards = { screen = Lab2Screen.Flashcards },
                            onOpenData = { screen = Lab2Screen.Data },
                            onOpenProjects = { screen = Lab2Screen.Projects },
                            onOpenExams = { screen = Lab2Screen.Exams },
                            onOpenSettings = { screen = Lab2Screen.Settings },
                        )
                        else -> Lab2DetailScreen(s, onBack = { screen = Lab2Screen.Home }, onNavigate = { next -> screen = next })
                    }
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
        is Lab2Screen.Mistakes -> MistakeJournalScreen(onBack = onBack)
        is Lab2Screen.Flashcards -> FlashcardReviewScreen(onBack = onBack)
        is Lab2Screen.Data -> LabDataScreen(onBack = onBack)
        is Lab2Screen.Projects -> ProjectsScreen(onBack = onBack)
        is Lab2Screen.Exams -> ExamsScreen(onBack = onBack)
        is Lab2Screen.Settings -> AiSettingsScreen(onBack = onBack)
        is Lab2Screen.Home -> Unit
    }
}
