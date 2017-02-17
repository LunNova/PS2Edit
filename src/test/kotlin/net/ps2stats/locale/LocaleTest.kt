package net.ps2stats.locale

import org.junit.Test
import kotlin.test.assertEquals

class LocaleTest {
    val locale = Locale()

    @Test
    fun testParseEntry() {
        val entry = locale.parseEntry("533205\tucdt\tDaum Title 006 Bundle Name\r\n")
        assertEquals(533205, entry.id)
        assertEquals("ucdt", entry.flags)
        assertEquals("Daum Title 006 Bundle Name\r\n", entry.text)
    }
}
