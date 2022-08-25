package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.api.AbstractRange.LongRange;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.LongRangeEntryBuilder;
import endorh.simpleconfig.core.entry.AbstractRangeEntry.AbstractSizedRangeEntry;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.api.IChildListEntry;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Contract;

import java.util.Optional;

public class LongRangeEntry
  extends AbstractSizedRangeEntry<Long, LongRange, LongRangeEntry> {
	protected LongRangeEntry(
	  ConfigEntryHolder parent, String name, LongRange value
	) {
		super(parent, name, value);
	}
	
	public static class Builder
	  extends AbstractSizedRangeEntry.Builder<Long, LongRange, LongRangeEntry, LongRangeEntryBuilder, Builder>
	  implements LongRangeEntryBuilder {
		public Builder(LongRange value) {
			super(value, LongRange.class);
		}
		
		@Override @Contract(pure=true) public LongRangeEntryBuilder min(long min) {
			return min((Long) min);
		}
		
		@Override @Contract(pure=true) public LongRangeEntryBuilder max(long max) {
			return max((Long) max);
		}
		
		@Override @Contract(pure=true) public LongRangeEntryBuilder withBounds(long min, long max) {
			return withBounds((Long) min, (Long) max);
		}
		
		@Override
		protected LongRangeEntry buildEntry(ConfigEntryHolder parent, String name) {
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
	
	@OnlyIn(Dist.CLIENT) @Override
	protected <EE extends AbstractConfigListEntry<Long> & IChildListEntry> EE buildLimitGUIEntry(
	  ConfigFieldBuilder builder, String name, Long value
	) {
		//noinspection unchecked
		return (EE) builder.startLongField(Component.literal(name), value)
		  .setDefaultValue(value)
		  .setMin(min).setMax(max)
		  .setName(name)
		  .build();
	}
	
	@Override public Optional<Component> getErrorFromGUI(LongRange value) {
		if (value.getMin() == null || value.getMax() == null)
			return Optional.of(Component.translatable("simpleconfig.config.error.invalid_integer"));
		return super.getErrorFromGUI(value);
	}
}
