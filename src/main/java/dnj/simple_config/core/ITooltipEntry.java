package dnj.simple_config.core;

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
	 * Unlike {@link ITooltipEntry#tooltipOpt(Function)}, this method
	 * takes the value used in the GUI directly, which might
	 * be of a different type than the entry itself<br><br>
	 * <b>Remember that all entries get automatically mapped optional tooltip
	 * translation keys</b>, by adding '.help' to their translation key. Use
	 * the automatic keys instead for simple tooltips.
	 * @param tooltipSupplier Maps GUI values to optional tooltip lines
	 */
	Self guiTooltipOpt(Function<Gui, Optional<ITextComponent[]>> tooltipSupplier);
	
	/**
	 * Set a single-line tooltip supplier for this entry<br>
	 * Unlike {@link ITooltipEntry#tooltipOpt1(Function)}, this method
	 * takes the value used in the GUI directly, which might be of
	 * a different type than the entry itself<br>
	 * To provide a multiline tooltip, use {@link ITooltipEntry#guiTooltipOpt(Function)}
	 * or its variants<br><br>
	 * <b>Remember that all entries get automatically mapped optional tooltip
	 * translation keys</b>, by adding '.help' to their translation key. Use
	 * the automatic keys instead for simple tooltips.
	 * @param tooltipSupplier Maps GUI values to an optional tooltip line
	 */
	default Self guiTooltipOpt1(Function<Gui, Optional<ITextComponent>> tooltipSupplier) {
		return guiTooltipOpt(v -> tooltipSupplier.apply(v).map(tc -> new ITextComponent[]{tc}));
	}
	
	/**
	 * Set a tooltip supplier for this entry<br>
	 * Unlike {@link ITooltipEntry#tooltip(Function)}, this method takes the
	 * value used in the GUI directly, which might be of a different type than
	 * the entry itself<br>
	 * Use {@link ITooltipEntry#guiTooltipOpt(Function)} to provide {@link Optional}
	 * return values instead of nullables<br><br>
	 * <b>Remember that all entries get automatically mapped optional tooltip
	 * translation keys</b>, by adding '.help' to their translation key. Use
	 * the automatic keys instead for simple tooltips.
	 * @param tooltipSupplier Maps GUI values to tooltip lines.
	 *                        If null is returned, no tooltip will be shown
	 */
	default Self guiTooltip(Function<Gui, ITextComponent[]> tooltipSupplier) {
		return guiTooltipOpt(v -> Optional.ofNullable(tooltipSupplier.apply(v)));
	}
	
	/**
	 * Set a single-line tooltip supplier for this entry<br>
	 * Unlike {@link ITooltipEntry#tooltip1(Function)}, this method takes the
	 * value used in the GUI directly, which might be of a different type than
	 * the entry itself<br>
	 * To provide a multiline tooltip, use {@link ITooltipEntry#guiTooltip(Function)}
	 * or its variants<br>
	 * Use {@link ITooltipEntry#guiTooltipOpt1(Function)} to provide {@link Optional}
	 * return values instead of nullables<br><br>
	 * <b>Remember that all entries get automatically mapped optional tooltip
	 * translation keys</b>, by adding '.help' to their translation key. Use
	 * the automatic keys instead for simple tooltips.
	 * @param tooltipSupplier Maps GUI values to a single-line tooltip.
	 *                        If null is returned, no tooltip will be shown
	 */
	default Self guiTooltip1(Function<Gui, ITextComponent> tooltipSupplier) {
		return guiTooltipOpt(v -> {
			final ITextComponent tc = tooltipSupplier.apply(v);
			return tc != null? Optional.of(new ITextComponent[]{tc}) : Optional.empty();
		});
	}
	
	/**
	 * Set a tooltip supplier for this entry<br>
	 * Unlike {@link ITooltipEntry#guiTooltipOpt(Function)}, this method takes the
	 * value used in the config entry, once parsed from the GUI, since these
	 * types may be different<br><br>
	 * <b>Remember that all entries get automatically mapped optional tooltip
	 * translation keys</b>, by adding '.help' to their translation key. Use
	 * the automatic keys instead for simple tooltips.
	 * @param tooltipSupplier Maps values to optional tooltip lines
	 */
	Self tooltipOpt(Function<V, Optional<ITextComponent[]>> tooltipSupplier);
	
	/**
	 * Set a single-line tooltip supplier for this entry<br>
	 * Unlike {@link ITooltipEntry#guiTooltipOpt1(Function)}, this method takes the
	 * value used in the config entry, once parsed from the GUI, since these
	 * types may be different<br>
	 * To provide a multiline tooltip, use {@link ITooltipEntry#tooltipOpt(Function)}
	 * or its variants<br><br>
	 * <b>Remember that all entries get automatically mapped optional tooltip
	 * translation keys</b>, by adding '.help' to their translation key. Use
	 * the automatic keys instead for simple tooltips.
	 * @param tooltipSupplier Maps values to an optional tooltip line
	 */
	default Self tooltipOpt1(Function<V, Optional<ITextComponent>> tooltipSupplier) {
		return tooltipOpt(v -> tooltipSupplier.apply(v).map(tc -> new ITextComponent[]{tc}));
	}
	
	/**
	 * Set a tooltip supplier for this entry<br>
	 * Unlike {@link ITooltipEntry#guiTooltip(Function)}, this method takes the
	 * value used in the config entry, once parsed from the GUI, since these
	 * types may be different<br>
	 * Use {@link ITooltipEntry#tooltipOpt(Function)} to provide {@link Optional}
	 * return values instead of nullables<br><br>
	 * <b>Remember that all entries get automatically mapped optional tooltip
	 * translation keys</b>, by adding '.help' to their translation key. Use
	 * the automatic keys instead for simple tooltips.
	 * @param tooltipSupplier Maps values to tooltip lines.
	 *                        If null is returned, no tooltip will be shown
	 */
	default Self tooltip(Function<V, ITextComponent[]> tooltipSupplier) {
		return tooltipOpt(v -> Optional.ofNullable(tooltipSupplier.apply(v)));
	}
	
	/**
	 * Set a single-line tooltip supplier for this entry<br>
	 * Unlike {@link ITooltipEntry#guiTooltip1(Function)}, this method takes the
	 * value used in the config entry, once parsed from the GUI, since these
	 * types may be different<br>
	 * To provide multiline tooltip suppliers, use {@link ITooltipEntry#guiTooltip(Function)}
	 * or its variants<br>
	 * Use {@link ITooltipEntry#tooltipOpt1(Function)} to provide {@link Optional} return
	 * values instead of nullables<br><br>
	 * <b>Remember that all entries get automatically mapped optional tooltip
	 * translation keys</b>, by adding '.help' to their translation key. Use
	 * the automatic keys instead for simple tooltips.
	 * @param tooltipSupplier Maps values to a single-line tooltip.
	 *                        If null is returned, no tooltip will be shown
	 */
	default Self tooltip1(Function<V, ITextComponent> tooltipSupplier) {
		return tooltipOpt(v -> {
			final ITextComponent tc = tooltipSupplier.apply(v);
			return tc != null? Optional.of(new ITextComponent[]{tc}) : Optional.empty();
		});
	}
	
	/**
	 * Set an optional tooltip supplier for this entry<br><br>
	 * <b>Remember that all entries get automatically mapped optional tooltip
	 * translation keys</b>, by adding '.help' to their translation key. Use
	 * the automatic keys instead for simple tooltips.
	 * @param tooltipSupplier Return optional tooltip lines
	 */
	default Self tooltipOpt(Supplier<Optional<ITextComponent[]>> tooltipSupplier) {
		return tooltipOpt(v -> tooltipSupplier.get());
	}
	
	/**
	 * Set an optional single-line tooltip supplier for this entry<br>
	 * To provide a multiline tooltip, use {@link ITooltipEntry#tooltipOpt(Supplier)}
	 * or its variants<br><br>
	 * <b>Remember that all entries get automatically mapped optional tooltip
	 * translation keys</b>, by adding '.help' to their translation key. Use
	 * the automatic keys instead for simple tooltips.
	 * @param tooltipSupplier Return an optional single-line tooltip
	 */
	default Self tooltipOpt1(Supplier<Optional<ITextComponent>> tooltipSupplier) {
		return tooltipOpt1(v -> tooltipSupplier.get());
	}
	
	/**
	 * Set a tooltip supplier for this entry<br><br>
	 * <b>Remember that all entries get automatically mapped optional tooltip
	 * translation keys</b>, by adding '.help' to their translation key. Use
	 * the automatic keys instead for simple tooltips.
	 * @param tooltipSupplier Return tooltip lines to display
	 */
	default Self tooltip(Supplier<ITextComponent[]> tooltipSupplier) {
		return tooltip(v -> tooltipSupplier.get());
	}
	
	/**
	 * Set a single-line tooltip supplier for this entry<br>
	 * To provide a multiline tooltip, use {@link ITooltipEntry#tooltip(Supplier)}
	 * or its variants<br><br>
	 * <b>Remember that all entries get automatically mapped optional tooltip
	 * translation keys</b>, by adding '.help' to their translation key. Use
	 * the automatic keys instead for simple tooltips.
	 * @param tooltipSupplier Return a single-line tooltip to display
	 */
	default Self tooltip1(Supplier<ITextComponent> tooltipSupplier) {
		return tooltip1(v -> tooltipSupplier.get());
	}
}
