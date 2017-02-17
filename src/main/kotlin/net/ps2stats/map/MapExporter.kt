package net.ps2stats.map

import me.nallar.jdds.JDDS
import net.ps2stats.edit.Assets
import net.ps2stats.edit.Paths
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import java.util.regex.Pattern
import javax.imageio.ImageIO

class MapExporter(internal val assets: Assets, internal val path: Paths) {
    fun exportMap(name: String): BufferedImage {
        val assetsMap = assets.files

        val search = Pattern.compile('^' + name + "_tile_([-\\d]+)_([-\\d]+)_LOD0\\.dds$", Pattern.CASE_INSENSITIVE)
        val files = ArrayList<NameXZ>()

        var maxX: Int
        var maxZ: Int
        var minX: Int
        var minZ: Int
        maxZ = Integer.MIN_VALUE
        maxX = maxZ
        minZ = Integer.MAX_VALUE
        minX = minZ
        for (item in assetsMap.keys) {
            val m = search.matcher(item)
            if (m.find()) {
                val x = Integer.parseInt(m.group(1))
                val z = Integer.parseInt(m.group(2))
                if (x > maxX)
                    maxX = x
                if (z > maxZ)
                    maxZ = z
                if (x < minX)
                    minX = x
                if (z < minZ)
                    minZ = z

                files.add(NameXZ(item, x, z))
            }
        }


        val width = ((maxX - minX) / GRID_SPACING + 1) * IMAGE_SIZE
        val height = ((maxZ - minZ) / GRID_SPACING + 1) * IMAGE_SIZE
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()

        for (nameXZ in files) {
            val drawX = (nameXZ.x - minX) / GRID_SPACING * IMAGE_SIZE
            val drawZ = (nameXZ.z - minZ) / GRID_SPACING * IMAGE_SIZE

            val entry = assetsMap[nameXZ.name] ?: error("Failed to get ${nameXZ.name}")

            var decompressed: BufferedImage? = null
            synchronized(JDDS::class.java) {
                decompressed = JDDS.readDDS(entry.data)
            }
            graphics.drawImage(decompressed, drawX, drawZ, null)
        }

        return flipImageVertically(image)
    }

    fun saveMap(name: String, format: String, location: File) {
        val image = exportMap(name)
        ImageIO.write(image, format, location)
    }

    private data class NameXZ(val name: String, val x: Int, val z: Int)

    companion object {
        private val IMAGE_SIZE = 256
        private val GRID_SPACING = 4

        private fun flipImageVertically(image: BufferedImage): BufferedImage {
            val tx = AffineTransform.getScaleInstance(1.0, -1.0)
            tx.translate(0.0, (-image.getHeight(null)).toDouble())
            val op = AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR)
            return op.filter(image, null)
        }
    }
}
