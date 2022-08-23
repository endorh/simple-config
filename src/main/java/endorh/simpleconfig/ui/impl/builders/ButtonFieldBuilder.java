package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.gui.entries.ButtonListEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

import java.util.function.Supplier;

public class ButtonFieldBuilder extends FieldBuilder<Runnable, ButtonListEntry, ButtonFieldBuilder> {
	private Supplier<Component> buttonLabelSupplier = () -> new TextComponent("");
	
	public ButtonFieldBuilder(
	  ConfigFieldBuilder builder, Component name, Runnable value
	) {
		super(ButtonListEntry.class, builder, name, value);
	}
	
	public ButtonFieldBuilder withButtonLabel(Component label) {
		return withButtonLabel(() -> label);
	}
	
	public ButtonFieldBuilder withButtonLabel(Supplier<Component> supplier) {
		buttonLabelSupplier = supplier;
		return this;
	}
	
	@Override protected ButtonListEntry buildEntry() {
		return new ButtonListEntry(value, fieldNameKey, buttonLabelSupplier);
	}
}
