package com.beril.kaomoji.ui.lab

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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Uygulamanın tek giriş noktası ve kendi iç navigasyonu — `MainActivity` doğrudan bunu açar.
 *
 * §31'in istediği beş kalıcı üst-seviye alan: her biri kendi "alan ana ekranı"na sahip, rail'de
 * (bkz. `LabNavShell`) her zaman görünür ve tek dokunuşla değişir — eskiden olduğu gibi tek bir
 * "Ana Sayfa"nın üstüne yığılmış on farklı buton değil.
 *
 * Her alanın kendi gezinme geçmişi var (`stacks`, alan → ekran listesi) — bir alandan çıkıp
 * geri dönmek o alanın ana ekranına sıfırlamaz, kaldığın yere döner (Aşama 26'nın "düz bir
 * geçiş, alan-başına geçmiş yığını yok" dürüst eksiğini kapatıyor).
 */
enum class LabArea(val label: String, val emoji: String) {
    LEARN("Öğren", "🧠"),
    KNOWLEDGE("Bilgi", "📚"),
    PROJECTS("Projeler", "⚗️"),
    ACADEMICS("Akademik", "🪞"),
    ARCHIVE("Arşiv", "🗄️"),
}

private sealed class LabScreen {
    data class Graph(val conceptId: String, val conceptTitle: String) : LabScreen()
    data class Session(val conceptId: String, val conceptTitle: String) : LabScreen()
    data object Search : LabScreen()
    data object Evaluation : LabScreen()
    data object Mistakes : LabScreen()
    data object Flashcards : LabScreen()
    data object Data : LabScreen()
    data object Projects : LabScreen()
    data object Exams : LabScreen()
    data object Writing : LabScreen()
    data object CurriculumGen : LabScreen()
}

@Composable
fun LabRoot() {
    var area by remember { mutableStateOf(LabArea.LEARN) }
    val stacks = remember { mutableStateMapOf<LabArea, List<LabScreen>>() }
    var settingsOpen by remember { mutableStateOf(false) }

    // AI Ayarları beş alandan biri değil (bkz. LabArea) — hangi alanda olursan ol erişilebilir
    // olması gerekiyor, bu yüzden alan/ekran yığınının dışında, kendi bağımsız bayrağıyla.
    if (settingsOpen) {
        AiSettingsScreen(onBack = { settingsOpen = false })
        return
    }

    val selectArea: (LabArea) -> Unit = { next -> area = next }
    val currentScreen = stacks[area]?.lastOrNull()
    // `a`'ya göre parametrelenmiş hal: aşağıdaki dar-ekran AnimatedContent'i geçiş animasyonu
    // SIRASINDA eski alanı da render edebilir (`targetState` değişirken çıkan içerik hâlâ
    // bir kare sürebilir) — o an tıklanırsa canlı `area` değil, o içeriğin GERÇEKTEN ait
    // olduğu alan güncellenmeli, yoksa bir alanın ekranı yanlışlıkla başka birine yazılabilir.
    val pushTo: (LabArea, LabScreen) -> Unit = { a, next -> stacks[a] = (stacks[a] ?: emptyList()) + next }
    val popFrom: (LabArea) -> Unit = { a -> stacks[a] = (stacks[a] ?: emptyList()).dropLast(1) }
    val push: (LabScreen) -> Unit = { next -> pushTo(area, next) }
    val pop: () -> Unit = { popFrom(area) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = breakpointFor(maxWidth.value.toInt()) == LabBreakpoint.TABLET_LANDSCAPE
        val showSecondary = wide && currentScreen != null

        if (wide) {
            // Genişlik yetiyorsa (§38) bir detay (graf/oturum/arama/değerlendirme) açmak alan
            // ana ekranını GİZLEMEZ — ikisi yan yana durur, "nereden geldin" her zaman görünür.
            LabNavShell(
                currentArea = area,
                onSelectArea = selectArea,
                onOpenSettings = { settingsOpen = true },
                secondaryPanel = if (showSecondary) {
                    {
                        // Detaylar arası (örn. bir kavramdan diğerine) sessiz bir çapraz
                        // geçişle değişir — §11/§38'in istediği "küçük, tatmin edici hareket".
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                            label = "lab-secondary-panel",
                        ) { detail ->
                            if (detail != null) {
                                LabDetailScreen(detail, onBack = pop, onNavigate = push)
                            }
                        }
                    }
                } else null,
            ) {
                LabAreaHome(area, onNavigate = push)
            }
        } else {
            LabNavShell(
                currentArea = area,
                onSelectArea = selectArea,
                onOpenSettings = { settingsOpen = true },
            ) {
                // Alan ana ekranından bir detaya gidiş sağdan kayarak girer (ileri gidiş
                // hissi), geri dönüş soldan — klasik, tahmin edilebilir bir yön dili. §38'in
                // "nereden geldiğini hissettir" ilkesi için (§11 — küçük, tatmin edici).
                AnimatedContent(
                    targetState = area to currentScreen,
                    transitionSpec = {
                        if (targetState.second != null) {
                            (slideInHorizontally(tween(220)) { it / 4 } + fadeIn(tween(220))) togetherWith
                                fadeOut(tween(140))
                        } else {
                            fadeIn(tween(200)) togetherWith
                                (slideOutHorizontally(tween(200)) { it / 4 } + fadeOut(tween(140)))
                        }
                    },
                    label = "lab-screen-transition",
                ) { (a, s) ->
                    if (s == null) {
                        LabAreaHome(a, onNavigate = { next -> pushTo(a, next) })
                    } else {
                        LabDetailScreen(s, onBack = { popFrom(a) }, onNavigate = { next -> pushTo(a, next) })
                    }
                }
            }
        }
    }
}

