package com.beril.kaomoji.ui.lab

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
import com.beril.kaomoji.ai.engine.AICapabilityGate
import com.beril.kaomoji.ai.engine.AIResult
import com.beril.kaomoji.lab.curriculum.LegacyCurriculumAdapter
import com.beril.kaomoji.lab.repository.LabRepository
import com.beril.kaomoji.ui.Btn
import com.beril.kaomoji.ui.Display
import com.beril.kaomoji.ui.Field
import com.beril.kaomoji.ui.GhostBtn
import com.beril.kaomoji.ui.J
import com.beril.kaomoji.ui.SectionLabel
import com.beril.kaomoji.ui.Small
import com.beril.kaomoji.ui.TitleM
import kotlinx.coroutines.launch

/**
 * Belgeden AI ile taslak müfredat üretme. Lab kendi müfredatını yazmaz — bir dış Curriculum
 * Contract içe aktarır (bkz. "Müfredatı Yenile", gerçek bir 123-nesnelik paket). Bu ekran onun
 * YERİNE geçmez: kullanıcının resmi bir paketi yoksa, kendi ders programı/syllabusundan bir
 * BAŞLANGIÇ TASLAĞI çıkarmak için var. AI çıktısı asla otomatik uygulanmaz — taslağı önce
 * özetiyle gösterir, "İçe Aktar" demeden hiçbir KnowledgeObjectEntity yazılmaz (§5).
 *
 * İçe aktarmak, aynı `packageId`'ye sahip önceki bir AI taslağını KALICI OLARAK DEĞİŞTİRİR —
 * bu kasıtlı (§17 "müfredat tamamen değiştirilebilir olmalı"), ama kullanıcıya açıkça söylenir.
 */
@Composable
fun CurriculumGenScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { LabRepository(ctx) }
    val gate = remember { AICapabilityGate.forContext(ctx) }
    val scope = rememberCoroutineScope()

    var docTitle by remember { mutableStateOf("") }
    var docText by remember { mutableStateOf("") }
    var generating by remember { mutableStateOf(false) }
    var genResult by remember { mutableStateOf<AIResult<String>?>(null) }
    var importing by remember { mutableStateOf(false) }
    var importMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            GhostBtn("Geri", onBack, emoji = "←")
            Spacer(Modifier.height(10.dp))
            Text("📄 Belgeden Müfredat Taslağı", style = Display)
            Text(
                "Resmi bir müfredat paketin yoksa: bir ders programı/syllabus/not yapıştır, " +
                    "AI uygulamanın anlayacağı bir TASLAK yapı çıkarır. Hiçbir şey otomatik " +
                    "uygulanmaz — taslağı gözden geçirip \"İçe Aktar\" demen gerekir, ve içe " +
                    "aktarmak bu kaynaktan önceki bir taslağı kalıcı olarak değiştirir.",
                style = Small,
            )
        }

        item {
            SectionLabel("belge", "📝")
            Spacer(Modifier.height(6.dp))
            Field(docTitle, { docTitle = it }, placeholder = "Belge başlığı (örn. \"10. Sınıf Fizik Programı\")", single = true)
            Spacer(Modifier.height(6.dp))
            Field(docText, { docText = it }, placeholder = "Ders programını/syllabusu buraya yapıştır", minLines = 8)
            Spacer(Modifier.height(8.dp))
            Btn(if (generating) "Oluşturuluyor…" else "AI'dan Taslak Oluştur", {
                if (!generating && docText.isNotBlank()) {
                    generating = true
                    importMessage = null
                    scope.launch {
                        genResult = gate.generateCurriculum(docTitle.ifBlank { "Müfredat" }, docText)
                        generating = false
                    }
                }
            }, enabled = !generating, emoji = "🤖")
        }

        item {
            AiResultView(genResult) { raw ->
                val preview = runCatching { LegacyCurriculumAdapter.toContractPackage(raw, "ai-generated-preview") }
                Column {
                    SectionLabel("taslak", "✨")
                    Spacer(Modifier.height(6.dp))
                    preview.fold(
                        onSuccess = { pkg ->
                            Text(
                                "${pkg.title} — ${pkg.units.size} birim, ${pkg.concepts.size} kavram, " +
                                    "${pkg.questions.size} görev",
                                style = TitleM,
                            )
                        },
                        onFailure = { e ->
                            Text(
                                "Taslak doğrulanamadı: ${e.message ?: "geçersiz JSON"}",
                                style = Small.copy(color = J.cherry),
                            )
                        },
                    )
                    if (preview.isSuccess) {
                        Spacer(Modifier.height(8.dp))
                        Btn(if (importing) "İçe aktarılıyor…" else "İçe Aktar", {
                            if (!importing) {
                                importing = true
                                scope.launch {
                                    val outcome = runCatching { repo.importGeneratedCurriculum(raw) }
                                    importMessage = outcome.fold(
                                        onSuccess = { "İçe aktarıldı." },
                                        onFailure = { e -> "İçe aktarılamadı: ${e.message}" },
                                    )
                                    importing = false
                                }
                            }
                        }, enabled = !importing, emoji = "✓")
                    }
                    importMessage?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, style = Small)
                    }
                }
            }
        }
    }
}
