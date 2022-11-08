package endorh.simpleconfig.api;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public interface ConfigGroupBuilder extends ConfigEntryHolderBuilder<ConfigGroupBuilder> {
	/**
	 * Add a group at the given index.<br>
	 */
	@Contract("_, _ -> this") @NotNull ConfigGroupBuilder n(ConfigGroupBuilder nested, int index);
	
	/**
	 * Set a baker method for this group.<br>
	 * It will be run after baking fields after every config change.
	 */
	@Contract("_ -> this") @NotNull ConfigGroupBuilder withBaker(Consumer<SimpleConfigGroup> baker);
	
	/**
	 * Add an entry as caption for this group.<br>
	 * The builder must implement {@link AtomicEntryBuilder}.
	 */
	@Contract("_, _ -> this") <
	  V, C, G,
	  B extends ConfigEntryBuilder<V, C, G, B> & AtomicEntryBuilder
	> @NotNull ConfigGroupBuilder caption(String name, B entry);
}
