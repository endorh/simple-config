package endorh.simpleconfig.ui.gui.entries;

import endorh.simpleconfig.ui.hotkey.HotKeyActionTypes;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public class DoubleSliderEntry extends SliderListEntry<Double> {
	public DoubleSliderEntry(
	  Component fieldName,
	  double min, double max, double value
	) {
		super(
		  fieldName, min, max, value,
		  v -> Component.translatable("simpleconfig.format.slider",
		    String.format("%5.2f", v)));
		final DoubleListEntry textEntry = new DoubleListEntry(Component.empty(), value);
		textEntry.setMinimum(min);
		textEntry.setMaximum(max);
		textEntry.setChildSubEntry(true);
		Stream.of(
		  HotKeyActionTypes.DOUBLE_ADD, HotKeyActionTypes.DOUBLE_ADD_CYCLE,
		  HotKeyActionTypes.DOUBLE_MULTIPLY, HotKeyActionTypes.DOUBLE_DIVIDE
		).forEach(hotKeyActionTypes::add);
		initWidgets(new DoubleSliderEntry.SliderWidget(0, 0, 100, 24), textEntry);
	}
	
	public class SliderWidget extends SliderListEntry<Double>.SliderWidget {
		public SliderWidget(
		  int x, int y, int width, int height
		) {
			super(x, y, width, height);
		}
		
		@Override public Double getValue() {
			return sliderMin + (sliderMax - sliderMin) * getSliderValue();
		}
		
		@Override public void setValue(final Double v) {
			setSliderValue((v - sliderMin) / (sliderMax - sliderMin));
		}
	}
}
