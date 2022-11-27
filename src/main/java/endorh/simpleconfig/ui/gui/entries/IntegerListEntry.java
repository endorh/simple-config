package endorh.simpleconfig.ui.gui.entries;

import endorh.simpleconfig.api.ui.TextFormatter;
import endorh.simpleconfig.ui.hotkey.HotKeyActionTypes;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.stream.Stream;

@OnlyIn(value = Dist.CLIENT)
public class IntegerListEntry extends TextFieldListEntry<Integer> implements IRangedEntry<Integer> {
	private int minimum = Integer.MIN_VALUE;
	private int maximum = Integer.MAX_VALUE;
	
	@Internal public IntegerListEntry(Component fieldName, Integer value) {
		super(fieldName, value, false);
		setTextFormatter(TextFormatter.numeric(true));
		Stream.of(HotKeyActionTypes.INT_ADD, HotKeyActionTypes.INT_ADD_CYCLE)
		  .forEach(hotKeyActionTypes::add);
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
	
	@Internal @Override public Optional<Component> getErrorMessage() {
		try {
			int i = Integer.parseInt(getText());
			if (i > this.maximum)
				return Optional.of(Component.translatable("simpleconfig.config.error.too_large", this.maximum));
			if (i < this.minimum)
				return Optional.of(Component.translatable("simpleconfig.config.error.too_small", this.minimum));
		} catch (NumberFormatException ex) {
			return Optional.of(
			  Component.translatable("simpleconfig.config.error.invalid_integer", getText()));
		}
		return super.getErrorMessage();
	}
}

