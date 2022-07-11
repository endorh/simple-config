package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.impl.builders.RangeListEntryBuilder;
import endorh.simpleconfig.core.*;
import endorh.simpleconfig.core.AbstractRange.AbstractSizedRange;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractRangeEntry<
  V extends Comparable<V>, R extends AbstractRange<V, R>,
  E extends AbstractRangeEntry<V, R, E>
> extends AbstractConfigEntry<R, String, R, E> implements IKeyEntry<String, R> {
	protected @Nullable V min = null;
	protected @Nullable V max = null;
	protected boolean canEditMinExclusiveness = false;
	protected boolean canEditMaxExclusiveness = false;
	
	protected AbstractRangeEntry(
	  ISimpleConfigEntryHolder parent, String name, R value
	) {
		super(parent, name, value);
	}
	
	public static abstract class Builder<
	  V extends Comparable<V>, R extends AbstractRange<V, R>,
	  E extends AbstractRangeEntry<V, R, E>,
	  Self extends Builder<V, R, E, Self>
	> extends AbstractConfigEntryBuilder<
	  R, String, R, E, Self
	> {
		protected @Nullable V min;
		protected @Nullable V max;
		protected boolean canEditMinExclusiveness = false;
		protected boolean canEditMaxExclusiveness = false;
		
		public Builder(R value, Class<R> typeClass) {
			super(value, typeClass);
		}
		
		public Self min(V min) {
			Self copy = copy();
			copy.min = min;
			return copy;
		}
		
		public Self max(V max) {
			Self copy = copy();
			copy.max = max;
			return copy;
		}
		
		public Self withBounds(V min, V max) {
			return min(min).max(max);
		}
		
		public Self canEditMinExclusive(boolean exclusive) {
			Self copy = copy();
			copy.canEditMinExclusiveness = exclusive;
			return copy;
		}
		
		public Self canEditMaxExclusive(boolean exclusive) {
			Self copy = copy();
			copy.canEditMaxExclusiveness = exclusive;
			return copy;
		}
		
		public Self canEditExclusiveness(boolean min, boolean max) {
			return canEditMinExclusive(min).canEditMaxExclusive(max);
		}
		
		public Self canEditExclusiveness(boolean canEdit) {
			return canEditExclusiveness(canEdit, canEdit);
		}
		
		@Override protected Self copy() {
			Self copy = super.copy();
			copy.min = min;
			copy.max = max;
			copy.canEditMinExclusiveness = canEditMinExclusiveness;
			copy.canEditMaxExclusiveness = canEditMaxExclusiveness;
			return copy;
		}
		
		@Override protected E build(ISimpleConfigEntryHolder parent, String name) {
			E built = super.build(parent, name);
			built.min = min;
			built.max = max;
			built.canEditMinExclusiveness = canEditMinExclusiveness;
			built.canEditMaxExclusiveness = canEditMaxExclusiveness;
			return built;
		}
	}
	
	@Override public Optional<ITextComponent> getError(R value) {
		if (value.getMin().compareTo(value.getMax()) > 0) return Optional.of(
		  new TranslationTextComponent("simpleconfig.config.error.min_greater_than_max"));
		return super.getError(value);
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
	
	@Override protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.of(decorate(builder).define(
		  name, forActualConfig(forConfig(value)), createConfigValidator()));
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
		return this.value.create(min != null? min : this.min, max != null? max : this.max, minEx, maxEx);
	}
	
	@Override public Optional<String> deserializeStringKey(@NotNull String key) {
		return Optional.of(key);
	}
	
	@Override public Optional<AbstractConfigListEntry<R>> buildGUIEntry(ConfigEntryBuilder builder) {
		R guiValue = forGui(get());
		RangeListEntryBuilder<V, R, ? extends AbstractConfigListEntry<V>> entryBuilder = builder.startRange(
		  getDisplayName(), guiValue,
		  buildLimitGUIEntry(builder, "min", guiValue.getMin()),
		  buildLimitGUIEntry(builder, "max", guiValue.getMax()))
		  .withMinExclusivenessEditable(canEditMinExclusiveness)
		  .withMaxExclusivenessEditable(canEditMaxExclusiveness);
		return Optional.of(decorate(entryBuilder).build());
	}
	
	protected abstract <EE extends AbstractConfigListEntry<V> & IChildListEntry>
	EE buildLimitGUIEntry(ConfigEntryBuilder builder, String name, V value);
	
	public static abstract class AbstractSizedRangeEntry<
	  V extends Comparable<V>, R extends AbstractSizedRange<V, R>,
	  E extends AbstractSizedRangeEntry<V, R, E>
	> extends AbstractRangeEntry<V, R, E> {
		protected double minSize = 0D;
		protected double maxSize = Double.POSITIVE_INFINITY;
		
		protected AbstractSizedRangeEntry(ISimpleConfigEntryHolder parent, String name, R value) {
			super(parent, name, value);
		}
		
		public static abstract class Builder<
		  V extends Comparable<V>, R extends AbstractSizedRange<V, R>,
		  E extends AbstractSizedRangeEntry<V, R, E>, Self extends Builder<V, R, E, Self>
		> extends AbstractRangeEntry.Builder<V, R, E, Self> {
			protected double minSize = 0D;
			protected double maxSize = Double.POSITIVE_INFINITY;
			
			public Builder(R value, Class<R> typeClass) {
				super(value, typeClass);
			}
			
			/**
			 * Allow empty ranges.<br>
			 * By default, empty ranges are not allowed.<br>
			 * Equivalent to {@code minSize(empty? Double.NEGATIVE_INFINITY : 0)}.
			 */
			public Self allowEmpty(boolean empty) {
				return minSize(empty? Double.NEGATIVE_INFINITY : 0D);
			}
			
			/**
			 * Allow only range values with at least the given size.<br>
			 * Empty ranges have negative sizes, so a minSize of 0 will
			 * only prevent empty ranges.<br>
			 * A range with only one value is not considered empty.
			 */
			public Self minSize(double size) {
				Self copy = copy();
				copy.minSize = size;
				return copy;
			}
			
			/**
			 * Allow only range values with at most the given size.
			 */
			public Self maxSize(double size) {
				Self copy = copy();
				copy.maxSize = size;
				return copy;
			}
			
			@Override protected E build(ISimpleConfigEntryHolder parent, String name) {
				E entry = super.build(parent, name);
				entry.minSize = minSize;
				entry.maxSize = maxSize;
				return entry;
			}
			
			@Override protected Self copy() {
				Self copy = super.copy();
				copy.minSize = minSize;
				copy.maxSize = maxSize;
				return copy;
			}
		}
		
		@Override public Optional<ITextComponent> getError(R value) {
			Optional<ITextComponent> opt = super.getError(value);
			if (opt.isPresent()) return opt;
			double size = value.getSize();
			if (size < minSize) return Optional.of(new TranslationTextComponent(
			  "simpleconfig.config.error.range_too_small", minSize, size));
			if (size > maxSize) return Optional.of(new TranslationTextComponent(
			  "simpleconfig.config.error.range_too_large", maxSize, size));
			return Optional.empty();
		}
	}
}