@Composable
private fun LabAreaHome(area: LabArea, onNavigate: (LabScreen) -> Unit) {
    when (area) {
        LabArea.LEARN -> LabHomeScreen(
            onOpenConcept = { id, title -> onNavigate(LabScreen.Graph(id, title)) },
        )
        LabArea.KNOWLEDGE -> LabKnowledgeHomeScreen(
            onOpenSearch = { onNavigate(LabScreen.Search) },
            onOpenWriting = { onNavigate(LabScreen.Writing) },
        )
        LabArea.PROJECTS -> LabProjectsHomeScreen(
            onOpenProjects = { onNavigate(LabScreen.Projects) },
            onOpenExams = { onNavigate(LabScreen.Exams) },
            onOpenCurriculumGen = { onNavigate(LabScreen.CurriculumGen) },
        )
        LabArea.ACADEMICS -> LabAcademicsHomeScreen(
            onOpenEvaluation = { onNavigate(LabScreen.Evaluation) },
            onOpenMistakes = { onNavigate(LabScreen.Mistakes) },
            onOpenFlashcards = { onNavigate(LabScreen.Flashcards) },
        )
        LabArea.ARCHIVE -> LabArchiveScreen(
            onOpenConcept = { id, title -> onNavigate(LabScreen.Graph(id, title)) },
            onOpenData = { onNavigate(LabScreen.Data) },
        )
    }
}

@Composable
private fun LabDetailScreen(screen: LabScreen, onBack: () -> Unit, onNavigate: (LabScreen) -> Unit) {
    when (screen) {
        is LabScreen.Graph -> ConceptGraphScreen(
            conceptId = screen.conceptId,
            conceptTitle = screen.conceptTitle,
            onBack = onBack,
            onOpenConcept = { id, title -> onNavigate(LabScreen.Graph(id, title)) },
            onStartSession = { id, title -> onNavigate(LabScreen.Session(id, title)) },
        )
        is LabScreen.Session -> LearningSessionScreen(
            conceptId = screen.conceptId,
            conceptTitle = screen.conceptTitle,
            onBack = onBack,
        )
        is LabScreen.Search -> LabSearchScreen(
            onBack = onBack,
            onOpenConcept = { id, title -> onNavigate(LabScreen.Graph(id, title)) },
        )
        is LabScreen.Evaluation -> LabEvaluationScreen(onBack = onBack)
        is LabScreen.Mistakes -> MistakeJournalScreen(onBack = onBack)
        is LabScreen.Flashcards -> FlashcardReviewScreen(onBack = onBack)
        is LabScreen.Data -> LabDataScreen(onBack = onBack)
        is LabScreen.Projects -> ProjectsScreen(onBack = onBack)
        is LabScreen.Exams -> ExamsScreen(onBack = onBack)
        is LabScreen.Writing -> WritingAssistScreen(onBack = onBack)
        is LabScreen.CurriculumGen -> CurriculumGenScreen(onBack = onBack)
    }
}
