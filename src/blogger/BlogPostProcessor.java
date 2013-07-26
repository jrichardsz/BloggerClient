package blogger;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import blogger.assets.AssetsLoader;
import blogger.macro.Macro;
import blogger.macro.MacroFactory;
import blogger.macro.impl.HtmlMacro;
import blogger.util.FileUtils;
import blogger.util.HtmlUtils;
import blogger.util.LogicAssert;
import blogger.util.PureStack;
import blogger.util.StringUtils;

public class BlogPostProcessor {
	private final File postFile;
	private final String charsetName;

	private final BlogPostInfoHolder blogPostInfoHolder;

	public BlogPostProcessor(final File postFile, final String charsetName) {
		this.postFile = postFile;
		this.charsetName = charsetName;
		this.blogPostInfoHolder = new BlogPostInfoHolder();
	}

	public BlogPostInfoHolder getBlogPostInfoHolder() {
		return blogPostInfoHolder;
	}

	public BlogPostInfoHolder processPostFile() throws Exception {
		System.out.println(String.format("processPostFile> file=%s", postFile));
		String[] mbArray = readMetadataAndBody();
		parseBlogPostMetadata(mbArray[0]);
		final StringBuilder body = new StringBuilder(mbArray[1]);
		mbArray = null;
		blogPostInfoHolder.setBody(body.toString());
		StringUtils.replaceAllStr(body, "\r\n", "\n");
		StringUtils.replaceAllStr(body, "\r", "\n");
		removeStartingEndingNewlines(body);
		StringUtils.replaceCharToStr(body, '\t', "    "); // 4 white spaces
		final ParsingContext parsingContext = new ParsingContext();
		processPostFile0(body, parsingContext);
		expandSpecialMacros(body);

		// add TOC to the beginning
		if (!parsingContext.notoc) {
			parsingContext.assetRelativePathSet.add("css/toc.css");
			final List<BlogPostHeaderMetadata> headerMetadataList = parsingContext.headerMetadataList;
			if (headerMetadataList.size() > 0) {
				parsingContext.assetRelativePathSet.add("css/headers.css");
			}
			final StringBuilder toc = new StringBuilder(1024);
			int headersCnt = generateTOC(toc, headerMetadataList, -1, new boolean[7]);
			LogicAssert.assertTrue(headersCnt == headerMetadataList.size(),
					"headersCnt (%d) != headerMetadataList.size (%d)", headersCnt, headerMetadataList.size());
			body.insert(0, toc);
		}

		// insert blogger more anchor (<!--more-->) in blog list page to hide TOC.
		// in order to prevent html elements' id conflict,
		// and also prevent relative anchor link be shown, else it'll link to error page.
		// there're only links with title in blog list page.
		body.insert(0, MacroFactory.READMORE.getHtml());

		for (String assetRelativePath : parsingContext.assetRelativePathSet) {
			body.insert(0, AssetsLoader.readAssetAsText(assetRelativePath));
		}
		File htmlFile = writeToPostHtmlFile(body, postFile.getParent(), postFile.getName(), charsetName);
		htmlFile.deleteOnExit();
		blogPostInfoHolder.setHtmlFile(htmlFile);
		blogPostInfoHolder.setHtmlBody(body.toString());
		return blogPostInfoHolder;
	}

	private static final String METADATA_START_TAG = "<metadata>";
	private static final String METADATA_END_TAG = "</metadata>";

	/**
	 * take care when changing the implementation, see
	 * {@link #updateNewUniquetokenAndSerialize(String)}
	 * 
	 * @return metadata-body array
	 */
	String[] readMetadataAndBody() throws IOException {
		final String postFileText = FileUtils.readFileAsText(postFile, charsetName);
		final StringBuilder metadata = new StringBuilder(1024);
		final int beforeMetadata = 1, inMetadata = 2, afterMetadata = 3;
		int state = beforeMetadata;
		int readLen = 0;
		try (BufferedReader bufferedReader = new BufferedReader(new StringReader(postFileText))) {
			String line;
			line = bufferedReader.readLine(); // read first line and ignore
			readLen = countNewReadLen(postFileText, readLen + line.length());
			LogicAssert.assertTrue(line != null, "file is empty");
			loop: while ((line = bufferedReader.readLine()) != null) {
				readLen = countNewReadLen(postFileText, readLen + line.length());
				switch (state) {
				case beforeMetadata:
					LogicAssert.assertTrue(line.equals(METADATA_START_TAG),
							"%s not found at the second line, line=%s", METADATA_START_TAG, line);
					state = inMetadata;
					break;
				case inMetadata:
					if (!line.equals(METADATA_END_TAG)) {
						metadata.append(line).append('\n');
						break;
					}
					else {
						state = afterMetadata;
						break loop;
					}
				default:
					LogicAssert.assertTrue(false, "unknown state=" + state);
				}
			}
		}
		LogicAssert.assertTrue(state == afterMetadata, "%s not found, state=%d", METADATA_END_TAG,
				state);
		return new String[] { metadata.toString(),
				// post file body, start from newline after "</metadata>"
				postFileText.substring(readLen) };
	}

