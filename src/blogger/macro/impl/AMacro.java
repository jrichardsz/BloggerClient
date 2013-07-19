package blogger.macro.impl;

import java.net.URI;

import blogger.macro.Macro;
import blogger.macro.MacroPair;
import blogger.util.LogicAssert;

/**
 * This macro will output html a element.<br/>
 * Text should be put between {{a}} and {{/a}}, format could be:<br/>
 * <ul>
 * <li>{{a}}href || title{{/a}}, host will be appended automatically.</li>
 * <li>{{a}}href || title || author{{/a}}, host will be appended automatically.</li>
 * <li>{{a}}href || title || author || {{/a}}, author could be empty, host won't be appended.</li>
 * <li>{{a}}#post-anchor || title{{/a}}, host won't be appended.</li>
 * <li>{{a}}#post-anchor || title || author{{/a}}, host won't be appended.</li>
 * </ul>
 */
public class AMacro extends MacroPair {
	public static final String START_TAG = "{{a}}";
	public static final String END_TAG = "{{/a}}";

	@Override
	protected Macro buildFirstMacro() {
		return Macro.newBuilder().setName(START_TAG).setPairedMacroName(END_TAG).build();
	}

	@Override
	protected Macro buildSecondMacro() {
		return Macro.newBuilder().setName(END_TAG).setPairedMacroName(START_TAG).build();
	}

	@Override
	public String getHtml(String embededStr) throws Exception {
		LogicAssert.assertTrue(embededStr.indexOf('\n') < 0, "link text include newline, text=%s",
				embededStr);
		final String[] ss = embededStr.split("\\|\\|");
		LogicAssert.assertTrue(ss.length >= 2 || ss.length <= 4, "invalid {{a}} block: %s", embededStr);
		final String href = ss[0].trim(), title = ss[1].trim(), author = (ss.length >= 3 ? ss[2].trim()
				: null);
		StringBuilder linkText = new StringBuilder(128).append(title);
		if (author != null && !author.isEmpty()) {
			linkText.append(" - ").append("Author: ").append(author);
		}
		if (href.startsWith("#")) { // it's anchor
			return String.format("<a href=\"%s\" rel=\"nofollow\">%s</a>", href, linkText);
		}
		else {
			if (ss.length < 4) { // ignore host when it's forbidden explicitly
				linkText.append(" - ").append(new URI(href).getHost());
			}
			return String.format("<a href=\"%s\" target=\"_blank\" rel=\"nofollow\">%s</a>", href,
					linkText);
		}
	}

}
