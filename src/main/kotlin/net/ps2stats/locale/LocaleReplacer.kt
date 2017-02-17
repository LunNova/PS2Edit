package net.ps2stats.locale

import net.ps2stats.edit.Paths
import java.io.File

class LocaleReplacements {
    fun replace(path: Paths) {
        val localesDir = path.localeReplacementsDir
        val files = localesDir.listFiles(File::isFile).orEmpty()
        if (files.isEmpty())
            return

        val locale = Locale()
        locale.load(path.locale)

        // TODO
    }
}
