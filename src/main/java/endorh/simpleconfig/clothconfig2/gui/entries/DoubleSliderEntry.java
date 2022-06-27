package endorh.simpleconfig.clothconfig2.gui.entries;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DoubleSliderEntry extends SliderListEntry<Double> {
	public DoubleSliderEntry(
	  ITextComponent fieldName,
	  double min, double max, double value
	) {
		super(
		  fieldName, min, max, value,
		  v -> new TranslationTextComponent(
		    "simpleconfig.format.slider",
		    String.format("%5.2f", v)));
		final DoubleListEntry textEntry =
		  new DoubleListEntry(StringTextComponent.EMPTY, value);
		textEntry.setMinimum(min);
		textEntry.setMaximum(max);
		textEntry.setChildSubEntry(true);
		initWidgets(new DoubleSliderEntry.SliderWidget(0, 0, 100, 24), textEntry);
	}
	
	public class SliderWidget extends SliderListEntry<Double>.SliderWidget {
		public SliderWidget(
		  int x, int y, int width, int height
		) {
			super(x, y, width, height);
		}
		
		@Override public Double getValue() {
			return min + ((max - min) * value);
		}
		
		@Override public void setValue(final Double v) {
			value = (v - min) / (max - min);
		}
	}
}
