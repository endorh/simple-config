package endorh.simple_config.clothconfig2.impl.builders;

import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.clothconfig2.gui.entries.DoubleSliderEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class DoubleSliderBuilder extends SliderFieldBuilder<Double, DoubleSliderEntry, DoubleSliderBuilder> {
	
	@Internal public DoubleSliderBuilder(ConfigEntryBuilder builder, ITextComponent name, double value, double min, double max) {
		super(builder, name, value, min, max);
	}
	
	@NotNull public DoubleSliderEntry buildEntry() {
		return new DoubleSliderEntry(fieldNameKey, min, max, value);
	}
}

