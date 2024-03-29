package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

public interface EntryPairListEntryBuilder<K, V, KC, C, KG, G,
  B extends ConfigEntryBuilder<V, C, G, B>,
  KB extends ConfigEntryBuilder<K, KC, KG, KB> & AtomicEntryBuilder
> extends ListEntryBuilder<
  @NotNull Pair<@NotNull K, @NotNull V>, @NotNull Pair<@NotNull KC, @NotNull C>, @NotNull Pair<@NotNull KG, @NotNull G>,
  EntryPairListEntryBuilder<K, V, KC, C, KG, G, B, KB>
> {}
