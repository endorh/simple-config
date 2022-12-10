package endorh.simpleconfig.api;

import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Supplier;

public interface ConfigEntryBuilder<V, Config, Gui, Self extends ConfigEntryBuilder<V, Config, Gui, Self>>
  extends TooltipEntryBuilder<V, Gui, Self>, ErrorEntryBuilder<V, Config, Gui, Self> {
	/**
	 * Get the default value of this entry builder.<br>
	 */
	@Internal V getValue();
	
	/**
	 * Change the default value of this entry.<br>
	 * Mainly useful when reusing an entry builder for multiple entries.
	 */
	@Contract(pure=true) @NotNull Self withValue(V value);
	
	/**
	 * Set the arguments that will be passed to the tooltip translation key<br>
	 * As a special case, {@code Supplier}s passed will be invoked before being
	 * passed as arguments.<br>
	 */
	@Contract(pure=true) @NotNull Self tooltipArgs(Object... args);
	
	/**
	 * Set the arguments that will be passed to the tooltip translation key<br>
	 */
	@Contract(pure=true) @NotNull Self tooltipArgs(Supplier<Object>... args);
	
	/**
	 * Set the arguments that will be passed to the name translation key<br>
	 * As a special case, {@code Supplier}s passed will be invoked before being passed as
	 * arguments.<br><br>
	 * Since names aren't refreshed, the suppliers are only invoked once, and
	 * can't be used to animate the title.
	 */
	@Contract(pure=true) @NotNull Self nameArgs(Object... args);
	
	/**
	 * Specify a function that will be used to bake the value of this entry.<br><br>
	 *
	 * This function will be applied to the values stored in config fields,
	 * as well as to the values available through Kotlin delegated properties.<br><br>
	 *
	 * This baking function is always applied before any backing field transformations,
	 * and affects all backing fields.<br><br>
	 *
	 * <b>This entry won't be committable</b> from baked values unless you also specify
	 * an inverse function.
	 *
	 * @param presentation The function that will be used to bake the value of this entry
	 * @see #field(Function, Class)
	 */
	@Contract(pure=true) @NotNull Self baked(Function<V, V> presentation);
	
	/**
	 * Specify a function that will be used to bake the value of this entry.<br>
	 * You don't need to pass an {@code inverse} function, unless you want to
	 * be able to modify the value from your code and have the inverse applied.<br><br>
	 * <p>
	 * This function will be applied to the values stored in config fields,
	 * as well as to the values available through Kotlin delegated properties.<br><br>
	 * <p>
	 * This baking function is always applied before any backing field transformations,
	 * and affects all backing fields.<br>
	 *
	 * @param presentation The function that will be used to bake the value of this entry.
	 * @param inverse The function that will be used to commit changes from modified baked values
	 * @see #field(Function, Class)
	 */
	@Contract(pure=true) @NotNull Self baked(Function<V, V> presentation, Function<V, V> inverse);
	
	/**
	 * Add a secondary field with the given suffix to the entry<br>
	 * To instead use a snake_case suffix, use {@link #add_field(String, Function, Class)}<br>
	 * To instead specify the full field name, use {@link #field(String, Function, Class)}
	 *
	 * @param suffix Suffix for the field name
	 * @param mapper Transformation to apply for this field
	 * @param type Type of the field
	 */
	@Contract(pure=true) <F> @NotNull Self addField(String suffix, Function<V, F> mapper, Class<?> type);
	
	/**
	 * Add a secondary field with the given snake_case suffix to the entry<br>
	 * To instead use a camelCase suffix, use {@link #addField(String, Function, Class)}<br>
	 * To instead specify the full field name, use {@link #field(String, Function, Class)}
	 *
	 * @param suffix Suffix for the field name (will be prepended with '_')
	 * @param mapper Transformation to apply for this field
	 * @param type Type of the field
	 */
	@Contract(pure=true) <F> @NotNull Self add_field(String suffix, Function<V, F> mapper, Class<?> type);
	
	/**
	 * Apply a field mapper to the main backing field of the entry.<br>
	 *
	 * @param mapper The transformation to apply to the main backing field
	 * @param reader Optional reading function to use when committing this field.
	 * @param type The type of the field, used for checking
	 * @see #field(String, Function, Class)
	 * @see #baked
	 */
	@Contract(pure=true) <F> @NotNull Self field(Function<V, F> mapper, Function<F, V> reader, Class<?> type);
	
	/**
	 * Add a secondary field with the given name to the entry.<br>
	 * To instead provide a suffix, use {@link #addField(String, Function, Class)}<br>
	 * To instead replace the default field, use {@link #field(Function, Function, Class)}<br>
	 */
	<F> @NotNull Self field(String name, Function<V, F> mapper, Class<?> type);
	
	/**
	 * Apply a field mapper to the main backing field of the entry.<br>
	 * <b>This entry won't be committable from this field</b> unless a reader
	 * function is also passed.
	 *
	 * @param mapper The transformation to apply to the main backing field
	 * @param type The type of the field, used for checking
	 */
	@Contract(pure=true) <F> @NotNull Self field(Function<V, F> mapper, Class<?> type);
	
	/**
	 * Flag this entry as requiring a restart to be effective
	 */
	@Contract(pure=true) @NotNull Self restart();
	
	/**
	 * Flag this entry as requiring a restart to be effective
	 */
	@Contract(pure=true) @NotNull Self restart(boolean requireRestart);
	
	/**
	 * Flag this entry as experimental.<br>
	 */
	@Contract(pure=true) @NotNull Self experimental();
	
	/**
	 * Flag this entry as experimental.<br>
	 */
	@Contract(pure=true) @NotNull Self experimental(boolean experimental);
	
	/**
	 * Makes this entry conditionally editable in the config menu.<br>
	 * <b>This should only be used as a visual cue</b> to express that the value of this entry is
	 * contextually irrelevant.<br>
	 * Avoid overusing it, since users may find it frustrating.<br>
	 * <b>Users may be able to edit this entry through other means.</b><br>
	 * You may alse use {@link #editable(Function)} to receive the parent entry holder
	 * of this entry as an argument.
	 */
	@Contract(pure=true) @NotNull Self editable(Supplier<Boolean> editable);
	
	/**
	 * Makes this entry conditionally editable in the config menu.<br>
	 * <b>This should only be used as a visual cue</b> to express that the value of this entry is
	 * contextually irrelevant.<br>
	 * Avoid overusing it, since users may find it frustrating.<br>
	 * <b>Users may be able to edit this entry through other means.</b>
	 */
	@Contract(pure=true) @NotNull Self editable(Function<ConfigEntryHolder, Boolean> editable);
	
	/**
	 * Makes this entry non-persistent.<br>
	 * Non-persistent entries only appear in the GUI, not in the config file,
	 * and they're reset on restart
	 */
	@Contract(pure=true) @NotNull Self temp();
	
	/**
	 * Makes this entry non-persistent.<br>
	 * Non-persistent entries only appear in the GUI, not in the config file,
	 * and they're reset on restart
	 */
	@Contract(pure=true) @NotNull Self temp(boolean nonPersistent);
	
	/**
	 * Ignore changes to this entry in the GUI.<br>
	 * Ignored entries automatically become non-persistent.<br>
	 * Useful for entries used to provide interaction in the config GUI,
	 * without actually storing any value
	 */
	@Contract(pure=true) @NotNull Self ignored();
	
	/**
	 * Ignore changes to this entry in the GUI.<br>
	 * Ignored entries automatically become non-persistent.<br>
	 * Useful for entries used to provide interaction in the config GUI,
	 * without actually storing any value
	 */
	@Contract(pure=true) @NotNull Self ignored(boolean ignored);
	
	/**
	 * Add {@link EntryTag}s to this entry.
	 */
	@Contract(pure=true) @NotNull Self withTags(EntryTag... tags);
	
	/**
	 * Add or remove {@link EntryTag}s to this entry.<br>
	 * @param add Whether to add or remove the tags
	 */
	@Contract(pure=true) @NotNull default Self withTags(boolean add, EntryTag... tags) {
		return add? withTags(tags) : withoutTags(tags);
	}
	
	/**
	 * Remove {@link EntryTag}s from this entry.
	 */
	@Contract(pure=true) @NotNull Self withoutTags(EntryTag... tags);
}
