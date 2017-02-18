package net.ps2stats.locale

import java.io.File

class LocaleReplacements {
    val textSubstitutions = mutableMapOf<String, String>()
	val textSubstitutionsCaseSensitive = mutableMapOf<String, String>()
    val idSubstitutions = mutableMapOf<Long, String>()

    fun load(file: File) {
        val default = File(file, "default.cfg")
        if (!default.exists())
            default.writeText("""#Locale Substitutions
#Format:
#ID:text
# OR
#sub:
#from
#to

""")

        val files = file.listFiles(File::isFile)
        files.forEach { file ->
            var inSub = 0
			var caseSensitive = false
			var caseSensitiveAuto = false
            var from = ""
            file.forEachLine {
                var line = it
                if (line.isBlank() || line[0] == '#')
                    return@forEachLine

                if (inSub > 0) {
                    if (!line.startsWith('\t'))
                        error("Substitution lines should be indented with tab character")
                    line = line.trimStart('\t').replace("\\n", "\r\n")
                    if (inSub == 2) {
                        from = line
                    } else {
						if (caseSensitiveAuto) {
							textSubstitutionsCaseSensitive[from] = line
							textSubstitutionsCaseSensitive[from.toUpperCase()] = line.toUpperCase()
							textSubstitutionsCaseSensitive[from.toLowerCase()] = line.toLowerCase()
						} else {
							(if (caseSensitive) textSubstitutionsCaseSensitive else textSubstitutions)[from] = line
						}
                        from = ""
                    }
                    inSub--

                    return@forEachLine
                }
				if (line == "sub:") {
					inSub = 2
					caseSensitive = false
					caseSensitiveAuto = false
					return@forEachLine
				}
				if (line == "cssub:") {
					inSub = 2
					caseSensitive = true
					return@forEachLine
				}
				if (line == "casub:") {
					inSub = 2
					caseSensitiveAuto = true
					return@forEachLine
				}
                val index = line.indexOf(':')
                if (index == -1)
                    error("Expected ':' in $line")

                val id = line.substring(0, index)
                val replacement = line.substring(index + 1)
                idSubstitutions[id.toLong()] = replacement.replace("\\n", "\r\n")
            }
            if (inSub > 0)
                error("Unmatched substitution")
        }
    }

    fun save(file: File) {
        file.bufferedWriter().use { writer ->
			textSubstitutions.forEach {
				writer.write("sub:\r\n\t${it.key.replace("\r\n", "\\n")}\r\n\t${it.value.replace("\r\n", "\\n")}\r\n")
			}
			textSubstitutionsCaseSensitive.forEach {
				writer.write("cssub:\r\n\t${it.key.replace("\r\n", "\\n")}\r\n\t${it.value.replace("\r\n", "\\n")}\r\n")
			}
            idSubstitutions.forEach {
                writer.write("${it.key}:${it.value.replace("\r\n", "\\n")}\r\n")
            }
        }
    }

    fun replace(localeFile: File) {
        val locale = Locale()
        locale.load(localeFile)
        locale.entries.forEach { entry ->
            val text = entry.text
            var newText = text
			textSubstitutions.forEach {
				newText = newText.replace(it.key, it.value, true)
			}
			textSubstitutionsCaseSensitive.forEach {
				newText = newText.replace(it.key, it.value)
			}
            if (newText != text) {
                println("Changed from $text to $newText")
                entry.text = newText
            }
        }
        idSubstitutions.forEach {
            val entry = locale.entriesById[it.key] ?: error("Couldn't find entry with ID ${it.key}")
            entry.text = it.value
        }
        locale.save(localeFile)
    }
}
