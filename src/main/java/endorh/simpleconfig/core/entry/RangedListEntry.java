package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.ui.impl.builders.RangedListFieldBuilder;
import endorh.simpleconfig.core.ISimpleConfigEntryHolder;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
		public Self min(@Nonnull V min) {
			Self copy = copy();
			copy.min = min;
			copy = copy.elemError(clamp(validator));
			return copy;
		}
		
		/**
		 * Set the maximum allowed value for the elements of this list entry (inclusive)
		 */
		public Self max(@Nonnull V max) {
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
		if (max instanceof Number) {
			final Number x = (Number) max, n = (Number) min;
			if (x.doubleValue() >= commentMax && n.doubleValue() <= commentMin) {
				return "~";
			} else if (x.doubleValue() >= commentMax) {
				return "> " + n;
			} else if (n.doubleValue() <= commentMin) {
				return "< " + x;
			} else return n + " ~ " + x;
		}
		return min + " ~ " + max;
	}
	
	@Override protected ForgeConfigSpec.Builder decorate(
	  ForgeConfigSpec.Builder builder
	) {
		builder = super.decorate(builder);
		String com = comment != null? comment + "\n" : "";
		com += " Range: " + getRangeComment();
		builder.comment(com);
		return builder;
	}
	
	@OnlyIn(Dist.CLIENT)
	protected <F extends RangedListFieldBuilder<Gui, ?, ?, F>> F decorate(F builder) {
		builder = super.decorate(builder);
		builder.setMin(elemForGui(min)).setMax(elemForGui(max));
		return builder;
	}
}
