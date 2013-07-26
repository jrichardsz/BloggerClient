package blogger;

import java.awt.Desktop;
import java.net.ProxySelector;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import blogger.ui.BloggerClientFrame;
import blogger.util.UiUtils;

public class BloggerClient {
	public static final String VERSION = "V0.6.0";
	public static final String NAME = "BloggerClientZZ"; // do not contain blanks

	public static void main(String[] args) {
		// custom L&F
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		JFrame.setDefaultLookAndFeelDecorated(true);
		JDialog.setDefaultLookAndFeelDecorated(true);
		//
		if (!verifyDesktopOpen()) {
			return;
		}
		//
		try {
			Configuration cfg = new Configuration(LocalFileLocator.getInstance().getConfFile(),
					BloggerClient.class.getResource("conf.properties").toURI());
			ProxySelector.setDefault(cfg.getProxySelector());
		}
		catch (Exception e) {
			e.printStackTrace();
			UiUtils.showErrorMessage(null, "Load conf.properties failed.", e);
			return;
		}
		// show UI
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					INSTANCE.createAndShowGUI();
				}
				catch (Exception e) {
					e.printStackTrace();
					UiUtils.showErrorMessage(null, "Create main UI failed.", e);
					System.exit(1);
				}
			}
		});
	}

	private static boolean verifyDesktopOpen() {
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
			return true;
		}
		else {
			String message = "File can't be opened by system default viewer, so html can't be previewed."
					+ " If you are running Linux,"
					+ " try to read http://zenzhong8383.blogspot.com/2013/05/enable-java-awt-desktop-on-linux-en.html"
					+ " to solve it.";
			UiUtils.showTextInTextareaDialog(null, message, "Desktop open doesn't work",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

	private static final BloggerClient INSTANCE = new BloggerClient();

	private BloggerClient() {
	}

	public static BloggerClient getInstance() {
		return INSTANCE;
	}

	private void createAndShowGUI() throws Exception {
		final BloggerClientFrame frame = new BloggerClientFrame(NAME);
		frame.setSize(900, 900);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

}
