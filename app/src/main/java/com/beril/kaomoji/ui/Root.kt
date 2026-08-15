package com.beril.kaomoji.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beril.kaomoji.audio.Player
import com.beril.kaomoji.audio.Recorder
import com.beril.kaomoji.data.MissionEngine
import com.beril.kaomoji.data.Store
import com.beril.kaomoji.storage.FileVault

sealed class Screen {
    data object Garden : Screen()
    data object Curriculum : Screen()
    data object Inbox : Screen()
    data object Projects : Screen()
    data object Bag : Screen()
    data object Audio : Screen()
    data object Mistakes : Screen()
    data object Assessments : Screen()
    data object Review : Screen()
    data object Storage : Screen()
    data object Resources : Screen()
    data object ExplainIt : Screen()
    data class UnitDetail(val id: String) : Screen()
    data class ProjectDetail(val id: String) : Screen()
}

private data class Tab(val emoji: String, val label: String, val screen: Screen)

private val tabs = listOf(
    Tab("🍎", "Bahçe", Screen.Garden),
    Tab("📚", "Müfredat", Screen.Curriculum),
    Tab("🧺", "Inbox", Screen.Inbox),
    Tab("🌱", "Projeler", Screen.Projects),
    Tab("🎒", "Çanta", Screen.Bag)
)

@Composable
fun Root(
    store: Store,
    recorder: Recorder,
    player: Player,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    onPickFolder: () -> Unit,
    widthDp: Int
) {
    // Foldable: cover screen is narrow, opened screen is wide.
    val compact = widthDp < 380
    val wide = widthDp >= 640

    var screen by remember { mutableStateOf<Screen>(Screen.Garden) }
    var back by remember { mutableStateOf<Screen>(Screen.Garden) }
    var recording by remember { mutableStateOf<RecordRequest?>(null) }
    var mistakeUnit by remember { mutableStateOf<String?>(null) }
    var problemUnit by remember { mutableStateOf<String?>(null) }
    var showProblemDialog by remember { mutableStateOf(false) }
    var intentAsked by remember { mutableStateOf(!compact) }

    val vault = remember(store.storageUri) { FileVault(storeCtx!!, store.storageUri) }

    fun go(s: Screen) {
        if (screen is Screen.Garden || screen is Screen.Curriculum ||
            screen is Screen.Projects || screen is Screen.Bag || screen is Screen.Inbox
        ) back = screen
        screen = s
    }

    // ── recording overlay takes the whole surface ──
    recording?.let { req ->
        RecordScreen(
            store, recorder, req, hasAudioPermission, onRequestPermission,
            onDone = { recording = null },
            onCancel = { recording = null }
        )
        return
    }

    if (compact && !intentAsked) {
        CoverIntent(
            store,
            onStudy = { intentAsked = true; screen = Screen.Garden },
            onContinue = {
                intentAsked = true
                store.currentUnit?.let { screen = Screen.UnitDetail(it.id) }
            },
            onRecord = { intentAsked = true; recording = RecordRequest(null, store.currentUnit?.id) },
            onCapture = { intentAsked = true; screen = Screen.Inbox }
        )
        return
    }

    Row(Modifier.fillMaxSize().background(J.paper)) {

        // Wide layout: persistent rail + optional second pane
        if (wide) {
            Rail(screen) { go(it) }
            Box(Modifier.width(1.dp).fillMaxHeight().background(J.line))
        }

        Column(Modifier.weight(1f)) {
            Box(Modifier.weight(1f)) {
                Body(
                    store, player, screen, compact, wide, vault, mistakeUnit,
                    onGo = { go(it) },
                    onBack = { screen = back },
                    onOpenUnit = { go(Screen.UnitDetail(it)) },
                    onOpenProject = { go(Screen.ProjectDetail(it)) },
                    onRecord = { recording = it },
                    onAddMistake = { u -> mistakeUnit = u; go(Screen.Mistakes) },
                    onLogProblems = { u -> problemUnit = u; showProblemDialog = true },
                    onPickFolder = onPickFolder
                )
            }
            if (!wide) BottomBar(screen) { go(it) }
        }

        // Second pane on wide screens: the mission always stays visible
        if (wide && screen !is Screen.Garden) {
            Box(Modifier.width(1.dp).fillMaxHeight().background(J.line))
            Box(Modifier.width(320.dp).fillMaxHeight().background(J.paper)) {
                SidePane(store) { recording = it }
            }
        }
    }

    if (showProblemDialog) {
        ProblemLogDialog(store, problemUnit) { showProblemDialog = false }
    }
}

