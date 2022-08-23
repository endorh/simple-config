package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.gui.entries.NestedListListEntry;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Function;

public class EntryListFieldBuilder<V, E extends AbstractConfigListEntry<V>>
  extends ListFieldBuilder<V, NestedListListEntry<V, E>, EntryListFieldBuilder<V, E>> {
	
	protected Function<NestedListListEntry<V, E>, E> cellFactory;
	
	public EntryListFieldBuilder(
	  ConfigFieldBuilder builder, Component name, List<V> value,
	  Function<NestedListListEntry<V, E>, E> cellFactory
	) {
		super(NestedListListEntry.class, builder, name, value);
		this.cellFactory = cellFactory;
	}
	
	@Override protected NestedListListEntry<V, E> buildEntry() {
		return new NestedListListEntry<>(fieldNameKey, value, cellFactory);
	}
}
