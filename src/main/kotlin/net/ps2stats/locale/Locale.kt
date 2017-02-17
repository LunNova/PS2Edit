package net.ps2stats.locale

import java.io.File
import java.io.OutputStreamWriter

class Locale {
    val entries = mutableListOf<Entry>()
    val entriesById = mutableMapOf<Int, Entry>()

    private fun dat(dir: File) = File(dir.parent, dir.nameWithoutExtension + ".dat")

    fun parseEntry(data: String): Entry {
        val first = data.indexOf('\t')
        val id = data.substring(0, first).toInt()
        val second = data.indexOf('\t', first + 1)
        val flags = data.substring(first + 1, second)
        return Entry(id, flags, data.substring(second + 1))
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
            val id = parts[0].toInt()
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

        dat.outputStream().writer().use { datWriter: OutputStreamWriter ->
            dir.outputStream().writer().use { dirWriter: OutputStreamWriter ->
                datWriter.append(0xEF.toChar()).append(0xBB.toChar()).append(0xBF.toChar())
                var offset = 3

                entries.forEach {
                    val length = it.id.toString().length + it.flags.length + it.text.length + 2;
                    dirWriter.write(it.id.toString() + '\t' + offset + '\t' + length + "d\r\n")
                    datWriter.write(it.id.toString() + '\t' + it.flags + '\t' + it.text)
                    offset += length + 2
                }
            }
        }
    }

    data class Entry(val id: Int, val flags: String, val text: String)
}
