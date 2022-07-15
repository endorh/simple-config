package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.core.*;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.gui.Icon;
import endorh.simpleconfig.ui.impl.builders.PairListEntryBuilder;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.List;
import java.util.Optional;

public class EntryPairEntry<
  L, R, LC, RC, LG, RG,
  LE extends AbstractConfigEntry<L, LC, LG, LE> & IKeyEntry<LG>,
  RE extends AbstractConfigEntry<R, RC, RG, RE> & IKeyEntry<RG>
> extends AbstractConfigEntry<
  Pair<L, R>, Pair<LC, RC>, Pair<LG, RG>,
  EntryPairEntry<L, R, LC, RC, LG, RG, LE, RE>
> implements IKeyEntry<Pair<LG, RG>> {
	protected final LE leftEntry;
	protected final RE rightEntry;
	protected float splitPos = 0.5F;
	protected @Nullable Icon middleIcon;
	
	protected EntryPairEntry(
	  ISimpleConfigEntryHolder parent, String name, Pair<L, R> value,
	  LE leftEntry, RE rightEntry
	) {
		super(parent, name, value);
		this.leftEntry = leftEntry.withSaver((v, h) -> {});
		this.rightEntry = rightEntry.withSaver((v, h) -> {});
	}
	
	public static class Builder<
	  L, R, LC, RC, LG, RG,
	  LE extends AbstractConfigEntry<L, LC, LG, LE> & IKeyEntry<LG>,
	  RE extends AbstractConfigEntry<R, RC, RG, RE> & IKeyEntry<RG>,
	  LB extends AbstractConfigEntryBuilder<L, LC, LG, LE, LB>,
	  RB extends AbstractConfigEntryBuilder<R, RC, RG, RE, RB>
	> extends AbstractConfigEntryBuilder<
	  Pair<L, R>, Pair<LC, RC>, Pair<LG, RG>,
	  EntryPairEntry<L, R, LC, RC, LG, RG, LE, RE>,
	  EntryPairEntry.Builder<L, R, LC, RC, LG, RG, LE, RE, LB, RB>
	> {
		protected LB leftBuilder;
		protected RB rightBuilder;
		protected float splitPos = 0.5F;
		protected @Nullable Icon middleIcon;
		
		public Builder(Pair<L, R> value, LB leftBuilder, RB rightBuilder) {
			super(value, Pair.class);
			this.leftBuilder = leftBuilder;
			this.rightBuilder = rightBuilder;
		}
		
		public Builder<L, R, LC, RC, LG, RG, LE, RE, LB, RB> withMiddleIcon(@Nullable Icon icon) {
			final Builder<L, R, LC, RC, LG, RG, LE, RE, LB, RB> copy = copy();
			copy.middleIcon = icon;
			return copy;
		}
		
		public Builder<L, R, LC, RC, LG, RG, LE, RE, LB, RB> withSplitPosition(
		  @Range(from = 0, to = 1) float splitPosition
		) {
			if (splitPosition < 0 || splitPosition > 1) throw new IllegalArgumentException(
			  "Split position must be between 0~1. Specifically: " + splitPosition);
			final Builder<L, R, LC, RC, LG, RG, LE, RE, LB, RB> copy = copy();
			copy.splitPos = splitPosition;
			return copy;
		}
		
		@Override protected EntryPairEntry<L, R, LC, RC, LG, RG, LE, RE> buildEntry(
		  ISimpleConfigEntryHolder parent, String name
		) {
			LE leftEntry = DummyEntryHolder.build(parent, leftBuilder).withSaver((v, h) -> {});
			RE rightEntry = DummyEntryHolder.build(parent, rightBuilder).withSaver((v, h) -> {});
			final EntryPairEntry<L, R, LC, RC, LG, RG, LE, RE> entry =
			  new EntryPairEntry<>(parent, name, value, leftEntry, rightEntry);
			entry.middleIcon = middleIcon;
			entry.splitPos = splitPos;
			return entry;
		}
		
		@Override protected Builder<L, R, LC, RC, LG, RG, LE, RE, LB, RB> createCopy() {
			final Builder<L, R, LC, RC, LG, RG, LE, RE, LB, RB> copy =
			  new Builder<>(value, copyBuilder(leftBuilder), copyBuilder(rightBuilder));
			copy.middleIcon = middleIcon;
			copy.splitPos = splitPos;
			return copy;
		}
	}
	
	@Override public Pair<Object, Object> forActualConfig(@Nullable Pair<LC, RC> value) {
		if (value == null) return null;
		return Pair.of(leftEntry.forActualConfig(value.getLeft()), rightEntry.forActualConfig(value.getRight()));
	}
	
	@Nullable @Override public Pair<LC, RC> fromActualConfig(@Nullable Object value) {
		LC left;
		RC right;
		if (value instanceof List<?>) {
			List<?> list = (List<?>) value;
			if (list.size() != 2) return null;
			left = leftEntry.fromActualConfig(list.get(0));
			right = rightEntry.fromActualConfig(list.get(1));
		} else if (value instanceof Pair) {
			final Pair<?, ?> pair = (Pair<?, ?>) value;
			left = leftEntry.fromActualConfig(pair.getLeft());
			right = rightEntry.fromActualConfig(pair.getRight());
		} else return null;
		if (left == null && right == null) return null;
		if (left == null) left = leftEntry.forConfig(leftEntry.defValue);
		if (right == null) right = rightEntry.forConfig(rightEntry.defValue);
		return Pair.of(left, right);
	}
	
	@Override public List<ITextComponent> getErrors(Pair<LG, RG> value) {
		List<ITextComponent> errors = super.getErrors(value);
		errors.addAll(leftEntry.getErrors(value.getLeft()));
		errors.addAll(rightEntry.getErrors(value.getRight()));
		return errors;
	}
	
	@Override public Pair<LC, RC> forConfig(Pair<L, R> value) {
		return Pair.of(leftEntry.forConfig(value.getLeft()),
		               rightEntry.forConfig(value.getRight()));
	}
	
	@Override public @Nullable Pair<L, R> fromConfig(@Nullable Pair<LC, RC> value) {
		if (value == null) return null;
		return Pair.of(leftEntry.fromConfigOrDefault(value.getLeft()),
		               rightEntry.fromConfigOrDefault(value.getRight()));
	}
	
	@Override public Pair<LG, RG> forGui(Pair<L, R> value) {
		return Pair.of(leftEntry.forGui(value.getLeft()),
		               rightEntry.forGui(value.getRight()));
	}
	
	@Override public @Nullable Pair<L, R> fromGui(@Nullable Pair<LG, RG> value) {
		if (value == null) return null;
		return Pair.of(leftEntry.fromGuiOrDefault(value.getLeft()),
		               rightEntry.fromGuiOrDefault(value.getRight()));
	}
	
	@Override protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.of(decorate(builder).define(name, forActualConfig(forConfig(defValue)), createConfigValidator()));
	}
	
	@Override public List<String> getConfigCommentTooltips() {
		List<String> tooltips = super.getConfigCommentTooltips();
		String leftComment = leftEntry.getConfigCommentTooltip();
		String rightComment = rightEntry.getConfigCommentTooltip();
		tooltips.add("Pair: " + (leftComment.isEmpty()? "?" : leftComment) + ", " +
		             (rightComment.isEmpty()? "?" : rightComment));
		return tooltips;
	}
	
	@Override public Optional<AbstractConfigListEntry<Pair<LG, RG>>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		PairListEntryBuilder<LG, RG, ?, ?> entryBuilder = builder.startPair(
		  getDisplayName(),
		  leftEntry.buildChildGUIEntry(builder),
		  rightEntry.buildChildGUIEntry(builder),
		  forGui(get()))
		  .withMiddleIcon(middleIcon)
		  .withSplitPos(splitPos);
		return Optional.of(decorate(entryBuilder).build());
	}
}
