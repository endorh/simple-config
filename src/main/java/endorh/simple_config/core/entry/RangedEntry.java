package endorh.simple_config.core.entry;

import endorh.simple_config.core.AbstractConfigEntry;
import endorh.simple_config.core.ISimpleConfigEntryHolder;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class RangedEntry
  <V extends Comparable<V>, Config, Gui, This extends RangedEntry<V, Config, Gui, This>>
  extends AbstractConfigEntry<V, Config, Gui, This> {
	protected V min;
	protected V max;
	protected boolean asSlider = false;
	protected String sliderFormat;
	protected Function<V, ITextComponent> sliderTextSupplier;
	
	public RangedEntry(V value, V min, V max, Class<?> typeClass) {
		this(value, min, max, "%d", typeClass);
	}
	
	public RangedEntry(V value, V min, V max, String sliderFormat, Class<?> typeClass) {
		super(value, typeClass);
		this.min = min;
		this.max = max;
		this.sliderFormat = sliderFormat;
	}
	
	protected void checkBounds() {}
	
	/**
	 * Set the minimum value allowed (inclusive)
	 */
	public This min(V min) {
		this.min = min;
		return self();
	}
	
	/**
	 * Set the maximum value allowed (inclusive)
	 */
	public This max(V max) {
		this.max = max;
		return self();
	}
	
	/**
	 * Set the minimum and maximum values allowed (inclusive)
	 */
	public This range(V min, V max) {
		this.min = min;
		this.max = max;
		return self();
	}
	
	public This slider() {
		return slider(v -> new TranslationTextComponent(
		  "simple-config.config.format.slider",
		  String.format(sliderFormat, v)));
	}
	
	public This slider(Function<V, ITextComponent> sliderTextSupplier) {
		this.asSlider = true;
		this.sliderTextSupplier = sliderTextSupplier;
		return self();
	}
	
	@Override protected void setParent(ISimpleConfigEntryHolder config) {
		super.setParent(config);
		checkBounds();
	}
	
	@Override
	protected Builder decorate(Builder builder) {
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
	protected Optional<ConfigValue<?>> buildConfigEntry(Builder builder) {
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
