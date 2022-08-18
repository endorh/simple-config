package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.gui.entries.FloatSliderEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
 public class FloatSliderBuilder extends SliderFieldBuilder<Float, FloatSliderEntry, FloatSliderBuilder> {
	
	public FloatSliderBuilder(ConfigFieldBuilder builder, ITextComponent name, float value, float min, float max) {
		super(FloatSliderEntry.class, builder, name, value, min, max);
	}
	
	@Override @NotNull public FloatSliderEntry buildEntry() {
		return new FloatSliderEntry(fieldNameKey, min, max, value);
	}
}

