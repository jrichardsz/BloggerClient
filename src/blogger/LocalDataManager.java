package blogger;

import java.io.File;

public class LocalDataManager {

	private static final LocalDataManager INSTANCE = new LocalDataManager();

	private LocalDataManager() {
	}

	public static LocalDataManager getInstance() {
		return INSTANCE;
	}

	private final File homeDir = new File(System.getProperty("user.home"), "." + BloggerClient.NAME);

	public File getHome() {
		if (!homeDir.exists()) {
			if (!homeDir.mkdir()) {
				throw new RuntimeException(String.format("create directory [%s] failed", homeDir));
			}
		}
		else if (!homeDir.isDirectory()) {
			throw new RuntimeException(String.format("[%s] already exists and it's not directory.", homeDir));
		}
		return homeDir;
	}

	public File getConfFile() {
		return new File(getHome(), "conf.properties");
	}

	public File getUsersDir() {
		File dir = new File(getHome(), "users");
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}

	public File getUserDir(String username) {
		File dir = new File(getUsersDir(), username);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}

	public File getCredentialStoreFile(String username) {
		return new File(getUserDir(username), "credential_store");
	}

	public File getBlogsDir(String username) {
		File dir = new File(getUserDir(username), "blogs");
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}

	public File getBlogsCacheFile(String username) {
		return new File(getBlogsDir(username), "blogs_cache");
	}

	public File getBlogDir(String username, String blogId) {
		File dir = new File(getBlogsDir(username), blogId);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}

	public File getPostsCacheFile(String username, String blogId) {
		return new File(getBlogDir(username, blogId), "posts_cache");
	}

}
