package net.ps2stats.ui

import java.awt.Color
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import javax.swing.text.*

class MessageConsole @JvmOverloads constructor(private val textComponent: JTextComponent, private val isAppend: Boolean = true) {
	private val document: Document

	init {
		this.document = textComponent.document
		textComponent.isEditable = false

		var cos = ConsoleOutputStream(Color.black, System.out)
		System.setOut(PrintStream(cos, true))
		cos = ConsoleOutputStream(Color.red, System.err)
		System.setErr(PrintStream(cos, true))
	}

	private inner class ConsoleOutputStream(textColor: Color?, private val printStream: PrintStream?) : ByteArrayOutputStream() {
		private val EOL = System.getProperty("line.separator")
		private var attributes: SimpleAttributeSet? = null
		private val buffer = StringBuffer(80)
		private var isFirstLine: Boolean = false

		init {
			if (textColor != null) {
				attributes = SimpleAttributeSet()
				StyleConstants.setForeground(attributes!!, textColor)
			}

			if (isAppend)
				isFirstLine = true
		}

		override fun flush() {
			val message = toString()

			if (message.isEmpty()) return
			if (isAppend)
				handleAppend(message)
			else
				handleInsert(message)
			reset()
		}

		private fun handleAppend(message: String) {
			if (document.length == 0)
				buffer.setLength(0)

			if (EOL == message) {
				buffer.append(message)
			} else {
				buffer.append(message)
				clearBuffer()
			}

		}

		private fun handleInsert(message: String) {
			buffer.append(message)

			if (EOL == message) {
				clearBuffer()
			}
		}

		private fun clearBuffer() {
			if (isFirstLine && document.length != 0) {
				buffer.insert(0, "\n")
			}

			isFirstLine = false
			val line = buffer.toString()

			try {
				if (isAppend) {
					val offset = document.length
					document.insertString(offset, line, attributes)
					textComponent.caretPosition = document.length
				} else {
					document.insertString(0, line, attributes)
					textComponent.caretPosition = 0
				}
			} catch (ignored: BadLocationException) {
			}

			printStream?.print(line)

			buffer.setLength(0)
		}
	}
}
