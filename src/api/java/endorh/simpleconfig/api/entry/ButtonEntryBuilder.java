package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryHolder;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface ButtonEntryBuilder extends GUIOnlyEntryBuilder<
  @NotNull Consumer<@NotNull ConfigEntryHolder>, Runnable, ButtonEntryBuilder
>, AtomicEntryBuilder {
	/**
	 * Set the label of the button as a translation key.
	 */
	@Contract(pure=true) @NotNull ButtonEntryBuilder label(String translation);
	
	/**
	 * Set the label of the button.
	 */
	@Contract(pure=true) @NotNull ButtonEntryBuilder label(ITextComponent label);
	
	/**
	 * Set the label of the button.
	 */
	@Contract(pure=true) @NotNull ButtonEntryBuilder label(Supplier<ITextComponent> label);
}
