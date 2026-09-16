package com.beril.glowup.ui.nav

import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText

/**
 * 2 tuşlu cihaz (D-pad yön + onay) için dikey liste odaklı focus navigasyonu.
 * Dokunmatik varsayımı yapmaz: her odaklanabilir görünüm net bir seçili durum
 * gösterir, ONAY tuşu her zaman o an odaklı görünümü tetikler.
 *
 * [EditText] alanları zincire dahil edilir (yukarı/aşağı ile erişilebilir) ama
 * ONAY tuşu bu alanlarda tetiklenmez — sistemin varsayılan metin düzenleme
 * davranışı (imleç/yazma) ezilmez.
 */
object DpadFocusHelper {

    /** [container] içindeki doğrudan çocukları dikey bir D-pad zinciri haline getirir. */
    fun dikeyZincirKur(container: ViewGroup, onSecildi: (View) -> Unit) {
        val odaklanabilirler = (0 until container.childCount)
            .map { container.getChildAt(it) }
            .filter { it.visibility == View.VISIBLE }
        zincirKur(odaklanabilirler, onSecildi)
    }

    /**
     * Verilen sırayla, iç içe konteynerlere dağılmış görünümleri de tek bir dikey
     * D-pad zinciri haline getirir (ör. bir ekrandaki birden çok seçim grubu +
     * eylem düğmeleri tek zincirde art arda gezilebilir).
     */
    fun zincirKur(siraliGorunumler: List<View>, onSecildi: (View) -> Unit) {
        val odaklanabilirler = siraliGorunumler.filter { it.visibility == View.VISIBLE }

        odaklanabilirler.forEachIndexed { index, view ->
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.nextFocusUpId = if (index > 0) odaklanabilirler[index - 1].id else view.id
            view.nextFocusDownId = if (index < odaklanabilirler.size - 1) odaklanabilirler[index + 1].id else view.id

            if (view is EditText) return@forEachIndexed

            view.setOnKeyListener { v, keyCode, event ->
                if (event.action == KeyEvent.ACTION_UP &&
                    (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)
                ) {
                    onSecildi(v)
                    true
                } else {
                    false
                }
            }
        }

        odaklanabilirler.firstOrNull()?.requestFocus()
    }
}
