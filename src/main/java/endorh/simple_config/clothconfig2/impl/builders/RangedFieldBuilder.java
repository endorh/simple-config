package endorh.simple_config.clothconfig2.impl.builders;

import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.clothconfig2.gui.entries.IRangedEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public abstract class RangedFieldBuilder<V extends Comparable<V>,
  Entry extends AbstractConfigListEntry<V> & IRangedEntry<V>,
  Self extends RangedFieldBuilder<V, Entry, Self>> extends FieldBuilder<V, Entry, Self> {
	V min;
	V max;
	
	protected RangedFieldBuilder(
	  ConfigEntryBuilder builder, ITextComponent name, V value
	) {
		super(builder, name, value);
	}
	
	public Self setMin(V min) {
		this.min = min;
		return self();
	}
	
	public Self setMax(V max) {
		this.max = max;
		return self();
	}
	
	@Override public @NotNull Entry build() {
		final Entry entry = super.build();
		if (min != null)
			entry.setMinimum(min);
		if (max != null)
			entry.setMaximum(max);
		return entry;
	}
}
