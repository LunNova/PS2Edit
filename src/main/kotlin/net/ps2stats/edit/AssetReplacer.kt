package net.ps2stats.edit

import java.io.File
import java.nio.file.Files
import java.util.*
import java.util.regex.Pattern

object AssetReplacer {
	private val SPACE_PATTERN = Pattern.compile(" ")
	private val COMMA_PATTERN = Pattern.compile(",")
	private val RANGE_PATTERN = Pattern.compile("\\{([0-9]+)\\- ?([0-9]+)}")

	fun loadReplacements(path: Paths, assets: Assets) {
		val replacementFiles = path.replacements.replacements

		for (replacement in replacementFiles) {
			var entry = replacement.name
			val isModel = entry.endsWith(".obj")
			if (isModel) {
				entry = entry.replace(".obj", ".dme")
			}

			assets.addAction(entry) {
				val data: ByteArray
				if (isModel) {
					data = assets.getByteData(entry).clone()
					DMEFile.replaceMesh(data, replacement)
				} else {
					data = Files.readAllBytes(replacement.toPath())
				}

				assets.setByteData(entry, data)
			}
		}
	}

	fun loadEffects(path: Paths, assets: Assets) {
		val effectsFile = File(path.configDir, "asset_text.yml")

		if (!effectsFile.exists()) {
			return
		}

		effectsFile.bufferedReader().use { effectsReader ->
			var type: String? = null
			var entry: String? = null
			var previousLine: String? = null

			effectsReader.readLines().forEach {
				var line = it
				if (!line.trim { it <= ' ' }.isEmpty() && line[0] != '#') {
					line = line.replace("\t", "").replace("\\t", "\t")
					if (line.isNotEmpty() && line[line.length - 1] == ':') {
						type = line.substring(0, line.length - 1)
						entry = if (type!!.contains('.')) type else null
					} else if (entry != null) {
						if (previousLine == null) {
							previousLine = line
						} else {
							val entries: Array<String>
							if (entry!!.contains(",")) {
								entries = COMMA_PATTERN.split(entry)
							} else {
								entries = arrayOf(entry!!)
							}

							val entriesList = ArrayList<String>()

							for (e1 in entries) {
								val matcher = RANGE_PATTERN.matcher(e1)
								if (matcher.find()) {
									val f = Integer.valueOf(matcher.group(1))!!
									val t = Integer.valueOf(matcher.group(2))!!

									(f..t).mapTo(entriesList) { matcher.replaceFirst(it.toString()) }
								} else {
									entriesList.add(e1)
								}
							}

							for (anEntriesList in entriesList) {
								line = line.replace("\\n", "\n")
								assets.addReplaceAction(anEntriesList, previousLine!!, line)
							}

							previousLine = null
						}
					} else if (type != null) {
						when (type) {
							"shader" -> assets.addReplaceAction("materials_3.xml", "Effect=\"" + line + '\"', "Effect=\"\"")
							"variable" -> {
								val parts = SPACE_PATTERN.split(line)
								assets.addReplaceAction("materials_3.xml", "Variable=\"" + parts[0] + "\" Default=\"" + parts[1] + '\"', "Variable=\"" + parts[0] + "\" Default=\"" + parts[2] + '\"')
							}
							else -> error("Unknown action $type is set.")
						}
					} else {
						error("Unknown state. Line: '$line'")
					}
				}
			}

			if (previousLine != null) {
				error("Dangling replace: from still set to " + previousLine)
			}
		}
	}
}
