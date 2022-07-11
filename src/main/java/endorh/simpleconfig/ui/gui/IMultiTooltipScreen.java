package endorh.simpleconfig.ui.gui;

import endorh.simpleconfig.ui.api.Tooltip;
import endorh.simpleconfig.ui.math.Rectangle;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Screen supporting the queuing of tooltips.
 */
public interface IMultiTooltipScreen {
	/**
	 * Add a tooltip for this frame
	 */
	void addTooltip(Tooltip tooltip);
	
	/**
	 * @return Mutable tooltip list for this frame
	 */
	List<Tooltip> getTooltips();
	
	/**
	 * Remove all tooltips already registered from an area for this frame
	 * @return True if at least one tooltip was removed
	 */
	default boolean removeTooltips(Rectangle area) {
		final List<Tooltip> tooltips = getTooltips();
		final List<Tooltip> removed =
		  tooltips.stream().filter(t -> area.contains(t.getPoint())).collect(Collectors.toList());
		return tooltips.removeAll(removed);
	}
}
