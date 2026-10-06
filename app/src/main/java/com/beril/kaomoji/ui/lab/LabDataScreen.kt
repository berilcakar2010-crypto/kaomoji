package com.beril.kaomoji.ui.lab

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.beril.kaomoji.lab.repository.LabRepository
import com.beril.kaomoji.ui.Btn
import com.beril.kaomoji.ui.Display
import com.beril.kaomoji.ui.GhostBtn
import com.beril.kaomoji.ui.SectionLabel
import com.beril.kaomoji.ui.Small
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Veri sahipliği (§34/§41) — kullanıcının Lab 2.0'da ürettiği HER şey (kavramlar, oturumlar,
 * hatalar, kartlar, notlar) tek bir dosyaya aktarılabilir, aynı dosyadan geri yüklenebilir.
 * Sunucu yok, hesap yok, vendor lock-in yok — dosya düz, okunabilir JSON.
 */
@Composable
fun LabDataScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { LabRepository(ctx) }
    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        working = true
        scope.launch {
            val json = repo.exportAll()
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            status = "Dışa aktarıldı."
            working = false
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        working = true
        scope.launch {
            val raw = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (raw == null) {
                status = "Dosya okunamadı."
            } else {
                val result = runCatching { repo.importAll(raw) }
                status = result.fold(
                    onSuccess = { "İçe aktarıldı: ${it.objects.size} nesne, ${it.relationships.size} ilişki." },
                    onFailure = { "İçe aktarma başarısız: bu dosya beklenen biçimde değil (${it.message})." },
                )
            }
            working = false
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
            Text("📁 Verim", style = Display)
            Text(
                "Lab 2.0'da ürettiğin her şey (kavram, oturum, hata, kart, not) senin. " +
                    "Sunucu yok, hesap yok — tek bir düz JSON dosyasına aktarılır/geri yüklenir.",
                style = Small,
            )
        }

        item {
            SectionLabel("dışa aktar", "⬇️")
            Btn(if (working) "Çalışıyor…" else "Yedek Dosyası Oluştur", {
                if (!working) {
                    val name = "lab2-yedek-${SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date())}.json"
                    exportLauncher.launch(name)
                }
            }, enabled = !working, emoji = "⬇️")
        }

        item {
            SectionLabel("içe aktar", "⬆️")
            Text("Var olan bir nesneyle aynı id'ye sahip bir kayıt varsa üzerine yazılır.", style = Small)
            Spacer(Modifier.height(6.dp))
            Btn(if (working) "Çalışıyor…" else "Yedek Dosyasından Geri Yükle", {
                if (!working) importLauncher.launch(arrayOf("application/json"))
            }, enabled = !working, emoji = "⬆️")
        }

        item {
            SectionLabel("eski uygulamadan taşı", "📦")
            Text(
                "Eski uygulamada (Laboratuvar/Çanta) biriken hatalar, tekrar kartları, " +
                    "anlatımlar, Brain Inbox notları, proje/sınav durumları buraya kopyalanır. " +
                    "Eski uygulamadan hiçbir şey silinmez ya da değiştirilmez — orası aynen çalışmayı sürdürür.",
                style = Small,
            )
            Spacer(Modifier.height(6.dp))
            Btn(if (working) "Çalışıyor…" else "Eski Verimi Kopyala", {
                if (!working) {
                    working = true
                    scope.launch {
                        val s = repo.migrateLegacyData(ctx)
                        status = "Taşındı: ${s.mistakes} hata, ${s.flashcards} kart, ${s.recordings} anlatım, " +
                            "${s.inboxNotes} inbox notu, ${s.problems} pratik günlüğü, ${s.reviews} haftalık değerlendirme, " +
                            "${s.projects} proje, ${s.assessments} sınav/ödev (toplam ${s.total})."
                        working = false
                    }
                }
            }, enabled = !working, emoji = "📦")
        }

        status?.let { s ->
            item {
                Column { Text(s, style = Small) }
            }
        }
    }
}
