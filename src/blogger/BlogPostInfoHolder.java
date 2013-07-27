package blogger;

import java.io.File;
import java.util.regex.Pattern;

import blogger.util.LogicAssert;

/**
 * It's *not* thread-safe, but it could be modified in different threads at different time.
 */
public class BlogPostInfoHolder {

	private volatile String title, tags, locale, uniquetoken, body;
	/** indicate whether the uniquetoken be from server or not */
	private volatile boolean isServerUniquetoken;

	private volatile File htmlFile;
	/** processed post file's body, it's html */
	private volatile String htmlBody;

	BlogPostInfoHolder() {
	}

	void setMetadata(String title, String tags, String locale, String serverUniquetoken) {
		if (title == null) { // tags/locale could be null
			throw new IllegalArgumentException("title can't be null");
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
		this.tags = (tags == null ? "" : tags.trim());
		this.locale = (locale == null ? "" : locale.trim());
		isServerUniquetoken = (serverUniquetoken != null && !serverUniquetoken.trim().isEmpty());
		this.uniquetoken = (isServerUniquetoken ? serverUniquetoken.trim() : enTitle.replace(' ', '-')
				.replace('.', '-').replace('_', '-').toLowerCase());
		// verify when uniquetoken is user defined
		if (!isServerUniquetoken) {
			LogicAssert.assertTrue(uniquetoken.indexOf("--") < 0,
					"invalid, -- is not allowed, uniquetoken=%s", uniquetoken); // verify blanks by mistake
			LogicAssert.assertTrue(UNIQUETOKEN_PATTERN.matcher(uniquetoken).matches(),
					"invalid, allowed character is [A-Za-z0-9 ._-], uniquetoken=%s", uniquetoken);
		}
	}

	private static final Pattern UNIQUETOKEN_PATTERN = Pattern
			.compile("^[a-z0-9][a-z0-9-]*[a-z0-9]$");

	/**
	 * @return real title, not include permalink related text
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * it will be separated by ',' if there're more than one tag
	 * 
	 * @return tags or empty string
	 */
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

	void setUniquetoken(String serverUniquetoken) {
		// unique token could be over-written
		this.uniquetoken = serverUniquetoken;
		isServerUniquetoken = true;
	}

	/** indicate whether the uniquetoken be from server or not */
	public boolean isServerUniquetoken() {
		return isServerUniquetoken;
	}

	String getBody() {
		return body;
	}

	void setBody(String body) {
		LogicAssert.assertTrue(body != null && this.body == null, "");
		this.body = body;
	}

	public File getHtmlFile() {
		return htmlFile;
	}

	public void setHtmlFile(File htmlFile) {
		LogicAssert.assertTrue(htmlFile != null && this.htmlFile == null, "");
		this.htmlFile = htmlFile;
	}

	public String getHtmlBody() {
		return htmlBody;
	}

	void setHtmlBody(String htmlBody) {
		LogicAssert.assertTrue(htmlBody != null && this.htmlBody == null, "");
		this.htmlBody = htmlBody;
	}

	@Override
	public String toString() {
		return String
				.format(
						"BlogPostInfoHolder [title=%s, tags=%s, locale=%s, uniquetoken=%s, isServerUniquetoken=%s, htmlFile=%s]",
						title, tags, locale, uniquetoken, isServerUniquetoken, htmlFile);
	}

}
