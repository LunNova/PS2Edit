package nallar.ps2edit;

import lombok.Data;
import lombok.val;
import me.nallar.jdds.JDDS;
import nallar.ps2edit.util.Throw;

import javax.imageio.ImageIO;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

public class MapExporter {
	private static final int IMAGE_SIZE = 256;
	private static final int GRID_SPACING = 4;
	final Assets assets;
	final Paths path;

	public MapExporter(Assets assets, Paths path) {
		this.assets = assets;
		this.path = path;
	}

	private static BufferedImage flipImageVertically(BufferedImage image) {
		AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
		tx.translate(0, -image.getHeight(null));
		AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
		return op.filter(image, null);
	}

	public BufferedImage exportMap(String name) {
		val assetsMap = assets.getFiles();

		Pattern search = Pattern.compile('^' + name + "_tile_([-\\d]+)_([-\\d]+)_LOD0\\.dds$", Pattern.CASE_INSENSITIVE);
		List<NameXZ> files = new ArrayList<>();

		int maxX, maxZ, minX, minZ;
		maxX = maxZ = Integer.MIN_VALUE;
		minX = minZ = Integer.MAX_VALUE;
		for (String item : assetsMap.keySet()) {
			Matcher m = search.matcher(item);
			if (m.find()) {
				val x = Integer.parseInt(m.group(1));
				val z = Integer.parseInt(m.group(2));
				if (x > maxX)
					maxX = x;
				if (z > maxZ)
					maxZ = z;
				if (x < minX)
					minX = x;
				if (z < minZ)
					minZ = z;

				files.add(new NameXZ(item, x, z));
			}
		}


		val width = (((maxX - minX) / GRID_SPACING) + 1) * IMAGE_SIZE;
		val height = (((maxZ - minZ) / GRID_SPACING) + 1) * IMAGE_SIZE;
		val image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		val graphics = image.createGraphics();

		for (NameXZ nameXZ : files) {
			val drawX = ((nameXZ.x - minX) / GRID_SPACING) * IMAGE_SIZE;
			val drawZ = ((nameXZ.z - minZ) / GRID_SPACING) * IMAGE_SIZE;

			val entry = assetsMap.get(nameXZ.getName());

			BufferedImage decompressed;
			synchronized (JDDS.class) {
				decompressed = JDDS.readDDS(entry.getData());
			}
			graphics.drawImage(decompressed, drawX, drawZ, null);
		}

		return flipImageVertically(image);
	}

	public void saveMap(String name, String format, File location) {
		val image = exportMap(name);
		try {
			ImageIO.write(image, format, location);
		} catch (IOException e) {
			throw Throw.sneaky(e);
		}
	}

	@Data
	private static class NameXZ {
		final String name;
		final int x;
		final int z;
	}
}
