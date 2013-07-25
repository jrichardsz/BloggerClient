package blogger;

import org.junit.Assert;
import org.junit.Test;

public class BloggerUtilsTest {

	@Test
	public void getPostUrlUniquetokenTest() {
		String postUrl = "http://zenzhong8383.blogspot.com/2013/06/install-macosx-mountain-lion-on-virtualbox-en.html";
		String expected = "install-macosx-mountain-lion-on-virtualbox-en";
		String actual = BloggerUtils.getPostUrlUniquetoken(postUrl);
		Assert.assertEquals(expected, actual);
	}

}
