package endorh.simpleconfig.core;

import endorh.simpleconfig.core.BackingField.BackingFieldBinding;
import endorh.simpleconfig.core.BackingField.BackingFieldBuilder;
import endorh.simpleconfig.core.SimpleConfig.IGUIEntryBuilder;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
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
  Entry extends AbstractConfigEntry<V, Config, Gui>,
  Self extends AbstractConfigEntryBuilder<V, Config, Gui, Entry, Self>
> implements IGUIEntryBuilder, ITooltipEntryBuilder<V, Gui, Self>,
             IErrorEntryBuilder<V, Config, Gui, Self> {
	protected V value;
	
	protected @Nullable BiFunction<AbstractConfigEntry<V, Config, Gui>, Gui, Optional<ITextComponent>> errorSupplier = null;
	protected @Nullable BiFunction<AbstractConfigEntry<V, Config, Gui>, Gui, List<ITextComponent>> tooltipSupplier = null;
	protected @Nullable Function<ISimpleConfigEntryHolder, Boolean> editableSupplier = null;
	protected @Nullable String translation = null;
	protected List<Object> nameArgs = new ArrayList<>();
	protected List<Object> tooltipArgs = new ArrayList<>();
	protected boolean requireRestart = false;
	protected boolean experimental = false;
	protected Class<?> typeClass;
	protected boolean nonPersistent = false;
	protected boolean ignored = false;
	protected Set<EntryTag> tags = new HashSet<>();
	protected BackingFieldBuilder<V, ?> backingFieldBuilder;
	protected List<BackingFieldBinding<V, ?>> backingFieldBindings = new ArrayList<>();
	
	public AbstractConfigEntryBuilder(V value, Class<?> typeClass) {
		this.value = value;
		this.typeClass = typeClass;
		backingFieldBuilder = typeClass != null? BackingFieldBuilder.<V, V>of(
		  Function.identity(), typeClass
		).withCommitter(Function.identity()) : null;
	}
	
	@Contract(pure=true) protected abstract Entry buildEntry(ISimpleConfigEntryHolder parent, String name);
	
	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	private static <T> Optional<T> or(Optional<T> a, Supplier<Optional<T>> b) {
		return a.isPresent()? a : b.get();
	}
	
	@Contract(pure=true)
	@Override public Self guiError(Function<Gui, Optional<ITextComponent>> guiErrorSupplier) {
		final Self copy = copy();
		BiFunction<AbstractConfigEntry<V, Config, Gui>, Gui, Optional<ITextComponent>> prev =
		  copy.errorSupplier == null? (e, g) -> Optional.empty() : copy.errorSupplier;
		copy.errorSupplier = (e, g) -> or(prev.apply(e, g), () -> guiErrorSupplier.apply(g));
		return copy;
	}
	
	@Contract(pure=true)
	@Override public Self error(Function<V, Optional<ITextComponent>> errorSupplier) {
		final Self copy = copy();
		BiFunction<AbstractConfigEntry<V, Config, Gui>, Gui, Optional<ITextComponent>> prev =
		  copy.errorSupplier == null? (e, g) -> Optional.empty() : copy.errorSupplier;
		copy.errorSupplier = (e, g) -> or(
		  prev.apply(e, g),
		  () -> {
			  V v = e.fromGui(g);
			  if (v == null) return Optional.of(new TranslationTextComponent(
			    "simpleconfig.config.error.missing_value"));
			  return errorSupplier.apply(v);
		  });
		return copy;
	}
	
	@Contract(pure=true)
	@Override public Self configError(Function<Config, Optional<ITextComponent>> configErrorSupplier) {
		final Self copy = copy();
		BiFunction<AbstractConfigEntry<V, Config, Gui>, Gui, Optional<ITextComponent>> prev =
		  copy.errorSupplier == null? (e, g) -> Optional.empty() : copy.errorSupplier;
		copy.errorSupplier = (e, g) -> or(
		  prev.apply(e, g),
		  () -> {
			  V v = e.fromGui(g);
			  if (v == null) return Optional.of(new TranslationTextComponent(
			    "simpleconfig.config.error.missing_value"));
			  return configErrorSupplier.apply(e.forConfig(v));
		  });
		return copy;
	}
	
	@Contract(pure=true)
	@Override public Self withoutError() {
		final Self copy = copy();
		copy.errorSupplier = null;
		return copy;
	}
	
	@Contract(pure=true)
	@Override public Self guiTooltip(Function<Gui, List<ITextComponent>> tooltipSupplier) {
		final Self copy = copy();
		copy.tooltipSupplier = (e, g) -> tooltipSupplier.apply(g);
		return copy;
	}
	
	@Contract(pure=true)
	@Override public Self tooltip(Function<V, List<ITextComponent>> tooltipSupplier) {
		final Self copy = copy();
		copy.tooltipSupplier = (e, g) -> {
			V v = e.fromGui(g);
			if (v == null) return Collections.emptyList();
			return tooltipSupplier.apply(v);
		};
		return copy;
	}
	
	@Contract(pure=true) @Override public Self withoutTooltip() {
		final Self copy = copy();
		copy.tooltipSupplier = null;
		return copy;
	}
	
	/**
	 * Set the arguments that will be passed to the tooltip translation key<br>
	 * As a special case, {@code Supplier}s passed will be invoked before being
	 * passed as arguments.<br>
	 */
	@Contract(pure=true)
	public Self tooltipArgs(Object... args) {
		final Self copy = copy();
		copy.tooltipArgs.clear();
		copy.tooltipArgs.addAll(Arrays.asList(args));
		return copy;
	}
	
	@Contract(pure=true)
	@SafeVarargs public final Self tooltipArgs(Supplier<Object>... args) {
		return tooltipArgs((Object[]) args);
	}
	
	/**
	 * Set the arguments that will be passed to the name translation key<br>
	 * As a special case, {@code Supplier}s passed will be invoked before being passed as
	 * arguments.<br><br>
	 * Since names aren't refreshed, the suppliers are only invoked once, and
	 * can't be used to animate the title.
	 */
	@Contract(pure=true)
	public Self nameArgs(Object... args) {
		Self copy = copy();
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
	@Contract(pure=true)
	public <F> Self addField(String suffix, Function<V, F> mapper, Class<F> type) {
		return addField(BackingFieldBinding.withSuffix(suffix, BackingFieldBuilder.of(mapper, type)));
	}
	
	/**
	 * Add a secondary field with the given snake_case suffix to the entry
	 * @param suffix Suffix for the field name (will be prepended with '_')
	 * @param mapper Transformation to apply for this field
	 * @param type   Type of the field
	 */
	@Contract(pure=true)
	public <F> Self add_field(String suffix, Function<V, F> mapper, Class<F> type) {
		return addField("_" + suffix, mapper, type);
	}
	
	/**
	 * Apply a field mapper to the main backing field of the entry.<br>
	 * @param mapper The transformation to apply to the main backing field
	 * @param reader Optional reading function to use when committing this field.
	 * @param type   The type of the field, used for checking
	 */
	@Contract(pure=true)
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
	@Contract(pure=true)
	public <F> Self field(Function<V, F> mapper, Class<F> type) {
		Self copy = copy();
		copy.backingFieldBuilder = BackingFieldBuilder.of(mapper, type);
		return copy;
	}
	
	@Contract(pure=true) protected <F> Self addField(BackingFieldBinding<V, F> binding) {
		Self copy = copy();
		copy.backingFieldBindings.add(binding);
		return copy;
	}
	
	/**
	 * Flag this entry as requiring a restart to be effective
	 */
	@Contract(pure=true) public Self restart() {
		return restart(true);
	}
	
	/**
	 * Flag this entry as requiring a restart to be effective
	 */
	@Contract(pure=true) public Self restart(boolean requireRestart) {
		Self copy = copy();
		copy.requireRestart = requireRestart;
		return copy;
	}
	
	/**
	 * Flag this entry as experimental.<br>
	 */
	@Contract(pure=true) public Self experimental() {
		return experimental(true);
	}
	
	/**
	 * Flag this entry as experimental.<br>
	 */
	@Contract(pure=true) public Self experimental(boolean experimental) {
		Self copy = copy();
		copy.experimental = experimental;
		return copy;
	}
	
	/**
	 * Makes this entry conditionally editable in the config menu.<br>
	 * <b>This should only be used as a visual cue</b> to express that the value of this entry is
	 * contextually irrelevant.<br>
	 * Avoid overusing it, since users may find it frustrating.<br>
	 * <b>Users may be able to edit this entry through other means.</b><br>
	 * You may alse use {@link #editable(Function)} to receive the parent entry holder
	 * of this entry as an argument.
	 */
	@Contract(pure=true) public Self editable(Supplier<Boolean> editable) {
		Self copy = copy();
		copy.editableSupplier = h -> editable.get();
		return copy;
	}
	
	/**
	 * Makes this entry conditionally editable in the config menu.<br>
	 * <b>This should only be used as a visual cue</b> to express that the value of this entry is
	 * contextually irrelevant.<br>
	 * Avoid overusing it, since users may find it frustrating.<br>
	 * <b>Users may be able to edit this entry through other means.</b>
	 */
	@Contract(pure=true) public Self editable(Function<ISimpleConfigEntryHolder, Boolean> editable) {
		Self copy = copy();
		copy.editableSupplier = editable;
		return copy;
	}
	
	/**
	 * Makes this entry non-persistent.<br>
	 * Non-persistent entries only appear in the GUI, not in the config file,
	 * and they're reset on restart
	 */
	@Contract(pure=true) public Self temp() {
		return temp(true);
	}
	
	/**
	 * Makes this entry non-persistent.<br>
	 * Non-persistent entries only appear in the GUI, not in the config file,
	 * and they're reset on restart
	 */
	@Contract(pure=true) public Self temp(boolean nonPersistent) {
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
	@Contract(pure=true) public Self ignored() {
		return ignored(true);
	}
	
	/**
	 * Ignore changes to this entry in the GUI.<br>
	 * Ignored entries automatically become non-persistent.<br>
	 * Useful for entries used to provide interaction in the config GUI,
	 * without actually storing any value
	 */
	@Contract(pure=true) public Self ignored(boolean ignored) {
		Self copy = copy();
		copy.ignored = ignored;
		if (ignored) copy.nonPersistent = true;
		return copy;
	}
	
	@Contract(pure=true)
	@Internal protected Self translation(@Nullable String translation) {
		Self copy = copy();
		copy.translation = translation;
		return copy;
	}
	
	/**
	 * Add {@link EntryTag}s to this entry.
	 */
	@Contract(pure=true) public Self withTags(EntryTag... tags) {
		Self copy = copy();
		copy.tags.addAll(Arrays.asList(tags));
		return copy;
	}
	
	/**
	 * Remove {@link EntryTag}s from this entry.
	 */
	@Contract(pure=true) public Self withoutTags(EntryTag... tags) {
		Self copy = copy();
		Arrays.stream(tags).forEach(copy.tags::remove);
		return copy;
	}
	
	/**
	 * Subclasses should instead override
	 * {@link AbstractConfigEntryBuilder#buildEntry(ISimpleConfigEntryHolder, String)}
	 * in most cases<br>
	 * Overrides should call super
	 */
	@Contract(value="_, _ -> new", pure=true) @MustBeInvokedByOverriders
	protected Entry build(@NotNull ISimpleConfigEntryHolder parent, String name) {
		final Entry e = buildEntry(parent, name);
		e.requireRestart = requireRestart;
		e.experimental = experimental;
		e.errorSupplier = errorSupplier;
		e.tooltipSupplier = tooltipSupplier;
		e.translation = translation;
		e.nameArgs = nameArgs;
		e.tooltipArgs = tooltipArgs;
		e.typeClass = typeClass;
		e.editableSupplier = editableSupplier;
		e.nonPersistent = nonPersistent;
		if (nonPersistent)
			e.actualValue = e.defValue;
		e.ignored = ignored;
		e.tags.clear();
		e.tags.addAll(tags);
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
	@Contract(value="-> new", pure=true) protected abstract Self createCopy();
	
	/**
	 * Creates a copy of this builder
	 */
	@Contract(value="-> new", pure=true) @MustBeInvokedByOverriders
	protected Self copy() {
		final Self copy = createCopy();
		copy.value = value;
		copy.translation = translation;
		copy.errorSupplier = errorSupplier;
		copy.tooltipSupplier = tooltipSupplier;
		copy.nameArgs = new ArrayList<>(nameArgs);
		copy.tooltipArgs = new ArrayList<>(tooltipArgs);
		copy.requireRestart = requireRestart;
		copy.experimental = experimental;
		copy.typeClass = typeClass;
		copy.editableSupplier = editableSupplier;
		copy.nonPersistent = nonPersistent;
		copy.ignored = ignored;
		copy.backingFieldBindings = new ArrayList<>(backingFieldBindings);
		copy.tags = new HashSet<>(tags);
		return copy;
	}
	
	// Accessor
	protected static <
	  V, C, G, E extends AbstractConfigEntry<V, C, G>,
	  B extends AbstractConfigEntryBuilder<V, C, G, E, B>
	> B copyBuilder(B builder) {
		return builder.copy();
	}
}
