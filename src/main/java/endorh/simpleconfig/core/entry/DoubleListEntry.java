package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.DoubleListEntryBuilder;
import endorh.simpleconfig.core.EntryType;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.DoubleListBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class DoubleListEntry extends RangedListEntry<Double, Number, Double, DoubleListEntry> {
	@Internal public DoubleListEntry(
	  ConfigEntryHolder parent, String name,
	  @Nullable List<Double> value
	) {
		super(parent, name, value);
	}
	
	public static class Builder extends RangedListEntry.Builder<Double, Number, Double,
	  DoubleListEntry, DoubleListEntryBuilder, Builder>
	  implements DoubleListEntryBuilder {
		public Builder(List<Double> value) {
			super(value, EntryType.of(Double.class));
		}
		
		@Override @Contract(pure=true) public @NotNull DoubleListEntryBuilder min(double min) {
			return super.min(min);
		}
		
		@Override @Contract(pure=true) public @NotNull DoubleListEntryBuilder max(double max) {
			return super.max(max);
		}
		
		@Override @Contract(pure=true) public @NotNull DoubleListEntryBuilder range(double min, double max) {
			return super.range(min, max);
		}
		
		@Override
		protected void checkBounds() {
			min = min != null ? min : Double.NEGATIVE_INFINITY;
			max = max != null ? max : Double.POSITIVE_INFINITY;
		}
		
		@Override
		protected DoubleListEntry buildEntry(ConfigEntryHolder parent, String name) {
			return new DoubleListEntry(parent, name, value);
		}
		
		@Contract(value="_ -> new", pure=true) @Override protected Builder createCopy(List<Double> value) {
			return new Builder(value);
		}
	}
	
	@Override
	protected Double elemFromConfig(Number value) {
		return value != null? value.doubleValue() : null;
	}
	
	@OnlyIn(Dist.CLIENT) @Override
	public Optional<FieldBuilder<List<Double>, ?, ?>> buildGUIEntry(ConfigFieldBuilder builder) {
		final DoubleListBuilder valBuilder = builder
		  .startDoubleList(getDisplayName(), get());
		return Optional.of(decorate(valBuilder));
	}
}
