package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import org.jetbrains.annotations.NotNull;

public interface EntryListEntryBuilder<
  V, C, G, B extends ConfigEntryBuilder<V, C, G, B>
> extends ListEntryBuilder<@NotNull V, C, G, EntryListEntryBuilder<V, C, G, B>> {}
