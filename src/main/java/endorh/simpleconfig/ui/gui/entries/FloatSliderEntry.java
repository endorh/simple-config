package endorh.simpleconfig.ui.gui.entries;

import endorh.simpleconfig.ui.hotkey.HotKeyActionTypes;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public class FloatSliderEntry extends SliderListEntry<Float> {
	
	public FloatSliderEntry(
	  Component fieldName,
	  float min, float max, float value
	) {
		super(
		  fieldName, min, max, value,
		  v -> Component.translatable("simpleconfig.format.slider",
		    String.format("%5.2f", v)));
		final FloatListEntry textEntry =
		  new FloatListEntry(Component.empty(), value);
		textEntry.setMinimum(min);
		textEntry.setMaximum(max);
		textEntry.setChildSubEntry(true);
		Stream.of(
		  HotKeyActionTypes.FLOAT_ADD, HotKeyActionTypes.FLOAT_ADD_CYCLE,
		  HotKeyActionTypes.FLOAT_MULTIPLY, HotKeyActionTypes.FLOAT_DIVIDE
		).forEach(hotKeyActionTypes::add);
		initWidgets(new SliderWidget(0, 0, 100, 24), textEntry);
	}
	
	public class SliderWidget extends SliderListEntry<Float>.SliderWidget {
		public SliderWidget(
		  int x, int y, int width, int height
		) {
			super(x, y, width, height);
		}
		
		@Override public Float getValue() {
			return sliderMin + (float) ((sliderMax - sliderMin) * getSliderValue());
		}
		
		@Override public void setValue(final Float v) {
			setSliderValue((v - sliderMin) / (sliderMax - sliderMin));
		}
	}
}
