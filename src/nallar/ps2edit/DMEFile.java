//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package nallar.ps2edit;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Ints;

import java.io.*;
import java.nio.*;

public class DMEFile {
	private static final DMEFile.vector t = new DMEFile.vector();

	public DMEFile() {
	}

	public static void replaceMesh(byte[] data, File objFile) throws IOException {
		debug("Reading obj file " + objFile.getName());
		ByteBuffer b = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
		String magic = String.valueOf((char) b.get()) + (char) b.get() + (char) b.get() + (char) b.get();
		if (!magic.equals("DMOD")) {
			throw new RuntimeException("Wrong magic value: " + magic + ", expected DMOD");
		} else {
			int version = b.getInt();
			int dmat_length = b.getInt();
			b.position(b.position() + dmat_length);
			debug("dmot version is " + version);
			debug("dmat_length is " + dmat_length);
			t.read(b);
			t.read(b);
			int mesh_count = b.getInt();
			debug("mesh_count is " + mesh_count);

			for (int i = 0; i < mesh_count; ++i) {
				b.position(b.position() + 16);
				int vertex_stream_count;
				int index_size;
				int index_count;
				int vertex_count;
				if (version == 3) {
					vertex_stream_count = 1;
					b.position(b.position() + 1);
					vertex_count = b.getInt();
					index_size = b.getInt();
					index_count = b.getInt();
				} else {
					if (version != 4) {
						throw new RuntimeException("Bad version for mesh: " + version);
					}

					vertex_stream_count = b.getInt();
					index_size = b.getInt();
					index_count = b.getInt();
					vertex_count = b.getInt();
				}

				debug("vertex stream count: " + vertex_stream_count);
				debug("vertex count: " + vertex_count);
				debug("index size: " + index_size);
				debug("index count: " + index_count);
				boolean warnEmpty = true;
				int j = 0;

				while (j < vertex_stream_count) {
					int bytes_per_vertex = b.getInt();
					debug("Stream " + j + " has " + bytes_per_vertex + " bytes/vertex.");
					if (j == 0) {
						BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(objFile), Charsets.ISO_8859_1));
						Throwable var16 = null;

						try {
							for (int x2 = 0; x2 < vertex_count; ++x2) {
								t.read(data, b.position(), bytes_per_vertex, x2);

								String line;
								for (line = br.readLine(); line != null && (line.length() <= 0 || line.charAt(0) != 118); line = br.readLine()) {
								}

								if (line == null) {
									t.x = 0.0F;
									t.y = 0.0F;
									t.z = 0.0F;
									if (warnEmpty) {
										System.err.println("Ran out of vertexes writing " + objFile.getName());
										warnEmpty = false;
									}
								} else {
									t.read(line);
								}

								t.write(data, b.position(), bytes_per_vertex, x2);
							}

							String var28 = br.readLine().replace(" ", "\t");
							if (var28.startsWith("v\t")) {
								System.err.println("dangling line is " + var28);
							}
						} catch (Throwable var26) {
							var16 = var26;
							throw var26;
						} finally {
							if (br != null) {
								if (var16 != null) {
									try {
										br.close();
									} catch (Throwable var25) {
										var16.addSuppressed(var25);
									}
								} else {
									br.close();
								}
							}

						}
					}
					b.position(b.position() + bytes_per_vertex * vertex_count);
					++j;
				}

				b.position(b.position() + index_count * index_size);
			}

		}
	}

	private static void debug(String s) {
		if (false)
			System.out.println(s);
	}

	public static void saveMesh(byte[] data, File objFile) throws IOException {
		ByteBuffer b = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
		String magic = String.valueOf((char) b.get()) + (char) b.get() + (char) b.get() + (char) b.get();
		if (!magic.equals("DMOD")) {
			throw new RuntimeException("Wrong magic value: " + magic + ", expected DMOD");
		} else {
			int version = b.getInt();
			int dmat_length = b.getInt();
			b.position(b.position() + dmat_length);
			debug("dmot version is " + version);
			debug("dmat_length is " + dmat_length);
			t.read(b);
			t.read(b);
			int mesh_count = b.getInt();
			debug("mesh_count is " + mesh_count);
			BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(objFile));
			Throwable var8 = null;

			try {
				for (int x2 = 0; x2 < mesh_count; ++x2) {
					b.position(b.position() + 16);
					int vertex_stream_count;
					int index_size;
					int index_count;
					int vertex_count;
					if (version == 3) {
						vertex_stream_count = 1;
						b.position(b.position() + 1);
						vertex_count = b.getInt();
						index_size = b.getInt();
						index_count = b.getInt();
					} else {
						if (version != 4) {
							throw new RuntimeException("Bad version for mesh: " + version);
						}

						vertex_stream_count = b.getInt();
						index_size = b.getInt();
						index_count = b.getInt();
						vertex_count = b.getInt();
					}

					debug("vertex stream count: " + vertex_stream_count);

					for (int j = 0; j < vertex_stream_count; ++j) {
						int bytes_per_vertex = b.getInt();
						debug("Stream " + j + " has " + bytes_per_vertex + " bytes/vertex.");

						for (int k = 0; k < vertex_count; ++k) {
							t.read(data, b.position(), bytes_per_vertex, k);
							os.write(t.write().getBytes(Charsets.ISO_8859_1));
						}

						b.position(b.position() + bytes_per_vertex * vertex_count);
					}
				}
			} catch (Throwable var24) {
				var8 = var24;
				throw var24;
			} finally {
				if (os != null) {
					if (var8 != null) {
						try {
							os.close();
						} catch (Throwable var23) {
							var8.addSuppressed(var23);
						}
					} else {
						os.close();
					}
				}

			}

		}
	}

	private static class vector {
		private static final Splitter splitter = Splitter.on(' ');
		public float x;
		public float y;
		public float z;

		public vector() {
		}

		public vector(ByteBuffer bb) {
			this.read(bb);
		}

		String write() {
			return "v " + this.x + ' ' + this.y + ' ' + this.z + '\n';
		}

		void read(String line) {
			String[] parts = Iterables.toArray(splitter.split(line), String.class);
			if (!parts[0].equals("v")) {
				throw new RuntimeException("Can\'t read " + line + " into vector, expected start \'v\'");
			} else {
				this.x = Float.valueOf(parts[1]);
				this.y = Float.valueOf(parts[2]);
				this.z = Float.valueOf(parts[3]);
			}
		}

		void read(byte[] d, int offset, int size, int count) {
			int b = offset + size * count + 0;
			this.x = Float.intBitsToFloat(Ints.fromBytes(d[b + 3], d[b + 2], d[b + 1], d[b + 0]));
			b = offset + size * count + 4;
			this.y = Float.intBitsToFloat(Ints.fromBytes(d[b + 3], d[b + 2], d[b + 1], d[b + 0]));
			b = offset + size * count + 8;
			this.z = Float.intBitsToFloat(Ints.fromBytes(d[b + 3], d[b + 2], d[b + 1], d[b + 0]));
		}

		void write(byte[] d, int offset, int size, int count) {
			int b = offset + size * count + 0;
			byte[] p = Ints.toByteArray(Float.floatToIntBits(this.x));
			d[b + 3] = p[0];
			d[b + 2] = p[1];
			d[b + 1] = p[2];
			d[b + 0] = p[3];
			b = offset + size * count + 4;
			p = Ints.toByteArray(Float.floatToIntBits(this.y));
			d[b + 3] = p[0];
			d[b + 2] = p[1];
			d[b + 1] = p[2];
			d[b + 0] = p[3];
			b = offset + size * count + 8;
			p = Ints.toByteArray(Float.floatToIntBits(this.z));
			d[b + 3] = p[0];
			d[b + 2] = p[1];
			d[b + 1] = p[2];
			d[b + 0] = p[3];
		}

		void read(ByteBuffer bb) {
			this.x = bb.getFloat();
			this.y = bb.getFloat();
			this.z = bb.getFloat();
		}

		public String toString() {
			return this.x + "\t" + this.y + '\t' + this.z;
		}
	}
}
