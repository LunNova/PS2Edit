package net.ps2stats.edit

import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors
import java.util.function.Consumer

class Assets(path: Paths, writable: Boolean) {
    internal val nameToOrig: MutableMap<String, PackFile.Entry> = HashMap(60)
    internal val packToActionList: MutableMap<Int, MutableList<() -> Unit>> = HashMap(60)
    internal val packFiles = mutableListOf<PackFile>()
    internal val nameToReplacement: MutableMap<String, ByteArray> = HashMap(60)
    internal val replacementPackFile: PackFile?

    init {
        val packFileDir = path.assetsDir

        var fakePackFileNumber = 0

        val storedReplacement = path.replacementPackFile

        var replacementPackFile: File
        do {
            replacementPackFile = File(packFileDir, String.format("Assets_%03d.pack", fakePackFileNumber++))
        } while (replacementPackFile.exists() && replacementPackFile != storedReplacement)
        --fakePackFileNumber

        if (fakePackFileNumber > EXPECTED_REPLACEMENT_PACK_FILE_ID)
            throw RuntimeException("Replacement pack file ID $fakePackFileNumber higher than expected $EXPECTED_REPLACEMENT_PACK_FILE_ID")
        replacementPackFile = File(packFileDir, String.format("Assets_%03d.pack", fakePackFileNumber))

        if (writable) {
            path.replacementPackFile = replacementPackFile
            Thread.sleep(1)

            this.replacementPackFile = PackFile(replacementPackFile)
        } else {
            this.replacementPackFile = null
        }


        for (i in 0..fakePackFileNumber - 1) {
            val pack = PackFile(File(packFileDir, String.format("Assets_%03d.pack", i)))
            this.nameToOrig.putAll(pack.entryMap)
            this.packFiles.add(pack)
        }

        sanityCheck()

    }

    private fun sanityCheck() {
        val files = HashMap<String, PackFile.Entry>()
        for ((i, packFile) in packFiles.withIndex()) {
            for ((key, value) in packFile.entryMap) {
                if (files.put(key, value) != null) {
                    if (i == EXPECTED_REPLACEMENT_PACK_FILE_ID) {
                        // Assuming this is the replacement pack file
                        packFile.file.delete()
                        packFile.file.deleteOnExit()
                        if (replacementPackFile != null) {
                            replacementPackFile.file.delete()
                            replacementPackFile.file.deleteOnExit()
                        }
                        throw RuntimeException("Replacement pack file not deleted properly, please try again.")
                    }
                    throw RuntimeException("Duplicate entry in pack file $i: $key")
                }
            }
        }
    }

    fun getByteData(file: String): ByteArray {
        val rep = this.nameToReplacement[file]
        return rep ?: this.nameToOrig[file]?.data ?: error("Couldn't find $file")
    }

    val files: Map<String, PackFile.Entry>
        get() {
            val files = HashMap<String, PackFile.Entry>()
            for (packFile in packFiles) {
                files.putAll(packFile.entryMap)
            }
            return files
        }

    fun setByteData(file: String, data: ByteArray) {
        this.nameToReplacement.put(file, data)
    }

    fun getStringData(file: String): String {
        return String(this.getByteData(file), Charsets.ISO_8859_1)
    }

    fun setStringData(file: String, data: String) {
        this.setByteData(file, data.toByteArray(Charsets.ISO_8859_1))
    }

    val numFiles: Int
        get() = this.nameToOrig.size

    val numFilesUpdated: Int
        get() = this.nameToReplacement.size

    fun addReplaceAction(file: String, from: String, to: String) {
        addAction(file, {
            val original = this@Assets.getStringData(file)
            if (original.contains(from)) {
                this@Assets.setStringData(file, original.replace(from, to))
            } else {
                System.err.println("$file does not contain $from to replace with $to")
            }
        })
    }

    fun addAction(file: String, action: () -> Unit) {
        val e = this.nameToOrig[file] ?: error("File $file not in assets to add action for.")
        packToActionList.getOrPut(e.packFile.asInt()) { mutableListOf<() -> Unit>() }.add(action)
    }

    /**
     * Calls `callable` on each entry passed in. Works on them in the order in the PackFiles.
     * Entries in a given pack file are worked on sequentially, and pre-opened.

     * @param entries
     * *
     * @param callable
     */
    fun forEntries(entries: List<PackFile.Entry>, callable: Consumer<PackFile.Entry>) {
        val perPackEntries = mutableMapOf<PackFile, MutableList<PackFile.Entry>>()

        entries.forEach { perPackEntries.getOrPut(it.packFile) { mutableListOf() }.add(it) }

        val pool = Executors.newFixedThreadPool(1)
        for ((k, v) in perPackEntries) {
            pool.execute {
                k.openRead()
                try {
                    v.forEach { callable.accept(it) }
                } finally {
                    k.close()
                }
            }
        }
    }

    fun save() {
        if (replacementPackFile == null)
            throw Error("Can't save Assets if created with writable=false")

        for ((key, replacements) in this.packToActionList) {
            val e = this.packFiles[key]
            e.openRead()

            try {
                replacements.forEach { it() }
            } finally {
                e.close()
            }
        }

        this.replacementPackFile.open()

        try {
            for ((key, value) in this.nameToReplacement) {
                replacementPackFile.addFile(key).setData(value)
            }
        } finally {
            this.replacementPackFile.closeForce()
        }
    }

    companion object {
        private val EXPECTED_REPLACEMENT_PACK_FILE_ID = 256

        fun deleteReplacement(path: Paths) {
            val original = path.replacementPackFile
            if (original == null) {
                println("Replacement pack file does not exist, no need to delete it.")
                return
            }
            if (original.exists() && !original.delete()) {
                System.err.println("Failed to delete replacement pack file.")
                Thread.sleep(5000)
                if (original.exists() && !original.delete()) {
                    original.deleteOnExit()
                    throw IOException("Failed to delete replacement pack file")
                }
            }
            path.replacementPackFile = null
        }
    }
}
