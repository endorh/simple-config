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
	
	public static abstract class Builder<V extends Comparable<V>, Config, Gui,
	  Entry extends AbstractRangedEntry<V, Config, Gui, Entry>,
	  Self extends Builder<V, Config, Gui, Entry, Self>>
	  extends AbstractConfigEntryBuilder<V, Config, Gui, Entry, Self> {
		
		protected String sliderFormat;
		
		protected V min;
		protected V max;
		protected boolean asSlider;
		protected Function<V, ITextComponent> sliderTextSupplier;
		
		public Builder(V value, Class<?> typeClass) { this(value, typeClass, "%d"); }
		
		public Builder(V value, Class<?> typeClass, String sliderFormat) {
			super(value, typeClass);
			this.sliderFormat = sliderFormat;
		}
		
		public Self min(V min) {
			this.min = min;
			return self();
		}
		
		public Self max(V max) {
			this.max = max;
			return self();
		}
		
		public Self range(V min, V max) {
			this.min = min;
			this.max = max;
			return self();
		}
		
		public Self slider() {
			return slider(true);
		}
		
		public Self slider(boolean asSlider) {
			if (asSlider) {
				return slider(v -> new TranslationTextComponent(
				  "simple-config.config.format.slider",
				  String.format(sliderFormat, v)));
			} else {
				this.asSlider = false;
				return self();
			}
		}
		
		public Self slider(Function<V, ITextComponent> sliderTextSupplier) {
			this.asSlider = true;
			this.sliderTextSupplier = sliderTextSupplier;
			return self();
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
	}
	
	protected V min;
	protected V max;
	protected boolean asSlider;
	protected Function<V, ITextComponent> sliderTextSupplier;
	
	public AbstractRangedEntry(
	  ISimpleConfigEntryHolder parent, String name,
	  V value, Class<?> typeClass
	) {
		super(parent, name, value);
	}
	
	@Override
	protected ForgeConfigSpec.Builder decorate(ForgeConfigSpec.Builder builder) {
		builder = super.decorate(builder);
		String com = comment != null? comment + "\n" : "";
		com += " Range: " + getRangeComment();
		builder.comment(com);
		return builder;
	}
	
	protected String getRangeComment() {
		/*if (max instanceof Number) {
			final Number x = (Number) max, n = (Number) min;
			if (x.intValue() == Integer.MAX_VALUE && n.intValue() == Integer.MIN_VALUE) {
				return "~";
			} else if (x.intValue() == Integer.MAX_VALUE) {
				return "> " + n;
			} else if (n.intValue() == Integer.MIN_VALUE) {
				return "< " + x;
			} else return n + " ~ " + x;
		}*/
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
