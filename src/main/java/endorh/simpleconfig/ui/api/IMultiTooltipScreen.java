package endorh.simpleconfig.ui.api;

import endorh.simpleconfig.api.ui.math.Rectangle;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

/**
 * {@link Screen} supporting the queuing of tooltips.
 */
public interface IMultiTooltipScreen {
	/**
	 * Add a tooltip for this frame
	 */
	default void addTooltip(Tooltip tooltip) {
		getTooltips().add(tooltip);
	}
	
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
		  tooltips.stream().filter(t -> area.contains(t.getPoint())).toList();
		return tooltips.removeAll(removed);
	}
}
