package endorh.simpleconfig.api;

import org.jetbrains.annotations.Contract;

import java.util.function.Consumer;

public interface ConfigGroupBuilder extends ConfigEntryHolderBuilder<ConfigGroupBuilder> {
	@Contract("_, _ -> this") ConfigGroupBuilder n(ConfigGroupBuilder nested, int index);
	
	@Contract("_ -> this") ConfigGroupBuilder withBaker(Consumer<SimpleConfigGroup> baker);
	
	@Contract("_, _ -> this") <
	  V, C, G,
	  B extends ConfigEntryBuilder<V, C, G, B>
	> ConfigGroupBuilder caption(String name, B entry);
}
