package com.beril.kaomoji.ui.lab

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.beril.kaomoji.ai.engine.AIResult
import com.beril.kaomoji.ui.J
import com.beril.kaomoji.ui.Small
import com.beril.kaomoji.ui.TitleM

/**
 * [AIResult]'ın her ekranda aynı şekilde gösterilmesi (§7/§48) — Offline bir hata gibi
 * kırmızı değil, "yerel çalışma alanın hâlâ kullanılabilir" diyen sakin bir not; Failure
 * okunabilir bir mesaj; Success gerçek içerik.
 *
 * Dıştaki `Column` her zaman var (sonuç null olsa da) — bu yüzden `animateContentSize()`
 * bir sonucun ilk belirişini (yükseklik 0'dan içeriğe) ve sonuçlar arası değişimi (örn.
 * Offline'dan Success'e) ani bir sıçrama değil, yumuşak bir büyüme olarak gösterebiliyor
 * (§11/§38 — tek yerde eklendiği için bu bileşeni kullanan HER ekrana yayılıyor).
 */
@Composable
fun <T> AiResultView(result: AIResult<T>?, renderSuccess: @Composable (T) -> Unit) {
    Column(Modifier.fillMaxWidth().animateContentSize()) {
        when (result) {
            null -> Unit
            is AIResult.Offline -> Column(
                Modifier.fillMaxWidth().background(J.paperDeep, RoundedCornerShape(12.dp)).padding(12.dp),
            ) {
                Text("Şu an çevrimdışı", style = TitleM)
                Text(
                    "Gemini/Groq'a ulaşılamadı ya da bir API anahtarı girilmemiş. Yerel çalışma " +
                        "alanın bundan etkilenmiyor — AI özellikleri tamamen opsiyonel.",
                    style = Small,
                )
            }
            is AIResult.Failure -> Column(
                Modifier.fillMaxWidth().background(J.blush.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(12.dp),
            ) {
                Text("AI isteği başarısız oldu", style = TitleM)
                Text(result.message, style = Small)
            }
            is AIResult.Success -> renderSuccess(result.value)
        }
    }
}
