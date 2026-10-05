package com.beril.kaomoji.lab.learning

/**
 * Bir öğrenme oturumunun geçebileceği bilişsel adımlar (§12'nin 13 aşamalık listesi). Bu
 * listenin TAMAMI her konu için zorunlu değil — [LearningSessionEngine.stagesFor] her
 * disiplin için anlamlı bir alt küme seçer. Bu dosya MEKANİZMAdır, içerik değil: hangi
 * konunun hangi aşamayı gerektirdiğine müfredat/kullanıcı karar verir, bu enum değil (§51).
 */
enum class LearningStage {
    ENCOUNTER,
    QUESTION,
    ATTEMPT,
    STRUGGLE,
    REVEAL,
    PRACTICE,
    VARIATION,
    APPLICATION,
    DERIVATION,
    EXPLAIN_OWN_WORDS,
    REFLECT,
    RETAIN,
    CONNECT,
}

/**
 * §12'nin verdiği örnekler ("Matematik: soru→deneme→hata→türetim→çeşitleme→ispat" vb.)
 * disipline göre farklı aşama alt kümeleri gerektirdiğini gösteriyor. Bu enum içerik değil —
 * sadece hangi aşama dizisinin seçileceğine karar veren bir anahtar. Kullanıcı ya da müfredat
 * paketi bir Concept'e hangi disiplinin uygulanacağını işaretler; uygulama kendisi bir konunun
 * "matematik mi fizik mi" olduğuna karar vermez.
 */
enum class LearningDiscipline {
    MATH,
    PHYSICS,
    PROGRAMMING,
    THEORY,
    GENERAL,
}
