package blogger;

import java.io.File;

import blogger.util.HtmlUtils;
import blogger.util.LogicAssert;

public class BlogPostInfoHolder {
	private final File postFile;
	private final String charsetName;

	private String title, tags, locale, uniquetoken;
	private File htmlFile;
	private String htmlBody;

	public BlogPostInfoHolder(final File postFile, final String charsetName) {
		this.postFile = postFile;
		this.charsetName = charsetName;
	}

	void setMetadata(String title, String tags, String locale) {
		if (title == null || tags == null || locale == null) {
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
		if (locale == null || locale.trim().isEmpty()) {
			this.locale = "";
			this.uniquetoken = enTitle.replace(' ', '-').replace('.', '-').toLowerCase();
		}
		else {
			this.locale = locale = locale.trim();
			this.uniquetoken = enTitle.replace(' ', '-').replace('.', '-').toLowerCase() + "-"
					+ locale.toLowerCase();
		}
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
