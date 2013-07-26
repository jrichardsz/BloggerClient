package blogger.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import blogger.BloggerClient;

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

	public static void enableContainers(JComponent container, boolean b) {
		if (container.getComponentCount() > 0) {
			Component[] comps = container.getComponents();
			for (Component comp : comps) {
				if (comp instanceof JComponent) {
					enableContainers((JComponent) comp, b);
					comp.setEnabled(b);
				}
			}
		}
	}

	public static void showErrorMessage(final Component parentComponent, String message,
			final Exception ex) {
		ex.printStackTrace();
		final JButton bDetails = new JButton("Show details");
		bDetails.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final StringWriter sw = new StringWriter(1024);
				sw.append(BloggerClient.NAME).append(' ').append(BloggerClient.VERSION).append('\n');
				ex.printStackTrace(new PrintWriter(sw));
				showTextInTextareaDialog(parentComponent, sw.toString(), "Exception stack",
						JOptionPane.ERROR_MESSAGE);
			}
		});
		Object[] options = new Object[] { bDetails, "Close" };
		JOptionPane.showOptionDialog(parentComponent, message, "Error", JOptionPane.YES_NO_OPTION,
				JOptionPane.ERROR_MESSAGE, null, options, null);
	}

	public static void setEnabled(boolean b, Component... components) {
		for (Component component : components) {
			component.setEnabled(b);
		}
	}

}
