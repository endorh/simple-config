package endorh.simpleconfig.core;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.google.common.collect.Lists;
import endorh.simpleconfig.SimpleConfigMod.ClientConfig;
import endorh.simpleconfig.core.NBTUtil.ExpectedType;
import endorh.simpleconfig.core.SimpleConfig.*;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigCategory;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.impl.builders.CaptionedSubCategoryBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
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
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static endorh.simpleconfig.core.SimpleConfigTextUtil.splitTtc;

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
	protected final ISimpleConfigEntryHolder parent;
	/**
	 * The default value of this entry
	 */
	protected final V value;
	protected String name;
	protected Class<?> typeClass;
	protected @Nullable String translation = null;
	protected @Nullable String tooltip = null;
	protected @Nullable String comment = null;
	protected boolean requireRestart = false;
	protected @Nullable Function<V, Optional<ITextComponent>> errorSupplier = null;
	protected @Nullable Function<V, Optional<ITextComponent[]>> tooltipSupplier = null;
	protected @Nullable Function<Gui, Optional<ITextComponent>> guiErrorSupplier = null;
	protected @Nullable Function<Gui, Optional<ITextComponent[]>> guiTooltipSupplier = null;
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
	protected @Nullable Field backingField2;
	protected @Nullable List<BackingField<V, ?>> secondaryBackingFields;
	protected boolean dirty = false;
	protected @Nullable ITextComponent displayName = null;
	protected List<Object> nameArgs = new ArrayList<>();
	protected List<Object> tooltipArgs = new ArrayList<>();
	protected @OnlyIn(Dist.CLIENT) @Nullable AbstractConfigListEntry<Gui> guiEntry;
	protected boolean nonPersistent = false;
	protected V actualValue = null;
	protected @Nullable ConfigValue<?> configValue = null;
	protected boolean ignored = false;
	
	protected AbstractConfigEntry(
	  ISimpleConfigEntryHolder parent, String name, V value
	) {
		this.parent = parent;
		this.value = value;
		this.name = name;
	}
	
	public String getPath() {
		if (parent instanceof AbstractSimpleConfigEntryHolder) {
			return ((AbstractSimpleConfigEntryHolder) parent).getPath() + "." + name;
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
	
	protected void setTooltip(@Nullable String translation) {
		this.tooltip = translation;
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
		this.displayName = name;
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
		return v != null ? v : this.value;
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
		return (V) value;
	}
	
	/**
	 * Transform value from Config type
	 */
	public V fromConfigOrDefault(Config value) {
		final V v = fromConfig(value);
		return v != null ? v : this.value;
	}
	
	protected void put(CommentedConfig config, Config value) {
		config.set(name, forActualConfig(value));
	}
	
	protected Config get(CommentedConfig config) {
		return fromActualConfig(config.getOrElse(name, forActualConfig(forConfig(value))));
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
	protected @Nullable Config fromActualConfig(@Nullable Object value) {
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
	
	@OnlyIn(Dist.CLIENT)
	protected Optional<ITextComponent[]> getTooltip(Gui value) {
		if (debugTranslations())
			return supplyDebugTooltip(value);
		Optional<ITextComponent[]> o;
		if (guiTooltipSupplier != null) {
			o = guiTooltipSupplier.apply(value);
			if (o.isPresent()) return o.map(t -> addExtraTooltip(t, value));
		}
		final V v = fromGui(value);
		if (tooltipSupplier != null) {
			if (v != null) {
				o = tooltipSupplier.apply(v);
				if (o.isPresent()) return o.map(t -> addExtraTooltip(t, value));
			}
		}
		if (tooltip != null && I18n.hasKey(tooltip)) {
			return Optional.of(splitTtc(tooltip, formatArgs(v, tooltipArgs)).toArray(new ITextComponent[0]))
			  .map(t -> addExtraTooltip(t, value));
		}
		final List<ITextComponent> extra = addExtraTooltip(value);
		return extra.isEmpty()? Optional.empty() : Optional.of(extra.toArray(new ITextComponent[0]));
	}
	
	protected ITextComponent[] addExtraTooltip(ITextComponent[] tooltip, Gui value) {
		return ArrayUtils.addAll(tooltip, addExtraTooltip(value).toArray(new ITextComponent[0]));
	}
	
	protected List<ITextComponent> addExtraTooltip(Gui value) {
		return Lists.newArrayList();
	}
	
	public List<ITextComponent> getErrors(Gui value) {
		return Stream.of(getError(value))
		  .filter(Optional::isPresent).map(Optional::get)
		  .collect(Collectors.toList());
	}
	
	public Optional<ITextComponent> getError(Gui value) {
		Optional<ITextComponent> o;
		if (guiErrorSupplier != null) {
			o = guiErrorSupplier.apply(value);
			if (o.isPresent()) return o;
		}
		if (errorSupplier != null) {
			final V v = fromGui(value);
			if (v != null) {
				o = errorSupplier.apply(v);
				if (o.isPresent()) return o;
			}
		}
		return Optional.empty();
	}
	
	protected ForgeConfigSpec.Builder decorate(ForgeConfigSpec.Builder builder) {
		if (comment != null)
			builder = builder.comment(comment);
		// This doesn't work
		// It seems that Config translations are not implemented in Forge's end
		// Also, mods I18n entries load after configs registration, which
		// makes impossible to set the comments to the tooltip translations
		if (tooltip != null)
			builder = builder.translation(tooltip);
		return builder;
	}
	
	@OnlyIn(Dist.CLIENT)
	protected <F extends FieldBuilder<Gui, ?, F>> F decorate(F builder) {
		builder.requireRestart(requireRestart)
		  .nonPersistent(nonPersistent)
		  .setDefaultValue(() -> forGui(value))
		  .setTooltipSupplier(this::getTooltip)
		  .setErrorSupplier(this::getError)
		  .setSaveConsumer(createSaveConsumer())
		  .setEditableSupplier(editableSupplier)
		  .setName(name)
		  .setIgnoreEdits(ignored);
		return builder;
	}
	
	public boolean isValidValue(V value) {
		return getErrors(forGui(value)).isEmpty();
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
		return Optional.empty();
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
		spec.define(parentPath + name, forConfig(value), createConfigValidator());
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
	  CaptionedSubCategoryBuilder<?, ?> group, ConfigEntryBuilder entryBuilder
	) {
		buildGUIEntry(entryBuilder).ifPresent(e -> {
			this.guiEntry = e;
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
			this.guiEntry = e;
			category.addEntry(e);
		});
	}
	
	protected void setConfigValue(@Nullable ConfigValue<?> value) {
		this.configValue = value;
	}
	
	protected V get() {
		if (nonPersistent) return actualValue;
		if (configValue == null) throw new NoSuchConfigEntryError(getGlobalPath());
		return get(configValue);
	}
	
	protected void set(V value) {
		if (!isValidValue(value))
			throw new InvalidConfigValueException(getGlobalPath(), value);
		if (nonPersistent) {
			actualValue = value;
		} else if (configValue == null) {
			throw new NoSuchConfigEntryError(getGlobalPath());
		} else {
			set(configValue, value);
		}
	}
	
	protected boolean trySet(V value) {
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
	
	public ExpectedType getExpectedType() {
		return new ExpectedType(typeClass);
	}
}
