package com.enderthor.kCustomField.datatype

import org.junit.Assert.assertEquals
import org.junit.Test

class TrimNumberTest {

    @Test
    fun `drops the fraction instead of truncating the integer part`() {
        assertEquals("1234", trimNumberTo3Chars("1234.5"))
        assertEquals("123", trimNumberTo3Chars("123.4"))
        assertEquals("-123", trimNumberTo3Chars("-123.4"))
    }

    @Test
    fun `keeps short decimals and leaves non-decimal strings alone`() {
        assertEquals("25", trimNumberTo3Chars("25.4"))
        assertEquals("9.8", trimNumberTo3Chars("9.8"))
        assertEquals("-12", trimNumberTo3Chars("-12.3"))
        assertEquals("47/53", trimNumberTo3Chars("47/53"))
        assertEquals("12.3k", trimNumberTo3Chars("12.3k"))
    }
}
