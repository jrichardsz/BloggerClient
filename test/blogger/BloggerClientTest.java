package blogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.Test;

import blogger.BloggerClient.MacroRegion;
import blogger.util.FileUtils;

public class BloggerClientTest {
	private static final BloggerClient client = BloggerClient.getInstance();
	private static final String charsetName = "UTF-8";

	@Test
	public void readMetadataAndBodyTest() throws IOException, URISyntaxException {
		BufferedReader bufferedReader = null;
		StringBuilder[] mbArray;
		try {
			bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(
					getTestBlogFile()), charsetName));
			mbArray = client.readMetadataAndBody(bufferedReader);
		}
		finally {
			if (bufferedReader != null)
				bufferedReader.close();
		}
		System.out.println("========== metadata ============");
		System.out.println(mbArray[0]);
		System.out.println("========== body ============");
		System.out.println(mbArray[1]);
	}

	private File getTestBlogFile() throws URISyntaxException {
		java.net.URL url = getClass().getResource("test-zh.txt");
		File blogFile = new File(url.toURI());
		return blogFile;
	}

	@Test
	public void processBlogFileTest() throws Exception {
		BlogPostMetadata metadata = client.processBlogFile(getTestBlogFile(), charsetName);
		System.out.println(metadata);
		System.out.println(metadata.getHtmlBody());
	}

	@Test
	public void generatePlainLinkTest() {
		StringBuilder str = new StringBuilder("abchttp://test.com <a href=\"http://a.b.cn\">a.b.cn</a>def");
		System.out.println(str);
		client.generatePlainLink(str);
		System.out.println(str);
	}

	@Test
	public void getMacroRegionListTest() throws IOException {
		List<MacroRegion> mrList = client.getMacroRegionList(FileUtils
				.readPackageFileAsText("blogger/test-zh.txt"));
		for (MacroRegion mr : mrList) {
			System.out.println(mr);
		}
	}

}
