package com.beril.kaomoji.ui.lab2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.beril.kaomoji.lab.model.KnowledgeObjectEntity
import com.beril.kaomoji.lab.model.ObjectKind
import com.beril.kaomoji.lab.model.ScheduleStatus
import com.beril.kaomoji.lab.repository.LabRepository
import com.beril.kaomoji.ui.Btn
import com.beril.kaomoji.ui.Display
import com.beril.kaomoji.ui.Empty
import com.beril.kaomoji.ui.Field
import com.beril.kaomoji.ui.GhostBtn
import com.beril.kaomoji.ui.J
import com.beril.kaomoji.ui.SectionLabel
import com.beril.kaomoji.ui.Small
import com.beril.kaomoji.ui.TitleM
import com.beril.kaomoji.ui.nav.dpadFocusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import kotlinx.coroutines.launch

/**
 * Lab 2.0'ın ana komut yüzeyi (§28) — kart yığını bir dashboard değil, "şu an ne önemli"
 * sorusuna cevap. Henüz hiçbir eski veri buraya göç etmedi (bu aşamanın kapsamı dışında),
 * bu yüzden ekran şu an gerçekten boş durumları gösteriyor — bu bilerek böyle: §49'un
 * istediği "boş durum sistemi öğretsin" ilkesi, Room gerçekten boşken en dürüst haliyle
 * test ediliyor.
 *
 * Mevcut uygulamadan (Çanta → 🧪 Lab 2.0) ayrı bir giriş noktası olarak eklendi — eski
 * Laboratuvar ekranının (GardenScreen) yerini almıyor, onun yanında duruyor.
 */
