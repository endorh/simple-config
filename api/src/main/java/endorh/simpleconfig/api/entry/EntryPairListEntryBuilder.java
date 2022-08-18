package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.KeyEntryBuilder;
import org.apache.commons.lang3.tuple.Pair;

public interface EntryPairListEntryBuilder<K, V, KC, C, KG, G,
  B extends ConfigEntryBuilder<V, C, G, B>,
  KB extends ConfigEntryBuilder<K, KC, KG, KB> & KeyEntryBuilder<KG>
> extends ListEntryBuilder<
  Pair<K, V>, Pair<KC, C>, Pair<KG, G>,
  EntryPairListEntryBuilder<K, V, KC, C, KG, G, B, KB>
> {}