	private int countNewReadLen(String postFileText, int readLen) {
		if (postFileText.length() > readLen) {
			char ch = postFileText.charAt(readLen);
			if (ch == '\r') {
				readLen++;
				if (postFileText.length() > readLen + 1) {
					ch = postFileText.charAt(readLen + 1);
					if (ch == '\n')
						readLen++;
				}
			}
			else if (ch == '\n')
				readLen++;
		}
		return readLen;
	}

	/**
	 * update new unique token, serialize to post file, so we'll get the same unique token at next
	 * time
	 */
	public void updateNewUniquetokenAndSerialize(String newUniquetoken) throws IOException {
		final File file = postFile;
		final File tempFile = new File(file.getParent(), file.getName() + ".tmp."
				+ new Random().nextInt());
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
				tempFile), charsetName))) {
			writer.newLine(); // write first line, keep it empty
			writer.write(METADATA_START_TAG);
			writer.newLine();
			writer.write("title = ");
			writer.write(blogPostInfoHolder.getTitle());
			writer.write(" || ");
			writer.write(newUniquetoken);
			writer.newLine();
			writer.write("tags = ");
			writer.write(blogPostInfoHolder.getTags());
			writer.newLine();
			if (!blogPostInfoHolder.getLocale().isEmpty()) {
				writer.write("locale = ");
				writer.write(blogPostInfoHolder.getLocale());
				writer.newLine();
			}
			writer.write(METADATA_END_TAG);
			writer.newLine();
			writer.write(blogPostInfoHolder.getBody());
		}
		file.delete();
		if (!tempFile.renameTo(file)) {
			throw new IOException(String.format("move [%s] to [%s] failed", tempFile, file));
		}
		blogPostInfoHolder.setUniquetoken(newUniquetoken);
	}

	/**
	 * just split properties text
	 */
	private void parseBlogPostMetadata(final String metadataStr) throws IOException {
		Properties props = new Properties();
		props.load(new StringReader(metadataStr));
		blogPostInfoHolder.setMetadata(props.getProperty("title"), props.getProperty("tags"),
				props.getProperty("locale"));
	}

	private void removeStartingEndingNewlines(final StringBuilder body) {
		int cnt = 0, len = body.length();
		// starting
		for (int index = 0; index < len; index++) {
			if (body.charAt(index) == '\n')
				cnt++;
			else
				break;
		}
		if (cnt > 0)
			body.delete(0, cnt);
		// ending
		cnt = 0;
		len = body.length();
		for (int index = len - 1; index >= 0; index--) {
			if (body.charAt(index) == '\n')
				cnt++;
			else
				break;
		}
		if (cnt > 0)
			body.delete(len - cnt, len);
	}

	private static final class ParsingContext {
		final List<BlogPostHeaderMetadata> headerMetadataList = new ArrayList<>();
		/**
		 * asset relative path must be unique, so HashSet is required
		 */
		final HashSet<String> assetRelativePathSet = new HashSet<>(1024, 0.25F);
		boolean notoc = false;
		int processingNestedMacrosDepth = 0;
	}

	private void processPostFile0(final StringBuilder body, final ParsingContext parsingContext)
			throws Exception {
		// replaceHtmlEntities for text that is outside of {{html}} and {{/html}}
		{
			List<MacroRegion> macroRegionList = getMacroRegionList(body);
			MacroRegion previousMacroRegion = new MacroRegion("", body.length() - 1, -1, -1);
			for (int index = macroRegionList.size() - 1; index >= 0;) {
				final MacroRegion macroRegion = macroRegionList.get(index);
				if (macroRegion.macroName.equals(HtmlMacro.END_TAG)) {
					HtmlUtils.replaceHtmlEntities(body, macroRegion.end, previousMacroRegion.start);
					previousMacroRegion = macroRegionList.get(macroRegion.pairedMacroRegionIndex);
					index -= (macroRegion.index - macroRegion.pairedMacroRegionIndex);
				}
				else {
					index--;
				}
			}
			if (previousMacroRegion.start > 0) {
				HtmlUtils.replaceHtmlEntities(body, 0, previousMacroRegion.start);
			}
			macroRegionList.clear();
			previousMacroRegion = null;
			macroRegionList = null;
		}
		final List<MacroRegion> macroRegionList = getMacroRegionList(body);
		StringBuilder newBody = processPostFile00(body, macroRegionList, 0, macroRegionList.size() - 1,
				parsingContext);
		body.setLength(0);
		body.append(newBody);
	}

	private StringBuilder processPostFile00(final StringBuilder body,
			final List<MacroRegion> macroRegionList, final int listStartIndex, final int listEndIndex,
			final ParsingContext parsingContext) throws Exception {
		final StringBuilder newBody = new StringBuilder(listEndIndex == 0 ? body.length() : 128);
		final boolean processingNestedMacros = parsingContext.processingNestedMacrosDepth > 0;
		final int startIndex = processingNestedMacros ? listStartIndex + 1 : listStartIndex;
		final int endIndex = processingNestedMacros ? listEndIndex - 1 : listEndIndex;
		final int textStart = processingNestedMacros ? macroRegionList.get(listStartIndex).end : 0;
		final int textEnd = processingNestedMacros ? macroRegionList.get(listEndIndex).start : body
				.length(); // textEnd - textStart == textLen
		MacroRegion previousMacroRegion = new MacroRegion("", textStart, textStart, -1);
		for (int index = startIndex; index <= endIndex;) {
			final MacroRegion macroRegion = macroRegionList.get(index);
			final Macro macro = MacroFactory.getMacro(macroRegion.macroName);
			macro.injectBlogPostProcessor(this);
			injectAssetRelativePaths(macro.getCssRelativePaths(), parsingContext);
			injectAssetRelativePaths(macro.getJsRelativePaths(), parsingContext);
			newBody.append(processMacroOuterRegion(
					body.substring(previousMacroRegion.end, macroRegion.start), parsingContext));
			if (macroRegion.pairedMacroRegionIndex < 0) { // no paired macro
				if (macro.getName().equals(MacroFactory.NOTOC.getName())) {
					parsingContext.notoc = true;
				}
				newBody.append(macro.getHtml());
				index++;
				previousMacroRegion = macroRegion;
			}
			else { // has paired macro
				MacroRegion pairedMacroRegion = macroRegionList.get(macroRegion.pairedMacroRegionIndex);
				if (macroRegion.pairedMacroRegionIndex - macroRegion.index == 1) { // no nested macros
					newBody.append(MacroFactory.getMacroPair(macro.getName()).getHtml(
							body.substring(macroRegion.end, pairedMacroRegion.start)));
				}
				else { // has nested macros
					parsingContext.processingNestedMacrosDepth += 1;
					StringBuilder embededStr = processPostFile00(body, macroRegionList, macroRegion.index,
							macroRegion.pairedMacroRegionIndex, parsingContext);
					parsingContext.processingNestedMacrosDepth -= 1;
					newBody.append(MacroFactory.getMacroPair(macro.getName()).getHtml(embededStr.toString()));
				}
				index += (macroRegion.pairedMacroRegionIndex - macroRegion.index + 1);
				previousMacroRegion = pairedMacroRegion;
			}
			macro.injectBlogPostProcessor(null);
		}
		// handle left end region
		if (previousMacroRegion.end < textEnd) {
			newBody.append(processMacroOuterRegion(body.substring(previousMacroRegion.end, textEnd),
					parsingContext));
		}
		return newBody;
	}

	List<MacroRegion> getMacroRegionList(StringBuilder body) {
		final int bodyLen = body.length();
		final List<MacroRegion> macroRegionList = new ArrayList<>();
		final List<StrRegion> strRegionList = new ArrayList<>();
		{
			Matcher matcher = Macro.MACRO_PATTERN.matcher(body);
			while (matcher.find()) {
				int start = matcher.start(), end = matcher.end();
				strRegionList.add(new StrRegion(body.substring(start, end), start, end));
			}
			matcher = null;
		}
		final PureStack<MacroRegion> macroRegionStack = new PureStack<>();
		for (int index = 0, len = strRegionList.size(); index < len; index++) {
			final StrRegion strRegion = strRegionList.get(index);
			final Macro macro = MacroFactory.getMacro(strRegion.str);
			LogicAssert.assertTrue(macro != null, "can't find macro, region=%s", strRegion);
			int start = strRegion.start, end = strRegion.end;
			start = macro.isRemovePreviousNewline() && start > 0 && body.charAt(start - 1) == '\n' ? start - 1
					: start;
			end = macro.isRemoveNextNewline() && end < bodyLen && body.charAt(end) == '\n' ? end + 1
					: end;
			if (macro.getPairedMacroName() == null) { // single macro
				macroRegionList.add(new MacroRegion(strRegion.str, start, end, index));
			}
			else { // paried macro
				MacroRegion macroRegion = new MacroRegion(strRegion.str, start, end, index);
				macroRegionList.add(macroRegion);
				if (macro.getName().indexOf('/') < 0) { // 1st macro of pair
					macroRegionStack.push(macroRegion);
				}
				else { // 2nd macro of pair
					MacroRegion pairedMacroRegion = macroRegionStack.pop();
					LogicAssert.assertTrue(pairedMacroRegion != null, "no paired macro region, region=%s",
							strRegion);
					LogicAssert.assertTrue(
							macro.getPairedMacroName().equals(pairedMacroRegion.macroName),
							"no paired macro, %s, %s, %s",
							macroRegion.macroName,
							pairedMacroRegion.macroName,
							body.substring(pairedMacroRegion.end,
									Math.min(macroRegion.start, pairedMacroRegion.end + 100)));
					if (macroRegion.macroName.equals(HtmlMacro.END_TAG)) {
						LogicAssert.assertTrue(macroRegion.index - pairedMacroRegion.index == 1,
								"{{html}} doesn't allow nested macros");
					}
					pairedMacroRegion.pairedMacroRegionIndex = macroRegion.index;
					macroRegion.pairedMacroRegionIndex = pairedMacroRegion.index;
				}
			}
		}
		return macroRegionList;
	}

	static final class MacroRegion {
		final String macroName;
		/**
		 * The beginning index, inclusive.<br/>
		 * It may be changed to another value.
		 */
		final int start;
		/**
		 * The ending index, exclusive.<br/>
		 * It may be changed to another value.
		 */
		final int end;
		/**
		 * Index in region list. Start from 0.
		 */
		final int index;
		/**
		 * -1 means no paired macro region
		 */
		int pairedMacroRegionIndex = -1;

		public MacroRegion(String macroName, int start, int end, int index) {
			super();
			this.macroName = macroName;
			this.start = start;
			this.end = end;
			this.index = index;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("MacroRegion [macroName=").append(macroName).append(", start=").append(start)
					.append(", end=").append(end).append(", index=").append(index)
					.append(", pairedMacroRegionIndex=").append(pairedMacroRegionIndex).append("]");
			return builder.toString();
		}
	}

	private void injectAssetRelativePaths(String assetRelativePaths, ParsingContext parsingContext) {
		if (assetRelativePaths.isEmpty()) {
			return;
		}
		String[] sa = assetRelativePaths.split(Macro.assetRelativePathsSeparator);
		String s;
		for (int i = 0, len = sa.length; i < len; i++) {
			s = sa[i].trim();
			if (s.length() > 0) {
				parsingContext.assetRelativePathSet.add(s);
			}
		}
	}

	String processMacroOuterRegion(final String str, final ParsingContext parsingContext)
			throws IOException {
		if (parsingContext.processingNestedMacrosDepth > 0) {
			// do not handle nested outer region
			return str;
		}
		final StringBuilder result = new StringBuilder(str);
		generatePlainLink(result);
		generateSpaceBrAndAnchor(result, parsingContext);
		return result.toString();
	}

	private static final Pattern HTTP_URL_PATTERN;
	static {
		String regex = "" // HTTP URL regex
				+ "(?:http(?:s)?://)" // schema
				+ "(?:[a-zA-Z0-9.-]+)" // host
				+ "(?::[0-9]+)?" // port
				+ "(?:" // start of (abs_path+query)
				+ "/(?:(?:[a-zA-Z0-9-_.!~*'()]+|(?:%[a-fA-F0-9]{2})+|[:@&=+$,]+)*(?:;(?:[a-zA-Z0-9-_.!~*'()]+|(?:%[a-fA-F0-9]{2})+|[:@&=+$,]+)*)*)(?:/(?:(?:[a-zA-Z0-9-_.!~*'()]+|(?:%[a-fA-F0-9]{2})+|[:@&=+$,]+)*(?:;(?:[a-zA-Z0-9-_.!~*'()]+|(?:%[a-fA-F0-9]{2})+|[:@&=+$,]+)*)*))*" // abs_path
				+ "(?:\\?(?:(?:&)+|[;/?:@=+$,a-zA-Z0-9-_.!~*'()]+|(?:%[a-fA-F0-9]{2})+)+)?" // query
				+ ")?" // end of (abs_path+query)
				+ "(?:#(?:[;/?:@&=+$,a-zA-Z0-9-_.!~*'()]+|(?:%[a-fA-F0-9]{2})+)+)?" // fragment, for anchor
		;
		HTTP_URL_PATTERN = Pattern.compile(regex);
	}

	void generatePlainLink(final StringBuilder result) {
		Matcher matcher = HTTP_URL_PATTERN.matcher(result);
		final List<StrRegion> linkTextRegionList = new ArrayList<>();
		while (matcher.find()) {
			int start = matcher.start(), end = matcher.end();
			linkTextRegionList.add(new StrRegion(result.substring(start, end), start, end));
		}
		int offset = 0;
		for (StrRegion region : linkTextRegionList) {
			String linkText = region.str;
			String link = String.format("<a href=\"%s\" target=\"_blank\" rel=\"nofollow\">%s</a>",
					linkText, linkText);
			result.replace(region.start + offset, region.end + offset, link);
			offset += (link.length() - linkText.length());
		}
	}

	void generateSpaceBrAndAnchor(final StringBuilder str, final ParsingContext parsingContext)
			throws IOException {
		final int strLen = str.length();
		if (strLen == 0) {
			return;
		}
		final StringBuilder newStr = new StringBuilder(strLen);
		int previousNewlineIdx = -1;
		for (int index = 0; index < strLen; index++) {
			if (str.charAt(index) == '\n') {
				generateSpaceBrAndAnchorOneLine(str.substring(previousNewlineIdx + 1, index), true, newStr,
						parsingContext);
				newStr.append('\n');
				previousNewlineIdx = index;
			}
		}
		if (previousNewlineIdx < strLen - 1) {
			boolean endsWithNewline = str.charAt(strLen - 1) == '\n';
			generateSpaceBrAndAnchorOneLine(str.substring(previousNewlineIdx + 1), endsWithNewline,
					newStr, parsingContext);
			if (endsWithNewline)
				newStr.append('\n');
		}
		str.setLength(0);
		str.append(newStr);
	}

	private void generateSpaceBrAndAnchorOneLine(final String line, final boolean endsWithNewline,
			final StringBuilder newStr, final ParsingContext parsingContext) throws IOException {
		if (line.isEmpty()) {
			if (endsWithNewline)
				newStr.append("<br/>");
			return;
		}
		final int lineLen = line.length();
		int startingEqualSignCount = 0, endingEqualSignCount = 0;
		for (int index = 0; index < lineLen; index++) {
			if (line.charAt(index) == '=')
				++startingEqualSignCount;
			else
				break;
		}
		// it's not header
		if (startingEqualSignCount == 0) {
			// remove ending white spaces if necessary
			int endingSpaceCnt = 0;
			if (endsWithNewline) {
				for (int index = lineLen - 1; index >= 0; index--) {
					if (line.charAt(index) == ' ')
						endingSpaceCnt++;
					else
						break;
				}
			}
			// replace starting white space to "&nbsp;"
			int startingSpaceCnt = 0;
			for (int index = 0; index < lineLen - endingSpaceCnt; index++) {
				if (line.charAt(index) == ' ') {
					newStr.append("&nbsp;");
					startingSpaceCnt++;
				}
				else
					break;
			}
			newStr.append(line, startingSpaceCnt, lineLen - endingSpaceCnt);
			if (endsWithNewline)
				newStr.append("<br/>");
		}
		// it's header (h1, ..., h6)
		else {
			for (int index = lineLen - 1; index >= startingEqualSignCount; index--) {
				if (line.charAt(index) == '=')
					++endingEqualSignCount;
				else
					break;
			}
			if (startingEqualSignCount == endingEqualSignCount) {
				LogicAssert.assertTrue(startingEqualSignCount <= 6,
						"invalid header, equal sign count must be <=6, current value=%d",
						startingEqualSignCount);
				final List<BlogPostHeaderMetadata> headerMetadataList = parsingContext.headerMetadataList;
				final boolean notoc = parsingContext.notoc;

				final String header = line.substring(startingEqualSignCount,
						line.length() - endingEqualSignCount).trim(); // trim, header/alias can't be empty
				final String section = getSection(headerMetadataList, startingEqualSignCount);
				String headerText, headerIdForAnchor = null;
				if (header.indexOf("||") >= 0) {
					LogicAssert.assertTrue(header.indexOf("||") > 0, "invalid header=%s", header);
					String[] ss = header.split("\\|\\|");
					if (ss.length == 1) { // case like "pre1||"
						ss = new String[] { ss[0], "" };
					}
					headerText = ss[0].trim();
					if (!notoc) {
						ss[1] = ss[1].trim();
						headerIdForAnchor = ss[1].isEmpty() ? headerText : ss[1];
					}
				}
				else {
					headerText = header.trim();
					if (!notoc) {
						headerIdForAnchor = "section-" + section;
					}
				}
				headerText = section + " " + headerText;
				if (!notoc) {
					headerIdForAnchor = headerIdForAnchor.replace(' ', '-').replace('.', '-')
							.replace('_', '-').toLowerCase();
					LogicAssert.assertTrue(ANCHOR_PATTERN.matcher(headerIdForAnchor).matches(),
							"invalid, allowed character is [A-Za-z0-9 ._-], anchor=%s", headerIdForAnchor);
				}
				headerMetadataList.add(new BlogPostHeaderMetadata(startingEqualSignCount, headerText,
						headerIdForAnchor));
				// ref http://www.w3.org/TR/html4/struct/links.html
				final int hd = startingEqualSignCount;
				newStr.append(notoc ? String.format("<h%d class=\"bc-h%d\">%s</h%d>", hd, hd, headerText,
						hd) : String.format("<h%d id=\"%s\" class=\"bc-h%d\">%s</h%d>", hd, headerIdForAnchor,
						hd, headerText, hd));
			}
			else {
				LogicAssert.assertTrue(false,
						"count of '=' are not the same, it may be wrong, current line: %s", line);
			}
		}
	}

	private static final Pattern ANCHOR_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]*[a-z0-9]$");

	/**
	 * it's invoked when parsing and reaching a new header
	 * 
	 * @param headerMetadataList
	 *          header metadata list before current header is added in it
	 * @param currentLevel
	 *          header level, range is [1,6]
	 * @return section of current header, i.e. "2.3.1"
	 */
	String getSection(final List<BlogPostHeaderMetadata> headerMetadataList, final int currentLevel) {
		StringBuilder section = new StringBuilder();
		int[] levelCounts = new int[7]; // nil, h1, ..., h6
		levelCounts[currentLevel] += 1;
		int currentComparingLevel = currentLevel;
		for (int len = headerMetadataList.size(), index = len - 1; index >= 0; index--) {
			final int level = headerMetadataList.get(index).level;
			if (level == currentComparingLevel) {
				levelCounts[level] += 1;
			}
			else if (level < currentComparingLevel) { // parent header reached
				levelCounts[level] += 1;
				currentComparingLevel = level;
			}
		}
		LogicAssert.assertTrue(levelCounts[1] > 0, "level 1 doesn't exist");
		for (int i = 1; i <= currentLevel; i++) {
			section.append(levelCounts[i]).append('.');
		}
		section.setLength(section.length() - 1); // remove last '.'
		return section.toString();
	}

	/**
	 * it's invoked at the end of process
	 * 
	 * @param toc
	 *          "table of contents" storage
	 * @param headerMetadataList
	 *          complete header metadata list
	 * @param listIndex
	 *          headerMetadataList index
	 * @param haveUnclosedLiOnLevels
	 *          represent whether current level has unclosed &lt;li&gt;, it must be closed before
	 *          creating another same level &lt;li&gt;. Index [1,6] will be used for [h1,h6], so its
	 *          size must be greater than 7.
	 * @return headers count
	 */
	int generateTOC(final StringBuilder toc, final List<BlogPostHeaderMetadata> headerMetadataList,
			final int listIndex, final boolean[] haveUnclosedLiOnLevels) throws IOException {
		if (headerMetadataList.isEmpty()) {
			return 0;
		}
		int headersCnt = 0;
		if (listIndex < 0) {
			HashSet<String> hashSet = new HashSet<>();
			for (BlogPostHeaderMetadata hm : headerMetadataList) {
				LogicAssert.assertTrue(hashSet.add(hm.headerIdForAnchor),
						"there're conflicted headers, %s", hm.headerIdForAnchor);
			}

			// "{{readmore}}" is added, else
			// append current time millis to html elements id,
			// since blog list page may include many TOC, it'll cause issue.
			final StringBuilder tocHeadHtml = AssetsLoader.readAssetAsText("html/toc-head.html");
			StringUtils.replaceFirstStr(tocHeadHtml, "${toc-title}", blogPostInfoHolder.getLocale()
					.equals("zh") ? "目录" : "Contents");
			toc.append(tocHeadHtml);

			headersCnt = generateTOC(toc, headerMetadataList, 0, haveUnclosedLiOnLevels);
			toc.append(AssetsLoader.readAssetAsText("html/toc-tail.html"));
		}
		else {
			final int currentLevel = headerMetadataList.get(listIndex).level;
			toc.append("<ul>\n");
			for (int i = listIndex, len = headerMetadataList.size(); i < len;) {
				final BlogPostHeaderMetadata hm = headerMetadataList.get(i);
				final int level = hm.level;
				if (level == currentLevel) {
					if (haveUnclosedLiOnLevels[currentLevel]) {
						toc.append("</li>\n");
					}
					else {
						haveUnclosedLiOnLevels[currentLevel] = true;
					}
					toc.append("<li><a href=\"#").append(hm.headerIdForAnchor).append("\">")
							.append(hm.headerText).append("</a>");
					i++;
					headersCnt++;
				}
				else if (level > currentLevel) {
					LogicAssert.assertTrue(level - currentLevel == 1,
							"invalid header: %s, previous level is %d", hm, currentLevel);
					final int subHeadersCnt = generateTOC(toc, headerMetadataList, i, haveUnclosedLiOnLevels);
					toc.append("</li>\n");
					haveUnclosedLiOnLevels[currentLevel] = false;
					i += subHeadersCnt;
					headersCnt += subHeadersCnt;
				}
				else { // level < currentLevel , sub-headers finished
					if (haveUnclosedLiOnLevels[currentLevel]) {
						toc.append("</li>\n");
						haveUnclosedLiOnLevels[currentLevel] = false;
					}
					break;
				}
			}
			if (haveUnclosedLiOnLevels[currentLevel]) {
				toc.append("</li>\n");
				haveUnclosedLiOnLevels[currentLevel] = false;
			}
			toc.append("</ul>\n");
		}
		return headersCnt;
	}

	static final class StrRegion {
		final String str;
		/**
		 * The beginning index, inclusive.
		 */
		final int start;
		/**
		 * The ending index, exclusive.
		 */
		final int end;

		public StrRegion(String str, int start, int end) {
			this.str = str;
			this.start = start;
			this.end = end;
		}

		@Override
		public String toString() {
			return String.format("StrRegion [str=%s, start=%s, end=%s]", str, start, end);
		}
	}

	private void expandSpecialMacros(final StringBuilder body) {
		// replace ^\* following white spaces to "&nbsp;" to prevent word-breaking
		String str = Pattern.compile("^\\* (?=[^\n ])", Pattern.MULTILINE).matcher(body)
				.replaceAll("*&nbsp;");
		body.setLength(0);
		body.append(str);
	}

	/**
	 * @return generated post html file
	 */
	private File writeToPostHtmlFile(StringBuilder body, String postFileDir, String postFileName,
			String charsetName) throws IOException {
		File postHtmlFile = new File(postFileDir, postFileName + "-handled-post-file.html");
		// won't check blogHtmlFile exist or not, just overwrite
		try (OutputStream out = new BufferedOutputStream(new FileOutputStream(postHtmlFile))) {
			out.write(body.toString().getBytes(charsetName));
		}
		return postHtmlFile;
	}

}
