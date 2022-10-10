package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.KeyEntryBuilder;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public interface EntryButtonEntryBuilder<
  V, Gui, B extends ConfigEntryBuilder<V, ?, Gui, B> & KeyEntryBuilder<Gui>
  > extends GUIOnlyEntryBuilder<V, Gui, EntryButtonEntryBuilder<V, Gui, B>>, KeyEntryBuilder<Gui> {
	@Contract(pure=true) @NotNull EntryButtonEntryBuilder<V, Gui, B> label(String translation);
	
	@Contract(pure=true) @NotNull EntryButtonEntryBuilder<V, Gui, B> label(ITextComponent label);
	
	@Contract(pure=true) @NotNull EntryButtonEntryBuilder<V, Gui, B> label(Supplier<ITextComponent> label);
}
