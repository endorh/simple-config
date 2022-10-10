package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.FloatListEntryBuilder;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FloatListBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class FloatListEntry extends RangedListEntry<Float, Number, Float, FloatListEntry> {
	@Internal public FloatListEntry(
	  ConfigEntryHolder parent, String name,
	  @Nullable List<Float> value
	) {
		super(parent, name, value);
	}
	
	public static class Builder extends RangedListEntry.Builder<Float, Number, Float,
	  FloatListEntry, FloatListEntryBuilder, Builder>
	  implements FloatListEntryBuilder {
		public Builder(List<Float> value) {
			super(value, Float.class);
		}
		
		@Override @Contract(pure=true) public @NotNull FloatListEntryBuilder min(float min) {
			return super.min(min);
		}
		
		@Override @Contract(pure=true) public @NotNull FloatListEntryBuilder max(float max) {
			return super.max(max);
		}
		
		@Override @Contract(pure=true) public @NotNull FloatListEntryBuilder range(float min, float max) {
			return super.range(min, max);
		}
		
		@Override
		protected void checkBounds() {
			min = min != null ? min : Float.NEGATIVE_INFINITY;
			max = max != null ? max : Float.POSITIVE_INFINITY;
		}
		
		@Override
		protected FloatListEntry buildEntry(ConfigEntryHolder parent, String name) {
			return new FloatListEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy() {
			return new Builder(value);
		}
	}
	
	@Override
	protected Float elemFromConfig(Number value) {
		return value != null? value.floatValue() : null;
	}
	
	@OnlyIn(Dist.CLIENT) @Override
	public Optional<FieldBuilder<List<Float>, ?, ?>> buildGUIEntry(ConfigFieldBuilder builder) {
		final FloatListBuilder valBuilder = builder
		  .startFloatList(getDisplayName(), get());
		return Optional.of(decorate(valBuilder));
	}
}
