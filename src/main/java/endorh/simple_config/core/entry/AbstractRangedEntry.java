package endorh.simple_config.core.entry;

import endorh.simple_config.core.AbstractConfigEntry;
import endorh.simple_config.core.AbstractConfigEntryBuilder;
import endorh.simple_config.core.ISimpleConfigEntryHolder;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class AbstractRangedEntry
  <V extends Comparable<V>, Config, Gui, This extends AbstractRangedEntry<V, Config, Gui, This>>
  extends AbstractConfigEntry<V, Config, Gui, This> {
	
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
		public Self min(V min) {
			Self copy = copy();
			copy.min = min;
			return copy;
		}
		
		/**
		 * Set max (inclusive)
		 */
		public Self max(V max) {
			Self copy = copy();
			copy.max = max;
			return copy;
		}
		
		/**
		 * Set range (inclusive)
		 */
		public Self range(V min, V max) {
			Self copy = copy();
			copy.min = min;
			copy.max = max;
			return copy;
		}
		
		/**
		 * Display as slider
		 */
		public Self slider() {
			return slider(true);
		}
		
		/**
		 * Display or not as slider
		 */
		public Self slider(boolean asSlider) {
			if (asSlider) {
				return slider("simple-config.format.slider");
			} else {
				Self copy = copy();
				copy.asSlider = false;
				return copy;
			}
		}
		
		/**
		 * Display as slider with given translation key as slider text.
		 */
		public Self slider(String sliderTextTranslation) {
			return slider(v -> new TranslationTextComponent(
			  sliderTextTranslation, String.format(sliderFormat, v)));
		}
		
		/**
		 * Display as slider with given text supplier.
		 */
		public Self slider(Function<V, ITextComponent> sliderTextSupplier) {
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
		protected final Entry build(ISimpleConfigEntryHolder parent, String name) {
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
	
	@Override
	protected ForgeConfigSpec.Builder decorate(ForgeConfigSpec.Builder builder) {
		builder = super.decorate(builder);
		String com = comment != null ? comment + "\n" : "";
		com += " Range: " + getRangeComment();
		builder.comment(com);
		return builder;
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
	
	@Override
	protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.of(decorate(builder).define(name, value, configValidator()));
	}
	
	@Override
	protected Predicate<Object> configValidator() {
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
			return super.configValidator().test(o);
		};
	}
}
