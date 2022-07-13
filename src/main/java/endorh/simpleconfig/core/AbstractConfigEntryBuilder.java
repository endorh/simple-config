package endorh.simpleconfig.core;

import endorh.simpleconfig.core.BackingField.BackingFieldBinding;
import endorh.simpleconfig.core.BackingField.BackingFieldBuilder;
import endorh.simpleconfig.core.SimpleConfig.IGUIEntryBuilder;
import endorh.simpleconfig.core.SimpleConfig.InvalidConfigValueTypeException;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An {@link AbstractConfigEntry} builder.<br>
 * Immutable. All methods return modified copies, so reusing is possible.<br>
 * All subclasses must follow this contract and implement the
 * {@link AbstractConfigEntryBuilder#createCopy()} method copying their state.
 * @param <V> The type of the value held by the entry
 * @param <Config> The type of the associated config entry
 * @param <Gui> The type of the associated GUI entry
 * @param <Entry> The type of the entry built
 * @param <Self> The actual subtype of this builder to be returned
 *               by builder-like methods
 */
public abstract class AbstractConfigEntryBuilder<
  V, Config, Gui,
  Entry extends AbstractConfigEntry<V, Config, Gui, Entry>,
  Self extends AbstractConfigEntryBuilder<V, Config, Gui, Entry, Self>
> implements IGUIEntryBuilder, ITooltipEntry<V, Gui, Self>, IErrorEntry<V, Gui, Self> {
	protected V value;
	
	protected @Nullable Function<V, Optional<ITextComponent>> errorSupplier = null;
	protected @Nullable Function<Gui, Optional<ITextComponent>> guiErrorSupplier = null;
	protected @Nullable Function<V, Optional<ITextComponent[]>> tooltipSupplier = null;
	protected @Nullable Function<Gui, Optional<ITextComponent[]>> guiTooltipSupplier = null;
	protected @Nullable Supplier<Boolean> editableSupplier = null;
	protected List<Object> nameArgs = new ArrayList<>();
	protected List<Object> tooltipArgs = new ArrayList<>();
	protected boolean requireRestart = false;
	protected Class<?> typeClass;
	protected boolean nonPersistent = false;
	protected boolean ignored = false;
	protected BackingFieldBuilder<V, ?> backingFieldBuilder;
	protected List<BackingFieldBinding<V, ?>> backingFieldBindings = new ArrayList<>();
	
	public AbstractConfigEntryBuilder(V value, Class<?> typeClass) {
		this.value = value;
		this.typeClass = typeClass;
		backingFieldBuilder = typeClass != null? BackingFieldBuilder.<V, V>of(
		  Function.identity(), typeClass
		).withCommitter(Function.identity()) : null;
	}
	
	protected abstract Entry buildEntry(ISimpleConfigEntryHolder parent, String name);
	
	@Override
	public Self guiError(Function<Gui, Optional<ITextComponent>> guiErrorSupplier) {
		final Self copy = copy();
		copy.guiErrorSupplier = guiErrorSupplier;
		return copy;
	}
	
	@Override
	public Self error(Function<V, Optional<ITextComponent>> errorSupplier) {
		final Self copy = copy();
		copy.errorSupplier = errorSupplier;
		return copy;
	}
	
	@Override
	public Self guiTooltip(Function<Gui, Optional<ITextComponent[]>> tooltipSupplier) {
		final Self copy = copy();
		copy.guiTooltipSupplier = tooltipSupplier;
		return copy;
	}
	
	@Override
	public Self tooltip(Function<V, Optional<ITextComponent[]>> tooltipSupplier) {
		final Self copy = copy();
		copy.tooltipSupplier = tooltipSupplier;
		return copy;
	}
	
	/**
	 * Set the arguments that will be passed to the tooltip translation key<br>
	 * As a special case, {@code Function}s and {@code Supplier}s passed
	 * will be invoked before being passed as arguments, with the entry value
	 * as argument
	 */
	public Self tooltipArgs(Object... args) {
		final Self copy = copy();
		for (Object o : args) { // Check function types to fail fast
			if (o instanceof Function) {
				try {
					//noinspection unchecked
					((Function<V, ?>) o).apply(value);
				} catch (ClassCastException e) {
					throw new InvalidConfigValueTypeException("",
					  e, "A translation argument provider expected an invalid value type");
				}
			}
		}
		copy.tooltipArgs.clear();
		copy.tooltipArgs.addAll(Arrays.asList(args));
		return copy;
	}
	
	@SafeVarargs
	public final Self tooltipArgs(Function<Gui, Object>... args) {
		return tooltipArgs((Object[]) args);
	}
	
	@SafeVarargs
	public final Self tooltipArgs(Supplier<Object>... args) {
		return tooltipArgs((Object[]) args);
	}
	
	/**
	 * Set the arguments that will be passed to the name translation key<br>
	 * As a special case, {@code Supplier}s passed
	 * will be invoked before being passed as arguments
	 */
	public Self nameArgs(Object... args) {
		Self copy = copy();
		for (Object arg : args) {
			if (arg instanceof Function)
				throw new IllegalArgumentException(
				  "Name args cannot be functions that depend on the value, since names aren't refreshed");
		}
		copy.nameArgs.clear();
		copy.nameArgs.addAll(Arrays.asList(args));
		return copy;
	}
	
	/**
	 * Add a secondary field with the given suffix to the entry
	 * @param suffix Suffix for the field name
	 * @param mapper Transformation to apply for this field
	 * @param type   Type of the field
	 */
	public <F> Self addField(String suffix, Function<V, F> mapper, Class<F> type) {
		return addField(BackingFieldBinding.withSuffix(suffix, BackingFieldBuilder.of(mapper, type)));
	}
	
	/**
	 * Add a secondary field with the given snake_case suffix to the entry
	 * @param suffix Suffix for the field name (will be prepended with '_')
	 * @param mapper Transformation to apply for this field
	 * @param type   Type of the field
	 */
	public <F> Self add_field(String suffix, Function<V, F> mapper, Class<F> type) {
		return addField("_" + suffix, mapper, type);
	}
	
	/**
	 * Apply a field mapper to the main backing field of the entry.<br>
	 * @param mapper The transformation to apply to the main backing field
	 * @param reader Optional reading function to use when committing this field.
	 * @param type   The type of the field, used for checking
	 */
	public <F> Self field(Function<V, F> mapper, Function<F, V> reader, Class<F> type) {
		Self copy = copy();
		copy.backingFieldBuilder = BackingFieldBuilder.of(mapper, type).withCommitter(reader);
		return copy;
	}
	
	/**
	 * Apply a field mapper to the main backing field of the entry.<br>
	 * <b>This entry won't be committable from this field</b> unless a reader
	 * function is also passed.
	 * @param mapper The transformation to apply to the main backing field
	 * @param type   The type of the field, used for checking
	 */
	public <F> Self field(Function<V, F> mapper, Class<F> type) {
		Self copy = copy();
		copy.backingFieldBuilder = BackingFieldBuilder.of(mapper, type);
		return copy;
	}
	
	protected <F> Self addField(BackingFieldBinding<V, F> binding) {
		Self copy = copy();
		copy.backingFieldBindings.add(binding);
		return copy;
	}
	
	/**
	 * Flag this entry as requiring a restart to be effective
	 */
	public Self restart() {
		return restart(true);
	}
	
	/**
	 * Flag this entry as requiring a restart to be effective
	 */
	public Self restart(boolean requireRestart) {
		Self copy = copy();
		copy.requireRestart = requireRestart;
		return copy;
	}
	
	public Self editable(Supplier<Boolean> editable) {
		Self copy = copy();
		copy.editableSupplier = editable;
		return copy;
	}
	
	/**
	 * Makes this entry non-persistent.<br>
	 * Non-persistent entries only appear in the GUI, not in the config file,
	 * and they're reset on restart
	 */
	public Self temp() {
		return temp(true);
	}
	
	/**
	 * Makes this entry non-persistent.<br>
	 * Non-persistent entries only appear in the GUI, not in the config file,
	 * and they're reset on restart
	 */
	public Self temp(boolean nonPersistent) {
		Self copy = copy();
		copy.nonPersistent = nonPersistent;
		if (!nonPersistent)
			copy.ignored = false;
		return copy;
	}
	
	/**
	 * Ignore changes to this entry in the GUI.<br>
	 * Ignored entries automatically become non-persistent.<br>
	 * Useful for entries used to provide interaction in the config GUI,
	 * without actually storing any value
	 */
	public Self ignored() {
		return ignored(true);
	}
	
	/**
	 * Ignore changes to this entry in the GUI.<br>
	 * Ignored entries automatically become non-persistent.<br>
	 * Useful for entries used to provide interaction in the config GUI,
	 * without actually storing any value
	 */
	public Self ignored(boolean ignored) {
		Self copy = copy();
		copy.ignored = ignored;
		if (ignored)
			copy.nonPersistent = true;
		return copy;
	}
	
	/**
	 * Subclasses should instead override
	 * {@link AbstractConfigEntryBuilder#buildEntry(ISimpleConfigEntryHolder, String)}
	 * in most cases<br>
	 * Overrides should call super
	 */
	protected Entry build(ISimpleConfigEntryHolder parent, String name) {
		final Entry e = buildEntry(parent, name);
		e.requireRestart = requireRestart;
		e.errorSupplier = errorSupplier;
		e.guiErrorSupplier = guiErrorSupplier;
		e.tooltipSupplier = tooltipSupplier;
		e.guiTooltipSupplier = guiTooltipSupplier;
		e.nameArgs = nameArgs;
		e.tooltipArgs = tooltipArgs;
		e.typeClass = typeClass;
		e.editableSupplier = editableSupplier;
		e.nonPersistent = nonPersistent;
		if (nonPersistent)
			e.actualValue = e.value;
		e.ignored = ignored;
		// if (!e.isValidValue(value))
		// 	throw new InvalidDefaultConfigValueException(e.getGlobalPath(), value);
		return e;
	}
	
	@Internal @Deprecated public static <V> V getValue(AbstractConfigEntryBuilder<V, ?, ?, ?, ?> builder) {
		return builder.value;
	}
	
	/**
	 * Create a copy of this builder<br>
	 * Do not call directly, instead use {@link AbstractConfigEntryBuilder#copy()}<br>
	 * Subclasses should implement this method copying all of their fields.
	 */
	protected abstract Self createCopy();
	
	/**
	 * Creates a copy of this builder
	 */
	protected Self copy() {
		final Self copy = createCopy();
		copy.value = value;
		copy.errorSupplier = errorSupplier;
		copy.guiErrorSupplier = guiErrorSupplier;
		copy.tooltipSupplier = tooltipSupplier;
		copy.guiTooltipSupplier = guiTooltipSupplier;
		copy.nameArgs = new ArrayList<>(nameArgs);
		copy.tooltipArgs = new ArrayList<>(tooltipArgs);
		copy.requireRestart = requireRestart;
		copy.typeClass = typeClass;
		copy.editableSupplier = editableSupplier;
		copy.nonPersistent = nonPersistent;
		copy.ignored = ignored;
		copy.backingFieldBindings = new ArrayList<>(backingFieldBindings);
		return copy;
	}
	
	// Accessor
	protected static <
	  V, C, G, E extends AbstractConfigEntry<V, C, G, E>,
	  B extends AbstractConfigEntryBuilder<V, C, G, E, B>
	> B copyBuilder(B builder) {
		return builder.copy();
	}
}
