package com.beril.kaomoji.ui.nav

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * 2 tuşlu (D-pad yön + onay) cihazlar için Compose odak desteği.
 *
 * Compose'un `clickable` değiştiricisi zaten fiziksel ONAY/ENTER tuşuna ve D-pad
 * yön tuşlarıyla odak taşınmasına yerleşik olarak yanıt verir — eksik olan,
 * kullanıcının dokunmadan hangi öğenin odaklı olduğunu **görebilmesiydi**. Bu
 * modifier tek bir şeyi tüm uygulamada standartlaştırıyor: odaklanınca beliren
 * net, tutarlı bir çerçeve (mor — VOIDLAB vurgu rengiyle aynı).
 */
fun Modifier.dpadFocusable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    focusColor: Color = Color(0xFF9D5CFF)
): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    this
        .onFocusChanged { focused = it.isFocused }
        .then(if (focused) Modifier.border(2.dp, focusColor, shape) else Modifier)
        .clickable(enabled = enabled) { onClick() }
}

/** [focusRequester] verilen bir öğeye, D-pad zincirinde belirli bir başlangıç noktası kurmak için kullanılır. */
fun Modifier.dpadFocusable(
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    focusColor: Color = Color(0xFF9D5CFF)
): Modifier = this.focusRequester(focusRequester).dpadFocusable(onClick, enabled, shape, focusColor)
