package endorh.simpleconfig.clothconfig2.gui.entries;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static java.lang.Math.*;

@OnlyIn(value = Dist.CLIENT)
public class IntegerSliderEntry extends SliderListEntry<Integer> {
	
	public IntegerSliderEntry(
	  ITextComponent fieldName,
	  int min, int max, int value
	) {
		super(
		  fieldName, min, max, value,
		  v -> new TranslationTextComponent(
		    "simpleconfig.format.slider",
		    String.format("%d", v)));
		final IntegerListEntry textEntry =
		  new IntegerListEntry(StringTextComponent.EMPTY, value);
		textEntry.setMinimum(min);
		textEntry.setMaximum(max);
		textEntry.setChildSubEntry(true);
		this.textFieldEntry = textEntry;
		initWidgets(new IntegerSliderEntry.SliderWidget(0, 0, 100, 24), textEntry);
	}
	
	public class SliderWidget extends SliderListEntry<Integer>.SliderWidget {
		public SliderWidget(
		  int x, int y, int width, int height
		) {
			super(x, y, width, height);
		}
		
		@Override public double getStep(boolean left) {
			setValue(getValue());
			final double step = super.getStep(left);
			return copySign(max(abs(step), 1.001D / (max - min)), step);
		}
		
		@Override public Integer getValue() {
			return (int) round(min + ((max - min) * value));
		}
		
		@Override public void setValue(Integer v) {
			value = (double) (v - min) / (double) (max - min);
		}
	}
}

