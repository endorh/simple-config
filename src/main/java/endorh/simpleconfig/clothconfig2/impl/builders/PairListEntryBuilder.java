package endorh.simpleconfig.clothconfig2.impl.builders;

import endorh.simpleconfig.clothconfig2.api.AbstractConfigEntry;
import endorh.simpleconfig.clothconfig2.api.ConfigEntryBuilder;
import endorh.simpleconfig.clothconfig2.api.IChildListEntry;
import endorh.simpleconfig.clothconfig2.gui.Icon;
import endorh.simpleconfig.clothconfig2.gui.entries.PairListEntry;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public class PairListEntryBuilder<
  L, R, LE extends AbstractConfigEntry<L> & IChildListEntry,
  RE extends AbstractConfigEntry<R> & IChildListEntry
> extends FieldBuilder<Pair<L, R>, PairListEntry<L, R, LE, RE>, PairListEntryBuilder<L, R, LE, RE>> {
	protected final LE leftEntry;
	protected final RE rightEntry;
	protected @Nullable Icon middleIcon = null;
	protected float splitPos = 0.5F;
	
	public PairListEntryBuilder(
	  ConfigEntryBuilder builder, ITextComponent name, Pair<L, R> value,
	  LE leftEntry, RE rightEntry
	) {
		super(builder, name, value);
		this.leftEntry = leftEntry;
		this.rightEntry = rightEntry;
	}
	
	public PairListEntryBuilder<L, R, LE, RE> withMiddleIcon(Icon middleIcon) {
		this.middleIcon = middleIcon;
		return self();
	}
	
	public PairListEntryBuilder<L, R, LE, RE> withSplitPos(
	  @Range(from = 0, to = 1) float splitPos
	) {
		if (splitPos < 0 || splitPos > 1) throw new IllegalArgumentException(
		  "Split position must be between 0 and 1");
		this.splitPos = splitPos;
		return self();
	}
	
	@Override protected PairListEntry<L, R, LE, RE> buildEntry() {
		return new PairListEntry<>(fieldNameKey, value, leftEntry, rightEntry);
	}
	
	@Override public @NotNull PairListEntry<L, R, LE, RE> build() {
		final PairListEntry<L, R, LE, RE> entry = super.build();
		entry.setMiddleIcon(middleIcon);
		entry.setSplitPos(splitPos);
		return entry;
	}
}
