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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import blogger.BlogPostMetadata;
import blogger.BlogPostProcessor;
import blogger.util.FileUtils;
import blogger.util.UiUtils;

public class ManualOperationPanel extends JPanel {
	private static final long serialVersionUID = 5520046034875968938L;

	private final BloggerClientFrame frame;

	private static final String DEFAULT_CHARSET_NAME = "UTF-8";
	private File currentDirectory = new File(System.getProperty("user.home"));

	private final AtomicInteger atomicGridy = new AtomicInteger(0);
	private final int columns = 2, insets = 5;

	public ManualOperationPanel(BloggerClientFrame frame) {
		this.frame = frame;
		createGUI();
	}

	private void createGUI() {
		final ManualOperationPanel p = this;
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
		fillOneLineComponents(p, new GBC(1, atomicGridy.get(), 1, 1).setFill(GBC.HORIZONTAL)
				.setWeight(1.0, 0.0), new JLabel(
				"Drag & drop file above OR click \"Choose blog file\" button"));
		final JPanel btnPanel = new JPanel();
		btnPanel.add(chooseBlogFileBtn);
		btnPanel.add(processBlogFileBtn);
		btnPanel.add(closeBlogFileBtn);
		fillOneLineComponents(p, new GBC(1, atomicGridy.get(), 1, 1).setFill(GBC.HORIZONTAL)
				.setWeight(1.0, 0.0), btnPanel);
		fillOneLineComponents(p, null, new JLabel("Title: "), titleField);
		fillOneLineComponents(p, null, new JLabel("Tags: "), tagsField);
		fillOneLineComponents(p, null, new JLabel("Unique token: "), uniquetokenField);
		fillOneLineComponents(p, new GBC(0, atomicGridy.get(), columns, 1).setFill(GBC.BOTH)
				.setWeight(1.0, 1.0), new JScrollPane(blogHtmlArea));

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
					UiUtils.handleEDTException(frame, ex);
				}
			}
		});
		//
		chooseBlogFileBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser jfc = new JFileChooser(currentDirectory);
				jfc.setPreferredSize(new Dimension(900, 900));
				jfc.showOpenDialog(frame);
				final File blogFile = jfc.getSelectedFile();
				if (blogFile != null) {
					currentDirectory = jfc.getCurrentDirectory();
					currentBlogFileField.setText(blogFile.getAbsolutePath());
				}
			}
		});
		//
		final AtomicReference<BlogPostProcessor> blogPostProcessorRef = new AtomicReference<>();
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
						BlogPostProcessor blogPostProcessor = new BlogPostProcessor();
						blogPostProcessorRef.set(blogPostProcessor);
						return blogPostProcessor.processBlogFile(blogFile, charsetName);
					}

					@Override
					protected void done() {
						try {
							BlogPostMetadata metadata = get();
							titleField.setText(metadata.getTitle());
							tagsField.setText(metadata.getTags());
							uniquetokenField.setText(metadata.getUniquetoken());
							blogHtmlArea.setText(metadata.getHtmlBody());
							Desktop.getDesktop().open(metadata.getHtmlFile());
						}
						catch (Exception e) {
							UiUtils.handleEDTException(frame, e);
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
				File blogHtmlFile = blogPostProcessorRef.get().getBlogPostMetadata().getHtmlFile();
				if (blogHtmlFile != null) {
					blogHtmlFile.delete();
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
				p.add(comp, gbc == null ? new GBC(gridx++, gridy, 1, 1).setFill(GBC.HORIZONTAL)
						.setInsets(insets) : gbc);
			}
		}
		atomicGridy.addAndGet(1);
	}

}
