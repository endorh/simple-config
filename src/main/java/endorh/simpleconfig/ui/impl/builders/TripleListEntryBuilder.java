package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.gui.Icon;
import endorh.simpleconfig.ui.gui.entries.TripleListEntry;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public class TripleListEntryBuilder<
  L, M, R, LE extends AbstractConfigListEntry<L> & IChildListEntry,
  ME extends AbstractConfigListEntry<M> & IChildListEntry,
  RE extends AbstractConfigListEntry<R> & IChildListEntry,
  LEB extends FieldBuilder<L, LE, LEB>,
  MEB extends FieldBuilder<M, ME, MEB>,
  REB extends FieldBuilder<R, RE, REB>
  > extends FieldBuilder<Triple<L, M, R>, TripleListEntry<L, M, R, LE, ME, RE>,
  TripleListEntryBuilder<L, M, R, LE, ME, RE, LEB, MEB, REB>> {
	protected final LEB leftEntry;
	protected final MEB middleEntry;
	protected final REB rightEntry;
	protected @Nullable Icon leftIcon;
	protected @Nullable Icon rightIcon;
	protected float leftWeight = 0.333F;
	protected float rightWeight = 0.333F;
	
	public TripleListEntryBuilder(
	  ConfigEntryBuilder builder, ITextComponent name, Triple<L, M, R> value,
	  LEB leftEntry, MEB middleEntry, REB rightEntry
	) {
		super(TripleListEntry.class, builder, name, value);
		this.leftEntry = leftEntry;
		this.middleEntry = middleEntry;
		this.rightEntry = rightEntry;
	}
	
	public TripleListEntryBuilder<L, M, R, LE, ME, RE, LEB, MEB, REB> withIcons(
	  @Nullable Icon leftIcon, @Nullable Icon rightIcon
	) {
		this.leftIcon = leftIcon;
		this.rightIcon = rightIcon;
		return self();
	}
	
	public TripleListEntryBuilder<L, M, R, LE, ME, RE, LEB, MEB, REB> withWeights(
	  @Range(from = 0, to = 1) float leftWeight, @Range(from = 0, to = 1) float rightWeight
	) {
		if (leftWeight < 0 || rightWeight < 0 || leftWeight + rightWeight > 1F)
			throw new IllegalArgumentException(
			  "Left and right weights must add up between 0 and 1");
		this.leftWeight = leftWeight;
		this.rightWeight = rightWeight;
		return self();
	}
	
	@Override protected TripleListEntry<L, M, R, LE, ME, RE> buildEntry() {
		return new TripleListEntry<>(
		  fieldNameKey, value, leftEntry.build(), middleEntry.build(), rightEntry.build());
	}
	
	@Override public @NotNull TripleListEntry<L, M, R, LE, ME, RE> build() {
		final TripleListEntry<L, M, R, LE, ME, RE> entry = super.build();
		entry.setLeftIcon(leftIcon);
		entry.setRightIcon(rightIcon);
		entry.setLeftWeight(leftWeight);
		entry.setRightWeight(rightWeight);
		return entry;
	}
}
