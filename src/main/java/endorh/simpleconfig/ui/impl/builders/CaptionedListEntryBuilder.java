package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.gui.entries.AbstractListListEntry;
import endorh.simpleconfig.ui.gui.entries.CaptionedListListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class CaptionedListEntryBuilder<
  V, E extends AbstractListListEntry<V, ?, E>, EB extends FieldBuilder<List<V>, E, ?>,
  C, CE extends AbstractConfigListEntry<C> & IChildListEntry, CEB extends FieldBuilder<C, CE, ?>
> extends FieldBuilder<
  Pair<C, List<V>>, CaptionedListListEntry<V, E, C, CE>,
  CaptionedListEntryBuilder<V, E, EB, C, CE, CEB>
> {
	protected EB listEntry;
	protected CEB captionEntry;
	
	public CaptionedListEntryBuilder(
	  ConfigEntryBuilder builder, ITextComponent name, Pair<C, List<V>> value,
	  EB listEntry, CEB captionEntry
	) {
		super(CaptionedListListEntry.class, builder, name, value);
		this.listEntry = listEntry;
		this.captionEntry = captionEntry;
	}
	
	@Override protected CaptionedListListEntry<V, E, C, CE> buildEntry() {
		final CaptionedListListEntry<V, E, C, CE> entry =
		  new CaptionedListListEntry<>(fieldNameKey, listEntry.build(), captionEntry.build());
		entry.setValue(value);
		entry.setOriginal(value);
		entry.setDefaultValue(defaultValue);
		entry.setDisplayedValue(value);
		entry.setDefaultValue(defaultValue);
		return entry;
	}
}
