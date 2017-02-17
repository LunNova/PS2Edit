package net.ps2stats.edit

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.util.*
import javax.swing.JFileChooser
import javax.swing.filechooser.FileFilter

class Paths {
    private val CLIENT_CONFIG_TEST = "ClientConfigTestLaunchpad.ini"
    private val CLIENT_CONFIG_LIVE = "ClientConfigLiveLaunchpad.ini"
    private val LAUNCHPAD_EXECUTABLE = "LaunchPad.exe"
    private val PLANETSIDE2_EXECUTABLE = "PlanetSide2_x64.exe"
    private val PROPERTIES_FILE = "./ps2.props"
    val configDir = ensureDir(File("PS2 Edit").normalize())
    val replacementsDir = ensureDir(File(configDir, "Asset Replacements"))
    val replacements = Replacements(replacementsDir)
    val propertiesFile = File(configDir, PROPERTIES_FILE)
    private val properties = Properties()

    init {
        if (propertiesFile.exists()) {
            println("Loading props from " + propertiesFile)
            properties.load(FileInputStream(propertiesFile))
        } else {
            properties.put("ps2dir", "")
            saveProperties()
        }
    }

    val localeReplacementsDir = ensureDir(File(configDir, "Locales"))
    val fontsDir = ensureDir(File(configDir, "Fonts"))
    val ps2Dir = {
        val dirs = pS2dirs
        val chosenDir: File = dirs
                .map(::File)
                .firstOrNull { validPS2Dir(it) }
                ?: guiSelectPS2Dir()
                ?: throw RuntimeException("Failed to find PS2 dir in one of: " + Arrays.toString(dirs))
        println("Selected PS2 dir: " + chosenDir)
        chosenDir
    }()
    val downloadInfo = File(ps2Dir, ".DownloadInfo.txt")
    val logsDirectory = File(ps2Dir, "Logs")
    val clientConfig = anyOf(ps2Dir, CLIENT_CONFIG_LIVE, CLIENT_CONFIG_TEST) ?: error("Couldn't find ClientConfigLive/TestLaunchpad.ini")
    val launchpadExe = File(ps2Dir, LAUNCHPAD_EXECUTABLE)
    val locale = File(ps2Dir, "Locale${File.separator}en_us_data.dir")
    val assetsDir = File(ps2Dir, "Resources" + File.separator + "Assets")
    val isLive = clientConfig.name == CLIENT_CONFIG_LIVE || clientConfig.name != CLIENT_CONFIG_TEST

    init {
        println("Live client: $isLive")
        if (isLive)
            println("Asset replacement disabled")
    }

    fun anyOf(parent: File, vararg child: String) =
            child.map { File(parent, it) }.filter(File::exists).firstOrNull()

    fun ensureDir(file: File) =
            if (!file.isDirectory && !file.mkdir()) error("Couldn't create " + file.canonicalFile) else file

    var replacementPackFile: File?
        get() {
            val path = properties["replacementPackFile"] as String? ?: return null
            val f = File(path)
            return if (f.exists()) f.canonicalFile else null
        }
        set(p) {
            properties.put("replacementPackFile", p?.absoluteFile?.toString() ?: "")
            saveProperties()
        }

    private fun saveProperties() {
        val propertiesFileBackup = File(propertiesFile.parent, propertiesFile.name + ".bak")
        try {
            if (propertiesFile.exists()) {
                Files.deleteIfExists(propertiesFileBackup.toPath())
                Files.copy(propertiesFile.toPath(), propertiesFileBackup.toPath())
            }
            FileOutputStream(propertiesFile).use { stream -> properties.store(stream, "") }
            Files.deleteIfExists(propertiesFileBackup.toPath())
        } catch (e: IOException) {
            if (propertiesFileBackup.exists() && !propertiesFile.exists() || propertiesFile.length() == 0L) {
                try {
                    Files.deleteIfExists(propertiesFile.toPath())
                    Files.move(propertiesFileBackup.toPath(), propertiesFile.toPath())
                } catch (e1: IOException) {
                    e1.printStackTrace()
                }
            }
            throw e
        }

    }

    private val pS2dirs: Array<String>
        get() {
            val dirs = ArrayList<String>()
            val config = properties.getProperty("ps2dir")
            if (config != null && !config.isEmpty())
                dirs.add(config)
            dirs.addAll(Arrays.asList(
                    "C:\\Users\\Public\\Daybreak Game Company\\Installed Games\\PlanetSide 2 Test",
                    "C:\\PS2\\PTS",
                    "C:\\Steam\\SteamApps\\common\\PlanetSide 2",
                    "C:\\Program Files (x86)\\Steam\\SteamApps\\common\\PlanetSide 2",
                    "C:\\Program Files\\Steam\\SteamApps\\common\\PlanetSide 2"
            ))
            return dirs.toTypedArray()
        }

    private fun guiSelectPS2Dir(): File? {
        val currentSelection = File(properties.getProperty("ps2dir"))
        val chooser = JFileChooser()
        chooser.currentDirectory = if (currentSelection.isDirectory) currentSelection else File("/")
        chooser.dialogTitle = "Select your PTS Planetside2_x64.exe"
        chooser.fileFilter = object : FileFilter() {
            override fun accept(f: File): Boolean {
                return f.isDirectory || f.name.toLowerCase() == PLANETSIDE2_EXECUTABLE.toLowerCase()
            }

            override fun getDescription(): String {
                return PLANETSIDE2_EXECUTABLE
            }
        }

        val result = chooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            val selected = chooser.selectedFile.parentFile.absoluteFile
            if (validPS2Dir(selected)) {
                properties.setProperty("ps2dir", selected.toString())
                saveProperties()
                return selected
            }
        }
        return null
    }

    fun validPS2Dir(triedPs2Dir: File): Boolean {
        return triedPs2Dir.isDirectory
                && File(triedPs2Dir, PLANETSIDE2_EXECUTABLE).isFile
                && anyOf(triedPs2Dir, CLIENT_CONFIG_LIVE, CLIENT_CONFIG_TEST) != null
    }
}
