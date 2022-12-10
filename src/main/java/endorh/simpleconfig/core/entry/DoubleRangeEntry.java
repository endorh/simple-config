package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.DoubleRangeEntryBuilder;
import endorh.simpleconfig.api.range.DoubleRange;
import endorh.simpleconfig.core.BackingField.BackingFieldBinding;
import endorh.simpleconfig.core.BackingField.BackingFieldBuilder;
import endorh.simpleconfig.core.EntryType;
import endorh.simpleconfig.core.entry.AbstractRangeEntry.AbstractSizedRangeEntry;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.api.IChildListEntry;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Function;

public class DoubleRangeEntry
  extends AbstractSizedRangeEntry<Double, DoubleRange, DoubleRangeEntry> {
	protected DoubleRangeEntry(
	  ConfigEntryHolder parent, String name, DoubleRange value
	) {
		super(parent, name, value);
	}
	
	public static class Builder
	  extends AbstractSizedRangeEntry.Builder<Double, DoubleRange, DoubleRangeEntry, DoubleRangeEntryBuilder, Builder>
	  implements DoubleRangeEntryBuilder {
		public Builder(DoubleRange value) {
			super(value, DoubleRange.class);
		}
		
		@Override @Contract(pure=true) public @NotNull DoubleRangeEntryBuilder min(double min) {
			return min((Double) min);
		}
		
		@Override @Contract(pure=true) public @NotNull DoubleRangeEntryBuilder max(double max) {
			return max((Double) max);
		}
		
		@Override @Contract(pure=true) public @NotNull DoubleRangeEntryBuilder withBounds(double min, double max) {
			return withBounds((Double) min, (Double) max);
		}
		
		@Override @Contract(pure=true) public @NotNull DoubleRangeEntryBuilder fieldScale(double scale) {
			return field(scale(scale), scale(1F / scale), DoubleRange.class);
		}
		
		@Override @Contract(pure=true) public @NotNull DoubleRangeEntryBuilder fieldScale(String name, double scale) {
			return addField(BackingFieldBinding.withName(
			  name, BackingFieldBuilder.of(scale(scale), EntryType.of(DoubleRange.class))));
		}
		
		@Override @Contract(pure=true) public @NotNull DoubleRangeEntryBuilder addFieldScale(String suffix, double scale) {
			return addField(suffix, scale(scale), DoubleRange.class);
		}
		
		@Override @Contract(pure=true) public @NotNull DoubleRangeEntryBuilder add_field_scale(String suffix, double scale) {
			return add_field(suffix, scale(scale), DoubleRange.class);
		}
		
		private static Function<DoubleRange, DoubleRange> scale(double scale) {
			if (scale == 0D || !Double.isFinite(scale)) throw new IllegalArgumentException(
			  "Scale must be a non-zero finite number");
			return d -> DoubleRange.of(
			  d.getMin() * scale, d.isExclusiveMin(),
			  d.getMax() * scale, d.isExclusiveMax());
		}
		
		@Override
		protected DoubleRangeEntry buildEntry(ConfigEntryHolder parent, String name) {
			return new DoubleRangeEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy(DoubleRange value) {
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
	
	@OnlyIn(Dist.CLIENT)
	@Override protected <EE extends AbstractConfigListEntry<Double> & IChildListEntry> EE buildLimitGUIEntry(
	  ConfigFieldBuilder builder, String name, Double value
	) {
		//noinspection unchecked
		return (EE) builder.startDoubleField(Component.literal(name), value)
		  .setDefaultValue(value)
		  .setMin(min).setMax(max)
		  .setName(name)
		  .build();
	}
	
	@Override public Optional<Component> getErrorFromGUI(DoubleRange value) {
		if (value.getMin() == null || value.getMax() == null)
			return Optional.of(Component.translatable("simpleconfig.config.error.invalid_float"));
		return super.getErrorFromGUI(value);
	}
}
