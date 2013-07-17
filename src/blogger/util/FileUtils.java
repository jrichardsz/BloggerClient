package blogger.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

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

	/**
	 * load package file from sub-package as text lines.
	 * <p>
	 * file encoding must be UTF-8.
	 * </p>
	 * <p>
	 * do NOT include Unicode BOM in package file, it won't be removed.
	 * </p>
	 * 
	 * @param name
	 * @return file's text lines file relative path, i.e. blogger/macro/PlainHtmlElem.list
	 */
	public static List<String> readPackageFileAsLines(final String name) throws IOException {
		final List<String> result = new ArrayList<>();
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new InputStreamReader(FileUtils.class.getClassLoader()
					.getResourceAsStream(name), "UTF-8"));
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				result.add(line);
			}
		}
		finally {
			if (bufferedReader != null)
				bufferedReader.close();
		}
		return result;
	}

}
