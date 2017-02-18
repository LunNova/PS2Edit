package net.ps2stats.edit

import sun.nio.ch.DirectBuffer
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel.MapMode
import java.nio.charset.Charset
import java.util.*
import java.util.zip.CRC32

class PackFile(val file: File) {
	val entryMap: MutableMap<String, PackFile.Entry> = HashMap()
	internal var f: MappedByteBuffer? = null
	private var openCount: Int = 0
	private var lastHeader: Int = 0
	private var headerFree: Int = 0

	init {

		try {
			if (!file.exists()) {
				if (!file.createNewFile()) {
					throw RuntimeException("Failed to create new pack file " + file)
				}
				this.makeHeader()
			} else {
				this.open_Read()
				var e = 0

				do {
					this.f!!.position(e)
					e = this.f!!.int
					if (e != 0) {
						this.lastHeader = e
					}

					val numEntries = this.f!!.int

					for (i in 0..numEntries - 1) {
						val entry = Entry(this.readString())
						entry.entryOffset = this.f!!.position()
						entry.offsetOffsetSizeCrc = this.f!!.position()
						entry.dataOffset = this.f!!.int
						entry.dataSize = this.f!!.int
						entry.maxSize = entry.dataSize
						entry.crc = this.f!!.int
						this.entryMap.put(entry.name, entry)
					}
				} while (e > 0)

				this.close_()
			}
		} catch (var6: IOException) {
			var6.printStackTrace()
			throw RuntimeException(var6)
		}

	}

	fun addFile(name: String): PackFile.Entry {
		f!!.position(this.lastHeader + 4)
		val size = headerSize(name)
		headerFree -= size
		if (headerFree < 0) {
			this.makeHeader()
		}

		val entries = this.f!!.int

		for (entry in 0..entries - 1) {
			this.f!!.position(this.f!!.int + this.f!!.position() + 12)
		}

		val var5 = Entry(name)
		this.entryMap.put(name, var5)
		var5.entryOffset = this.f!!.position()
		var5.writeEntryFullyAtCurrentPosition()
		this.f!!.position(this.lastHeader + 4)
		this.f!!.putInt(entries + 1)
		return var5
	}

	private fun makeHeader() {
		val headerFree = MAX_HEADER_SIZE.toShort()
		val end = this.expand(headerFree.toInt())
		this.open()
		this.f!!.position(this.lastHeader)
		this.f!!.putInt(end)
		this.f!!.position(end)
		this.f!!.putInt(0)
		this.f!!.putInt(0)
		val headerFree1 = headerFree - 8
		this.close()
		this.headerFree = headerFree1
		this.lastHeader = end
	}

	override fun hashCode(): Int {
		return this.file.name.hashCode()
	}

	@Synchronized fun open() {
		if (++this.openCount == 1) {
			this.open_()
		}
	}

	@Synchronized fun openRead() {
		if (++this.openCount == 1) {
			this.open_Read()
		}
	}

	@Synchronized fun closeForce() {
		if (this.openCount != 1) {
			throw RuntimeException("Wrong open count: " + this.openCount + " to force closed.")
		} else {
			this.f!!.force()
			this.close()
		}
	}

	@Synchronized fun close() {
		if (openCount > 0 && --this.openCount == 0) {
			this.close_()
		}
	}

	private fun close_() {
		(this.f as DirectBuffer).cleaner().clean()
		this.f = null
	}

	private fun open_() {
		try {
			RandomAccessFile(this.file, "rw").use { e -> this.f = e.channel.map(MapMode.READ_WRITE, 0L, e.length()) }
		} catch (var14: IOException) {
			throw RuntimeException(var14)
		}

	}

	private fun open_Read() {
		try {
			RandomAccessFile(this.file, "r").use { e -> this.f = e.channel.map(MapMode.READ_ONLY, 0L, e.length()) }
		} catch (var14: IOException) {
			throw RuntimeException(var14)
		}

	}

	private fun expand(bytes: Int): Int {
		val open = this.f != null
		if (open) {
			this.close_()
		}

		val start = this.expand_(bytes)
		if (open) {
			this.open_()
		}

		return start
	}

	private fun expand_(bytes: Int): Int {
		try {
			RandomAccessFile(this.file, "rw").use { e ->
				val start = e.length().toInt()
				e.setLength((start + bytes + 1).toLong())

				return start
			}
		} catch (var16: IOException) {
			throw RuntimeException(var16)
		}

	}

