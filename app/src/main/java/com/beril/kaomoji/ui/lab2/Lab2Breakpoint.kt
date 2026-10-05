package com.beril.kaomoji.ui.lab2

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
enum class Lab2Breakpoint { COMPACT, TABLET_PORTRAIT, TABLET_LANDSCAPE }

fun breakpointFor(widthDp: Int): Lab2Breakpoint = when {
    widthDp < 600 -> Lab2Breakpoint.COMPACT
    widthDp < 900 -> Lab2Breakpoint.TABLET_PORTRAIT
    else -> Lab2Breakpoint.TABLET_LANDSCAPE
}
