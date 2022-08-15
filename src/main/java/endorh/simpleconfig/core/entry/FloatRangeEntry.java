package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.core.AbstractRange.FloatRange;
import endorh.simpleconfig.core.BackingField.BackingFieldBinding;
import endorh.simpleconfig.core.BackingField.BackingFieldBuilder;
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
import java.util.function.Function;

public class FloatRangeEntry
  extends AbstractSizedRangeEntry<Float, FloatRange, FloatRangeEntry> {
	protected FloatRangeEntry(
	  ISimpleConfigEntryHolder parent, String name, FloatRange value
	) {
		super(parent, name, value);
	}
	
	public static class Builder
	  extends AbstractSizedRangeEntry.Builder<Float, FloatRange, FloatRangeEntry, Builder> {
		public Builder(FloatRange value) {
			super(value, FloatRange.class);
		}
		
		@Contract(pure=true) public Builder min(float min) {
			return min((Float) min);
		}
		
		@Contract(pure=true) public Builder max(float max) {
			return max((Float) max);
		}
		
		@Contract(pure=true) public Builder withBounds(float min, float max) {
			return withBounds((Float) min, (Float) max);
		}
		
		/**
		 * Scale the backing field of this entry by the given scale.<br>
		 * The scale is applied in both directions, when committing the field's value,
		 * the inverse of the scale is applied before saving the changes to the config.<br>
		 * To instead add a secondary backing field, use {@link #fieldScale(String, float)},
		 * {@link #addFieldScale(String, float)} or {@link #add_field_scale(String, float)}.
		 *
		 * @param scale The scale by which the config value is <em>multiplied</em>
		 *   before being stored in the backing field
		 * @see #fieldScale(String, float)
		 * @see #addFieldScale(String, float)
		 * @see #add_field_scale(String, float)
		 */
		@Contract(pure=true) public Builder fieldScale(float scale) {
			return field(scale(scale), scale(1F / scale), FloatRange.class);
		}
		
		/**
		 * Add a secondary backing field with the given name, whose value is pre-multiplied
		 * by the given scale on bake.
		 *
		 * @param name The name of the secondary backing field
		 * @param scale The scale by which the config value is <em>multiplied</em>
		 *   before being stored in the backing field
		 * @see #fieldScale(float)
		 * @see #addFieldScale(String, float)
		 * @see #add_field_scale(String, float)
		 */
		@Contract(pure=true) public Builder fieldScale(String name, float scale) {
			return addField(BackingFieldBinding.withName(
			  name, BackingFieldBuilder.of(scale(scale), FloatRange.class)));
		}
		
		/**
		 * Add a secondary backing field with the given camelCase suffix, whose value
		 * is pre-multiplied by the given scale on bake.
		 *
		 * @param suffix The camelCase suffix used to generate the backing field's name
		 * @param scale The scale by which the config value is <em>multiplied</em>
		 *   before being stored in the backing field
		 * @see #fieldScale(float)
		 * @see #fieldScale(String, float)
		 * @see #add_field_scale(String, float)
		 */
		@Contract(pure=true) public Builder addFieldScale(String suffix, float scale) {
			return addField(suffix, scale(scale), FloatRange.class);
		}
		
		/**
		 * Add a secondary backing field with the given snake_case suffix, whose value
		 * is pre-multiplied by the given scale on bake.
		 *
		 * @param suffix The snake_case suffix used to generate the backing field's name
		 * @param scale The scale by which the config value is <em>multiplied</em>
		 *   before being stored in the backing field
		 * @see #fieldScale(float)
		 * @see #fieldScale(String, float)
		 * @see #addFieldScale(String, float)
		 */
		@Contract(pure=true) public Builder add_field_scale(String suffix, float scale) {
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
		protected FloatRangeEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
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
	  ConfigEntryBuilder builder, String name, Float value
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
			  "text.cloth-config.error.not_valid_number_float"));
		return super.getErrorFromGUI(value);
	}
}
