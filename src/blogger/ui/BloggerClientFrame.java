package blogger.ui;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

public class BloggerClientFrame extends JFrame {
	private static final long serialVersionUID = -7454768920259135362L;

	public BloggerClientFrame(String title) {
		super(title);
		final JTabbedPane contentPane = new JTabbedPane();
		contentPane.addTab("Manual Operation", new ManualOperationPanel(this));
		contentPane.addTab("Auto Sync", new AutoSyncPanel(this));
		setContentPane(contentPane);
	}

}
