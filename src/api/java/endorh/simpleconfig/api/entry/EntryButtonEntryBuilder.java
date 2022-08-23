package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.KeyEntryBuilder;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Contract;

import java.util.function.Supplier;

public interface EntryButtonEntryBuilder<
  V, Gui, B extends ConfigEntryBuilder<V, ?, Gui, B> & KeyEntryBuilder<Gui>
  > extends GUIOnlyEntryBuilder<V, Gui, EntryButtonEntryBuilder<V, Gui, B>>, KeyEntryBuilder<Gui> {
	@Contract(pure=true) EntryButtonEntryBuilder<V, Gui, B> label(String translation);
	
	@Contract(pure=true) EntryButtonEntryBuilder<V, Gui, B> label(Component label);
	
	@Contract(pure=true) EntryButtonEntryBuilder<V, Gui, B> label(Supplier<Component> label);
}
