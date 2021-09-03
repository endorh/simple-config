package endorh.simple_config.clothconfig2.gui.entries;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class FloatSliderEntry extends SliderListEntry<Float> {
	
	public FloatSliderEntry(
	  ITextComponent fieldName,
	  float min, float max, float value
	) {
		super(
		  fieldName, min, max, value,
		  v -> new TranslationTextComponent(
		    "simple-config.config.format.slider",
		    String.format("%5.2f", v)));
		final FloatListEntry textEntry =
		  new FloatListEntry(StringTextComponent.EMPTY, value);
		textEntry.setMinimum(min);
		textEntry.setMaximum(max);
		textEntry.setChild(true);
		initWidgets(new SliderWidget(0, 0, 100, 24), textEntry);
	}
	
	public class SliderWidget extends SliderListEntry<Float>.SliderWidget {
		public SliderWidget(
		  int x, int y, int width, int height
		) {
			super(x, y, width, height);
		}
		
		@Override public Float getValue() {
			return min + (float) ((max - min) * sliderValue);
		}
		
		@Override public void setValue(Float value) {
			sliderValue = (value - min) / (max - min);
		}
	}
}
