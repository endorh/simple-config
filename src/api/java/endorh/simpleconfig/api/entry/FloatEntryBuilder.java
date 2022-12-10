package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface FloatEntryBuilder
  extends RangedEntryBuilder<@NotNull Float, Number, Float, FloatEntryBuilder>, AtomicEntryBuilder {
	/**
	 * Set min (inclusive)
	 */
	@Contract(pure=true) @NotNull default FloatEntryBuilder min(float min) {
		return min((Float) min);
	}
	
	/**
	 * Set max (inclusive)
	 */
	@Contract(pure=true) @NotNull default FloatEntryBuilder max(float max) {
		return max((Float) max);
	}
	
	/**
	 * Set inclusive range
	 */
	@Contract(pure=true) @NotNull default FloatEntryBuilder range(float min, float max) {
		return range((Float) min, (Float) max);
	}
	
	/**
	 * Set a different range (inclusive) for the slider.<br>
	 * Will be constrained by the range set by {@link #range}.<br>
	 * Useful to display a recommended/commonly useful range of values,
	 * while allowing users to override this range by manually entering
	 * a value beyond it (but still within the normal range).<br>
	 * Implies {@link #slider()}.
	 */
	@Contract(pure=true) default @NotNull FloatEntryBuilder sliderRange(float min, float max) {
		return sliderRange((Float) min, (Float) max);
	}
	
	/**
	 * Scale baked values of this entry by the given scale.<br>
	 * Equivalent to {@code baked(f -> f * scale, f -> f / scale)}.<br><br>
	 *
	 * The scale is applied in both directions, when committing the field's value,
	 * the inverse of the scale is applied before saving the changes to the config.<br><br>
	 *
	 * This will affect all backing fields, as well as Kotlin delegated properties.<br>
	 * To instead scale only the main backing field, use {@link #fieldScale(float)}<br>
	 * To instead add a scaled secondary backing field, use {@link #fieldScale(String, float)},
	 * {@link #addFieldScale(String, float)} or {@link #add_field_scale(String, float)}.
	 *
	 * @param scale The scale by which the config value is <em>multiplied</em> when being baked.
	 * @see #fieldScale(float)
	 * @see #fieldScale(String, float)
	 * @see #addFieldScale(String, float)
	 * @see #add_field_scale(String, float)
	 */
	@Contract(pure=true) @NotNull FloatEntryBuilder bakeScale(float scale);
	
	/**
	 * Scale the backing field of this entry by the given scale.<br>
	 * Prefer using {@link #bakeScale(float)} if you don't need the unscaled value.<br><br>
	 *
	 * The scale is applied in both directions, when committing the field's value,
	 * the inverse of the scale is applied before saving the changes to the config.<br><br>
	 *
	 * To instead add a secondary backing field, use {@link #fieldScale(String, float)},
	 * {@link #addFieldScale(String, float)} or {@link #add_field_scale(String, float)}.
	 *
	 * @param scale The scale by which the config value is <em>multiplied</em>
	 *   before being stored in the backing field
	 * @see #bakeScale
	 * @see #fieldScale(String, float)
	 * @see #addFieldScale(String, float)
	 * @see #add_field_scale(String, float)
	 */
	@Contract(pure=true) @NotNull FloatEntryBuilder fieldScale(float scale);
	
	/**
	 * Add a secondary backing field with the given name, whose value is pre-multiplied
	 * by the given scale on bake.
	 *
	 * @param name The name of the secondary backing field
	 * @param scale The scale by which the config value is <em>multiplied</em>
	 *   before being stored in the backing field
	 * @see #bakeScale
	 * @see #fieldScale(float)
	 * @see #addFieldScale(String, float)
	 * @see #add_field_scale(String, float)
	 */
	@Contract(pure=true) @NotNull FloatEntryBuilder fieldScale(String name, float scale);
	
	/**
	 * Add a secondary backing field with the given camelCase suffix, whose value
	 * is pre-multiplied by the given scale on bake.
	 *
	 * @param suffix The camelCase suffix used to generate the backing field's name
	 * @param scale The scale by which the config value is <em>multiplied</em>
	 *   before being stored in the backing field
	 * @see #bakeScale
	 * @see #fieldScale(float)
	 * @see #fieldScale(String, float)
	 * @see #add_field_scale(String, float)
	 */
	@Contract(pure=true) @NotNull FloatEntryBuilder addFieldScale(String suffix, float scale);
	
	/**
	 * Add a secondary backing field with the given snake_case suffix, whose value
	 * is pre-multiplied by the given scale on bake.
	 *
	 * @param suffix The snake_case suffix used to generate the backing field's name
	 * @param scale The scale by which the config value is <em>multiplied</em>
	 *   before being stored in the backing field
	 * @see #bakeScale
	 * @see #fieldScale(float)
	 * @see #fieldScale(String, float)
	 * @see #addFieldScale(String, float)
	 */
	@Contract(pure=true) @NotNull FloatEntryBuilder add_field_scale(String suffix, float scale);
}
