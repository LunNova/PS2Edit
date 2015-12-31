package nallar.ps2edit;

import lombok.val;
import nallar.ps2edit.util.Throw;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Paths {
	private static final String CLIENT_CONFIG = "ClientConfigTestLaunchpad.ini";
	private static final String LAUNCHPAD_EXECUTABLE = "LaunchPad.exe";
	private static final String PLANETSIDE2_EXECUTABLE = "PlanetSide2_x64.exe";
	private static final String PROPERTIES_FILE = "./ps2.props";
	public final File ps2Dir;
	public final File assetsDir;
	public final File replacementsDir;
	public final File downloadInfo;
	public final File logsDirectory;
	public final File clientConfig;
	public final File launchpadExe;
	public final Replacements replacements;
	private final Properties properties;
	private final File propertiesFile;

	public Paths() {
		properties = new Properties();
		replacementsDir = new File("./PS2 Patches");
		if (!replacementsDir.isDirectory())
			try {
				Files.createDirectory(replacementsDir.toPath());
			} catch (IOException e) {
				throw Throw.sneaky(e);
			}

		propertiesFile = new File(replacementsDir, PROPERTIES_FILE);
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

		ps2Dir = getPS2dir();
		assetsDir = new File(ps2Dir, "Resources" + File.separator + "Assets");
		downloadInfo = new File(ps2Dir, ".DownloadInfo.txt");
		logsDirectory = new File(ps2Dir, "Logs");
		clientConfig = new File(ps2Dir, CLIENT_CONFIG);
		launchpadExe = new File(ps2Dir, LAUNCHPAD_EXECUTABLE);
		replacements = new Replacements(replacementsDir.toPath());
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
		File propertiesFileBackup = new File(propertiesFile.getParent(), propertiesFile.getName() + ".bak");
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
		val config = properties.getProperty("ps2dir");
		if (config != null && !config.isEmpty())
			dirs.add(config);
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
		String[] dirs = getPS2dirs();
		System.out.println(Arrays.toString(dirs));
		File chosenDir = null;
		for (String possibleDir : dirs) {
			File triedPs2Dir = new File(possibleDir);
			if (validPS2Dir(triedPs2Dir)) {
				chosenDir = triedPs2Dir;
				break;
			}
		}
		if (chosenDir == null) {
			chosenDir = guiSelectPS2Dir();
			if (chosenDir == null)
				throw new RuntimeException("Failed to find PS2 dir in one of: " + Arrays.toString(dirs));
		}
		System.out.println("Selected PS2 dir: " + chosenDir + " available: " + Arrays.toString(dirs));
		return chosenDir;
	}

	private File guiSelectPS2Dir() {
		val currentSelection = new File(properties.getProperty("ps2dir"));
		val chooser = new JFileChooser();
		chooser.setCurrentDirectory(currentSelection.isDirectory() ? currentSelection : new File("/"));
		chooser.setDialogTitle("Select your PTS Planetside2_x64.exe");
		chooser.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase().equals(PLANETSIDE2_EXECUTABLE.toLowerCase());
			}

			@Override
			public String getDescription() {
				return PLANETSIDE2_EXECUTABLE;
			}
		});

		int result = chooser.showOpenDialog(null);
		if (result == JFileChooser.APPROVE_OPTION) {
			File selected = chooser.getSelectedFile().getParentFile().getAbsoluteFile();
			if (validPS2Dir(selected)) {
				properties.setProperty("ps2dir", selected.toString());
				saveProperties();
				return selected;
			}
		}
		return null;
	}

	private boolean validPS2Dir(File triedPs2Dir) {
		return triedPs2Dir.exists() && triedPs2Dir.isDirectory()
				&& new File(triedPs2Dir, PLANETSIDE2_EXECUTABLE).exists()
				&& new File(triedPs2Dir, CLIENT_CONFIG).exists();
	}
}
