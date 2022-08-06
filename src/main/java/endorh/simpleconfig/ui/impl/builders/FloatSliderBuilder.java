package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.gui.entries.FloatSliderEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
 public class FloatSliderBuilder extends SliderFieldBuilder<Float, FloatSliderEntry, FloatSliderBuilder> {
	
	public FloatSliderBuilder(ConfigEntryBuilder builder, ITextComponent name, float value, float min, float max) {
		super(builder, name, value, min, max);
	}
	
	@Override @NotNull public FloatSliderEntry buildEntry() {
		return new FloatSliderEntry(fieldNameKey, min, max, value);
	}
}

