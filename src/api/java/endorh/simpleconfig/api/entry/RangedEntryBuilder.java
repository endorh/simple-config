package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Contract;

import java.util.function.Function;

public interface RangedEntryBuilder<
  V extends Comparable<V>, Config, Gui,
  Self extends RangedEntryBuilder<V, Config, Gui, Self>
> extends ConfigEntryBuilder<V, Config, Gui, Self> {
	/**
	 * Set min (inclusive)
	 */
	@Contract(pure=true) Self min(V min);
	
	/**
	 * Set max (inclusive)
	 */
	@Contract(pure=true) Self max(V max);
	
	/**
	 * Set range (inclusive)
	 */
	@Contract(pure=true) Self range(V min, V max);
	
	/**
	 * Display as slider
	 */
	@Contract(pure=true) Self slider();
	
	/**
	 * Display or not as slider
	 */
	@Contract(pure=true) Self slider(boolean asSlider);
	
	/**
	 * Display as slider with given translation key as slider text.
	 */
	@Contract(pure=true) Self slider(String sliderTextTranslation);
	
	/**
	 * Display as slider with given text supplier.
	 */
	@Contract(pure=true) Self slider(Function<V, Component> sliderTextSupplier);
}
