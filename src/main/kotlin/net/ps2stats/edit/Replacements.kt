package net.ps2stats.edit

import java.io.File
import java.util.*

class Replacements(private val dir: File) {
    val replacements: List<File>
        get() {
            return dir.listFiles()!!.toList()
        }

    val replacementNames: List<String>
        get() {
            return dir.listFiles()!!.mapTo(ArrayList<String>()) { it.name }
        }
}
