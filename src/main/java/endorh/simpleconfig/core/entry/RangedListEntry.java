package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.core.ISimpleConfigEntryHolder;
import endorh.simpleconfig.ui.impl.builders.RangedListFieldBuilder;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public abstract class RangedListEntry<
  V extends Comparable<V>, Config, Gui extends Comparable<Gui>,
  Self extends RangedListEntry<V, Config, Gui, Self>>
  extends AbstractListEntry<V, Config, Gui, Self> {
	protected V min;
	protected V max;
	
	protected double commentMin = Integer.MIN_VALUE;
	protected double commentMax = Integer.MAX_VALUE;
	
	public RangedListEntry(
	  ISimpleConfigEntryHolder parent, String name, @Nullable List<V> value
	) {
		super(parent, name, value);
	}
	
	public static abstract class Builder<V extends Comparable<V>, Config,
	  Gui extends Comparable<Gui>,
	  Entry extends RangedListEntry<V, Config, Gui, Entry>,
	  Self extends Builder<V, Config, Gui, Entry, Self>>
	  extends AbstractListEntry.Builder<V, Config, Gui, Entry, Self> {
		
		protected V min = null;
		protected V max = null;
		
		public Builder(List<V> value, Class<?> innerType) {
			super(value, innerType);
		}
		
		/**
		 * Set the minimum allowed value for the elements of this list entry (inclusive)
		 */
		public Self min(@NotNull V min) {
			Self copy = copy();
			copy.min = min;
			copy = copy.elemError(clamp(validator));
			return copy;
		}
		
		/**
		 * Set the maximum allowed value for the elements of this list entry (inclusive)
		 */
		public Self max(@NotNull V max) {
			Self copy = copy();
			copy.max = max;
			copy = copy.elemError(clamp(validator));
			return copy;
		}
		
		/**
		 * Set the minimum and the maximum allowed for the elements of this list entry (inclusive)
		 */
		public Self range(V min, V max) {
			Self copy = copy();
			copy.min = min;
			copy.max = max;
			copy = copy.elemError(clamp(validator));
			return copy;
		}
		
		@Override
		public Self elemError(Function<V, Optional<ITextComponent>> validator) {
			return super.elemError(clamp(validator));
		}
		
		protected Function<V, Optional<ITextComponent>> clamp(
		  @Nullable Function<V, Optional<ITextComponent>> validator
		) {
			checkBounds();
			return t -> {
				if (t.compareTo(min) < 0)
					return Optional
					  .of(new TranslationTextComponent("text.cloth-config.error.too_small", min));
				if (t.compareTo(max) > 0)
					return Optional
					  .of(new TranslationTextComponent("text.cloth-config.error.too_large", max));
				return validator != null ? validator.apply(t) : Optional.empty();
			};
		}
		
		protected void checkBounds() {}
		
		@Override
		protected Entry build(ISimpleConfigEntryHolder parent, String name) {
			checkBounds();
			final Entry e = super.build(parent, name);
			e.min = min;
			e.max = max;
			return e;
		}
		
		@Override protected Self copy() {
			final Self copy = super.copy();
			copy.min = min;
			copy.max = max;
			return copy;
		}
	}
	
	protected String getRangeComment() {
		if (max == null && min == null) return "~";
		if (max instanceof Number || min instanceof Number) {
			assert max == null || max instanceof Number;
			assert min == null || min instanceof Number;
			final Number x = (Number) max, n = (Number) min;
			boolean noMax = x == null || x.doubleValue() >= commentMax;
			boolean noMin = n == null || n.doubleValue() <= commentMin;
			if (noMax && noMin) return "~";
			if (noMax) return ">= " + n;
			if (noMin) return "<= " + x;
			return n + " ~ " + x;
		}
		if (max == null) return ">= " + min;
		if (min == null) return "<= " + max;
		return min + " ~ " + max;
	}
	
	@Override protected @Nullable String getListTypeComment() {
		return innerType.getSimpleName() + ", Range: " + getRangeComment();
	}
	
	@OnlyIn(Dist.CLIENT)
	protected <F extends RangedListFieldBuilder<Gui, ?, ?, F>> F decorate(F builder) {
		builder = super.decorate(builder);
		builder.setMin(elemForGui(min)).setMax(elemForGui(max));
		return builder;
	}
}
