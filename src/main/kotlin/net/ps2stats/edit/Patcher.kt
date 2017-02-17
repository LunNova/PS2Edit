package net.ps2stats.edit

import java.io.*
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

class Patcher(private val path: Paths) {
    private var checkShouldPatch: Thread? = null
    @Volatile private var shouldPatch = true

    fun runGameWithPatches() {
        checkShouldPatch()
        val ps2WasRunning = Utils.kill("wws_crashreport_uploader.exe") or
                Utils.kill("Launchpad.exe") or
                Utils.kill("AwesomiumProcess.exe") or
                Utils.kill("Planetside2_x64.exe")
        profile("Killing PS2 tasks")

        if (path.downloadInfo.exists() && !path.downloadInfo.delete()) {
            throw RuntimeException("Failed to delete old downloadInfo")
        }
        if (path.logsDirectory.exists()) {
            Utils.removeRecursive(path.logsDirectory.toPath())
            println("Cleaned up PS2 logs directory")
        }
        profile("Cleaning up logs")

        if (ps2WasRunning) {
            sleep(3.0)
            lastTime = System.nanoTime()
        }

        if (!path.isLive) {
            ClientConfigUpdater.revertChanges(path.clientConfig)
            profile("Test Client - Reverting client config")
            Assets.deleteReplacement(path)
            profile("Test Client - Removed replacement files")
        }

        if (START_GAME) {
            println("Launching: " + path.launchpadExe)
            Runtime.getRuntime().exec(arrayOf("C:\\windows\\explorer.exe", path.launchpadExe.canonicalPath), null, path.launchpadExe.parentFile)
            profile("Starting game")
            sleep(3.5)
        }

        waitForLaunchpadReady()

        if (!checkShouldPatchEnd()) {
            println("Patching canceled - [ENTER] was pressed.")
            return
        }

        lastTime = System.nanoTime()
        profile("Replacing " + replaceFonts() + " fonts")

        val assets = Assets(path, !path.isLive)
        profile("Loading " + assets.numFiles + " from assets")

        if (path.isLive) {
            ClientConfigUpdater.applyChanges(path.clientConfig)
            profile("Test client - Updating client config")
            AssetReplacer.loadReplacements(path, assets)
            profile("Test client - Loading asset replacement actions")
            AssetReplacer.loadEffects(path, assets)
            profile("Test client - Loading asset text replacement actions")
            assets.save()
            profile("Test client - Updated ${assets.numFilesUpdated} assets")
        } else {
            println("Live client - Not updating assets")
        }
    }

    private fun replaceFonts(): Int {
        val fontsDir = path.fontsDir
        if (!fontsDir.isDirectory) {
            return 0
        }
        var foundFonts = 0
        Files.walkFileTree(fontsDir.toPath(), object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                super.visitFile(file, attrs)
                val original = File(path.ps2Dir, "UI/Resource/Fonts/" + file.fileName)
                if (original.delete()) {
                    Files.copy(file, original.toPath())
                    ++foundFonts
                } else {
                    print("Font " + file.fileName + " was not found in PS2 fonts dir to replace")
                }

                return FileVisitResult.CONTINUE
            }
        })

        return foundFonts
    }

    private fun waitForLaunchpadReady() {
        if (!START_GAME) {
            println("Not launching PS2 - just patching.")
            return
        }
        var launcherReady = false
        while (!launcherReady) {
            try {
                path.downloadInfo.readLines(Charsets.UTF_8).forEach {
                    if (it.contains("All files are up to date")) {
                        println("Detected not previously patched and ready - " + it)
                        launcherReady = true
                        return@forEach
                    }

                    if (it.contains("Finished downloading ")) {
                        println("Detected previously patched or updated and ready - " + it)
                        launcherReady = true
                        return@forEach
                    }
                }
            } catch (ignored: IOException) {
                // Failed to open file, launcher not yet created it
            } catch (ignored: FileNotFoundException) {
                // Launcher hasn't even started logging yet.
            }

            sleep(0.4)
        }
    }

    @Suppress("DEPRECATION")
    private fun checkShouldPatchEnd(): Boolean {
        checkShouldPatch!!.interrupt()
        checkShouldPatch!!.stop()
        return shouldPatch
    }

    private fun checkShouldPatch() {
        println("Press [ENTER] to prevent the game from being patched.")
        System.out.flush()
        checkShouldPatch = Thread {
            try {
                val reader = BufferedReader(InputStreamReader(System.`in`))

                while (!reader.ready()) {
                    try {
                        Thread.sleep(400)
                    } catch (e: InterruptedException) {
                        return@Thread
                    }

                }

                reader.readLine()
                shouldPatch = false
            } catch (ignored: Throwable) {
            }
        }
        checkShouldPatch!!.start()
    }

    companion object {
        private val START_GAME = true
        private var lastTime = System.nanoTime()

        private fun profile(partDone: String): Long {
            val newTime = System.nanoTime()
            println(partDone + " took " + (newTime - lastTime).toFloat() / 1.0E9f + " seconds.")
            lastTime = System.nanoTime()
            return lastTime
        }

        internal fun sleep(s: Double) {
            try {
                Thread.sleep((s * 1000.0).toLong())
            } catch (ignored: InterruptedException) {
            }

        }
    }
}
