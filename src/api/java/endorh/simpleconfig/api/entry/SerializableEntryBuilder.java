package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import org.jetbrains.annotations.NotNull;

public interface SerializableEntryBuilder<
  V, Self extends SerializableEntryBuilder<V, Self>
> extends ConfigEntryBuilder<@NotNull V, String, String, Self>, AtomicEntryBuilder {}