@Composable
private fun Body(
    store: Store,
    player: Player,
    screen: Screen,
    compact: Boolean,
    wide: Boolean,
    vault: FileVault,
    mistakeUnit: String?,
    onGo: (Screen) -> Unit,
    onBack: () -> Unit,
    onOpenUnit: (String) -> Unit,
    onOpenProject: (String) -> Unit,
    onRecord: (RecordRequest) -> Unit,
    onAddMistake: (String?) -> Unit,
    onLogProblems: (String) -> Unit,
    onPickFolder: () -> Unit
) {
    when (screen) {
        is Screen.Garden -> GardenScreen(store, wide, onOpenUnit, onGo, onRecord, onOpenProject)
        is Screen.Curriculum -> CurriculumScreen(store, onOpenUnit)
        is Screen.Inbox -> InboxScreen(store)
        is Screen.Projects -> ProjectsScreen(store, onOpenProject)
        is Screen.Bag -> StudyBagScreen(store, onGo) { onGo(Screen.ExplainIt) }
        is Screen.Audio -> AudioLibraryScreen(store, player, onBack) {
            onRecord(RecordRequest(null, store.currentUnit?.id))
        }
        is Screen.Mistakes -> MistakesScreen(store, mistakeUnit, onBack)
        is Screen.Assessments -> AssessmentsScreen(store, onBack)
        is Screen.Review -> ReviewScreen(store, onBack)
        is Screen.Storage -> StorageScreen(store, vault, compact, onPickFolder, onBack)
        is Screen.Resources -> ResourcesScreen(store, onBack)
        is Screen.ExplainIt -> ExplainItScreen(store, onBack, onRecord)
        is Screen.UnitDetail -> UnitDetailScreen(
            store, screen.id, onBack, onRecord, onAddMistake, onLogProblems
        )
        is Screen.ProjectDetail -> ProjectDetailScreen(store, screen.id, onBack, onRecord)
    }
}

@Composable
private fun BottomBar(current: Screen, onGo: (Screen) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(J.card)
            .border(1.dp, J.line, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEach { t ->
            val on = current::class == t.screen::class
            Column(
                Modifier
                    .clickable { onGo(t.screen) }
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .background(
                            if (on) J.lime.copy(alpha = 0.45f) else Color.Transparent,
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 13.dp, vertical = 4.dp)
                ) { Text(t.emoji, style = TextStyle(fontSize = 17.sp)) }
                Spacer(Modifier.height(2.dp))
                Text(
                    t.label,
                    style = Tiny.copy(
                        color = if (on) J.forest else J.inkFaint,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal
                    )
                )
            }
        }
    }
}

@Composable
private fun Rail(current: Screen, onGo: (Screen) -> Unit) {
    Column(
        Modifier
            .width(92.dp)
            .fillMaxHeight()
            .background(J.card)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "(≧▽≦)",
            style = TextStyle(
                fontSize = 13.sp, color = J.forest,
                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
            )
        )
        Spacer(Modifier.height(20.dp))
        tabs.forEach { t ->
            val on = current::class == t.screen::class
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { onGo(t.screen) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .background(
                            if (on) J.lime.copy(alpha = 0.45f) else Color.Transparent,
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) { Text(t.emoji, style = TextStyle(fontSize = 18.sp)) }
                Spacer(Modifier.height(3.dp))
                Text(
                    t.label,
                    style = Tiny.copy(
                        color = if (on) J.forest else J.inkFaint,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal
                    )
                )
            }
        }
    }
}

