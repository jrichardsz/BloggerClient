package blogger;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Test;

import blogger.BlogPostProcessor.MacroRegion;
import blogger.util.FileUtils;

public class BlogPostProcessorTest {
	private final BlogPostProcessor blogPostProcessor = new BlogPostProcessor(getTestPostFile(),
			"UTF-8");

	@Test
	public void readMetadataAndBodyTest() throws IOException, URISyntaxException {
		String[] mbArray = blogPostProcessor.readMetadataAndBody();
		System.out.println("========== metadata ============");
		System.out.println(mbArray[0]);
		System.out.println("========== body ============");
		System.out.println(mbArray[1]);
	}

	private File getTestPostFile() {
		try {
			return new File(getClass().getResource("test-zh.txt").toURI());
		}
		catch (URISyntaxException e) {
			throw new RuntimeException("local post file failed", e);
		}
	}

	@Test
	public void processBlogFileTest() throws Exception {
		blogPostProcessor.processPostFile();
		BlogPostInfoHolder holder = blogPostProcessor.getBlogPostInfoHolder();
		System.out.println(holder);
		System.out.println(holder.getHtmlBody());
	}

	@Test
	public void generatePlainLinkTest() {
		StringBuilder str = new StringBuilder(
				"abchttp://test.com <a href=\"http://a.b.cn\">a.b.cn</a>def");
		System.out.println(str);
		blogPostProcessor.generatePlainLink(str);
		System.out.println(str);
	}

	@Test
	public void getMacroRegionListTest() throws IOException, URISyntaxException {
		StringBuilder strBuilder = new StringBuilder(FileUtils.readPackageFileAsText(
				"blogger/test-zh.txt", StandardCharsets.UTF_8));
		List<MacroRegion> mrList = blogPostProcessor.getMacroRegionList(strBuilder);
		for (MacroRegion mr : mrList) {
			System.out.println(mr);
		}
	}

}
