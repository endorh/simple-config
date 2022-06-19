package endorh.simpleconfig.clothconfig2.impl.builders;

import endorh.simpleconfig.clothconfig2.api.AbstractConfigListEntry;
import endorh.simpleconfig.clothconfig2.api.ConfigEntryBuilder;
import endorh.simpleconfig.clothconfig2.gui.entries.NestedListListEntry;
import net.minecraft.util.text.ITextComponent;

import java.util.List;
import java.util.function.Function;

public class EntryListFieldBuilder<V, E extends AbstractConfigListEntry<V>>
  extends ListFieldBuilder<V, NestedListListEntry<V, E>, EntryListFieldBuilder<V, E>> {
	
	protected Function<NestedListListEntry<V, E>, E> cellFactory;
	
	public EntryListFieldBuilder(
	  ConfigEntryBuilder builder, ITextComponent name, List<V> value,
	  Function<NestedListListEntry<V, E>, E> cellFactory
	) {
		super(builder, name, value);
		this.cellFactory = cellFactory;
	}
	
	@Override protected NestedListListEntry<V, E> buildEntry() {
		return new NestedListListEntry<>(fieldNameKey, value, cellFactory);
	}
}
