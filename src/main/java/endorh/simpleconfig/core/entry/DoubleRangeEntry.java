package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.core.AbstractRange.DoubleRange;
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

public class DoubleRangeEntry
  extends AbstractSizedRangeEntry<Double, DoubleRange, DoubleRangeEntry> {
	protected DoubleRangeEntry(
	  ISimpleConfigEntryHolder parent, String name, DoubleRange value
	) {
		super(parent, name, value);
	}
	
	public static class Builder
	  extends AbstractSizedRangeEntry.Builder<Double, DoubleRange, DoubleRangeEntry, Builder> {
		public Builder(DoubleRange value) {
			super(value, DoubleRange.class);
		}
		
		@Contract(pure=true) public Builder min(double min) {
			return min((Double) min);
		}
		
		@Contract(pure=true) public Builder max(double max) {
			return max((Double) max);
		}
		
		@Contract(pure=true) public Builder withBounds(double min, double max) {
			return withBounds((Double) min, (Double) max);
		}
		
		/**
		 * Scale the backing field of this entry by the given scale.<br>
		 * The scale is applied in both directions, when committing the field's value,
		 * the inverse of the scale is applied before saving the changes to the config.<br>
		 * To instead add a secondary backing field, use {@link #fieldScale(String, double)},
		 * {@link #addFieldScale(String, double)} or {@link #add_field_scale(String, double)}.
		 *
		 * @param scale The scale by which the config value is <em>multiplied</em>
		 *   before being stored in the backing field
		 * @see #fieldScale(String, double)
		 * @see #addFieldScale(String, double)
		 * @see #add_field_scale(String, double)
		 */
		@Contract(pure=true) public Builder fieldScale(double scale) {
			return field(scale(scale), scale(1F / scale), DoubleRange.class);
		}
		
		/**
		 * Add a secondary backing field with the given name, whose value is pre-multiplied
		 * by the given scale on bake.
		 *
		 * @param name The name of the secondary backing field
		 * @param scale The scale by which the config value is <em>multiplied</em>
		 *   before being stored in the backing field
		 * @see #fieldScale(double)
		 * @see #addFieldScale(String, double)
		 * @see #add_field_scale(String, double)
		 */
		@Contract(pure=true) public Builder fieldScale(String name, double scale) {
			return addField(BackingFieldBinding.withName(
			  name, BackingFieldBuilder.of(scale(scale), DoubleRange.class)));
		}
		
		/**
		 * Add a secondary backing field with the given camelCase suffix, whose value
		 * is pre-multiplied by the given scale on bake.
		 *
		 * @param suffix The camelCase suffix used to generate the backing field's name
		 * @param scale The scale by which the config value is <em>multiplied</em>
		 *   before being stored in the backing field
		 * @see #fieldScale(double)
		 * @see #fieldScale(String, double)
		 * @see #add_field_scale(String, double)
		 */
		@Contract(pure=true) public Builder addFieldScale(String suffix, double scale) {
			return addField(suffix, scale(scale), DoubleRange.class);
		}
		
		/**
		 * Add a secondary backing field with the given snake_case suffix, whose value
		 * is pre-multiplied by the given scale on bake.
		 *
		 * @param suffix The snake_case suffix used to generate the backing field's name
		 * @param scale The scale by which the config value is <em>multiplied</em>
		 *   before being stored in the backing field
		 * @see #fieldScale(double)
		 * @see #fieldScale(String, double)
		 * @see #addFieldScale(String, double)
		 */
		@Contract(pure=true) public Builder add_field_scale(String suffix, double scale) {
			return add_field(suffix, scale(scale), DoubleRange.class);
		}
		
		private static Function<DoubleRange, DoubleRange> scale(double scale) {
			if (scale == 0D || !Double.isFinite(scale)) throw new IllegalArgumentException(
			  "Scale must be a non-zero finite number");
			return d -> DoubleRange.of(
			  d.getMin() * scale, d.isExclusiveMin(),
			  d.getMax() * scale, d.isExclusiveMax());
		}
		
		@Override
		protected DoubleRangeEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new DoubleRangeEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy() {
			return new Builder(value);
		}
	}
	
	@Override protected Double deserializeElement(String value) {
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException ignored) {
			return null;
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override protected <EE extends AbstractConfigListEntry<Double> & IChildListEntry> EE buildLimitGUIEntry(
	  ConfigEntryBuilder builder, String name, Double value
	) {
		//noinspection unchecked
		return (EE) builder.startDoubleField(new StringTextComponent(name), value)
		  .setDefaultValue(value)
		  .setMin(min).setMax(max)
		  .setName(name)
		  .build();
	}
	
	@Override public Optional<ITextComponent> getErrorFromGUI(DoubleRange value) {
		if (value.getMin() == null || value.getMax() == null)
			return Optional.of(new TranslationTextComponent(
			  "text.cloth-config.error.not_valid_number_double"));
		return super.getErrorFromGUI(value);
	}
}
