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

	public File getUserDir(String userId) {
		File dir = new File(getUsersDir(), userId);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}

	public File getCredentialStoreFile(String userId) {
		return new File(getUserDir(userId), "credential_store");
	}

}
