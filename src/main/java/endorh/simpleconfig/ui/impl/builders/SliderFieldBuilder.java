package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.gui.entries.SliderListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public abstract class SliderFieldBuilder<
  V extends Comparable<V>, Entry extends SliderListEntry<V>,
  Self extends SliderFieldBuilder<V, Entry, Self>> extends FieldBuilder<V, Entry, Self> {
	
	protected final V min;
	protected final V max;
	protected Function<V, ITextComponent> textGetter = null;
	
	public SliderFieldBuilder(
	  Class<?> entryClass, ConfigFieldBuilder builder, ITextComponent name, V value, V min, V max
	) {
		super(entryClass, builder, name, Objects.requireNonNull(value));
		this.min = Objects.requireNonNull(min);
		this.max = Objects.requireNonNull(max);
		if (min.compareTo(value) > 0 || value.compareTo(max) > 0)
			throw new IllegalArgumentException("Value not within bounds");
	}
	
	public Self setTextGetter(
	  Function<V, ITextComponent> textGetter
	) {
		this.textGetter = textGetter;
		return self();
	}
	
	@Override public @NotNull Entry build() {
		final Entry entry = super.build();
		entry.setMin(min);
		entry.setMax(max);
		if (textGetter != null)
			entry.setTextGetter(textGetter);
		return entry;
	}
}
