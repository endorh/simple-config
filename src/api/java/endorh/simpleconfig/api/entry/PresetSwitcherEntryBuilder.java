package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import org.jetbrains.annotations.NotNull;

public interface PresetSwitcherEntryBuilder
  extends GUIOnlyEntryBuilder<@NotNull String, String, PresetSwitcherEntryBuilder>,
          AtomicEntryBuilder {}
