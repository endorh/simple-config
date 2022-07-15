package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.core.AbstractRange.DoubleRange;
import endorh.simpleconfig.core.ISimpleConfigEntryHolder;
import endorh.simpleconfig.core.entry.AbstractRangeEntry.AbstractSizedRangeEntry;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.api.IChildListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Optional;

public class DoubleRangeEntry
  extends AbstractSizedRangeEntry<Double, DoubleRange, DoubleRangeEntry> {
	protected DoubleRangeEntry(
	  ISimpleConfigEntryHolder parent, String name, DoubleRange value
	) {
		super(parent, name, value);
	}
	
	public static class Builder
	  extends AbstractSizedRangeEntry.Builder<Double, DoubleRange, DoubleRangeEntry, Builder> {
		public Builder(DoubleRange value) {
			super(value, DoubleRange.class);
		}
		
		public Builder min(double min) {
			return min((Double) min);
		}
		
		public Builder max(double max) {
			return max((Double) max);
		}
		
		public Builder withBounds(double min, double max) {
			return withBounds((Double) min, (Double) max);
		}
		
		@Override
		protected DoubleRangeEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new DoubleRangeEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy() {
			return new Builder(value);
		}
	}
	
	@Override protected Double deserializeElement(String value) {
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException ignored) {
			return null;
		}
	}
	
	@Override
	protected <EE extends AbstractConfigListEntry<Double> & IChildListEntry> EE buildLimitGUIEntry(
	  ConfigEntryBuilder builder, String name, Double value
	) {
		//noinspection unchecked
		return (EE) builder.startDoubleField(new StringTextComponent(name), value)
		  .setDefaultValue(value)
		  .setMin(min).setMax(max)
		  .setName(name)
		  .build();
	}
	
	@Override public Optional<ITextComponent> getError(DoubleRange value) {
		if (value.getMin() == null || value.getMax() == null)
			return Optional.of(new TranslationTextComponent(
			  "text.cloth-config.error.not_valid_number_double"));
		return super.getError(value);
	}
}
