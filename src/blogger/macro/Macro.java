package blogger.macro;

import java.util.regex.Pattern;

public class Macro {

	public static final Pattern MACRO_PATTERN = Pattern.compile("\\{\\{/?[a-z0-9-]+\\}\\}");

	private String name;
	private String pairedMacroName;
	private String html;
	private boolean removePreviousNewline;
	private boolean removeNextNewline;
	/**
	 * CSS files' relative paths in assets package, separated by ','
	 */
	private String cssRelativePaths = "";
	/**
	 * JavaScript files' relative paths in assets package, separated by ','
	 */
	private String jsRelativePaths = "";
	public static final String assetRelativePathsSeparator = ",";

	public String getName() {
		return name;
	}

	public String getPairedMacroName() {
		return pairedMacroName;
	}

	public String getHtml() {
		return html;
	}

	public boolean isRemovePreviousNewline() {
		return removePreviousNewline;
	}

	public boolean isRemoveNextNewline() {
		return removeNextNewline;
	}

	public String getCssRelativePaths() {
		return cssRelativePaths;
	}

	public String getJsRelativePaths() {
		return jsRelativePaths;
	}

	public static final class Builder {
		private final Class<? extends Macro> clazz;

		private Builder(Class<? extends Macro> clazz) {
			this.clazz = clazz;
		}

		private String name;
		private String html;
		private String pairedMacroName = null;
		private boolean removePreviousNewline = false;
		private boolean removeNextNewline = false;
		private String cssRelativePaths = "";
		private String jsRelativePaths = "";

		public Builder setName(String name) {
			this.name = name;
			return this;
		}

		public Builder setHtml(String html) {
			this.html = html;
			return this;
		}

		public Builder setPairedMacroName(String pairedMacroName) {
			this.pairedMacroName = pairedMacroName;
			return this;
		}

		public Builder setRemovePreviousNewline(boolean removePreviousNewline) {
			this.removePreviousNewline = removePreviousNewline;
			return this;
		}

		public Builder setRemoveNextNewline(boolean removeNextNewline) {
			this.removeNextNewline = removeNextNewline;
			return this;
		}

		public Builder setCssRelativePaths(String cssRelativePaths) {
			this.cssRelativePaths = cssRelativePaths;
			return this;
		}

		public Builder setJsRelativePaths(String jsRelativePaths) {
			this.jsRelativePaths = jsRelativePaths;
			return this;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Builder [clazz=").append(clazz).append(", name=").append(name)
					.append(", html=").append(html).append(", pairedMacroName=").append(pairedMacroName)
					.append(", removePreviousNewline=").append(removePreviousNewline)
					.append(", removeNextNewline=").append(removeNextNewline).append(", cssRelativePaths=")
					.append(cssRelativePaths).append(", jsRelativePaths=").append(jsRelativePaths)
					.append("]");
			return builder.toString();
		}

		public Macro build() {
			Macro macro;
			try {
				macro = clazz.newInstance();
			}
			catch (Exception e) {
				throw new RuntimeException("build macro failed, " + this, e);
			}
			macro.name = name;
			macro.pairedMacroName = pairedMacroName;
			macro.html = html;
			macro.removePreviousNewline = removePreviousNewline;
			macro.removeNextNewline = removeNextNewline;
			macro.cssRelativePaths = cssRelativePaths;
			macro.jsRelativePaths = jsRelativePaths;
			return macro;
		}
	}

	public static Builder newBuilder() {
		return new Builder(Macro.class);
	}

	public static Builder newBuilder(Class<? extends Macro> clazz) {
		return new Builder(clazz);
	}

}
