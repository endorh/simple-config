package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.ByteListEntryBuilder;
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
public class ByteListEntry extends RangedListEntry<Byte, Number, Integer, ByteListEntry> {
	
	@Internal public ByteListEntry(
	  ConfigEntryHolder parent, String name,
	  @Nullable List<Byte> value
	) {
		super(parent, name, value);
	}
	
	public static class Builder extends RangedListEntry.Builder<Byte, Number, Integer, ByteListEntry, ByteListEntryBuilder, Builder>
	  implements ByteListEntryBuilder {
		public Builder(List<Byte> value) {
			super(value, Byte.class);
		}
		
		@Override @Contract(pure=true) public @NotNull ByteListEntryBuilder min(byte min) {
			return super.min(min);
		}
		
		@Override @Contract(pure=true) public @NotNull ByteListEntryBuilder max(byte max) {
			return super.max(max);
		}
		
		@Override @Contract(pure=true) public @NotNull ByteListEntryBuilder range(byte min, byte max) {
			return super.range(min, max);
		}
		
		@Override
		protected void checkBounds() {
			min = min != null ? min : Byte.MIN_VALUE;
			max = max != null ? max : Byte.MAX_VALUE;
		}
		
		@Override
		protected ByteListEntry buildEntry(ConfigEntryHolder parent, String name) {
			return new ByteListEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy() {
			return new Builder(value);
		}
	}
	
	@Override
	protected Byte elemFromConfig(Number value) {
		return value != null? value.byteValue() : null;
	}
	
	@OnlyIn(Dist.CLIENT) @Override
	public Optional<FieldBuilder<List<Integer>, ?, ?>> buildGUIEntry(ConfigFieldBuilder builder) {
		final IntListBuilder valBuilder = builder
		  .startIntList(getDisplayName(), forGui(get()));
		return Optional.of(decorate(valBuilder));
	}
}
