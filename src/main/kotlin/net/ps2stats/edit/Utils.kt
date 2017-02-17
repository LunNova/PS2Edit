package net.ps2stats.edit

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

object Utils {
    private val cachedProcesses = {
        val e = StringBuilder()
        val p = Runtime.getRuntime().exec("tasklist")
        p.inputStream.reader().readLines().forEach {
            e.append('\n').append(it.substring(0, it.indexOfAny(charArrayOf('\t', ' ')) + 1))
        }

        e.toString().toLowerCase()
    }()

    fun removeRecursive(path: Path) {
        Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                Files.delete(file)
                return FileVisitResult.CONTINUE
            }

            override fun visitFileFailed(file: Path, exc: IOException?): FileVisitResult {
                Files.delete(file)
                return FileVisitResult.CONTINUE
            }

            override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                if (exc == null) {
                    Files.delete(dir)
                    return FileVisitResult.CONTINUE
                } else {
                    throw exc
                }
            }
        })
    }

    fun isRunning(serviceName: String): Boolean {
        return cachedProcesses.contains('\n' + serviceName.toLowerCase())
    }

    @Throws(IOException::class)
    fun kill(serviceName: String): Boolean {
        Runtime.getRuntime().exec("taskkill /F /IM $serviceName /T")
        return isRunning(serviceName)
    }
}
