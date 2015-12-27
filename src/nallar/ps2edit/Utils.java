//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package nallar.ps2edit;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.io.Files;
import nallar.ps2edit.util.Throw;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;

public class Utils {
	private static final String cachedProcesses = getProcesses();

	public static boolean replaceWithoutModified(File file, String f, String t) {
		if (f.length() != t.length()) {
			throw new RuntimeException("Mismatched lengths");
		} else {
			try {
				String content = Files.toString(file, Charsets.UTF_8);
				if (!content.contains(f)) {
					return false;
				} else {
					content = content.replace(f, t);
					long lastModified = file.lastModified();
					Files.write(content, file, Charsets.UTF_8);
					if (!file.setLastModified(lastModified)) {
						throw new RuntimeException("Failed to unmark file " + f + " as modified.");
					} else {
						return true;
					}
				}
			} catch (IOException e) {
				throw Throw.sneaky(e);
			}
		}
	}

	public static void removeRecursive(Path path) throws IOException {
		java.nio.file.Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				java.nio.file.Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				java.nio.file.Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (exc == null) {
					java.nio.file.Files.delete(dir);
					return FileVisitResult.CONTINUE;
				} else {
					throw exc;
				}
			}
		});
	}

	private static String getProcesses() {
		try {
			StringBuilder e = new StringBuilder();
			Process p = Runtime.getRuntime().exec("tasklist");
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

			String line;
			while ((line = reader.readLine()) != null) {
				e.append('\n').append(line.substring(0, (line.contains("\t") ? line.indexOf(9) : line.indexOf(32)) + 1));
			}

			return e.toString().toLowerCase();
		} catch (IOException var4) {
			throw Throwables.propagate(var4);
		}
	}

	public static boolean isRunning(String serviceName) {
		return cachedProcesses.contains('\n' + serviceName.toLowerCase());
	}

	public static boolean kill(String serviceName) throws IOException {
		Runtime.getRuntime().exec("taskkill /F /IM " + serviceName + " /T");
		return isRunning(serviceName);
	}
}
