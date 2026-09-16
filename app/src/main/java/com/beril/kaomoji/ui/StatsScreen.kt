package com.beril.kaomoji.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.beril.kaomoji.data.DailyLog
import com.beril.kaomoji.data.Store
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Zamana bağlı takip edilen istatistikler — gün, hafta, ay ve yıl bazında.
 * Kaynak veri [Store.dailyLogs] (her görev/kart tekrarında otomatik yazılır,
 * bkz. Store.logToday). Kütüphane bağımlılığı yok, sade tek renkli çubuklar.
 */
@Composable
fun StatsScreen(store: Store, onBack: () -> Unit) {
    val periods = listOf("Gün", "Hafta", "Ay", "Yıl")
    var period by remember { mutableStateOf(periods[0]) }
    val logs = store.dailyLogs.values.toList()

    val buckets = remember(period, logs.size) { aggregate(logs, period) }
    val maxMinutes = buckets.maxOfOrNull { it.minutesLogged } ?: 0
    val maxTasks = buckets.maxOfOrNull { it.tasksDone } ?: 0

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            GhostBtn("Geri", onBack, emoji = "←")
            Spacer(Modifier.height(10.dp))
            Text("📈 İstatistikler", style = Display)
            Text("Gün, hafta, ay ve yıl bazında çalışma özeti.", style = Small)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatTile("${store.currentStreak}", "günlük seri", "🔥", J.cherry)
                StatTile("${store.totalDone}", "toplam görev", "✅", J.forest)
                StatTile("${store.flashcards.size}", "tekrar kartı", "🃏", J.lilac)
            }
        }

        item {
            Selector(periods, period, { period = it })
        }

        if (buckets.isEmpty()) {
            item { Empty("📈", "Henüz veri yok", "Bir görev tamamlandığında burada görünecek.") }
        } else {
            item { SectionLabel("çalışma dakikası", "⏱️") }
            items(buckets) { b -> BarRow(b.label, b.minutesLogged, maxMinutes, J.forest, "dk") }

            item {
                Spacer(Modifier.height(6.dp))
                SectionLabel("tamamlanan görev", "✅")
            }
            items(buckets) { b -> BarRow(b.label, b.tasksDone, maxTasks, J.cherry, "") }
        }
    }
}

@Composable
private fun BarRow(label: String, value: Int, maxValue: Int, color: androidx.compose.ui.graphics.Color, suffix: String) {
    val maxBarDp = 160.dp
    val barDp = if (maxValue > 0 && value > 0)
        (maxBarDp.value * value.toFloat() / maxValue.toFloat()).dp.coerceAtLeast(4.dp)
    else 0.dp

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = Tiny, modifier = Modifier.width(64.dp))
        Box(
            Modifier
                .width(barDp)
                .height(14.dp)
                .background(color, androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text("$value$suffix", style = Tiny.copy(color = J.inkFaint))
    }
    Spacer(Modifier.height(4.dp))
}

private data class Bucket(val key: String, val label: String, var tasksDone: Int = 0, var cardsReviewed: Int = 0, var minutesLogged: Int = 0)

private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

/** Günlük kayıtları seçilen periyoda göre gruplar, kronolojik sıralar, en son N öğeyi döner. */
private fun aggregate(logs: List<DailyLog>, period: String): List<Bucket> {
    if (logs.isEmpty()) return emptyList()

    val buckets = linkedMapOf<String, Bucket>()
    logs.forEach { log ->
        val date = try { dayFormat.parse(log.dayKey) } catch (_: Exception) { null } ?: return@forEach
        val cal = Calendar.getInstance().apply { time = date }

        val (key, label) = when (period) {
            "Gün" -> log.dayKey to log.dayKey.substring(5) // MM-dd
            "Hafta" -> {
                val yil = cal.get(Calendar.YEAR)
                val hafta = cal.get(Calendar.WEEK_OF_YEAR)
                "$yil-W%02d".format(hafta) to "$yil W$hafta"
            }
            "Ay" -> log.dayKey.substring(0, 7) to log.dayKey.substring(0, 7)
            else -> log.dayKey.substring(0, 4) to log.dayKey.substring(0, 4) // Yıl
        }

        val b = buckets.getOrPut(key) { Bucket(key, label) }
        b.tasksDone += log.tasksDone
        b.cardsReviewed += log.cardsReviewed
        b.minutesLogged += log.minutesLogged
    }

    val siraliListe = buckets.values.sortedBy { it.key }
    val limit = when (period) {
        "Gün" -> 14
        "Hafta" -> 8
        "Ay" -> 12
        else -> Int.MAX_VALUE // Yıl: tüm geçmiş — kaydın başladığı yıla kadar
    }
    return siraliListe.takeLast(limit)
}
