package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.ButtonEntryBuilder;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.ButtonFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ButtonEntry extends GUIOnlyEntry<
  Consumer<ConfigEntryHolder>, Runnable, ButtonEntry> {
	protected Supplier<Component> buttonLabelSupplier = () -> Component.literal("");
	
	public ButtonEntry(
	  ConfigEntryHolder parent, String name, Consumer<ConfigEntryHolder> value
	) {
		super(parent, name, value, false, Consumer.class);
	}
	
	public static class Builder extends GUIOnlyEntry.Builder<
	  Consumer<ConfigEntryHolder>, Runnable, ButtonEntry, ButtonEntryBuilder, Builder
	> implements ButtonEntryBuilder {
		protected Supplier<Component> buttonLabelSupplier =
		  () -> Component.translatable("simpleconfig.label.run");
		
		public Builder(Consumer<ConfigEntryHolder> value) {
			super(value, Void.class);
		}
		
		@Override @Contract(pure=true) public @NotNull Builder label(String translation) {
			Builder copy = copy();
			final MutableComponent ttc = Component.translatable(translation);
			copy.buttonLabelSupplier = () -> ttc;
			return copy;
		}
		
		@Override @Contract(pure=true) public @NotNull Builder label(Component label) {
			Builder copy = copy();
			copy.buttonLabelSupplier = () -> label;
			return copy;
		}
		
		@Override @Contract(pure=true) public @NotNull Builder label(Supplier<Component> label) {
			Builder copy = copy();
			copy.buttonLabelSupplier = label;
			return copy;
		}
		
		@Override protected ButtonEntry buildEntry(ConfigEntryHolder parent, String name) {
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
	  Consumer<ConfigEntryHolder> value
	) {
		return () -> value.accept(parent);
	}
	
	@OnlyIn(Dist.CLIENT) @Override public Optional<FieldBuilder<Runnable, ?, ?>> buildGUIEntry(
	  ConfigFieldBuilder builder
	) {
		ButtonFieldBuilder entryBuilder = builder.startButton(getDisplayName(), forGui(get()))
		  .withButtonLabel(buttonLabelSupplier);
		return Optional.of(entryBuilder);
	}
}
