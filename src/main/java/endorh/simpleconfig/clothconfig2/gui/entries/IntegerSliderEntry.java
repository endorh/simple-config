package endorh.simpleconfig.clothconfig2.gui.entries;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static java.lang.Math.round;

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
		textEntry.setChild(true);
		this.textFieldEntry = textEntry;
		initWidgets(new IntegerSliderEntry.SliderWidget(0, 0, 100, 24), textEntry);
	}
	
	public class SliderWidget extends SliderListEntry<Integer>.SliderWidget {
		public SliderWidget(
		  int x, int y, int width, int height
		) {
			super(x, y, width, height);
		}
		
		@Override public Integer getValue() {
			return (int) round(min + ((max - min) * sliderValue));
		}
		
		@Override public void setValue(Integer v) {
			sliderValue = (double) (v - min) / (double) (max - min);
		}
	}
}

