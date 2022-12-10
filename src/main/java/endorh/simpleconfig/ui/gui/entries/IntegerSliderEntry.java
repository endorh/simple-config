package endorh.simpleconfig.ui.gui.entries;

import endorh.simpleconfig.api.entry.RangedEntryBuilder.InvertibleDouble2DoubleFunction;
import endorh.simpleconfig.api.ui.TextFormatter;
import endorh.simpleconfig.ui.hotkey.HotKeyActionType;
import endorh.simpleconfig.ui.hotkey.HotKeyActionTypes;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.minecraft.util.math.MathHelper.clamp;

@OnlyIn(Dist.CLIENT)
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
		textFieldEntry = textEntry;
		Stream.of(
		  HotKeyActionTypes.INT_ADD, HotKeyActionTypes.INT_ADD_CYCLE,
		  HotKeyActionTypes.INT_MUL, HotKeyActionTypes.INT_DIV
		).forEach(hotKeyActionTypes::add);
		initWidgets(new IntegerSliderEntry.SliderWidget(0, 0, 100, 24), textEntry);
	}
	
	private static boolean isFloating(HotKeyActionType<Integer, ?> type) {
		return type == HotKeyActionTypes.INT_MUL || type == HotKeyActionTypes.INT_DIV;
	}
	
	@Override public void setHotKeyActionType(HotKeyActionType<Integer, ?> type) {
		boolean prevFloating = isFloating(getHotKeyActionType());
		Float prev = prevFloating? getDisplayedFloat() : (Float) (float) getDisplayedValue();
		super.setHotKeyActionType(type);
		boolean floating = isFloating(type);
		if (floating) setDisplayedFloat(prev);
		if (floating != prevFloating) {
			textFieldEntry.setTextFormatter(TextFormatter.numeric(!floating));
			if (!floating) {
				Float f = getDisplayedFloat();
				setDisplayedValue((int) (f != null? f : getValue()));
			}
		}
	}
	
	@Override public void setHotKeyActionValue(Object value) {
		HotKeyActionType<Integer, ?> type = getHotKeyActionType();
		if (isFloating(type)) {
			setDisplayedFloat(value);
		} else super.setHotKeyActionValue(value);
	}
	
	@Override public Object getHotKeyActionValue() {
		return isFloating(getHotKeyActionType())? getDisplayedFloat() : super.getHotKeyActionValue();
	}
	
	private Float getDisplayedFloat() {
		try {
			return Float.parseFloat(textFieldEntry.textFieldWidget.getText());
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	private void setDisplayedFloat(Object d) {
		textFieldEntry.textFieldWidget.setText(d != null? String.valueOf(d) : "");
	}
	
	public class SliderWidget extends SliderListEntry<Integer>.SliderWidget {
		public SliderWidget(int x, int y, int width, int height) {
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
			int prev = toInt(getSliderValue()), vv = prev;
			while (vv == prev) {
				v += step;
				if (v <= 0) return 0;
				if (v >= 1) return 1;
				vv = toInt(map.applyAsDouble(v));
			}
			double r = clamp(map.inverse(toDouble(vv)), 0, 1);
			return toInt(map.applyAsDouble(r)) == vv? r : v;
		}
		
		public int toInt(double v) {
			return (int) round(sliderMin + (sliderMax - sliderMin) * v);
		}
		
		public double toDouble(int v) {
			return (double) (v - sliderMin) / (double) (sliderMax - sliderMin);
		}
		
		@Override public Integer getValue() {
			return toInt(getSliderValue());
		}
		
		@Override public void setValue(Integer v) {
			setSliderValue(toDouble(v));
		}
	}
}
