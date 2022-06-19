package endorh.simpleconfig.clothconfig2.gui.entries;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static java.lang.Math.round;

@OnlyIn(value = Dist.CLIENT)
public class LongSliderEntry extends SliderListEntry<Long> {
	
	public LongSliderEntry(
	  ITextComponent fieldName,
	  long min, long max, long value
	) {
		super(
		  fieldName, min, max, value,
		  v -> new TranslationTextComponent(
		    "simpleconfig.format.slider",
		    String.format("%d", v)));
		final LongListEntry textEntry =
		  new LongListEntry(StringTextComponent.EMPTY, value);
		textEntry.setMinimum(min);
		textEntry.setMaximum(max);
		textEntry.setChild(true);
		initWidgets(new SliderWidget(0, 0, 100, 24), textEntry);
	}
	
	public class SliderWidget extends SliderListEntry<Long>.SliderWidget {
		public SliderWidget(
		  int x, int y, int width, int height
		) {
			super(x, y, width, height);
		}
		
		@Override public Long getValue() {
			return round(min + ((max - min) * sliderValue));
		}
		
		@Override public void setValue(Long value) {
			sliderValue = (double) (value - min) / (double) (max - min);
		}
	}
}

