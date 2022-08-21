package endorh.simpleconfig.core;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.EntryTag;
import endorh.simpleconfig.api.SimpleConfig;
import endorh.simpleconfig.api.SimpleConfig.ConfigReflectiveOperationException;
import endorh.simpleconfig.api.SimpleConfig.InvalidConfigValueException;
import endorh.simpleconfig.api.SimpleConfig.InvalidConfigValueTypeException;
import endorh.simpleconfig.api.SimpleConfig.NoSuchConfigEntryError;
import endorh.simpleconfig.config.ClientConfig;
import endorh.simpleconfig.core.SimpleConfigImpl.IGUIEntry;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigCategoryBuilder;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.gui.AbstractConfigScreen;
import endorh.simpleconfig.ui.impl.builders.CaptionedSubCategoryBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import endorh.simpleconfig.yaml.NodeComments;
import endorh.simpleconfig.yaml.SimpleConfigCommentedYamlFormat;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.comments.CommentLine;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.*;
import java.util.function.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static endorh.simpleconfig.api.SimpleConfigTextUtil.splitTtc;
import static endorh.simpleconfig.api.SimpleConfigTextUtil.stripFormattingCodes;
import static endorh.simpleconfig.yaml.SimpleConfigCommentedYamlWriter.commentLine;

/**
 * An abstract config entry, which may or may not produce an entry in
 * the actual config and/or the config GUI<br>
 * Subclasses may override {@link AbstractConfigEntry#buildConfigEntry}
 * and {@link AbstractConfigEntry#buildGUIEntry} to generate the appropriate
 * entries in both ends
 *
 * @param <V> The type of the value held by the entry
 * @param <Config> The type of the associated config entry
 * @param <Gui> The type of the associated GUI config entry
 * @see AbstractConfigEntryBuilder
 */
public abstract class AbstractConfigEntry<V, Config, Gui> implements IGUIEntry {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Pattern LINE_BREAK = Pattern.compile("\\R");
	private static final ITextComponent[] EMPTY_TEXT_ARRAY = new ITextComponent[0];
	
	public final V defValue;
	protected final ConfigEntryHolder parent;
	protected String name;
	protected Class<?> typeClass;
	protected @Nullable String translation = null;
	protected @Nullable String tooltip = null;
	protected boolean requireRestart = false;
	protected boolean experimental;
	protected @Nullable BiFunction<AbstractConfigEntry<V, Config, Gui>, Gui, Optional<ITextComponent>> errorSupplier = null;
	protected @Nullable BiFunction<AbstractConfigEntry<V, Config, Gui>, Gui, List<ITextComponent>> tooltipSupplier = null;
	protected @Nullable BiFunction<AbstractConfigEntry<V, Config, Gui>, Gui, List<ITextComponent>> warningSupplier = null;
	protected @Nullable BiConsumer<Gui, ConfigEntryHolder> saver = null;
	protected @Nullable Function<ConfigEntryHolder, Boolean> editableSupplier = null;
	protected @Nullable BackingField<V, ?> backingField;
	protected @Nullable List<BackingField<V, ?>> secondaryBackingFields;
	protected boolean dirty = false;
	protected @Nullable ITextComponent displayName = null;
	protected List<Object> nameArgs = new ArrayList<>();
	protected List<Object> tooltipArgs = new ArrayList<>();
	@OnlyIn(Dist.CLIENT) private @Nullable AbstractConfigListEntry<Gui> guiEntry;
	@OnlyIn(Dist.CLIENT) private @Nullable AbstractConfigListEntry<Gui> remoteGuiEntry;
	protected boolean nonPersistent = false;
	protected final Set<EntryTag> tags = new HashSet<>();
	protected final Set<EntryTag> builtInTags = new HashSet<>();
	protected final Set<EntryTag> allTags = Sets.union(tags, builtInTags);
	@Internal public EntryTag copyTag;
	protected V actualValue = null;
	protected @Nullable ConfigValue<?> configValue = null;
	protected boolean ignored = false;
	
	protected AbstractConfigEntry(
	  ConfigEntryHolder parent, String name, V defValue
	) {
		this.parent = parent;
		this.defValue = defValue;
		this.name = name;
	}
	
	@Internal public ConfigEntryHolder getParent() {
		return parent;
	}
	
	@Internal public SimpleConfig getRoot() {
		return getParent().getRoot();
	}
	
