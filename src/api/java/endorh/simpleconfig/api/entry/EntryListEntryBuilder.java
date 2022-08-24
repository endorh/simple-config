package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;

public interface EntryListEntryBuilder<
  V, C, G, B extends ConfigEntryBuilder<V, C, G, B>
> extends ListEntryBuilder<V, C, G, EntryListEntryBuilder<V, C, G, B>> {}
