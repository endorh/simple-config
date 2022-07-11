package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.impl.builders.IntListBuilder;
import endorh.simpleconfig.core.ISimpleConfigEntryHolder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Deprecated
public class ShortListEntry extends RangedListEntry<Short, Number, Integer, ShortListEntry> {
	@Internal
	public ShortListEntry(
	  ISimpleConfigEntryHolder parent, String name,
	  @Nullable List<Short> value
	) {
		super(parent, name, value);
	}
	
	public static class Builder extends RangedListEntry.Builder<Short, Number, Integer, ShortListEntry, Builder> {
		
		public Builder(List<Short> value) {
			super(value, Short.class);
		}
		
		/**
		 * Set the minimum allowed value for the elements of this list entry (inclusive)
		 */
		public Builder min(short min) {
			return super.min(min);
		}
		
		/**
		 * Set the maximum allowed value for the elements of this list entry (inclusive)
		 */
		public Builder max(short max) {
			return super.max(max);
		}
		
		/**
		 * Set the minimum and the maximum allowed for the elements of this list entry (inclusive)
		 */
		public Builder range(short min, short max) {
			return super.range(min, max);
		}
		
		@Override
		protected void checkBounds() {
			min = min != null ? min : Short.MIN_VALUE;
			max = max != null ? max : Short.MAX_VALUE;
		}
		
		@Override
		protected ShortListEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
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
	
	@Override
	protected List<Short> get(ConfigValue<?> spec) {
		//noinspection unchecked
		return ((List<Number>) (List<?>) super.get(spec))
		  .stream().map(this::elemFromConfig).collect(Collectors.toList());
	}
	
	@OnlyIn(Dist.CLIENT) @Override
	public Optional<AbstractConfigListEntry<List<Integer>>> buildGUIEntry(ConfigEntryBuilder builder) {
		final IntListBuilder valBuilder = builder
		  .startIntList(getDisplayName(), forGui(get()));
		return Optional.of(decorate(valBuilder).build());
	}
}
