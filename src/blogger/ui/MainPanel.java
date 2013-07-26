package blogger.ui;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
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
	/** make sure update it in UI thread */
	private File currentDirectory = new File(System.getProperty("user.home"));

	private final AtomicInteger atomicGridy = new AtomicInteger(0);
	private final int columns = 2, insets = 5;

	public MainPanel(BloggerClientFrame frame) {
		this.frame = frame;
		createGUI();
	}

	private void createGUI() {
		setLayout(new GridBagLayout());

		final JTextField tfCharset = new JTextField(DEFAULT_CHARSET_NAME);
		final JButton bChoose = new JButton("Choose");
		final JButton bProcess = new JButton("Process");
		final JButton bPreview = new JButton("Preview");
		final JButton bPost = new JButton("Post");
		final JButton bClose = new JButton("Close");
		final JTextField tfPostFile = new JTextField();
		final JTextField tfTitle = new JTextField();
		final JTextField tfTags = new JTextField();
		final JTextField tfUniquetoken = new JTextField();
		final JTextArea taBodyHtml = new JTextArea();

		fillOneLineComponents(null, new JLabel("File encoding: "), tfCharset);
		fillOneLineComponents(null, new JLabel("Blog post file: "), tfPostFile);
		fillOneLineComponents(
				new GBC(1, atomicGridy.get(), 1, 1).setFill(GBC.HORIZONTAL).setWeight(1.0, 0.0),
				new JLabel("Drag & drop file above OR click \"Choose\" button"));
		final JPanel pButtons = new JPanel();
		pButtons.add(bChoose);
		pButtons.add(bProcess);
		pButtons.add(bPreview);
		pButtons.add(bPost);
		pButtons.add(bClose);
		fillOneLineComponents(
				new GBC(1, atomicGridy.get(), 1, 1).setFill(GBC.HORIZONTAL).setWeight(1.0, 0.0), pButtons);
		fillOneLineComponents(null, new JLabel("Title: "), tfTitle);
		fillOneLineComponents(null, new JLabel("Tags: "), tfTags);
		fillOneLineComponents(null, new JLabel("Unique token: "), tfUniquetoken);
		fillOneLineComponents(new GBC(0, atomicGridy.get(), columns, 1).setFill(GBC.HORIZONTAL)
				.setWeight(1.0, 0.0), new JLabel("Body html:"));
		fillOneLineComponents(
				new GBC(0, atomicGridy.get(), columns, 1).setFill(GBC.BOTH).setWeight(1.0, 1.0),
				new JScrollPane(taBodyHtml));

		// === initComponents ===
		//
		taBodyHtml.setEditable(false);
		taBodyHtml.setLineWrap(false);
		taBodyHtml.setWrapStyleWord(false);
		tfPostFile.setEditable(true);
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
						blogPostProcessor.processPostFile();
						return blogPostProcessor.getBlogPostInfoHolder();
					}

					@Override
					protected void done() {
						try {
							BlogPostInfoHolder holder = get();
							currentDirectory = blogPostProcessorRef.get().getPostFile().getParentFile();
							tfTitle.setText(holder.getTitle());
							tfTags.setText(holder.getTags());
							tfUniquetoken.setText(holder.getUniquetoken());
							taBodyHtml.setText(holder.getHtmlBody());
						}
						catch (Exception e) {
							UiUtils.showErrorMessage(frame, "Process failed.", e);
						}
					}
				}.execute();
			}
		});
		//
		bPreview.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				BlogPostProcessor blogPostProcessor = blogPostProcessorRef.get();
				BlogPostInfoHolder holder;
				if (blogPostProcessor != null
						&& (holder = blogPostProcessor.getBlogPostInfoHolder()) != null) {
					File htmlFile = holder.getHtmlFile();
					if (htmlFile == null)
						return;
					try {
						Desktop.getDesktop().open(htmlFile);
					}
					catch (IOException ex) {
						ex.printStackTrace();
						UiUtils.showErrorMessage(frame, "Open html file failed.", ex);
					}
				}
			}
		});
		//
		bPost.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final RemotePanel remotePanel = frame.getRemotePanel();
				if (!remotePanel.isLoggedIn()) {
					JOptionPane.showMessageDialog(frame, "Login at [Login] panel.");
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
								UiUtils.showErrorMessage(frame, "Post failed.", e);
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

	private void fillOneLineComponents(final GBC gbc, final Component... comps) {
		int gridx = 0, gridy = atomicGridy.get();
		if (gbc != null)
			gbc.setInsets(insets);
		if (comps.length == 1) {
			add(comps[0],
					gbc == null ? new GBC(0, gridy, columns, 1).setFill(GBC.HORIZONTAL).setInsets(insets)
							: gbc);
		}
		else {
			for (Component comp : comps) {
				add(comp,
						gbc == null ? new GBC(gridx++, gridy, 1, 1).setFill(GBC.HORIZONTAL).setInsets(insets)
								: gbc);
			}
		}
		atomicGridy.addAndGet(1);
	}

}
