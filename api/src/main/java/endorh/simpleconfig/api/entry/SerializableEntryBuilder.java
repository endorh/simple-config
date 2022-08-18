package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.KeyEntryBuilder;

public interface SerializableEntryBuilder<
  V, Self extends SerializableEntryBuilder<V, Self>
> extends ConfigEntryBuilder<V, String, String, Self>, KeyEntryBuilder<String> {}
