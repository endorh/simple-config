package endorh.simpleconfig.core;

import net.minecraft.util.text.ITextComponent;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Interface for {@link AbstractConfigEntry} providing methods to set the tooltip
 * supplier accepting broader signatures
 * @param <V> The type of the entry
 * @param <Gui> The GUI type of the entry
 * @param <Self> The actual entry subtype to be returned by builder-like methods
 */
public interface ITooltipEntry<V, Gui, Self extends ITooltipEntry<V, Gui, Self>> {
	
	/**
	 * Set a tooltip supplier for this entry<br>
	 * Unlike {@link ITooltipEntry#tooltip(Function)}, this method
	 * takes the value used in the GUI directly, which might
	 * be of a different type than the entry itself<br><br>
	 * <b>Remember that all entries get automatically mapped optional tooltip
	 * translation keys</b>, by adding '.help' to their translation key. Use
	 * the automatic keys instead for simple tooltips.
	 * @param tooltipSupplier Maps GUI values to optional tooltip lines
	 */
	Self guiTooltip(Function<Gui, Optional<ITextComponent[]>> tooltipSupplier);
	
	/**
	 * Set a tooltip supplier for this entry<br>
	 * Unlike {@link ITooltipEntry#guiTooltip(Function)}, this method takes the
	 * value used in the config entry, once parsed from the GUI, since these
	 * types may be different<br><br>
	 * <b>Remember that all entries get automatically mapped optional tooltip
	 * translation keys</b>, by adding '.help' to their translation key. Use
	 * the automatic keys instead for simple tooltips.
	 * @param tooltipSupplier Maps values to optional tooltip lines
	 */
	Self tooltip(Function<V, Optional<ITextComponent[]>> tooltipSupplier);
	
	/**
	 * Set an optional tooltip supplier for this entry<br><br>
	 * <b>Remember that all entries get automatically mapped optional tooltip
	 * translation keys</b>, by adding '.help' to their translation key. Use
	 * the automatic keys instead for simple tooltips.
	 * @param tooltipSupplier Return optional tooltip lines
	 */
	default Self tooltip(Supplier<Optional<ITextComponent[]>> tooltipSupplier) {
		return tooltip(v -> tooltipSupplier.get());
	}
	
	/**
	 * Set a tooltip for this entry<br><br>
	 * <b>Remember that all entries get automatically mapped optional tooltip
	 * translation keys</b>, by adding '.help' to their translation key. Use
	 * the automatic keys instead for simple tooltips.
	 * @param tooltipSupplier Return optional tooltip lines
	 */
	default Self tooltip(ITextComponent[] tooltipSupplier) {
		return tooltip(v -> Optional.of(tooltipSupplier));
	}
}
