package blogger.assets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

public class AssetsLoader {

	/**
	 * load asset file from assets sub-package as text, AssetsLoader.class.getResource(name) method
	 * will be used.
	 * <p>
	 * asset encoding must be UTF-8.
	 * </p>
	 * <p>
	 * do NOT include Unicode BOM in asset file, it won't be removed.
	 * </p>
	 * 
	 * @param name
	 *          asset relative path, i.e. html/toc-head.html
	 * @return asset's text
	 */
	public static StringBuilder readAssetAsText(final String name) throws IOException,
			URISyntaxException {
		final StringBuilder result = new StringBuilder(1024);
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(
					AssetsLoader.class.getResource(name).toURI())), "UTF-8"));
			final char[] cbuf = new char[1024];
			int len;
			while ((len = bufferedReader.read(cbuf)) >= 0) {
				result.append(cbuf, 0, len);
			}
		}
		finally {
			if (bufferedReader != null)
				bufferedReader.close();
		}
		return result;
	}

}
