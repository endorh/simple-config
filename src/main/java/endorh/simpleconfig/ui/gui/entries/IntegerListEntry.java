package endorh.simpleconfig.ui.gui.entries;

import endorh.simpleconfig.api.ui.TextFormatter;
import endorh.simpleconfig.ui.hotkey.HotKeyActionType;
import endorh.simpleconfig.ui.hotkey.HotKeyActionTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public class IntegerListEntry extends TextFieldListEntry<Integer> implements IRangedEntry<Integer> {
	private int minimum = Integer.MIN_VALUE;
	private int maximum = Integer.MAX_VALUE;
	
	@Internal public IntegerListEntry(Component fieldName, Integer value) {
		super(fieldName, value, false);
		setTextFormatter(TextFormatter.numeric(true));
		Stream.of(
		  HotKeyActionTypes.INT_ADD, HotKeyActionTypes.INT_ADD_CYCLE,
		  HotKeyActionTypes.INT_MUL, HotKeyActionTypes.INT_DIV
		).forEach(hotKeyActionTypes::add);
	}
	
	@Override public void setMinimum(Integer minimum) {
		this.minimum = minimum != null ? minimum : Integer.MIN_VALUE;
	}
	
	@Override public void setMaximum(Integer maximum) {
		this.maximum = maximum != null ? maximum : Integer.MAX_VALUE;
	}
	
	@Override
	public @Nullable Integer fromString(String s) {
		try {
			return Integer.valueOf(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	private static boolean isFloating(HotKeyActionType<Integer, ?> type) {
		return type == HotKeyActionTypes.INT_MUL || type == HotKeyActionTypes.INT_DIV;
	}
	
	@Override public void setHotKeyActionType(HotKeyActionType<Integer, ?> type) {
		boolean prevFloating = isFloating(getHotKeyActionType());
		super.setHotKeyActionType(type);
		boolean floating = isFloating(type);
		if (floating != prevFloating) {
			setTextFormatter(TextFormatter.numeric(!floating));
			if (!floating) {
				Float f = getDisplayedFloat();
				setDisplayedValue((int) (f != null? f : getValue()));
			}
		}
	}
	
	@Override public void setHotKeyActionValue(Object value) {
		HotKeyActionType<Integer, ?> type = getHotKeyActionType();
		if (isFloating(type)) {
			textFieldWidget.setValue(String.valueOf(value));
		} else super.setHotKeyActionValue(value);
	}
	
	@Override public Object getHotKeyActionValue() {
		return isFloating(getHotKeyActionType())? getDisplayedFloat() : super.getHotKeyActionValue();
	}
	
	private Float getDisplayedFloat() {
		try {
			return Float.parseFloat(textFieldWidget.getValue());
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	@Internal @Override public Optional<Component> getErrorMessage() {
		try {
			int i = Integer.parseInt(getText());
			if (i > maximum)
				return Optional.of(new TranslatableComponent("simpleconfig.config.error.too_large", maximum));
			if (i < minimum)
				return Optional.of(new TranslatableComponent("simpleconfig.config.error.too_small", minimum));
		} catch (NumberFormatException ex) {
			return Optional.of(
			  new TranslatableComponent("simpleconfig.config.error.invalid_integer", getText()));
		}
		return super.getErrorMessage();
	}
}

