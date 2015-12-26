package nallar.ps2edit.ui.viewer;

import com.google.common.html.HtmlEscapers;
import lombok.val;
import nallar.ps2edit.Assets;
import nallar.ps2edit.PackFile;
import nallar.ps2edit.Paths;
import nallar.ps2edit.util.Throw;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.io.*;
import java.util.*;

public class Viewer {
	private JTextField searchField;
	private JList<String> list;
	private JPanel panel;
	private JLabel label;
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
		Collections.sort(assetsList);
		list.addListSelectionListener((e) -> {
			if (!e.getValueIsAdjusting())
				updateSelection();
		});
		searchField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				change();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				change();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				change();
			}

			private void change() {
				List<String> strings = searchAssets(searchField.getText());
				list.setListData(strings.toArray(new String[strings.size()]));
			}
		});
	}

	private List<String> searchAssets(String search) {
		if (search.isEmpty())
			return getReplacements();

		search = search.toLowerCase();
		val results = new ArrayList<String>();
		for (String asset : assetsList) {
			if (asset.toLowerCase().contains(search))
				results.add(asset);
		}

		return results.size() > 25000 ? results.subList(0, 25000) : results;
	}

	private List<String> getReplacements() {
		val list = path.replacementsDir.list();
		return list == null ? Collections.emptyList() : Arrays.asList(list);
	}

	private void updateSelection() {
		val selected = list.getSelectedValue();
		val entry = assetsMap.get(selected);

		if (entry == null)
			return;

		entry.getPackFile().openRead();

		try {

			if (isTextDocument(selected)) {
				label.setText(convertStringForLabel(entry.getStringData()));
			} else {
				label.setText("Can not preview this file type");
			}
		} finally {
			entry.getPackFile().close();
		}
	}

	private static String convertStringForLabel(String stringData) {
		stringData = HtmlEscapers.htmlEscaper().escape(stringData);
		stringData = stringData.replace("\r\n", "<br>");
		stringData = stringData.replace("\n", "<br>");
		return "<html>" + stringData + "</html>";
	}

	private static boolean isTextDocument(String name) {
		val i = name.lastIndexOf('.');
		if (i == -1)
			return false;
		val type = name.substring(i + 1, name.length());
		switch (type) {
			case "xml":
			case "txt":
			case "cfg":
			case "ini":
				return true;
		}
		return false;
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
