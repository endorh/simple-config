package endorh.simple_config.clothconfig2.impl.builders;

import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.clothconfig2.gui.entries.AbstractListListEntry;
import endorh.simple_config.clothconfig2.gui.entries.AbstractListListEntry.AbstractListCell;
import endorh.simple_config.clothconfig2.gui.entries.BaseListCell;
import endorh.simple_config.clothconfig2.gui.entries.IRangedEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@OnlyIn(Dist.CLIENT) public abstract class RangedListFieldBuilder<
  V extends Comparable<V>, C extends AbstractListCell<V, C, Entry>,
  Entry extends AbstractListListEntry<V, C, Entry> & IRangedEntry<V>,
  Self extends ListFieldBuilder<V, Entry, Self>>
  extends ListFieldBuilder<V, Entry, Self> {
	
	V min;
	V max;
	
	protected RangedListFieldBuilder(ConfigEntryBuilder builder, ITextComponent name, List<V> value) {
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
