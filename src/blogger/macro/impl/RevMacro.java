package blogger.macro.impl;

import blogger.BloggerClient;
import blogger.macro.Macro;
import blogger.util.StringUtils;

/**
 * This macro will output blog post revisions.<br/>
 * Usage example: <br/>
 * {{rev}}<br/>
 * 20130504 first revision<br/>
 * 20130716 second revision
 */
public class RevMacro extends Macro {

	@Override
	public String getHtml() {
		StringBuilder str = new StringBuilder(super.getHtml());
		StringUtils.replaceFirstStr(str, "${rev}", BloggerClient.getInstance().getBlogPostMetadata()
				.getLocale().equals("zh") ? "文章版本" : "Revisions");
		return str.toString();
	}

}
