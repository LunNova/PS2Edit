//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package nallar.ps2edit;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.Uninterruptibles;
import lombok.val;
import nallar.ps2edit.PackFile.Entry;
import nallar.ps2edit.util.Throw;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class Assets {
	private static final int EXPECTED_REPLACEMENT_PACK_FILE_ID = 256;
	final Map<String, Entry> nameToOrig = new HashMap<>(60);
	final Map<Integer, ArrayList<Runnable>> packToActionList = new HashMap<>(60);
	final ArrayList<PackFile> packFiles = new ArrayList<>(256);
	final Map<String, byte[]> nameToReplacement = new HashMap<>(60);
	final PackFile replacementPackFile;

	public Assets(Paths path, boolean writable) {
		File packFileDir = path.assetsDir;

		int fakePackFileNumber = 0;

		Path storedReplacementPath = path.getReplacementPackFile();
		File storedReplacement = storedReplacementPath == null ? null : storedReplacementPath.toAbsolutePath().toFile();

		File replacementPackFile;
		do {
			replacementPackFile = new File(packFileDir, String.format("Assets_%03d.pack", fakePackFileNumber++));
		} while (replacementPackFile.exists() && !replacementPackFile.equals(storedReplacement));
		--fakePackFileNumber;

		if (fakePackFileNumber > EXPECTED_REPLACEMENT_PACK_FILE_ID)
			throw new RuntimeException("Replacement pack file ID " + fakePackFileNumber + " higher than expected " + EXPECTED_REPLACEMENT_PACK_FILE_ID);
		replacementPackFile = new File(packFileDir, String.format("Assets_%03d.pack", fakePackFileNumber));

		if (writable) {
			path.setReplacementPackFile(replacementPackFile.toPath().toAbsolutePath());
			Uninterruptibles.sleepUninterruptibly(1L, TimeUnit.MILLISECONDS);

			this.replacementPackFile = new PackFile(replacementPackFile);
		} else {
			this.replacementPackFile = null;
		}


		for (int i = 0; i < fakePackFileNumber; ++i) {
			PackFile pack = new PackFile(new File(packFileDir, String.format("Assets_%03d.pack", i)));
			this.nameToOrig.putAll(pack.entryMap);
			this.packFiles.add(pack);
		}

		sanityCheck();

	}

	public static void deleteReplacement(Paths path) {
		Path replacementFilePath = path.getReplacementPackFile();
		if (replacementFilePath == null) {
			System.out.println("Replacement pack file does not exist, no need to delete it.");
			return;
		}
		File original = replacementFilePath.toFile();
		if (original.exists() && !original.delete()) {
			System.err.println("Failed to delete replacement pack file.");
			Uninterruptibles.sleepUninterruptibly(5, TimeUnit.SECONDS);
			if (original.exists() && !original.delete()) {
				original.deleteOnExit();
				throw Throw.sneaky(new IOException("Failed to delete replacement pack file"));
			}
		}
		path.setReplacementPackFile(null);
	}

	private void sanityCheck() {
		Map<String, PackFile.Entry> files = new HashMap<>();
		int i = 0;
		for (PackFile packFile : packFiles) {
			for (val entry : packFile.entryMap.entrySet()) {
				if (files.put(entry.getKey(), entry.getValue()) != null) {
					if (i == EXPECTED_REPLACEMENT_PACK_FILE_ID) {
						// Assuming this is the replacement pack file
						packFile.file.delete();
						packFile.file.deleteOnExit();
						if (replacementPackFile != null) {
							replacementPackFile.file.delete();
							replacementPackFile.file.deleteOnExit();
						}
						throw new RuntimeException("Replacement pack file not deleted properly, please try again.");
					}
					throw new RuntimeException("Duplicate entry in pack file " + i + ": " + entry.getKey());
				}
			}
			i++;
		}
	}

	public byte[] getByteData(String file) {
		byte[] rep = this.nameToReplacement.get(file);
		return rep != null ? rep : this.nameToOrig.get(file).getData();
	}

	public Map<String, PackFile.Entry> getFiles() {
		Map<String, PackFile.Entry> files = new HashMap<>();
		for (PackFile packFile : packFiles) {
			files.putAll(packFile.entryMap);
		}
		return files;
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
		if (replacementPackFile == null)
			throw new Error("Can't save Assets if created with writable=false");

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
