package blogger.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FileUtils {

	public static File getFileByPathOrURI(String path) throws URISyntaxException {
		path = path.trim();
		if (path.startsWith("file://")) {
			// drag and drop file to Swing text field, the uri may be invalid
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
	 * do NOT include Unicode BOM in package file, it won't be removed.
	 * 
	 * @param name
	 *          file relative path, i.e. blogger/macro/PlainHtmlElem.list
	 * @return text lines, it won't be null
	 * @throws URISyntaxException
	 *           if package file can't be loaded
	 * @see {@link java.nio.file.Files.readAllLines}
	 */
	public static List<String> readPackageFileAsLines(String name, Charset charset)
			throws IOException, URISyntaxException {
		List<String> result = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(FileUtils.class
				.getClassLoader().getResourceAsStream(name), charset))) {
			String line;
			while ((line = reader.readLine()) != null) {
				result.add(line);
			}
		}
		return result;
	}

	/**
	 * load package file from sub-package as text.
	 * <p>
	 * do NOT include Unicode BOM in package file, it won't be removed.
	 * 
	 * @param name
	 *          file relative path, i.e. blogger/macro/PlainHtmlElem.list
	 * @return file's text, it won't be null
	 * @throws URISyntaxException
	 *           if package file can't be loaded
	 */
	public static String readPackageFileAsText(String name, Charset charset) throws IOException,
			URISyntaxException {
		StringBuilder text = new StringBuilder(8192);
		try (Reader reader = new BufferedReader(new InputStreamReader(FileUtils.class.getClassLoader()
				.getResourceAsStream(name), charset))) {
			char[] cbuf = new char[1024];
			int len;
			while ((len = reader.read(cbuf)) > 0) {
				text.append(cbuf, 0, len);
			}
		}
		return text.toString();
	}

	/**
	 * @return text, it won't be null
	 */
	public static String readFileAsText(File file, String charsetName) throws IOException {
		return new String(Files.readAllBytes(file.toPath()), charsetName);
	}

	/**
	 * @return text, it won't be null
	 */
	public static String readFileAsText(File file, Charset charset) throws IOException {
		return new String(Files.readAllBytes(file.toPath()), charset);
	}

	private static final Random FILENAME_RANDOM = new Random();

	/**
	 * create a empty temp file at file's directory, temp file's name is based on file's name, i.e.
	 * input "f1.txt", may output "f1.txt.tmp.467913456"
	 */
	public static File getTempFile(File file) throws IOException {
		File tempFile;
		while (true) {
			tempFile = new File(file.getParent(), file.getName() + ".tmp." + FILENAME_RANDOM.nextInt());
			if (!tempFile.exists()) {
				if (!tempFile.createNewFile()) {
					throw new IOException(String.format("create %s failed", tempFile));
				}
				return tempFile;
			}
			else
				continue;
		}
	}

}
