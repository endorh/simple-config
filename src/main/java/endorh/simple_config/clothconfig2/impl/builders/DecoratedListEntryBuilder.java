package endorh.simple_config.clothconfig2.impl.builders;

import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.clothconfig2.api.IChildListEntry;
import endorh.simple_config.clothconfig2.gui.entries.AbstractListListEntry;
import endorh.simple_config.clothconfig2.gui.entries.DecoratedListEntry;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class DecoratedListEntryBuilder<V, E extends AbstractListListEntry<V, ?, E>,
  C, CE extends AbstractConfigListEntry<C> & IChildListEntry>
  extends FieldBuilder<Pair<C, List<V>>, DecoratedListEntry<V, E, C, CE>,
  DecoratedListEntryBuilder<V, E, C, CE>> {
	protected E listEntry;
	protected CE captionEntry;
	
	public DecoratedListEntryBuilder(
	  ConfigEntryBuilder builder, ITextComponent name, Pair<C, List<V>> value,
	  E listEntry, CE captionEntry
	) {
		super(builder, name, value);
		this.listEntry = listEntry;
		this.captionEntry = captionEntry;
	}
	
	@Override protected DecoratedListEntry<V, E, C, CE> buildEntry() {
		final DecoratedListEntry<V, E, C, CE> entry =
		  new DecoratedListEntry<>(fieldNameKey, listEntry, captionEntry);
		entry.setValue(value);
		listEntry.setOriginal(value.getValue());
		entry.setDefaultValue(defaultValue);
		listEntry.setDefaultValue(() -> defaultValue.get().getValue());
		captionEntry.setDefaultValue(() -> defaultValue.get().getKey());
		return entry;
	}
}
