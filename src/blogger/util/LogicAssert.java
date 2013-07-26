package blogger.util;

public class LogicAssert {

	public static void assertTrue(boolean flag, String errStr) {
		if (!flag)
			throw new RuntimeException(errStr);
	}

	public static void assertTrue(boolean flag, String formatStr, Object... formatArgs) {
		if (!flag)
			throw new RuntimeException(String.format(formatStr, formatArgs));
	}

}
