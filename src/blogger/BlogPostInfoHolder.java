package blogger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Random;

import blogger.util.HtmlUtils;
import blogger.util.LogicAssert;

public class BlogPostInfoHolder {
	private final File postFile;
	private final String charsetName;

	private String title, tags, locale, uniquetoken;
	/**
	 * post file body, just after "&lt;/metadata&gt;", include the next newline
	 */
	private String body;

	private File htmlFile;
	/** processed post file's body, it's html */
	private String htmlBody;

	public BlogPostInfoHolder(final File postFile, final String charsetName) {
		this.postFile = postFile;
		this.charsetName = charsetName;
	}

	void setMetadata(String title, String tags, String locale) {
		if (title == null || tags == null) {
			throw new IllegalArgumentException("metadata can't be null");
		}
		if (this.title != null || this.tags != null || this.locale != null) {
			throw new IllegalStateException("metadata was setted before");
		}
		String enTitle;
		if (title.indexOf("||") >= 0) {
			String[] ss = title.split("\\|\\|");
			LogicAssert.assertTrue(ss.length == 2, "invalid title=%s", title);
			this.title = ss[0].trim();
			enTitle = ss[1].trim();
		}
		else {
			this.title = enTitle = title;
		}
		this.tags = tags;
		this.locale = (locale == null ? "" : locale.trim());
		this.uniquetoken = enTitle.replace(' ', '-').replace('.', '-').toLowerCase();
		// in order to verify blanks by mistake
		LogicAssert.assertTrue(uniquetoken.indexOf("--") < 0, "invalid uniquetoken=%s", uniquetoken);
		HtmlUtils.verifyHtmlElementId(uniquetoken);
	}

	public File getPostFile() {
		return postFile;
	}

	public String getCharsetName() {
		return charsetName;
	}

	/**
	 * @return real title, not include permalink related text
	 */
	public String getTitle() {
		return title;
	}

	public String getTags() {
		return tags;
	}

	/**
	 * @return locale or empty string
	 */
	public String getLocale() {
		return locale;
	}

	public String getUniquetoken() {
		return uniquetoken;
	}

	void setBody(String body) {
		this.body = body;
	}

	public File getHtmlFile() {
		return htmlFile;
	}

	void setHtmlFile(File htmlFile) {
		this.htmlFile = htmlFile;
	}

	public String getHtmlBody() {
		return htmlBody;
	}

	void setHtmlBody(String htmlBody) {
		this.htmlBody = htmlBody;
	}

	/**
	 * update new unique token, serialize to post file, so we'll get the same unique token at next
	 * time
	 */
	public void updateNewUniquetokenAndSerialize(String newUniquetoken) throws IOException {
		final File file = postFile;
		final File tempFile = new File(file.getParent(), file.getName() + ".tmp."
				+ new Random().nextInt());
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
				tempFile), getCharsetName()))) {
			writer.newLine(); // write first line, keep it empty
			writer.write(BlogPostProcessor.metadataStartTag);
			writer.newLine();
			writer.write("title = ");
			writer.write(title);
			writer.write(" || ");
			writer.write(newUniquetoken);
			writer.newLine();
			writer.write("tags = ");
			writer.write(tags);
			writer.newLine();
			if (!locale.isEmpty()) {
				writer.write("locale = ");
				writer.write(locale);
				writer.newLine();
			}
			writer.write(BlogPostProcessor.metadataEndTag);
			writer.write(body);
		}
		file.delete();
		if (!tempFile.renameTo(file)) {
			throw new IOException(String.format("move [%s] to [%s] failed", tempFile, file));
		}
		this.uniquetoken = newUniquetoken;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BlogPostInfoHolder [postFile=").append(postFile).append(", charsetName=")
				.append(charsetName).append(", title=").append(title).append(", tags=").append(tags)
				.append(", locale=").append(locale).append(", uniquetoken=").append(uniquetoken)
				.append("]");
		return builder.toString();
	}

}
