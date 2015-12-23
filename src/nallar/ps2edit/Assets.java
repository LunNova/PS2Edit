//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package nallar.ps2edit;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.Uninterruptibles;
import nallar.ps2edit.PackFile.Entry;
import nallar.ps2edit.util.Throw;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Assets {
	final Map<String, Entry> nameToOrig = new HashMap<>(60);
	final Map<Integer, ArrayList<Runnable>> packToActionList = new HashMap<>(60);
	final ArrayList<PackFile> packFiles = new ArrayList<>(256);
	final Map<String, byte[]> nameToReplacement = new HashMap<>(60);
	final PackFile replacementPackFile;

	public static void deleteReplacement(File replacementFilePathPath) {
		// replacementFilePathPath is the path to a file, which contains a single line of text:
		// the path of the replacement pack file
		if (!replacementFilePathPath.exists()) {
			System.out.println("Replacement pack file does not exist, no need to delete it.");
			return;
		}
		File original;
		try {
			original = new File(new String(Files.readAllBytes(replacementFilePathPath.toPath()), Charsets.UTF_8));
		} catch (IOException e) {
			throw Throw.sneaky(e);
		}
		if (original.exists() && !original.delete()) {
			System.err.println("Failed to delete replacement pack file.");
			Uninterruptibles.sleepUninterruptibly(5, TimeUnit.SECONDS);
			if (original.exists() && !original.delete()) {
				replacementFilePathPath.deleteOnExit();
				original.deleteOnExit();
				throw Throw.sneaky(new IOException("Failed to delete replacement pack file"));
			}
		}
		if (!replacementFilePathPath.delete()) {
			System.err.println("Failed to delete replacement pack path file.");
			Uninterruptibles.sleepUninterruptibly(5, TimeUnit.SECONDS);
			if (replacementFilePathPath.exists() && !replacementFilePathPath.delete()) {
				replacementFilePathPath.deleteOnExit();
				throw Throw.sneaky(new IOException("Failed to delete replacement pack path file"));
			}
		}
	}

	public Assets(File packFileDir, File replacementFilePathPath) throws IOException {
		int fakePackFileNumber = 0;

		File replacementPackFile;
		do {
			replacementPackFile = new File(packFileDir, String.format("Assets_%03d.pack", fakePackFileNumber++));
		} while (replacementPackFile.exists());
		--fakePackFileNumber;
		replacementPackFile = new File(packFileDir, String.format("Assets_%03d.pack", fakePackFileNumber));
		if (replacementPackFile.exists()) {
			throw new RuntimeException("Replacement pack file should not already exist at this stage." +
					"Should have been deleted earlier or errored at failed deletion.");
		}
		Files.write(replacementFilePathPath.toPath(), replacementPackFile.toString().getBytes(Charsets.UTF_8));
		Uninterruptibles.sleepUninterruptibly(1L, TimeUnit.MILLISECONDS);

		this.replacementPackFile = new PackFile(replacementPackFile);

		for (int i = 0; i < fakePackFileNumber; ++i) {
			PackFile pack = new PackFile(new File(packFileDir, String.format("Assets_%03d.pack", i)));
			this.nameToOrig.putAll(pack.entryMap);
			this.packFiles.add(pack);
		}

	}

	public byte[] getByteData(String file) {
		byte[] rep = this.nameToReplacement.get(file);
		return rep != null ? rep : this.nameToOrig.get(file).getData();
	}

	public void setByteData(String file, byte[] data) {
		this.nameToReplacement.put(file, data);
	}

	public String getStringData(String file) {
		return new String(this.getByteData(file), Charsets.ISO_8859_1);
	}

	public void setStringData(String file, String data) {
		this.setByteData(file, data.getBytes(Charsets.ISO_8859_1));
	}

	public int getNumFiles() {
		return this.nameToOrig.size();
	}

	public int getNumFilesUpdated() {
		return this.nameToReplacement.size();
	}

	public boolean addReplaceAction(final String file, final String from, final String to) {
		return this.addAction(file, () -> {
			String original = Assets.this.getStringData(file);
			if (original.contains(from)) {
				Assets.this.setStringData(file, original.replace(from, to));
			} else {
				System.err.println(file + " does not contain " + from + " to replace with " + to);
			}

		});
	}

	public boolean addAction(String file, Runnable action) {
		Entry e = this.nameToOrig.get(file);
		if (e == null) {
			System.err.println("File " + file + " not in assets to add action for.");
			return false;
		} else {
			int pack = e.getPackFile().asInt();
			ArrayList<Runnable> actions = this.packToActionList.get(pack);
			if (actions == null) {
				this.packToActionList.put(pack, actions = new ArrayList<>());
			}

			actions.add(action);
			return true;
		}
	}

	public void save() {
		for (Map.Entry<Integer, ArrayList<Runnable>> integerArrayListEntry : this.packToActionList.entrySet()) {
			ArrayList<Runnable> replacements = integerArrayListEntry.getValue();
			PackFile e = this.packFiles.get(integerArrayListEntry.getKey());
			e.openRead();

			replacements.forEach(Runnable::run);

			e.close();
		}

		this.replacementPackFile.open();

		for (Map.Entry<String, byte[]> entry : this.nameToReplacement.entrySet()) {
			replacementPackFile.addFile(entry.getKey()).setData(entry.getValue());
		}

		this.replacementPackFile.closeForce();
	}
}
