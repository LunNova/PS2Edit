package nallar.ps2edit.ui.viewer;

import lombok.val;
import nallar.ps2edit.Assets;
import nallar.ps2edit.PackFile;
import nallar.ps2edit.Paths;
import nallar.ps2edit.util.Throw;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Viewer {
	private JTextField textField1;
	private JList list;
	private JPanel panel;
	private final Map<String, PackFile.Entry> assetsMap;
	private final List<String> assetsList;
	private final Paths path;
	private final Assets assets;

	public Viewer()  {
		path = new Paths();
		try {
			assets = new Assets(path, false);
		} catch (IOException e) {
			throw Throw.sneaky(e);
		}
		assetsMap = assets.getFiles();
		assetsList = new ArrayList<>(assetsMap.keySet());
	}

	private void searchAssets(String search) {
		val results = new ArrayList<String>();
		for (String asset : assetsList) {
			results.add(asset);
		}
	}

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
