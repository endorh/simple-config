package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.api.AbstractRange.IntRange;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.IntegerRangeEntryBuilder;
import endorh.simpleconfig.core.entry.AbstractRangeEntry.AbstractSizedRangeEntry;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.api.IChildListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class IntegerRangeEntry
  extends AbstractSizedRangeEntry<Integer, IntRange, IntegerRangeEntry> {
	protected IntegerRangeEntry(
	  ConfigEntryHolder parent, String name, IntRange value
	) {
		super(parent, name, value);
	}
	
	public static class Builder
	  extends AbstractSizedRangeEntry.Builder<Integer, IntRange, IntegerRangeEntry, IntegerRangeEntryBuilder, Builder>
	  implements IntegerRangeEntryBuilder {
		public Builder(IntRange value) {
			super(value, IntRange.class);
		}
		
		@Override @Contract(pure=true) public @NotNull IntegerRangeEntryBuilder min(int min) {
			return min((Integer) min);
		}
		
		@Override @Contract(pure=true) public @NotNull IntegerRangeEntryBuilder max(int max) {
			return max((Integer) max);
		}
		
		@Override @Contract(pure=true) public @NotNull IntegerRangeEntryBuilder withBounds(int min, int max) {
			return withBounds((Integer) min, (Integer) max);
		}
		
		@Override
		protected IntegerRangeEntry buildEntry(ConfigEntryHolder parent, String name) {
			return new IntegerRangeEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy(IntRange value) {
			return new Builder(value);
		}
	}
	
	@Override protected Integer deserializeElement(String value) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException ignored) {
			return null;
		}
	}
	
	@OnlyIn(Dist.CLIENT) @Override
	protected <EE extends AbstractConfigListEntry<Integer> & IChildListEntry> EE buildLimitGUIEntry(
	  ConfigFieldBuilder builder, String name, Integer value
	) {
		//noinspection unchecked
		return (EE) builder.startIntField(new StringTextComponent(name), value)
		  .setDefaultValue(value)
		  .setMin(min).setMax(max)
		  .setName(name)
		  .build();
	}
	
	@Override public Optional<ITextComponent> getErrorFromGUI(IntRange value) {
		if (value.getMin() == null || value.getMax() == null)
			return Optional.of(new TranslationTextComponent(
			  "simpleconfig.config.error.invalid_integer"));
		return super.getErrorFromGUI(value);
	}
}
