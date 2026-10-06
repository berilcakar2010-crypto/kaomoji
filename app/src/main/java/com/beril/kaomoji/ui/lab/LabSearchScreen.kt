package com.beril.kaomoji.ui.lab

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
import com.beril.kaomoji.lab.model.KnowledgeObjectEntity
import com.beril.kaomoji.lab.repository.LabRepository
import com.beril.kaomoji.ui.Display
import com.beril.kaomoji.ui.Empty
import com.beril.kaomoji.ui.Field
import com.beril.kaomoji.ui.GhostBtn
import com.beril.kaomoji.ui.J
import com.beril.kaomoji.ui.Small
import com.beril.kaomoji.ui.TitleM
import com.beril.kaomoji.ui.nav.dpadFocusable

/**
 * Lab 2.0'ın genel araması (§30) — başlık VE gövde (description) üzerinde, her nesne
 * türünde (Concept/Note/Idea/Mistake/Project/...). `LabRepository.search()` Aşama 1'den beri
 * vardı ama hiçbir ekran çağırmıyordu; bu ekran onun ilk gerçek tüketicisi.
 */
@Composable
fun LabSearchScreen(onBack: () -> Unit, onOpenConcept: (id: String, title: String) -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { LabRepository(ctx) }

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<KnowledgeObjectEntity>>(emptyList()) }
    var searched by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            results = emptyList()
            searched = false
        } else {
            results = repo.search(query.trim())
            searched = true
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            GhostBtn("Geri", onBack, emoji = "←")
            Spacer(Modifier.height(10.dp))
            Text("🔎 Ara", style = Display)
            Spacer(Modifier.height(8.dp))
            Field(query, { query = it }, placeholder = "Başlık ya da içerikte ara…")
        }

        if (!searched) {
            item { Text("Kavram, not, fikir, proje — her şey burada aranır.", style = Small) }
        } else if (results.isEmpty()) {
            item { Empty("🔎", "Eşleşme yok", "\"$query\" için bir sonuç bulunamadı.") }
        } else {
            items(results, key = { it.id }) { obj ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(J.card, RoundedCornerShape(14.dp))
                        .dpadFocusable(onClick = { onOpenConcept(obj.id, obj.title) }, shape = RoundedCornerShape(14.dp))
                        .padding(13.dp),
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(obj.title, style = TitleM)
                        Text(obj.kind.name, style = Small.copy(color = J.inkFaint))
                    }
                }
            }
        }
    }
}
