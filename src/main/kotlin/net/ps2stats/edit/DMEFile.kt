package net.ps2stats.edit

import com.google.common.base.Splitter
import com.google.common.collect.Iterables
import com.google.common.primitives.Ints
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DMEFile {
    private class Vector {
        var x: Float = 0.toFloat()
        var y: Float = 0.toFloat()
        var z: Float = 0.toFloat()

        internal fun write(): String {
            return "v " + this.x + ' ' + this.y + ' ' + this.z + '\n'
        }

        internal fun read(line: String) {
            val parts = Iterables.toArray(splitter.split(line), String::class.java)
            if (parts[0] != "v") {
                throw RuntimeException("Can\'t read $line into vector, expected start \'v\'")
            } else {
                this.x = java.lang.Float.valueOf(parts[1])
                this.y = java.lang.Float.valueOf(parts[2])
                this.z = java.lang.Float.valueOf(parts[3])
            }
        }

        internal fun read(d: ByteArray, offset: Int, size: Int, count: Int) {
            var b = offset + size * count + 0
            this.x = java.lang.Float.intBitsToFloat(Ints.fromBytes(d[b + 3], d[b + 2], d[b + 1], d[b + 0]))
            b = offset + size * count + 4
            this.y = java.lang.Float.intBitsToFloat(Ints.fromBytes(d[b + 3], d[b + 2], d[b + 1], d[b + 0]))
            b = offset + size * count + 8
            this.z = java.lang.Float.intBitsToFloat(Ints.fromBytes(d[b + 3], d[b + 2], d[b + 1], d[b + 0]))
        }

        internal fun write(d: ByteArray, offset: Int, size: Int, count: Int) {
            var b = offset + size * count + 0
            var p = Ints.toByteArray(java.lang.Float.floatToIntBits(this.x))
            d[b + 3] = p[0]
            d[b + 2] = p[1]
            d[b + 1] = p[2]
            d[b + 0] = p[3]
            b = offset + size * count + 4
            p = Ints.toByteArray(java.lang.Float.floatToIntBits(this.y))
            d[b + 3] = p[0]
            d[b + 2] = p[1]
            d[b + 1] = p[2]
            d[b + 0] = p[3]
            b = offset + size * count + 8
            p = Ints.toByteArray(java.lang.Float.floatToIntBits(this.z))
            d[b + 3] = p[0]
            d[b + 2] = p[1]
            d[b + 1] = p[2]
            d[b + 0] = p[3]
        }

        internal fun read(bb: ByteBuffer) {
            this.x = bb.float
            this.y = bb.float
            this.z = bb.float
        }

        override fun toString(): String {
            return this.x.toString() + "\t" + this.y + '\t' + this.z
        }

        companion object {
            private val splitter = Splitter.on(' ')
        }
    }

    companion object {
        private val t = DMEFile.Vector()

        fun replaceMesh(data: ByteArray, objFile: File) {
            debug("Reading obj file " + objFile.name)
            val b = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            val magic = b.get().toChar().toString() + b.get().toChar() + b.get().toChar() + b.get().toChar()
            if (magic != "DMOD") {
                throw RuntimeException("Wrong magic value: $magic, expected DMOD")
            } else {
                val version = b.int
                val dmat_length = b.int
                b.position(b.position() + dmat_length)
                debug("dmot version is " + version)
                debug("dmat_length is " + dmat_length)
                t.read(b)
                t.read(b)
                val mesh_count = b.int
                debug("mesh_count is " + mesh_count)

                for (i in 0..mesh_count - 1) {
                    b.position(b.position() + 16)
                    val vertex_stream_count: Int
                    val index_size: Int
                    val index_count: Int
                    val vertex_count: Int
                    if (version == 3) {
                        vertex_stream_count = 1
                        b.position(b.position() + 1)
                        vertex_count = b.int
                        index_size = b.int
                        index_count = b.int
                    } else {
                        if (version != 4) {
                            throw RuntimeException("Bad version for mesh: " + version)
                        }

                        vertex_stream_count = b.int
                        index_size = b.int
                        index_count = b.int
                        vertex_count = b.int
                    }

                    debug("vertex stream count: " + vertex_stream_count)
                    debug("vertex count: " + vertex_count)
                    debug("index size: " + index_size)
                    debug("index count: " + index_count)
                    var warnEmpty = true
                    var j = 0

                    while (j < vertex_stream_count) {
                        val bytes_per_vertex = b.int
                        debug("Stream $j has $bytes_per_vertex bytes/vertex.")
                        if (j == 0) {
                            val br = BufferedReader(InputStreamReader(FileInputStream(objFile), Charsets.ISO_8859_1))

                            try {
                                for (x2 in 0..vertex_count - 1) {
                                    t.read(data, b.position(), bytes_per_vertex, x2)

                                    var line: String?
                                    line = br.readLine()
                                    while (line != null && (line.isEmpty() || line[0].toInt() != 118)) {
                                        line = br.readLine()
                                    }

                                    if (line == null) {
                                        t.x = 0.0f
                                        t.y = 0.0f
                                        t.z = 0.0f
                                        if (warnEmpty) {
                                            System.err.println("Ran out of vertexes writing " + objFile.name)
                                            warnEmpty = false
                                        }
                                    } else {
                                        t.read(line)
                                    }

                                    t.write(data, b.position(), bytes_per_vertex, x2)
                                }

                                val var28 = br.readLine().replace(" ", "\t")
                                if (var28.startsWith("v\t")) {
                                    System.err.println("dangling line is " + var28)
                                }
                            } catch (var26: Throwable) {
                                throw var26
                            } finally {
                                br.close()
                            }
                        }
                        b.position(b.position() + bytes_per_vertex * vertex_count)
                        ++j
                    }

                    b.position(b.position() + index_count * index_size)
                }

            }
        }

        private fun debug(s: String) {
            if (false)
                println(s)
        }

        fun saveMesh(data: ByteArray): ByteArray {
            val b = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            val magic = b.get().toChar().toString() + b.get().toChar() + b.get().toChar() + b.get().toChar()
            if (magic != "DMOD") {
                throw RuntimeException("Wrong magic value: $magic, expected DMOD")
            } else {
                val version = b.int
                val dmat_length = b.int
                b.position(b.position() + dmat_length)
                debug("dmot version is " + version)
                debug("dmat_length is " + dmat_length)
                t.read(b)
                t.read(b)
                val mesh_count = b.int
                debug("mesh_count is " + mesh_count)
                val os = ByteArrayOutputStream()

                try {
                    for (x2 in 0..mesh_count - 1) {
                        b.position(b.position() + 16)
                        val vertex_stream_count: Int
                        val index_size: Int
                        val index_count: Int
                        val vertex_count: Int
                        if (version == 3) {
                            vertex_stream_count = 1
                            b.position(b.position() + 1)
                            vertex_count = b.int
                            index_size = b.int
                            index_count = b.int
                        } else {
                            if (version != 4) {
                                throw RuntimeException("Bad version for mesh: " + version)
                            }

                            vertex_stream_count = b.int
                            index_size = b.int
                            index_count = b.int
                            vertex_count = b.int
                        }

                        debug("vertex stream count: " + vertex_stream_count)

                        for (j in 0..vertex_stream_count - 1) {
                            val bytes_per_vertex = b.int
                            debug("Stream $j has $bytes_per_vertex bytes/vertex.")

                            for (k in 0..vertex_count - 1) {
                                t.read(data, b.position(), bytes_per_vertex, k)
                                os.write(t.write().toByteArray(Charsets.ISO_8859_1))
                            }

                            b.position(b.position() + bytes_per_vertex * vertex_count)
                        }
                    }
                } catch (var24: Throwable) {
                    throw var24
                } finally {
                    os.close()
                }
                return os.toByteArray()
            }
        }
    }
}
