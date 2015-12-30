//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package nallar.ps2edit;

import sun.nio.ch.DirectBuffer;

import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel.*;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.*;

public class PackFile {
	private static final int MAX_HEADER_SIZE = 2050;
	private static final Charset charset = Charset.forName("ISO-8859-1");
	private static final CRC32 crc32 = new CRC32();
	public final Map<String, PackFile.Entry> entryMap = new HashMap<>();
	final File file;
	MappedByteBuffer f;
	private int openCount;
	private int lastHeader;
	private int headerFree;

	public PackFile(File file) {
		this.file = file;

		try {
			if (!file.exists()) {
				if (!file.createNewFile()) {
					throw new RuntimeException("Failed to create new pack file " + file);
				}
				this.makeHeader();
			} else {
				this.open_Read();
				int e = 0;

				do {
					this.f.position(e);
					e = this.f.getInt();
					if (e != 0) {
						this.lastHeader = e;
					}

					int numEntries = this.f.getInt();

					for (int i = 0; i < numEntries; ++i) {
						PackFile.Entry entry = new PackFile.Entry();
						entry.entryOffset = this.f.position();
						entry.name = this.readString();
						entry.offsetOffsetSizeCrc = this.f.position();
						entry.dataOffset = this.f.getInt();
						entry.maxSize = entry.dataSize = this.f.getInt();
						entry.crc = this.f.getInt();
						this.entryMap.put(entry.name, entry);
					}
				} while (e > 0);

				this.close_();
			}
		} catch (IOException var6) {
			var6.printStackTrace();
			throw new RuntimeException(var6);
		}
	}

	private static int headerSize(String name) {
		return name.length() + 16;
	}

	public PackFile.Entry addFile(String name) {
		this.f.position(this.lastHeader + 4);
		int size = headerSize(name);
		if ((this.headerFree -= size) < 0) {
			this.makeHeader();
		}

		int entries = this.f.getInt();

		for (int entry = 0; entry < entries; ++entry) {
			this.f.position(this.f.getInt() + this.f.position() + 12);
		}

		PackFile.Entry var5 = new PackFile.Entry();
		this.entryMap.put(name, var5);
		var5.name = name;
		var5.entryOffset = this.f.position();
		var5.writeEntryFullyAtCurrentPosition();
		this.f.position(this.lastHeader + 4);
		this.f.putInt(entries + 1);
		return var5;
	}

	private void makeHeader() {
		short headerFree = MAX_HEADER_SIZE;
		int end = this.expand(headerFree);
		this.open();
		this.f.position(this.lastHeader);
		this.f.putInt(end);
		this.f.position(end);
		this.f.putInt(0);
		this.f.putInt(0);
		int headerFree1 = headerFree - 8;
		this.close();
		this.headerFree = headerFree1;
		this.lastHeader = end;
	}

	public int hashCode() {
		return this.file.getName().hashCode();
	}

	public synchronized void open() {
		if (++this.openCount == 1) {
			this.open_();
		}
	}

	public void openRead() {
		if (++this.openCount == 1) {
			this.open_Read();
		}
	}

	public synchronized void closeForce() {
		if (this.openCount != 1) {
			throw new RuntimeException("Wrong open count: " + this.openCount + " to force closed.");
		} else {
			this.f.force();
			this.close();
		}
	}

	public synchronized void close() {
		if (--this.openCount == 0) {
			this.close_();
		}
	}

	private void close_() {
		((DirectBuffer) this.f).cleaner().clean();
		this.f = null;
	}

	private void open_() {
		try (RandomAccessFile e = new RandomAccessFile(this.file, "rw")) {
			this.f = e.getChannel().map(MapMode.READ_WRITE, 0L, e.length());
		} catch (IOException var14) {
			throw new RuntimeException(var14);
		}
	}

	private void open_Read() {
		try (RandomAccessFile e = new RandomAccessFile(this.file, "r")) {
			this.f = e.getChannel().map(MapMode.READ_ONLY, 0L, e.length());
		} catch (IOException var14) {
			throw new RuntimeException(var14);
		}
	}

	private int expand(int bytes) {
		boolean open = this.f != null;
		if (open) {
			this.close_();
		}

		int start = this.expand_(bytes);
		if (open) {
			this.open_();
		}

		return start;
	}

