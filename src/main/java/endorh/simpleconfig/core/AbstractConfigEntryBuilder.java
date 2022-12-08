package endorh.simpleconfig.core;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.EntryTag;
import endorh.simpleconfig.core.BackingField.BackingFieldBinding;
import endorh.simpleconfig.core.BackingField.BackingFieldBuilder;
import endorh.simpleconfig.core.wrap.ConfigEntryDelegate;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An {@link AbstractConfigEntry} builder.<br>
 * Immutable. All methods return modified copies, so reusing is possible.<br>
 * All subclasses must follow this contract and implement the
 * {@link AbstractConfigEntryBuilder#createCopy(Object)} method copying their state.
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
  Self extends ConfigEntryBuilder<V, Config, Gui, Self>,
  SelfImpl extends AbstractConfigEntryBuilder<V, Config, Gui, Entry, Self, SelfImpl>
> implements ConfigEntryBuilder<V, Config, Gui, Self> {
	protected V value;
	
	protected @Nullable Consumer<AbstractConfigEntry<V, Config, Gui>> buildListener = null;
	protected @Nullable BiFunction<AbstractConfigEntry<V, Config, Gui>, Gui, Optional<ITextComponent>> errorSupplier = null;
	protected @Nullable BiFunction<AbstractConfigEntry<V, Config, Gui>, Gui, List<ITextComponent>> tooltipSupplier = null;
	protected @Nullable Function<ConfigEntryHolder, Boolean> editableSupplier = null;
	protected @Nullable String translation = null;
	protected List<Object> nameArgs = new ArrayList<>();
	protected List<Object> tooltipArgs = new ArrayList<>();
	protected boolean requireRestart = false;
	protected boolean experimental = false;
	protected Class<?> typeClass;
	protected EntryType<?> type;
	protected boolean nonPersistent = false;
	protected boolean ignored = false;
	protected Set<EntryTag> tags = new HashSet<>();
	protected BackingFieldBuilder<V, ?> backingFieldBuilder;
	protected List<BackingFieldBinding<V, ?>> backingFieldBindings = new ArrayList<>();
	protected @Nullable ConfigEntryDelegate<V> delegate = null;
	
	public AbstractConfigEntryBuilder(V value, EntryType<?> type) {
		this.value = value;
		this.typeClass = type != null? type.type() : null;
		this.type = type;
		backingFieldBuilder = type != null? BackingFieldBuilder.<V, V>of(
		  Function.identity(), type
		).withCommitter(Function.identity()) : null;
	}
	
	@Internal public static EntryType<?> getEntryType(ConfigEntryBuilder<?, ?, ?, ?> builder) {
		return ((AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?>) builder).type;
	}
	
	@Contract(pure=true) @Internal public SelfImpl withBuildListener(
	  Consumer<AbstractConfigEntry<V, Config, Gui>> buildListener
	) {
		SelfImpl copy = copy();
		copy.buildListener = buildListener;
		return copy;
	}
	
	@Contract(pure=true) protected abstract Entry buildEntry(ConfigEntryHolder parent, String name);
	
	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	private static <T> Optional<T> or(Optional<T> a, Supplier<Optional<T>> b) {
		return a.isPresent()? a : b.get();
	}
	
	@SuppressWarnings("unchecked") protected Self castSelf() {
		return (Self) this;
	}
	
	@Override @Internal public V getValue() {
		return value;
	}
	
	@Internal public Class<?> getTypeClass() {
		return typeClass;
	}
	
	@Internal public EntryType<?> getEntryType() {
		return type;
	}
	
	@Contract(pure=true)
	@Override public @NotNull Self withValue(V value) {
		return copy(value).castSelf();
	}
	
	@Contract(pure=true)
	@Override public @NotNull Self guiError(Function<Gui, Optional<ITextComponent>> guiErrorSupplier) {
		final SelfImpl copy = copy();
		BiFunction<AbstractConfigEntry<V, Config, Gui>, Gui, Optional<ITextComponent>> prev =
		  copy.errorSupplier == null? (e, g) -> Optional.empty() : copy.errorSupplier;
		copy.errorSupplier = (e, g) -> or(prev.apply(e, g), () -> guiErrorSupplier.apply(g));
		return copy.castSelf();
	}
	
	@Contract(pure=true)
	@Override public @NotNull Self error(Function<V, Optional<ITextComponent>> errorSupplier) {
		final SelfImpl copy = copy();
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
		return copy.castSelf();
	}
	
	@Contract(pure=true)
	@Override public @NotNull Self configError(Function<Config, Optional<ITextComponent>> configErrorSupplier) {
		final SelfImpl copy = copy();
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
		return copy.castSelf();
	}
	
	@Contract(pure=true)
	@Override public @NotNull Self withoutError() {
		final SelfImpl copy = copy();
		copy.errorSupplier = null;
		return copy.castSelf();
	}
	
	@Contract(pure=true)
	@Override public @NotNull Self guiTooltip(Function<Gui, List<ITextComponent>> tooltipSupplier) {
		final SelfImpl copy = copy();
		copy.tooltipSupplier = (e, g) -> tooltipSupplier.apply(g);
		return copy.castSelf();
	}
	
	@Contract(pure=true)
	@Override public @NotNull Self tooltip(Function<V, List<ITextComponent>> tooltipSupplier) {
		final SelfImpl copy = copy();
		copy.tooltipSupplier = (e, g) -> {
			V v = e.fromGui(g);
			if (v == null) return Collections.emptyList();
			return tooltipSupplier.apply(v);
		};
		return copy.castSelf();
	}
	
	@Contract(pure=true) @Override public @NotNull Self withoutTooltip() {
		SelfImpl copy = copy();
		copy.tooltipSupplier = null;
		return copy.castSelf();
	}
	
	@Override @Contract(pure=true)
	public @NotNull Self tooltipArgs(Object... args) {
		SelfImpl copy = copy();
		copy.tooltipArgs.clear();
		copy.tooltipArgs.addAll(Arrays.asList(args));
		return copy.castSelf();
	}
	
	@Override @Contract(pure=true)
	@SafeVarargs public final @NotNull Self tooltipArgs(Supplier<Object>... args) {
		return tooltipArgs((Object[]) args);
	}
	
	@Override @Contract(pure=true)
	public @NotNull Self nameArgs(Object... args) {
		SelfImpl copy = copy();
		copy.nameArgs.clear();
		copy.nameArgs.addAll(Arrays.asList(args));
		return copy.castSelf();
	}
	
	@Override @Contract(pure=true)
	public <F> @NotNull Self addField(String suffix, Function<V, F> mapper, Class<?> type) {
		return addField(BackingFieldBinding.withSuffix(suffix, BackingFieldBuilder.of(mapper, EntryType.unchecked(type))));
	}
	
	@Override @Contract(pure=true)
	public <F> @NotNull Self add_field(String suffix, Function<V, F> mapper, Class<?> type) {
		return addField("_" + suffix, mapper, type);
	}
	
	@Override @Contract(pure=true)
	public <F> @NotNull Self field(Function<V, F> mapper, Function<F, V> reader, Class<?> type) {
		SelfImpl copy = copy();
		copy.backingFieldBuilder = BackingFieldBuilder.of(mapper, EntryType.unchecked(type)).withCommitter(reader);
		return copy.castSelf();
	}
	
	@Override public <F> @NotNull Self field(String name, Function<V, F> mapper, Class<?> type) {
		return addField(BackingFieldBinding.withName(name, BackingFieldBuilder.of(mapper, EntryType.unchecked(type))));
	}
	
	@Override @Contract(pure=true)
	public <F> @NotNull Self field(Function<V, F> mapper, Class<?> type) {
		SelfImpl copy = copy();
		copy.backingFieldBuilder = BackingFieldBuilder.of(mapper, EntryType.unchecked(type));
		return copy.castSelf();
	}
	
	@Contract(pure=true)
	@Internal public <F> Self addField(BackingFieldBinding<V, F> binding) {
		SelfImpl copy = copy();
		copy.backingFieldBindings.add(binding);
		return copy.castSelf();
	}
	
	@Override @Contract(pure=true) public @NotNull Self restart() {
		return restart(true);
	}
	
	@Override @Contract(pure=true) public @NotNull Self restart(boolean requireRestart) {
		SelfImpl copy = copy();
		copy.requireRestart = requireRestart;
		return copy.castSelf();
	}
	
	@Override @Contract(pure=true) public @NotNull Self experimental() {
		return experimental(true);
	}
	
	@Override @Contract(pure=true) public @NotNull Self experimental(boolean experimental) {
		SelfImpl copy = copy();
		copy.experimental = experimental;
		return copy.castSelf();
	}
	
	@Override @Contract(pure=true) public @NotNull Self editable(Supplier<Boolean> editable) {
		SelfImpl copy = copy();
		copy.editableSupplier = h -> editable.get();
		return copy.castSelf();
	}
	
	@Override @Contract(pure=true) public @NotNull Self editable(Function<ConfigEntryHolder, Boolean> editable) {
		SelfImpl copy = copy();
		copy.editableSupplier = editable;
		return copy.castSelf();
	}
	
	@Override @Contract(pure=true) public @NotNull Self temp() {
		return temp(true);
	}
	
	@Override @Contract(pure=true) public @NotNull Self temp(boolean nonPersistent) {
		SelfImpl copy = copy();
		copy.nonPersistent = nonPersistent;
		if (!nonPersistent)
			copy.ignored = false;
		return copy.castSelf();
	}
	
	@Override @Contract(pure=true) public @NotNull Self ignored() {
		return ignored(true);
	}
	
	@Override @Contract(pure=true) public @NotNull Self ignored(boolean ignored) {
		SelfImpl copy = copy();
		copy.ignored = ignored;
		if (ignored) copy.nonPersistent = true;
		return copy.castSelf();
	}
	
	@Contract(pure=true)
	@Internal public SelfImpl translation(@Nullable String translation) {
		SelfImpl copy = copy();
		copy.translation = translation;
		return copy;
	}
	
	@Override @Contract(pure=true) public @NotNull Self withTags(EntryTag... tags) {
		SelfImpl copy = copy();
		copy.tags.addAll(Arrays.asList(tags));
		return copy.castSelf();
	}
	
	@Override @Contract(pure=true) public @NotNull Self withoutTags(EntryTag... tags) {
		SelfImpl copy = copy();
		Arrays.stream(tags).forEach(copy.tags::remove);
		return copy.castSelf();
	}
	
	@Internal @Contract(pure=true) public SelfImpl withDelegate(@Nullable ConfigEntryDelegate<V> delegate) {
		SelfImpl copy = copy();
		copy.delegate = delegate;
		return copy;
	}
	
	/**
	 * Subclasses should instead override
	 * {@link AbstractConfigEntryBuilder#buildEntry(ConfigEntryHolder, String)}
	 * in most cases<br>
	 * Overrides should call super
	 */
	@Contract(value="_, _ -> new", pure=true) @MustBeInvokedByOverriders
	protected Entry build(@NotNull ConfigEntryHolder parent, String name) {
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
		if (buildListener != null) buildListener.accept(e);
		if (delegate != null) e.setDelegate(delegate);
		return e;
	}
	
	/**
	 * Create a copy of this builder<br>
	 * Do not call directly, instead use {@link AbstractConfigEntryBuilder#copy()}<br>
	 * Subclasses should implement this method copying all of their fields.
	 */
	@Contract(value="_ -> new", pure=true) protected abstract SelfImpl createCopy(V value);
	
	/**
	 * Creates a copy of this builder
	 */
	@Contract(value="-> new", pure=true) @MustBeInvokedByOverriders
	@Internal public SelfImpl copy() {
		return copy(value);
	}
	
	/**
	 * Creates a copy of this builder
	 */
	@Contract(value="_ -> new", pure=true) @MustBeInvokedByOverriders
	@Internal public SelfImpl copy(V value) {
		final SelfImpl copy = createCopy(value);
		copy.value = value;
		copy.translation = translation;
		copy.buildListener = buildListener;
		copy.errorSupplier = errorSupplier;
		copy.tooltipSupplier = tooltipSupplier;
		copy.nameArgs = new ArrayList<>(nameArgs);
		copy.tooltipArgs = new ArrayList<>(tooltipArgs);
		copy.requireRestart = requireRestart;
		copy.experimental = experimental;
		copy.typeClass = typeClass;
		copy.type = type;
		copy.editableSupplier = editableSupplier;
		copy.nonPersistent = nonPersistent;
		copy.ignored = ignored;
		copy.backingFieldBuilder = backingFieldBuilder;
		copy.backingFieldBindings = new ArrayList<>(backingFieldBindings);
		copy.tags = new HashSet<>(tags);
		copy.delegate = delegate;
		return copy;
	}
}
