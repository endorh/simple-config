package endorh.simpleconfig.clothconfig2.api;

public interface IExpandable {
	boolean isExpanded();
	default void setExpanded(boolean expanded) {
		setExpanded(expanded, false);
	}
	void setExpanded(boolean expanded, boolean recurse);
	
	/**
	 * Compute the relative y position of the top of the
	 * focused element, recurring into {@link IExpandable} children
	 */
	int getFocusedScroll();
	
	/**
	 * Compute the height of the focused element, recurring into
	 * {@link IExpandable} children
	 */
	int getFocusedHeight();
}

