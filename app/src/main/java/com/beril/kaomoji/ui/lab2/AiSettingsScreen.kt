package com.beril.kaomoji.ui.lab2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.beril.kaomoji.ai.ApiKeyStore
import com.beril.kaomoji.ai.AiProvider
import com.beril.kaomoji.ui.Btn
import com.beril.kaomoji.ui.Display
import com.beril.kaomoji.ui.Field
import com.beril.kaomoji.ui.GhostBtn
import com.beril.kaomoji.ui.J
import com.beril.kaomoji.ui.SectionLabel
import com.beril.kaomoji.ui.Selector
import com.beril.kaomoji.ui.Small
import com.beril.kaomoji.ui.TitleM

/**
 * AI sağlayıcısı + API anahtarı ayarları (§35). Aşama 15'te eski ekranlar (CurriculumGenScreen,
 * EvaluationScreen, AudioScreens vb.) silinirken, anahtarı girebilecek TEK yer de onlarla
 * birlikte gitti — `AICapabilityGate.forContext` hâlâ `ApiKeyStore`'dan okuyordu ama hiçbir
 * ekran artık oraya yazamıyordu, yani her AI özelliği (açıkla/değerlendir/not düzenle/yazım
 * yardımı/plan öner) sessizce hep `AIResult.Offline` dönüyordu. Bu ekran o boşluğu kapatıyor.
 *
 * Anahtar hiçbir zaman bu ekranın dışına çıkmaz — [ApiKeyStore] onu Android Keystore destekli
 * `EncryptedSharedPreferences` ile saklar (§35), uygulama verisiyle birlikte dışa aktarılmaz.
 */
@Composable
fun AiSettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var provider by remember { mutableStateOf(ApiKeyStore.provider(ctx)) }
    var key by remember { mutableStateOf(ApiKeyStore.get(ctx, provider).orEmpty()) }
    var savedTick by remember { mutableStateOf(0) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            GhostBtn("Geri", onBack, emoji = "←")
            Spacer(Modifier.height(10.dp))
            Text("🤖 AI Ayarları", style = Display)
            Text(
                "AI tamamen opsiyonel — sadece burada kendi API anahtarını girersen çalışır. " +
                    "Anahtar girmezsen uygulama hiçbir zaman internete çıkmaz; AI özellikleri " +
                    "(açıklama/değerlendirme/not düzenleme/yazım yardımı/plan önerisi) sessizce " +
                    "devre dışı kalır, hiçbir şey bozulmaz.",
                style = Small,
            )
        }

        item {
            SectionLabel("sağlayıcı", "🔌")
            Spacer(Modifier.height(6.dp))
            Selector(
                options = AiProvider.entries.map { it.name },
                selected = provider.name,
                onSelect = { name ->
                    provider = AiProvider.fromName(name)
                    key = ApiKeyStore.get(ctx, provider).orEmpty()
                },
                labels = { name -> AiProvider.fromName(name).label },
            )
        }

        item {
            SectionLabel("api anahtarı", "🔑")
            Spacer(Modifier.height(6.dp))
            Text("${provider.keySource}.", style = Small)
            Spacer(Modifier.height(6.dp))
            Field(key, onChange = { key = it }, placeholder = provider.keyHint, single = true)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn("Kaydet", {
                    ApiKeyStore.setProvider(ctx, provider)
                    ApiKeyStore.set(ctx, key, provider)
                    savedTick++
                }, emoji = "💾")
                GhostBtn("Anahtarı Temizle", {
                    ApiKeyStore.clear(ctx, provider)
                    key = ""
                    savedTick++
                }, emoji = "🗑️")
            }
        }

        item {
            val storedKey = remember(savedTick, provider) { ApiKeyStore.get(ctx, provider) }
            val configured = !storedKey.isNullOrBlank()
            Column {
                Text(
                    if (configured) "✓ ${provider.label} için anahtar kayıtlı" else "Henüz anahtar girilmedi",
                    style = TitleM.copy(color = if (configured) J.forest else J.inkFaint),
                )
            }
        }
    }
}
