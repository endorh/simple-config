package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.core.BackingField.BackingFieldBinding;
import endorh.simpleconfig.core.BackingField.BackingFieldBuilder;
import endorh.simpleconfig.core.IKeyEntry;
import endorh.simpleconfig.core.ISimpleConfigEntryHolder;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FloatFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FloatSliderBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class FloatEntry extends AbstractRangedEntry<Float, Number, Float, FloatEntry>
  implements IKeyEntry<Float> {
	@Internal public FloatEntry(
	  ISimpleConfigEntryHolder parent, String name, float value
	) {
		super(parent, name, value);
		commentMin = -Float.MAX_VALUE;
		commentMax = Float.MAX_VALUE;
	}
	
	public static class Builder
	  extends AbstractRangedEntry.Builder<Float, Number, Float, FloatEntry, Builder> {
		public Builder(Float value) {
			super(value, Float.class, "%.2f");
		}
		
		/**
		 * Set min (inclusive)
		 */
		@Contract(pure=true) public Builder min(float min) {
			return super.min(min);
		}
		
		/**
		 * Set max (inclusive)
		 */
		@Contract(pure=true) public Builder max(float max) {
			return super.max(max);
		}
		
		/**
		 * Set inclusive range
		 */
		@Contract(pure=true) public Builder range(float min, float max) {
			return super.range(min, max);
		}
		
		/**
		 * Scale the backing field of this entry by the given scale.<br>
		 * The scale is applied in both directions, when committing the field's value,
		 * the inverse of the scale is applied before saving the changes to the config.
		 * To instead add a secondary backing field, use {@link #fieldScale(String, float)},
		 * {@link #addFieldScale(String, float)} or {@link #add_field_scale(String, float)}.
		 *
		 * @param scale The scale by which the config value is <em>multiplied</em>
		 *              before being stored in the backing field
		 * @see #fieldScale(String, float)
		 * @see #addFieldScale(String, float)
		 * @see #add_field_scale(String, float)
		 */
		@Contract(pure=true) public Builder fieldScale(float scale) {
			if (scale == 0F || !Float.isFinite(scale))
				throw new IllegalArgumentException("Scale must be a non-zero finite number");
			return field(f -> f * scale, f -> f / scale, Float.class);
		}
		
		/**
		 * Add a secondary backing field with the given name, whose value is pre-multiplied
		 * by the given scale on bake.
		 * @param name The name of the secondary backing field
		 * @param scale The scale by which the config value is <em>multiplied</em>
		 *              before being stored in the backing field
		 * @see #fieldScale(float)
		 * @see #addFieldScale(String, float)
		 * @see #add_field_scale(String, float)
		 */
		@Contract(pure=true) public Builder fieldScale(String name, float scale) {
			return addField(BackingFieldBinding.withName(
			  name, BackingFieldBuilder.of(f -> f * scale, Float.class)));
		}
		
		/**
		 * Add a secondary backing field with the given camelCase suffix, whose value
		 * is pre-multiplied by the given scale on bake.
		 * @param suffix The camelCase suffix used to generate the backing field's name
		 * @param scale The scale by which the config value is <em>multiplied</em>
		 *              before being stored in the backing field
		 * @see #fieldScale(float)
		 * @see #fieldScale(String, float)
		 * @see #add_field_scale(String, float)
		 */
		@Contract(pure=true) public Builder addFieldScale(String suffix, float scale) {
			return addField(suffix, f -> f * scale, Float.class);
		}
		
		/**
		 * Add a secondary backing field with the given snake_case suffix, whose value
		 * is pre-multiplied by the given scale on bake.
		 * @param suffix The snake_case suffix used to generate the backing field's name
		 * @param scale The scale by which the config value is <em>multiplied</em>
		 *              before being stored in the backing field
		 * @see #fieldScale(float)
		 * @see #fieldScale(String, float)
		 * @see #addFieldScale(String, float)
		 */
		@Contract(pure=true) public Builder add_field_scale(String suffix, float scale) {
			return add_field(suffix, f -> f * scale, Float.class);
		}
		
		@Override
		protected void checkBounds() {
			min = min == null ? Float.NEGATIVE_INFINITY : min;
			max = max == null ? Float.POSITIVE_INFINITY : max;
			if (min.isNaN() || max.isNaN())
				throw new IllegalArgumentException("NaN bound in float config entry");
			if (asSlider && (min.isInfinite() || max.isInfinite()))
				throw new IllegalArgumentException("Infinite bound in float config entry");
			super.checkBounds();
		}
		
		@Override
		protected FloatEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new FloatEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy() {
			return new Builder(value);
		}
	}
	
	@Nullable
	@Override public Float fromConfig(@Nullable Number value) {
		return value != null ? value.floatValue() : null;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override public Optional<FieldBuilder<Float, ?, ?>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		if (!asSlider) {
			final FloatFieldBuilder valBuilder = builder
			  .startFloatField(getDisplayName(), get())
			  .setMin(min).setMax(max);
			return Optional.of(decorate(valBuilder));
		} else {
			final FloatSliderBuilder valBuilder =
			  new FloatSliderBuilder(builder, getDisplayName(), get(), min, max)
				 .setTextGetter(sliderTextSupplier);
			return Optional.of(decorate(valBuilder));
		}
	}
	
}
