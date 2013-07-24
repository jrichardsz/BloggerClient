package blogger;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.google.api.services.blogger.model.Post;

public class BloggerUtilsTest {

	@Test
	public void getPostUrlUniquetokenTest() {
		String postUrl = "http://zenzhong8383.blogspot.com/2013/06/install-macosx-mountain-lion-on-virtualbox-en.html";
		String expected = "install-macosx-mountain-lion-on-virtualbox-en";
		String actual = BloggerUtils.getPostUrlUniquetoken(postUrl);
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void getPostObjForCreateTest() throws IOException {
		String blogUrl = "http://zenzhong8383.blogspot.com/";
		BlogPostMetadata metadata = new BlogPostMetadata("title 1", "en, Java", "en");
		Post post1 = BloggerUtils.getPostObjForCreate(blogUrl, metadata);
		System.out.println(post1.toPrettyString());
		blogUrl = "http://zenzhong8383.blogspot.com";
		Post post2 = BloggerUtils.getPostObjForCreate(blogUrl, metadata);
		System.out.println(post2.toPrettyString());
		Assert.assertEquals(post1.getUrl(), post2.getUrl());
	}

}
