package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.core.AbstractRange.LongRange;
import endorh.simpleconfig.core.ISimpleConfigEntryHolder;
import endorh.simpleconfig.core.entry.AbstractRangeEntry.AbstractSizedRangeEntry;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.api.IChildListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Optional;

public class LongRangeEntry
  extends AbstractSizedRangeEntry<Long, LongRange, LongRangeEntry> {
	protected LongRangeEntry(
	  ISimpleConfigEntryHolder parent, String name, LongRange value
	) {
		super(parent, name, value);
	}
	
	public static class Builder
	  extends AbstractSizedRangeEntry.Builder<Long, LongRange, LongRangeEntry, Builder> {
		public Builder(LongRange value) {
			super(value, LongRange.class);
		}
		
		public Builder min(long min) {
			return min((Long) min);
		}
		
		public Builder max(long max) {
			return max((Long) max);
		}
		
		public Builder withBounds(long min, long max) {
			return withBounds((Long) min, (Long) max);
		}
		
		@Override
		protected LongRangeEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new LongRangeEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy() {
			return new Builder(value);
		}
	}
	
	@Override protected Long deserializeElement(String value) {
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException ignored) {
			return null;
		}
	}
	
	@Override
	protected <EE extends AbstractConfigListEntry<Long> & IChildListEntry> EE buildLimitGUIEntry(
	  ConfigEntryBuilder builder, String name, Long value
	) {
		//noinspection unchecked
		return (EE) builder.startLongField(new StringTextComponent(name), value)
		  .setDefaultValue(value)
		  .setMin(min).setMax(max)
		  .setName(name)
		  .build();
	}
	
	@Override public Optional<ITextComponent> getError(LongRange value) {
		if (value.getMin() == null || value.getMax() == null)
			return Optional.of(new TranslationTextComponent(
			  "text.cloth-config.error.not_valid_number_Long"));
		return super.getError(value);
	}
}
