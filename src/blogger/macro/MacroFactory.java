package blogger.macro;

import java.util.HashMap;
import java.util.List;

import blogger.macro.impl.AMacro;
import blogger.macro.impl.CodeMacro;
import blogger.macro.impl.PlainHtmlElemMacro;
import blogger.macro.impl.PreMacro;
import blogger.macro.impl.RevMacro;
import blogger.macro.impl.TableMacro;
import blogger.util.FileUtils;
import blogger.util.LogicAssert;

public class MacroFactory {
	private static final HashMap<String, Macro> MACRO_STORE = new HashMap<>(128, 0.25F);
	private static final HashMap<String, MacroPair> MACRO_PAIR_STORE = new HashMap<>(128, 0.25F);

	/**
	 * used to generate "Read more" anchor in blogger list page
	 */
	public static final Macro READMORE = Macro.newBuilder().setName("{{readmore}}")
			.setHtml("<!--more-->").setRemoveNextNewline(true).build();

	/**
	 * used to forbidden TOC (Table Of Contents).<br/>
	 * There's TOC by default.
	 */
	public static final Macro NOTOC = Macro.newBuilder().setName("{{notoc}}").setHtml("")
			.setRemoveNextNewline(true).build();

	/**
	 * used to generate blog post revisions header
	 */
	public static final Macro REV = Macro.newBuilder(RevMacro.class).setName("{{rev}}")
			.setHtml("<h4 class=\"revisions\">=${rev}=</h4>\n").setRemoveNextNewline(true)
			.setCssRelativePaths("css/revisions.css").build();

	static {
		try {
			addMacros();
		}
		catch (Exception e) {
			throw new RuntimeException("add macros failed", e);
		}
	}

	private static void addMacros() throws Exception {
		addMacro(READMORE);
		addMacro(NOTOC);
		addMacro(REV);

		addMacroPair(new PreMacro());
		addMacroPair(new CodeMacro());
		addMacroPair(new AMacro());
		addMacroPair(new TableMacro());

		List<String> htmlElemNames = FileUtils
				.readPackageFileAsLines("blogger/macro/PlainHtmlElem.list");
		for (String htmlElemName : htmlElemNames) {
			htmlElemName = htmlElemName.trim();
			if (htmlElemName.length() > 0) {
				addMacroPair(new PlainHtmlElemMacro(htmlElemName));
			}
		}
	}

	private static void addMacro(Macro macro) {
		String macroName = macro.getName();
		LogicAssert.assertTrue(Macro.MACRO_PATTERN.matcher(macroName).matches(),
				"invalid macro name=%s, %s", macroName, macro);
		LogicAssert.assertTrue(!MACRO_STORE.containsKey(macroName), "macro name is already registered",
				macroName);
		MACRO_STORE.put(macroName, macro);
	}

	private static void addMacroPair(MacroPair macroPair) {
		final Macro first = macroPair.getFirst(), second = macroPair.getSecond();
		addMacro(first);
		addMacro(second);
		LogicAssert.assertTrue(first.getName().equals(second.getPairedMacroName())
				&& first.getPairedMacroName().equals(second.getName()),
				"macro names don't match in macroPair, %s", macroPair);
		MACRO_PAIR_STORE.put(first.getName(), macroPair);
	}

	public static Macro getMacro(String name) {
		return MACRO_STORE.get(name);
	}

	/**
	 * @param name
	 *          macroPair.first.name
	 */
	public static MacroPair getMacroPair(String name) {
		return MACRO_PAIR_STORE.get(name);
	}

}
