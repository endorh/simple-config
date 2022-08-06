package endorh.simpleconfig.core;

import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.Contract;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Interface for {@link AbstractConfigEntry} providing methods to set the tooltip
 * supplier accepting broader signatures
 * @param <V> The type of the entry
 * @param <Gui> The GUI type of the entry
 * @param <Self> The actual entry subtype to be returned by builder-like methods
 */
public interface ITooltipEntryBuilder<V, Gui, Self extends ITooltipEntryBuilder<V, Gui, Self>> {
	
	/**
	 * Set a tooltip supplier for this entry<br>
	 * Unlike {@link ITooltipEntryBuilder#tooltip(Function)}, this method
	 * takes the value used in the GUI directly, which might
	 * be of a different type than the entry itself<br><br>
	 * <b>Remember that all entries get automatically mapped optional tooltip
	 * translation keys</b>, by adding '.help' to their translation key. Use
	 * the automatic keys instead for simple tooltips.
	 * @param tooltipSupplier Maps GUI values to optional tooltip lines
	 */
	@Contract(pure=true) Self guiTooltip(Function<Gui, List<ITextComponent>> tooltipSupplier);
	
	/**
	 * Set a tooltip supplier for this entry<br>
	 * Unlike {@link ITooltipEntryBuilder#guiTooltip(Function)}, this method takes the
	 * value used in the config entry, once parsed from the GUI, since these
	 * types may be different<br><br>
	 * <b>Remember that all entries get automatically mapped optional tooltip
	 * translation keys</b>, by adding '.help' to their translation key. Use
	 * the automatic keys instead for simple tooltips.
	 * @param tooltipSupplier Maps values to optional tooltip lines
	 */
	@Contract(pure=true) Self tooltip(Function<V, List<ITextComponent>> tooltipSupplier);
	
	/**
	 * Set an optional tooltip supplier for this entry<br><br>
	 * <b>Remember that all entries get automatically mapped optional tooltip
	 * translation keys</b>, by adding '.help' to their translation key. Use
	 * the automatic keys instead for simple tooltips.
	 * @param tooltipSupplier Return optional tooltip lines
	 */
	@Contract(pure=true) default Self tooltip(Supplier<List<ITextComponent>> tooltipSupplier) {
		return tooltip(v -> tooltipSupplier.get());
	}
	
	/**
	 * Set a tooltip for this entry<br><br>
	 * <b>Remember that all entries get automatically mapped optional tooltip
	 * translation keys</b>, by adding '.help' to their translation key. Use
	 * the automatic keys instead for simple tooltips.
	 * @param tooltip Return optional tooltip lines
	 */
	@Contract(pure=true) default Self tooltip(List<ITextComponent> tooltip) {
		return tooltip(v -> tooltip);
	}
}
