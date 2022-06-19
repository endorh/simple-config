package endorh.simpleconfig.clothconfig2.api;

import org.jetbrains.annotations.NotNull;

public interface ReferenceProvider {
	@NotNull AbstractConfigEntry<?> provideReferenceEntry();
}

