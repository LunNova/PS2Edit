package net.ps2stats.locale

import java.io.DataOutputStream
import java.io.File
import java.security.MessageDigest

class Locale {
    val entries = mutableListOf<Entry>()
    val entriesById = mutableMapOf<Long, Entry>()

    private fun dat(dir: File) = File(dir.parent, dir.nameWithoutExtension + ".dat")

    fun parseEntry(data: String): Entry {
		try {
			val first = data.indexOf('\t')
			val id = data.substring(0, first).toLong()
			val second = data.indexOf('\t', first + 1)
			val flags = data.substring(first + 1, second)
			return Entry(id, flags, data.substring(second + 1))
		} catch (t: Throwable) {
			throw IllegalStateException("Failed to parse entry from $data", t)
		}
    }

    fun load(dir: File) {
        entries.clear()
        entriesById.clear()

        val dat = dat(dir).readBytes()
        val idx = dir.readLines(Charsets.UTF_8)

        idx.forEach {
            if (it[0] == '#')
                return@forEach

            // Index line:
            // 93264	3	47	d
            // Data line:
            // 93264	ucdt	Gunner Kill Assist Share - Lightning

            val parts = it.split('\t')
            val id = parts[0].toLong()
            val start = parts[1].toInt()
            val length = parts[2].toInt()

            val data = String(dat, start, length, Charsets.UTF_8)
            val entry = parseEntry(data)

            if (entry.id != id)
                error("ID mismatch for line:\n$it\ndata:\n$data")

            entries.add(entry)
            entriesById[entry.id] = entry
        }
    }

    fun save(dir: File) {
        val dat = dat(dir)
		val dirContents = StringBuilder()

		DataOutputStream(dat.outputStream()).buffered().use { datWriter ->
			datWriter.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
			var offset = 3

			entries.forEach {
				val data = (it.id.toString() + '\t' + it.flags + '\t' + it.text + "\r\n").toByteArray(Charsets.UTF_8)
				dirContents.append(it.id.toString() + '\t' + offset + '\t' + (data.size - 2) + "\td\r\n")
				datWriter.write(data)
				offset += data.size
			}
			datWriter.write("\r\n".toByteArray(Charsets.UTF_8))
		}

		val md5 = MessageDigest.getInstance("MD5").digest(dat.readBytes())
		val md5text = StringBuffer()
		for (i in 0..md5.size - 1) {
			md5text.append(Integer.toHexString(md5[i].toInt() and 0xFF or 0x100).substring(1, 3))
		}
		dir.writeText("## Count:\t${entries.size}\r\n## MD5Checksum: ${md5text.toString().toUpperCase()}\r\n" + dirContents, Charsets.UTF_8)
    }

    data class Entry(val id: Long, val flags: String, var text: String)
}
