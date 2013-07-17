package blogger.macro.impl;

import blogger.macro.Macro;
import blogger.macro.MacroPair;

public class PlainHtmlElemMacro extends MacroPair {
	private final String htmlElemName;
	private final String START_TAG, END_TAG;

	public PlainHtmlElemMacro(String htmlElemName) {
		super();
		this.htmlElemName = htmlElemName;
		START_TAG = "{{" + htmlElemName + "}}";
		END_TAG = "{{/" + htmlElemName + "}}";
	}

	@Override
	protected Macro buildFirstMacro() {
		return Macro.newBuilder().setName(START_TAG).setPairedMacroName(END_TAG)
				.setHtml("<" + htmlElemName + ">").setRemoveNextNewline(true).build();
	}

	@Override
	protected Macro buildSecondMacro() {
		return Macro.newBuilder().setName(END_TAG).setPairedMacroName(START_TAG)
				.setHtml("</" + htmlElemName + ">").setRemovePreviousNewline(true).build();
	}

}
