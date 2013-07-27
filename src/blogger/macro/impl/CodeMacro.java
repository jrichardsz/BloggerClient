package blogger.macro.impl;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import blogger.assets.AssetsLoader;
import blogger.macro.MacroPair;
import blogger.macro.Macro;
import blogger.util.StringUtils;

/**
 * This macro will output html code element.<br/>
 * Text should be put between {{code}} and {{/code}}.
 */
public class CodeMacro extends MacroPair {
	public static final String START_TAG = "{{code}}";
	public static final String END_TAG = "{{/code}}";

	@Override
	protected Macro buildFirstMacro() {
		return Macro.newBuilder().setName(START_TAG).setPairedMacroName(END_TAG)
				.setRemoveNextNewline(true).setCssRelativePaths("css/code.css")
				.setJsRelativePaths("js/showhide.js, js/wrapline-textarea.js").build();
	}

	@Override
	protected Macro buildSecondMacro() {
		return Macro.newBuilder().setName(END_TAG).setPairedMacroName(START_TAG)
				.setRemovePreviousNewline(true).setRemoveNextNewline(true).build();
	}

	@Override
	public String getHtml(String embededStr) throws Exception {
		// ref http://stackoverflow.com/questions/657795/how-remove-wordwrap-from-textarea
		int rows = 0;
		Matcher m = Pattern.compile("\\n").matcher(embededStr);
		int end = 0;
		while (m.find(end)) {
			end = m.end();
			++rows;
		}
		rows += 1; // there's one line even no new line
		StringBuilder result = new StringBuilder(
				AssetsLoader.readAssetAsText("html/code-textarea-head.html"));
		StringUtils.replaceFirst(result, "${rows}", rows + "");
		final long uniqueId = new Random().nextInt(Integer.MAX_VALUE);
		StringUtils.replaceAll(result, "${code-span-id}", "bc-code-span-" + uniqueId);
		StringUtils.replaceAll(result, "${wrapline-span-id}", "bc-wrapline-span-" + uniqueId);
		StringUtils.replaceAll(result, "${code-textarea-id}", "bc-code-textarea-" + uniqueId);
		result.append(embededStr);
		result.append("</textarea></div>");
		return result.toString();
	}

}
