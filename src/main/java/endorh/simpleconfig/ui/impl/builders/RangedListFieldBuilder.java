package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.gui.entries.AbstractListListEntry;
import endorh.simpleconfig.ui.gui.entries.AbstractListListEntry.AbstractListCell;
import endorh.simpleconfig.ui.gui.entries.IRangedEntry;
import net.minecraft.network.chat.Component;
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
	
	protected RangedListFieldBuilder(
	  Class<?> entryClass, ConfigFieldBuilder builder, Component name, List<V> value
	) {
		super(entryClass, builder, name, value);
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
