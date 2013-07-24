package blogger;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.Test;

import blogger.BlogPostProcessor.MacroRegion;
import blogger.util.FileUtils;

public class BlogPostProcessorTest {
	private static final String charsetName = "UTF-8";

	@Test
	public void readMetadataAndBodyTest() throws IOException, URISyntaxException {
		StringBuilder[] mbArray = new BlogPostProcessor().readMetadataAndBody(getTestBlogFile(),
				charsetName);
		System.out.println("========== metadata ============");
		System.out.println(mbArray[0]);
		System.out.println("========== body ============");
		System.out.println(mbArray[1]);
	}

	private File getTestBlogFile() throws URISyntaxException {
		return new File(getClass().getResource("test-zh.txt").toURI());
	}

	@Test
	public void processBlogFileTest() throws Exception {
		BlogPostMetadata metadata = new BlogPostProcessor().processBlogFile(getTestBlogFile(),
				charsetName);
		System.out.println(metadata);
		System.out.println(metadata.getHtmlBody());
	}

	@Test
	public void generatePlainLinkTest() {
		StringBuilder str = new StringBuilder(
				"abchttp://test.com <a href=\"http://a.b.cn\">a.b.cn</a>def");
		System.out.println(str);
		new BlogPostProcessor().generatePlainLink(str);
		System.out.println(str);
	}

	@Test
	public void getMacroRegionListTest() throws IOException {
		List<MacroRegion> mrList = new BlogPostProcessor().getMacroRegionList(FileUtils
				.readPackageFileAsText("blogger/test-zh.txt"));
		for (MacroRegion mr : mrList) {
			System.out.println(mr);
		}
	}

}
