package endorh.simpleconfig.clothconfig2.impl.builders;

import endorh.simpleconfig.clothconfig2.api.AbstractConfigListEntry;
import endorh.simpleconfig.clothconfig2.api.ConfigEntryBuilder;
import endorh.simpleconfig.clothconfig2.api.IChildListEntry;
import endorh.simpleconfig.clothconfig2.gui.Icon;
import endorh.simpleconfig.clothconfig2.gui.SimpleConfigIcons;
import endorh.simpleconfig.clothconfig2.gui.entries.RangeListEntry;
import endorh.simpleconfig.core.AbstractRange;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RangeListEntryBuilder<
  V extends Comparable<V>, R extends AbstractRange<V, R>,
  E extends AbstractConfigListEntry<V> & IChildListEntry
> extends FieldBuilder<R, RangeListEntry<V, R, E>, RangeListEntryBuilder<V, R, E>> {
	protected final E minEntry;
	protected final E maxEntry;
	protected boolean minExclusivenessEditable = false;
	protected boolean maxExclusivenessEditable = false;
	protected @NotNull Icon comparisonIcon = SimpleConfigIcons.Entries.LESS_EQUAL;
	protected @Nullable Icon middleIcon = null;
	
	public RangeListEntryBuilder(
	  ConfigEntryBuilder builder, ITextComponent name, R value, FieldBuilder<V, E, ?> entryBuilder
	) {
		this(builder, name, value, entryBuilder.build(), entryBuilder.build());
	}
	
	public RangeListEntryBuilder(
	  ConfigEntryBuilder builder, ITextComponent name, R value, E minEntry, E maxEntry
	) {
		super(builder, name, value);
		this.minEntry = minEntry;
		this.maxEntry = maxEntry;
	}
	
	public RangeListEntryBuilder<V, R, E> withMiddleIcon(@Nullable Icon middleIcon) {
		this.middleIcon = middleIcon;
		return self();
	}
	
	public RangeListEntryBuilder<V, R, E> withComparisonIcon(@NotNull Icon icon) {
		this.comparisonIcon = icon;
		return self();
	}
	
	public RangeListEntryBuilder<V, R, E> withMinExclusivenessEditable(boolean editable) {
		this.minExclusivenessEditable = editable;
		return self();
	}
	
	public RangeListEntryBuilder<V, R, E> withMaxExclusivenessEditable(boolean editable) {
		this.maxExclusivenessEditable = editable;
		return self();
	}
	
	public RangeListEntryBuilder<V, R, E> withExclusivenessEditable(boolean min, boolean max) {
		return withMinExclusivenessEditable(min).withMaxExclusivenessEditable(max);
	}
	
	public RangeListEntryBuilder<V, R, E> withExclusivenessEditable(boolean editable) {
		return withExclusivenessEditable(editable, editable);
	}
	
	@Override protected RangeListEntry<V, R, E> buildEntry() {
		return new RangeListEntry<>(fieldNameKey, value, minEntry, maxEntry);
	}
	
	@Override public @NotNull RangeListEntry<V, R, E> build() {
		RangeListEntry<V, R, E> entry = super.build();
		entry.setMiddleIcon(middleIcon);
		entry.setComparisonIcon(comparisonIcon);
		entry.setMinExclusivenessEditable(minExclusivenessEditable);
		entry.setMaxExclusivenessEditable(maxExclusivenessEditable);
		return entry;
	}
}
