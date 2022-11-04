package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryHolder;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface EntryButtonEntryBuilder<
  V, Gui, B extends ConfigEntryBuilder<V, ?, Gui, B> & AtomicEntryBuilder
> extends GUIOnlyEntryBuilder<@NotNull V, Gui, EntryButtonEntryBuilder<V, Gui, B>>,
          AtomicEntryBuilder {
	@Contract(pure=true) public @NotNull EntryButtonEntryBuilder<V, Gui, B> withAction(BiConsumer<V, ConfigEntryHolder> action);
	@Contract(pure=true) public @NotNull EntryButtonEntryBuilder<V, Gui, B> withAction(Consumer<V> action);
	
	@Contract(pure=true) @NotNull EntryButtonEntryBuilder<V, Gui, B> label(String translation);
	@Contract(pure=true) @NotNull EntryButtonEntryBuilder<V, Gui, B> label(ITextComponent label);
	@Contract(pure=true) @NotNull EntryButtonEntryBuilder<V, Gui, B> label(Supplier<ITextComponent> label);
}
