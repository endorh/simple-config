package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.core.*;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.gui.Icon;
import endorh.simpleconfig.ui.impl.builders.TripleListEntryBuilder;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.List;
import java.util.Optional;

public class EntryTripleEntry<
  L, M, R, LC, MC, RC, LG, MG, RG,
  LE extends AbstractConfigEntry<L, LC, LG, LE> & IKeyEntry<LG>,
  ME extends AbstractConfigEntry<M, MC, MG, ME> & IKeyEntry<MG>,
  RE extends AbstractConfigEntry<R, RC, RG, RE> & IKeyEntry<RG>
> extends AbstractConfigEntry<
  Triple<L, M, R>, Triple<LC, MC, RC>, Triple<LG, MG, RG>,
  EntryTripleEntry<L, M, R, LC, MC, RC, LG, MG, RG, LE, ME, RE>
> implements IKeyEntry<Triple<LG, MG, RG>> {
	protected final LE leftEntry;
	protected final ME middleEntry;
	protected final RE rightEntry;
	
	protected @Nullable Icon leftIcon;
	protected @Nullable Icon rightIcon;
	protected float leftWeight = 0.333F;
	protected float rightWeight = 0.333F;
	
	protected EntryTripleEntry(
	  ISimpleConfigEntryHolder parent, String name, Triple<L, M, R> value,
	  LE leftEntry, ME middleEntry, RE rightEntry
	) {
		super(parent, name, value);
		this.leftEntry = leftEntry;
		this.middleEntry = middleEntry;
		this.rightEntry = rightEntry;
	}
	
	public static class Builder<
	  L, M, R, LC, MC, RC, LG, MG, RG,
	  LE extends AbstractConfigEntry<L, LC, LG, LE> & IKeyEntry<LG>,
	  ME extends AbstractConfigEntry<M, MC, MG, ME> & IKeyEntry<MG>,
	  RE extends AbstractConfigEntry<R, RC, RG, RE> & IKeyEntry<RG>,
	  LB extends AbstractConfigEntryBuilder<L, LC, LG, LE, LB>,
	  MB extends AbstractConfigEntryBuilder<M, MC, MG, ME, MB>,
	  RB extends AbstractConfigEntryBuilder<R, RC, RG, RE, RB>
	> extends AbstractConfigEntryBuilder<
	  Triple<L, M, R>, Triple<LC, MC, RC>, Triple<LG, MG, RG>,
	  EntryTripleEntry<L, M, R, LC, MC, RC, LG, MG, RG, LE, ME, RE>,
	  EntryTripleEntry.Builder<L, M, R, LC, MC, RC, LG, MG, RG, LE, ME, RE, LB, MB, RB>
	> {
		protected LB leftBuilder;
		protected MB middleBuilder;
		protected RB rightBuilder;
		
		protected @Nullable Icon leftIcon;
		protected @Nullable Icon rightIcon;
		protected float leftWeight = 0.333F;
		protected float rightWeight = 0.333F;
		
		public Builder(
		  Triple<L, M, R> value, LB leftBuilder, MB middleBuilder, RB rightBuilder
		) {
			super(value, Triple.class);
			this.leftBuilder = leftBuilder;
			this.middleBuilder = middleBuilder;
			this.rightBuilder = rightBuilder;
		}
		
		public Builder<L, M, R, LC, MC, RC, LG, MG, RG, LE, ME, RE, LB, MB, RB> withLeftIcon(
		  @Nullable Icon leftIcon
		) {
			final Builder<L, M, R, LC, MC, RC, LG, MG, RG, LE, ME, RE, LB, MB, RB> copy = copy();
			copy.leftIcon = leftIcon;
			return copy;
		}
		
		public Builder<L, M, R, LC, MC, RC, LG, MG, RG, LE, ME, RE, LB, MB, RB> withRightIcon(
		  @Nullable Icon rightIcon
		) {
			final Builder<L, M, R, LC, MC, RC, LG, MG, RG, LE, ME, RE, LB, MB, RB> copy = copy();
			copy.rightIcon = rightIcon;
			return copy;
		}
		
		public Builder<L, M, R, LC, MC, RC, LG, MG, RG, LE, ME, RE, LB, MB, RB> withIcons(
		  @Nullable Icon leftIcon, @Nullable Icon rightIcon
		) {
			final Builder<L, M, R, LC, MC, RC, LG, MG, RG, LE, ME, RE, LB, MB, RB> copy = copy();
			copy.leftIcon = leftIcon;
			copy.rightIcon = rightIcon;
			return copy;
		}
		
		public Builder<L, M, R, LC, MC, RC, LG, MG, RG, LE, ME, RE, LB, MB, RB> withWeights(
		  @Range(from = 0, to = 1) float leftWeight, @Range(from = 0, to = 1) float rightWeight
		) {
			if (leftWeight < 0 || rightWeight < 0) throw new IllegalArgumentException(
			  "Weights can be negative. Specifically: " + leftWeight + ", " + rightWeight);
			if (leftWeight + rightWeight > 1) throw new IllegalArgumentException(
			  "Weights must add to less than 1. Specifically: " + leftWeight + ", " + rightWeight);
			final Builder<L, M, R, LC, MC, RC, LG, MG, RG, LE, ME, RE, LB, MB, RB> copy = copy();
			copy.leftWeight = leftWeight;
			copy.rightWeight = rightWeight;
			return copy;
		}
		
		@Override protected EntryTripleEntry<L, M, R, LC, MC, RC, LG, MG, RG, LE, ME, RE> buildEntry(
		  ISimpleConfigEntryHolder parent, String name
		) {
			LE leftEntry = DummyEntryHolder.build(parent, leftBuilder).withSaver((v, h) -> {});
			ME middleEntry = DummyEntryHolder.build(parent, middleBuilder).withSaver((v, h) -> {});
			RE rightEntry = DummyEntryHolder.build(parent, rightBuilder).withSaver((v, h) -> {});
			final EntryTripleEntry<L, M, R, LC, MC, RC, LG, MG, RG, LE, ME, RE> entry =
			  new EntryTripleEntry<>(parent, name, value, leftEntry, middleEntry, rightEntry);
			entry.leftIcon = leftIcon;
			entry.rightIcon = rightIcon;
			entry.leftWeight = leftWeight;
			entry.rightWeight = rightWeight;
			return entry;
		}
		
		@Override
		protected Builder<L, M, R, LC, MC, RC, LG, MG, RG, LE, ME, RE, LB, MB, RB> createCopy() {
			final Builder<L, M, R, LC, MC, RC, LG, MG, RG, LE, ME, RE, LB, MB, RB> copy =
			  new Builder<>(value, leftBuilder, middleBuilder, rightBuilder);
			copy.leftIcon = leftIcon;
			copy.rightIcon = rightIcon;
			copy.leftWeight = leftWeight;
			copy.rightWeight = rightWeight;
			return copy;
		}
	}
	
	@Override public Triple<Object, Object, Object> forActualConfig(@Nullable Triple<LC, MC, RC> value) {
		if (value == null) return null;
		return Triple.of(
		  leftEntry.forActualConfig(value.getLeft()),
		  middleEntry.forActualConfig(value.getMiddle()),
		  rightEntry.forActualConfig(value.getRight()));
	}
	
	@Nullable @Override public Triple<LC, MC, RC> fromActualConfig(@Nullable Object value) {
		LC left;
		MC middle;
		RC right;
		if (value instanceof List<?>) {
			List<?> list = (List<?>) value;
			if (list.size() != 3) return null;
			left = leftEntry.fromActualConfig(list.get(0));
			middle = middleEntry.fromActualConfig(list.get(1));
			right = rightEntry.fromActualConfig(list.get(2));
		} else if (value instanceof Triple) {
			final Triple<?, ?, ?> pair = (Triple<?, ?, ?>) value;
			left = leftEntry.fromActualConfig(pair.getLeft());
			middle = middleEntry.fromActualConfig(pair.getMiddle());
			right = rightEntry.fromActualConfig(pair.getRight());
		} else return null;
		if (left == null && middle == null && right == null) return null;
		if (left == null) left = leftEntry.forConfig(leftEntry.defValue);
		if (middle == null) middle = middleEntry.forConfig(middleEntry.defValue);
		if (right == null) right = rightEntry.forConfig(rightEntry.defValue);
		return Triple.of(left, middle, right);
	}
	
	@Override public List<ITextComponent> getErrorsFromGUI(Triple<LG, MG, RG> value) {
		List<ITextComponent> errors = super.getErrorsFromGUI(value);
		errors.addAll(leftEntry.getErrorsFromGUI(value.getLeft()));
		errors.addAll(middleEntry.getErrorsFromGUI(value.getMiddle()));
		errors.addAll(rightEntry.getErrorsFromGUI(value.getRight()));
		return errors;
	}
	
	@Override public Triple<LC, MC, RC> forConfig(Triple<L, M, R> value) {
		return Triple.of(leftEntry.forConfig(value.getLeft()),
							middleEntry.forConfig(value.getMiddle()),
		               rightEntry.forConfig(value.getRight()));
	}
	
	@Override public @Nullable Triple<L, M, R> fromConfig(@Nullable Triple<LC, MC, RC> value) {
		if (value == null) return null;
		L left = leftEntry.fromConfig(value.getLeft());
		M middle = middleEntry.fromConfig(value.getMiddle());
		R right = rightEntry.fromConfig(value.getRight());
		return left != null && middle != null && right != null? Triple.of(left, middle, right) : null;
	}
	
	@Override public Triple<LG, MG, RG> forGui(Triple<L, M, R> value) {
		return Triple.of(leftEntry.forGui(value.getLeft()),
							middleEntry.forGui(value.getMiddle()),
		               rightEntry.forGui(value.getRight()));
	}
	
	@Override public @Nullable Triple<L, M, R> fromGui(@Nullable Triple<LG, MG, RG> value) {
		if (value == null) return null;
		L left = leftEntry.fromGui(value.getLeft());
		M middle = middleEntry.fromGui(value.getMiddle());
		R right = rightEntry.fromGui(value.getRight());
		return left != null && middle != null && right != null? Triple.of(left, middle, right) : null;
	}
	
	@Override public List<String> getConfigCommentTooltips() {
		List<String> tooltips = super.getConfigCommentTooltips();
		String leftComment = leftEntry.getConfigCommentTooltip();
		String middleComment = middleEntry.getConfigCommentTooltip();
		String rightComment = rightEntry.getConfigCommentTooltip();
		tooltips.add("Triple: " + (leftComment.isEmpty()? "?" : leftComment) + ", " +
		             (middleComment.isEmpty()? "?" : middleComment) + ", " +
		             (rightComment.isEmpty()? "?" : rightComment));
		return tooltips;
	}
	
	@Override public Optional<AbstractConfigListEntry<Triple<LG, MG, RG>>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		TripleListEntryBuilder<LG, MG, RG, ?, ?, ?> entryBuilder = builder.startTriple(
		  getDisplayName(),
		  leftEntry.buildChildGUIEntry(builder),
		  middleEntry.buildChildGUIEntry(builder),
		  rightEntry.buildChildGUIEntry(builder),
		  forGui(get()))
		  .withIcons(leftIcon, rightIcon)
		  .withWeights(leftWeight, rightWeight);
		return Optional.of(decorate(entryBuilder).build());
	}
}
