package blogger.macro.impl;

import java.io.BufferedReader;
import java.io.StringReader;

import blogger.macro.MacroPair;
import blogger.macro.Macro;
import blogger.util.LogicAssert;

/**
 * This macro will output html table element from input.<br/>
 * Text should be put between {{table}} and {{/table}}.
 */
public class TableMacro extends MacroPair {
	public static final String START_TAG = "{{table}}";
	public static final String END_TAG = "{{/table}}";

	@Override
	protected Macro buildFirstMacro() {
		return Macro.newBuilder().setName(START_TAG).setPairedMacroName(END_TAG)
				.setRemoveNextNewline(true).setCssRelativePaths("css/plain-table.css").build();
	}

	@Override
	protected Macro buildSecondMacro() {
		return Macro.newBuilder().setName(END_TAG).setPairedMacroName(START_TAG)
				.setRemovePreviousNewline(true).setRemoveNextNewline(true).build();
	}

	/**
	 * embededStr format: <br/>
	 * column1 || column2<br/>
	 * value1.1 || value1.2<br/>
	 * value2.1 || value2.2<br/>
	 */
	@Override
	public String getHtml(String embededStr) throws Exception {
		//TODO reverse '|' and '\' by '\'
		LogicAssert.assertTrue(embededStr.trim().length() > 0, "empty table content");
		final StringBuilder result = new StringBuilder(embededStr.length());
		result.append("<table class=\"plain-table\">");
		BufferedReader br = new BufferedReader(new StringReader(embededStr));
		String line;
		int columnCnt = -1;
		while ((line = br.readLine()) != null) {
			final String[] ss = line.split("\\|\\|");
			if (columnCnt > 0)
				LogicAssert.assertTrue(columnCnt == ss.length,
						"column count doesn't match, current line=%s, cnt=%d", line, ss.length);
			result.append("<tr>");
			for (String s : ss) {
				result.append("<td>").append(s.trim()).append("</td>");
			}
			result.append("</tr>");
		}
		br.close();
		result.append("</table>\n");
		return result.toString();
	}

}
