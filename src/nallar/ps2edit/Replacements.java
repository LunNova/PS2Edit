package nallar.ps2edit;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.val;

import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

@Data
public class Replacements {
	private final Path path;

	public List<Path> getReplacements() {
		return getReplacementsInternal().collect(Collectors.toList());
	}

	public List<String> getReplacementNames() {
		return getReplacementsInternal().map((x) -> x.getFileName().toString()).collect(Collectors.toList());
	}

	@SneakyThrows
	private Stream<Path> getReplacementsInternal() {
		return Files.list(path).filter(this::validReplacement);
	}

	public boolean validReplacement(Path path) {
		if (Files.isDirectory(path))
			return false;

		val name = path.getFileName().toString();

		switch (name) {
			case "effects.yml":
			case "ps2.props":
				return false;
		}

		return true;
	}
}
