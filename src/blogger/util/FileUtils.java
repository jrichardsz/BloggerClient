package blogger.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;

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
	 * @see {@link java.nio.file.Files.readAllLines}
	 */
	public static List<String> readPackageFileAsLines(String name, Charset charset)
			throws IOException, URISyntaxException {
		return Files.readAllLines(
				new File(FileUtils.class.getClassLoader().getResource(name).toURI()).toPath(), charset);
	}

	/**
	 * load package file from sub-package as text.
	 * <p>
	 * do NOT include Unicode BOM in package file, it won't be removed.
	 * 
	 * @param name
	 *          file relative path, i.e. blogger/macro/PlainHtmlElem.list
	 * @return file's text, it won't be null
	 */
	public static String readPackageFileAsText(String name, Charset charset) throws IOException,
			URISyntaxException {
		return readFileAsText(new File(FileUtils.class.getClassLoader().getResource(name).toURI()),
				charset);
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

}
