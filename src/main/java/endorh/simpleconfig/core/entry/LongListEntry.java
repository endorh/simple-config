package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.api.ISimpleConfigEntryHolder;
import endorh.simpleconfig.api.entry.LongListEntryBuilder;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import endorh.simpleconfig.ui.impl.builders.LongListBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class LongListEntry extends RangedListEntry<Long, Number, Long, LongListEntry> {
	@Internal public LongListEntry(
	  ISimpleConfigEntryHolder parent, String name,
	  @Nullable List<Long> value
	) {
		super(parent, name, value);
	}
	
	public static class Builder extends RangedListEntry.Builder<Long, Number, Long, LongListEntry,
	  LongListEntryBuilder, Builder>
	  implements LongListEntryBuilder {
		public Builder(List<Long> value) {
			super(value, Long.class);
		}
		
		@Override @Contract(pure=true) public LongListEntryBuilder min(long min) {
			return super.min(min);
		}
		
		@Override @Contract(pure=true) public LongListEntryBuilder max(long max) {
			return super.max(max);
		}
		
		@Override @Contract(pure=true) public LongListEntryBuilder range(long min, long max) {
			return super.range(min, max);
		}
		
		@Override
		protected void checkBounds() {
			min = min != null ? min : Long.MIN_VALUE;
			max = max != null ? max : Long.MAX_VALUE;
		}
		
		@Override
		protected LongListEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new LongListEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy() {
			return new Builder(value);
		}
	}
	
	@Override
	protected Long elemFromConfig(Number value) {
		return value != null? value.longValue() : null;
	}
	
	@OnlyIn(Dist.CLIENT) @Override
	public Optional<FieldBuilder<List<Long>, ?, ?>> buildGUIEntry(ConfigFieldBuilder builder) {
		final LongListBuilder valBuilder = builder
		  .startLongList(getDisplayName(), get());
		return Optional.of(decorate(valBuilder));
	}
}
