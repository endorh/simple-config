package endorh.simpleconfig.core.entry;

import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.RangedEntryBuilder;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class AbstractRangedEntry<V extends Comparable<V>, Config, Gui>
  extends AbstractConfigEntry<V, Config, Gui> {
	
	protected double commentMin = Integer.MIN_VALUE;
	protected double commentMax = Integer.MAX_VALUE;
	
	public static abstract class Builder<V extends Comparable<V>, Config, Gui,
	  Entry extends AbstractRangedEntry<V, Config, Gui>,
	  Self extends RangedEntryBuilder<V, Config, Gui, Self>,
	  SelfImpl extends Builder<V, Config, Gui, Entry, Self, SelfImpl>
	> extends AbstractConfigEntryBuilder<V, Config, Gui, Entry, Self, SelfImpl>
	  implements RangedEntryBuilder<V, Config, Gui, Self> {
		
		protected String sliderFormat;
		
		protected V min;
		protected V max;
		protected boolean asSlider;
		protected Function<V, Component> sliderTextSupplier;
		
		public Builder(V value, Class<?> typeClass) {this(value, typeClass, "%d");}
		
		public Builder(V value, Class<?> typeClass, String sliderFormat) {
			super(value, typeClass);
			this.sliderFormat = sliderFormat;
		}
		
		@Override @Contract(pure=true) public @NotNull Self min(V min) {
			SelfImpl copy = copy();
			copy.min = min;
			return copy.castSelf();
		}
		
		@Override @Contract(pure=true) public @NotNull Self max(V max) {
			SelfImpl copy = copy();
			copy.max = max;
			return copy.castSelf();
		}
		
		@Override @Contract(pure=true) public @NotNull Self range(V min, V max) {
			SelfImpl copy = copy();
			copy.min = min;
			copy.max = max;
			return copy.castSelf();
		}
		
		@Override @Contract(pure=true) public @NotNull Self slider() {
			return slider(true);
		}
		
		@Override @Contract(pure=true) public @NotNull Self slider(boolean asSlider) {
			if (asSlider) {
				return slider("simpleconfig.format.slider");
			} else {
				SelfImpl copy = copy();
				copy.asSlider = false;
				return copy.castSelf();
			}
		}
		
		@Override @Contract(pure=true) public @NotNull Self slider(String sliderTextTranslation) {
			return slider(v -> Component.translatable(sliderTextTranslation, String.format(sliderFormat, v)));
		}
		
		@Override @Contract(pure=true) public @NotNull Self slider(
		  Function<V, Component> sliderTextSupplier
		) {
			SelfImpl copy = copy();
			copy.asSlider = true;
			copy.sliderTextSupplier = sliderTextSupplier;
			return copy.castSelf();
		}
		
		protected void checkBounds() {
			if (value.compareTo(min) < 0 || value.compareTo(max) > 0)
				throw new IllegalArgumentException(
				  "Ranged entry's default value is not within bounds");
		}
		
		@Override
		protected final Entry build(@NotNull ConfigEntryHolder parent, String name) {
			checkBounds();
			final Entry e = super.build(parent, name);
			e.min = min;
			e.max = max;
			e.asSlider = asSlider;
			e.sliderTextSupplier = sliderTextSupplier;
			return e;
		}
		
		@Override public SelfImpl copy() {
			final SelfImpl copy = super.copy();
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
	protected Function<V, Component> sliderTextSupplier;
	
	public AbstractRangedEntry(
	  ConfigEntryHolder parent, String name, V value
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
	
	@Override public Optional<Component> getErrorFromGUI(Gui value) {
		Optional<Component> error = super.getErrorFromGUI(value);
		if (error.isPresent()) return error;
		V v = fromGui(value);
		if (v == null) return Optional.of(
		  Component.translatable("simpleconfig.config.error.missing_value"));
		if (min != null && min.compareTo(v) > 0)
			return Optional.of(
			  Component.translatable("simpleconfig.config.error.too_small", coloredBound(min)));
		if (max != null && max.compareTo(v) < 0)
			return Optional.of(
			  Component.translatable("simpleconfig.config.error.too_large", coloredBound(max)));
		return Optional.empty();
	}
	
	protected static MutableComponent coloredBound(Object bound) {
		return Component.literal(String.valueOf(bound))
		  .withStyle(ChatFormatting.DARK_AQUA);
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
	
	@Override public boolean addCommandSuggestions(SuggestionsBuilder builder) {
		super.addCommandSuggestions(builder);
		String minSerialized = forCommand(getMin());
		String maxSerialized = forCommand(getMax());
		if (minSerialized != null) builder.suggest(
		  minSerialized, Component.translatable("simpleconfig.command.suggest.min"));
		if (maxSerialized != null) builder.suggest(
		  maxSerialized, Component.translatable("simpleconfig.command.suggest.max"));
		return true;
	}
}