	@OnlyIn(Dist.CLIENT)
	protected static void addTranslationsDebugSuffix(List<ITextComponent> tooltip) {
		tooltip.add(new StringTextComponent(" "));
		tooltip.add(new StringTextComponent(" ⚠ Simple Config translation debug mode active").withStyle(TextFormatting.GOLD));
	}
	
	public String getPath() {
		if (parent instanceof SimpleConfigImpl) {
			return name;
		} else if (parent instanceof AbstractSimpleConfigEntryHolder) {
			return ((AbstractSimpleConfigEntryHolder) parent).getPathPart() + name;
		} else return name;
	}
	
	public String getGlobalPath() {
		if (parent instanceof AbstractSimpleConfigEntryHolder) {
			return ((AbstractSimpleConfigEntryHolder) parent).getGlobalPath() + "." + name;
		} else return name;
	}
	
	@Internal public @Nullable String getTranslation() {
		return translation;
	}
	@Internal public void setTranslation(@Nullable String translation) {
		this.translation = translation;
	}
	
	@Internal public @Nullable String getTooltipKey() {
		return tooltip;
	}
	@Internal public void setTooltipKey(@Nullable String translation) {
		tooltip = translation;
	}
	
	@Internal public String getName() {
		return name;
	}
	@Internal public void setName(String name) {
		this.name = name;
	}
	
	@OnlyIn(Dist.CLIENT)
	protected String fillArgs(String translation, V value, List<Object> args) {
		return I18n.get(translation, formatArgs(value, args));
	}
	
	protected Object[] formatArgs(V value, List<Object> args) {
		return args.stream().map(a -> {
			if (a instanceof Supplier) {
				try {
					return ((Supplier<?>) a).get();
				} catch (RuntimeException e){
					return new StringTextComponent("<null>").withStyle(TextFormatting.RED);
				}
			} else return a;
		}).toArray();
	}
	
	@Internal public void setSaver(BiConsumer<Gui, ConfigEntryHolder> saver) {
		this.saver = saver;
	}
	
	@Internal public void setDisplayName(ITextComponent name) {
		displayName = name;
	}
	
	private <T> void toggle(Set<T> set, T value, boolean include) {
		if (set.contains(value) != include) {
			if (include) set.add(value);
			else set.remove(value);
		}
	}
	
