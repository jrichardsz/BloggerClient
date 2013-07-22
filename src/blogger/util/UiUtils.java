package blogger.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class UiUtils {

	/**
	 * {@link javax.swing.JOptionPane#showMessageDialog(Component, Object, String, int)} will be used,
	 * parameters are the same as it.
	 * 
	 * @param messageType
	 *          JOptionPane message type
	 */
	public static void showTextInTextareaDialog(Component parentComponent, String message,
			String title, int messageType) {
		final JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(800, 600));
		BorderLayout layout = new BorderLayout();
		panel.setLayout(layout);
		JTextArea exStackArea = new JTextArea();
		exStackArea.setEditable(false);
		exStackArea.setLineWrap(false);
		exStackArea.setWrapStyleWord(false);
		exStackArea.setMinimumSize(new Dimension(640, 240));
		panel.add(new JScrollPane(exStackArea), BorderLayout.CENTER);
		exStackArea.setText(message);
		JOptionPane.showMessageDialog(parentComponent, panel, title, messageType);
	}

	public static void enableDisableContainers(JComponent container, boolean enable) {
		if (container.getComponentCount() > 0) {
			Component[] comps = container.getComponents();
			for (Component comp : comps) {
				if (comp instanceof JComponent) {
					enableDisableContainers((JComponent) comp, enable);
					comp.setEnabled(enable);
				}
			}
		}
	}

}
