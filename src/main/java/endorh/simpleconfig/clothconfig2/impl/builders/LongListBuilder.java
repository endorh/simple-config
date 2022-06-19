package endorh.simpleconfig.clothconfig2.impl.builders;

import endorh.simpleconfig.clothconfig2.api.ConfigEntryBuilder;
import endorh.simpleconfig.clothconfig2.gui.entries.LongListListEntry;
import endorh.simpleconfig.clothconfig2.gui.entries.LongListListEntry.LongListCell;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@OnlyIn(value = Dist.CLIENT)
public class LongListBuilder
  extends RangedListFieldBuilder<Long, LongListCell, LongListListEntry, LongListBuilder> {
	
	public LongListBuilder(ConfigEntryBuilder builder, ITextComponent name, List<Long> value) {
		super(builder, name, value);
	}
	
	@Override @NotNull public LongListListEntry buildEntry() {
		return new LongListListEntry(this.fieldNameKey, this.value);
	}
}

