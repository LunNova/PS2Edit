package nallar.ps2edit;

import nallar.ps2edit.util.Throw;

import java.io.*;
import java.util.*;

public class Paths {
	private static final String CLIENT_CONFIG = "ClientConfigTestLaunchpad.ini";
	private final File propertiesFile = new File("./ps2.props");
	private final Properties properties;
	final File ps2Dir;
	final File assetsDir;
	final File replacementsDir;
	final File replacementFilePathPath;
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
		}
		System.out.println(properties.get("ps2dir"));

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
		if (!propertiesFile.exists()) {
			try {
				properties.store(new FileOutputStream(propertiesFile), "");
			} catch (IOException e) {
				throw Throw.sneaky(e);
			}
		}
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
