package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.core.ISimpleConfigEntryHolder;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.gui.entries.ButtonListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Contract;

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
		  () -> new TranslationTextComponent("simpleconfig.label.run");
		
		public Builder(Consumer<ISimpleConfigEntryHolder> value) {
			super(value, Void.class);
		}
		
		@Contract(pure=true) public Builder label(String translation) {
			Builder copy = copy();
			final TranslationTextComponent ttc = new TranslationTextComponent(translation);
			copy.buttonLabelSupplier = () -> ttc;
			return copy;
		}
		
		@Contract(pure=true) public Builder label(ITextComponent label) {
			Builder copy = copy();
			copy.buttonLabelSupplier = () -> label;
			return copy;
		}
		
		@Contract(pure=true) public Builder label(Supplier<ITextComponent> label) {
			Builder copy = copy();
			copy.buttonLabelSupplier = label;
			return copy;
		}
		
		@Override protected ButtonEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			final ButtonEntry entry = new ButtonEntry(parent, name, value);
			entry.buttonLabelSupplier = buttonLabelSupplier;
			return entry;
		}
		
		@Override protected Builder createCopy() {
			final Builder copy = new Builder(value);
			copy.buttonLabelSupplier = buttonLabelSupplier;
			return copy;
		}
	}
	
	@Override public Runnable forGui(
	  Consumer<ISimpleConfigEntryHolder> value
	) {
		return () -> value.accept(parent);
	}
	
	@OnlyIn(Dist.CLIENT) @Override public Optional<AbstractConfigListEntry<Runnable>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		final ButtonListEntry entry =
		  new ButtonListEntry(forGui(get()), getDisplayName(), buttonLabelSupplier);
		return Optional.of(entry);
	}
}
