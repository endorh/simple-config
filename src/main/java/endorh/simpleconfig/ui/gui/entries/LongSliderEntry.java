package endorh.simpleconfig.ui.gui.entries;

import endorh.simpleconfig.ui.hotkey.HotKeyActionTypes;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.stream.Stream;

import static java.lang.Math.*;

@OnlyIn(value = Dist.CLIENT)
public class LongSliderEntry extends SliderListEntry<Long> {
	
	public LongSliderEntry(
	  Component fieldName,
	  long min, long max, long value
	) {
		super(
		  fieldName, min, max, value,
		  v -> Component.translatable("simpleconfig.format.slider",
		    String.format("%d", v)));
		final LongListEntry textEntry =
		  new LongListEntry(Component.empty(), value);
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
			final double step = super.getStep(left);
			return copySign(max(abs(step), 1.001D / (max - min)), step);
		}
		
		@Override public Long getValue() {
			return round(min + ((max - min) * value));
		}
		
		@Override public void setValue(Long v) {
			value = (double) (v - min) / (double) (max - min);
		}
	}
}

