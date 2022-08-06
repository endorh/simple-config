package endorh.simpleconfig.core;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.google.common.collect.Lists;
import endorh.simpleconfig.SimpleConfigMod.ClientConfig;
import endorh.simpleconfig.core.SimpleConfig.*;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigCategory;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
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
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;
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

import static endorh.simpleconfig.core.SimpleConfigTextUtil.splitTtc;
import static endorh.simpleconfig.core.SimpleConfigTextUtil.stripFormattingCodes;
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
 * @param <Self> The actual subtype of this entry to be
 *              returned by builder-like methods
 * @see AbstractConfigEntryBuilder
 */
public abstract class AbstractConfigEntry<V, Config, Gui, Self extends AbstractConfigEntry<V, Config, Gui, Self>>
  implements IGUIEntry {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Pattern LINE_BREAK = Pattern.compile("\\R");
	protected final ISimpleConfigEntryHolder parent;
	/**
	 * The default value of this entry
	 */
	public final V defValue;
	protected String name;
	protected Class<?> typeClass;
	protected @Nullable String translation = null;
	protected @Nullable String tooltip = null;
	protected boolean requireRestart = false;
	protected @Nullable Function<Config, Optional<ITextComponent>> configErrorSupplier = null;
	protected @Nullable Function<V, Optional<ITextComponent>> errorSupplier = null;
	protected @Nullable Function<Gui, Optional<ITextComponent>> guiErrorSupplier = null;
	protected @Nullable Function<V, List<ITextComponent>> tooltipSupplier = null;
	protected @Nullable Function<Gui, List<ITextComponent>> guiTooltipSupplier = null;
	protected @Nullable BiConsumer<Gui, ISimpleConfigEntryHolder> saver = null;
	/**
	 * Returning false makes the entry not editable in the GUI<br>
	 * Note that users may still be able to modify these entries from the
	 * config file. This feature is only meant to visually express that
	 * an entry's value is currently irrelevant, perhaps due to other
	 * entries' values, but don't overuse it to the point where editing
	 * the config becomes frustrating.
	 */
	protected @Nullable Supplier<Boolean> editableSupplier = null;
	protected @Nullable BackingField<V, ?> backingField;
	protected @Nullable List<BackingField<V, ?>> secondaryBackingFields;
	protected boolean dirty = false;
	protected @Nullable ITextComponent displayName = null;
	protected List<Object> nameArgs = new ArrayList<>();
	protected List<Object> tooltipArgs = new ArrayList<>();
	protected @OnlyIn(Dist.CLIENT) @Nullable AbstractConfigListEntry<Gui> guiEntry;
	protected @OnlyIn(Dist.CLIENT) @Nullable AbstractConfigListEntry<Gui> remoteGuiEntry;
	protected boolean nonPersistent = false;
	protected V actualValue = null;
	protected @Nullable ConfigValue<?> configValue = null;
	protected boolean ignored = false;
	
	protected AbstractConfigEntry(
	  ISimpleConfigEntryHolder parent, String name, V defValue
	) {
		this.parent = parent;
		this.defValue = defValue;
		this.name = name;
	}
	
	public String getPath() {
		if (parent instanceof SimpleConfig) {
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
	
	@SuppressWarnings("unchecked") protected Self self() {
		return (Self) this;
	}
	
	protected void setTranslation(@Nullable String translation) {
		this.translation = translation;
	}
	
	protected @Nullable String getTranslation() {
		return translation;
	}
	
	protected void setTooltipKey(@Nullable String translation) {
		tooltip = translation;
	}
	
	protected @Nullable String getTooltipKey() {
		return tooltip;
	}
	
	@OnlyIn(Dist.CLIENT)
	protected String fillArgs(String translation, V value, List<Object> args) {
		return I18n.format(translation, formatArgs(value, args));
	}
	
	protected Object[] formatArgs(V value, List<Object> args) {
		return args.stream().map(a -> {
			if (a instanceof Function) {
				try {
					//noinspection unchecked
					return ((Function<V, ?>) a).apply(value);
				} catch (ClassCastException e) {
					throw new InvalidConfigValueTypeException(
					  getGlobalPath(), e, "A translation argument provider expected an invalid value type");
				}
			} else if (a instanceof Supplier) {
				return ((Supplier<?>) a).get();
			} else return a;
		}).toArray();
	}
	
	@Internal public Self withSaver(BiConsumer<Gui, ISimpleConfigEntryHolder> saver) {
		this.saver = saver;
		return self();
	}
	
	@Internal public Self withDisplayName(ITextComponent name) {
		displayName = name;
		return self();
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
		if (translation != null && I18n.hasKey(translation))
			return new TranslationTextComponent(translation, formatArgs(null, nameArgs));
		return new StringTextComponent(name);
	}
	
	@OnlyIn(Dist.CLIENT)
	protected ITextComponent getDebugDisplayName() {
		if (translation != null) {
			IFormattableTextComponent status =
			  I18n.hasKey(translation) ? new StringTextComponent("✔ ") : new StringTextComponent("✘ ");
			if (tooltip != null) {
				status = status.append(
				  I18n.hasKey(tooltip)
				  ? new StringTextComponent("✔ ").mergeStyle(TextFormatting.DARK_AQUA)
				  : new StringTextComponent("_ ").mergeStyle(TextFormatting.DARK_AQUA));
			}
			TextFormatting format =
			  I18n.hasKey(translation)? TextFormatting.DARK_GREEN : TextFormatting.RED;
			// status = status.append(new StringTextComponent("⧉").modifyStyle(s -> s
			//   .setFormatting(TextFormatting.WHITE)
			//   .setHoverEvent(new HoverEvent(
			// 	 HoverEvent.Action.SHOW_TEXT, new TranslationTextComponent(
			// 	 "simpleconfig.debug.copy").mergeStyle(TextFormatting.GRAY)))
			//   .setClickEvent(new ClickEvent(
			//     ClickEvent.Action.COPY_TO_CLIPBOARD, translation)))
			// ).appendString(" ");
			// if (tooltip != null)
			// 	status = status.append(new StringTextComponent("⧉").modifyStyle(s -> s
			// 	  .setFormatting(TextFormatting.GRAY)
			// 	  .setHoverEvent(new HoverEvent(
			// 		 HoverEvent.Action.SHOW_TEXT, new TranslationTextComponent(
			// 		 "simpleconfig.debug.copy.help").mergeStyle(TextFormatting.GRAY)))
			// 	  .setClickEvent(new ClickEvent(
			// 	    ClickEvent.Action.COPY_TO_CLIPBOARD, tooltip)))
			// 	).appendString(" ");
			return new StringTextComponent("").append(status.append(new StringTextComponent(translation)).mergeStyle(format));
		} else return new StringTextComponent("").append(new StringTextComponent("⚠ " + name).mergeStyle(TextFormatting.DARK_RED));
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
			// guiEntry = null; // Discard the entry
		};
	}
	
	private static final ITextComponent[] EMPTY_TEXT_ARRAY = new ITextComponent[0];
	@OnlyIn(Dist.CLIENT)
	protected Optional<ITextComponent[]> getTooltip(Gui value) {
		if (debugTranslations())
			return supplyDebugTooltip(value);
		List<ITextComponent> l;
		if (guiTooltipSupplier != null) {
			l = guiTooltipSupplier.apply(value);
			if (!l.isEmpty()) return Optional.of(addExtraTooltip(l.toArray(EMPTY_TEXT_ARRAY), value));
		}
		final V v = fromGui(value);
		if (tooltipSupplier != null) {
			if (v != null) {
				l = tooltipSupplier.apply(v);
				if (!l.isEmpty()) return Optional.of(addExtraTooltip(l.toArray(EMPTY_TEXT_ARRAY), value));
			}
		}
		if (tooltip != null && I18n.hasKey(tooltip)) {
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
		if (guiErrorSupplier != null) {
			o = guiErrorSupplier.apply(value);
			if (o.isPresent()) return o;
		}
		final V v = fromGui(value);
		if (v == null) return Optional.of(new TranslationTextComponent(
		  "simpleconfig.config.error.missing_value"));
		if (errorSupplier != null) {
			o = errorSupplier.apply(v);
			if (o.isPresent()) return o;
		}
		Config c = forConfig(v);
		if (configErrorSupplier != null) {
			o = configErrorSupplier.apply(c);
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
		if (!configComment.isEmpty()) {
			if (blockComments == null) blockComments = Lists.newArrayList();
			blockComments.removeIf(l -> l.getValue().startsWith("#"));
			Arrays.stream(LINE_BREAK.split(configComment))
			  .map(line -> commentLine("# " + line))
			  .forEach(blockComments::add);
			previous.setBlockComments(blockComments);
		}
		return previous.isNotEmpty()? previous : null;
	}
	
	@Internal public List<String> getConfigCommentTooltips() {
		ArrayList<String> tooltips = Lists.newArrayList();
		if (requireRestart) tooltips.add("Requires restart!");
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
	
	protected ForgeConfigSpec.Builder decorate(ForgeConfigSpec.Builder builder) {
		// Forge's comment change detection is too buggy to use and runs before
		// I18n entries have been loaded, so it can't use the entries' descriptions.
		// String comment = getConfigComment();
		// if (comment != null)
		// 	builder = builder.comment(comment);
		// if (tooltip != null)
		// 	builder = builder.translation(tooltip);
		return builder;
	}
	
	@OnlyIn(Dist.CLIENT)
	protected <F extends FieldBuilder<Gui, ?, F>> F decorate(F builder) {
		builder.requireRestart(requireRestart)
		  .nonPersistent(nonPersistent)
		  .setDefaultValue(() -> forGui(defValue))
		  .setTooltipSupplier(this::getTooltip)
		  .setErrorSupplier(this::getErrorFromGUI)
		  .setSaveConsumer(createSaveConsumer())
		  .setEditableSupplier(editableSupplier)
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
	 * @param builder Entry builder
	 */
	@OnlyIn(Dist.CLIENT)
	public Optional<AbstractConfigListEntry<Gui>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		return Optional.empty();
	}
	
	/**
	 * Add entry to the GUI<br>
	 * Subclasses should instead override {@link AbstractConfigEntry#buildGUIEntry(ConfigEntryBuilder)}
	 */
	@OnlyIn(Dist.CLIENT) public void buildGUI(
	  CaptionedSubCategoryBuilder<?, ?> group, ConfigEntryBuilder entryBuilder, boolean forHotKey
	) {
		buildGUIEntry(entryBuilder).ifPresent(e -> {
			if (!forHotKey) guiEntry = e;
			group.add(e);
		});
	}
	
	/**
	 * Add entry to the GUI<br>
	 * Subclasses should instead override {@link AbstractConfigEntry#buildGUIEntry} in most cases
	 */
	@OnlyIn(Dist.CLIENT)
	@Override @Internal public void buildGUI(
	  ConfigCategory category, ConfigEntryBuilder entryBuilder
	) {
		buildGUIEntry(entryBuilder).ifPresent(e -> {
			guiEntry = e;
			category.addEntry(e);
		});
	}
	
	@Internal public void setConfigValue(@Nullable ConfigValue<?> value) {
		configValue = value;
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
		if (FMLEnvironment.dist == Dist.CLIENT)
			if (guiEntry != null) guiEntry.setExternalValue(forGui(value));
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
		if (FMLEnvironment.dist != Dist.CLIENT) return false;
		return guiEntry != null;
	}
	protected Gui getGUI() {
		if (FMLEnvironment.dist != Dist.CLIENT) return forGui(get());
		return guiEntry != null? guiEntry.getValue() : forGui(get());
	}
	protected V getFromGUI() {
		if (FMLEnvironment.dist != Dist.CLIENT) return get();
		return guiEntry != null? fromGui(getGUI()) : get();
	}
	protected Config guiForConfig() {
		return forConfig(getFromGUI());
	}
	protected Config getForConfig() {
		return forConfig(get());
	}
	
	@OnlyIn(Dist.CLIENT) protected void setGUI(Gui value) {
		if (guiEntry != null) {
			guiEntry.setValueTransparently(value);
		} else throw new IllegalStateException("Cannot set GUI value without GUI");
	}
	@OnlyIn(Dist.CLIENT) protected void setGUIAsExternal(Gui value) {
		if (guiEntry != null) {
			guiEntry.setExternalValue(value);
		} else throw new IllegalStateException("Cannot set GUI value without GUI");
	}
	@OnlyIn(Dist.CLIENT) protected void setForGUI(V value) {
		setGUI(forGui(value));
	}
	@OnlyIn(Dist.CLIENT) protected void setForGUIAsExternal(V value) {
		setGUIAsExternal(forGui(value));
	}
	@OnlyIn(Dist.CLIENT) protected void setFromConfigForGUI(Config value) {
		setForGUI(fromConfigOrDefault(value));
	}
	@OnlyIn(Dist.CLIENT) protected void setFromConfigForGUIAsExternal(Config value) {
		setForGUIAsExternal(fromConfigOrDefault(value));
	}
	
	protected void setFromGUI(Gui value) {
		set(fromGui(value));
	}
	
	protected void trySetFromGUI(Gui value) {
		trySet(fromGui(value));
	}
	
	protected void setFromConfig(Config value) {
		set(fromConfigOrDefault(value));
	}
	
	protected void trySetFromConfig(Config value) {
		trySet(fromConfigOrDefault(value));
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
		if (backingField != null) {
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
		if (guiTooltipSupplier != null)
			tooltip.add(new StringTextComponent(" + Has GUI tooltip supplier").mergeStyle(TextFormatting.GRAY));
		if (tooltipSupplier != null)
			tooltip.add(new StringTextComponent(" + Has tooltip supplier").mergeStyle(TextFormatting.GRAY));
		if (guiErrorSupplier != null)
			tooltip.add(new StringTextComponent(" + Has GUI error supplier").mergeStyle(TextFormatting.GRAY));
		if (errorSupplier != null)
			tooltip.add(new StringTextComponent(" + Has error supplier").mergeStyle(TextFormatting.GRAY));
	}
	@OnlyIn(Dist.CLIENT)
	protected static void addTranslationsDebugSuffix(List<ITextComponent> tooltip) {
		tooltip.add(new StringTextComponent(" "));
		tooltip.add(new StringTextComponent(" ⚠ Simple Config translation debug mode active").mergeStyle(TextFormatting.GOLD));
	}
	@OnlyIn(Dist.CLIENT)
	protected Optional<ITextComponent[]> supplyDebugTooltip(Gui value) {
		List<ITextComponent> lines = new ArrayList<>();
		lines.add(new StringTextComponent("Translation key:").mergeStyle(TextFormatting.GRAY));
		if (translation != null) {
			final IFormattableTextComponent status =
			  I18n.hasKey(translation)
			  ? new StringTextComponent("(✔ present)").mergeStyle(TextFormatting.DARK_GREEN)
			  : new StringTextComponent("(✘ missing)").mergeStyle(TextFormatting.RED);
			lines.add(new StringTextComponent("   " + translation + " ")
			            .mergeStyle(TextFormatting.DARK_AQUA).append(status));
		} else lines.add(new StringTextComponent("   Error: couldn't map translation key").mergeStyle(TextFormatting.RED));
		lines.add(new StringTextComponent("Tooltip key:").mergeStyle(TextFormatting.GRAY));
		if (tooltip != null) {
			final IFormattableTextComponent status =
			  I18n.hasKey(tooltip)
			  ? new StringTextComponent("(✔ present)").mergeStyle(TextFormatting.DARK_GREEN)
			  : new StringTextComponent("(not present)").mergeStyle(TextFormatting.GOLD);
			lines.add(new StringTextComponent("   " + tooltip + " ")
			            .mergeStyle(TextFormatting.DARK_AQUA).append(status));
		} else lines.add(new StringTextComponent("   Error: couldn't map tooltip translation key").mergeStyle(TextFormatting.RED));
		addTranslationsDebugInfo(lines);
		addTranslationsDebugSuffix(lines);
		return Optional.of(lines.toArray(new ITextComponent[0]));
	}
}
