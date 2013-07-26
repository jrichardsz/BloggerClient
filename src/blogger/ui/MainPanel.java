package blogger.ui;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import blogger.BlogPostInfoHolder;
import blogger.BlogPostProcessor;
import blogger.util.FileUtils;
import blogger.util.UiUtils;

class MainPanel extends JPanel {
	private static final long serialVersionUID = 5520046034875968938L;

	private final BloggerClientFrame frame;

	private static final String DEFAULT_CHARSET_NAME = "UTF-8";
	private File currentDirectory = new File(System.getProperty("user.home"));

	private final AtomicInteger atomicGridy = new AtomicInteger(0);
	private final int columns = 2, insets = 5;

	public MainPanel(BloggerClientFrame frame) {
		this.frame = frame;
		createGUI();
	}

	private void createGUI() {
		final MainPanel p = this;
		p.setLayout(new GridBagLayout());

		final JTextField tfCharset = new JTextField(DEFAULT_CHARSET_NAME);
		final JButton bChoose = new JButton("Choose");
		final JButton bProcess = new JButton("Process");
		final JButton bPost = new JButton("Post");
		final JButton bClose = new JButton("Close");
		final JTextField tfPostFile = new JTextField();
		final JTextField tfTitle = new JTextField();
		final JTextField tfTags = new JTextField();
		final JTextField tfUniquetoken = new JTextField();
		final JTextArea taBodyHtml = new JTextArea();

		fillOneLineComponents(p, null, new JLabel("File encoding: "), tfCharset);
		fillOneLineComponents(p, null, new JLabel("Blog post file: "), tfPostFile);
		fillOneLineComponents(p,
				new GBC(1, atomicGridy.get(), 1, 1).setFill(GBC.HORIZONTAL).setWeight(1.0, 0.0),
				new JLabel("Drag & drop file above OR click \"Choose\" button"));
		final JPanel pButtons = new JPanel();
		pButtons.add(bChoose);
		pButtons.add(bProcess);
		pButtons.add(bPost);
		pButtons.add(bClose);
		fillOneLineComponents(p,
				new GBC(1, atomicGridy.get(), 1, 1).setFill(GBC.HORIZONTAL).setWeight(1.0, 0.0), pButtons);
		fillOneLineComponents(p, null, new JLabel("Title: "), tfTitle);
		fillOneLineComponents(p, null, new JLabel("Tags: "), tfTags);
		fillOneLineComponents(p, null, new JLabel("Unique token: "), tfUniquetoken);
		fillOneLineComponents(p, new GBC(0, atomicGridy.get(), columns, 1).setFill(GBC.HORIZONTAL)
				.setWeight(1.0, 0.0), new JLabel("Body html:"));
		fillOneLineComponents(p,
				new GBC(0, atomicGridy.get(), columns, 1).setFill(GBC.BOTH).setWeight(1.0, 1.0),
				new JScrollPane(taBodyHtml));

		// === initComponents ===
		//
		taBodyHtml.setEditable(false);
		taBodyHtml.setLineWrap(false);
		taBodyHtml.setWrapStyleWord(false);
		//
		tfPostFile.setEditable(true);
		tfPostFile.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				try {
					currentDirectory = FileUtils.getFileByPathOrURI(tfPostFile.getText()).getParentFile();
				}
				catch (URISyntaxException ex) {
					UiUtils.handleEDTException(frame, ex);
				}
			}
		});
		//
		bChoose.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser jfc = new JFileChooser(currentDirectory);
				jfc.setPreferredSize(new Dimension(900, 900));
				jfc.showOpenDialog(frame);
				final File blogFile = jfc.getSelectedFile();
				if (blogFile != null) {
					currentDirectory = jfc.getCurrentDirectory();
					tfPostFile.setText(blogFile.getAbsolutePath());
				}
			}
		});
		//
		final AtomicReference<BlogPostProcessor> blogPostProcessorRef = new AtomicReference<>();
		//
		bProcess.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final String blogFilePath = tfPostFile.getText().trim();
				if (blogFilePath.isEmpty())
					return;
				new SwingWorker<BlogPostInfoHolder, Void>() {
					@Override
					protected BlogPostInfoHolder doInBackground() throws Exception {
						if (blogPostProcessorRef.get() != null) {
							File htmlFile = blogPostProcessorRef.get().getBlogPostInfoHolder().getHtmlFile();
							if (htmlFile != null) {
								htmlFile.delete();
							}
							blogPostProcessorRef.set(null);
						}
						File postFile = FileUtils.getFileByPathOrURI(blogFilePath);
						String charsetName = tfCharset.getText().trim();
						if (charsetName.isEmpty())
							charsetName = DEFAULT_CHARSET_NAME;
						BlogPostProcessor blogPostProcessor = new BlogPostProcessor(postFile, charsetName);
						blogPostProcessorRef.set(blogPostProcessor);
						return blogPostProcessor.processPostFile();
					}

					@Override
					protected void done() {
						try {
							BlogPostInfoHolder holder = get();
							tfTitle.setText(holder.getTitle());
							tfTags.setText(holder.getTags());
							tfUniquetoken.setText(holder.getUniquetoken());
							taBodyHtml.setText(holder.getHtmlBody());
							Desktop.getDesktop().open(holder.getHtmlFile());
						}
						catch (Exception e) {
							UiUtils.handleEDTException(frame, e);
						}
					}
				}.execute();
			}
		});
		//
		bPost.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final RemotePanel remotePanel = frame.getRemotePanel();
				if (!remotePanel.isLoggedIn()) {
					JOptionPane.showMessageDialog(frame, "Login first.");
					return;
				}
				else {
					final BlogPostProcessor blogPostProcessor = blogPostProcessorRef.get();
					if (blogPostProcessor == null)
						return;
					if (blogPostProcessor.getBlogPostInfoHolder().getHtmlBody() == null)
						return;
					UiUtils.setEnabled(false, bChoose, bProcess, bPost, bClose);
					new SwingWorker<Void, Void>() {
						@Override
						protected Void doInBackground() throws Exception {
							remotePanel.post(blogPostProcessor);
							return null;
						}

						@Override
						public void done() {
							try {
								get();
							}
							catch (InterruptedException | ExecutionException e) {
								e.printStackTrace();
								JOptionPane.showMessageDialog(frame, "Post failed.", "Error",
										JOptionPane.ERROR_MESSAGE);
							}
							finally {
								UiUtils.setEnabled(true, bChoose, bProcess, bPost, bClose);
							}
						}
					}.execute();
				}
			}
		});
		//
		bClose.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				tfPostFile.setText("");
				tfTitle.setText("");
				tfTags.setText("");
				tfUniquetoken.setText("");
				taBodyHtml.setText("");
				if (blogPostProcessorRef.get() != null) {
					File htmlFile = blogPostProcessorRef.get().getBlogPostInfoHolder().getHtmlFile();
					if (htmlFile != null) {
						htmlFile.delete();
					}
					blogPostProcessorRef.set(null);
				}
			}
		});
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

}
