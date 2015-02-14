//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package nallar.ps2edit;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nallar.ps2edit.Assets;
import nallar.ps2edit.DMEFile;
import nallar.ps2edit.Utils;

public class Main {
    private static boolean START_GAME = true;
    private static final Pattern SPACE_PATTERN = Pattern.compile(" ");
    private static final Pattern COMMA_PATTERN = Pattern.compile(",");
    private static final Pattern RANGE_PATTERN = Pattern.compile("\\{([0-9]+)\\- ?([0-9]+)}");
    private static long lastTime = System.nanoTime();
    private static volatile boolean shouldPatch = true;

    public Main() {
    }

    private static long profile(String partDone) {
        long newTime = System.nanoTime();
        System.out.println(partDone + " took " + (float)(newTime - lastTime) / 1.0E9F + " seconds.");
        return lastTime = System.nanoTime();
    }

    static void sleep(double s) {
        try {
            Thread.sleep((long)(s * 1000.0D));
        } catch (InterruptedException var3) {
            ;
        }

    }

    public static void main(String[] args) {
        try {
            run();
        } catch (Throwable var5) {
            var5.printStackTrace(System.err);
            sleep(100.0D);
        } finally {
            sleep(START_GAME?30.0D:180.0D);
        }

    }

    private static void run() throws IOException {
        System.out.println("Press [ENTER] to prevent the game from being patched.");
        System.out.flush();
        Thread checkShouldPatch = new Thread() {
            public void run() {
                try {
                    BufferedReader ignored = new BufferedReader(new InputStreamReader(System.in));

                    while(!ignored.ready()) {
                        Main.sleep(0.2D);
                    }

                    ignored.readLine();
                    Main.shouldPatch = false;
                } catch (Throwable var2) {
                    ;
                }

            }
        };
        checkShouldPatch.start();
        boolean waitForSteamToBeSlowAndRealisePS2HasExited = Utils.kill("wws_crashreport_uploader.exe") || Utils.kill("Launchpad.exe") || Utils.kill("AwesomiumProcess.exe") || Utils.kill("Planetside2.exe");
        profile("Killing PS2 tasks");
        String[] ps2Dirs = new String[]{"C:\\Steam\\SteamApps\\common\\PlanetSide 2", "C:\\Program Files (x86)\\Steam\\SteamApps\\common\\PlanetSide 2"};
        final File triedFile = null;
        String[] ps2Dir = ps2Dirs;
        int assetsDir = ps2Dirs.length;

        for(int replacementAssetsDir = 0; replacementAssetsDir < assetsDir; ++replacementAssetsDir) {
            String replacementFilePathPath = ps2Dir[replacementAssetsDir];
            triedFile = new File(replacementFilePathPath);
            if(triedFile.exists() && triedFile.isDirectory()) {
                break;
            }

            triedFile = null;
        }

        if(triedFile == null) {
            throw new RuntimeException("Failed to detect PS2 directory, tried:\n" + Arrays.toString(ps2Dirs));
        } else {
            File var59 = new File(triedFile, "Resources" + File.separator + "Assets");
            File var60 = new File(triedFile, "backup");
            File var61 = new File(var60, "replacementFilePath");
            File downloadInfo = new File(triedFile, ".DownloadInfo.txt");
            File logsDirectory = new File(triedFile, "Logs");
            File cCLLP = new File(triedFile, "ClientConfigLiveLaunchpad.ini");
            downloadInfo.delete();
            if(logsDirectory.exists()) {
                Utils.removeRecursive(logsDirectory.toPath());
                System.out.println("Cleaned up PS2 logs directory");
            }

            if(waitForSteamToBeSlowAndRealisePS2HasExited) {
                sleep(3.0D);
                lastTime = System.nanoTime();
            }

            String original;
            if(var61.exists()) {
                original = new String(Files.readAllBytes(var61.toPath()), Charsets.UTF_8);
                if(!(new File(original)).delete() || !var61.delete()) {
                    System.err.println("Failed to delete replacement pack file.");
                    sleep(5.0D);
                    if(!var61.delete()) {
                        var61.deleteOnExit();
                        System.err.println("Still failed to delete replacement pack file. Giving up.");
                        return;
                    }
                }
            }

            profile("Deleting old replacement pack file");
            original = "[CrashReporter]\r\nAddress=ps2recap.station.sony.com:15081\r\n";
            String modified = "[CrashReporter]\r\nAddress=ation.tony.com:15081\r\nEnabled=0\r\n";
            if(!Utils.replaceWithoutModified(cCLLP, modified, original)) {
                System.err.println("cCLLP.ini not already modified, can\'t revert.");
            }

            profile("Reverting ClientConfigLiveLaunchpad changes");
            if(START_GAME) {
                Desktop.getDesktop().browse(URI.create("steam://run/218230"));
                profile("Starting game");
                sleep(3.5D);
            }

            for(boolean launcherReady = !START_GAME; !launcherReady; sleep(0.4D)) {
                try {
                    FileInputStream assets = new FileInputStream(downloadInfo);
                    Throwable fonts = null;

                    try {
                        BufferedReader repFile = new BufferedReader(new InputStreamReader(assets));

                        String br;
                        while((br = repFile.readLine()) != null) {
                            if(br.contains("All files are up to date")) {
                                System.out.println("Detected not previously patched and ready - " + br);
                                launcherReady = true;
                                break;
                            }

                            if(br.contains("Finished downloading ")) {
                                System.out.println("Detected previously patched and ready - " + br);
                                launcherReady = true;
                                break;
                            }
                        }
                    } catch (Throwable var56) {
                        fonts = var56;
                        throw var56;
                    } finally {
                        if(assets != null) {
                            if(fonts != null) {
                                try {
                                    assets.close();
                                } catch (Throwable var53) {
                                    fonts.addSuppressed(var53);
                                }
                            } else {
                                assets.close();
                            }
                        }

                    }
                } catch (FileNotFoundException var58) {
                    ;
                }
            }

            checkShouldPatch.interrupt();
            checkShouldPatch.stop(new Throwable("stop patch"));
            checkShouldPatch.stop();
            if(!shouldPatch) {
                System.out.println("Patching canceled - [ENTER] was pressed.");
            } else {
                lastTime = System.nanoTime();
                if(!Utils.replaceWithoutModified(cCLLP, original, modified)) {
                    throw new RuntimeException("Failed to update cCLP.ini, missing search string " + original);
                } else {
                    profile("Updated cCLLP");
                    final Assets var62 = new Assets(var59, var61);
                    profile("Loading " + var62.getNumFiles() + " from assets");
                    final int[] var63 = new int[1];
                    Files.walkFileTree((new File(var60, "fonts")).toPath(), new SimpleFileVisitor() {
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            super.visitFile(file, attrs);
                            File original = new File(triedFile, "UI/Resource/Fonts/" + file.getFileName());
                            if(original.delete()) {
                                Files.copy(file, original.toPath(), new CopyOption[0]);
                                ++var63[0];
                            }

                            return FileVisitResult.CONTINUE;
                        }
                    });
                    profile("Replacing " + var63[0] + " fonts");
                    File[] var64 = var60.listFiles();
                    int var66 = var64.length;

                    final String entry;
                    for(int i$ = 0; i$ < var66; ++i$) {
                        final File x2 = var64[i$];
                        entry = x2.getName();
                        if(entry.contains(".") && !x2.isDirectory() && !entry.equals("effects.yml")) {
                            final boolean from = entry.endsWith(".obj");
                            if(from) {
                                entry = entry.replace(".obj", ".dme");
                            }

                            if(!var62.addAction(entry, new Runnable() {
                                public void run() {
                                    try {
                                        byte[] e;
                                        if(from) {
                                            e = (byte[])var62.getByteData(entry).clone();
                                            DMEFile.replaceMesh(e, x2);
                                        } else {
                                            e = Files.readAllBytes(x2.toPath());
                                        }

                                        var62.setByteData(entry, e);
                                    } catch (IOException var2) {
                                        throw Throwables.propagate(var2);
                                    }
                                }
                            })) {
                                System.err.println("Failed to find file " + x2.getName() + " in assets pack to replace.");
                            }
                        }
                    }

                    profile("Adding assets_replace actions");
                    File var65 = new File(var60, "effects.yml");
                    BufferedReader var67 = new BufferedReader(new FileReader(var65));
                    Throwable var68 = null;

                    try {
                        String var69 = null;
                        entry = null;
                        String var70 = null;

                        String line;
                        while((line = var67.readLine()) != null) {
                            if(!line.trim().isEmpty() && (line.length() <= 0 || line.charAt(0) != 35)) {
                                line = line.replace("\t", "").replace("\\t", "\t");
                                if(line.length() > 0 && line.charAt(line.length() - 1) == 58) {
                                    var69 = line.substring(0, line.length() - 1);
                                    entry = var69.contains(".")?var69:null;
                                } else {
                                    String[] v;
                                    if(entry != null) {
                                        if(var70 == null) {
                                            var70 = line;
                                        } else {
                                            String[] entries;
                                            if(entry.contains(",")) {
                                                entries = COMMA_PATTERN.split(entry);
                                            } else {
                                                entries = new String[]{entry};
                                            }

                                            ArrayList entriesList = new ArrayList();
                                            v = entries;
                                            int e = entries.length;

                                            for(int i$1 = 0; i$1 < e; ++i$1) {
                                                String e1 = v[i$1];
                                                Matcher matcher = RANGE_PATTERN.matcher(e1);
                                                if(matcher.find()) {
                                                    int f = Integer.valueOf(matcher.group(1)).intValue();
                                                    int t = Integer.valueOf(matcher.group(2)).intValue();

                                                    for(int i = f; i <= t; ++i) {
                                                        entriesList.add(matcher.replaceFirst(String.valueOf(i)));
                                                    }
                                                } else {
                                                    entriesList.add(e1);
                                                }
                                            }

                                            Iterator var72 = entriesList.iterator();

                                            while(var72.hasNext()) {
                                                String var73 = (String)var72.next();
                                                line = line.replace("\\n", "\n");
                                                var62.addReplaceAction(var73, var70, line);
                                            }

                                            var70 = null;
                                        }
                                    } else {
                                        byte var71 = -1;
                                        switch(var69.hashCode()) {
                                        case -1249586564:
                                            if(var69.equals("variable")) {
                                                var71 = 1;
                                            }
                                            break;
                                        case -903579675:
                                            if(var69.equals("shader")) {
                                                var71 = 0;
                                            }
                                        }

                                        switch(var71) {
                                        case 0:
                                            var62.addReplaceAction("materials_3.xml", "Effect=\"" + line + '\"', "Effect=\"\"");
                                            break;
                                        case 1:
                                            v = SPACE_PATTERN.split(line);
                                            var62.addReplaceAction("materials_3.xml", "Variable=\"" + v[0] + "\" Default=\"" + v[1] + '\"', "Variable=\"" + v[0] + "\" Default=\"" + v[2] + '\"');
                                            break;
                                        default:
                                            System.err.println("Unkown action " + var69 + " is set.");
                                        }
                                    }
                                }
                            }
                        }

                        if(var70 != null) {
                            System.err.println("Dangling replace: from still set to " + var70);
                        }
                    } catch (Throwable var54) {
                        var68 = var54;
                        throw var54;
                    } finally {
                        if(var67 != null) {
                            if(var68 != null) {
                                try {
                                    var67.close();
                                } catch (Throwable var52) {
                                    var68.addSuppressed(var52);
                                }
                            } else {
                                var67.close();
                            }
                        }

                    }

                    profile("Adding effects.yml actions");
                    var62.save();
                    profile("Writing assets");
                    System.out.println("Updated " + var62.getNumFilesUpdated() + " entries.");
                }
            }
        }
    }
}
