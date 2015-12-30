package nallar.ps2edit.ui.viewer;

import com.google.common.html.HtmlEscapers;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import lombok.Data;
import lombok.val;
import me.nallar.jdds.JDDS;
import nallar.ps2edit.Assets;
import nallar.ps2edit.PackFile;
import nallar.ps2edit.Patcher;
import nallar.ps2edit.Paths;
import nallar.ps2edit.util.Throw;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

public class Viewer {
	private final Map<String, PackFile.Entry> assetsMap;
	private final List<String> assetsList;
	private final Paths path;
	private final Assets assets;
	private final Console console;
	private JTextField searchField;
	private JList<String> list;
	private JPanel panel;
	private JLabel label;
	private JScrollPane leftScrollPane;
	private JScrollPane rightScrollPane;
	private JSplitPane splitPane;
	private JButton patchAndRunButton;

	public Viewer(Console console) {
		this.console = console;
		path = new Paths();
		assets = new Assets(path, false);
		assetsMap = assets.getFiles();
		assetsList = new ArrayList<>(assetsMap.keySet());
		Collections.sort(assetsList);
		list.addListSelectionListener((e) -> {
			if (!e.getValueIsAdjusting())
				updateSelection();
		});
		list.addMouseListener(new FileListListener(list));
		searchField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				handleSearch();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				handleSearch();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				handleSearch();
			}
		});
		leftScrollPane.getVerticalScrollBar().setUnitIncrement(20);
		rightScrollPane.getVerticalScrollBar().setUnitIncrement(20);
		handleSearch();
		patchAndRunButton.addActionListener(e -> {
			launchGame();
		});
	}

	private static String convertStringForLabel(String stringData) {
		if (stringData.length() > 100000) {
			stringData = stringData.substring(0, 100000);
		}
		stringData = HtmlEscapers.htmlEscaper().escape(stringData);
		stringData = stringData.replace("\r\n", "<br>");
		stringData = stringData.replace("\n", "<br>");
		return "<html>" + stringData + "</html>";
	}

	public static void main(String[] args) {
		val console = Console.create();

		JFrame frame = new JFrame("Viewer");
		frame.setContentPane(new Viewer(console).panel);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

	private void launchGame() {
		new Thread(() -> {
			console.panel.grabFocus();
			Patcher.main(path);
		}).start();
	}

	private void handleSearch() {
		List<String> strings = searchAssets(searchField.getText());
		list.setListData(strings.toArray(new String[strings.size()]));
	}

	private List<String> searchAssets(String search) {
		if (search.isEmpty())
			return getReplacements();

		search = search.toLowerCase();
		val results = new ArrayList<String>();

		boolean plainSearch = false;
		try {
			val regex = Pattern.compile(search, Pattern.CASE_INSENSITIVE);
			for (String asset : assetsList) {
				if (regex.matcher(asset).find())
					results.add(asset);
			}
		} catch (PatternSyntaxException e) {
			plainSearch = true;
		}

		if (plainSearch) {
			System.out.println("Plain search for: " + search);
			for (String asset : assetsList) {
				if (asset.toLowerCase().contains(search))
					results.add(asset);
			}
		}

		return results.size() > 25000 ? results.subList(0, 25000) : results;
	}

	private List<String> getReplacements() {
		val list = new ArrayList<String>();

		val replacementsArray = path.replacementsDir.list();

		List<String> replacements = replacementsArray == null ? Collections.emptyList() : Arrays.asList(replacementsArray);

		list.add("Showing " + replacements.size() + " replacement files.");
		list.add("Enter text in the search box above to search all PS2 files.");
		list.add("Right click -> edit to open the file for editing.");

		list.addAll(replacements);

		return list;
	}

	private void updateSelection() {
		val selected = list.getSelectedValue();
		val entry = assetsMap.get(selected);

		if (selected == null || selected.isEmpty())
			return;

		val i = selected.lastIndexOf('.');
		val type = selected.substring(i + 1, selected.length()).toLowerCase();
		label.setText(null);
		label.setIcon(null);

		if (entry == null)
			return;

		switch (type) {
			case "xml":
			case "txt":
			case "cfg":
			case "adr":
			case "ini":
			case "props":
				// plain text
				label.setText(convertStringForLabel(entry.getStringData()));
				break;
			case "dds":
				// compressed DDS image
				label.setIcon(new ImageIcon(JDDS.readDDS(entry.getData())));
				break;
			default:
				label.setText("Can not preview this file type");
		}
	}

	private void createUIComponents() {
		// TODO: place custom component creation code here
	}

	{
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
		$$$setupUI$$$();
	}

	/**
	 * Method generated by IntelliJ IDEA GUI Designer
	 * >>> IMPORTANT!! <<<
	 * DO NOT edit this method OR call it in your code!
	 *
	 * @noinspection ALL
	 */
	private void $$$setupUI$$$() {
		panel = new JPanel();
		panel.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
		searchField = new JTextField();
		searchField.setToolTipText("File Search");
		panel.add(searchField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(400, -1), null, 0, false));
		patchAndRunButton = new JButton();
		patchAndRunButton.setText("Patch and Run");
		panel.add(patchAndRunButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		splitPane = new JSplitPane();
		splitPane.setContinuousLayout(false);
		splitPane.setEnabled(true);
		splitPane.setResizeWeight(0.2);
		panel.add(splitPane, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(500, 500), null, 0, false));
		rightScrollPane = new JScrollPane();
		splitPane.setLeftComponent(rightScrollPane);
		list = new JList();
		final DefaultListModel defaultListModel1 = new DefaultListModel();
		list.setModel(defaultListModel1);
		rightScrollPane.setViewportView(list);
		leftScrollPane = new JScrollPane();
		splitPane.setRightComponent(leftScrollPane);
		label = new JLabel();
		label.setText("");
		leftScrollPane.setViewportView(label);
	}

	/**
	 * @noinspection ALL
	 */
	public JComponent $$$getRootComponent$$$() {
		return panel;
	}

	@Data
	private static class NameXZ {
		final String name;
		final int x;
		final int z;
	}

	private class FileListListener implements MouseListener {
		private static final int IMAGE_SIZE = 256;
		private static final int GRID_SPACING = 4;
		private final JList<String> list;
		private final JPopupMenu menu;
		private final JMenuItem exportMap;

		public FileListListener(JList<String> list) {
			this.list = list;
			this.menu = new JPopupMenu();
			exportMap = menuItem("Export Map", this::exportMap);

			menu.add(menuItem("Edit", (e) -> {

				List<String> selectedFiles = list.getSelectedValuesList();
				List<PackFile.Entry> entryList = new ArrayList<PackFile.Entry>();

				for (String selectedFile : selectedFiles) {
					PackFile.Entry asset = assetsMap.get(selectedFile);

					if (asset == null) {
						continue;
					}

					entryList.add(asset);
				}

				if (Desktop.isDesktopSupported()) {
					try {
						Desktop.getDesktop().open(path.replacementsDir);
					} catch (IOException e1) {
						throw Throw.sneaky(e1);
					}
				}
				assets.forEntries(entryList, (asset) -> {
					File replacement = new File(path.replacementsDir, asset.name);
					if (!replacement.exists()) {
						try {
							byte[] data = asset.getData();
							Files.write(replacement.toPath(), data);
						} catch (IOException e1) {
							throw Throw.sneaky(e1);
						}
					}
				});
			}));
		}

		private JMenuItem menuItem(String name, ActionListener l) {
			JMenuItem item = new JMenuItem(name);
			item.addActionListener(l);
			return item;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (!SwingUtilities.isRightMouseButton(e))
				return;

			if (list.getSelectedIndices().length <= 1)
				list.setSelectedIndex(list.locationToIndex(e.getPoint()));

			menu.remove(exportMap);
			if (list.getSelectedValue() != null &&
					list.getSelectedValue().toLowerCase().contains("tile_") &&
					list.getSelectedValue().toLowerCase().contains("lod0")) {
				menu.add(exportMap);
			}

			menu.show(e.getComponent(), e.getX(), e.getY());
		}

		private void exportMap(ActionEvent actionEvent) {
			String selectedItem = list.getSelectedValue();
			String start = selectedItem.substring(0, selectedItem.toLowerCase().indexOf("_tile"));
			Pattern search = Pattern.compile('^' + start + "_tile_([-\\d]+)_([-\\d]+)_LOD0\\.dds$", Pattern.CASE_INSENSITIVE);
			List<NameXZ> files = new ArrayList<>();

			int maxX, maxZ, minX, minZ;
			maxX = maxZ = Integer.MIN_VALUE;
			minX = minZ = Integer.MAX_VALUE;
			for (String item : assetsList) {
				Matcher m = search.matcher(item);
				if (m.find()) {
					val x = Integer.parseInt(m.group(1));
					val z = Integer.parseInt(m.group(2));
					if (x > maxX)
						maxX = x;
					if (z > maxZ)
						maxZ = z;
					if (x < minX)
						minX = x;
					if (z < minZ)
						minZ = z;

					files.add(new NameXZ(item, x, z));
				}
			}


			val width = ((maxX - minX) / GRID_SPACING) * IMAGE_SIZE;
			val height = ((maxZ - minZ) / GRID_SPACING) * IMAGE_SIZE;
			val image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			val graphics = image.createGraphics();

			for (NameXZ nameXZ : files) {
				val drawX = ((nameXZ.x - minX) / GRID_SPACING) * IMAGE_SIZE;
				val drawZ = ((nameXZ.z - minZ) / GRID_SPACING) * IMAGE_SIZE;

				val entry = assetsMap.get(nameXZ.getName());

				graphics.drawImage(JDDS.readDDS(entry.getData()), drawX, drawZ, null);
			}

			File output = new File(path.replacementsDir, start + " map.png");
			try {
				ImageIO.write(image, "png", output);
			} catch (IOException e) {
				throw Throw.sneaky(e);
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
		}

		@Override
		public void mouseReleased(MouseEvent e) {
		}

		@Override
		public void mouseEntered(MouseEvent e) {
		}

		@Override
		public void mouseExited(MouseEvent e) {
		}
	}
}
