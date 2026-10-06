package com.beril.kaomoji.ui.lab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.beril.kaomoji.ui.Display
import com.beril.kaomoji.ui.GhostBtn
import com.beril.kaomoji.ui.Small

/**
 * "Projeler" alanının ana ekranı (§31) — uzun soluklu çalışmalar: bağımsız projeler, yaklaşan
 * sınav/ödev hazırlığı, ve bir ders programından AI ile taslak müfredat çıkarma (bu üçü de
 * "bir şey inşa ediyorum" kategorisine giriyor, günlük öğrenme akışından ayrı).
 */
@Composable
fun LabProjectsHomeScreen(
    onOpenProjects: () -> Unit,
    onOpenExams: () -> Unit,
    onOpenCurriculumGen: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("⚗️ Projeler", style = Display)
            Text("Bağımsız projeler, sınav/ödev hazırlığı, ve belgeden taslak müfredat çıkarma.", style = Small)
            Spacer(Modifier.height(10.dp))
            GhostBtn("Projeler", onOpenProjects, emoji = "⚗️")
            Spacer(Modifier.height(8.dp))
            GhostBtn("Sınavlar", onOpenExams, emoji = "📋")
            Spacer(Modifier.height(8.dp))
            GhostBtn("Belgeden Taslak Müfredat Oluştur", onOpenCurriculumGen, emoji = "📄")
        }
    }
}