	private fun readString(): String {
		val nameLength = this.f!!.int
		if (nameLength > 256) {
			throw RuntimeException("Insane entry name length of " + nameLength)
		} else {
			val chars = ByteArray(nameLength)
			this.f!!.get(chars)
			return String(chars, charset)
		}
	}

	private fun writeString(string: String) {
		val nameLength = string.length
		if (nameLength > 256) {
			throw RuntimeException("Insane entry name length of " + nameLength)
		} else {
			this.f!!.putInt(nameLength)
			val chars = string.toByteArray()
			this.f!!.put(chars)
		}
	}

	fun asInt(): Int {
		return Integer.valueOf(this.file.name.replace("Assets_", "").replace(".pack", ""))!!
	}

	inner class Entry(val name: String) {
		var crc: Int = 0
		internal var entryOffset: Int = 0
		internal var dataOffset: Int = 0
		internal var dataSize: Int = 0
		internal var offsetOffsetSizeCrc: Int = 0
		internal var maxSize: Int = 0
		private var lastData: ByteArray? = null

		val packFile: PackFile
			get() = this@PackFile

		val stringData: String
			get() = String(this.data, PackFile.charset)

		val data: ByteArray
			get() {
				return this.lastData ?: {
					var close = false
					var buffer = this@PackFile.f
					if (buffer == null) {
						this@PackFile.openRead()
						close = true
						buffer = this@PackFile.f
					}
					try {
						buffer!!.position(this.dataOffset)
						val data = ByteArray(this.dataSize)
						this.lastData = data
						buffer.get(data)
						data
					} finally {
						if (close)
							this@PackFile.close()
					}
				}()
			}

		fun setData(data: String) {
			this.setData(data.toByteArray(PackFile.charset))
		}

		fun setData(data: ByteArray) {
			if (Arrays.equals(data, this.lastData)) {
				System.err.println("Updated " + this.name + " with matching cached data - ignoring.")
			} else {
				this.lastData = data
				PackFile.crc32.reset()
				PackFile.crc32.update(data)
				val newCrc = PackFile.crc32.value.toInt()
				if (newCrc == this.crc) {
					System.err.println(this.name + " is in assets_replace but has not been modified.")
				} else {
					this.crc = newCrc
					this.dataSize = data.size
					if (data.size > this.maxSize) {
						this.dataOffset = this@PackFile.expand(this.dataSize)
						this.maxSize = data.size
					}

					this@PackFile.f!!.position(this.dataOffset)
					this@PackFile.f!!.put(data)
					this.writeEntry()
				}
			}
		}

		fun getData(from: Int, length: Int): ByteArray {
			val data = this.data
			return Arrays.copyOfRange(data, from, from + length)
		}

		fun setData(from: Int, data: ByteArray) {
			val oldData = this.data
			System.arraycopy(data, 0, oldData, from, data.size)
			this.setData(oldData)
		}

		fun writeEntryFullyAtCurrentPosition() {
			this@PackFile.writeString(this.name)
			this.offsetOffsetSizeCrc = this@PackFile.f!!.position()
			this@PackFile.f!!.putInt(this.dataOffset)
			this@PackFile.f!!.putInt(this.dataSize)
			this@PackFile.f!!.putInt(this.crc)
		}

		fun writeEntry() {
			this@PackFile.f!!.position(this.offsetOffsetSizeCrc)
			this@PackFile.f!!.putInt(this.dataOffset)
			this@PackFile.f!!.putInt(this.dataSize)
			this@PackFile.f!!.putInt(this.crc)
		}

		override fun toString(): String {
			return "Entry{entryOffset=" + this.entryOffset + ", name=\'" + this.name + '\'' + ", dataOffset=" + this.dataOffset + ", dataSize=" + this.dataSize + ", crc=" + this.crc + ", offsetOffsetSizeCrc=" + this.offsetOffsetSizeCrc + ", maxSize=" + this.maxSize + '}'
		}
	}

	companion object {
		private val MAX_HEADER_SIZE = 2050
		private val charset = Charset.forName("ISO-8859-1")
		private val crc32 = CRC32()

		private fun headerSize(name: String): Int {
			return name.length + 16
		}
	}
}
