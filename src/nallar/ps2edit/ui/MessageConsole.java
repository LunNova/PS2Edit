package nallar.ps2edit.ui;

import javax.swing.text.*;
import java.awt.*;
import java.io.*;

public class MessageConsole {
	private JTextComponent textComponent;
	private Document document;
	private boolean isAppend;

	public MessageConsole(JTextComponent textComponent) {
		this(textComponent, true);
	}

	public MessageConsole(JTextComponent textComponent, boolean isAppend) {
		this.textComponent = textComponent;
		this.document = textComponent.getDocument();
		this.isAppend = isAppend;
		textComponent.setEditable(false);

		ConsoleOutputStream cos = new ConsoleOutputStream(Color.black, System.out);
		System.setOut(new PrintStream(cos, true));
		cos = new ConsoleOutputStream(Color.red, System.err);
		System.setErr(new PrintStream(cos, true));
	}

	private class ConsoleOutputStream extends ByteArrayOutputStream {
		private final String EOL = System.getProperty("line.separator");
		private SimpleAttributeSet attributes;
		private PrintStream printStream;
		private StringBuffer buffer = new StringBuffer(80);
		private boolean isFirstLine;

		public ConsoleOutputStream(Color textColor, PrintStream printStream) {
			if (textColor != null) {
				attributes = new SimpleAttributeSet();
				StyleConstants.setForeground(attributes, textColor);
			}

			this.printStream = printStream;

			if (isAppend)
				isFirstLine = true;
		}

		public void flush() {
			String message = toString();

			if (message.length() == 0) return;

			if (isAppend)
				handleAppend(message);
			else
				handleInsert(message);

			reset();
		}

		private void handleAppend(String message) {
			if (document.getLength() == 0)
				buffer.setLength(0);

			if (EOL.equals(message)) {
				buffer.append(message);
			} else {
				buffer.append(message);
				clearBuffer();
			}

		}

		private void handleInsert(String message) {
			buffer.append(message);

			if (EOL.equals(message)) {
				clearBuffer();
			}
		}

		private void clearBuffer() {
			if (isFirstLine && document.getLength() != 0) {
				buffer.insert(0, "\n");
			}

			isFirstLine = false;
			String line = buffer.toString();

			try {
				if (isAppend) {
					int offset = document.getLength();
					document.insertString(offset, line, attributes);
					textComponent.setCaretPosition(document.getLength());
				} else {
					document.insertString(0, line, attributes);
					textComponent.setCaretPosition(0);
				}
			} catch (BadLocationException ignored) {
			}

			if (printStream != null) {
				printStream.print(line);
			}

			buffer.setLength(0);
		}
	}
}