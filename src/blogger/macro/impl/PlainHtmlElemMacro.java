package blogger.macro.impl;

import blogger.macro.Macro;
import blogger.macro.MacroPair;

public class PlainHtmlElemMacro extends MacroPair {
	private final String htmlElemName;
	private final String startTag, endTag;

	public PlainHtmlElemMacro(String htmlElemName) {
		super();
		this.htmlElemName = htmlElemName;
		startTag = "{{" + htmlElemName + "}}";
		endTag = "{{/" + htmlElemName + "}}";
	}

	@Override
	protected Macro buildFirstMacro() {
		return Macro.newBuilder().setName(startTag).setPairedMacroName(endTag)
				.setHtml("<" + htmlElemName + ">").setRemoveNextNewline(true).build();
	}

	@Override
	protected Macro buildSecondMacro() {
		return Macro.newBuilder().setName(endTag).setPairedMacroName(startTag)
				.setHtml("</" + htmlElemName + ">").setRemovePreviousNewline(true).build();
	}

}
