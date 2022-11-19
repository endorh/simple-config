package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.api.entry.RangedEntryBuilder.InvertibleDouble2DoubleFunction;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.gui.entries.SliderListEntry;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public abstract class SliderFieldBuilder<
  V extends Comparable<V>, Entry extends SliderListEntry<V>,
  Self extends SliderFieldBuilder<V, Entry, Self>> extends FieldBuilder<V, Entry, Self> {
	
	protected final V min;
	protected final V max;
	protected @Nullable V sliderMin;
	protected @Nullable V sliderMax;
	protected Function<V, Component> textGetter = null;
	protected @Nullable InvertibleDouble2DoubleFunction sliderMap = null;
	
	public SliderFieldBuilder(
	  Class<?> entryClass, ConfigFieldBuilder builder, Component name, V value, V min, V max
	) {
		super(entryClass, builder, name, Objects.requireNonNull(value));
		this.min = Objects.requireNonNull(min);
		this.max = Objects.requireNonNull(max);
		if (min.compareTo(value) > 0 || value.compareTo(max) > 0)
			throw new IllegalArgumentException("Value not within bounds");
	}
	
	public Self setSliderMin(@Nullable V sliderMin) {
		this.sliderMin = sliderMin;
		return self();
	}
	
	public Self setSliderMax(@Nullable V sliderMax) {
		this.sliderMax = sliderMax;
		return self();
	}
	
	public Self setSliderMap(@Nullable InvertibleDouble2DoubleFunction map) {
		sliderMap = map;
		return self();
	}
	
	public Self setTextGetter(
	  Function<V, Component> textGetter
	) {
		this.textGetter = textGetter;
		return self();
	}
	
	@Override public @NotNull Entry build() {
		final Entry entry = super.build();
		entry.setMin(min);
		entry.setMax(max);
		if (sliderMin != null) entry.setSliderMin(sliderMin);
		if (sliderMax != null) entry.setSliderMax(sliderMax);
		if (sliderMap != null) entry.setSliderMap(sliderMap);
		if (textGetter != null) entry.setTextGetter(textGetter);
		return entry;
	}
}
