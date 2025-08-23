package com.lazymohan.zebraprinter.grn.util

import org.junit.Assert.*
import org.junit.Test

class MatchHelpersTest {

    @Test
    fun testBestMatchIndex_basicMatch() {
        val items = listOf(
            ExtractedItem("Urapidil 50 mg/10 ml amp 5's Tachyben", 10.0, "2025-12-31", "B123"),
            ExtractedItem("Ibuprofen 200mg", 5.0, "2025-11-30", "B124"),
            ExtractedItem("Paracetamol 650mg", 8.0, "2025-10-15", "B125")
        )
        val used = mutableSetOf<Int>()
        val idx = bestMatchIndex(items, "TACHYBEN (URAPIDIL) 50MG/10ML AMP 5'S", used)
        assertEquals(0, idx)
    }

    @Test
    fun testBestMatchIndex_skipsUsed() {
        val items = listOf(
            ExtractedItem("Paracetamol 500mg", 10.0, "2025-12-31", "B123"),
            ExtractedItem("Ibuprofen 200mg", 5.0, "2025-11-30", "B124"),
            ExtractedItem("Paracetamol 650mg", 8.0, "2025-10-15", "B125")
        )
        val used = mutableSetOf(0)
        val idx = bestMatchIndex(items, "Paracetamol", used)
        assertEquals(2, idx)
    }

    @Test
    fun testBestMatchIndex_noMatchBelowThreshold() {
        val items = listOf(
            ExtractedItem("Aspirin 100mg", 10.0, "2025-12-31", "B123"),
            ExtractedItem("Ibuprofen 200mg", 5.0, "2025-11-30", "B124")
        )
        val used = mutableSetOf<Int>()
        val idx = bestMatchIndex(items, "Paracetamol", used, threshold = 0.5)
        assertNull(idx)
    }
}