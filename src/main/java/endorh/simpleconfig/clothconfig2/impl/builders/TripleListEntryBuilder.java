package endorh.simpleconfig.clothconfig2.impl.builders;

import endorh.simpleconfig.clothconfig2.api.AbstractConfigEntry;
import endorh.simpleconfig.clothconfig2.api.ConfigEntryBuilder;
import endorh.simpleconfig.clothconfig2.api.IChildListEntry;
import endorh.simpleconfig.clothconfig2.gui.Icon;
import endorh.simpleconfig.clothconfig2.gui.entries.TripleListEntry;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public class TripleListEntryBuilder<
  L, M, R, LE extends AbstractConfigEntry<L> & IChildListEntry,
  ME extends AbstractConfigEntry<M> & IChildListEntry,
  RE extends AbstractConfigEntry<R> & IChildListEntry
  > extends FieldBuilder<Triple<L, M, R>, TripleListEntry<L, M, R, LE, ME, RE>,
  TripleListEntryBuilder<L, M, R, LE, ME, RE>> {
	protected final LE leftEntry;
	protected final ME middleEntry;
	protected final RE rightEntry;
	protected @Nullable Icon leftIcon;
	protected @Nullable Icon rightIcon;
	protected float leftWeight = 0.333F;
	protected float rightWeight = 0.333F;
	
	public TripleListEntryBuilder(
	  ConfigEntryBuilder builder, ITextComponent name, Triple<L, M, R> value,
	  LE leftEntry, ME middleEntry, RE rightEntry
	) {
		super(builder, name, value);
		this.leftEntry = leftEntry;
		this.middleEntry = middleEntry;
		this.rightEntry = rightEntry;
	}
	
	public TripleListEntryBuilder<L, M, R, LE, ME, RE> withIcons(
	  @Nullable Icon leftIcon, @Nullable Icon rightIcon
	) {
		this.leftIcon = leftIcon;
		this.rightIcon = rightIcon;
		return self();
	}
	
	public TripleListEntryBuilder<L, M, R, LE, ME, RE> withWeights(
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
		return new TripleListEntry<>(fieldNameKey, value, leftEntry, middleEntry, rightEntry);
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
