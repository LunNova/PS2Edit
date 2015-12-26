package nallar.ps2edit;

import nallar.ps2edit.util.Throw;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Paths {
	private static final String CLIENT_CONFIG = "ClientConfigTestLaunchpad.ini";
	private final File propertiesFile = new File("./ps2.props");
	private final File propertiesFileBackup = new File("./ps2.props");
	private final Properties properties;
	final File ps2Dir;
	final File assetsDir;
	final File replacementsDir;
	final File downloadInfo;
	final File logsDirectory;
	final File clientConfig;
	final File launchpadExe;

	public Paths() {
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
			saveProperties();
		}
		System.out.println(properties.get("ps2dir"));

		ps2Dir = getPS2dir();
		assetsDir = new File(ps2Dir, "Resources" + File.separator + "Assets");
		replacementsDir = new File("./replacements");
		downloadInfo = new File(ps2Dir, ".DownloadInfo.txt");
		logsDirectory = new File(ps2Dir, "Logs");
		clientConfig = new File(ps2Dir, CLIENT_CONFIG);
		launchpadExe = new File(ps2Dir, "launchpad.exe");
		if (!clientConfig.exists()) {
			throw new RuntimeException("Client test launchpad config not found. PS2 Patcher only works on the test server!");
		}
	}

	public Path getReplacementPackFile() {
		String path = (String) properties.get("replacementPackFile");
		if (path == null) {
			return null;
		}

		Path p = java.nio.file.Paths.get(path);
		if (!Files.exists(p)) {
			return null;
		}

		return p;
	}

	public void setReplacementPackFile(Path p) {
		properties.put("replacementPackFile", p == null ? "" : p.toAbsolutePath().toString());
		saveProperties();
	}

	private void saveProperties() {
		try {
			if (propertiesFile.exists()) {
				Files.deleteIfExists(propertiesFileBackup.toPath());
				Files.copy(propertiesFile.toPath(), propertiesFileBackup.toPath());
			}
			try (OutputStream stream = new FileOutputStream(propertiesFile)) {
				properties.store(stream, "");
			}
			Files.deleteIfExists(propertiesFileBackup.toPath());
		} catch (IOException e) {
			if (propertiesFileBackup.exists() && !propertiesFile.exists() || propertiesFile.length() == 0) {
				try {
					Files.deleteIfExists(propertiesFile.toPath());
					Files.move(propertiesFileBackup.toPath(), propertiesFile.toPath());
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			throw Throw.sneaky(e);
		}
	}

	private String[] getPS2dirs() {
		ArrayList<String> dirs = new ArrayList<>();
		dirs.add(properties.getProperty("ps2dir"));
		dirs.addAll(Arrays.asList(
				"C:\\Users\\Public\\Daybreak Game Company\\Installed Games\\PlanetSide 2 Test",
				"C:\\PS2\\PTS",
				"C:\\Steam\\SteamApps\\common\\PlanetSide 2",
				"C:\\Program Files (x86)\\Steam\\SteamApps\\common\\PlanetSide 2",
				"C:\\Program Files\\Steam\\SteamApps\\common\\PlanetSide 2"
		));
		return dirs.toArray(new String[dirs.size()]);
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
}