//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package nallar.ps2edit;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.Uninterruptibles;
import nallar.ps2edit.PackFile.Entry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Assets {
	final Map<String, Entry> nameToOrig = new HashMap<>('?');
	final Map<Integer, ArrayList<Runnable>> packToActionList = new HashMap<>(60);
	final ArrayList<PackFile> packFiles = new ArrayList<>(256);
	final Map<String, byte[]> nameToReplacement = new HashMap<>(60);
	final PackFile replacementPackFile;

	public Assets(File packFileDir, File replacementFilePathPath) throws IOException {
		int fakePackFileNumber = 0;

		File replacementPackFileFile;
		do {
			replacementPackFileFile = new File(packFileDir, String.format("Assets_%03d.pack", fakePackFileNumber++));
		} while (replacementPackFileFile.exists());
		--fakePackFileNumber;
		replacementPackFileFile = new File(packFileDir, String.format("Assets_%03d.pack", fakePackFileNumber));
		replacementPackFileFile.delete();
		Files.write(replacementFilePathPath.toPath(), replacementPackFileFile.toString().getBytes(Charsets.UTF_8));
		Uninterruptibles.sleepUninterruptibly(1L, TimeUnit.MILLISECONDS);
		this.replacementPackFile = new PackFile(replacementPackFileFile);

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
			Map.Entry<Integer, ArrayList<Runnable>> entry;
			entry = integerArrayListEntry;
			int name = entry.getKey();
			ArrayList<Runnable> replacement = entry.getValue();
			PackFile e = this.packFiles.get(name);
			e.openRead();

			for (Object aReplacement : replacement) {
				Runnable runnable = (Runnable) aReplacement;
				runnable.run();
			}

			e.close();
		}

		this.replacementPackFile.open();

		for (Map.Entry<String, byte[]> entry : this.nameToReplacement.entrySet()) {
			String name1 = entry.getKey();
			byte[] replacement1 = entry.getValue();
			Entry e1 = this.replacementPackFile.addFile(name1);
			e1.setData(replacement1);
		}

		this.replacementPackFile.closeForce();
	}
}
