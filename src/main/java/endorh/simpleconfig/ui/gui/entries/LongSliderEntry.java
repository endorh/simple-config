package endorh.simpleconfig.ui.gui.entries;

import endorh.simpleconfig.api.entry.RangedEntryBuilder.InvertibleDouble2DoubleFunction;
import endorh.simpleconfig.api.ui.TextFormatter;
import endorh.simpleconfig.ui.hotkey.HotKeyActionType;
import endorh.simpleconfig.ui.hotkey.HotKeyActionTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
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
		Stream.of(
		  HotKeyActionTypes.LONG_ADD, HotKeyActionTypes.LONG_ADD_CYCLE,
		  HotKeyActionTypes.LONG_MUL, HotKeyActionTypes.LONG_DIV
		).forEach(hotKeyActionTypes::add);
		initWidgets(new SliderWidget(0, 0, 100, 24), textEntry);
	}
	
	private static boolean isFloating(HotKeyActionType<Long, ?> type) {
		return type == HotKeyActionTypes.LONG_MUL || type == HotKeyActionTypes.LONG_DIV;
	}
	
	@Override public void setHotKeyActionType(HotKeyActionType<Long, ?> type) {
		boolean prevFloating = isFloating(getHotKeyActionType());
		Double prev = prevFloating? getDisplayedDouble() : (Double) (double) getDisplayedValue();
		super.setHotKeyActionType(type);
		boolean floating = isFloating(type);
		if (floating) setDisplayedDouble(prev);
		if (floating != prevFloating) {
			textFieldEntry.setTextFormatter(TextFormatter.numeric(!floating));
			if (!floating) {
				Double d = getDisplayedDouble();
				setDisplayedValue((long) (d != null? d : getValue()));
			}
		}
	}
	
	@Override public void setHotKeyActionValue(Object value) {
		HotKeyActionType<Long, ?> type = getHotKeyActionType();
		if (isFloating(type)) {
			setDisplayedDouble(value);
		} else super.setHotKeyActionValue(value);
	}
	
	@Override public Object getHotKeyActionValue() {
		return isFloating(getHotKeyActionType())? getDisplayedDouble() : super.getHotKeyActionValue();
	}
	
	private Double getDisplayedDouble() {
		try {
			return Double.parseDouble(textFieldEntry.textFieldWidget.getValue());
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	private void setDisplayedDouble(Object d) {
		textFieldEntry.textFieldWidget.setValue(d != null? String.valueOf(d) : "");
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
			double v = value + step;
			if (v <= 0) return 0;
			if (v >= 1) return 1;
			long prev = toLong(getSliderValue()), vv = prev;
			while (vv == prev) {
				v += step;
				if (v <= 0) return 0;
				if (v >= 1) return 1;
				vv = toLong(map.applyAsDouble(v));
			}
			double r = Mth.clamp(map.inverse(toDouble(vv)), 0, 1);
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
			setSliderValue((double) (v - sliderMin) / (double) (sliderMax - sliderMin));
		}
	}
}

