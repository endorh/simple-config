package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.KeyEntryBuilder;
import org.jetbrains.annotations.Contract;

public interface FloatEntryBuilder
  extends RangedEntryBuilder<Float, Number, Float, FloatEntryBuilder>, KeyEntryBuilder<Float> {
	/**
	 * Set min (inclusive)
	 */
	@Contract(pure=true) FloatEntryBuilder min(float min);
	
	/**
	 * Set max (inclusive)
	 */
	@Contract(pure=true) FloatEntryBuilder max(float max);
	
	/**
	 * Set inclusive range
	 */
	@Contract(pure=true) FloatEntryBuilder range(float min, float max);
	
	/**
	 * Scale the backing field of this entry by the given scale.<br>
	 * The scale is applied in both directions, when committing the field's value,
	 * the inverse of the scale is applied before saving the changes to the config.
	 * To instead add a secondary backing field, use {@link #fieldScale(String, float)},
	 * {@link #addFieldScale(String, float)} or {@link #add_field_scale(String, float)}.
	 *
	 * @param scale The scale by which the config value is <em>multiplied</em>
	 *   before being stored in the backing field
	 * @see #fieldScale(String, float)
	 * @see #addFieldScale(String, float)
	 * @see #add_field_scale(String, float)
	 */
	@Contract(pure=true) FloatEntryBuilder fieldScale(float scale);
	
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
	@Contract(pure=true) FloatEntryBuilder fieldScale(String name, float scale);
	
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
	@Contract(pure=true) FloatEntryBuilder addFieldScale(String suffix, float scale);
	
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
	@Contract(pure=true) FloatEntryBuilder add_field_scale(String suffix, float scale);
}
