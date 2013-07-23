package blogger.macro.impl;

import blogger.macro.Macro;
import blogger.util.StringUtils;

public class RevMacro extends Macro {

	@Override
	public String getHtml() {
		StringBuilder str = new StringBuilder(super.getHtml());
		StringUtils.replaceFirstStr(str, "${rev}", getBlogPostProcessor().getBlogPostMetadata()
				.getLocale().equals("zh") ? "文章版本" : "Revisions");
		return str.toString();
	}

}
