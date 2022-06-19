package endorh.simpleconfig.clothconfig2.impl.builders;

import endorh.simpleconfig.clothconfig2.api.ConfigEntryBuilder;
import endorh.simpleconfig.clothconfig2.gui.entries.LongSliderEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(value = Dist.CLIENT)
public class LongSliderBuilder extends SliderFieldBuilder<Long, LongSliderEntry, LongSliderBuilder> {
	
	public LongSliderBuilder(
	  ConfigEntryBuilder builder, ITextComponent name, long value, long min, long max
	) {
		super(builder, name, value, min, max);
	}
	
	@Override @NotNull public LongSliderEntry buildEntry() {
		return new LongSliderEntry(fieldNameKey, min, max, value);
	}
}

