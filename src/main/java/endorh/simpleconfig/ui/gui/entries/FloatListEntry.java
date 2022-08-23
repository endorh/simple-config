package endorh.simpleconfig.ui.gui.entries;

import endorh.simpleconfig.api.ui.ITextFormatter;
import endorh.simpleconfig.ui.hotkey.HotKeyActionTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.stream.Stream;

@OnlyIn(value = Dist.CLIENT)
public class FloatListEntry extends TextFieldListEntry<Float> implements IRangedEntry<Float> {
	private float minimum = Float.NEGATIVE_INFINITY;
	private float maximum = Float.POSITIVE_INFINITY;
	
	@Internal public FloatListEntry(
	  Component fieldName, Float value
	) {
		super(fieldName, value, false);
		setTextFormatter(ITextFormatter.numeric(false));
		Stream.of(
		  HotKeyActionTypes.FLOAT_ADD, HotKeyActionTypes.FLOAT_ADD_CYCLE,
		  HotKeyActionTypes.FLOAT_MULTIPLY, HotKeyActionTypes.FLOAT_DIVIDE
		).forEach(hotKeyActionTypes::add);
	}
	
	@Override public void setMinimum(Float minimum) {
		this.minimum = minimum != null ? minimum : Float.NEGATIVE_INFINITY;
	}
	
	@Override public void setMaximum(Float maximum) {
		this.maximum = maximum != null ? maximum : Float.POSITIVE_INFINITY;
	}
	
	@Override public @Nullable Float fromString(String s) {
		try {
			return Float.valueOf(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	@Internal @Override public Optional<Component> getErrorMessage() {
		try {
			float i = Float.parseFloat(getText());
			if (i > this.maximum)
				return Optional.of(new TranslatableComponent("simpleconfig.config.error.too_large", this.maximum));
			if (i < this.minimum)
				return Optional.of(new TranslatableComponent("simpleconfig.config.error.too_small", this.minimum));
		} catch (NumberFormatException ex) {
			return Optional.of(
			  new TranslatableComponent("simpleconfig.config.error.invalid_float", getText()));
		}
		return super.getErrorMessage();
	}
}

