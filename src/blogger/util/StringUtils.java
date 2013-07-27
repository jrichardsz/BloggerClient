package blogger.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class StringUtils {

	/**
	 * @param fromChar
	 *          it could not exist
	 */
	public static void replaceAll(StringBuilder strBuilder, char fromChar, String toStr) {
		replaceAll(strBuilder, 0, strBuilder.length(), fromChar, toStr);
	}

	/**
	 * @param startIndex
	 *          The beginning index, inclusive
	 * @param endIndex
	 *          The ending index, exclusive
	 */
	public static void replaceAll(StringBuilder strBuilder, int startIndex, int endIndex,
			char fromChar, String toStr) {
		// replace from ending to starting, no need to change index dynamically
		for (int index = endIndex - 1; index >= startIndex; index--) {
			if (strBuilder.charAt(index) == fromChar) {
				strBuilder.replace(index, index + 1, toStr);
			}
		}
	}

	/**
	 * @param fromStr
	 *          it must exist
	 */
	public static void replaceFirst(StringBuilder strBuilder, String fromStr, String toStr) {
		// won't use String.replace, since it use regex, it need to be reserved.
		int idx = strBuilder.indexOf(fromStr);
		strBuilder.replace(idx, idx + fromStr.length(), toStr);
	}

	/**
	 * @param fromStr
	 *          it could not exist, text won't be changed in this case
	 */
	public static void replaceAll(StringBuilder strBuilder, String fromStr, String toStr) {
		// won't use String.replace, since it use regex, it need to be reserved.
		int fromStrIdx, fromIndex = 0;
		final int fromStrLen = fromStr.length(), toStrLen = toStr.length();
		while (true) {
			fromStrIdx = strBuilder.indexOf(fromStr, fromIndex);
			if (fromStrIdx >= 0) {
				strBuilder.replace(fromStrIdx, fromStrIdx + fromStrLen, toStr);
				fromIndex = fromStrIdx + toStrLen;
			}
			else {
				break;
			}
		}
	}

	/**
	 * @return lines list, list may be not thread-safe, it won't be null
	 */
	public static List<String> readTextAsLines(String text) throws IOException {
		final List<String> result = new ArrayList<>();
		try (BufferedReader bufferedReader = new BufferedReader(new StringReader(text))) {
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				result.add(line);
			}
		}
		return result;
	}

}
