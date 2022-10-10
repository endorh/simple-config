package endorh.simpleconfig.api;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public interface ConfigGroupBuilder extends ConfigEntryHolderBuilder<ConfigGroupBuilder> {
	@Contract("_, _ -> this") @NotNull ConfigGroupBuilder n(ConfigGroupBuilder nested, int index);
	
	@Contract("_ -> this") @NotNull ConfigGroupBuilder withBaker(Consumer<SimpleConfigGroup> baker);
	
	@Contract("_, _ -> this") <
	  V, C, G,
	  B extends ConfigEntryBuilder<V, C, G, B>
	> @NotNull ConfigGroupBuilder caption(String name, B entry);
}
