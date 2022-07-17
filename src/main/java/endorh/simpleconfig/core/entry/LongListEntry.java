package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.core.ISimpleConfigEntryHolder;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.impl.builders.LongListBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
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
	
	public static class Builder extends RangedListEntry.Builder<Long, Number, Long, LongListEntry, Builder> {
		public Builder(List<Long> value) {
			super(value, Long.class);
		}
		
		/**
		 * Set the minimum allowed value for the elements of this list entry (inclusive)
		 */
		public Builder min(long min) {
			return super.min(min);
		}
		
		/**
		 * Set the maximum allowed value for the elements of this list entry (inclusive)
		 */
		public Builder max(long max) {
			return super.max(max);
		}
		
		/**
		 * Set the minimum and the maximum allowed for the elements of this list entry (inclusive)
		 */
		public Builder range(long min, long max) {
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
	public Optional<AbstractConfigListEntry<List<Long>>> buildGUIEntry(ConfigEntryBuilder builder) {
		final LongListBuilder valBuilder = builder
		  .startLongList(getDisplayName(), get());
		return Optional.of(decorate(valBuilder).build());
	}
}
