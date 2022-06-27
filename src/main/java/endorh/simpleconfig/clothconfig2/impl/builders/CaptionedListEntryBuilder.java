package endorh.simpleconfig.clothconfig2.impl.builders;

import endorh.simpleconfig.clothconfig2.api.AbstractConfigListEntry;
import endorh.simpleconfig.clothconfig2.api.ConfigEntryBuilder;
import endorh.simpleconfig.clothconfig2.api.IChildListEntry;
import endorh.simpleconfig.clothconfig2.gui.entries.AbstractListListEntry;
import endorh.simpleconfig.clothconfig2.gui.entries.CaptionedListListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class CaptionedListEntryBuilder<V, E extends AbstractListListEntry<V, ?, E>,
  C, CE extends AbstractConfigListEntry<C> & IChildListEntry>
  extends FieldBuilder<Pair<C, List<V>>, CaptionedListListEntry<V, E, C, CE>,
  CaptionedListEntryBuilder<V, E, C, CE>> {
	protected E listEntry;
	protected CE captionEntry;
	
	public CaptionedListEntryBuilder(
	  ConfigEntryBuilder builder, ITextComponent name, Pair<C, List<V>> value,
	  E listEntry, CE captionEntry
	) {
		super(builder, name, value);
		this.listEntry = listEntry;
		this.captionEntry = captionEntry;
	}
	
	@Override protected CaptionedListListEntry<V, E, C, CE> buildEntry() {
		final CaptionedListListEntry<V, E, C, CE> entry =
		  new CaptionedListListEntry<>(fieldNameKey, listEntry, captionEntry);
		entry.setValue(value);
		entry.setOriginal(value);
		entry.setDefaultValue(defaultValue);
		entry.setDisplayedValue(value);
		entry.setDefaultValue(defaultValue);
		return entry;
	}
}
