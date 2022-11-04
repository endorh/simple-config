package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import org.jetbrains.annotations.NotNull;

public interface GUIOnlyEntryBuilder<V, Gui, Self extends GUIOnlyEntryBuilder<V, Gui, Self>>
  extends ConfigEntryBuilder<@NotNull V, Void, Gui, Self> {}
