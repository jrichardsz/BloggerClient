package blogger.macro.impl;

import blogger.macro.Macro;
import blogger.macro.MacroPair;

/**
 * This macro will output html code from input.<br/>
 * Text should be put between {{html}} and {{/html}}, text should be pure html code, nested macros
 * are not allowed.
 */
public class HtmlMacro extends MacroPair {
	public static final String START_TAG = "{{html}}";
	public static final String END_TAG = "{{/html}}";

	@Override
	protected Macro buildFirstMacro() {
		return Macro.newBuilder().setName(START_TAG).setPairedMacroName(END_TAG).setHtml("<div>")
				.setRemoveNextNewline(true).build();
	}

	@Override
	protected Macro buildSecondMacro() {
		return Macro.newBuilder().setName(END_TAG).setPairedMacroName(START_TAG).setHtml("</div>")
				.setRemovePreviousNewline(true).setRemoveNextNewline(true).build();
	}

}
