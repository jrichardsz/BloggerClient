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

	public PostListMemoryStore() {
		postList.setItems(new ArrayList<Post>(initialCapacity));
	}

	/**
	 * add a post to store, post could already exist in store
	 * @param post a com.google.api.services.blogger.model.Post
	 */
	public synchronized void add(Post post) {
		// atomic
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

	private File getPostsCacheFile(String username, String blogId) {
		return LocalDataManager.getInstance().getPostsCacheFile(username, blogId);
	}

	public synchronized void parseJsonFromFile(String username, String blogId) throws IOException {
		File file = getPostsCacheFile(username, blogId);
		if (file.exists()) {
			PostList tempPostList = new PostList();
			tempPostList.setItems(new ArrayList<Post>(initialCapacity));
			BloggerUtils.parseJsonFromFile(file, tempPostList);
			addAll(tempPostList.getItems());
		}
	}

	public synchronized void serializeJsonToFile(String username, String blogId) throws IOException {
		File file = getPostsCacheFile(username, blogId);
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

}
