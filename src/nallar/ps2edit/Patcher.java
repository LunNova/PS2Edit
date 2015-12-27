//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package nallar.ps2edit;

import com.google.common.base.Throwables;
import nallar.ps2edit.util.Throw;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.regex.*;

public class Patcher {
	private static final Pattern SPACE_PATTERN = Pattern.compile(" ");
	private static final Pattern COMMA_PATTERN = Pattern.compile(",");
	private static final Pattern RANGE_PATTERN = Pattern.compile("\\{([0-9]+)\\- ?([0-9]+)}");
	private static final String original = "[CrashReporter]\r\nAddress=ps2recap.station.sony.com:15081\r\n";
	private static final String modified = "[CrashReporter]\r\nAddress=ation.tony.com:15081\r\nEnabled=0\r\n";
	private static final boolean START_GAME = false;
	private static long lastTime = System.nanoTime();
	private final Paths path;
	private Thread checkShouldPatch;
	private volatile boolean shouldPatch = true;

	public Patcher(Paths path) {
		this.path = path;
	}

	public static void main(Paths path) {
		try {
			new Patcher(path).runGameWithPatches();
		} catch (Throwable var5) {
			var5.printStackTrace(System.err);
			sleep(100.0D);
		} finally {
			sleep(START_GAME ? 30.0D : 180.0D);
		}
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

	private void runGameWithPatches() throws IOException {
		checkShouldPatch();
		boolean ps2WasRunning =
				Utils.kill("wws_crashreport_uploader.exe") |
						Utils.kill("Launchpad.exe") |
						Utils.kill("AwesomiumProcess.exe") |
						Utils.kill("Planetside2.exe");
		profile("Killing PS2 tasks");

		if (path.downloadInfo.exists() && !path.downloadInfo.delete()) {
			throw new RuntimeException("Failed to delete old downloadInfo");
		}
		if (path.logsDirectory.exists()) {
			Utils.removeRecursive(path.logsDirectory.toPath());
			System.out.println("Cleaned up PS2 logs directory");
		}

		if (ps2WasRunning) {
			sleep(3.0D);
			lastTime = System.nanoTime();
		}

		Assets.deleteReplacement(path);
		profile("Deleting old replacement pack file");

		revertCCLP();
		profile("Reverting ClientConfigLiveLaunchpad changes");

		if (START_GAME) {
			// Desktop.getDesktop().browse(URI.create("steam://run/218230"));
			Runtime.getRuntime().exec(new String[]{path.launchpadExe.toString()});
			profile("Starting game");
			sleep(3.5D);
		}

		waitForLaunchpadReady();

		if (!checkShouldPatchEnd()) {
			System.out.println("Patching canceled - [ENTER] was pressed.");
			return;
		}

		lastTime = System.nanoTime();
		modifyCCLP();
		profile("Updated clientConfig");
		final Assets assets = new Assets(path, true);
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

	private void modifyCCLP() {
		if (!Utils.replaceWithoutModified(path.clientConfig, original, modified)) {
			throw new RuntimeException("Failed to update cCLP.ini, missing search string " + original);
		}
	}

	private void revertCCLP() {
		if (!Utils.replaceWithoutModified(path.clientConfig, modified, original)) {
			System.err.println("clientConfig.ini not already modified, can\'t revert.");
		}
	}

	private void loadReplacements(Assets assets) {
		File[] replacementFiles = path.replacementsDir.listFiles();
		if (replacementFiles == null) {
			throw Throw.sneaky(new IOException("Replacement file dir " + path.replacementsDir + " does not exist."));
		}

		for (final File replacement : replacementFiles) {
			String entry = replacement.getName();
			if (entry.contains(".") && !replacement.isDirectory() && !entry.equals("effects.yml")) {
				final boolean isModel = entry.endsWith(".obj");
				if (isModel) {
					entry = entry.replace(".obj", ".dme");
				}

				final String finalEntry = entry;
				if (!assets.addAction(entry, () -> {
					try {
						byte[] data;
						if (isModel) {
							data = assets.getByteData(finalEntry).clone();
							DMEFile.replaceMesh(data, replacement);
						} else {
							data = Files.readAllBytes(replacement.toPath());
						}

						assets.setByteData(finalEntry, data);
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
		File effectsFile = new File(path.replacementsDir, "effects.yml");

		if (!effectsFile.exists()) {
			System.out.println("Skipping effects");
			return;
		}

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
								System.err.println("Unknown action " + type + " is set.");
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
		File fontsDir = new File(path.replacementsDir, "fonts");
		if (!fontsDir.isDirectory()) {
			return 0;
		}
		final int[] foundFonts = new int[1];
		try {
			Files.walkFileTree(fontsDir.toPath(), new SimpleFileVisitor<Path>() {
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					super.visitFile(file, attrs);
					File original = new File(path.ps2Dir, "UI/Resource/Fonts/" + file.getFileName());
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
				FileInputStream assets = new FileInputStream(path.downloadInfo);

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
					BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

					while (!reader.ready()) {
						try {
							Thread.sleep(400);
						} catch (InterruptedException e) {
							return;
						}
					}

					reader.readLine();
					shouldPatch = false;
				} catch (Throwable ignored) {
				}
			}
		};
		checkShouldPatch.start();
	}
}
