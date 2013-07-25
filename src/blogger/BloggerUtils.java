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

import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonGenerator;
import com.google.api.client.json.JsonParser;
import com.google.api.services.blogger.model.Post;

public class BloggerUtils {

	public static void parseJsonFromFile(GenericJson json, File file) throws IOException {
		JsonParser jsonParser = null;
		try {
			jsonParser = BloggerAPIBuilder.getJsonFactory().createJsonParser(
					new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8")));
			jsonParser.parse(json);
		}
		finally {
			if (jsonParser != null)
				jsonParser.close();
		}
	}

	public static void serializeJsonToFile(GenericJson json, File file) throws IOException {
		File tempFile = new File(file.getParent(), file.getName() + ".tmp." + new Random().nextInt());
		JsonGenerator jsonGenerator = null;
		try {
			jsonGenerator = BloggerAPIBuilder.getJsonFactory().createJsonGenerator(
					new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile), "UTF-8")));
			jsonGenerator.serialize(json);
		}
		finally {
			if (jsonGenerator != null)
				jsonGenerator.close();
		}
		file.delete();
		if (!tempFile.renameTo(file)) {
			throw new IOException(String.format("[%s] move to [%s] failed", tempFile, file));
		}
	}

	/**
	 * get unique token from post url. i.e.
	 * input "http://zenzhong8383.blogspot.com/2013/06/install-macosx-mountain-lion-on-virtualbox-en.html",
	 * it will output "install-macosx-mountain-lion-on-virtualbox-en"
	 * @param postUrl blog post's url
	 * @return unique token
	 */
	public static String getPostUrlUniquetoken(String postUrl) {
		return postUrl.substring(postUrl.lastIndexOf('/') + 1, postUrl.lastIndexOf('.'));
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
