package com.beril.kaomoji.ai.engine

/**
 * Her AI çağrısının tek tip sonucu. Hiçbir çağrı yeri "internet yoksa ne olacak" sorusunu
 * kendi try/catch'iyle çözmek zorunda kalmaz (§7/§48) — ya veri gelir, ya anlaşılır bir hata
 * gelir, ya da "şu an çevrimdışı" gelir; üçü de birbirinden tip olarak ayrı, biri diğerini
 * taklit edemez.
 */
sealed class AIResult<out T> {
    data class Success<T>(val value: T) : AIResult<T>()
    data class Failure(val message: String) : AIResult<Nothing>()
    /** Sağlayıcı yapılandırılmamış ya da ağ ulaşılamaz — "hata" değil, beklenen bir durum.
     *  Hiçbir ekran bunu kırmızı bir hata gibi göstermemeli; "yerel çalışma alanın hâlâ
     *  kullanılabilir" demeli (§7/§48). */
    object Offline : AIResult<Nothing>()
}

internal inline fun <T> runAICall(block: () -> T): AIResult<T> = try {
    AIResult.Success(block())
} catch (e: java.io.IOException) {
    AIResult.Offline
} catch (e: Exception) {
    AIResult.Failure(e.message ?: "Beklenmeyen bir hata oluştu.")
}
