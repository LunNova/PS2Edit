//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package nallar.ps2edit;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import nallar.ps2edit.PackFile;
import nallar.ps2edit.PackFile.Entry;

public class Assets {
    final Map<String, Entry> nameToOrig = new HashMap('?');
    final Map<Integer, ArrayList<Runnable>> packToActionList = new HashMap(60);
    final ArrayList<PackFile> packFiles = new ArrayList(256);
    final Map<String, byte[]> nameToReplacement = new HashMap(60);
    final PackFile replacementPackFile;

    public Assets(File packFileDir, File replacementFilePathPath) throws IOException {
        int fakePackFileNumber = 0;

        File replacementPackFileFile;
        do {
            replacementPackFileFile = new File(packFileDir, String.format("Assets_%03d.pack", new Object[]{Integer.valueOf(fakePackFileNumber++)}));
        } while(replacementPackFileFile.exists());

        Object[] var10004 = new Object[1];
        --fakePackFileNumber;
        var10004[0] = Integer.valueOf(fakePackFileNumber);
        replacementPackFileFile = new File(packFileDir, String.format("Assets_%03d.pack", var10004));
        replacementPackFileFile.delete();
        Files.write(replacementFilePathPath.toPath(), replacementPackFileFile.toString().getBytes(Charsets.UTF_8), new OpenOption[0]);
        Uninterruptibles.sleepUninterruptibly(1L, TimeUnit.MILLISECONDS);
        this.replacementPackFile = new PackFile(replacementPackFileFile);

        for(int i = 0; i < fakePackFileNumber; ++i) {
            PackFile pack = new PackFile(new File(packFileDir, String.format("Assets_%03d.pack", new Object[]{Integer.valueOf(i)})));
            this.nameToOrig.putAll(pack.entryMap);
            this.packFiles.add(pack);
        }

    }

    public byte[] getByteData(String file) {
        byte[] rep = (byte[])this.nameToReplacement.get(file);
        return rep != null?rep:((Entry)this.nameToOrig.get(file)).getData();
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
        return this.addAction(file, new Runnable() {
            public void run() {
                String original = Assets.this.getStringData(file);
                if(original.contains(from)) {
                    Assets.this.setStringData(file, original.replace(from, to));
                } else {
                    System.err.println(file + " does not contain " + from + " to replace with " + to);
                }

            }
        });
    }

    public boolean addAction(String file, Runnable action) {
        Entry e = (Entry)this.nameToOrig.get(file);
        if(e == null) {
            System.err.println("File " + file + " not in assets to add action for.");
            return false;
        } else {
            int pack = e.getPackFile().asInt();
            ArrayList actions = (ArrayList)this.packToActionList.get(Integer.valueOf(pack));
            if(actions == null) {
                this.packToActionList.put(Integer.valueOf(pack), actions = new ArrayList());
            }

            actions.add(action);
            return true;
        }
    }

    public void save() {
        Iterator i$ = this.packToActionList.entrySet().iterator();

        java.util.Map.Entry entry;
        while(i$.hasNext()) {
            entry = (java.util.Map.Entry)i$.next();
            int name = ((Integer)entry.getKey()).intValue();
            ArrayList replacement = (ArrayList)entry.getValue();
            PackFile e = (PackFile)this.packFiles.get(name);
            e.openRead();
            Iterator i$1 = replacement.iterator();

            while(i$1.hasNext()) {
                Runnable runnable = (Runnable)i$1.next();
                runnable.run();
            }

            e.close();
        }

        this.replacementPackFile.open();
        i$ = this.nameToReplacement.entrySet().iterator();

        while(i$.hasNext()) {
            entry = (java.util.Map.Entry)i$.next();
            String name1 = (String)entry.getKey();
            byte[] replacement1 = (byte[])entry.getValue();
            Entry e1 = this.replacementPackFile.addFile(name1);
            e1.setData(replacement1);
        }

        this.replacementPackFile.closeForce();
    }
}