	@Internal protected Set<EntryTag> getTags() {
		toggle(builtInTags, EntryTag.REQUIRES_RESTART, requireRestart);
		toggle(builtInTags, EntryTag.EXPERIMENTAL, experimental);
		toggle(builtInTags, EntryTag.NON_PERSISTENT, nonPersistent);
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			String path = getPath();
			builtInTags.remove(copyTag);
			List<ITextComponent> tooltip = splitTtc("simpleconfig.config.tag.copy_path", path)
			  .stream().map(l -> l.copy().withStyle(TextFormatting.GRAY))
			  .collect(Collectors.toList());
			builtInTags.add(copyTag = EntryTag.copyTag(-1000, path, () -> tooltip));
		});
		return allTags;
	}
	
	protected boolean debugTranslations() {
		return ClientConfig.advanced.translation_debug_mode;
	}
	
	@OnlyIn(Dist.CLIENT)
	protected ITextComponent getDisplayName() {
		if (displayName != null)
			return displayName;
		if (debugTranslations())
			return getDebugDisplayName();
		if (translation != null && I18n.exists(translation))
			return new TranslationTextComponent(translation, formatArgs(null, nameArgs));
		return new StringTextComponent(name);
	}
	
	@OnlyIn(Dist.CLIENT)
	protected ITextComponent getDebugDisplayName() {
		if (translation != null) {
			IFormattableTextComponent status =
			  I18n.exists(translation) ? new StringTextComponent("✔ ") : new StringTextComponent("✘ ");
			if (tooltip != null) {
				status = status.append(
				  I18n.exists(tooltip)
				  ? new StringTextComponent("✔ ").withStyle(TextFormatting.DARK_AQUA)
				  : new StringTextComponent("_ ").withStyle(TextFormatting.DARK_AQUA));
			}
			TextFormatting format =
			  I18n.exists(translation)? TextFormatting.DARK_GREEN : TextFormatting.RED;
			return new StringTextComponent("").append(status.append(new StringTextComponent(translation)).withStyle(format));
		} else return new StringTextComponent("").append(new StringTextComponent("⚠ " + name).withStyle(TextFormatting.DARK_RED));
	}
	
	/**
	 * Transform value to Gui type
	 */
	public Gui forGui(V value) {
		//noinspection unchecked
		return (Gui) value;
	}
	
	/**
	 * Transform value from Gui type
	 */
	@Nullable public V fromGui(@Nullable Gui value) {
		//noinspection unchecked
		return (V) value;
	}
	
	/**
	 * Transform value from Gui type
	 */
	public V fromGuiOrDefault(Gui value) {
		final V v = fromGui(value);
		return v != null ? v : defValue;
	}
	
	/**
	 * Transform value to Config type
	 */
	public Config forConfig(V value) {
		//noinspection unchecked
		return (Config) value;
	}
	
	/**
	 * Transform value from Config type
	 */
	@Nullable public V fromConfig(@Nullable Config value) {
		//noinspection unchecked
		return typeClass.isInstance(value)? (V) value : null;
	}
	
	/**
	 * Transform value from Config type
	 */
	public V fromConfigOrDefault(Config value) {
		try {
			final V v = fromConfig(value);
			return v != null ? v : defValue;
		} catch (ClassCastException ignored) {
			return defValue;
		}
	}
	
	protected void put(CommentedConfig config, Config value) {
		config.set(name, forActualConfig(value));
	}
	
	protected Config get(CommentedConfig config) {
		return fromActualConfig(config.getOrElse(name, forActualConfig(forConfig(defValue))));
	}
	
	/**
	 * Last conversion step before being stored in the config
	 */
	public Object forActualConfig(@Nullable Config value) {
		return value;
	}
	
	/**
	 * First conversion step before reading from the config
	 */
	public @Nullable Config fromActualConfig(@Nullable Object value) {
		try {
			//noinspection unchecked
			return (Config) value;
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(getGlobalPath(), e);
		}
	}
	
	/**
	 * Subclasses may override to prevent nesting at build time
	 */
	protected boolean canBeNested() {
		return !nonPersistent;
	}
	
	protected void dirty() {
		dirty(true);
	}
	
	protected void dirty(boolean dirty) {
		this.dirty = dirty;
		if (dirty) parent.markDirty();
	}

	protected Consumer<Gui> createSaveConsumer() {
		if (saver != null)
			return g -> saver.accept(g, parent);
		if (ignored) return g -> {};
		return g -> {
			final V v = fromGuiOrDefault(g);
			if (!Objects.equals(get(), v)) {
				if (trySet(v)) dirty();
				else LOGGER.error("Unexpected error saving config entry \"" + getGlobalPath() + "\"");
			}
			// setGuiEntry(null); // Discard the entry
		};
	}
	
	@OnlyIn(Dist.CLIENT)
	protected Optional<ITextComponent[]> getTooltip(Gui value) {
		if (debugTranslations())
			return supplyDebugTooltip(value);
		List<ITextComponent> l;
		if (tooltipSupplier != null) {
			l = tooltipSupplier.apply(this, value);
			if (!l.isEmpty()) return Optional.of(addExtraTooltip(l.toArray(EMPTY_TEXT_ARRAY), value));
		}
		final V v = fromGui(value);
		if (tooltip != null && I18n.exists(tooltip)) {
			return Optional.of(splitTtc(tooltip, formatArgs(v, tooltipArgs)).toArray(EMPTY_TEXT_ARRAY))
			  .map(t -> addExtraTooltip(t, value));
		}
		final List<ITextComponent> extra = addExtraTooltip(value);
		return extra.isEmpty()? Optional.empty() : Optional.of(extra.toArray(EMPTY_TEXT_ARRAY));
	}
	
	protected ITextComponent[] addExtraTooltip(ITextComponent[] tooltip, Gui value) {
		return ArrayUtils.addAll(tooltip, addExtraTooltip(value).toArray(new ITextComponent[0]));
	}
	
	protected List<ITextComponent> addExtraTooltip(Gui value) {
		return Lists.newArrayList();
	}
	
	public List<ITextComponent> getErrorsFromGUI(Gui value) {
		return Stream.of(getErrorFromGUI(value))
		  .filter(Optional::isPresent).map(Optional::get)
		  .collect(Collectors.toList());
	}
	
	public Optional<ITextComponent> getErrorFromGUI(Gui value) {
		Optional<ITextComponent> o;
		if (errorSupplier != null) {
			o = errorSupplier.apply(this, value);
			if (o.isPresent()) return o;
		}
		return Optional.empty();
	}
	
	public Optional<ITextComponent> getError(@NotNull V value) {
		return getErrorFromGUI(forGui(value));
	}
	
	public Optional<ITextComponent> getErrorFromCommand(String command) {
		try {
			V value = fromCommand(command);
			if (value == null) return Optional.of(new TranslationTextComponent(
			  "simpleconfig.config.error.invalid_value_generic", command));
			return getErrorsFromGUI(forGui(value)).stream().findFirst();
		} catch (InvalidConfigValueTypeException e) {
			return Optional.empty();
		} catch (InvalidConfigValueException e) {
			return Optional.of(new TranslationTextComponent(
			  "simpleconfig.command.error.invalid_yaml", e.getLocalizedMessage()));
		}
	}
	
	protected @Nullable NodeComments getNodeComments(@Nullable NodeComments previous) {
		if (previous == null) previous = new NodeComments();
		List<CommentLine> blockComments = previous.getBlockComments();
		String configComment = getConfigComment();
		if (configComment.endsWith("\n")) configComment = configComment.substring(0, configComment.length() - 1);
		if (blockComments == null) blockComments = Lists.newArrayList();
		// Remove doc comments (starting with ##)
		blockComments.removeIf(l -> l.getValue().startsWith("#"));
		if (!configComment.isEmpty()) Arrays.stream(LINE_BREAK.split(configComment))
		  .map(line -> commentLine("# " + line))
		  .forEach(blockComments::add);
		// FIXME: Remove once snakeyaml is updated to 1.31 (see bitbucket.org/snakeyaml/snakeyaml/issues/518)
		if (blockComments.size() > 90) {
			LOGGER.warn("Entry " + getGlobalPath() + " has too many comments [BUG]. Trimmed to the last 90");
			blockComments.subList(0, blockComments.size() - 90).clear();
		}
		if (blockComments.isEmpty()) blockComments = null;
		previous.setBlockComments(blockComments);
		return previous.isNotEmpty()? previous : null;
	}
	
	@Internal public List<String> getConfigCommentTooltips() {
		List<String> tooltips = Lists.newArrayList();
		getTags().stream().map(EntryTag::getComment).filter(Objects::nonNull).forEach(tooltips::add);
		return tooltips;
	}
	
	@Internal public String getConfigCommentTooltip() {
		return getConfigCommentTooltips().stream()
		  .map(t -> "[" + t + "]")
		  .collect(Collectors.joining(" "));
	}
	
	@Internal public String getConfigComment() {
		StringBuilder builder = new StringBuilder();
		if (translation != null && ServerI18n.hasKey(translation)) {
			String name = stripFormattingCodes(ServerI18n.format(
			  translation, formatArgs(null, nameArgs)
			).trim());
			builder.append(name).append('\n');
			if (tooltip != null && ServerI18n.hasKey(tooltip)) {
				String tooltip = "  " + stripFormattingCodes(ServerI18n.format(
				  this.tooltip, formatArgs(get(), tooltipArgs)
				).trim().replace("\n", "\n  "));
				builder.append(tooltip).append('\n');
			}
		}
		builder.append(getConfigCommentTooltip()).append('\n');
		return builder.toString();
	}
	
	@MustBeInvokedByOverriders
	protected ForgeConfigSpec.Builder decorate(ForgeConfigSpec.Builder builder) {
		// Forge's comment change detection is too buggy to use and runs before
		// I18n entries have been loaded, so it can't use the entries' descriptions.
		// Instead, we patch the comments on write, and update them once I18n has been loaded.
		// Comments starting with "##" are removed and replaced with the correct ones.
		// Other comments are treated as user comments and preserved.
		// builder = builder.comment(comment);
		return builder;
	}
	
	@OnlyIn(Dist.CLIENT) @MustBeInvokedByOverriders
	protected <F extends FieldBuilder<Gui, ?, F>> F decorate(F builder) {
		builder.requireRestart(requireRestart)
		  .nonPersistent(nonPersistent)
		  .setDefaultValue(() -> forGui(defValue))
		  .setTooltipSupplier(this::getTooltip)
		  .setErrorSupplier(this::getErrorFromGUI)
		  .withSaveConsumer(createSaveConsumer())
		  .setEditableSupplier(() -> editableSupplier == null || editableSupplier.apply(parent))
		  .withTags(getTags())
		  .setName(name)
		  .setIgnoreEdits(ignored);
		return builder;
	}
	
	public boolean isValidValue(V value) {
		if (value == null) return false;
		return getErrorsFromGUI(forGui(value)).isEmpty();
	}
	
	protected Predicate<Object> createConfigValidator() {
		return o -> {
			try {
				final Config c = fromActualConfig(o);
				final V v = fromConfig(c);
				return v != null && isValidValue(v);
			} catch (ClassCastException e) {
				return false;
			}
		};
	}
	
	/**
	 * Build the associated config entry for this entry, if any
	 */
	protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.of(decorate(builder).define(name, forActualConfig(forConfig(defValue)), createConfigValidator()));
	}
	
	/**
	 * Build the config for this entry<br>
	 * Subclasses should instead override {@link AbstractConfigEntry#buildConfigEntry(Builder)}
	 * in most cases
	 */
	protected void buildConfig(
	  ForgeConfigSpec.Builder builder
	) {
		if (!nonPersistent)
			buildConfigEntry(builder).ifPresent(this::setConfigValue);
	}
	
	/**
	 * Build a config spec
	 *
	 * @param spec       The built config spec
	 * @param parentPath The path of the parents of this entry, including
	 *                   a final dot.
	 */
	protected void buildSpec(
	  ConfigSpec spec, String parentPath
	) {
		spec.define(parentPath + name, forConfig(defValue), createConfigValidator());
	}
	
	/**
	 * Generate an {@link AbstractConfigListEntry} to be added to the GUI, if any
	 *
	 * @param builder Entry builder
	 */
	@OnlyIn(Dist.CLIENT) public Optional<FieldBuilder<Gui, ?, ?>> buildGUIEntry(
	  ConfigFieldBuilder builder
	) {
		return Optional.empty();
	}
	
	public void decorateGUIBuilder(FieldBuilder<Gui, ?, ?> builder, boolean forRemote) {
		if (forRemote) builder.withSaveConsumer(g -> {});
		builder.withBuildListener(forRemote? this::setRemoteGuiEntry : this::setGuiEntry);
	}
	
	/**
	 * Add entry to the GUI<br>
	 * Subclasses should instead override {@link AbstractConfigEntry#buildGUIEntry(ConfigFieldBuilder)}
	 */
	@OnlyIn(Dist.CLIENT) public void buildGUI(
	  CaptionedSubCategoryBuilder<?, ?, ?> group, ConfigFieldBuilder entryBuilder, boolean forRemote
	) {
		buildGUIEntry(entryBuilder).ifPresent(b -> {
			decorateGUIBuilder(b, forRemote);
			group.add(b);
		});
	}
	
	/**
	 * Add entry to the GUI<br>
	 * Subclasses should instead override {@link AbstractConfigEntry#buildGUIEntry} in most cases
	 */
	@OnlyIn(Dist.CLIENT)
	@Override @Internal public void buildGUI(
	  ConfigCategoryBuilder category, ConfigFieldBuilder entryBuilder, boolean forRemote
	) {
		buildGUIEntry(entryBuilder).ifPresent(b -> {
			decorateGUIBuilder(b, forRemote);
			category.addEntry(b);
		});
	}
	
	@Internal protected @Nullable AbstractConfigListEntry<Gui> getGuiEntry() {
		if (remoteGuiEntry != null) {
			AbstractConfigScreen screen = (AbstractConfigScreen) parent.getRoot().getGUI();
			if (screen != null && screen.isEditingServer()) return remoteGuiEntry;
		}
		return guiEntry;
	}
	
	@Internal protected @Nullable AbstractConfigListEntry<Gui> getGuiEntry(boolean remote) {
		return remote ? remoteGuiEntry : guiEntry;
	}
	
	@Internal protected void setGuiEntry(@Nullable AbstractConfigListEntry<Gui> guiEntry) {
		this.guiEntry = guiEntry;
	}
	
	@Internal protected void setRemoteGuiEntry(@Nullable AbstractConfigListEntry<Gui> guiEntry) {
		remoteGuiEntry = guiEntry;
	}
	
	@Internal protected void resetGuiEntry() {
		resetGuiEntry(false);
	}
	
	@Internal protected void resetGuiEntry(boolean remote) {
		AbstractConfigListEntry<Gui> entry = getGuiEntry(remote);
		if (entry != null) entry.resetValue();
	}
	
	@Internal protected void restoreGuiEntry() {
		restoreGuiEntry(false);
	}
	
	@Internal protected void restoreGuiEntry(boolean remote) {
		AbstractConfigListEntry<Gui> entry = getGuiEntry(remote);
		if (entry != null) entry.restoreValue();
	}
	
	protected void removeGUI() {
		guiEntry = null;
		remoteGuiEntry = null;
	}
	
	@Internal public void setConfigValue(@Nullable ConfigValue<?> value) {
		configValue = value;
	}
	
	/**
	 * Convenience method to capture generics.
	 */
	@Internal public <T> T apply(Function<? super AbstractConfigEntry<V, Config, Gui>, T> f) {
		return f.apply(this);
	}
	
	/**
	 * Convenience method to capture generics.
	 */
	@Internal public void accept(Consumer<? super AbstractConfigEntry<V, Config, Gui>> c) {
		c.accept(this);
	}
	
	@Internal public V get() {
		if (nonPersistent) return actualValue;
		if (configValue == null) throw new NoSuchConfigEntryError(getGlobalPath());
		return get(configValue);
	}
	
	@Internal public void set(V value) {
		if (!isValidValue(value))
			throw new InvalidConfigValueException(getGlobalPath(), value);
		if (nonPersistent) {
			actualValue = value;
		} else if (configValue == null) {
			throw new NoSuchConfigEntryError(getGlobalPath());
		} else {
			set(configValue, value);
		}
		if (hasGUI()) setGUIAsExternal(forGui(value), false);
	}
	
	@Internal public boolean trySet(V value) {
		if (isValidValue(value)) {
			if (nonPersistent) {
				actualValue = value;
			} else if (configValue == null) {
				throw new NoSuchConfigEntryError(getGlobalPath());
			} else set(configValue, value);
			return true;
		} else return false;
	}
	
	/**
	 * Get the value held by this entry
	 *
	 * @param spec Config spec to look into
	 * @throws ClassCastException if the found value type does not match the expected
	 */
	protected V get(ConfigValue<?> spec) {
		return fromConfigOrDefault(fromActualConfig(spec.get()));
	}

	/**
	 * Set the value held by this entry
	 *
	 * @param spec Config spec to update
	 * @throws ClassCastException if the found value type does not match the expected
	 */
	protected void set(ConfigValue<?> spec, V value) {
		//noinspection unchecked
		((ConfigValue<Object>) spec).set(forActualConfig(forConfig(value)));
		bakeField();
	}

	protected boolean hasGUI() {
		return hasGUI(false);
	}
	
	protected boolean hasGUI(boolean remote) {
		if (FMLEnvironment.dist != Dist.CLIENT) return false;
		return getGuiEntry(remote) != null;
	}

	protected Gui getGUI() {
		return getGUI(false);
	}
	
	protected Gui getGUI(boolean remote) {
		if (FMLEnvironment.dist != Dist.CLIENT) return forGui(get());
		AbstractConfigListEntry<Gui> guiEntry = getGuiEntry(remote);
		return guiEntry != null? guiEntry.getValue() : forGui(get());
	}

	@OnlyIn(Dist.CLIENT) protected void setGUI(Gui value) {
		setGUI(value, false);
	}
	
	@OnlyIn(Dist.CLIENT) protected void setGUI(Gui value, boolean remote) {
		AbstractConfigListEntry<Gui> guiEntry = getGuiEntry(remote);
		if (guiEntry != null) {
			guiEntry.setValueTransparently(value);
		} else throw new IllegalStateException("Cannot set GUI value without GUI");
	}
	
	@OnlyIn(Dist.CLIENT) protected void setGUIAsExternal(Gui value, boolean forRemote) {
		AbstractConfigListEntry<Gui> guiEntry = getGuiEntry(forRemote);
		if (guiEntry != null) {
			guiEntry.setExternalValue(value);
		} else throw new IllegalStateException("Cannot set GUI value for " + getGlobalPath() + " without GUI");
	}
	
	@Internal public @Nullable String forCommand(V value) {
		Yaml yaml = SimpleConfigCommentedYamlFormat.getDefaultYaml();
		try {
			return yaml.dumpAs(forActualConfig(forConfig(value)), null, FlowStyle.FLOW).trim();
		} catch (YAMLException e) {
			return null;
		}
	}
	
	@Internal public V fromCommand(String value) {
		Yaml yaml = SimpleConfigCommentedYamlFormat.getDefaultYaml();
		try {
			return fromConfig(fromActualConfig(yaml.load(value)));
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(getGlobalPath(), e);
		} catch (YAMLException e) {
			throw new InvalidConfigValueException(getGlobalPath(), e);
		}
	}
	
	@Internal public String getForCommand() {
		return forCommand(get());
	}
	
	@Internal public void setFromCommand(String value) {
		set(fromCommand(value));
	}
	
	protected void setBackingField(V value) throws IllegalAccessException {
		if (backingField != null)
			backingField.setValue(value);
		if (secondaryBackingFields != null) {
			for (BackingField<V, ?> field : secondaryBackingFields)
				field.setValue(value);
		}
	}
	
	protected V getFromBackingField() throws IllegalAccessException {
		if (backingField == null)
			throw new IllegalStateException("Missing backing field for entry " + getGlobalPath());
		if (!backingField.canBeRead())
			throw new IllegalStateException("Backing field for entry " + getGlobalPath() + " is not readable");
		try {
			return backingField.readValue();
		} catch (InvalidConfigValueTypeException e) {
			throw new InvalidConfigValueTypeException(getGlobalPath(), e);
		}
	}
	
	protected void commitField() throws IllegalAccessException {
		if (backingField != null && backingField.canBeRead())
			set(getFromBackingField());
	}
	
	protected void bakeField() {
		if (backingField != null || secondaryBackingFields != null && !secondaryBackingFields.isEmpty()) {
			try {
				setBackingField(get());
			} catch (IllegalAccessException e) {
				throw new ConfigReflectiveOperationException(
				  "Could not access mod config field during config bake\n  Details: " + e.getMessage(), e);
			}
		}
	}

	/**
	 * Overrides should call super
	 */
	@OnlyIn(Dist.CLIENT)
	protected void addTranslationsDebugInfo(List<ITextComponent> tooltip) {
		if (tooltipSupplier != null)
			tooltip.add(new StringTextComponent(" + Has tooltip supplier").withStyle(TextFormatting.GRAY));
		if (errorSupplier != null)
			tooltip.add(new StringTextComponent(" + Has error supplier").withStyle(TextFormatting.GRAY));
	}

	@OnlyIn(Dist.CLIENT)
	protected Optional<ITextComponent[]> supplyDebugTooltip(Gui value) {
		List<ITextComponent> lines = new ArrayList<>();
		lines.add(new StringTextComponent("Translation key:").withStyle(TextFormatting.GRAY));
		if (translation != null) {
			final IFormattableTextComponent status =
			  I18n.exists(translation)
			  ? new StringTextComponent("(✔ present)").withStyle(TextFormatting.DARK_GREEN)
			  : new StringTextComponent("(✘ missing)").withStyle(TextFormatting.RED);
			lines.add(new StringTextComponent("   " + translation + " ")
			            .withStyle(TextFormatting.DARK_AQUA).append(status));
		} else lines.add(new StringTextComponent("   Error: couldn't map translation key").withStyle(TextFormatting.RED));
		lines.add(new StringTextComponent("Tooltip key:").withStyle(TextFormatting.GRAY));
		if (tooltip != null) {
			final IFormattableTextComponent status =
			  I18n.exists(tooltip)
			  ? new StringTextComponent("(✔ present)").withStyle(TextFormatting.DARK_GREEN)
			  : new StringTextComponent("(not present)").withStyle(TextFormatting.GOLD);
			lines.add(new StringTextComponent("   " + tooltip + " ")
			            .withStyle(TextFormatting.DARK_AQUA).append(status));
		} else lines.add(new StringTextComponent("   Error: couldn't map tooltip translation key").withStyle(TextFormatting.RED));
		addTranslationsDebugInfo(lines);
		addTranslationsDebugSuffix(lines);
		return Optional.of(lines.toArray(new ITextComponent[0]));
	}
}
