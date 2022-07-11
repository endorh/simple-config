package endorh.simpleconfig.ui.gui.entries;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FloatSliderEntry extends SliderListEntry<Float> {
	
	public FloatSliderEntry(
	  ITextComponent fieldName,
	  float min, float max, float value
	) {
		super(
		  fieldName, min, max, value,
		  v -> new TranslationTextComponent(
		    "simpleconfig.format.slider",
		    String.format("%5.2f", v)));
		final FloatListEntry textEntry =
		  new FloatListEntry(StringTextComponent.EMPTY, value);
		textEntry.setMinimum(min);
		textEntry.setMaximum(max);
		textEntry.setChildSubEntry(true);
		initWidgets(new SliderWidget(0, 0, 100, 24), textEntry);
	}
	
	public class SliderWidget extends SliderListEntry<Float>.SliderWidget {
		public SliderWidget(
		  int x, int y, int width, int height
		) {
			super(x, y, width, height);
		}
		
		@Override public Float getValue() {
			return min + (float) ((max - min) * value);
		}
		
		@Override public void setValue(final Float v) {
			value = (v - min) / (max - min);
		}
	}
}
