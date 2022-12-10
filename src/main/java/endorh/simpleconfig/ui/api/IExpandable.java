package endorh.simpleconfig.ui.api;

public interface IExpandable {
	boolean isExpanded();
	default void setExpanded(boolean expanded) {
		setExpanded(expanded, false);
	}
	default void setExpanded(boolean expanded, boolean recurse) {
		setExpanded(expanded, recurse, true);
	}
	void setExpanded(boolean expanded, boolean recurse, boolean animate);
	
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

