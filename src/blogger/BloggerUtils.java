package blogger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import blogger.util.LogicAssert;

import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonGenerator;
import com.google.api.client.json.JsonParser;
import com.google.api.services.blogger.model.Post;

public class BloggerUtils {

	public static void parseJsonFromFile(File file, GenericJson dest) throws IOException {
		JsonParser jsonParser = null;
		try {
			jsonParser = BloggerAPIBuilder.getJsonFactory().createJsonParser(
					new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8")));
			jsonParser.parse(dest);
		}
		finally {
			if (jsonParser != null)
				jsonParser.close();
		}
	}

	public static void serializeJsonToFile(GenericJson src, File file) throws IOException {
		File tempFile = new File(file.getParent(), file.getName() + ".tmp." + new Random().nextInt());
		JsonGenerator jsonGenerator = null;
		try {
			jsonGenerator = BloggerAPIBuilder.getJsonFactory().createJsonGenerator(
					new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile), "UTF-8")));
			jsonGenerator.serialize(src);
		}
		finally {
			if (jsonGenerator != null)
				jsonGenerator.close();
		}
		file.delete();
		if (!tempFile.renameTo(file)) {
			LogicAssert.assertTrue(false, "[%s] move to [%s] failed", tempFile, file);
		}
	}

	public static String getPostUrlUniquetoken(String postUrl) {
		//i.e. http://zenzhong8383.blogspot.com/2013/06/install-macosx-mountain-lion-on-virtualbox-en.html
		// unique token is: install-macosx-mountain-lion-on-virtualbox-en
		final int lastSlashIndex = postUrl.lastIndexOf('/'), lastDotIndex = postUrl.lastIndexOf('.');
		LogicAssert.assertTrue(lastSlashIndex > 0 && lastDotIndex > 0, "invalid index, postUrl=%s",
				postUrl);
		return postUrl.substring(lastSlashIndex + 1, lastDotIndex);
	}

	public static Post getPostObjForCreate(String blogUrl, BlogPostMetadata metadata) {
		Post post = new Post();
		post.setTitle(metadata.getTitle());
		post.setContent(metadata.getHtmlBody());
		// labels
		{
			List<String> labels = new ArrayList<>();
			for (String label : metadata.getTags().split(",")) {
				label = label.trim();
				if (!label.isEmpty()) {
					labels.add(label);
				}
			}
			post.setLabels(labels);
		}
		// url
		{
			StringBuilder url = new StringBuilder(128);
			url.append(blogUrl);
			if (!blogUrl.endsWith("/")) {
				url.append('/');
			}
			url.append(new SimpleDateFormat("yyyy/MM/").format(System.currentTimeMillis()));
			url.append(metadata.getUniquetoken());
			url.append(".html");
			post.setUrl(url.toString());
		}
		return post;
	}

	public static Post getPostObjForUpdate(BlogPostMetadata metadata) {
		Post post = new Post();
		post.setTitle(metadata.getTitle());
		post.setContent(metadata.getHtmlBody());
		// labels
		{
			List<String> labels = new ArrayList<>();
			for (String label : metadata.getTags().split(",")) {
				label = label.trim();
				if (!label.isEmpty()) {
					labels.add(label);
				}
			}
			post.setLabels(labels);
		}
		return post;
	}

}
