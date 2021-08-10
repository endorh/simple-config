package endorh.simple_config.core.entry;

import endorh.simple_config.core.ISimpleConfigEntryHolder;
import endorh.simple_config.gui.ButtonListEntry;
import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ButtonEntry extends GUIOnlyEntry<
  Consumer<ISimpleConfigEntryHolder>, Runnable, ButtonEntry> {
	protected Supplier<ITextComponent> buttonLabelSupplier = () -> new StringTextComponent("");
	
	public ButtonEntry(
	  ISimpleConfigEntryHolder parent, String name, Consumer<ISimpleConfigEntryHolder> value
	) {
		super(parent, name, value, false, Consumer.class);
	}
	
	public static class Builder extends GUIOnlyEntry.Builder<
	  Consumer<ISimpleConfigEntryHolder>, Runnable, ButtonEntry, Builder> {
		protected Supplier<ITextComponent> buttonLabelSupplier =
		  () -> new TranslationTextComponent("simple-config.label.run");
		
		public Builder(Consumer<ISimpleConfigEntryHolder> value) {
			super(value, Void.class);
		}
		
		public Builder label(String translation) {
			final TranslationTextComponent ttc = new TranslationTextComponent(translation);
			this.buttonLabelSupplier = () -> ttc;
			return self();
		}
		
		public Builder label(ITextComponent label) {
			this.buttonLabelSupplier = () -> label;
			return self();
		}
		
		public Builder label(Supplier<ITextComponent> label) {
			this.buttonLabelSupplier = label;
			return self();
		}
		
		@Override protected ButtonEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			final ButtonEntry entry = new ButtonEntry(parent, name, value);
			entry.buttonLabelSupplier = buttonLabelSupplier;
			return entry;
		}
	}
	
	@Override protected Runnable forGui(
	  Consumer<ISimpleConfigEntryHolder> value
	) {
		return () -> value.accept(parent);
	}
	
	@Override public Optional<AbstractConfigListEntry<Runnable>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		final ButtonListEntry entry =
		  new ButtonListEntry(forGui(get()), getDisplayName(), buttonLabelSupplier,
		                      () -> supplyTooltip(forGui(get())));
		return Optional.of(entry);
	}
}
