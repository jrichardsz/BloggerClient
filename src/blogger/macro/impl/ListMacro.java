package blogger.macro.impl;

import java.util.ArrayList;
import java.util.List;

import blogger.macro.Macro;
import blogger.macro.MacroPair;
import blogger.util.LogicAssert;
import blogger.util.StringUtils;

/**
 * This macro will output html ul/ol element, it is similar to Mediawiki format, only '*' and '#'
 * are allowed and they could be mixed.<br/>
 * Text should be put between {{list}} and {{/list}}.
 */
public class ListMacro extends MacroPair {
	public static final String START_TAG = "{{list}}";
	public static final String END_TAG = "{{/list}}";

	@Override
	protected Macro buildFirstMacro() {
		return Macro.newBuilder().setName(START_TAG).setPairedMacroName(END_TAG)
				.setRemoveNextNewline(true).build();
	}

	@Override
	protected Macro buildSecondMacro() {
		return Macro.newBuilder().setName(END_TAG).setPairedMacroName(START_TAG)
				.setRemovePreviousNewline(true).setRemoveNextNewline(true).build();
	}

	private static final class ListItemMetadata {
		private final String levelStr, bodyStr;

		public ListItemMetadata(String levelStr, String bodyStr) {
			super();
			this.levelStr = levelStr;
			this.bodyStr = bodyStr;
		}

		@Override
		public String toString() {
			return String.format("ListItemMetadata [levelStr=%s, bodyStr=%s]", levelStr, bodyStr);
		}
	}

	@Override
	public String getHtml(String embededStr) throws Exception {
		final StringBuilder result = new StringBuilder(embededStr.length() + 128);
		// get ListItemMetadata list
		final int maxLevelStrLen = 16;
		final List<ListItemMetadata> itemMetadataList = new ArrayList<>();
		for (String line : StringUtils.readTextAsLines(embededStr)) {
			char ch;
			int levelStrLen = 0;
			for (int index = 0, len = line.length(); index < len; index++) {
				ch = line.charAt(index);
				if (ch == '*' || ch == '#')
					levelStrLen++;
				else
					break;
			}
			LogicAssert.assertTrue(levelStrLen > 0, "no '*' or '#' at the starting of line, line=%s",
					line);
			LogicAssert.assertTrue(levelStrLen <= maxLevelStrLen,
					"at most %d '*' and '#' is allowed, current count=%d", maxLevelStrLen, levelStrLen);
			itemMetadataList.add(new ListItemMetadata(line.substring(0, levelStrLen), line
					.substring(levelStrLen)));
		}
		// verify metadata list style: the same length levelStr must be the same
		{
			String[] levelStrs = new String[maxLevelStrLen + 1];
			for (ListItemMetadata md : itemMetadataList) {
				String levelStr = levelStrs[md.levelStr.length()];
				if (levelStr == null)
					levelStrs[md.levelStr.length()] = md.levelStr;
				else
					LogicAssert.assertTrue(levelStr.equals(md.levelStr),
							"the same length levelStr must be the same, %s vs %s", levelStr, md.levelStr);
			}
			levelStrs = null;
		}
		//
		getListHtml(result, itemMetadataList, 0, new boolean[maxLevelStrLen + 1]);
		return result.toString();
	}

	private int getListHtml(final StringBuilder listHtml,
			final List<ListItemMetadata> itemMetadataList, final int listIndex,
			final boolean[] haveUnclosedLiOnLevels) {
		if (itemMetadataList.isEmpty()) {
			return 0;
		}
		int listItemsCnt = 0;
		final int currentLevel = itemMetadataList.get(listIndex).levelStr.length();
		listHtml.append(getListElementHtml(itemMetadataList.get(listIndex), true));
		for (int i = listIndex, len = itemMetadataList.size(); i < len;) {
			final ListItemMetadata md = itemMetadataList.get(i);
			final int level = md.levelStr.length();
			if (level == currentLevel) {
				if (haveUnclosedLiOnLevels[currentLevel]) {
					listHtml.append("</li>\n");
				}
				else {
					haveUnclosedLiOnLevels[currentLevel] = true;
				}
				listHtml.append("<li>").append(md.bodyStr);
				i++;
				listItemsCnt++;
			}
			else if (level > currentLevel) {
				LogicAssert.assertTrue(level - currentLevel == 1,
						"invalid list item: %s%s, previous level is %d", md.levelStr, md.bodyStr, currentLevel);
				final int subHeadersCnt = getListHtml(listHtml, itemMetadataList, i, haveUnclosedLiOnLevels);
				listHtml.append("</li>\n");
				haveUnclosedLiOnLevels[currentLevel] = false;
				i += subHeadersCnt;
				listItemsCnt += subHeadersCnt;
			}
			else { // level < currentLevel , sub-list-items finished
				if (haveUnclosedLiOnLevels[currentLevel]) {
					listHtml.append("</li>\n");
					haveUnclosedLiOnLevels[currentLevel] = false;
				}
				break;
			}
		}
		if (haveUnclosedLiOnLevels[currentLevel]) {
			listHtml.append("</li>\n");
			haveUnclosedLiOnLevels[currentLevel] = false;
		}
		listHtml.append(getListElementHtml(itemMetadataList.get(listIndex), false));
		return listItemsCnt;
	}

	private String getListElementHtml(final ListItemMetadata md, final boolean start) {
		final String levelStr = md.levelStr;
		switch (levelStr.charAt(levelStr.length() - 1)) {
		case '*':
			return start ? "<ul>" : "</ul>";
		case '#':
			return start ? "<ol>" : "</ol>";
		default:
			throw new RuntimeException("unknown char, levelStr=" + levelStr);
		}
	}

}
