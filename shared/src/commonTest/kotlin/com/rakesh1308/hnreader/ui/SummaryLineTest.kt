package com.rakesh1308.hnreader.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class SummaryLineTest {
    @Test fun plural() = assertEquals("30 stories shown · 2 saved", summaryLine(30, 2))
    @Test fun singular() = assertEquals("1 story shown · 0 saved", summaryLine(1, 0))
    @Test fun empty() = assertEquals("0 stories shown · 0 saved", summaryLine(0, 0))
}
