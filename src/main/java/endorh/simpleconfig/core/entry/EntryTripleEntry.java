package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.EntryTripleEntryBuilder;
import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.core.*;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import endorh.simpleconfig.ui.impl.builders.TripleListEntryBuilder;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.List;
import java.util.Optional;

public class EntryTripleEntry<
  L, M, R, LC, MC, RC, LG, MG, RG
> extends AbstractConfigEntry<
  Triple<L, M, R>, Triple<LC, MC, RC>, Triple<LG, MG, RG>
> implements AtomicEntry<Triple<LG, MG, RG>> {
	protected final AbstractConfigEntry<L, LC, LG> leftEntry;
	protected final AbstractConfigEntry<M, MC, MG> middleEntry;
	protected final AbstractConfigEntry<R, RC, RG> rightEntry;
	
	protected @Nullable Icon leftIcon;
	protected @Nullable Icon rightIcon;
	protected float leftWeight = 0.333F;
	protected float rightWeight = 0.333F;
	
	protected EntryTripleEntry(
	  ConfigEntryHolder parent, String name, Triple<L, M, R> value,
	  AbstractConfigEntry<L, LC, LG> leftEntry,
	  AbstractConfigEntry<M, MC, MG> middleEntry,
	  AbstractConfigEntry<R, RC, RG> rightEntry
	) {
		super(parent, name, value);
		this.leftEntry = leftEntry;
		this.middleEntry = middleEntry;
		this.rightEntry = rightEntry;
	}
	
	@SuppressWarnings("unchecked") protected
	<E extends AbstractConfigEntry<L, LC, LG> & AtomicEntry<LG>> E getLeftEntry() {
		return (E) leftEntry;
	}
	
	@SuppressWarnings("unchecked") protected
	<E extends AbstractConfigEntry<M, MC, MG> & AtomicEntry<MG>> E getMiddleEntry() {
		return (E) middleEntry;
	}
	
	@SuppressWarnings("unchecked") protected
	<E extends AbstractConfigEntry<R, RC, RG> & AtomicEntry<RG>> E getRightEntry() {
		return (E) rightEntry;
	}
	
	public static class Builder<
	  L, M, R, LC, MC, RC, LG, MG, RG,
	  LS extends ConfigEntryBuilder<L, LC, LG, LS> & AtomicEntryBuilder,
	  MS extends ConfigEntryBuilder<M, MC, MG, MS> & AtomicEntryBuilder,
	  RS extends ConfigEntryBuilder<R, RC, RG, RS> & AtomicEntryBuilder,
	  LB extends AbstractConfigEntryBuilder<L, LC, LG, ?, LS, LB> & AtomicEntryBuilder,
	  MB extends AbstractConfigEntryBuilder<M, MC, MG, ?, MS, MB> & AtomicEntryBuilder,
	  RB extends AbstractConfigEntryBuilder<R, RC, RG, ?, RS, RB> & AtomicEntryBuilder
	> extends AbstractConfigEntryBuilder<
	  Triple<L, M, R>, Triple<LC, MC, RC>, Triple<LG, MG, RG>,
	  EntryTripleEntry<L, M, R, LC, MC, RC, LG, MG, RG>,
	  EntryTripleEntryBuilder<L, M, R, LC, MC, RC, LG, MG, RG>,
	  Builder<L, M, R, LC, MC, RC, LG, MG, RG, LS, MS, RS, LB, MB, RB>
	> implements EntryTripleEntryBuilder<L, M, R, LC, MC, RC, LG, MG, RG> {
		protected LB leftBuilder;
		protected MB middleBuilder;
		protected RB rightBuilder;
		
		protected @Nullable Icon leftIcon;
		protected @Nullable Icon rightIcon;
		protected float leftWeight = 0.333F;
		protected float rightWeight = 0.333F;
		
		@SuppressWarnings("unchecked") public Builder(
		  Triple<L, M, R> value, LS leftBuilder, MS middleBuilder, RS rightBuilder
		) {
			super(value, EntryType.of(Triple.class, getEntryType(leftBuilder), getEntryType(middleBuilder), getEntryType(rightBuilder)));
			if (!(leftBuilder instanceof AbstractConfigEntryBuilder)) throw new IllegalArgumentException(
			  "Mixed API use: builder not subclass of AbstractConfigEntryBuilder");
			if (!(middleBuilder instanceof AbstractConfigEntryBuilder)) throw new IllegalArgumentException(
			  "Mixed API use: builder not subclass of AbstractConfigEntryBuilder");
			if (!(rightBuilder instanceof AbstractConfigEntryBuilder)) throw new IllegalArgumentException(
			  "Mixed API use: builder not subclass of AbstractConfigEntryBuilder");
			this.leftBuilder = (LB) leftBuilder;
			this.middleBuilder = (MB) middleBuilder;
			this.rightBuilder = (RB) rightBuilder;
		}
		
		public Builder(
		  Triple<L, M, R> value, LB leftBuilder, MB middleBuilder, RB rightBuilder
		) {
			super(value, EntryType.of(Triple.class, getEntryType(leftBuilder), getEntryType(middleBuilder), getEntryType(rightBuilder)));
			this.leftBuilder = leftBuilder;
			this.middleBuilder = middleBuilder;
			this.rightBuilder = rightBuilder;
		}
		
		@Override @Contract(pure=true)
		public Builder<L, M, R, LC, MC, RC, LG, MG, RG, LS, MS, RS, LB, MB, RB> withLeftIcon(
		  @Nullable Icon leftIcon
		) {
			final Builder<L, M, R, LC, MC, RC, LG, MG, RG, LS, MS, RS, LB, MB, RB> copy = copy();
			copy.leftIcon = leftIcon;
			return copy;
		}
		
		@Override @Contract(pure=true)
		public @NotNull Builder<L, M, R, LC, MC, RC, LG, MG, RG, LS, MS, RS, LB, MB, RB> withRightIcon(
		  @Nullable Icon rightIcon
		) {
			final Builder<L, M, R, LC, MC, RC, LG, MG, RG, LS, MS, RS, LB, MB, RB> copy = copy();
			copy.rightIcon = rightIcon;
			return copy;
		}
		
		@Override @Contract(pure=true)
		public @NotNull Builder<L, M, R, LC, MC, RC, LG, MG, RG, LS, MS, RS, LB, MB, RB> withIcons(
		  @Nullable Icon leftIcon, @Nullable Icon rightIcon
		) {
			final Builder<L, M, R, LC, MC, RC, LG, MG, RG, LS, MS, RS, LB, MB, RB> copy = copy();
			copy.leftIcon = leftIcon;
			copy.rightIcon = rightIcon;
			return copy;
		}
		
		@Override @Contract(pure=true)
		public Builder<L, M, R, LC, MC, RC, LG, MG, RG, LS, MS, RS, LB, MB, RB> withWeights(
		  @Range(from=0, to=1) double leftWeight, @Range(from=0, to=1) double rightWeight
		) {
			if (leftWeight < 0 || rightWeight < 0) throw new IllegalArgumentException(
			  "Weights can be negative. Specifically: " + leftWeight + ", " + rightWeight);
			if (leftWeight + rightWeight > 1) throw new IllegalArgumentException(
			  "Weights must add to less than 1. Specifically: " + leftWeight + ", " + rightWeight);
			final Builder<L, M, R, LC, MC, RC, LG, MG, RG, LS, MS, RS, LB, MB, RB> copy = copy();
			copy.leftWeight = (float) leftWeight;
			copy.rightWeight = (float) rightWeight;
			return copy;
		}
		
		@Override protected EntryTripleEntry<L, M, R, LC, MC, RC, LG, MG, RG> buildEntry(
		  ConfigEntryHolder parent, String name
		) {
			AbstractConfigEntry<L, LC, LG> leftEntry = DummyEntryHolder.build(parent, leftBuilder);
			AbstractConfigEntry<M, MC, MG> middleEntry = DummyEntryHolder.build(parent, middleBuilder);
			AbstractConfigEntry<R, RC, RG> rightEntry = DummyEntryHolder.build(parent, rightBuilder);
			if (!(leftEntry instanceof AtomicEntry)) throw new IllegalStateException(
			  "KeyEntryBuilder produced a non-key entry, violating its contract: " + leftBuilder.getClass().getSimpleName());
			if (!(middleEntry instanceof AtomicEntry)) throw new IllegalStateException(
			  "KeyEntryBuilder produced a non-key entry, violating its contract: " + middleBuilder.getClass().getSimpleName());
			if (!(rightEntry instanceof AtomicEntry)) throw new IllegalStateException(
			  "KeyEntryBuilder produced a non-key entry, violating its contract: " + rightBuilder.getClass().getSimpleName());
			final EntryTripleEntry<L, M, R, LC, MC, RC, LG, MG, RG> entry =
			  new EntryTripleEntry<>(parent, name, value, leftEntry, middleEntry, rightEntry);
			entry.leftIcon = leftIcon;
			entry.rightIcon = rightIcon;
			entry.leftWeight = leftWeight;
			entry.rightWeight = rightWeight;
			return entry;
		}
		
		@Contract(value="_ -> new", pure=true) @Override
		protected Builder<L, M, R, LC, MC, RC, LG, MG, RG, LS, MS, RS, LB, MB, RB> createCopy(
		  Triple<L, M, R> value
		) {
			Builder<L, M, R, LC, MC, RC, LG, MG, RG, LS, MS, RS, LB, MB, RB> copy = new Builder<>(
			  Triple.of(value.getLeft(), value.getMiddle(), value.getRight()),
			  leftBuilder, middleBuilder, rightBuilder);
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
		if (value instanceof List<?> list) {
			if (list.size() != 3) return null;
			left = leftEntry.fromActualConfig(list.get(0));
			middle = middleEntry.fromActualConfig(list.get(1));
			right = rightEntry.fromActualConfig(list.get(2));
		} else if (value instanceof final Triple<?, ?, ?> pair) {
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
	
	@Override public List<Component> getErrorsFromGUI(Triple<LG, MG, RG> value) {
		List<Component> errors = super.getErrorsFromGUI(value);
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
	
	@SuppressWarnings("unchecked") public <
	  LGE extends AbstractConfigListEntry<LG> & IChildListEntry,
	  MGE extends AbstractConfigListEntry<MG> & IChildListEntry,
	  RGE extends AbstractConfigListEntry<RG> & IChildListEntry,
	  LGEB extends FieldBuilder<LG, LGE, LGEB>,
	  MGEB extends FieldBuilder<MG, MGE, MGEB>,
	  RGEB extends FieldBuilder<RG, RGE, RGEB>
	> TripleListEntryBuilder<LG, MG, RG, LGE, MGE, RGE, LGEB, MGEB, RGEB> makeGUIBuilder(
	  ConfigFieldBuilder builder,
	  FieldBuilder<LG, LGE, ?> leftBuilder,
	  FieldBuilder<MG, MGE, ?> middleBuilder,
	  FieldBuilder<RG, RGE, ?> rightBuilder
	) {
		return builder.startTriple(
		  getDisplayName(), (LGEB) leftBuilder, (MGEB) middleBuilder,
		  (RGEB) rightBuilder, forGui(get()));
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override public Optional<FieldBuilder<Triple<LG, MG, RG>, ?, ?>> buildGUIEntry(
	  ConfigFieldBuilder builder
	) {
		TripleListEntryBuilder<LG, MG, RG, ?, ?, ?, ?, ?, ?> entryBuilder = makeGUIBuilder(
		  builder, getLeftEntry().buildAtomicChildGUIEntry(builder),
		  getMiddleEntry().buildAtomicChildGUIEntry(builder),
		  getRightEntry().buildAtomicChildGUIEntry(builder))
		  .withIcons(leftIcon, rightIcon)
		  .withWeights(leftWeight, rightWeight);
		return Optional.of(decorate(entryBuilder));
	}
}
