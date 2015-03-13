//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package nallar.ps2edit;

import com.google.common.base.Throwables;
import nallar.ps2edit.util.Throw;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
	private static boolean START_GAME = true;
	private static final File propertiesFile = new File("./ps2.props");
	private static final Properties properties;
	private static final Pattern SPACE_PATTERN = Pattern.compile(" ");
	private static final Pattern COMMA_PATTERN = Pattern.compile(",");
	private static final Pattern RANGE_PATTERN = Pattern.compile("\\{([0-9]+)\\- ?([0-9]+)}");
	private static final String CLIENT_CONFIG = "ClientConfigTestLaunchpad.ini";
	private static long lastTime = System.nanoTime();

	final File ps2Dir;
	final File assetsDir;
	final File replacementsDir;
	final File replacementFilePathPath;
	final File downloadInfo;
	final File logsDirectory;
	final File clientConfig;
	final File launchpadExe;
	private Thread checkShouldPatch;
	private volatile boolean shouldPatch = true;

	static {
		properties = new Properties();
		if (propertiesFile.exists()) {
			System.out.println("Loading props");
			try {
				properties.load(new FileInputStream(propertiesFile));
			} catch (IOException e) {
				throw Throw.sneaky(e);
			}
		} else {
			properties.put("ps2dir", "");
		}
		System.out.println(properties.get("ps2dir"));
	}

	private static String[] getPS2dirs() {
		ArrayList<String> dirs = new ArrayList<>();
		dirs.add(properties.getProperty("ps2dir"));
		dirs.addAll(Arrays.asList(
				"C:\\Steam\\SteamApps\\common\\PlanetSide 2",
				"C:\\Program Files (x86)\\Steam\\SteamApps\\common\\PlanetSide 2",
				"C:\\Program Files\\Steam\\SteamApps\\common\\PlanetSide 2"
		));
		if (!propertiesFile.exists()) {
			try {
				properties.store(new FileOutputStream(propertiesFile), "");
			} catch (IOException e) {
				throw Throw.sneaky(e);
			}
		}
		return dirs.toArray(new String[dirs.size()]);
	}

	public Main() {
		ps2Dir = getPS2dir();
		assetsDir = new File(ps2Dir, "Resources" + File.separator + "Assets");
		replacementsDir = new File("./replacements");
		replacementFilePathPath = new File(replacementsDir, "replacementFilePath");
		downloadInfo = new File(ps2Dir, ".DownloadInfo.txt");
		logsDirectory = new File(ps2Dir, "Logs");
		clientConfig = new File(ps2Dir, CLIENT_CONFIG);
		launchpadExe = new File(ps2Dir, "launchpad.exe");
		if (!clientConfig.exists()) {
			throw new RuntimeException("Client test launchpad config not found. PS2 Patcher only works on the test server!");
		}
	}

	public static void main(String[] args) {
		try {
			new Main().runGameWithPatches();
		} catch (Throwable var5) {
			var5.printStackTrace(System.err);
			sleep(100.0D);
		} finally {
			sleep(START_GAME ? 30.0D : 180.0D);
		}
	}

	private void runGameWithPatches() throws IOException {
		checkShouldPatch();
		boolean ps2WasRunning =
				Utils.kill("wws_crashreport_uploader.exe") |
						Utils.kill("Launchpad.exe") |
						Utils.kill("AwesomiumProcess.exe") |
						Utils.kill("Planetside2.exe");
		profile("Killing PS2 tasks");

		if (downloadInfo.exists() && !downloadInfo.delete()) {
			throw new RuntimeException("Failed to delete old downloadInfo");
		}
		if (logsDirectory.exists()) {
			Utils.removeRecursive(logsDirectory.toPath());
			System.out.println("Cleaned up PS2 logs directory");
		}

		if (ps2WasRunning) {
			sleep(3.0D);
			lastTime = System.nanoTime();
		}

		Assets.deleteReplacement(replacementFilePathPath);
		profile("Deleting old replacement pack file");

		modifyCCLLP();
		profile("Reverting ClientConfigLiveLaunchpad changes");

		if (START_GAME) {
			// Desktop.getDesktop().browse(URI.create("steam://run/218230"));
			Runtime.getRuntime().exec(new String[]{launchpadExe.toString()});
			profile("Starting game");
			sleep(3.5D);
		}

		waitForLaunchpadReady();

		if (!checkShouldPatchEnd()) {
			System.out.println("Patching canceled - [ENTER] was pressed.");
			return;
		}

		lastTime = System.nanoTime();
		revertCCLLP();
		profile("Updated clientConfig");
		final Assets assets = new Assets(assetsDir, replacementFilePathPath);
		profile("Loading " + assets.getNumFiles() + " from assets");
		profile("Replacing " + replaceFonts() + " fonts");
		loadReplacements(assets);
		profile("Adding assets_replace actions");

		loadEffects(assets);
		profile("Adding effects.yml actions");

		assets.save();
		profile("Writing assets");

		System.out.println("Updated " + assets.getNumFilesUpdated() + " entries.");

	}

	private static long profile(String partDone) {
		long newTime = System.nanoTime();
		System.out.println(partDone + " took " + (float) (newTime - lastTime) / 1.0E9F + " seconds.");
		return lastTime = System.nanoTime();
	}

	static void sleep(double s) {
		try {
			Thread.sleep((long) (s * 1000.0D));
		} catch (InterruptedException ignored) {
		}
	}

	private File getPS2dir() {
		File triedPs2Dir = null;
		String[] dirs = getPS2dirs();
		for (String possibleDir : dirs) {
			if (possibleDir == null) {
				continue;
			}
			triedPs2Dir = new File(possibleDir);
			if (triedPs2Dir.exists() && triedPs2Dir.isDirectory() && new File(triedPs2Dir, "PlanetSide2.exe").exists()) {
				break;
			}

			triedPs2Dir = null;
		}
		if (triedPs2Dir == null) {
			throw new RuntimeException("Failed to find PS2 dir in one of: " + Arrays.toString(dirs));
		}
		System.out.println("Selected PS2 dir: " + triedPs2Dir + " available: " + Arrays.toString(dirs));
		return triedPs2Dir;
	}

	private static final String original = "[CrashReporter]\r\nAddress=ps2recap.station.sony.com:15081\r\n";
	private static final String modified = "[CrashReporter]\r\nAddress=ation.tony.com:15081\r\nEnabled=0\r\n";

	private void revertCCLLP() {
		if (!Utils.replaceWithoutModified(clientConfig, original, modified)) {
			throw new RuntimeException("Failed to update cCLP.ini, missing search string " + original);
		}
	}

	private void modifyCCLLP() {
		if (!Utils.replaceWithoutModified(clientConfig, modified, original)) {
			System.err.println("clientConfig.ini not already modified, can\'t revert.");
		}
	}

	private void loadReplacements(Assets assets) {
		File[] replacementFiles = replacementsDir.listFiles();
		if (replacementFiles == null) {
			throw Throw.sneaky(new IOException("Replacement file dir " + replacementsDir + " does not exist."));
		}

		for (final File replacement : replacementFiles) {
			String entry = replacement.getName();
			if (entry.contains(".") && !replacement.isDirectory() && !entry.equals("effects.yml")) {
				final boolean from = entry.endsWith(".obj");
				if (from) {
					entry = entry.replace(".obj", ".dme");
				}

				final String finalEntry = entry;
				if (!assets.addAction(entry, () -> {
					try {
						byte[] e;
						if (from) {
							e = assets.getByteData(finalEntry).clone();
							DMEFile.replaceMesh(e, replacement);
						} else {
							e = Files.readAllBytes(replacement.toPath());
						}

						assets.setByteData(finalEntry, e);
					} catch (IOException var2) {
						throw Throwables.propagate(var2);
					}
				})) {
					System.err.println("Failed to find file " + replacement.getName() + " in assets pack to replace.");
				}
			}
		}
	}

	private void loadEffects(Assets assets) throws IOException {
		File effectsFile = new File(replacementsDir, "effects.yml");

		try (BufferedReader effectsReader = new BufferedReader(new FileReader(effectsFile))) {
			String type = null;
			String entry = null;
			String previousLine = null;

			String line;
			while ((line = effectsReader.readLine()) != null) {
				if (!line.trim().isEmpty() && line.charAt(0) != '#') {
					line = line.replace("\t", "").replace("\\t", "\t");
					if (line.length() > 0 && line.charAt(line.length() - 1) == ':') {
						type = line.substring(0, line.length() - 1);
						entry = type.contains(".") ? type : null;
					} else if (entry != null) {
						if (previousLine == null) {
							previousLine = line;
						} else {
							String[] entries;
							if (entry.contains(",")) {
								entries = COMMA_PATTERN.split(entry);
							} else {
								entries = new String[]{entry};
							}

							ArrayList<String> entriesList = new ArrayList<>();

							for (String e1 : entries) {
								Matcher matcher = RANGE_PATTERN.matcher(e1);
								if (matcher.find()) {
									int f = Integer.valueOf(matcher.group(1));
									int t = Integer.valueOf(matcher.group(2));

									for (int i = f; i <= t; ++i) {
										entriesList.add(matcher.replaceFirst(String.valueOf(i)));
									}
								} else {
									entriesList.add(e1);
								}
							}

							for (String anEntriesList : entriesList) {
								line = line.replace("\\n", "\n");
								assets.addReplaceAction(anEntriesList, previousLine, line);
							}

							previousLine = null;
						}
					} else if (type != null) {
						switch (type) {
							case "shader":
								assets.addReplaceAction("materials_3.xml", "Effect=\"" + line + '\"', "Effect=\"\"");
								break;
							case "variable":
								String[] parts = SPACE_PATTERN.split(line);
								assets.addReplaceAction("materials_3.xml", "Variable=\"" + parts[0] + "\" Default=\"" + parts[1] + '\"', "Variable=\"" + parts[0] + "\" Default=\"" + parts[2] + '\"');
								break;
							default:
								System.err.println("Unkown action " + type + " is set.");
						}
					} else {
						throw new RuntimeException("Unknown state. Line: '" + line + "'");
					}
				}
			}

			if (previousLine != null) {
				System.err.println("Dangling replace: from still set to " + previousLine);
			}
		}
	}

	private int replaceFonts() {
		File fontsDir = new File(replacementsDir, "fonts");
		if (!fontsDir.isDirectory()) {
			return 0;
		}
		final int[] foundFonts = new int[1];
		try {
			Files.walkFileTree(fontsDir.toPath(), new SimpleFileVisitor<Path>() {
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					super.visitFile(file, attrs);
					File original = new File(ps2Dir, "UI/Resource/Fonts/" + file.getFileName());
					if (original.delete()) {
						Files.copy(file, original.toPath());
						++foundFonts[0];
					}

					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			throw Throw.sneaky(e);
		}
		return foundFonts[0];
	}

	private void waitForLaunchpadReady() {
		if (!START_GAME) {
			System.out.println("Not launching PS2 - just patching.");
			return;
		}
		for (boolean launcherReady = false; !launcherReady; sleep(0.4D)) {
			try {
				FileInputStream assets = new FileInputStream(downloadInfo);

				try (BufferedReader repFile = new BufferedReader(new InputStreamReader(assets))) {
					String br;
					while ((br = repFile.readLine()) != null) {
						if (br.contains("All files are up to date")) {
							System.out.println("Detected not previously patched and ready - " + br);
							launcherReady = true;
							break;
						}

						if (br.contains("Finished downloading ")) {
							System.out.println("Detected previously patched and ready - " + br);
							launcherReady = true;
							break;
						}
					}
				} catch (IOException e) {
					throw Throw.sneaky(e);
				}
			} catch (FileNotFoundException ignored) {
				// Launcher hasn't even started logging yet.
			}
		}
	}

	@SuppressWarnings("deprecation")
	private boolean checkShouldPatchEnd() {
		checkShouldPatch.interrupt();
		checkShouldPatch.stop();
		return shouldPatch;
	}

	private void checkShouldPatch() {
		System.out.println("Press [ENTER] to prevent the game from being patched.");
		System.out.flush();
		checkShouldPatch = new Thread() {
			public void run() {
				try {
					BufferedReader ignored = new BufferedReader(new InputStreamReader(System.in));

					while (!ignored.ready()) {
						try {
							Thread.sleep(400);
						} catch (InterruptedException e) {
							return;
						}
					}

					ignored.readLine();
					shouldPatch = false;
				} catch (Throwable ignored) {
				}
			}
		};
		checkShouldPatch.start();
	}
}
