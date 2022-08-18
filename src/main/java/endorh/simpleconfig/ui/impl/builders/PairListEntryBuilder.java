package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.gui.entries.PairListEntry;
import endorh.simpleconfig.ui.icon.Icon;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public class PairListEntryBuilder<
  L, R, LE extends AbstractConfigListEntry<L> & IChildListEntry,
  RE extends AbstractConfigListEntry<R> & IChildListEntry,
  LEB extends FieldBuilder<L, LE, LEB>, REB extends FieldBuilder<R, RE, REB>
> extends FieldBuilder<Pair<L, R>, PairListEntry<L, R, LE, RE>, PairListEntryBuilder<L, R, LE, RE, LEB, REB>> {
	protected final LEB leftEntry;
	protected final REB rightEntry;
	protected @Nullable Icon middleIcon = null;
	protected float splitPos = 0.5F;
	
	public PairListEntryBuilder(
	  ConfigFieldBuilder builder, ITextComponent name, Pair<L, R> value,
	  LEB leftEntry, REB rightEntry
	) {
		super(PairListEntry.class, builder, name, value);
		this.leftEntry = leftEntry;
		this.rightEntry = rightEntry;
	}
	
	public PairListEntryBuilder<L, R, LE, RE, LEB, REB> withMiddleIcon(Icon middleIcon) {
		this.middleIcon = middleIcon;
		return self();
	}
	
	public PairListEntryBuilder<L, R, LE, RE, LEB, REB> withSplitPos(
	  @Range(from = 0, to = 1) float splitPos
	) {
		if (splitPos < 0 || splitPos > 1) throw new IllegalArgumentException(
		  "Split position must be between 0 and 1");
		this.splitPos = splitPos;
		return self();
	}
	
	@Override protected PairListEntry<L, R, LE, RE> buildEntry() {
		return new PairListEntry<>(fieldNameKey, value, leftEntry.build(), rightEntry.build());
	}
	
	@Override public @NotNull PairListEntry<L, R, LE, RE> build() {
		final PairListEntry<L, R, LE, RE> entry = super.build();
		entry.setMiddleIcon(middleIcon);
		entry.setSplitPos(splitPos);
		return entry;
	}
}