/** Wide-screen companion pane: the mission never leaves the screen. */
@Composable
private fun SidePane(store: Store, onRecord: (RecordRequest) -> Unit) {
    val mission = remember(store.done.size, store.recordings.size, store.mistakes.size) {
        MissionEngine.pick(store)
    }
    val unit = store.currentUnit

    Column(Modifier.fillMaxSize().padding(14.dp)) {
        SectionLabel("bugünün küçük görevi", "🍀")
        Spacer(Modifier.height(9.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .background(J.card, RoundedCornerShape(16.dp))
                .border(1.dp, J.cherry.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .padding(13.dp)
        ) {
            Text(mission.emoji, style = TextStyle(fontSize = 21.sp))
            Spacer(Modifier.height(7.dp))
            Text(mission.title, style = TitleM)
            Spacer(Modifier.height(7.dp))
            Text(mission.why, style = Small)
            Spacer(Modifier.height(11.dp))
            if (mission.taskId != null)
                Btn("Bitirdim", { store.toggleTask(mission.taskId) }, bg = J.forest, emoji = "✓")
            else if (mission.recordingPrompt != null)
                Btn("Kaydet", {
                    onRecord(RecordRequest(mission.recordingPrompt, mission.unitId, null, true))
                }, bg = J.cherry, emoji = "🎙️")
        }

        Spacer(Modifier.height(16.dp))
        if (unit != null) {
            SectionLabel("şu anki birim", "🌱")
            Spacer(Modifier.height(8.dp))
            Text(unit.title, style = TitleM)
            Spacer(Modifier.height(7.dp))
            Bar(store.unitProgress(unit), J.forest)
            Spacer(Modifier.height(6.dp))
            Text(
                "${unit.tasks.count { store.done[it.id] == true }} / ${unit.tasks.size}",
                style = Mono
            )
        }

        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatTile("${store.totalDone}", "görev", "✅", J.forest)
            StatTile("${store.problemsSolved}", "problem", "✏️", J.cherry)
        }
    }
}

/** Cover screen: one question, four answers. No dashboard, no shame. */
@Composable
private fun CoverIntent(
    store: Store,
    onStudy: () -> Unit,
    onContinue: () -> Unit,
    onRecord: () -> Unit,
    onCapture: () -> Unit
) {
    val unit = store.currentUnit
    val mission = remember(store.done.size) { MissionEngine.pick(store) }

    Column(
        Modifier
            .fillMaxSize()
            .background(J.paper)
            .gingham()
            .padding(14.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "(≧▽≦)",
            style = TextStyle(
                fontSize = 30.sp, fontWeight = FontWeight.Bold,
                color = J.forest, fontFamily = FontFamily.Monospace
            ),
            modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "ne yapıyoruz?",
            style = TitleM.copy(color = J.inkSoft),
            modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(18.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .background(J.card, RoundedCornerShape(15.dp))
                .border(1.dp, J.cherry.copy(alpha = 0.3f), RoundedCornerShape(15.dp))
                .padding(11.dp)
        ) {
            Text("🍀 BUGÜNÜN GÖREVİ", style = Tiny.copy(color = J.cherry, fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(5.dp))
            Text(mission.title, style = Body, maxLines = 4)
        }

        Spacer(Modifier.height(14.dp))
        Btn("Devam et", onContinue, Modifier.fillMaxWidth(), J.forest, emoji = "🌱")
        Spacer(Modifier.height(7.dp))
        Btn("Anlat", onRecord, Modifier.fillMaxWidth(), J.cherry, emoji = "🎙️")
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            GhostBtn("Yakala", onCapture, Modifier.weight(1f), "🧺")
            GhostBtn("Bahçe", onStudy, Modifier.weight(1f), "🍎")
        }

        unit?.let {
            Spacer(Modifier.height(14.dp))
            Text(
                it.title,
                style = Tiny,
                modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
            )
        }
    }
}
