package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryHolder;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface EntryButtonEntryBuilder<
  V, Gui, B extends ConfigEntryBuilder<V, ?, Gui, B> & AtomicEntryBuilder
> extends GUIOnlyEntryBuilder<@NotNull V, Gui, EntryButtonEntryBuilder<V, Gui, B>>,
          AtomicEntryBuilder {
	/**
	 * Set the action performed when the button is clicked
	 */
	@Contract(pure=true) @NotNull EntryButtonEntryBuilder<V, Gui, B> withAction(BiConsumer<V, ConfigEntryHolder> action);
	/**
	 * Set the action performed when the button is clicked
	 */
	@Contract(pure=true) @NotNull EntryButtonEntryBuilder<V, Gui, B> withAction(Consumer<V> action);
	
	/**
	 * Set the label of the button as a translation key.
	 */
	@Contract(pure=true) @NotNull EntryButtonEntryBuilder<V, Gui, B> label(String translation);
	/**
	 * Set the label of the button.
	 */
	@Contract(pure=true) @NotNull EntryButtonEntryBuilder<V, Gui, B> label(Component label);
	/**
	 * Set the label of the button.
	 */
	@Contract(pure=true) @NotNull EntryButtonEntryBuilder<V, Gui, B> label(Supplier<Component> label);
}
