package blogger;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import blogger.util.FileUtils;
import blogger.util.GBC;

class BloggerClientFrame extends JFrame {
	private static final long serialVersionUID = -7454768920259135362L;

	private static final String DEFAULT_CHARSET_NAME = "UTF-8";
	private final BloggerClient client = BloggerClient.getInstance();
	private File currentDirectory = new File(System.getProperty("user.home"));

	private final AtomicInteger atomicGridy = new AtomicInteger(0);
	private final int columns = 2, insets = 5;
	private final Dimension preferredSize = new Dimension(900, 900);

	public BloggerClientFrame(String title) {
		super(title);
		setSize(preferredSize);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		setContentPane(getContentPanel());
	}

	private JPanel getContentPanel() {
		JPanel p = new JPanel();
		p.setLayout(new GridBagLayout());

		final JTextField charsetField = new JTextField(DEFAULT_CHARSET_NAME);
		final JButton chooseBlogFileBtn = new JButton("Choose blog file");
		final JButton processBlogFileBtn = new JButton("Process blog file");
		final JButton closeBlogFileBtn = new JButton("Close blog file");
		final JTextField currentBlogFileField = new JTextField();
		final JTextField titleField = new JTextField();
		final JTextField tagsField = new JTextField();
		final JTextField uniquetokenField = new JTextField();
		final JTextArea blogHtmlArea = new JTextArea();

		fillOneLineComponents(p, null, new JLabel("File encoding: "), charsetField);
		fillOneLineComponents(p, null, new JLabel("Current blog file: "), currentBlogFileField);
		fillOneLineComponents(p,
				new GBC(1, atomicGridy.get(), 1, 1).setFill(GBC.HORIZONTAL).setWeight(1.0, 0.0),
				new JLabel("Drag & drop file above OR click \"Choose blog file\" button"));
		final JPanel btnPanel = new JPanel();
		btnPanel.add(chooseBlogFileBtn);
		btnPanel.add(processBlogFileBtn);
		btnPanel.add(closeBlogFileBtn);
		fillOneLineComponents(p,
				new GBC(1, atomicGridy.get(), 1, 1).setFill(GBC.HORIZONTAL).setWeight(1.0, 0.0), btnPanel);
		fillOneLineComponents(p, null, new JLabel("Title: "), titleField);
		fillOneLineComponents(p, null, new JLabel("Tags: "), tagsField);
		fillOneLineComponents(p, null, new JLabel("Unique token: "), uniquetokenField);
		fillOneLineComponents(p,
				new GBC(0, atomicGridy.get(), columns, 1).setFill(GBC.BOTH).setWeight(1.0, 1.0),
				new JScrollPane(blogHtmlArea));

		// === initComponents ===
		//
		blogHtmlArea.setEditable(false);
		blogHtmlArea.setLineWrap(false);
		blogHtmlArea.setWrapStyleWord(false);
		//
		currentBlogFileField.setEditable(true);
		currentBlogFileField.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				try {
					currentDirectory = FileUtils.getFileByPathOrURI(currentBlogFileField.getText())
							.getParentFile();
				}
				catch (URISyntaxException ex) {
					handleEDTException(ex);
				}
			}
		});
		//
		chooseBlogFileBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser jfc = new JFileChooser(currentDirectory);
				jfc.setPreferredSize(preferredSize);
				jfc.showOpenDialog(BloggerClientFrame.this);
				final File blogFile = jfc.getSelectedFile();
				if (blogFile != null) {
					currentDirectory = jfc.getCurrentDirectory();
					currentBlogFileField.setText(blogFile.getAbsolutePath());
				}
			}
		});
		//
		processBlogFileBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final String blogFilePath = currentBlogFileField.getText().trim();
				if (blogFilePath.isEmpty())
					return;
				new SwingWorker<BlogPostMetadata, Void>() {
					@Override
					protected BlogPostMetadata doInBackground() throws Exception {
						File blogFile = FileUtils.getFileByPathOrURI(blogFilePath);
						String charsetName = charsetField.getText().trim();
						if (charsetName.isEmpty())
							charsetName = DEFAULT_CHARSET_NAME;
						return client.processBlogFile(blogFile, charsetName);
					}

					@Override
					protected void done() {
						try {
							BlogPostMetadata metadata = get();
							titleField.setText(metadata.getTitle());
							tagsField.setText(metadata.getTags());
							uniquetokenField.setText(metadata.getUniquetoken());
							blogHtmlArea.setText(metadata.getHtmlBody());
							if (verifyDesktopOpen()) {
								Desktop.getDesktop().open(metadata.getHtmlFile());
							}
						}
						catch (Exception e) {
							handleEDTException(e);
						}
					}
				}.execute();
			}
		});
		//
		closeBlogFileBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				currentBlogFileField.setText("");
				titleField.setText("");
				tagsField.setText("");
				uniquetokenField.setText("");
				blogHtmlArea.setText("");
				File blogHtmlFile = client.getBlogPostMetadata().getHtmlFile();
				client.getBlogHtmlFileList().remove(blogHtmlFile);
				if (blogHtmlFile.exists() && blogHtmlFile.delete() == false) {
					System.out.println(String.format("delete %s failed", blogHtmlFile.getAbsolutePath()));
				}
			}
		});

		return p;
	}

	private void handleEDTException(Exception e) {
		//TODO redirect stdout/stderr to UI console
		e.printStackTrace();
		// show error message in textarea
		StringWriter sw = new StringWriter(1024);
		sw.append(BloggerClient.NAME).append(' ').append(BloggerClient.VERSION).append('\n');
		e.printStackTrace(new PrintWriter(sw));
		showTextInTextareaDialog(sw.toString(), "Exception stack", JOptionPane.ERROR_MESSAGE);
	}

	private void fillOneLineComponents(final JPanel p, final GBC gbc, final Component... comps) {
		int gridx = 0, gridy = atomicGridy.get();
		if (gbc != null)
			gbc.setInsets(insets);
		if (comps.length == 1) {
			p.add(comps[0], gbc == null ? new GBC(0, gridy, columns, 1).setFill(GBC.HORIZONTAL)
					.setInsets(insets) : gbc);
		}
		else {
			for (Component comp : comps) {
				p.add(comp,
						gbc == null ? new GBC(gridx++, gridy, 1, 1).setFill(GBC.HORIZONTAL).setInsets(insets)
								: gbc);
			}
		}
		atomicGridy.addAndGet(1);
	}

	private boolean verifyDesktopOpen() {
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
			return true;
		}
		else {
			String message = "File can't be opened by system default viewer, so html can't be previewed."
					+ " If you are running Linux,"
					+ " try to read http://zenzhong8383.blogspot.com/2013/05/enable-java-awt-desktop-on-linux-en.html"
					+ " to solve it.";
			showTextInTextareaDialog(message, "Desktop open doesn't work", JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

	private void showTextInTextareaDialog(String message, String title, int messageType) {
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
		JOptionPane.showMessageDialog(this, panel, title, messageType);
	}

}
