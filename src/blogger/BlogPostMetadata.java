package blogger;

import java.io.File;

import blogger.util.HtmlUtils;
import blogger.util.LogicAssert;

public class BlogPostMetadata {
	private final String title, tags, locale, uniquetoken;
	private File htmlFile;
	private String htmlBody;

	public BlogPostMetadata(String title, String tags, String locale) {
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
		LogicAssert.assertTrue(locale.length() > 0, "locale is not set");
		this.locale = locale;
		this.uniquetoken = enTitle.replace(' ', '-').replace('.', '-').toLowerCase() + "-"
				+ locale.toLowerCase();
		// in order to verify blanks by mistake
		LogicAssert.assertTrue(uniquetoken.indexOf("--") < 0, "invalid uniquetoken=%s", uniquetoken);
		HtmlUtils.verifyHtmlElementId(uniquetoken);
	}

	public String getTitle() {
		return title;
	}

	public String getTags() {
		return tags;
	}

	public String getLocale() {
		return locale;
	}

	public String getUniquetoken() {
		return uniquetoken;
	}

	File getHtmlFile() {
		return htmlFile;
	}

	void setHtmlFile(File htmlFile) {
		this.htmlFile = htmlFile;
	}

	String getHtmlBody() {
		return htmlBody;
	}

	void setHtmlBody(String htmlBody) {
		this.htmlBody = htmlBody;
	}

	@Override
	public String toString() {
		return String.format(
				"BlogPostMetadata [title=%s, tags=%s, locale=%s, uniquetoken=%s, blogHtmlFile=%s]", title,
				tags, locale, uniquetoken, htmlFile);
	}

}
