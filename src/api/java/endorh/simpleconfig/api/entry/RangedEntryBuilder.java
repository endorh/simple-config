package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public interface RangedEntryBuilder<
  V extends Comparable<V>, Config, Gui,
  Self extends RangedEntryBuilder<V, Config, Gui, Self>
> extends ConfigEntryBuilder<@NotNull V, Config, Gui, Self> {
	/**
	 * Set min (inclusive)
	 */
	@Contract(pure=true) @NotNull Self min(V min);
	
	/**
	 * Set max (inclusive)
	 */
	@Contract(pure=true) @NotNull Self max(V max);
	
	/**
	 * Set range (inclusive)
	 */
	@Contract(pure=true) @NotNull Self range(V min, V max);
	
	/**
	 * Display as slider
	 */
	@Contract(pure=true) @NotNull Self slider();
	
	/**
	 * Display or not as slider
	 */
	@Contract(pure=true) @NotNull Self slider(boolean asSlider);
	
	/**
	 * Display as slider with given translation key as slider text.
	 */
	@Contract(pure=true) @NotNull Self slider(String sliderTextTranslation);
	
	/**
	 * Display as slider with given text supplier.
	 */
	@Contract(pure=true) @NotNull Self slider(Function<V, Component> sliderTextSupplier);
}
