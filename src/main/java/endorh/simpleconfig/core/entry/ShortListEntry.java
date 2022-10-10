package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.ShortListEntryBuilder;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import endorh.simpleconfig.ui.impl.builders.IntListBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

@Deprecated
public class ShortListEntry extends RangedListEntry<Short, Number, Integer, ShortListEntry> {
	@Internal
	public ShortListEntry(
	  ConfigEntryHolder parent, String name,
	  @Nullable List<Short> value
	) {
		super(parent, name, value);
	}
	
	public static class Builder extends RangedListEntry.Builder<Short, Number, Integer,
	  ShortListEntry, ShortListEntryBuilder, Builder>
	  implements ShortListEntryBuilder {
		
		public Builder(List<Short> value) {
			super(value, Short.class);
		}
		
		@Override @Contract(pure=true) public @NotNull ShortListEntryBuilder min(short min) {
			return super.min(min);
		}
		
		@Override @Contract(pure=true) public @NotNull ShortListEntryBuilder max(short max) {
			return super.max(max);
		}
		
		@Override @Contract(pure=true) public @NotNull ShortListEntryBuilder range(short min, short max) {
			return super.range(min, max);
		}
		
		@Override
		protected void checkBounds() {
			min = min != null ? min : Short.MIN_VALUE;
			max = max != null ? max : Short.MAX_VALUE;
		}
		
		@Override
		protected ShortListEntry buildEntry(ConfigEntryHolder parent, String name) {
			return new ShortListEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy() {
			return new Builder(value);
		}
	}
	
	@Override
	protected Short elemFromConfig(Number value) {
		return value != null? value.shortValue() : null;
	}
	
	@OnlyIn(Dist.CLIENT) @Override
	public Optional<FieldBuilder<List<Integer>, ?, ?>> buildGUIEntry(ConfigFieldBuilder builder) {
		final IntListBuilder valBuilder = builder
		  .startIntList(getDisplayName(), forGui(get()));
		return Optional.of(decorate(valBuilder));
	}
}
