package nallar.ps2edit.ui.viewer;

import javax.swing.*;

public class Viewer {
	private JTextField textField1;
	private JList list1;
	private JPanel panel;

	private void createUIComponents() {
		// TODO: place custom component creation code here
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame("Viewer");
		frame.setContentPane(new Viewer().panel);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}
}
