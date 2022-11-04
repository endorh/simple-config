package endorh.simpleconfig.core.entry;

import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import endorh.simpleconfig.api.AbstractRange;
import endorh.simpleconfig.api.AbstractRange.AbstractSizedRange;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.RangeEntryBuilder;
import endorh.simpleconfig.api.entry.SizedRangeEntryBuilder;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractConfigEntryBuilder;
import endorh.simpleconfig.core.AtomicEntry;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import endorh.simpleconfig.ui.impl.builders.RangeListEntryBuilder;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractRangeEntry<
  V extends Comparable<V>, R extends AbstractRange<V, R>
  > extends AbstractConfigEntry<R, String, R> implements AtomicEntry<R> {
	protected @Nullable V min = null;
	protected @Nullable V max = null;
	protected boolean canEditMinExclusiveness = false;
	protected boolean canEditMaxExclusiveness = false;
	protected double commentMin = -Double.MAX_VALUE;
	protected double commentMax = Double.MAX_VALUE;
	
	protected AbstractRangeEntry(
	  ConfigEntryHolder parent, String name, R value
	) {
		super(parent, name, value);
	}
	
	public static abstract class Builder<
	  V extends Comparable<V>, R extends AbstractRange<V, R>,
	  E extends AbstractRangeEntry<V, R>,
	  Self extends RangeEntryBuilder<V, R, Self>,
	  SelfImpl extends Builder<V, R, E, Self, SelfImpl>
	> extends AbstractConfigEntryBuilder<
	  R, String, R, E, Self, SelfImpl
	> implements RangeEntryBuilder<V, R, Self> {
		protected @Nullable V min;
		protected @Nullable V max;
		protected boolean canEditMinExclusiveness = false;
		protected boolean canEditMaxExclusiveness = false;
		
		public Builder(R value, Class<R> typeClass) {
			super(value, typeClass);
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
		
		@Override @Contract(pure=true) public @NotNull Self withBounds(V min, V max) {
			return min(min).max(max);
		}
		
		@Override @Contract(pure=true) public @NotNull Self canEditMinExclusive() {
			return canEditMinExclusive(true);
		}
		
		@Override @Contract(pure=true) public @NotNull Self canEditMinExclusive(boolean exclusive) {
			SelfImpl copy = copy();
			copy.canEditMinExclusiveness = exclusive;
			return copy.castSelf();
		}
		
		@Override @Contract(pure=true) public @NotNull Self canEditMaxExclusive() {
			return canEditMaxExclusive(true);
		}
		
		@Override @Contract(pure=true) public @NotNull Self canEditMaxExclusive(boolean exclusive) {
			SelfImpl copy = copy();
			copy.canEditMaxExclusiveness = exclusive;
			return copy.castSelf();
		}
		
		@Override @Contract(pure=true) public @NotNull Self canEditExclusiveness(boolean min, boolean max) {
			return canEditMinExclusive(min).canEditMaxExclusive(max);
		}
		
		@Override @Contract(pure=true) public @NotNull Self canEditExclusiveness() {
			return canEditExclusiveness(true);
		}
		
		@Override @Contract(pure=true) public @NotNull Self canEditExclusiveness(boolean canEdit) {
			return canEditExclusiveness(canEdit, canEdit);
		}
		
		@Override public SelfImpl copy() {
			SelfImpl copy = super.copy();
			copy.min = min;
			copy.max = max;
			copy.canEditMinExclusiveness = canEditMinExclusiveness;
			copy.canEditMaxExclusiveness = canEditMaxExclusiveness;
			return copy;
		}
		
		@Override protected E build(@NotNull ConfigEntryHolder parent, String name) {
			E built = super.build(parent, name);
			built.min = min;
			built.max = max;
			built.canEditMinExclusiveness = canEditMinExclusiveness;
			built.canEditMaxExclusiveness = canEditMaxExclusiveness;
			return built;
		}
	}
	
	@Override public Optional<Component> getErrorFromGUI(R value) {
		if (value.getMin().compareTo(value.getMax()) > 0) return Optional.of(
		  Component.translatable("simpleconfig.config.error.min_greater_than_max"));
		return super.getErrorFromGUI(value);
	}
	
	protected String serializeElement(V element) {
		return String.valueOf(element);
	}
	
	protected abstract @Nullable V deserializeElement(String value);
	
	@Override public String forConfig(R value) {
		return (value.isExclusiveMin()? "(" : "[")
		       + serializeElement(value.getMin()) + ", " + serializeElement(value.getMax())
		       + (value.isExclusiveMax()? ")" : "]");
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
	
	protected String getTypeComment() {
		return "Range: " + (canEditMinExclusiveness? "[(" : defValue.isExclusiveMin()? "(" : "[")
		       + "min, max" + (canEditMaxExclusiveness? ")]" : defValue.isExclusiveMax()? ")" : "]");
	}
	
	@Override public List<String> getConfigCommentTooltips() {
		List<String> tooltips = super.getConfigCommentTooltips();
		tooltips.add(getTypeComment());
		tooltips.add("Bounds: " + getRangeComment());
		return tooltips;
	}
	
	protected static final Pattern RANGE_PATTERN = Pattern.compile(
	  "^\\s*+(?<lp>[(\\[])\\s*+(?<l>.*?)\\s*+,\\s*+(?<r>.*?)\\s*+(?<rp>[])])\\s*+$");
	@Override public @Nullable R fromConfig(@Nullable String value) {
		if (value == null) return null;
		Matcher m = RANGE_PATTERN.matcher(value);
		if (!m.matches()) return null;
		boolean minEx = m.group("lp").contains("(");
		boolean maxEx = m.group("rp").contains(")");
		V min = deserializeElement(m.group("l"));
		V max = deserializeElement(m.group("r"));
		return this.defValue.create(min != null? min : this.min, max != null? max : this.max, minEx, maxEx);
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override public Optional<FieldBuilder<R, ?, ?>> buildGUIEntry(ConfigFieldBuilder builder) {
		R guiValue = forGui(get());
		RangeListEntryBuilder<V, R, ? extends AbstractConfigListEntry<V>> entryBuilder = builder.startRange(
		  getDisplayName(), guiValue,
		  buildLimitGUIEntry(builder, "min", guiValue.getMin()),
		  buildLimitGUIEntry(builder, "max", guiValue.getMax()))
		  .withMinExclusivenessEditable(canEditMinExclusiveness)
		  .withMaxExclusivenessEditable(canEditMaxExclusiveness);
		return Optional.of(decorate(entryBuilder));
	}
	
	@Override public boolean addCommandSuggestions(SuggestionsBuilder builder) {
		super.addCommandSuggestions(builder);
		R value = get();
		R maxSize = value.create(min, max, value.isExclusiveMin(), value.isExclusiveMax());
		if (isValidValue(maxSize)) builder.suggest(
		  forCommand(maxSize), Component.translatable("simpleconfig.command.suggest.largest"));
		return true;
	}
	
	@OnlyIn(Dist.CLIENT) protected abstract <EE extends AbstractConfigListEntry<V> & IChildListEntry>
	EE buildLimitGUIEntry(ConfigFieldBuilder builder, String name, V value);
	
	public static abstract class AbstractSizedRangeEntry<
	  V extends Comparable<V>, R extends AbstractSizedRange<V, R>,
	  E extends AbstractSizedRangeEntry<V, R, E>
	> extends AbstractRangeEntry<V, R> {
		protected double minSize = 0D;
		protected double maxSize = Double.POSITIVE_INFINITY;
		
		protected AbstractSizedRangeEntry(ConfigEntryHolder parent, String name, R value) {
			super(parent, name, value);
		}
		
		public static abstract class Builder<
		  V extends Comparable<V>, R extends AbstractSizedRange<V, R>,
		  E extends AbstractSizedRangeEntry<V, R, E>,
		  Self extends SizedRangeEntryBuilder<V, R, Self>,
		  SelfImpl extends Builder<V, R, E, Self, SelfImpl>
		> extends AbstractRangeEntry.Builder<V, R, E, Self, SelfImpl>
		  implements SizedRangeEntryBuilder<V, R, Self> {
			protected double minSize = 0D;
			protected double maxSize = Double.POSITIVE_INFINITY;
			
			public Builder(R value, Class<R> typeClass) {
				super(value, typeClass);
			}
			
			@Override @Contract(pure=true) public @NotNull Self allowEmpty(boolean empty) {
				return minSize(empty? Double.NEGATIVE_INFINITY : 0D);
			}
			
			@Override @Contract(pure=true) public @NotNull Self minSize(double size) {
				SelfImpl copy = copy();
				copy.minSize = size;
				return copy.castSelf();
			}
			
			@Override @Contract(pure=true) public @NotNull Self maxSize(double size) {
				SelfImpl copy = copy();
				copy.maxSize = size;
				return copy.castSelf();
			}
			
			@Override protected E build(@NotNull ConfigEntryHolder parent, String name) {
				E entry = super.build(parent, name);
				entry.minSize = minSize;
				entry.maxSize = maxSize;
				return entry;
			}
			
			@Override public SelfImpl copy() {
				SelfImpl copy = super.copy();
				copy.minSize = minSize;
				copy.maxSize = maxSize;
				return copy;
			}
		}
		
		@Override public Optional<Component> getErrorFromGUI(R value) {
			Optional<Component> opt = super.getErrorFromGUI(value);
			if (opt.isPresent()) return opt;
			double size = value.getSize();
			if (size < minSize) return Optional.of(Component.translatable("simpleconfig.config.error.range_too_small", minSize, size));
			if (size > maxSize) return Optional.of(Component.translatable("simpleconfig.config.error.range_too_large", maxSize, size));
			return Optional.empty();
		}
	}
}
