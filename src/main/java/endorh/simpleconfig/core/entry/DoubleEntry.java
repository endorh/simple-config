package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.core.BackingField.BackingFieldBinding;
import endorh.simpleconfig.core.BackingField.BackingFieldBuilder;
import endorh.simpleconfig.core.IKeyEntry;
import endorh.simpleconfig.core.ISimpleConfigEntryHolder;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.impl.builders.DoubleFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.DoubleSliderBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class DoubleEntry extends AbstractRangedEntry<Double, Number, Double, DoubleEntry>
  implements IKeyEntry<Double> {
	@Internal public DoubleEntry(
	  ISimpleConfigEntryHolder parent, String name, double value
	) {
		super(parent, name, value);
		commentMin = -Double.MAX_VALUE;
		commentMax = Double.MAX_VALUE;
	}
	
	public static class Builder
	  extends AbstractRangedEntry.Builder<Double, Number, Double, DoubleEntry, Builder> {
		public Builder(Double value) {
			super(value, Double.class, "%.2f");
		}
		
		/**
		 * Set min (inclusive)
		 */
		@Contract(pure=true) public Builder min(double min) {
			return super.min(min);
		}
		
		/**
		 * Set max (inclusive)
		 */
		@Contract(pure=true) public Builder max(double max) {
			return super.max(max);
		}
		
		/**
		 * Set inclusive range
		 */
		@Contract(pure=true) public Builder range(double min, double max) {
			return super.range(min, max);
		}
		
		/**
		 * Scale the backing field of this entry by the given scale.<br>
		 * The scale is applied in both directions, when committing the field's value,
		 * the inverse of the scale is applied before saving the changes to the config.<br>
		 * To instead add a secondary backing field, use {@link #fieldScale(String, double)},
		 * {@link #addFieldScale(String, double)} or {@link #add_field_scale(String, double)}.
		 *
		 * @param scale The scale by which the config value is <em>multiplied</em>
		 *              before being stored in the backing field
		 * @see #fieldScale(String, double)
		 * @see #addFieldScale(String, double)
		 * @see #add_field_scale(String, double)
		 */
		@Contract(pure=true) public Builder fieldScale(double scale) {
			if (scale == 0D || !Double.isFinite(scale))
				throw new IllegalArgumentException("Scale must be a non-zero finite number");
			return field(d -> d * scale, d -> d / scale, Double.class);
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
			if (scale == 0D || !Double.isFinite(scale))
				throw new IllegalArgumentException("Scale must be a non-zero finite number");
			return addField(BackingFieldBinding.withName(
			  name, BackingFieldBuilder.of(f -> f * scale, Double.class)));
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
			if (scale == 0D || !Double.isFinite(scale))
				throw new IllegalArgumentException("Scale must be a non-zero finite number");
			return addField(suffix, f -> f * scale, Double.class);
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
			if (scale == 0D || !Double.isFinite(scale))
				throw new IllegalArgumentException("Scale must be a non-zero finite number");
			return add_field(suffix, f -> f * scale, Double.class);
		}
		
		@Override
		protected void checkBounds() {
			min = min == null ? Double.NEGATIVE_INFINITY : min;
			max = max == null ? Double.POSITIVE_INFINITY : max;
			if (min.isNaN() || max.isNaN())
				throw new IllegalArgumentException("NaN bound in double config entry");
			if (asSlider && (min.isInfinite() || max.isInfinite()))
				throw new IllegalArgumentException("Infinite bound in double config entry");
			super.checkBounds();
		}
		
		@Override
		protected DoubleEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new DoubleEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy() {
			return new Builder(value);
		}
	}
	
	@Nullable
	@Override public Double fromConfig(@Nullable Number value) {
		return value != null ? value.doubleValue() : null;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override public Optional<FieldBuilder<Double, ?, ?>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		if (!asSlider) {
			final DoubleFieldBuilder valBuilder = builder
			  .startDoubleField(getDisplayName(), get())
			  .setMin(min).setMax(max);
			return Optional.of(decorate(valBuilder));
		} else {
			final DoubleSliderBuilder valBuilder =
			  new DoubleSliderBuilder(builder, getDisplayName(), get(), min, max)
				 .setTextGetter(sliderTextSupplier);
			return Optional.of(decorate(valBuilder));
		}
	}
	
}
