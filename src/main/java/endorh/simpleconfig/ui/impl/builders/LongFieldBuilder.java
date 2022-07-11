package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.gui.entries.LongListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(value = Dist.CLIENT)
public class LongFieldBuilder
  extends RangedFieldBuilder<Long, LongListEntry, LongFieldBuilder> {
	
	public LongFieldBuilder(ConfigEntryBuilder builder, ITextComponent name, long value) {
		super(builder, name, value);
	}
	
	@Override
	@NotNull
	public LongListEntry buildEntry() {
		return new LongListEntry(
		  fieldNameKey, value
		);
	}
}

