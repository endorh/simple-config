package endorh.simpleconfig.api;

import org.jetbrains.annotations.Contract;

import java.util.function.Consumer;

public interface IGroupBuilder extends ConfigEntryHolderBuilder<IGroupBuilder> {
	@Contract("_, _ -> this") IGroupBuilder n(IGroupBuilder nested, int index);
	
	@Contract("_ -> this") IGroupBuilder withBaker(Consumer<ISimpleConfigGroup> baker);
	
	@Contract("_, _ -> this") <
	  V, C, G,
	  B extends ConfigEntryBuilder<V, C, G, B>
	> IGroupBuilder caption(String name, B entry);
}
