package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.core.AbstractRange.IntRange;
import endorh.simpleconfig.core.ISimpleConfigEntryHolder;
import endorh.simpleconfig.core.entry.AbstractRangeEntry.AbstractSizedRangeEntry;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.api.IChildListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Contract;

import java.util.Optional;

public class IntegerRangeEntry
  extends AbstractSizedRangeEntry<Integer, IntRange, IntegerRangeEntry> {
	protected IntegerRangeEntry(
	  ISimpleConfigEntryHolder parent, String name, IntRange value
	) {
		super(parent, name, value);
	}
	
	public static class Builder
	  extends AbstractSizedRangeEntry.Builder<Integer, IntRange, IntegerRangeEntry, Builder> {
		public Builder(IntRange value) {
			super(value, IntRange.class);
		}
		
		@Contract(pure=true) public Builder min(int min) {
			return min((Integer) min);
		}
		
		@Contract(pure=true) public Builder max(int max) {
			return max((Integer) max);
		}
		
		@Contract(pure=true) public Builder withBounds(int min, int max) {
			return withBounds((Integer) min, (Integer) max);
		}
		
		@Override
		protected IntegerRangeEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new IntegerRangeEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy() {
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
	  ConfigEntryBuilder builder, String name, Integer value
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
			  "text.cloth-config.error.not_valid_number_int"));
		return super.getErrorFromGUI(value);
	}
}
