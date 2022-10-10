package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.KeyEntryBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Predicate;

public interface BlockEntryBuilder
  extends ConfigEntryBuilder<Block, String, Block, BlockEntryBuilder>,
          KeyEntryBuilder<Block> {
	/**
	 * When true (the default), requires the block item to have a group.<br>
	 * This excludes the AIR and BARRIER blocks, as well as other special blocks.
	 */
	@Contract(pure=true) @NotNull BlockEntryBuilder setRequireGroup(boolean requireGroup);
	
	@Contract(pure=true) @NotNull BlockEntryBuilder from(Predicate<Block> filter);
	
	@Contract(pure=true) @NotNull BlockEntryBuilder from(List<Block> choices);
	
	@Contract(pure=true) @NotNull BlockEntryBuilder from(Block... choices);
	
	/**
	 * Restrict the selectable items to those of a tag<br>
	 * This can only be done on server configs, since tags
	 * are server-dependant
	 */
	@Contract(pure=true) @NotNull BlockEntryBuilder from(TagKey<Block> tag);
}
