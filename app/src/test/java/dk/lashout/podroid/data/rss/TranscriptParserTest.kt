package dk.lashout.podroid.data.rss

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TranscriptParserTest {

    private lateinit var parser: TranscriptParser

    @Before
    fun setUp() { parser = TranscriptParser() }

    // ── VTT ──────────────────────────────────────────────────────────────────

    @Test
    fun `parseVtt extracts cue text with HH-MM-SS timestamps`() {
        val vtt = """
            WEBVTT

            00:00:01.000 --> 00:00:04.000
            Hello world

            00:00:05.000 --> 00:00:09.000
            This is a test
        """.trimIndent()

        val result = parser.parseVtt(vtt)
        assertTrue(result.isNotEmpty())
        assertTrue(result.any { it.text.contains("Hello world") || it.text.contains("This is a test") })
    }

    @Test
    fun `parseVtt extracts startMs correctly`() {
        val vtt = """
            WEBVTT

            00:01:30.500 --> 00:01:35.000
            Some text here
        """.trimIndent()

        val result = parser.parseVtt(vtt)
        val first = result.firstOrNull { it.text.contains("Some text here") }
        assertEquals(90_500L, first?.startMs)
    }

    @Test
    fun `parseVtt handles MM-SS timestamps`() {
        val vtt = """
            WEBVTT

            01:05.000 --> 01:10.000
            Short format
        """.trimIndent()

        val result = parser.parseVtt(vtt)
        val first = result.firstOrNull { it.text.contains("Short format") }
        assertEquals(65_000L, first?.startMs)
    }

    @Test
    fun `parseVtt returns empty for blank input`() {
        assertTrue(parser.parseVtt("WEBVTT\n\n").isEmpty())
    }

    // ── SRT ──────────────────────────────────────────────────────────────────

    @Test
    fun `parseSrt extracts segments from standard SRT`() {
        val srt = """
            1
            00:00:01,000 --> 00:00:04,000
            First line

            2
            00:00:05,000 --> 00:00:09,000
            Second line
        """.trimIndent()

        val result = parser.parseSrt(srt)
        assertTrue(result.any { "First line" in it.text || "Second line" in it.text })
    }

    @Test
    fun `parseSrt parses startMs with comma decimal separator`() {
        val srt = """
            1
            00:02:00,000 --> 00:02:05,000
            At two minutes
        """.trimIndent()

        val result = parser.parseSrt(srt)
        val first = result.firstOrNull { "two minutes" in it.text }
        assertEquals(120_000L, first?.startMs)
    }

    // ── JSON ─────────────────────────────────────────────────────────────────

    @Test
    fun `parseJson extracts segments from Podcast Index JSON format`() {
        val json = """{"version":"1.0.0","segments":[{"startTime":0,"endTime":5,"body":"Hello"},{"startTime":10,"endTime":15,"body":"World"}]}"""

        val result = parser.parseJson(json)
        assertEquals(2, result.size)
        assertEquals(0L, result[0].startMs)
        assertEquals("Hello", result[0].text)
        assertEquals(10_000L, result[1].startMs)
        assertEquals("World", result[1].text)
    }

    @Test
    fun `parseJson falls back to plain text for unrecognised JSON`() {
        val result = parser.parseJson("""{"foo":"bar"}""")
        // No segments matched; falls back to parsePlainText which returns single segment
        assertEquals(1, result.size)
    }

    // ── Plain text ───────────────────────────────────────────────────────────

    @Test
    fun `parsePlainText returns single segment spanning full text`() {
        val result = parser.parsePlainText("This is the transcript.")
        assertEquals(1, result.size)
        assertEquals(0L, result[0].startMs)
        assertEquals("This is the transcript.", result[0].text)
    }

    @Test
    fun `parsePlainText returns empty for blank input`() {
        assertTrue(parser.parsePlainText("   ").isEmpty())
    }

    // ── parse dispatch ────────────────────────────────────────────────────────

    @Test
    fun `parse routes to VTT parser for text-vtt mime type`() {
        val vtt = "WEBVTT\n\n00:00:01.000 --> 00:00:03.000\nHello"
        val result = parser.parse(vtt, "text/vtt")
        assertTrue(result.any { "Hello" in it.text })
    }

    @Test
    fun `parse routes to SRT parser for text-srt mime type`() {
        val srt = "1\n00:00:01,000 --> 00:00:03,000\nHello"
        val result = parser.parse(srt, "text/srt")
        assertTrue(result.any { "Hello" in it.text })
    }

    @Test
    fun `parse routes to plain text for unknown mime type`() {
        val result = parser.parse("plain text here", "text/plain")
        assertEquals(1, result.size)
    }
}