	private int expand_(int bytes) {
		try (RandomAccessFile e = new RandomAccessFile(this.file, "rw")) {
			int start = (int) e.length();
			e.setLength((long) (start + bytes + 1));

			return start;
		} catch (IOException var16) {
			throw new RuntimeException(var16);
		}
	}

	private String readString() {
		int nameLength = this.f.getInt();
		if (nameLength > 256) {
			throw new RuntimeException("Insane entry name length of " + nameLength);
		} else {
			byte[] chars = new byte[nameLength];
			this.f.get(chars);
			return new String(chars, charset);
		}
	}

	private void writeString(String string) {
		int nameLength = string.length();
		if (nameLength > 256) {
			throw new RuntimeException("Insane entry name length of " + nameLength);
		} else {
			this.f.putInt(nameLength);
			byte[] chars = string.getBytes();
			this.f.put(chars);
		}
	}

	public int asInt() {
		return Integer.valueOf(this.file.getName().replace("Assets_", "").replace(".pack", ""));
	}

	public class Entry {
		public String name;
		public int crc;
		int entryOffset;
		int dataOffset;
		int dataSize;
		int offsetOffsetSizeCrc;
		int maxSize;
		private byte[] lastData;

		public Entry() {
		}

		public PackFile getPackFile() {
			return PackFile.this;
		}

		public String getStringData() {
			return new String(this.getData(), PackFile.charset);
		}

		public byte[] getData() {
			if (this.lastData != null) {
				return this.lastData;
			} else {
				boolean close = false;
				MappedByteBuffer buffer = PackFile.this.f;
				if (buffer == null) {
					PackFile.this.openRead();
					close = true;
					buffer = PackFile.this.f;
				}
				try {
					buffer.position(this.dataOffset);
					byte[] data = new byte[this.dataSize];
					this.lastData = data;
					buffer.get(data);
					return data;
				} finally {
					if (close)
						PackFile.this.close();
				}
			}
		}

		public void setData(byte[] data) {
			if (Arrays.equals(data, this.lastData)) {
				System.err.println("Updated " + this.name + " with matching cached data - ignoring.");
			} else {
				this.lastData = data;
				PackFile.crc32.reset();
				PackFile.crc32.update(data);
				int newCrc = (int) PackFile.crc32.getValue();
				if (newCrc == this.crc) {
					System.err.println(this.name + " is in assets_replace but has not been modified.");
				} else {
					this.crc = newCrc;
					this.dataSize = data.length;
					if (data.length > this.maxSize) {
						this.dataOffset = PackFile.this.expand(this.dataSize);
						this.maxSize = data.length;
					}

					PackFile.this.f.position(this.dataOffset);
					PackFile.this.f.put(data);
					this.writeEntry();
				}
			}
		}

		public void setData(String data) {
			this.setData(data.getBytes(PackFile.charset));
		}

		public byte[] getData(int from, int length) {
			byte[] data = this.getData();
			return Arrays.copyOfRange(data, from, from + length);
		}

		public void setData(int from, byte[] data) {
			byte[] oldData = this.getData();
			System.arraycopy(data, 0, oldData, from, data.length);
			this.setData(oldData);
		}

		public void writeEntryFullyAtCurrentPosition() {
			PackFile.this.writeString(this.name);
			this.offsetOffsetSizeCrc = PackFile.this.f.position();
			PackFile.this.f.putInt(this.dataOffset);
			PackFile.this.f.putInt(this.dataSize);
			PackFile.this.f.putInt(this.crc);
		}

		public void writeEntry() {
			PackFile.this.f.position(this.offsetOffsetSizeCrc);
			PackFile.this.f.putInt(this.dataOffset);
			PackFile.this.f.putInt(this.dataSize);
			PackFile.this.f.putInt(this.crc);
		}

		public String toString() {
			return "Entry{entryOffset=" + this.entryOffset + ", name=\'" + this.name + '\'' + ", dataOffset=" + this.dataOffset + ", dataSize=" + this.dataSize + ", crc=" + this.crc + ", offsetOffsetSizeCrc=" + this.offsetOffsetSizeCrc + ", maxSize=" + this.maxSize + '}';
		}
	}
}
