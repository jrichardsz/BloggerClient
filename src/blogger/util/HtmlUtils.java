package blogger.util;

public class HtmlUtils {

	/**
	 * from http://www.w3schools.com/tags/ref_entities.asp <br/>
	 * space and soft hyphen are excluded. <br/>
	 * '&' is moved to the first element, since it should be replaced at first.
	 */
	private static final String[][] LEFT_HTML_ENTITIES = new String[][] { { "&", "&amp;" },
			{ "\"", "&quot;" }, { "'", "&apos;" }, { "<", "&lt;" }, { ">", "&gt;" }, { "¡", "&iexcl;" },
			{ "¢", "&cent;" }, { "£", "&pound;" }, { "¤", "&curren;" }, { "¥", "&yen;" },
			{ "¦", "&brvbar;" }, { "§", "&sect;" }, { "¨", "&uml;" }, { "©", "&copy;" },
			{ "ª", "&ordf;" }, { "«", "&laquo;" }, { "¬", "&not;" }, { "®", "&reg;" }, { "¯", "&macr;" },
			{ "°", "&deg;" }, { "±", "&plusmn;" }, { "²", "&sup2;" }, { "³", "&sup3;" },
			{ "´", "&acute;" }, { "µ", "&micro;" }, { "¶", "&para;" }, { "·", "&middot;" },
			{ "¸", "&cedil;" }, { "¹", "&sup1;" }, { "º", "&ordm;" }, { "»", "&raquo;" },
			{ "¼", "&frac14;" }, { "½", "&frac12;" }, { "¾", "&frac34;" }, { "¿", "&iquest;" },
			{ "×", "&times;" }, { "÷", "&divide;" }, { "À", "&Agrave;" }, { "Á", "&Aacute;" },
			{ "Â", "&Acirc;" }, { "Ã", "&Atilde;" }, { "Ä", "&Auml;" }, { "Å", "&Aring;" },
			{ "Æ", "&AElig;" }, { "Ç", "&Ccedil;" }, { "È", "&Egrave;" }, { "É", "&Eacute;" },
			{ "Ê", "&Ecirc;" }, { "Ë", "&Euml;" }, { "Ì", "&Igrave;" }, { "Í", "&Iacute;" },
			{ "Î", "&Icirc;" }, { "Ï", "&Iuml;" }, { "Ð", "&ETH;" }, { "Ñ", "&Ntilde;" },
			{ "Ò", "&Ograve;" }, { "Ó", "&Oacute;" }, { "Ô", "&Ocirc;" }, { "Õ", "&Otilde;" },
			{ "Ö", "&Ouml;" }, { "Ø", "&Oslash;" }, { "Ù", "&Ugrave;" }, { "Ú", "&Uacute;" },
			{ "Û", "&Ucirc;" }, { "Ü", "&Uuml;" }, { "Ý", "&Yacute;" }, { "Þ", "&THORN;" },
			{ "ß", "&szlig;" }, { "à", "&agrave;" }, { "á", "&aacute;" }, { "â", "&acirc;" },
			{ "ã", "&atilde;" }, { "ä", "&auml;" }, { "å", "&aring;" }, { "æ", "&aelig;" },
			{ "ç", "&ccedil;" }, { "è", "&egrave;" }, { "é", "&eacute;" }, { "ê", "&ecirc;" },
			{ "ë", "&euml;" }, { "ì", "&igrave;" }, { "í", "&iacute;" }, { "î", "&icirc;" },
			{ "ï", "&iuml;" }, { "ð", "&eth;" }, { "ñ", "&ntilde;" }, { "ò", "&ograve;" },
			{ "ó", "&oacute;" }, { "ô", "&ocirc;" }, { "õ", "&otilde;" }, { "ö", "&ouml;" },
			{ "ø", "&oslash;" }, { "ù", "&ugrave;" }, { "ú", "&uacute;" }, { "û", "&ucirc;" },
			{ "ü", "&uuml;" }, { "ý", "&yacute;" }, { "þ", "&thorn;" }, { "ÿ", "&yuml;" } };

	static {
		String entity;
		for (String[] entityPair : LEFT_HTML_ENTITIES) {
			entity = entityPair[1];
			if (!(entityPair[0].length() == 1 && entity.length() > 1 && entity.charAt(0) == '&' && entity
					.charAt(entity.length() - 1) == ';')) {
				throw new RuntimeException(String.format("invalid entity: { \"%s\", \"%s\" }",
						entityPair[0], entity));
			}
		}
	}

	/**
	 * replace HTML entities, i.e. "&lt;" to "&amp;lt;", space and soft hyphen are excluded.
	 */
	public static void replaceHtmlEntities(StringBuilder strBuilder) {
		replaceHtmlEntities(strBuilder, 0, strBuilder.length());
	}

	/**
	 * replace HTML entities, i.e. "&lt;" to "&amp;lt;", space and soft hyphen are excluded.
	 * 
	 * @param startIndex
	 *          The beginning index, inclusive
	 * @param endIndex
	 *          The ending index, exclusive
	 */
	public static void replaceHtmlEntities(StringBuilder strBuilder, int startIndex, int endIndex) {
		for (String[] entity : LEFT_HTML_ENTITIES) {
			StringUtils.replaceAll(strBuilder, startIndex, endIndex, entity[0].charAt(0), entity[1]);
		}
	}

}
