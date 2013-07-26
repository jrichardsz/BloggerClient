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

import blogger.BlogPostInfoHolder;
import blogger.BlogPostProcessor;
import blogger.BloggerAPIBuilder;
import blogger.BloggerUtils;
import blogger.LocalFileLocator;
import blogger.PostListMemoryStore;
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
	private volatile Blog blog;
	private volatile PostListMemoryStore postListStore;

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
				if (username == null) {
					String[] usernames = LocalFileLocator.getInstance().getUsersDir().list();
					//TODO support several users, the first user is picked for now
					username = (usernames == null ? showUsernameInputDialog() : usernames[0]);
					LocalFileLocator.getInstance().getUserDir(username).mkdirs();
				}
				try {
					blogger = BloggerAPIBuilder.getInstance(username).getBlogger();
				}
				catch (IOException | GeneralSecurityException ex) {
					ex.printStackTrace();
					//TODO add "Open conf folder" button
					String message = String.format("Connect to server failed, try to enable proxy at %s",
							LocalFileLocator.getInstance().getConfFile());
					UiUtils.showErrorMessage(frame, message, ex);
					return;
				}
				self.start();
			}
		});
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
			private final int retSucceed = 0, retNoBlogs = 2;

			@Override
			protected Integer doInBackground() throws Exception {
				// load blogs
				BlogList blogList = new BlogList();
				blogList.setItems(new ArrayList<Blog>());
				final File blogsCacheFile = LocalFileLocator.getInstance().getBlogsCacheFile(username);
				if (blogsCacheFile.exists()) {
					try {
						BloggerUtils.parseJsonFromFile(blogList, blogsCacheFile);
						System.out.println("blogs count (from local): " + blogList.getItems().size());
					}
					catch (IOException e) {
						e.printStackTrace();
						blogList.getItems().clear();
					}
				}
				if (blogList.getItems().isEmpty()) {
					List<Blog> blogs = loadBlogs();
					if (blogs == null || blogs.isEmpty()) {
						return retNoBlogs;
					}
					else {
						blogList.getItems().addAll(blogs);
						BloggerUtils.serializeJsonToFile(blogList, blogsCacheFile);
					}
				}

				// select blog
				//TODO one user could create many blogs, the first one will be used for now
				Blog blog = blogList.getItems().get(0);
				self.blog = blog;

				// load posts
				final PostListMemoryStore postListStore = self.postListStore = new PostListMemoryStore(
						username, blog.getId());
				try {
					postListStore.parseFromFile();
					System.out.println("posts count (from local): " + postListStore.size());
				}
				catch (IOException e) {
					e.printStackTrace();
					postListStore.clear();
				}
				if (postListStore.isEmpty()) {
					loadPosts();
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
						if (ret == retNoBlogs) {
							lBlog.setText("No blogs.");
						}
					}
				}
				catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
					if (!bRetry.isVisible())
						bRetry.setVisible(true);
					UiUtils.showErrorMessage(frame, "Load blogs/posts failed.", e);
				}
			}
		}.execute();
	}

	private List<Blog> loadBlogs() throws IOException {
		List<Blog> blogs = blogger.blogs().listByUser("self").execute().getItems();
		System.out.println("blogs count (from remote): " + (blogs == null ? 0 : blogs.size()));
		return blogs;
	}

	private static final String REQ_POSTLIST_FIELDS = "items(id,title,url,labels,published,updated)";

	private void loadPosts() throws IOException {
		final long maxResults = 20L;
		final Blogger blogger = self.blogger;
		final PostListMemoryStore postListStore = self.postListStore;
		final Blog blog = self.blog;
		List<Post> posts = blogger.posts().list(blog.getId()).setFields(REQ_POSTLIST_FIELDS)
				.setMaxResults(maxResults).execute().getItems();
		while (posts != null && !posts.isEmpty()) {
			postListStore.addAll(posts);
			posts = blogger.posts().list(blog.getId()).setFields(REQ_POSTLIST_FIELDS)
					.setMaxResults(maxResults).setEndDate(posts.get(posts.size() - 1).getPublished())
					.execute().getItems();
		}
		System.out.println("posts count (from remote): " + postListStore.size());
		postListStore.serializeToFile();
	}

	boolean isLoggedIn() {
		return blog != null;
	}

	private static final String REQ_POST_FIELDS = "id,title,url,labels,published,updated";

	/**
	 * It will access network, do not invoke it in UI thread.
	 */
	void post(BlogPostProcessor blogPostProcessor) throws IOException {
		final BlogPostInfoHolder holder = blogPostProcessor.getBlogPostInfoHolder();
		final Post existingPost = postListStore.getByUniquetoken(holder.getUniquetoken());
		if (existingPost == null) {
			String message = String.format("[%s] is a new post, do you want to create it?",
					holder.getTitle());
			final int selection = JOptionPane.showConfirmDialog(null, message, "Create post",
					JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (selection == JOptionPane.YES_OPTION) {
				insertPost(blogPostProcessor);
			}
		}
		else {
			String message = String.format("[%s] is an old post ( %s ), do you want to update it?",
					holder.getTitle(), existingPost.getUrl());
			final int selection = JOptionPane.showConfirmDialog(null, message, "Update post",
					JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (selection == JOptionPane.YES_OPTION) {
				patchPost(existingPost.getId(), holder);
			}
		}
	}

	private void insertPost(BlogPostProcessor blogPostProcessor) throws IOException {
		final BlogPostInfoHolder holder = blogPostProcessor.getBlogPostInfoHolder();
		/*
		 * can't custom permalink when inserting a post, workaround steps:
		 * 1) insert a post, set title as unique token of permalink
		 * 2) update tile to the real title
		 */
		//- insert post
		Post toBeCreatedPost = BloggerUtils.getPostObjForCreate(blog.getUrl(), holder);
		toBeCreatedPost.setTitle(holder.getUniquetoken());
		Post createdPost = blogger.posts().insert(blog.getId(), toBeCreatedPost)
				.setFields(REQ_POST_FIELDS).execute();
		System.out.println(createdPost.toPrettyString());
		postListStore.add(createdPost);
		postListStore.serializeToFile();
		//- update title
		/*
		 * Blogger server has a bug, if only update title,
		 * then title will be updated, but it response "400 Bad Request",
		 * so update content and labels too.
		 * It's allowed in document https://developers.google.com/blogger/docs/3.0/performance#patch
		 */
		Post updatedPost = patchPost(createdPost.getId(), holder);
		//- if the generated unique token is not the expected one, then accept it and update local metadata
		String newUniquetoken = BloggerUtils.getPostUrlUniquetoken(updatedPost.getUrl());
		if (!holder.getUniquetoken().equals(newUniquetoken)) {
			// update local post file
			blogPostProcessor.updateNewUniquetokenAndSerialize(newUniquetoken);
		}
	}

	private Post patchPost(String postId, BlogPostInfoHolder holder) throws IOException {
		final Post toBeUpdatedPost = BloggerUtils.getPostObjForUpdate(holder);
		final Post updatedPost = blogger.posts().patch(blog.getId(), postId, toBeUpdatedPost)
				.setFields(REQ_POST_FIELDS).execute();
		System.out.println(updatedPost.toPrettyString());
		postListStore.add(updatedPost);
		postListStore.serializeToFile();
		return updatedPost;
	}

}
