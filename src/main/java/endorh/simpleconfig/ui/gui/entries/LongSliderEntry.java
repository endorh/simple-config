package endorh.simpleconfig.ui.gui.entries;

import endorh.simpleconfig.api.entry.RangedEntryBuilder.InvertibleDouble2DoubleFunction;
import endorh.simpleconfig.ui.hotkey.HotKeyActionTypes;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.minecraft.util.math.MathHelper.clamp;

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
		textEntry.setChildSubEntry(true);
		Stream.of(HotKeyActionTypes.LONG_ADD, HotKeyActionTypes.LONG_ADD_CYCLE)
			 .forEach(hotKeyActionTypes::add);
		initWidgets(new SliderWidget(0, 0, 100, 24), textEntry);
	}
	
	public class SliderWidget extends SliderListEntry<Long>.SliderWidget {
		public SliderWidget(
		  int x, int y, int width, int height
		) {
			super(x, y, width, height);
		}
		
		@Override public double getStep(boolean left) {
			setValue(getValue());
			return super.getStep(left);
		}
		
		@Override public double applyStep(double step) {
			InvertibleDouble2DoubleFunction map = sliderMap;
			if (map == null) return super.applyStep(copySign(
			  max(abs(step), 1.001D / (sliderMax - sliderMin)), step));
			double v = sliderValue + step;
			if (v <= 0) return 0;
			if (v >= 1) return 1;
			long prev = toLong(getSliderValue()), vv = prev;
			while (vv == prev) {
				v += step;
				if (v <= 0) return 0;
				if (v >= 1) return 1;
				vv = toLong(map.applyAsDouble(v));
			}
			double r = clamp(map.inverse(toDouble(vv)), 0, 1);
			return toLong(map.applyAsDouble(r)) == vv? r : v;
		}
		
		public long toLong(double v) {
			return round(sliderMin + (sliderMax - sliderMin) * v);
		}
		
		public double toDouble(long v) {
			return (double) (v - sliderMin) / (double) (sliderMax - sliderMin);
		}
		
		@Override public Long getValue() {
			return toLong(getSliderValue());
		}
		
		@Override public void setValue(Long v) {
			setSliderValue(toDouble(v));
		}
	}
}

