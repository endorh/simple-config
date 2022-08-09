package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractConfigEntryBuilder;
import endorh.simpleconfig.core.ISimpleConfigEntryHolder;
import net.minecraft.util.text.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class AbstractRangedEntry
  <V extends Comparable<V>, Config, Gui, This extends AbstractRangedEntry<V, Config, Gui, This>>
  extends AbstractConfigEntry<V, Config, Gui> {
	
	protected double commentMin = Integer.MIN_VALUE;
	protected double commentMax = Integer.MAX_VALUE;
	
	public static abstract class Builder<V extends Comparable<V>, Config, Gui,
	  Entry extends AbstractRangedEntry<V, Config, Gui, Entry>,
	  Self extends Builder<V, Config, Gui, Entry, Self>>
	  extends AbstractConfigEntryBuilder<V, Config, Gui, Entry, Self> {
		
		protected String sliderFormat;
		
		protected V min;
		protected V max;
		protected boolean asSlider;
		protected Function<V, ITextComponent> sliderTextSupplier;
		
		public Builder(V value, Class<?> typeClass) {this(value, typeClass, "%d");}
		
		public Builder(V value, Class<?> typeClass, String sliderFormat) {
			super(value, typeClass);
			this.sliderFormat = sliderFormat;
		}
		
		/**
		 * Set min (inclusive)
		 */
		@Contract(pure=true) public Self min(V min) {
			Self copy = copy();
			copy.min = min;
			return copy;
		}
		
		/**
		 * Set max (inclusive)
		 */
		@Contract(pure=true) public Self max(V max) {
			Self copy = copy();
			copy.max = max;
			return copy;
		}
		
		/**
		 * Set range (inclusive)
		 */
		@Contract(pure=true) public Self range(V min, V max) {
			Self copy = copy();
			copy.min = min;
			copy.max = max;
			return copy;
		}
		
		/**
		 * Display as slider
		 */
		@Contract(pure=true) public Self slider() {
			return slider(true);
		}
		
		/**
		 * Display or not as slider
		 */
		@Contract(pure=true) public Self slider(boolean asSlider) {
			if (asSlider) {
				return slider("simpleconfig.format.slider");
			} else {
				Self copy = copy();
				copy.asSlider = false;
				return copy;
			}
		}
		
		/**
		 * Display as slider with given translation key as slider text.
		 */
		@Contract(pure=true) public Self slider(String sliderTextTranslation) {
			return slider(v -> new TranslationTextComponent(
			  sliderTextTranslation, String.format(sliderFormat, v)));
		}
		
		/**
		 * Display as slider with given text supplier.
		 */
		@Contract(pure=true) public Self slider(Function<V, ITextComponent> sliderTextSupplier) {
			Self copy = copy();
			copy.asSlider = true;
			copy.sliderTextSupplier = sliderTextSupplier;
			return copy;
		}
		
		protected void checkBounds() {
			if (value.compareTo(min) < 0 || value.compareTo(max) > 0)
				throw new IllegalArgumentException(
				  "Ranged entry's default value is not within bounds");
		}
		
		@Override
		protected final Entry build(@NotNull ISimpleConfigEntryHolder parent, String name) {
			checkBounds();
			final Entry e = super.build(parent, name);
			e.min = min;
			e.max = max;
			e.asSlider = asSlider;
			e.sliderTextSupplier = sliderTextSupplier;
			return e;
		}
		
		@Override protected Self copy() {
			final Self copy = super.copy();
			copy.sliderFormat = sliderFormat;
			copy.min = min;
			copy.max = max;
			copy.asSlider = asSlider;
			copy.sliderTextSupplier = sliderTextSupplier;
			return copy;
		}
	}
	
	protected V min;
	protected V max;
	protected boolean asSlider;
	protected Function<V, ITextComponent> sliderTextSupplier;
	
	public AbstractRangedEntry(
	  ISimpleConfigEntryHolder parent, String name, V value
	) {
		super(parent, name, value);
	}
	
	@Override public List<String> getConfigCommentTooltips() {
		List<String> tooltips = super.getConfigCommentTooltips();
		String typeComment = getTypeComment();
		if (typeComment != null) tooltips.add(typeComment);
		tooltips.add("Range: " + getRangeComment());
		return tooltips;
	}
	
	public V getMin() {
		return min;
	}
	public V getMax() {
		return max;
	}
	
	protected @Nullable String getTypeComment() {
		return typeClass.getSimpleName();
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
	
	@Override public Optional<ITextComponent> getErrorFromGUI(Gui value) {
		Optional<ITextComponent> error = super.getErrorFromGUI(value);
		if (error.isPresent()) return error;
		V v = fromGui(value);
		if (v == null) return Optional.of(new TranslationTextComponent(
		  "simpleconfig.config.error.missing_value"));
		if (min != null && min.compareTo(v) > 0)
			return Optional.of(new TranslationTextComponent(
			  "simpleconfig.config.error.too_small", coloredBound(min)));
		if (max != null && max.compareTo(v) < 0)
			return Optional.of(new TranslationTextComponent(
		  "simpleconfig.config.error.too_large", coloredBound(max)));
		return Optional.empty();
	}
	
	protected static IFormattableTextComponent coloredBound(Object bound) {
		return new StringTextComponent(String.valueOf(bound))
		  .mergeStyle(TextFormatting.DARK_AQUA);
	}
	
	@Override
	protected Predicate<Object> createConfigValidator() {
		return o -> {
			if (o == null)
				return false;
			try {
				if (min instanceof Number || max instanceof Number) {
					final Number n = (Number) o;
					//noinspection ConstantConditions
					if (min != null && ((Number) min).doubleValue() > n.doubleValue()
					    || max != null && ((Number) max).doubleValue() < n.doubleValue())
						return false;
				} else {
					//noinspection unchecked
					final V v = (V) o;
					if (min.compareTo(v) > 0 || max.compareTo(v) < 0)
						return false;
				}
			} catch (ClassCastException ignored) {
				return false;
			}
			return super.createConfigValidator().test(o);
		};
	}
}
