package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.gui.entries.LongListEntry;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(value = Dist.CLIENT)
public class LongFieldBuilder
  extends RangedFieldBuilder<Long, LongListEntry, LongFieldBuilder> {
	
	public LongFieldBuilder(ConfigFieldBuilder builder, Component name, long value) {
		super(LongListEntry.class, builder, name, value);
	}
	
	@Override
	@NotNull
	public LongListEntry buildEntry() {
		return new LongListEntry(
		  fieldNameKey, value
		);
	}
}

