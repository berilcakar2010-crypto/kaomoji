package com.beril.kaomoji.lab.learning

/**
 * Lab 2.0'ın öğrenme motoru (§12-14). "Çalışma süresi" birincil birim değil — bir oturum
 * bilişsel aşamalardan geçer: önce soru/tahmin (§13 — "soru-önce öğrenme"), sonra deneme
 * (kullanıcının denemesi KORUNUR — kendisi öğrenme verisidir), sonra gerektiği kadarıyla
 * açıklama, sonra pratik/bağlantı. Hangi aşamaların gerekli olduğu disipline göre değişir;
 * hiçbiri zorunlu değil, hepsi atlanabilir (`skip`).
 */
object LearningSessionEngine {
    fun stagesFor(discipline: LearningDiscipline): List<LearningStage> = when (discipline) {
        // Matematik: soru → deneme → hata → türetim → çeşitleme → kendi sözleriyle anlatım
        LearningDiscipline.MATH -> listOf(
            LearningStage.QUESTION, LearningStage.ATTEMPT, LearningStage.STRUGGLE,
            LearningStage.DERIVATION, LearningStage.VARIATION, LearningStage.EXPLAIN_OWN_WORDS,
        )
        // Fizik: tahmin → problem → model → hesap → yorumlama → uygulama
        LearningDiscipline.PHYSICS -> listOf(
            LearningStage.ENCOUNTER, LearningStage.QUESTION, LearningStage.ATTEMPT,
            LearningStage.PRACTICE, LearningStage.EXPLAIN_OWN_WORDS, LearningStage.APPLICATION,
        )
        // Programlama: problem → deneme → hata ayıklama → açıklama → uygulama → çeşitleme
        LearningDiscipline.PROGRAMMING -> listOf(
            LearningStage.QUESTION, LearningStage.ATTEMPT, LearningStage.STRUGGLE,
            LearningStage.REVEAL, LearningStage.EXPLAIN_OWN_WORDS, LearningStage.VARIATION,
        )
        // Teori: soru → hipotez → açıklama → karşı örnek → bağlantı → sentez/kalıcılık
        LearningDiscipline.THEORY -> listOf(
            LearningStage.QUESTION, LearningStage.ATTEMPT, LearningStage.EXPLAIN_OWN_WORDS,
            LearningStage.STRUGGLE, LearningStage.CONNECT, LearningStage.RETAIN,
        )
        // Genel/bilinmeyen tür — §12'nin tam 13 aşamalı ilerlemesinin tamamı, sırayla.
        LearningDiscipline.GENERAL -> listOf(
            LearningStage.ENCOUNTER, LearningStage.QUESTION, LearningStage.ATTEMPT,
            LearningStage.STRUGGLE, LearningStage.REVEAL, LearningStage.PRACTICE,
            LearningStage.VARIATION, LearningStage.APPLICATION, LearningStage.DERIVATION,
            LearningStage.EXPLAIN_OWN_WORDS, LearningStage.REFLECT, LearningStage.RETAIN,
            LearningStage.CONNECT,
        )
    }
}

/**
 * Tek bir oturumun değişmez anlık durumu — saf veri, hiçbir I/O yok. UI bunu tutar,
 * [LabRepository] periyodik olarak kalıcı hale getirir (en azından her `advance`'te, böylece
 * kullanıcının denemesi her zaman kaybolmaz).
 */
data class LearningSessionState(
    val discipline: LearningDiscipline,
    val stages: List<LearningStage> = LearningSessionEngine.stagesFor(discipline),
    val stageIndex: Int = 0,
    val attemptText: String = "",
    val completed: Boolean = false,
) {
    val currentStage: LearningStage get() = stages[stageIndex]
    val isLastStage: Boolean get() = stageIndex == stages.lastIndex

    fun withAttempt(text: String): LearningSessionState = copy(attemptText = text)

    /** Sıradaki aşamaya geçer; son aşamadaysa oturumu tamamlanmış işaretler. Zorla bir
     *  aşama atlanmaz — her zaman bir sonraki, hiçbir zaman rastgele bir sıçrama. */
    fun advance(): LearningSessionState =
        if (isLastStage) copy(completed = true) else copy(stageIndex = stageIndex + 1)

    /** Kullanıcı bir aşamayı bilerek atlamak isterse (§12 — "not every topic requires every
     *  stage"). Sistem kendiliğinden atlamaz, bu her zaman kullanıcının kararıdır. */
    fun skipCurrentStage(): LearningSessionState = advance()
}
