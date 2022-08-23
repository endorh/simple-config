package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;

public interface GUIOnlyEntryBuilder<V, Gui, Self extends GUIOnlyEntryBuilder<V, Gui, Self>>
  extends ConfigEntryBuilder<V, Void, Gui, Self> {}
