package net.ps2stats.locale

import org.junit.Test
import java.io.File
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

	@Test
	fun testLoadSave() {
		val locale = Locale()
		val before = File("Test Data/locale.dir")
		val after = File("Test Data/locale_saved.dir")
		locale.load(before)
		locale.save(after)
		val localeAfter = Locale()
		localeAfter.load(after)
		assertEquals(localeAfter.entries.take(1000), locale.entries.take(1000))
	}
}
