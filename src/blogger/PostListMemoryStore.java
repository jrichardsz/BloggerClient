package blogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import blogger.util.LogicAssert;

import com.google.api.services.blogger.model.Post;
import com.google.api.services.blogger.model.PostList;

public class PostListMemoryStore {
	private final int initialCapacity = 512;

	private final PostList postList = new PostList();

	/** map<uniquetoken in url, Post> */
	private final HashMap<String, Post> uniquetokenIndex = new HashMap<>(initialCapacity);

	/** map<post ID, Post> */
	private final HashMap<String, Post> idIndex = new HashMap<>(initialCapacity);

	private final String username, blogId;

	public PostListMemoryStore(String username, String blogId) throws IllegalArgumentException {
		if (username == null) {
			throw new IllegalArgumentException("username is null");
		}
		if (blogId == null) {
			throw new IllegalArgumentException("blogId is null");
		}
		postList.setItems(new ArrayList<Post>(initialCapacity));
		this.username = username;
		this.blogId = blogId;
	}

	/**
	 * add a post to store, post could already exist in store.<br/>
	 * It's atomic.
	 * 
	 * @param post
	 *          a com.google.api.services.blogger.model.Post
	 */
	public synchronized void add(Post post) {
		final String uniquetoken = BloggerUtils.getPostUrlUniquetoken(post.getUrl());
		Post oldPost = uniquetokenIndex.get(uniquetoken);
		if (oldPost != null) {
			LogicAssert.assertTrue(oldPost.getId().equals(post.getId()),
					"Duplicated uniquetoken found, [%s], [%s]", oldPost.getUrl(), post.getUrl());
		}
		final String postId = post.getId();
		if (idIndex.containsKey(postId)) {
			final List<Post> posts = postList.getItems();
			for (int i = 0, len = post.size(); i < len; i++) {
				if (postId.equals(posts.get(i).getId())) {
					posts.set(i, post);
					break;
				}
			}
		}
		else {
			postList.getItems().add(post);
		}
		uniquetokenIndex.put(uniquetoken, post);
		idIndex.put(postId, post);
	}

	public synchronized void addAll(Collection<Post> c) {
		for (Post post : c) {
			add(post);
		}
	}

	private File getPostsCacheFile() {
		return LocalFileLocator.getInstance().getPostsCacheFile(username, blogId);
	}

	public synchronized void parseFromFile() throws IOException {
		File file = getPostsCacheFile();
		if (file.exists()) {
			PostList tempPostList = new PostList();
			tempPostList.setItems(new ArrayList<Post>(initialCapacity));
			BloggerUtils.parseJsonFromFile(tempPostList, file);
			addAll(tempPostList.getItems());
		}
	}

	public synchronized void serializeToFile() throws IOException {
		File file = getPostsCacheFile();
		if (file.exists())
			BloggerUtils.serializeJsonToFile(postList, file);
	}

	public synchronized int size() {
		return postList.getItems().size();
	}

	public synchronized boolean isEmpty() {
		return postList.getItems().size() == 0;
	}

	public synchronized void clear() {
		postList.getItems().clear();
	}

	public synchronized Post getByUniquetoken(String uniquetoken) {
		return uniquetokenIndex.get(uniquetoken);
	}

	@Override
	public String toString() {
		return String.format("PostListMemoryStore [username=%s, blogId=%s]", username, blogId);
	}

}
