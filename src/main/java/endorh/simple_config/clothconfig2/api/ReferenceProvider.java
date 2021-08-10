package endorh.simple_config.clothconfig2.api;

import org.jetbrains.annotations.NotNull;

public interface ReferenceProvider<T> {
   @NotNull
   AbstractConfigEntry<T> provideReferenceEntry();
}
