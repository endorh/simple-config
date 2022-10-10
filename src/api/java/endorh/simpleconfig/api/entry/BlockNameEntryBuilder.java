package endorh.simpleconfig.api.entry;

import net.minecraft.block.Block;
import net.minecraft.tags.ITag;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface BlockNameEntryBuilder
  extends ResourceEntryBuilder<BlockNameEntryBuilder> {
	/**
	 * Restrict the selectable items to those of a tag<br>
	 * This can only be done on server configs, since tags
	 * are server-dependant
	 */
	@Contract(pure=true) @NotNull BlockNameEntryBuilder suggest(ITag<Block> tag);
}
