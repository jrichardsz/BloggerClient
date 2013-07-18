package blogger.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class StringUtils {

	public static void replaceCharToStr(StringBuilder builder, final char ch, final String str) {
		// replace from ending to starting, no need to change index dynamically
		for (int index = builder.length() - 1; index >= 0; index--) {
			if (builder.charAt(index) == ch) {
				builder.replace(index, index + 1, str);
			}
		}
	}

	/**
	 * @param fromStr it must exist
	 */
	public static void replaceFirstStr(StringBuilder builder, String fromStr, String toStr) {
		// won't use String.replace, since it use regex, it need to be reserved.
		int idx = builder.indexOf(fromStr);
		builder.replace(idx, idx + fromStr.length(), toStr);
	}

	/**
	 * @param fromStr it could not exist, builder won't be changed in this case
	 */
	public static void replaceAllStr(StringBuilder builder, String fromStr, String toStr) {
		// won't use String.replace, since it use regex, it need to be reserved.
		int fromStrIdx, fromIndex = 0;
		final int fromStrLen = fromStr.length(), toStrLen = toStr.length();
		while (true) {
			fromStrIdx = builder.indexOf(fromStr, fromIndex);
			if (fromStrIdx >= 0) {
				builder.replace(fromStrIdx, fromStrIdx + fromStrLen, toStr);
				fromIndex = fromStrIdx + toStrLen;
			}
			else {
				break;
			}
		}
	}

	public static List<String> readTextAsLines(final String text) throws IOException {
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
