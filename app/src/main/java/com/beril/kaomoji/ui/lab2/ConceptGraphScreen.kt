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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.beril.kaomoji.lab.graph.EdgeBucket
import com.beril.kaomoji.lab.graph.bucketRelationships
import com.beril.kaomoji.lab.model.KnowledgeObjectEntity
import com.beril.kaomoji.lab.repository.LabRepository
import com.beril.kaomoji.ui.Btn
import com.beril.kaomoji.ui.Display
import com.beril.kaomoji.ui.Empty
import com.beril.kaomoji.ui.GhostBtn
import com.beril.kaomoji.ui.J
import com.beril.kaomoji.ui.SectionLabel
import com.beril.kaomoji.ui.Small
import com.beril.kaomoji.ui.TitleM
import com.beril.kaomoji.ui.nav.dpadFocusable

/**
 * Bir kavramın gerçek ilişki grafiği (§16) — süs bir diyagram değil, gezilebilir bir liste:
 * hangi kavramlar bunu önkoşul olarak istiyor, bu hangilerini açıyor, hangi disiplinlerarası
 * bağlantılar var. Her satır tıklanabilir — graf burada gerçekten davranışı yönlendiriyor
 * (bir kavramdan diğerine geçmeni sağlıyor), dekoratif bir node-link çizimi değil.
 */
@Composable
fun ConceptGraphScreen(
    conceptId: String,
    conceptTitle: String,
    onBack: () -> Unit,
    onOpenConcept: (id: String, title: String) -> Unit,
    onStartSession: (id: String, title: String) -> Unit,
) {
    val ctx = LocalContext.current
    val repo = remember { LabRepository(ctx) }

    var prerequisites by remember { mutableStateOf<List<Pair<KnowledgeObjectEntity, String?>>>(emptyList()) }
    var enables by remember { mutableStateOf<List<Pair<KnowledgeObjectEntity, String?>>>(emptyList()) }
    var related by remember { mutableStateOf<List<Pair<KnowledgeObjectEntity, String?>>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(conceptId) {
        val buckets = bucketRelationships(conceptId, repo.relationshipsOf(conceptId))
        val prereqs = mutableListOf<Pair<KnowledgeObjectEntity, String?>>()
        val enableList = mutableListOf<Pair<KnowledgeObjectEntity, String?>>()
        val relatedList = mutableListOf<Pair<KnowledgeObjectEntity, String?>>()

        buckets.forEach { edge ->
            val other = repo.getById(edge.otherId) ?: return@forEach
            when (edge.bucket) {
                EdgeBucket.PREREQUISITE -> prereqs += other to edge.note
                EdgeBucket.ENABLES -> enableList += other to edge.note
                EdgeBucket.RELATED -> relatedList += other to edge.note
            }
        }
        prerequisites = prereqs
        enables = enableList
        related = relatedList
        loaded = true
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            GhostBtn("Geri", onBack, emoji = "←")
            Spacer(Modifier.height(10.dp))
            Text("🕸️ $conceptTitle", style = Display)
            Spacer(Modifier.height(8.dp))
            Btn("▶ Öğrenme Oturumu Başlat", { onStartSession(conceptId, conceptTitle) })
        }

        item { SectionLabel("önce bunları bilmen gerekiyor", "←") }
        if (!loaded) {
            item { Text("Yükleniyor…", style = Small) }
        } else if (prerequisites.isEmpty()) {
            item { Empty("←", "Önkoşulu yok", "Bu kavram başka hiçbir şeye bağımlı değil — doğrudan başlanabilir.") }
        } else {
            items(prerequisites, key = { "pre-${it.first.id}" }) { (obj, note) ->
                RelatedRow(obj, noteLabel(note), onOpenConcept)
            }
        }

        item { SectionLabel("bu, şunları açıyor", "→") }
        if (loaded && enables.isEmpty()) {
            item { Empty("→", "Henüz hiçbir şeyi açmıyor", "Bu, grafikte başka bir kavramın önkoşulu olarak işaretli değil.") }
        } else {
            items(enables, key = { "en-${it.first.id}" }) { (obj, note) ->
                RelatedRow(obj, noteLabel(note), onOpenConcept)
            }
        }

        item { SectionLabel("ilişkili kavramlar", "🔗") }
        if (loaded && related.isEmpty()) {
            item { Empty("🔗", "Henüz işaretli bir disiplinlerarası bağlantı yok", null) }
        } else {
            items(related, key = { "rel-${it.first.id}" }) { (obj, note) ->
                RelatedRow(obj, note, onOpenConcept)
            }
        }
    }
}

@Composable
private fun RelatedRow(obj: KnowledgeObjectEntity, label: String?, onOpen: (String, String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(J.card, RoundedCornerShape(14.dp))
            .dpadFocusable(onClick = { onOpen(obj.id, obj.title) }, shape = RoundedCornerShape(14.dp))
            .padding(13.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(obj.title, style = TitleM)
            if (label != null) Text(label, style = Small.copy(color = J.inkFaint))
        }
    }
}

private fun noteLabel(note: String?): String? = when (note) {
    "hard" -> "ZORUNLU ÖNKOŞUL"
    "soft" -> "YUMUŞAK ÖNKOŞUL — hafif bir işleniş bununsuz da mümkün"
    "tool" -> "ARAÇ OLARAK KULLANILIYOR — kendi kuramı gerekmiyor"
    "intuition" -> "SEZGİ / BENZETME — öğrenmeyi hızlandırır"
    "co-requisite" -> "BİRLİKTE ÇALIŞILMALI"
    else -> note
}
