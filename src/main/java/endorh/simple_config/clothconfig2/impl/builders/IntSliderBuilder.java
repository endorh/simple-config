package endorh.simple_config.clothconfig2.impl.builders;

import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.clothconfig2.gui.entries.IntegerSliderEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(value = Dist.CLIENT)
public class IntSliderBuilder extends SliderFieldBuilder<Integer, IntegerSliderEntry, IntSliderBuilder> {
		
	public IntSliderBuilder(
	  ConfigEntryBuilder builder, ITextComponent name, int value, int min, int max
	) {
		super(builder, name, value, min, max);
	}
	
	@Override @NotNull public IntegerSliderEntry buildEntry() {
		return new IntegerSliderEntry(fieldNameKey, min, max, value);
	}
}

