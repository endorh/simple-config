package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.gui.entries.ButtonListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import java.util.function.Supplier;

public class ButtonFieldBuilder extends FieldBuilder<Runnable, ButtonListEntry, ButtonFieldBuilder> {
	private Supplier<ITextComponent> buttonLabelSupplier = () -> new StringTextComponent("");
	
	public ButtonFieldBuilder(
	  ConfigEntryBuilder builder, ITextComponent name, Runnable value
	) {
		super(ButtonListEntry.class, builder, name, value);
	}
	
	public ButtonFieldBuilder withButtonLabel(ITextComponent label) {
		return withButtonLabel(() -> label);
	}
	
	public ButtonFieldBuilder withButtonLabel(Supplier<ITextComponent> supplier) {
		buttonLabelSupplier = supplier;
		return this;
	}
	
	@Override protected ButtonListEntry buildEntry() {
		return new ButtonListEntry(value, fieldNameKey, buttonLabelSupplier);
	}
}
