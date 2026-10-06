package com.beril.kaomoji.ui.lab

/**
 * Portrait-tablet öncelikli üç katman (mimari değerlendirme §7/§31). Saf bir genişlik→katman
 * fonksiyonu — Compose'dan bağımsız, birim testinde doğrulanabilir. Eşikler:
 *  - <600dp: COMPACT (katlanabilir kapalı ekran, telefon) — tek sütun, rail yok
 *  - 600–900dp: TABLET_PORTRAIT (asıl hedef — tablet dik tutulduğunda) — kalıcı sol rail
 *  - >900dp: TABLET_LANDSCAPE (tablet yatay / katlanabilir açık) — rail + içerik + yan panel
 *
 * 600dp eşiği rastgele değil — Android'in kendi WindowSizeClass "medium" eşiğiyle aynı, yani
 * gerçek cihaz verisine dayanıyor, uydurma bir sayı değil.
 */
enum class LabBreakpoint { COMPACT, TABLET_PORTRAIT, TABLET_LANDSCAPE }

fun breakpointFor(widthDp: Int): LabBreakpoint = when {
    widthDp < 600 -> LabBreakpoint.COMPACT
    widthDp < 900 -> LabBreakpoint.TABLET_PORTRAIT
    else -> LabBreakpoint.TABLET_LANDSCAPE
}
