package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

public interface SerializableEntryBuilder<
  V, Self extends SerializableEntryBuilder<V, Self>
> extends ConfigEntryBuilder<@NotNull V, String, String, Self>, AtomicEntryBuilder {
	/**
	 * Suggest possible values in a combo-box.
	 */
	@Contract(pure=true) @NotNull Self suggest(V... suggestions);
	
	/**
	 * Suggest possible values in a combo-box.
	 */
	@Contract(pure=true) @NotNull Self suggest(@NotNull List<V> suggestions);
	
	/**
	 * Suggest possible values in a combo-box dynamically.
	 */
	@Contract(pure=true) @NotNull Self suggest(Supplier<List<V>> suggestionSupplier);
}
