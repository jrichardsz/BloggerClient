package blogger;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JDialog;
import javax.swing.JFrame;

import blogger.assets.AssetsLoader;
import blogger.macro.MacroFactory;
import blogger.macro.Macro;
import blogger.util.HtmlUtils;
import blogger.util.LogicAssert;
import blogger.util.StringUtils;

public class BloggerClient {
	static {
		// try {
		// UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		// }
		// catch (ClassNotFoundException | InstantiationException | IllegalAccessException
		// | UnsupportedLookAndFeelException e) {
		// e.printStackTrace();
		// }
		JFrame.setDefaultLookAndFeelDecorated(true);
		JDialog.setDefaultLookAndFeelDecorated(true);
	}

	public static void main(String[] args) {
		INSTANCE.showUI();
	}

	private static final BloggerClient INSTANCE = new BloggerClient();

	private BloggerClient() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				StringBuilder undeletedErrStr = new StringBuilder(1024);
				for (File f : blogHtmlFileList) {
					if (f.exists() && f.delete() == false) {
						undeletedErrStr.append("delete ").append(f.getAbsolutePath()).append(" failed.\n");
					}
				}
				System.out.println(undeletedErrStr);
			}
		});
	}

	public static BloggerClient getInstance() {
		return INSTANCE;
	}

	private void showUI() {
		BloggerClientFrame frame = new BloggerClientFrame("Blogger Client");
		frame.setVisible(true);
	}

	private final ArrayList<File> blogHtmlFileList = new ArrayList<>();

	ArrayList<File> getBlogHtmlFileList() {
		return blogHtmlFileList;
	}

	/*
	article metadata must be put at the second line of article,
	since Java won't remove Unicode BOM, so ignore the first line.

	format:
	<metadata>
	title = {title} / title = {title} || {title-for-permalink}
	tags = {tag}, {tag}
	locale = 
	</metadata>

	header format:
	= headerText || headerIdForAnchor =
	could be several '='
	headerIdForAnchor could be [a-zA-Z0-9-_. ]
	e.g:
	= 标题 1 || header-1 =
	== 标题 1.1 ==
	== title 1.2 || ==
	== title 1.3 ==
	*/
	BlogPostMetadata processBlogFile(final File blogFile, final String charsetName) throws Exception {
		System.out.println(String.format("processBlogFile> file=%s", blogFile));
		BufferedReader bufferedReader = null;
		StringBuilder[] mbArray;
		try {
			bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(blogFile),
					charsetName));
			mbArray = readMetadataAndBody(bufferedReader);
		}
		finally {
			if (bufferedReader != null)
				bufferedReader.close();
		}
		final StringBuilder metadataStr = mbArray[0], body = mbArray[1];
		blogPostMetadata = parseBlogPostMetadata(metadataStr);
		StringUtils.replaceAllStr(body, "\r\n", "\n");
		StringUtils.replaceAllStr(body, "\r", "\n");
		removeStartingEndingNewlines(body);
		StringUtils.replaceCharToStr(body, '\t', "    "); // 4 white spaces
		HtmlUtils.replaceHtmlEntities(body);
		final ParsingContext parsingContext = new ParsingContext();
		processBlogFile0(body, parsingContext);
		expandSpecialMacros(body);

		// add TOC to the beginning
		if (!parsingContext.notoc) {
			parsingContext.assetRelativePathSet.add("css/toc.css");
			final List<BlogPostHeaderMetadata> headerMetadataList = parsingContext.headerMetadataList;
			if (headerMetadataList.size() > 0) {
				parsingContext.assetRelativePathSet.add("css/headers.css");
			}
			final StringBuilder toc = new StringBuilder(1024);
			int headersCnt = generateTOC(toc, headerMetadataList, -1);
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
		blogPostMetadata.setHtmlFile(writeToBlogHtmlFile(body, blogFile.getParent(),
				blogFile.getName(), charsetName));
		blogHtmlFileList.add(blogPostMetadata.getHtmlFile());
		blogPostMetadata.setHtmlBody(body.toString());
		return blogPostMetadata;
	}

	private BlogPostMetadata blogPostMetadata;

	public BlogPostMetadata getBlogPostMetadata() {
		return blogPostMetadata;
	}

	/**
	 * @return metadata-body array
	 */
	StringBuilder[] readMetadataAndBody(final BufferedReader bufferedReader) throws IOException {
		StringBuilder[] mb = new StringBuilder[2];
		String line;
		final StringBuilder metadata = new StringBuilder(1024), body = new StringBuilder(8192);
		final int beforeMetadata = 1, inMetadata = 2, afterMetadata = 3;
		int state = beforeMetadata;
		LogicAssert.assertTrue(bufferedReader.readLine() != null, "file is empty");
		while ((line = bufferedReader.readLine()) != null) {
			switch (state) {
			case beforeMetadata:
				LogicAssert.assertTrue(line.equals("<metadata>"),
						"<metadata> not found at the second line, line=%s", line);
				state = inMetadata;
				break;
			case inMetadata:
				if (!line.equals("</metadata>"))
					metadata.append(line).append('\n');
				else
					state = afterMetadata;
				break;
			case afterMetadata:
				body.append(line).append('\n');
				break;
			default:
				LogicAssert.assertTrue(false, "unknown state=" + state);
			}
		}
		LogicAssert.assertTrue(state == afterMetadata, "</metadata> not found, state=%d", state);
		mb[0] = metadata;
		mb[1] = body;
		return mb;
	}

	/**
	 * just split properties text
	 */
	private BlogPostMetadata parseBlogPostMetadata(final StringBuilder metadataStr)
			throws IOException {
		Properties props = new Properties();
		props.load(new StringReader(metadataStr.toString()));
		return new BlogPostMetadata(props.getProperty("title"), props.getProperty("tags"),
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
			body.delete(body.length() - cnt, body.length());
	}

	private void processBlogFile0(final StringBuilder body, final ParsingContext parsingContext)
			throws Exception {
		final List<StrRegion> macroRegionList = getMacrosRegion(body);
		final int bodyLen = body.length();
		final StringBuilder newBody = new StringBuilder(bodyLen);
		Macro pair1stMacro = null;
		StrRegion previousMacroRegion = new StrRegion("", 0, 0);
		for (StrRegion macroRegion : macroRegionList) {
			Macro macro = MacroFactory.getMacro(macroRegion.str);
			LogicAssert.assertTrue(macro != null, "can't find macro, %s", macroRegion);
			injectAssetRelativePaths(macro.getCssRelativePaths(), parsingContext);
			injectAssetRelativePaths(macro.getJsRelativePaths(), parsingContext);
			if (pair1stMacro != null) {
				LogicAssert.assertTrue(
						pair1stMacro.getPairedMacroName().equals(macro.getName()),
						"no paired macro, %s, %s, %s",
						pair1stMacro.getName(),
						macro.getName(),
						body.substring(previousMacroRegion.end,
								Math.min(macroRegion.start, previousMacroRegion.end + 100)));
			}
			{
				int start = macroRegion.start, end = macroRegion.end;
				start = macro.isRemovePreviousNewline() && start > 0 && body.charAt(start - 1) == '\n' ? start - 1
						: start;
				end = macro.isRemoveNextNewline() && end < bodyLen && body.charAt(end) == '\n' ? end + 1
						: end;
				if (start != macroRegion.start || end != macroRegion.end)
					macroRegion = new StrRegion(macroRegion.str, start, end);
			}
			if (macro.getPairedMacroName() != null) {
				//TODO nested macro is not supported for now
				if (macro.getName().indexOf('/') >= 0) {
					LogicAssert.assertTrue(
							pair1stMacro != null && pair1stMacro.getPairedMacroName().equals(macro.getName()),
							"no paired macro, %s, %s, %s",
							pair1stMacro.getName(),
							macro.getName(),
							body.substring(previousMacroRegion.end,
									Math.min(macroRegion.start, previousMacroRegion.end + 100)));
					newBody.append(expandMacroPair(pair1stMacro, macro,
							body.substring(previousMacroRegion.end, macroRegion.start)));
					pair1stMacro = null;
				}
				else {
					newBody.append(processMacroOuterRegion(
							body.substring(previousMacroRegion.end, macroRegion.start), parsingContext));
					pair1stMacro = macro;
				}
			}
			else {
				if (macro.getName().equals(MacroFactory.NOTOC.getName())) {
					parsingContext.notoc = true;
				}
				newBody.append(processMacroOuterRegion(
						body.substring(previousMacroRegion.end, macroRegion.start), parsingContext));
				newBody.append(expandMacro(macro));
			}
			previousMacroRegion = macroRegion;
		}
		// handle the left region
		if (previousMacroRegion.end < bodyLen) {
			newBody.append(processMacroOuterRegion(body.substring(previousMacroRegion.end, bodyLen),
					parsingContext));
		}

		body.setLength(0);
		body.append(newBody);
	}

	private static final class ParsingContext {
		final List<BlogPostHeaderMetadata> headerMetadataList = new ArrayList<>();
		/**
		 * asset relative path must be unique, so HashSet is required
		 */
		final HashSet<String> assetRelativePathSet = new HashSet<>(1024, 0.25F);
		boolean notoc = false;
	}

	List<StrRegion> getMacrosRegion(StringBuilder body) {
		final ArrayList<StrRegion> regionList = new ArrayList<>();
		Matcher matcher = Macro.MACRO_PATTERN.matcher(body);
		while (matcher.find()) {
			int start = matcher.start(), end = matcher.end();
			regionList.add(new StrRegion(body.substring(start, end), start, end));
		}
		return regionList;
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
			throws IOException, URISyntaxException {
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
			String linkText = result.substring(start, end);
			linkTextRegionList.add(new StrRegion(linkText, start, end));
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
			throws IOException, URISyntaxException {
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
			final StringBuilder newStr, final ParsingContext parsingContext) throws IOException,
			URISyntaxException {
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
		// it's header (h1, h2, h3, ...)
		else {
			for (int index = lineLen - 1; index >= startingEqualSignCount; index--) {
				if (line.charAt(index) == '=')
					++endingEqualSignCount;
				else
					break;
			}
			if (startingEqualSignCount == endingEqualSignCount) {
				final List<BlogPostHeaderMetadata> headerMetadataList = parsingContext.headerMetadataList;
				final boolean notoc = parsingContext.notoc;

				final String header = line.substring(startingEqualSignCount, line.length()
						- endingEqualSignCount); // space is allowed, so no header.trim()
				final String section = getSection(headerMetadataList, startingEqualSignCount);
				String headerText, headerIdForAnchor = null;
				if (header.indexOf("||") >= 0) {
					String[] ss = header.split("\\|\\|");
					LogicAssert.assertTrue(ss.length == 2, "invalid header=%s", header);
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
					headerIdForAnchor = headerIdForAnchor.replace(' ', '-').replace('.', '-').toLowerCase();
					HtmlUtils.verifyHtmlElementId(headerIdForAnchor);
				}
				headerMetadataList.add(new BlogPostHeaderMetadata(startingEqualSignCount, headerText,
						headerIdForAnchor));
				// ref http://www.w3.org/TR/html4/struct/links.html
				int hd = startingEqualSignCount + 1;
				newStr.append(notoc ? String.format("<h%d class=\"h%d\">%s</h%d>", hd, hd, headerText, hd)
						: String.format("<h%d id=\"%s\" class=\"h%d\">%s</h%d>", hd, headerIdForAnchor, hd,
								headerText, hd));
			}
			else {
				LogicAssert.assertTrue(false,
						"count of '=' are not the same, it may be wrong, current line: %s", line);
			}
		}
	}

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
	 * @return headers count
	 */
	int generateTOC(final StringBuilder toc, final List<BlogPostHeaderMetadata> headerMetadataList,
			final int listIndex) throws IOException, URISyntaxException {
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
			StringUtils.replaceFirstStr(tocHeadHtml, "${toc-title}",
					blogPostMetadata.getLocale().equals("zh") ? "目录" : "Contents");
			toc.append(tocHeadHtml);

			headersCnt = generateTOC(toc, headerMetadataList, 0);
			toc.append(AssetsLoader.readAssetAsText("html/toc-tail.html"));
		}
		else {
			final int currentLevel = headerMetadataList.get(listIndex).level;
			toc.append("<ul>\n");
			for (int i = listIndex, len = headerMetadataList.size(); i < len;) {
				final BlogPostHeaderMetadata hm = headerMetadataList.get(i);
				final int level = hm.level;
				if (level == currentLevel) {
					++headersCnt;
					toc.append("<li><a href=\"#").append(hm.headerIdForAnchor).append("\">")
							.append(hm.headerText).append("</a></li>").append('\n');
					i++;
				}
				else if (level > currentLevel) {
					LogicAssert.assertTrue(level - currentLevel == 1,
							"invalid header: %s, previous level is %d", hm, currentLevel);
					int subHeadersCnt = generateTOC(toc, headerMetadataList, i);
					i += subHeadersCnt;
					headersCnt += subHeadersCnt;
				}
				else { // level < currentLevel , sub-headers finished
					break;
				}
			}
			toc.append("</ul>\n");
		}
		return headersCnt;
	}

	private static final class StrRegion {
		final String str;
		final int start, end;

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

	private String expandMacro(Macro macro) {
		return macro.getHtml();
	}

	private String expandMacroPair(Macro firstMacro, Macro secondMacro, String embededStr)
			throws Exception {
		LogicAssert.assertTrue(firstMacro.getPairedMacroName().equals(secondMacro.getName()),
				"invalid macro pair, %s, %s", firstMacro, secondMacro);
		return MacroFactory.getMacroPair(firstMacro.getName()).getHtml(embededStr);
	}

	private void expandSpecialMacros(final StringBuilder body) {
		// replace ^\* following white spaces to "&nbsp;" to prevent word-breaking
		String str = Pattern.compile("^\\* (?=[^\n ])", Pattern.MULTILINE).matcher(body)
				.replaceAll("*&nbsp;");
		body.setLength(0);
		body.append(str);
	}

	/**
	 * @return generated blog html file
	 */
	private File writeToBlogHtmlFile(StringBuilder body, String blogFileDir, String blogFileName,
			String charsetName) throws IOException {
		File blogHtmlFile = new File(blogFileDir, blogFileName + "-handled-blog-file.html");
		// won't check blogHtmlFile exist or not, just overwrite
		BufferedOutputStream out = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(blogHtmlFile));
			out.write(body.toString().getBytes(charsetName));
			out.flush();
		}
		finally {
			if (out != null)
				out.close();
		}
		return blogHtmlFile;
	}

}