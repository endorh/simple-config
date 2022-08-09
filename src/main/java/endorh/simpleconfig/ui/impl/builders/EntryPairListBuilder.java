package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.gui.entries.EntryPairListListEntry;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.function.Function;

public class EntryPairListBuilder<
  K, V, KE extends AbstractConfigListEntry<K> & IChildListEntry,
  VE extends AbstractConfigListEntry<V>>
  extends ListFieldBuilder<Pair<K, V>, EntryPairListListEntry<K, V, KE, VE>,
  EntryPairListBuilder<K, V, KE, VE>> {
	
	protected Function<EntryPairListListEntry<K, V, KE, VE>, Pair<KE, VE>> cellFactory;
	protected boolean ignoreOrder = false;
	
	public EntryPairListBuilder(
	  ConfigEntryBuilder builder, ITextComponent name, List<Pair<K, V>> value,
	  Function<EntryPairListListEntry<K, V, KE, VE>, Pair<KE, VE>> cellFactory
	) {
		super(EntryPairListListEntry.class, builder, name, value);
		this.cellFactory = cellFactory;
	}
	
	public EntryPairListBuilder<K, V, KE, VE> setIgnoreOrder(boolean ignoreOrder) {
		this.ignoreOrder = ignoreOrder;
		return self();
	}
	
	@Override protected EntryPairListListEntry<K, V, KE, VE> buildEntry() {
		return new EntryPairListListEntry<>(fieldNameKey, value, cellFactory, ignoreOrder);
	}
}
