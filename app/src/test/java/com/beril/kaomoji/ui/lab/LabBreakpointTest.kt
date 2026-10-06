package com.beril.kaomoji.ui.lab

import org.junit.Assert.assertEquals
import org.junit.Test

class LabBreakpointTest {

    @Test
    fun `narrow widths (folded, phone) are compact`() {
        assertEquals(LabBreakpoint.COMPACT, breakpointFor(360))
        assertEquals(LabBreakpoint.COMPACT, breakpointFor(599))
    }

    @Test
    fun `the primary target — tablet held in portrait — gets its own tier`() {
        assertEquals(LabBreakpoint.TABLET_PORTRAIT, breakpointFor(600))
        assertEquals(LabBreakpoint.TABLET_PORTRAIT, breakpointFor(800))
        assertEquals(LabBreakpoint.TABLET_PORTRAIT, breakpointFor(899))
    }

    @Test
    fun `wide widths (tablet landscape, unfolded) get the secondary-panel tier`() {
        assertEquals(LabBreakpoint.TABLET_LANDSCAPE, breakpointFor(900))
        assertEquals(LabBreakpoint.TABLET_LANDSCAPE, breakpointFor(1280))
    }
}
