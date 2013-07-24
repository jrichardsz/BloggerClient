package blogger.ui;

import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import blogger.BlogPostMetadata;
import blogger.BloggerAPIBuilder;
import blogger.BloggerUtils;
import blogger.LocalDataManager;
import blogger.PostListMemoryStore;
import blogger.util.LogicAssert;
import blogger.util.UiUtils;

import com.google.api.services.blogger.Blogger;
import com.google.api.services.blogger.model.Blog;
import com.google.api.services.blogger.model.BlogList;
import com.google.api.services.blogger.model.Post;

class RemotePanel extends JPanel {
	private static final long serialVersionUID = -3124121818719696806L;

	private final RemotePanel self = this;
	private final BloggerClientFrame frame;

	private String username;
	private Blogger blogger;
	private Blog blog;
	private final PostListMemoryStore postListStore = new PostListMemoryStore();

	public RemotePanel(BloggerClientFrame frame) throws Exception {
		this.frame = frame;
		createGUI();
	}

	private void createGUI() {
		final JButton bStart = new JButton("Start");
		add(bStart);
		bStart.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					String[] usernames = LocalDataManager.getInstance().getUsersDir().list();
					username = (usernames == null ? showUsernameInputDialog() : usernames[0]);
					LocalDataManager.getInstance().getUserDir(username).mkdirs();
					blogger = BloggerAPIBuilder.getInstance(username).getBlogger();
					self.start();
				}
				catch (IOException | GeneralSecurityException ex) {
					ex.printStackTrace();
				}
			}
		});
	}

	private void start() {
		final RemotePanel p = this;
		p.removeAll();
		p.setLayout(new GridBagLayout());
		final JButton bBlogDetails = new JButton("Blog details");
		final JLabel lBlog = new JLabel();
		final JButton bRetry = new JButton("Retry");

		final int columns = 1, distance = 5;
		final AtomicInteger gridyAtomic = new AtomicInteger(0);

		p.add(new JLabel(username),
				new GBC(0, gridyAtomic.getAndAdd(1), columns, 1).setFill(GBC.HORIZONTAL)
						.setInsets(distance));
		lBlog.setText("Loading blogs...");
		p.add(bBlogDetails, new GBC(0, gridyAtomic.getAndAdd(1), columns, 1).setFill(GBC.HORIZONTAL)
				.setInsets(distance));
		p.add(lBlog, new GBC(0, gridyAtomic.getAndAdd(1), columns, 1).setFill(GBC.HORIZONTAL)
				.setInsets(distance));
		p.add(bRetry, new GBC(0, gridyAtomic.getAndAdd(1), columns, 1).setFill(GBC.HORIZONTAL)
				.setInsets(distance));

		bRetry.setVisible(false);

		bBlogDetails.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (self.blog != null) {
					try {
						UiUtils.showTextInTextareaDialog(frame, blog.toPrettyString(), "Blog details",
								JOptionPane.PLAIN_MESSAGE);
					}
					catch (IOException ioe) {
						ioe.printStackTrace();
					}
				}
			}
		});

		bRetry.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				self.start();
			}
		});

		new SwingWorker<Integer, Void>() {
			final int retSucceed = 0, retLoadBlogsFailed = 1, retNoBlogs = 2;

			@Override
			protected Integer doInBackground() throws Exception {
				final Long maxResults = 20L;
				final Blogger blogger = self.blogger;

				// load blogs
				BlogList blogList = new BlogList();
				blogList.setItems(new ArrayList<Blog>());
				final File blogsCacheFile = LocalDataManager.getInstance().getBlogsCacheFile(username);
				if (blogsCacheFile.exists()) {
					try {
						BloggerUtils.parseJsonFromFile(blogsCacheFile, blogList);
						System.out.println("blogs count (from local): " + blogList.getItems().size());
					}
					catch (IOException e) {
						e.printStackTrace();
						blogList.getItems().clear();
					}
				}
				if (blogList.getItems().isEmpty()) {
					List<Blog> blogs = null;
					try {
						blogs = blogger.blogs().listByUser("self").execute().getItems();
						System.out.println("blogs count (from remote): " + blogList.getItems().size());
					}
					catch (IOException ex) {
						ex.printStackTrace();
						return retLoadBlogsFailed;
					}
					if (blogs == null || blogs.isEmpty()) {
						return retNoBlogs;
					}
					else {
						blogList.getItems().addAll(blogs);
						BloggerUtils.serializeJsonToFile(blogList, blogsCacheFile);
					}
				}

				// select blog
				Blog blog = blogList.getItems().get(0);
				self.blog = blog;

				// load posts
				final PostListMemoryStore postListStore = self.postListStore;
				try {
					postListStore.parseJsonFromFile(username, blog.getId());
					System.out.println("posts count (from local): " + postListStore.size());
				}
				catch (IOException e) {
					e.printStackTrace();
					postListStore.clear();
				}
				if (postListStore.isEmpty()) {
					final String fields = "items(id,title,url,labels,published)";
					List<Post> posts = blogger.posts().list(blog.getId()).setFields(fields)
							.setMaxResults(maxResults).execute().getItems();
					while (posts != null && !posts.isEmpty()) {
						postListStore.addAll(posts);
						posts = blogger.posts().list(blog.getId()).setFields(fields).setMaxResults(maxResults)
								.setEndDate(posts.get(posts.size() - 1).getPublished()).execute().getItems();
					}
					System.out.println("posts count (from remote): " + postListStore.size());
					postListStore.serializeJsonToFile(username, blog.getId());
				}
				return retSucceed;
			}

			@Override
			public void done() {
				try {
					final int ret = get().intValue();
					if (ret == retSucceed) {
						lBlog.setText(blog.getUrl());
						if (bRetry.isVisible())
							bRetry.setVisible(false);
					}
					else {
						if (!bRetry.isVisible())
							bRetry.setVisible(true);
						if (ret == retLoadBlogsFailed) {
							JOptionPane.showMessageDialog(frame, "Load blogs failed.", "Error",
									JOptionPane.ERROR_MESSAGE);
						}
						else if (ret == retNoBlogs) {
							lBlog.setText("No blogs.");
						}
					}
				}
				catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
					if (!bRetry.isVisible())
						bRetry.setVisible(true);
					JOptionPane.showMessageDialog(frame, "Load posts failed.", "Error",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		}.execute();
	}

	private String showUsernameInputDialog() {
		String errorMessage = null;
		while (true) {
			final JPanel p = new JPanel();
			p.setLayout(new GridBagLayout());
			final int distance = 5;
			final JTextField tfUsername = new JTextField();
			p.add(new JLabel("Input username, anyone you like, not Google Account."), new GBC(0, 0, 2, 1)
					.setFill(GBC.HORIZONTAL).setInsets(distance));
			p.add(new JLabel("Username:"), new GBC(0, 1, 1, 1).setInsets(distance));
			p.add(tfUsername, new GBC(1, 1, 1, 1).setFill(GBC.HORIZONTAL).setInsets(distance));
			if (errorMessage != null) {
				final JLabel label = new JLabel(errorMessage);
				label.setForeground(Color.RED);
				p.add(label, new GBC(0, 2, 2, 2).setFill(GBC.HORIZONTAL).setInsets(distance));
			}
			final Pattern usernamePattern = Pattern.compile("^[A-Za-z0-9._-]+$");
			final Object[] options = new Object[] { "   OK   ", " Cancel " };
			final int selection = JOptionPane.showOptionDialog(frame, p, "Input Username",
					JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, null);
			if (selection == JOptionPane.YES_OPTION) {
				final String username = tfUsername.getText();
				if (usernamePattern.matcher(username).matches()) {
					return username;
				}
				else {
					errorMessage = "Invalid username, valid format: [A-Za-z0-9._-]+";
					continue;
				}
			}
			else {
				errorMessage = "Must input a username.";
				continue;
			}
		}
	}

	boolean isLoggedIn() {
		return blog != null;
	}

	/**
	 * It will access network, do not invoke it in UI thread.
	 */
	void post(BlogPostMetadata metadata) throws IOException {
		final Post existingPost = postListStore.getByUniquetoken(metadata.getUniquetoken());
		if (existingPost == null) {
			String message = String.format("[%s] is a new post, do you want to create it?",
					metadata.getTitle());
			final int selection = JOptionPane.showConfirmDialog(null, message, "Create post",
					JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (selection == JOptionPane.YES_OPTION) {
				Post toBeCreatedPost = BloggerUtils.getPostObjForCreate(blog.getUrl(), metadata);
				Post createdPost = self.blogger.posts().insert(blog.getId(), toBeCreatedPost)
						.setFields("id,title,url,labels,published").execute();
				System.out.println(createdPost.toPrettyString());
				LogicAssert.assertTrue(toBeCreatedPost.getUrl().equals(createdPost.getUrl()),
						"urls don't match, expect [%s], got [%s]", toBeCreatedPost.getUrl(),
						createdPost.getUrl());
				postListStore.add(createdPost);
				postListStore.serializeJsonToFile(username, blog.getId());
			}
		}
		else {
			String message = String.format("[%s] is an old post ( %s ), do you want to update it?",
					metadata.getTitle(), existingPost.getUrl());
			final int selection = JOptionPane.showConfirmDialog(null, message, "Update post",
					JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (selection == JOptionPane.YES_OPTION) {
				Post updatePost = self.blogger.posts()
						.patch(blog.getId(), existingPost.getId(), BloggerUtils.getPostObjForUpdate(metadata))
						.setFields("id,title,url,labels,published").execute();
				System.out.println(updatePost.toPrettyString());
				postListStore.add(updatePost);
				postListStore.serializeJsonToFile(username, blog.getId());
			}
		}
	}

}
