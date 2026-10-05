package com.beril.kaomoji.ui.lab2

import org.junit.Assert.assertEquals
import org.junit.Test

class Lab2BreakpointTest {

    @Test
    fun `narrow widths (folded, phone) are compact`() {
        assertEquals(Lab2Breakpoint.COMPACT, breakpointFor(360))
        assertEquals(Lab2Breakpoint.COMPACT, breakpointFor(599))
    }

    @Test
    fun `the primary target — tablet held in portrait — gets its own tier`() {
        assertEquals(Lab2Breakpoint.TABLET_PORTRAIT, breakpointFor(600))
        assertEquals(Lab2Breakpoint.TABLET_PORTRAIT, breakpointFor(800))
        assertEquals(Lab2Breakpoint.TABLET_PORTRAIT, breakpointFor(899))
    }

    @Test
    fun `wide widths (tablet landscape, unfolded) get the secondary-panel tier`() {
        assertEquals(Lab2Breakpoint.TABLET_LANDSCAPE, breakpointFor(900))
        assertEquals(Lab2Breakpoint.TABLET_LANDSCAPE, breakpointFor(1280))
    }
}
