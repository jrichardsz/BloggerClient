package blogger.ui;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

public class BloggerClientFrame extends JFrame {
	private static final long serialVersionUID = -7454768920259135362L;

	private final MainPanel mainPanel;
	private final RemotePanel remotePanel;

	public BloggerClientFrame(String title) throws Exception {
		super(title);
		final JTabbedPane contentPane = new JTabbedPane();
		this.mainPanel = new MainPanel(this);
		contentPane.addTab("Main", mainPanel);
		this.remotePanel = new RemotePanel(this);
		contentPane.addTab("Login", remotePanel);
		setContentPane(contentPane);
	}

	MainPanel getMainPanel() {
		return mainPanel;
	}

	RemotePanel getRemotePanel() {
		return remotePanel;
	}

}
