package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.IntegerListEntryBuilder;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import endorh.simpleconfig.ui.impl.builders.IntListBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class IntegerListEntry extends RangedListEntry<Integer, Number, Integer, IntegerListEntry> {
	@Internal
	public IntegerListEntry(
	  ConfigEntryHolder parent, String name,
	  @Nullable List<Integer> value
	) {
		super(parent, name, value);
	}
	
	public static class Builder extends RangedListEntry.Builder<Integer, Number, Integer, IntegerListEntry, IntegerListEntryBuilder, Builder>
	  implements IntegerListEntryBuilder {
		
		public Builder(List<Integer> value) {
			super(value, Integer.class);
		}
		
		@Override @Contract(pure=true) public IntegerListEntryBuilder min(int min) {
			return super.min(min);
		}
		
		@Override @Contract(pure=true) public IntegerListEntryBuilder max(int max) {
			return super.max(max);
		}
		
		@Override @Contract(pure=true) public IntegerListEntryBuilder range(int min, int max) {
			return super.range(min, max);
		}
		
		@Override
		protected void checkBounds() {
			min = min != null ? min : Integer.MIN_VALUE;
			max = max != null ? max : Integer.MAX_VALUE;
		}
		
		@Override
		protected IntegerListEntry buildEntry(ConfigEntryHolder parent, String name) {
			return new IntegerListEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy() {
			return new Builder(value);
		}
	}
	
	@Override
	protected Integer elemFromConfig(Number value) {
		return value != null? value.intValue() : null;
	}
	
	@OnlyIn(Dist.CLIENT) @Override
	public Optional<FieldBuilder<List<Integer>, ?, ?>> buildGUIEntry(ConfigFieldBuilder builder) {
		final IntListBuilder valBuilder = builder
		  .startIntList(getDisplayName(), get());
		return Optional.of(decorate(valBuilder));
	}
}
