package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.gui.entries.LongSliderEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(value = Dist.CLIENT)
public class LongSliderBuilder extends SliderFieldBuilder<Long, LongSliderEntry, LongSliderBuilder> {
	
	public LongSliderBuilder(
	  ConfigEntryBuilder builder, ITextComponent name, long value, long min, long max
	) {
		super(LongSliderEntry.class, builder, name, value, min, max);
	}
	
	@Override @NotNull public LongSliderEntry buildEntry() {
		return new LongSliderEntry(fieldNameKey, min, max, value);
	}
}

