//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package nallar.ps2edit;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import java.awt.Desktop;
import java.io.*;
import java.net.URI;
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
                } catch (Throwable ignored) {
				}
            }
        };
        checkShouldPatch.start();
        boolean waitForSteamToBeSlowAndRealisePS2HasExited = Utils.kill("wws_crashreport_uploader.exe") || Utils.kill("Launchpad.exe") || Utils.kill("AwesomiumProcess.exe") || Utils.kill("Planetside2.exe");
        profile("Killing PS2 tasks");
        String[] ps2Dirs = new String[]{"C:\\Steam\\SteamApps\\common\\PlanetSide 2", "C:\\Program Files (x86)\\Steam\\SteamApps\\common\\PlanetSide 2"};
        File triedFile = null;

		for (String replacementFilePathPath : ps2Dirs) {
			triedFile = new File(replacementFilePathPath);
			if (triedFile.exists() && triedFile.isDirectory()) {
				break;
			}

			triedFile = null;
		}

        if(triedFile == null) {
            throw new RuntimeException("Failed to detect PS2 directory, tried:\n" + Arrays.toString(ps2Dirs));
        } else {
            File assetsDir = new File(triedFile, "Resources" + File.separator + "Assets");
            File replacementsDir = new File(triedFile, "backup");
            File replacementFilePathPath = new File(replacementsDir, "replacementFilePath");
            File downloadInfo = new File(triedFile, ".DownloadInfo.txt");
            File logsDirectory = new File(triedFile, "Logs");
            File cCLLP = new File(triedFile, "ClientConfigLiveLaunchpad.ini");
            if (downloadInfo.exists() && !downloadInfo.delete()) {
				throw new RuntimeException("Failed to delete old downloadInfo");
			}
            if(logsDirectory.exists()) {
                Utils.removeRecursive(logsDirectory.toPath());
                System.out.println("Cleaned up PS2 logs directory");
            }

            if(waitForSteamToBeSlowAndRealisePS2HasExited) {
                sleep(3.0D);
                lastTime = System.nanoTime();
            }

            String original;
            if(replacementFilePathPath.exists()) {
                original = new String(Files.readAllBytes(replacementFilePathPath.toPath()), Charsets.UTF_8);
                if(!(new File(original)).delete() || !replacementFilePathPath.delete()) {
                    System.err.println("Failed to delete replacement pack file.");
                    sleep(5.0D);
                    if(!replacementFilePathPath.delete()) {
                        replacementFilePathPath.deleteOnExit();
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
                    final Assets var62 = new Assets(assetsDir, replacementFilePathPath);
                    profile("Loading " + var62.getNumFiles() + " from assets");
                    final int[] foundFonts = new int[1];
					final File finalTriedFile = triedFile;
					Files.walkFileTree((new File(replacementsDir, "fonts")).toPath(), new SimpleFileVisitor<Path>() {
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            super.visitFile(file, attrs);
                            File original = new File(finalTriedFile, "UI/Resource/Fonts/" + file.getFileName());
                            if(original.delete()) {
                                Files.copy(file, original.toPath());
                                ++foundFonts[0];
                            }

                            return FileVisitResult.CONTINUE;
                        }
                    });
                    profile("Replacing " + foundFonts[0] + " fonts");
                    File[] replacementFiles = replacementsDir.listFiles();

                    String entry;
					for (final File replacement : replacementFiles) {
						entry = replacement.getName();
						if (entry.contains(".") && !replacement.isDirectory() && !entry.equals("effects.yml")) {
							final boolean from = entry.endsWith(".obj");
							if (from) {
								entry = entry.replace(".obj", ".dme");
							}

							final String finalEntry = entry;
							if (!var62.addAction(entry, () -> {
								try {
									byte[] e;
									if (from) {
										e = var62.getByteData(finalEntry).clone();
										DMEFile.replaceMesh(e, replacement);
									} else {
										e = Files.readAllBytes(replacement.toPath());
									}

									var62.setByteData(finalEntry, e);
								} catch (IOException var2) {
									throw Throwables.propagate(var2);
								}
							})) {
								System.err.println("Failed to find file " + replacement.getName() + " in assets pack to replace.");
							}
						}
					}

                    profile("Adding assets_replace actions");
                    File effectsFile = new File(replacementsDir, "effects.yml");
                    BufferedReader effectsReader = new BufferedReader(new FileReader(effectsFile));
                    Throwable var68 = null;

                    try {
                        String type = null;
                        entry = null;
                        String previousLine = null;

                        String line;
                        while((line = effectsReader.readLine()) != null) {
                            if(!line.trim().isEmpty() && (line.length() == 0 || line.charAt(0) != 35)) {
                                line = line.replace("\t", "").replace("\\t", "\t");
                                if(line.length() > 0 && line.charAt(line.length() - 1) == ':') {
                                    type = line.substring(0, line.length() - 1);
                                    entry = type.contains(".")?type:null;
                                } else {
                                    String[] v;
                                    if(entry != null) {
                                        if(previousLine == null) {
                                            previousLine = line;
                                        } else {
                                            String[] entries;
                                            if(entry.contains(",")) {
                                                entries = COMMA_PATTERN.split(entry);
                                            } else {
                                                entries = new String[]{entry};
                                            }

                                            ArrayList<String> entriesList = new ArrayList<>();
                                            v = entries;
                                            int e = entries.length;

                                            for(int i$1 = 0; i$1 < e; ++i$1) {
                                                String e1 = v[i$1];
                                                Matcher matcher = RANGE_PATTERN.matcher(e1);
                                                if(matcher.find()) {
                                                    int f = Integer.valueOf(matcher.group(1));
                                                    int t = Integer.valueOf(matcher.group(2));

                                                    for(int i = f; i <= t; ++i) {
                                                        entriesList.add(matcher.replaceFirst(String.valueOf(i)));
                                                    }
                                                } else {
                                                    entriesList.add(e1);
                                                }
                                            }

											for (String anEntriesList : entriesList) {
												line = line.replace("\\n", "\n");
												var62.addReplaceAction(anEntriesList, previousLine, line);
											}

                                            previousLine = null;
                                        }
                                    } else {
										switch(type) {
											case "shader":
												var62.addReplaceAction("materials_3.xml", "Effect=\"" + line + '\"', "Effect=\"\"");
												break;
											case "variable":
												v = SPACE_PATTERN.split(line);
												var62.addReplaceAction("materials_3.xml", "Variable=\"" + v[0] + "\" Default=\"" + v[1] + '\"', "Variable=\"" + v[0] + "\" Default=\"" + v[2] + '\"');
												break;
											default:
												System.err.println("Unkown action " + type + " is set.");
										}
                                    }
                                }
                            }
                        }

                        if(previousLine != null) {
                            System.err.println("Dangling replace: from still set to " + previousLine);
                        }
                    } catch (Throwable var54) {
                        var68 = var54;
                        throw var54;
                    } finally {
                        if(effectsReader != null) {
                            if(var68 != null) {
                                try {
                                    effectsReader.close();
                                } catch (Throwable var52) {
                                    var68.addSuppressed(var52);
                                }
                            } else {
                                effectsReader.close();
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
