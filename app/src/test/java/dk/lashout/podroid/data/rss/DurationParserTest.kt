package dk.lashout.podroid.data.rss

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationParserTest {

    @Test
    fun `null input returns 0`() {
        assertEquals(0L, DurationParser.parse(null))
    }

    @Test
    fun `blank string returns 0`() {
        assertEquals(0L, DurationParser.parse("   "))
    }

    @Test
    fun `plain seconds string is parsed`() {
        assertEquals(90L, DurationParser.parse("90"))
    }

    @Test
    fun `minutes and seconds format is parsed`() {
        assertEquals(90L, DurationParser.parse("1:30"))
    }

    @Test
    fun `hours minutes seconds format is parsed`() {
        assertEquals(3_661L, DurationParser.parse("1:01:01"))
    }

    @Test
    fun `all-zero duration returns 0`() {
        assertEquals(0L, DurationParser.parse("0:00:00"))
    }

    @Test
    fun `non-numeric string returns 0`() {
        assertEquals(0L, DurationParser.parse("not-a-duration"))
    }

    @Test
    fun `leading and trailing whitespace is trimmed`() {
        assertEquals(60L, DurationParser.parse("  1:00  "))
    }

    @Test
    fun `large hour value is parsed`() {
        assertEquals(36_000L, DurationParser.parse("10:00:00"))
    }
}
