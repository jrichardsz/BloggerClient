package blogger.util;

public class LogicAssert {

	/**
	 * @throws RuntimeException
	 *           if condition is false
	 */
	public static void assertTrue(boolean condition, String errStr) {
		if (!condition)
			throw new RuntimeException(errStr);
	}

	/**
	 * @throws RuntimeException
	 *           if condition is false
	 */
	public static void assertTrue(boolean condition, String errFormat, Object... errFormatArgs) {
		if (!condition)
			throw new RuntimeException(String.format(errFormat, errFormatArgs));
	}

	/**
	 * @param exceptionClass
	 *          custom exception class here, it must have constructor with one String parameter
	 * @throws IllegalArgumentException
	 *           if throwableClass is invalid
	 * @throws RuntimeException
	 *           if condition is false
	 */
	public static void assertTrue(boolean condition,
			Class<? extends RuntimeException> exceptionClass, String errStr) {
		if (!condition) {
			RuntimeException throwable;
			try {
				throwable = exceptionClass.getConstructor(String.class).newInstance(errStr);
			}
			catch (Throwable t) {
				throw new IllegalArgumentException(String.format(
						"invalid throwableClass [%s], it must have constructor with one String parameter",
						exceptionClass));
			}
			throw throwable;
		}
	}

	/**
	 * @param exceptionClass
	 *          custom exception class here, it must have constructor with one String parameter
	 * @throws IllegalArgumentException
	 *           if throwableClass is invalid
	 * @throws RuntimeException
	 *           if condition is false
	 */
	public static void assertTrue(boolean condition,
			Class<? extends RuntimeException> exceptionClass, String errFormat, Object... errFormatArgs) {
		String errStr = String.format(errFormat, errFormatArgs);
		assertTrue(condition, exceptionClass, errStr);
	}

}
