package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AbstractRange.DoubleRange;
import endorh.simpleconfig.api.AtomicEntryBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface DoubleRangeEntryBuilder extends SizedRangeEntryBuilder<
  @NotNull Double, DoubleRange, DoubleRangeEntryBuilder
>, AtomicEntryBuilder {
	@Contract(pure=true) @NotNull DoubleRangeEntryBuilder min(double min);
	
	@Contract(pure=true) @NotNull DoubleRangeEntryBuilder max(double max);
	
	@Contract(pure=true) @NotNull DoubleRangeEntryBuilder withBounds(double min, double max);
	
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
	@Contract(pure=true) @NotNull DoubleRangeEntryBuilder fieldScale(double scale);
	
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
	@Contract(pure=true) @NotNull DoubleRangeEntryBuilder fieldScale(String name, double scale);
	
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
	@Contract(pure=true) @NotNull DoubleRangeEntryBuilder addFieldScale(String suffix, double scale);
	
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
	@Contract(pure=true) @NotNull DoubleRangeEntryBuilder add_field_scale(String suffix, double scale);
}
