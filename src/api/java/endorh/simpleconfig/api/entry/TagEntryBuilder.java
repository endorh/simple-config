package endorh.simpleconfig.api.entry;

import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;

public interface TagEntryBuilder
  extends SerializableEntryBuilder<@NotNull Tag, TagEntryBuilder> {}
