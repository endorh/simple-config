package endorh.simpleconfig.api;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public interface ConfigEntryBuilder<V, Config, Gui, Self extends ConfigEntryBuilder<V, Config, Gui, Self>>
  extends TooltipEntryBuilder<V, Gui, Self>,
          ErrorEntryBuilder<V, Config, Gui, Self> {
	@Internal V getValue();
	
	@Contract(pure=true)
	@Override Self guiError(Function<Gui, Optional<Component>> guiErrorSupplier);
	
	@Contract(pure=true)
	@Override Self error(Function<V, Optional<Component>> errorSupplier);
	
	@Contract(pure=true)
	@Override Self configError(Function<Config, Optional<Component>> configErrorSupplier);
	
	@Contract(pure=true)
	@Override Self withoutError();
	
	@Contract(pure=true)
	@Override Self guiTooltip(Function<Gui, List<Component>> tooltipSupplier);
	
	@Contract(pure=true)
	@Override Self tooltip(Function<V, List<Component>> tooltipSupplier);
	
	@Contract(pure=true) @Override Self withoutTooltip();
	
	/**
	 * Set the arguments that will be passed to the tooltip translation key<br>
	 * As a special case, {@code Supplier}s passed will be invoked before being
	 * passed as arguments.<br>
	 */
	@Contract(pure=true) Self tooltipArgs(Object... args);
	
	@Contract(pure=true)
	Self tooltipArgs(Supplier<Object>... args);
	
	/**
	 * Set the arguments that will be passed to the name translation key<br>
	 * As a special case, {@code Supplier}s passed will be invoked before being passed as
	 * arguments.<br><br>
	 * Since names aren't refreshed, the suppliers are only invoked once, and
	 * can't be used to animate the title.
	 */
	@Contract(pure=true) Self nameArgs(Object... args);
	
	/**
	 * Add a secondary field with the given suffix to the entry<br>
	 * To instead use a snake_case suffix, use {@link #add_field(String, Function, Class)}<br>
	 * To instead specify the full field name, use {@link #field(String, Function, Class)}
	 *
	 * @param suffix Suffix for the field name
	 * @param mapper Transformation to apply for this field
	 * @param type Type of the field
	 */
	@Contract(pure=true) <F> Self addField(String suffix, Function<V, F> mapper, Class<?> type);
	
	/**
	 * Add a secondary field with the given snake_case suffix to the entry<br>
	 * To instead use a camelCase suffix, use {@link #addField(String, Function, Class)}<br>
	 * To instead specify the full field name, use {@link #field(String, Function, Class)}
	 *
	 * @param suffix Suffix for the field name (will be prepended with '_')
	 * @param mapper Transformation to apply for this field
	 * @param type Type of the field
	 */
	@Contract(pure=true) <F> Self add_field(String suffix, Function<V, F> mapper, Class<?> type);
	
	/**
	 * Apply a field mapper to the main backing field of the entry.<br>
	 *
	 * @param mapper The transformation to apply to the main backing field
	 * @param reader Optional reading function to use when committing this field.
	 * @param type The type of the field, used for checking
	 */
	@Contract(pure=true) <F> Self field(Function<V, F> mapper, Function<F, V> reader, Class<?> type);
	
	/**
	 * Add a secondary field with the given name to the entry.<br>
	 * To instead provide a suffix, use {@link #addField(String, Function, Class)}<br>
	 * To instead replace the default field, use {@link #field(Function, Function, Class)}<br>
	 */
	<F> Self field(String name, Function<V, F> mapper, Class<?> type);
	
	/**
	 * Apply a field mapper to the main backing field of the entry.<br>
	 * <b>This entry won't be committable from this field</b> unless a reader
	 * function is also passed.
	 *
	 * @param mapper The transformation to apply to the main backing field
	 * @param type The type of the field, used for checking
	 */
	@Contract(pure=true) <F> Self field(Function<V, F> mapper, Class<?> type);
	
	/**
	 * Flag this entry as requiring a restart to be effective
	 */
	@Contract(pure=true) Self restart();
	
	/**
	 * Flag this entry as requiring a restart to be effective
	 */
	@Contract(pure=true) Self restart(boolean requireRestart);
	
	/**
	 * Flag this entry as experimental.<br>
	 */
	@Contract(pure=true) Self experimental();
	
	/**
	 * Flag this entry as experimental.<br>
	 */
	@Contract(pure=true) Self experimental(boolean experimental);
	
	/**
	 * Makes this entry conditionally editable in the config menu.<br>
	 * <b>This should only be used as a visual cue</b> to express that the value of this entry is
	 * contextually irrelevant.<br>
	 * Avoid overusing it, since users may find it frustrating.<br>
	 * <b>Users may be able to edit this entry through other means.</b><br>
	 * You may alse use {@link #editable(Function)} to receive the parent entry holder
	 * of this entry as an argument.
	 */
	@Contract(pure=true) Self editable(Supplier<Boolean> editable);
	
	/**
	 * Makes this entry conditionally editable in the config menu.<br>
	 * <b>This should only be used as a visual cue</b> to express that the value of this entry is
	 * contextually irrelevant.<br>
	 * Avoid overusing it, since users may find it frustrating.<br>
	 * <b>Users may be able to edit this entry through other means.</b>
	 */
	@Contract(pure=true) Self editable(Function<ConfigEntryHolder, Boolean> editable);
	
	/**
	 * Makes this entry non-persistent.<br>
	 * Non-persistent entries only appear in the GUI, not in the config file,
	 * and they're reset on restart
	 */
	@Contract(pure=true) Self temp();
	
	/**
	 * Makes this entry non-persistent.<br>
	 * Non-persistent entries only appear in the GUI, not in the config file,
	 * and they're reset on restart
	 */
	@Contract(pure=true) Self temp(boolean nonPersistent);
	
	/**
	 * Ignore changes to this entry in the GUI.<br>
	 * Ignored entries automatically become non-persistent.<br>
	 * Useful for entries used to provide interaction in the config GUI,
	 * without actually storing any value
	 */
	@Contract(pure=true) Self ignored();
	
	/**
	 * Ignore changes to this entry in the GUI.<br>
	 * Ignored entries automatically become non-persistent.<br>
	 * Useful for entries used to provide interaction in the config GUI,
	 * without actually storing any value
	 */
	@Contract(pure=true) Self ignored(boolean ignored);
	
	/**
	 * Add {@link EntryTag}s to this entry.
	 */
	@Contract(pure=true) Self withTags(EntryTag... tags);
	
	/**
	 * Remove {@link EntryTag}s from this entry.
	 */
	@Contract(pure=true) Self withoutTags(EntryTag... tags);
}
