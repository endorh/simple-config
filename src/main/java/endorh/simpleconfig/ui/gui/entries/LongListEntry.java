package endorh.simpleconfig.ui.gui.entries;

import endorh.simpleconfig.api.ui.TextFormatter;
import endorh.simpleconfig.ui.hotkey.HotKeyActionType;
import endorh.simpleconfig.ui.hotkey.HotKeyActionTypes;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public class LongListEntry
  extends TextFieldListEntry<Long> implements IRangedEntry<Long> {
	private long minimum = Long.MIN_VALUE;
	private long maximum = Long.MAX_VALUE;
	
	@Internal public LongListEntry(Component fieldName, Long value) {
		super(fieldName, value, false);
		setTextFormatter(TextFormatter.numeric(true));
		Stream.of(
		  HotKeyActionTypes.LONG_ADD, HotKeyActionTypes.LONG_ADD_CYCLE,
		  HotKeyActionTypes.LONG_MUL, HotKeyActionTypes.LONG_DIV
		).forEach(hotKeyActionTypes::add);
	}
	
	@Override public void setMinimum(Long minimum) {
		this.minimum = minimum != null ? minimum : Long.MIN_VALUE;
	}
	
	@Override public void setMaximum(Long maximum) {
		this.maximum = maximum != null ? maximum : Long.MAX_VALUE;
	}
	
	@Override protected @Nullable Long fromString(String s) {
		try {
			return Long.valueOf(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	private static boolean isFloating(HotKeyActionType<Long, ?> type) {
		return type == HotKeyActionTypes.LONG_MUL || type == HotKeyActionTypes.LONG_DIV;
	}
	
	@Override public void setHotKeyActionType(HotKeyActionType<Long, ?> type) {
		boolean prevFloating = isFloating(getHotKeyActionType());
		super.setHotKeyActionType(type);
		boolean floating = isFloating(type);
		if (floating != prevFloating) {
			setTextFormatter(TextFormatter.numeric(!floating));
			if (!floating) {
				Double d = getDisplayedDouble();
				setDisplayedValue((long) (d != null? d : getValue()));
			}
		}
	}
	
	@Override public void setHotKeyActionValue(Object value) {
		HotKeyActionType<Long, ?> type = getHotKeyActionType();
		if (isFloating(type)) {
			textFieldWidget.setValue(String.valueOf(value));
		} else super.setHotKeyActionValue(value);
	}
	
	@Override public Object getHotKeyActionValue() {
		return isFloating(getHotKeyActionType())? getDisplayedDouble() : super.getHotKeyActionValue();
	}
	
	private Double getDisplayedDouble() {
		try {
			return Double.parseDouble(textFieldWidget.getValue());
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	@Internal @Override public Optional<Component> getErrorMessage() {
		try {
			long i = Long.parseLong(getText());
			if (i > maximum)
				return Optional.of(Component.translatable("simpleconfig.config.error.too_large", maximum));
			if (i < minimum)
				return Optional.of(Component.translatable("simpleconfig.config.error.too_small", minimum));
		} catch (NumberFormatException ex) {
			return Optional.of(
			  Component.translatable("simpleconfig.config.error.invalid_integer", getText()));
		}
		return super.getErrorMessage();
	}
}

