package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface DoubleEntryBuilder
  extends RangedEntryBuilder<@NotNull Double, Number, Double, DoubleEntryBuilder>,
          AtomicEntryBuilder {
	/**
	 * Set min (inclusive)
	 */
	@Contract(pure=true) @NotNull default DoubleEntryBuilder min(double min) {
		return min((Double) min);
	}
	
	/**
	 * Set max (inclusive)
	 */
	@Contract(pure=true) @NotNull default DoubleEntryBuilder max(double max) {
		return max((Double) max);
	}
	
	/**
	 * Set inclusive range
	 */
	@Contract(pure=true) @NotNull default DoubleEntryBuilder range(double min, double max) {
		return range((Double) min, (Double) max);
	}
	
	/**
	 * Set a different range (inclusive) for the slider.<br>
	 * Will be constrained by the range set by {@link #range}.<br>
	 * Useful to display a recommended/commonly useful range of values,
	 * while allowing users to override this range by manually entering
	 * a value beyond it (but still within the normal range).<br>
	 * Implies {@link #slider()}.
	 */
	@Contract(pure=true) default @NotNull DoubleEntryBuilder sliderRange(double min, double max) {
		return sliderRange((Double) min, (Double) max);
	}
	
	/**
	 * Scale baked values of this entry by the given scale.<br>
	 * Equivalent to {@code baked(d -> d * scale, d -> d / scale)}.<br><br>
	 *
	 * The scale is applied in both directions, when committing the field's value,
	 * the inverse of the scale is applied before saving the changes to the config.<br><br>
	 *
	 * This will affect all backing fields, as well as Kotlin delegated properties.<br>
	 * To instead scale only the main backing field, use {@link #fieldScale(double)}<br>
	 * To instead add a scaled secondary backing field, use {@link #fieldScale(String, double)},
	 * {@link #addFieldScale(String, double)} or {@link #add_field_scale(String, double)}.
	 *
	 * @param scale The scale by which the config value is <em>multiplied</em> when being baked.
	 * @see #fieldScale(double)
	 * @see #fieldScale(String, double)
	 * @see #addFieldScale(String, double)
	 * @see #add_field_scale(String, double)
	 */
	@Contract(pure=true) @NotNull DoubleEntryBuilder bakeScale(double scale);
	
	/**
	 * Scale the backing field of this entry by the given scale.<br>
	 * Prefer using {@link #bakeScale(double)} if you don't need the unscaled value.<br><br>
	 *
	 * The scale is applied in both directions, when committing the field's value,
	 * the inverse of the scale is applied before saving the changes to the config.<br><br>
	 *
	 * The scale is applied in both directions, when committing the field's value,
	 * the inverse of the scale is applied before saving the changes to the config.<br><br>
	 * To instead add a secondary backing field, use {@link #fieldScale(String, double)},
	 * {@link #addFieldScale(String, double)} or {@link #add_field_scale(String, double)}.
	 *
	 * @param scale The scale by which the config value is <em>multiplied</em>
	 *   before being stored in the backing field
	 * @see #bakeScale 
	 * @see #fieldScale(String, double)
	 * @see #addFieldScale(String, double)
	 * @see #add_field_scale(String, double)
	 */
	@Contract(pure=true) @NotNull DoubleEntryBuilder fieldScale(double scale);
	
	/**
	 * Add a secondary backing field with the given name, whose value is pre-multiplied
	 * by the given scale on bake.
	 *
	 * @param name The name of the secondary backing field
	 * @param scale The scale by which the config value is <em>multiplied</em>
	 *   before being stored in the backing field
	 * @see #bakeScale
	 * @see #fieldScale(double)
	 * @see #addFieldScale(String, double)
	 * @see #add_field_scale(String, double)
	 */
	@Contract(pure=true) @NotNull DoubleEntryBuilder fieldScale(String name, double scale);
	
	/**
	 * Add a secondary backing field with the given camelCase suffix, whose value
	 * is pre-multiplied by the given scale on bake.
	 *
	 * @param suffix The camelCase suffix used to generate the backing field's name
	 * @param scale The scale by which the config value is <em>multiplied</em>
	 *   before being stored in the backing field
	 * @see #bakeScale
	 * @see #fieldScale(double)
	 * @see #fieldScale(String, double)
	 * @see #add_field_scale(String, double)
	 */
	@Contract(pure=true) @NotNull DoubleEntryBuilder addFieldScale(String suffix, double scale);
	
	/**
	 * Add a secondary backing field with the given snake_case suffix, whose value
	 * is pre-multiplied by the given scale on bake.
	 *
	 * @param suffix The snake_case suffix used to generate the backing field's name
	 * @param scale The scale by which the config value is <em>multiplied</em>
	 *   before being stored in the backing field
	 * @see #bakeScale
	 * @see #fieldScale(double)
	 * @see #fieldScale(String, double)
	 * @see #addFieldScale(String, double)
	 */
	@Contract(pure=true) @NotNull DoubleEntryBuilder add_field_scale(String suffix, double scale);
}
