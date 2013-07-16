package blogger;

/**
 * it represent header in article, i.e. = header1 || h1 =, then<br/>
 * level=1, headerText=header1, headerIdForAnchor=h1
 */
class BlogPostHeaderMetadata {
	final int level;
	final String headerText;
	final String headerIdForAnchor;

	public BlogPostHeaderMetadata(int level, String headerText, String headerIdForAnchor) {
		this.level = level;
		this.headerText = headerText;
		this.headerIdForAnchor = headerIdForAnchor;
	}

	@Override
	public String toString() {
		return String.format("BlogPostHeaderMetadata [level=%s, headerText=%s, headerIdForAnchor=%s]",
				level, headerText, headerIdForAnchor);
	}
}
