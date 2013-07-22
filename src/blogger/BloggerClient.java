package blogger;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.ProxySelector;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import blogger.assets.AssetsLoader;
import blogger.macro.Macro;
import blogger.macro.MacroFactory;
import blogger.macro.impl.HtmlMacro;
import blogger.util.HtmlUtils;
import blogger.util.LogicAssert;
import blogger.util.PureStack;
import blogger.util.StringUtils;

public class BloggerClient {
	public static final String VERSION = "V0.6.0";
	public static final String NAME = "BloggerClientZZ"; // do not contain blanks
	public static final String CONF_FILENAME = "conf.properties";

	public File getUserDataHome() {
		File dir = new File(System.getProperty("user.home"), "." + NAME);
		if (!dir.exists()) {
			if (!dir.mkdir()) {
				throw new RuntimeException(String.format("create directory [%s] failed", dir));
			}
		}
		else if (!dir.isDirectory()) {
			throw new RuntimeException(String.format("[%s] already exists and it's not directory.", dir));
		}
		return dir;
	}

	public static void main(String[] args) {
		// custom L&F
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		JFrame.setDefaultLookAndFeelDecorated(true);
		JDialog.setDefaultLookAndFeelDecorated(true);
		//
		try {
			Configuration cfg = new Configuration(new File(INSTANCE.getUserDataHome(), CONF_FILENAME),
					BloggerClient.class.getResource(CONF_FILENAME).toURI());
			ProxySelector.setDefault(cfg.getProxySelector());
		}
		catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		// show UI
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				INSTANCE.createAndShowGUI();
			}
		});
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

	private void createAndShowGUI() {
		final BloggerClientFrame frame = new BloggerClientFrame(NAME);
		frame.setSize(900, 900);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null);
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

	private static final class ParsingContext {
		final List<BlogPostHeaderMetadata> headerMetadataList = new ArrayList<>();
		/**
		 * asset relative path must be unique, so HashSet is required
		 */
		final HashSet<String> assetRelativePathSet = new HashSet<>(1024, 0.25F);
		boolean notoc = false;
		int processingNestedMacrosDepth = 0;
	}

	private void processBlogFile0(final StringBuilder body, final ParsingContext parsingContext)
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
		StringBuilder newBody = processBlogFile00(body, macroRegionList, 0, macroRegionList.size() - 1,
				parsingContext);
		body.setLength(0);
		body.append(newBody);
	}

	private StringBuilder processBlogFile00(final StringBuilder body,
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
					StringBuilder embededStr = processBlogFile00(body, macroRegionList, macroRegion.index,
							macroRegion.pairedMacroRegionIndex, parsingContext);
					parsingContext.processingNestedMacrosDepth -= 1;
					newBody.append(MacroFactory.getMacroPair(macro.getName()).getHtml(embededStr.toString()));
				}
				index += (macroRegion.pairedMacroRegionIndex - macroRegion.index + 1);
				previousMacroRegion = pairedMacroRegion;
			}
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
							body.subSequence(pairedMacroRegion.end,
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
					headerIdForAnchor = headerIdForAnchor.replace(' ', '-').replace('.', '-').toLowerCase();
					HtmlUtils.verifyHtmlElementId(headerIdForAnchor);
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
			StringUtils.replaceFirstStr(tocHeadHtml, "${toc-title}",
					blogPostMetadata.getLocale().equals("zh") ? "目录" : "Contents");
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
