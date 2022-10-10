package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.api.AbstractRange.FloatRange;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.FloatRangeEntryBuilder;
import endorh.simpleconfig.core.BackingField.BackingFieldBinding;
import endorh.simpleconfig.core.BackingField.BackingFieldBuilder;
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
import java.util.function.Function;

public class FloatRangeEntry
  extends AbstractSizedRangeEntry<Float, FloatRange, FloatRangeEntry> {
	protected FloatRangeEntry(
	  ConfigEntryHolder parent, String name, FloatRange value
	) {
		super(parent, name, value);
	}
	
	public static class Builder
	  extends AbstractSizedRangeEntry.Builder<Float, FloatRange, FloatRangeEntry, FloatRangeEntryBuilder, Builder>
	  implements FloatRangeEntryBuilder {
		public Builder(FloatRange value) {
			super(value, FloatRange.class);
		}
		
		@Override @Contract(pure=true) public @NotNull FloatRangeEntryBuilder min(float min) {
			return min((Float) min);
		}
		
		@Override @Contract(pure=true) public @NotNull FloatRangeEntryBuilder max(float max) {
			return max((Float) max);
		}
		
		@Override @Contract(pure=true) public @NotNull FloatRangeEntryBuilder withBounds(float min, float max) {
			return withBounds((Float) min, (Float) max);
		}
		
		@Override @Contract(pure=true) public @NotNull FloatRangeEntryBuilder fieldScale(float scale) {
			return field(scale(scale), scale(1F / scale), FloatRange.class);
		}
		
		@Override @Contract(pure=true) public @NotNull FloatRangeEntryBuilder fieldScale(String name, float scale) {
			return addField(BackingFieldBinding.withName(
			  name, BackingFieldBuilder.of(scale(scale), FloatRange.class)));
		}
		
		@Override @Contract(pure=true) public @NotNull FloatRangeEntryBuilder addFieldScale(String suffix, float scale) {
			return addField(suffix, scale(scale), FloatRange.class);
		}
		
		@Override @Contract(pure=true) public @NotNull FloatRangeEntryBuilder add_field_scale(String suffix, float scale) {
			return add_field(suffix, scale(scale), FloatRange.class);
		}
		
		private static Function<FloatRange, FloatRange> scale(float scale) {
			if (scale == 0D || !Float.isFinite(scale)) throw new IllegalArgumentException(
			  "Scale must be a non-zero finite number");
			return d -> FloatRange.of(
			  d.getMin() * scale, d.isExclusiveMin(),
			  d.getMax() * scale, d.isExclusiveMax());
		}
		
		@Override
		protected FloatRangeEntry buildEntry(ConfigEntryHolder parent, String name) {
			return new FloatRangeEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy() {
			return new Builder(value);
		}
	}
	
	@Override protected Float deserializeElement(String value) {
		try {
			return Float.parseFloat(value);
		} catch (NumberFormatException ignored) {
			return null;
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	protected <EE extends AbstractConfigListEntry<Float> & IChildListEntry> EE buildLimitGUIEntry(
	  ConfigFieldBuilder builder, String name, Float value
	) {
		//noinspection unchecked
		return (EE) builder.startFloatField(new StringTextComponent(name), value)
		  .setDefaultValue(value)
		  .setMin(min).setMax(max)
		  .setName(name)
		  .build();
	}
	
	@Override public Optional<ITextComponent> getErrorFromGUI(FloatRange value) {
		if (value.getMin() == null || value.getMax() == null)
			return Optional.of(new TranslationTextComponent(
			  "simpleconfig.config.error.invalid_float"));
		return super.getErrorFromGUI(value);
	}
}