@Composable
fun Lab2HomeScreen(onBack: () -> Unit, onOpenConcept: (id: String, title: String) -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { LabRepository(ctx) }
    val scope = rememberCoroutineScope()

    var recent by remember { mutableStateOf<List<KnowledgeObjectEntity>>(emptyList()) }
    var upcoming by remember { mutableStateOf<List<KnowledgeObjectEntity>>(emptyList()) }
    var pastTarget by remember { mutableStateOf<List<KnowledgeObjectEntity>>(emptyList()) }
    var concepts by remember { mutableStateOf<List<KnowledgeObjectEntity>>(emptyList()) }
    var capture by remember { mutableStateOf("") }
    var newConcept by remember { mutableStateOf("") }
    var refreshTick by remember { mutableStateOf(0) }
    var importedCount by remember { mutableStateOf(0) }
    var importing by remember { mutableStateOf(false) }

    LaunchedEffect(refreshTick) {
        upcoming = repo.upcoming(days = 14)
        pastTarget = repo.pastTargetDate()
        concepts = repo.getByKind(ObjectKind.CONCEPT)
        importedCount = repo.countFromPackage("lab2-personal-curriculum")
    }
    LaunchedEffect(Unit) {
        repo.observeAll().collect { recent = it.take(10) }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            GhostBtn("Geri", onBack, emoji = "←")
            Spacer(Modifier.height(10.dp))
            Text("🧪 Lab 2.0 (Beta)", style = Display)
            Text(
                "Yeni bilgi grafiği altyapısı üzerine kurulu ana komut yüzeyi. Henüz eski " +
                    "verin göç etmedi — bu ekran gerçekten boş durumları gösteriyor.",
                style = Small,
            )
        }

        item { QuickCapture(capture, onChange = { capture = it }, onCapture = {
            val text = capture.trim()
            if (text.isNotEmpty()) {
                scope.launch {
                    repo.quickCapture(text)
                    capture = ""
                    refreshTick++
                }
            }
        }) }

        item { SectionLabel("şu an önemli olan", "🎯") }
        if (upcoming.isEmpty() && pastTarget.isEmpty()) {
            item {
                Empty(
                    "🎯",
                    "Henüz yaklaşan ya da hedef tarihi geçmiş bir şey yok",
                    "Bir sınav, ödev ya da hedef tarih eklediğinde burada görünecek. " +
                        "Bir tarihin geçmesi hiçbir şeyi \"bozmaz\" — sadece bir seçenek listesi olur.",
                )
            }
        } else {
            items(pastTarget, key = { "past-${it.id}" }) { obj -> KnowledgeRow(obj, label = "HEDEF TARİHİ GEÇTİ — seçenek, hata değil") }
            items(upcoming, key = { "up-${it.id}" }) { obj -> KnowledgeRow(obj, label = statusLabel(obj)) }
        }

        item {
            SectionLabel("müfredat paketi", "📚")
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        if (importedCount > 0) "İçe aktarıldı: $importedCount nesne (önkoşul grafiği + bağlantılar)"
                        else "Henüz içe aktarılmadı",
                        style = Small,
                    )
                    Spacer(Modifier.height(6.dp))
                    Btn(
                        if (importing) "İçe aktarılıyor…" else "📚 Kişisel Müfredatı İçe Aktar",
                        {
                            if (!importing) {
                                importing = true
                                scope.launch {
                                    repo.importExternalCurriculum(ctx)
                                    importing = false
                                    refreshTick++
                                }
                            }
                        },
                        enabled = !importing,
                    )
                }
            }
        }

        item {
            SectionLabel("kavramlar · öğrenme oturumu", "🧠")
            Text(
                "Bir kavram seç, önce kendi tahminini/denemeni yaz, sonra sadece gereken " +
                    "kadarını açığa çıkar (§13 — soru-önce öğrenme).",
                style = Small,
            )
            Spacer(Modifier.height(6.dp))
            Field(newConcept, { newConcept = it }, placeholder = "Yeni bir kavram adı (örn. \"Faraday Yasası\")")
            Spacer(Modifier.height(6.dp))
            Btn("Kavram Ekle", {
                val t = newConcept.trim()
                if (t.isNotEmpty()) {
                    scope.launch { repo.createConcept(t); newConcept = ""; refreshTick++ }
                }
            }, emoji = "🧠")
        }
        if (concepts.isEmpty()) {
            item { Empty("🧠", "Henüz bir kavram yok", "Yukarıdan bir kavram ekle, öğrenme oturumu ona bağlanacak.") }
        } else {
            items(concepts, key = { "concept-${it.id}" }) { c ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(J.card, RoundedCornerShape(14.dp))
                        .dpadFocusable(onClick = { onOpenConcept(c.id, c.title) }, shape = RoundedCornerShape(14.dp))
                        .padding(13.dp),
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(c.title, style = TitleM)
                        Text("Öğrenme oturumu başlat →", style = Small.copy(color = J.inkFaint))
                    }
                }
            }
        }

        item { SectionLabel("son eklenenler", "🕓") }
        if (recent.isEmpty()) {
            item {
                Empty(
                    "🕓",
                    "Henüz bir bilgi nesnesi yok",
                    "Yukarıdan hızlı yakalama ile bir şey ekle, ya da bir müfredat paketi içe aktar.",
                )
            }
        } else {
            items(recent, key = { it.id }) { obj -> KnowledgeRow(obj, label = obj.kind.name) }
        }
    }
}

@Composable
private fun QuickCapture(value: String, onChange: (String) -> Unit, onCapture: () -> Unit) {
    Column {
        SectionLabel("hızlı yakalama", "📥")
        Field(value, onChange, placeholder = "Bir fikir, soru ya da not yakala — sınıflandırma sonra")
        Spacer(Modifier.height(6.dp))
        Btn("Yakala", onCapture, emoji = "+")
    }
}

@Composable
private fun KnowledgeRow(obj: KnowledgeObjectEntity, label: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.fillMaxWidth()) {
            Text(obj.title, style = TitleM)
            Text(label, style = Small.copy(color = J.inkFaint))
        }
    }
}

private fun statusLabel(obj: KnowledgeObjectEntity): String = when (obj.schedule?.status) {
    ScheduleStatus.SCHEDULED -> "ZAMANLANDI"
    ScheduleStatus.TARGET_SET -> "HEDEF BELİRLENDİ"
    ScheduleStatus.OVERDUE -> "GEÇTİ — seçenek, hata değil"
    ScheduleStatus.COMPLETED -> "TAMAMLANDI"
    ScheduleStatus.SKIPPED -> "ATLANDI"
    ScheduleStatus.PAUSED -> "DURAKLATILDI"
    else -> "ÖNERİLDİ"
}
