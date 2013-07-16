package blogger.util;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public class FileUtils {

	public static File getFileByPathOrURI(String path) throws URISyntaxException {
		path = path.trim();
		if (path.startsWith("file://")) {
			path = path.replace(" ", "%20");
			return new File(new URI(path));
		}
		else {
			return new File(path);
		}
	}

}
