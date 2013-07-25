package blogger.macro;

import blogger.util.LogicAssert;

public abstract class MacroPair {

	private Macro first;
	private Macro second;

	public MacroPair() {
	}

	public Macro getFirst() {
		if (first == null) {
			first = buildFirstMacro();
		}
		return first;
	}

	public Macro getSecond() {
		if (second == null) {
			second = buildSecondMacro();
		}
		return second;
	}

	/**
	 * it will be invoked once in constructor
	 */
	protected abstract Macro buildFirstMacro();

	/**
	 * it will be invoked once in constructor
	 */
	protected abstract Macro buildSecondMacro();

	/**
	 * @return processed html (must include processed embededStr)
	 */
	public String getHtml(String embededStr) throws Exception {
		StringBuilder result = new StringBuilder();
		String firstHtml = first.getHtml();
		LogicAssert.assertTrue(firstHtml != null, "html is null, macro=%s", first);
		result.append(firstHtml);
		result.append(embededStr);
		String secondHtml = second.getHtml();
		LogicAssert.assertTrue(secondHtml != null, "html is null, macro=%s", second);
		result.append(secondHtml);
		return result.toString();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MacroPair [first=").append(first).append(", second=").append(second)
				.append("]");
		return builder.toString();
	}

}
