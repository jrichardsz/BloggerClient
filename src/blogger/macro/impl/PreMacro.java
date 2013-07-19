package blogger.macro.impl;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import blogger.macro.MacroPair;
import blogger.macro.Macro;
import blogger.util.StringUtils;

/**
 * This macro will output html pre element from input.<br/>
 * Text should be put between {{pre}} and {{/pre}}.
 */
public class PreMacro extends MacroPair {
	public static final String START_TAG = "{{pre}}";
	public static final String END_TAG = "{{/pre}}";

	@Override
	protected Macro buildFirstMacro() {
		return Macro.newBuilder().setName(START_TAG).setPairedMacroName(END_TAG)
				.setRemoveNextNewline(true).setCssRelativePaths("css/code.css")
				.setJsRelativePaths("js/showhide.js").build();
	}

	@Override
	protected Macro buildSecondMacro() {
		return Macro.newBuilder().setName(END_TAG).setPairedMacroName(START_TAG)
				.setRemovePreviousNewline(true).setRemoveNextNewline(true).build();
	}

	@Override
	public String getHtml(String embededStr) throws Exception {
		int rows = 0;
		Matcher m = Pattern.compile("\\n").matcher(embededStr);
		int end = 0;
		while (m.find(end)) {
			end = m.end();
			++rows;
		}
		rows += 1; // there's one line even no new line
		StringBuilder result = new StringBuilder();
		if (rows > 15) {
			result.append("<div class=\"bc-code-outer-div\">\n");
			result
					.append("<div class=\"bc-code-inner-div\"><span>Pre</span><a href=\"#\" onclick=\"return false;\"><span id=\"${code-span-id}\" onclick=\"bc_showhide('${code-pre-id}', '${code-span-id}');\">[-]</span></a></div>\n");
			result.append("<pre id=\"${code-pre-id}\" class=\"bc-code-inner-pre\">\n");
			long uniqueId = new Random().nextInt(Integer.MAX_VALUE);
			StringUtils.replaceAllStr(result, "${code-span-id}", "bc-code-span-" + uniqueId);
			StringUtils.replaceAllStr(result, "${code-pre-id}", "bc-code-pre-" + uniqueId);
			result.append(embededStr);
			result.append("</pre>\n");
			result.append("</div>\n");
		}
		else {
			result.append("<pre class=\"bc-code-pre\">\n");
			result.append(embededStr);
			result.append("</pre>\n");
		}
		return result.toString();
	}

}
