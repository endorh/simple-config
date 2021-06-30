package dnj.simple_config.core;

import com.google.common.base.CaseFormat;
import dnj.simple_config.core.SimpleConfig.IGUIEntry;
import dnj.simple_config.core.SimpleConfig.InvalidConfigValueTypeException;
import dnj.simple_config.core.SimpleConfig.NoSuchConfigEntryError;
import dnj.simple_config.gui.NestedListEntry;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.*;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder.CellCreatorBuilder;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder.TopCellElementBuilder;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.tags.ITag;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.*;
import java.util.function.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// TODO: Make Groups be Entries
/**
 * An abstract config entry, which may or may not produce an entry in
 * the actual config and/or the config GUI<br>
 * Entries should not be accessed by API users after
 * their config has been registered. Doing so will result in
 * undefined behaviour.<br>
 * In particular, users can not modify the default
 * value/bounds/validators of an entry after the registering phase
 * has ended.<br>
 * Subclasses may override {@link Entry#buildConfigEntry}
 * and {@link Entry#buildGUIEntry} to generate the appropriate
 * entries in both ends
 *
 * @param <V> The type of the value held by the entry
 * @param <Config> The type of the associated config entry
 * @param <Gui> The type of the associated GUI config entry
 * @param <Self> The actual subtype of this entry to be
 *              returned by builder-like methods
 */
public abstract class Entry<V, Config, Gui, Self extends Entry<V, Config, Gui, Self>>
  implements IGUIEntry, ITooltipEntry<V, Gui, Self>, IErrorEntry<V, Gui, Self> {
	protected String name = null;
	protected @Nullable String translation = null;
	protected @Nullable String tooltip = null;
	protected @Nullable String comment = null;
	protected boolean requireRestart = false;
	protected @Nullable Function<V, Optional<ITextComponent>> errorSupplier = null;
	protected @Nullable Function<V, Optional<ITextComponent[]>> tooltipSupplier = null;
	protected @Nullable Function<Gui, Optional<ITextComponent>> guiErrorSupplier = null;
	protected @Nullable Function<Gui, Optional<ITextComponent[]>> guiTooltipSupplier = null;
	protected @Nullable BiConsumer<Gui, ISimpleConfigEntryHolder> saver = null;
	protected final V value;
	protected @Nullable Field backingField;
	protected ISimpleConfigEntryHolder parent;
	protected boolean dirty = false;
	
	// Builder methods
	@SuppressWarnings("unused")
	public static class Builders {
		// Basic types
		public static BooleanEntry bool(boolean value) {
			return new BooleanEntry(value);
		}
		public static StringEntry string(String value) {
			return new StringEntry(value);
		}
		public static <E extends Enum<E>> EnumEntry<E> enum_(E value) {
			return new EnumEntry<>(value);
		}
		public static LongEntry number(long value) {
			return number(value, (Long) null, null);
		}
		public static LongEntry number(long value, Integer max) {
			return number(value, 0L, (long) max);
		}
		public static LongEntry number(long value, Long max) {
			return number(value, 0L, max);
		}
		public static LongEntry number(long value, Integer min, Integer max) {
			return new LongEntry(value, (long) min, (long) max);
		}
		public static LongEntry number(long value, Long min, Long max) {
			return new LongEntry(value, min, max);
		}
		public static DoubleEntry number(double value) {
			return number(value, null, null);
		}
		public static DoubleEntry number(double value, Number max) {
			return number(value, 0D, max);
		}
		public static DoubleEntry number(double value, Number min, Number max) {
			return new DoubleEntry(value, min != null? min.doubleValue() : null, max != null? max.doubleValue() : null);
		}
		public static DoubleEntry fractional(double value) {
			if (0D > value || value > 1D)
				throw new IllegalArgumentException("Fraction values must be within [0, 1], passed " + value);
			return number(value, 0D, 1D);
		}
		
		public static ColorEntry color(Color value) {
			return color(value, false);
		}
		public static ColorEntry color(Color value, boolean alpha) {
			return alpha? new AlphaColorEntry(value) : new ColorEntry(value);
		}
		
		// String serializable entries
		public static <T> SerializableEntry<T> entry(T value, Function<T, String> serializer, Function<String, Optional<T>> deserializer) {
			return new SerializableEntry<>(value, serializer, deserializer);
		}
		public static <T extends ISerializableConfigEntry<T>> SerializableEntry<T> entry(T value) {
			return new SerializableConfigEntry<>(value);
		}
		
		// Convenience Minecraft entries
		public static ItemEntry item(@Nullable Item value) {
			return new ItemEntry(value);
		}
		
		// List entries
		public static StringListEntry list(List<String> value) {
			return new StringListEntry(value);
		}
		public static LongListEntry list(List<Long> value, Long min, Long max) {
			return new LongListEntry(value, min, max);
		}
		public static DoubleListEntry list(List<Double> value, Double min, Double max) {
			return new DoubleListEntry(value, min, max);
		}
		
		// List of other entries
		public static <V, C, G, E extends Entry<V, C, G, E>>
		EntryListEntry<V, C, G, E> list(Entry<V, C, G, E> entry) {
			return list(entry, new ArrayList<>());
		}
		public static <V, C, G, E extends Entry<V, C, G, E>>
		EntryListEntry<V, C, G, E> list(Entry<V, C, G, E> entry, List<V> value) {
			return new EntryListEntry<>(value, entry);
		}
	}
	
	protected Entry(V value) {
		this.value = value;
	}
	
	protected void setParent(ISimpleConfigEntryHolder config) {
		this.parent = config;
	}
	
	@SuppressWarnings("unchecked")
	protected Self self() {
		return (Self) this;
	}
	
	protected Self name(String name) {
		this.name = name;
		return self();
	}
	
	@SuppressWarnings("UnusedReturnValue")
	protected Self translate(String translation) {
		this.translation = translation;
		return self();
	}
	
	@SuppressWarnings("UnusedReturnValue")
	protected Self tooltip(String translation) {
		this.tooltip = translation;
		return self();
	}
	
	@Override public Self guiTooltipOpt(Function<Gui, Optional<ITextComponent[]>> tooltipSupplier) {
		this.guiTooltipSupplier = tooltipSupplier;
		return self();
	}
	
	@Override public Self tooltipOpt(Function<V, Optional<ITextComponent[]>> tooltipSupplier) {
		this.tooltipSupplier = tooltipSupplier;
		return self();
	}
	
	@Override public Self guiError(Function<Gui, Optional<ITextComponent>> errorSupplier) {
		this.guiErrorSupplier = errorSupplier;
		return self();
	}
	
	@Override public Self error(Function<V, Optional<ITextComponent>> errorSupplier) {
		this.errorSupplier = errorSupplier;
		return self();
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
		this.requireRestart = requireRestart;
		return self();
	}
	
	protected Self withSaver(BiConsumer<Gui, ISimpleConfigEntryHolder> saver) {
		this.saver = saver;
		return self();
	}
	
	protected boolean debugTranslations() {
		return parent != null && parent.getRoot().debugTranslations;
	}
	
	protected ITextComponent getDisplayName() {
		if (debugTranslations()) {
			if (translation != null)
				return new StringTextComponent(translation);
			else return new StringTextComponent(name);
		}
		if (translation != null && I18n.hasKey(translation))
			return new TranslationTextComponent(translation);
		return new StringTextComponent(name);
	}
	
	/**
	 * Transform value to Gui type
	 */
	protected Gui forGui(V value) {
		//noinspection unchecked
		return (Gui) value;
	}
	
	/**
	 * Transform value from Gui type
	 */
	protected @Nullable V fromGui(@Nullable Gui value) {
		//noinspection unchecked
		return (V) value;
	}
	
	/**
	 * Transform value from Gui type
	 */
	protected V fromGuiOrDefault(Gui value) {
		final V v = fromGui(value);
		return v != null ? v : this.value;
	}
	
	/**
	 * Transform value to Config type
	 */
	protected Config forConfig(V value) {
		//noinspection unchecked
		return (Config) value;
	}
	
	/**
	 * Transform value from Config type
	 */
	protected @Nullable V fromConfig(@Nullable Config value) {
		//noinspection unchecked
		return (V) value;
	}
	
	/**
	 * Transform value from Config type
	 */
	protected V fromConfigOrDefault(Config value) {
		final V v = self().fromConfig(value);
		return v != null ? v : this.value;
	}
	
	protected void markDirty() {
		markDirty(true);
	}
	
	protected void markDirty(boolean dirty) {
		this.dirty = dirty;
		if (dirty) parent.markDirty();
	}
	
	protected Consumer<Gui> saveConsumer(ISimpleConfigEntryHolder c) {
		if (saver != null)
			return g -> saver.accept(g, c);
		final String n = name; // Use the current name
		return g -> {
			// The save consumer shouldn't run with invalid values in the first place
			final V v = fromGuiOrDefault(g);
			if (!c.get(n).equals(v)) {
				markDirty();
				c.markDirty().set(n, v);
			}
		};
	}
	
	@OnlyIn(Dist.CLIENT)
	protected Optional<ITextComponent[]> supplyTooltip(Gui value) {
		if (debugTranslations())
			return supplyDebugTooltip(value);
		Optional<ITextComponent[]> o;
		if (guiTooltipSupplier != null) {
			o = guiTooltipSupplier.apply(value);
			if (o.isPresent()) return o;
		}
		final V v = fromGui(value);
		if (tooltipSupplier != null) {
			if (v != null) {
				o = tooltipSupplier.apply(v);
				if (o.isPresent()) return o;
			}
		}
		if (tooltip != null && I18n.hasKey(tooltip))
			return Optional.of(
			  Arrays.stream(I18n.format(tooltip, v).split("\n"))
			    .map(StringTextComponent::new).toArray(ITextComponent[]::new));
		return Optional.empty();
	}
	
	protected Optional<ITextComponent> supplyError(Gui value) {
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
	protected <F extends FieldBuilder<?, ?>> F decorate(F builder) {
		try {
			builder.requireRestart(requireRestart);
		} catch (UnsupportedOperationException ignored) {}
		return builder;
	}
	
	/**
	 * Build the associated config entry for this entry, if any
	 */
	protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.empty();
	}
	
	/**
	 * Generate an {@link AbstractConfigListEntry} to be added to the GUI, if any
	 * @param builder Entry builder
	 * @param config Config holder
	 */
	@OnlyIn(Dist.CLIENT)
	protected Optional<AbstractConfigListEntry<?>> buildGUIEntry(
	  ConfigEntryBuilder builder, ISimpleConfigEntryHolder config
	) {
		return Optional.empty();
	}
	
	/**
	 * Add entry to the GUI<br>
	 * Subclasses should instead override {@link Entry#buildGUIEntry} in most cases
	 */
	@OnlyIn(Dist.CLIENT)
	@Override public void buildGUI(
	  ConfigCategory category, ConfigEntryBuilder entryBuilder, ISimpleConfigEntryHolder config
	) {
		buildGUIEntry(entryBuilder, config).ifPresent(category::addEntry);
	}
	
	/**
	 * Get the value held by this entry
	 *
	 * @param spec Config spec to look into
	 * @throws ClassCastException if the found value type does not match the expected
	 */
	protected V get(ConfigValue<?> spec) {
		//noinspection unchecked
		return fromConfigOrDefault(((ConfigValue<Config>) spec).get());
	}
	
	/**
	 * Set the value held by this entry
	 *
	 * @param spec Config spec to update
	 * @throws ClassCastException if the found value type does not match the expected
	 */
	protected void set(ConfigValue<?> spec, V value) {
		//noinspection unchecked
		((ConfigValue<Config>) spec).set(forConfig(value));
	}
	
	protected void addTranslationsDebugInfo(List<ITextComponent> tooltip) {}
	protected Optional<ITextComponent[]> supplyDebugTooltip(Gui value) {
		List<ITextComponent> lines = new ArrayList<>();
		if (translation != null) {
			lines.add(new StringTextComponent("Translation key:"));
			final String status = I18n.hasKey(translation) ? "(✔ present)" : "(✘ missing)";
			lines.add(new StringTextComponent("   " + translation + " " + status));
		} else lines.add(new StringTextComponent("Error: couldn't map translation key"));
		if (tooltip != null) {
			lines.add(new StringTextComponent("Tooltip key:"));
			final String status = I18n.hasKey(tooltip)? "(✔ present)" : "(not present)";
			lines.add(new StringTextComponent("   " + tooltip + " " + status));
		} else lines.add(new StringTextComponent("Error: couldn't map tooltip translation key"));
		if (guiTooltipSupplier != null)
			lines.add(new StringTextComponent(" + Has GUI tooltip supplier"));
		if (tooltipSupplier != null)
			lines.add(new StringTextComponent(" + Has tooltip supplier"));
		if (guiErrorSupplier != null)
			lines.add(new StringTextComponent(" + Has GUI error supplier"));
		if (errorSupplier != null)
			lines.add(new StringTextComponent(" + Has error supplier"));
		addTranslationsDebugInfo(lines);
		lines.add(new StringTextComponent(" "));
		lines.add(new StringTextComponent(" ⚠ Translation debug mode active"));
		lines.add(new StringTextComponent("     Remember to remove the call to .debugTranslations()"));
		return Optional.of(lines.toArray(new ITextComponent[0]));
	}
	
	
	// Subclasses follow ----------------------------------------------
	
	public static class EmptyEntry extends Entry<Void, Void, Void, EmptyEntry> {
		public EmptyEntry() {
			super(null);
		}
	}
	
	public static class TextEntry extends EmptyEntry {
		private static final Logger LOGGER = LogManager.getLogger();
		protected @Nullable Supplier<ITextComponent> translation = null; // Lazy
		
		public TextEntry(@Nullable Supplier<ITextComponent> supplier) {
			this.translation = supplier;
		}
		
		public TextEntry() {
			if (super.translation != null)
				this.translation = () -> new TranslationTextComponent(super.translation);
		}
		
		@Override protected EmptyEntry translate(String translation) {
			if (this.translation == null) {
				super.translate(translation);
				if (super.translation != null)
					this.translation = () -> new TranslationTextComponent(super.translation);
			}
			return this;
		}
		
		@OnlyIn(Dist.CLIENT)
		@Override
		public Optional<AbstractConfigListEntry<?>> buildGUIEntry(
		  ConfigEntryBuilder builder, ISimpleConfigEntryHolder c
		) {
			if (translation != null) {
				final TextDescriptionBuilder valBuilder = builder
				  .startTextDescription(translation.get());
				return Optional.of(decorate(valBuilder).build());
			} else {
				LOGGER.warn("Malformed text entry in config with name " + name);
				return Optional.empty();
			}
		}
	}
	
	public static class BooleanEntry extends Entry<Boolean, Boolean, Boolean, BooleanEntry> {
		protected Function<Boolean, ITextComponent> yesNoSupplier = null;
		
		public BooleanEntry(boolean value) {
			super(value);
		}
		
		@Override protected Optional<ConfigValue<?>> buildConfigEntry(Builder builder) {
			return Optional.of(decorate(builder).define(name, value.booleanValue()));
		}
		
		/**
		 * Set a Yes/No supplier for this entry
		 */
		public BooleanEntry displayAs(Function<Boolean, ITextComponent> displayAdapter) {
			this.yesNoSupplier = displayAdapter;
			return this;
		}
		
		@OnlyIn(Dist.CLIENT)
		@Override protected Optional<AbstractConfigListEntry<?>> buildGUIEntry(
		  ConfigEntryBuilder builder, ISimpleConfigEntryHolder c
		) {
			final BooleanToggleBuilder valBuilder = builder
			  .startBooleanToggle(getDisplayName(), c.get(name))
			  .setDefaultValue(value)
			  .setSaveConsumer(saveConsumer(c))
			  .setYesNoTextSupplier(yesNoSupplier)
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(this::supplyError);
			return Optional.of(decorate(valBuilder).build());
		}
	}
	
	public static abstract class RangedEntry
	  <V, Config, Gui, This extends RangedEntry<V, Config, Gui, This>> extends Entry<V, Config, Gui, This> {
		public V min;
		public V max;
		protected boolean asSlider = false;
		
		public RangedEntry(V value, V min, V max) {
			super(value);
			this.min = min;
			this.max = max;
		}
		
		public This min(V min) {
			this.min = min;
			return self();
		}
		
		public This max(V max) {
			this.max = max;
			return self();
		}
	}
	
	public static class LongEntry extends RangedEntry<Long, Long, Long, LongEntry> {
		public LongEntry(long value, Long min, Long max) {
			super(value,
			      min == null ? Long.MIN_VALUE : min,
			      max == null ? Long.MAX_VALUE : max);
		}
		
		public LongEntry slider() { return slider(true); }
		
		public LongEntry slider(boolean slider) {
			this.asSlider = slider;
			return this;
		}
		
		@Override protected Optional<ConfigValue<?>> buildConfigEntry(Builder builder) {
			return Optional.of(decorate(builder).defineInRange(name, value, min, max));
		}
		
		@OnlyIn(Dist.CLIENT)
		@Override protected Optional<AbstractConfigListEntry<?>> buildGUIEntry(
		  ConfigEntryBuilder builder, ISimpleConfigEntryHolder c
		) {
			if (!asSlider) {
				final LongFieldBuilder valBuilder = builder
				  .startLongField(getDisplayName(), c.get(name))
				  .setDefaultValue(value)
				  .setMin(min).setMax(max)
				  .setSaveConsumer(saveConsumer(c))
				  .setTooltipSupplier(this::supplyTooltip)
				  .setErrorSupplier(this::supplyError);
				return Optional.of(decorate(valBuilder).build());
			} else {
				final LongSliderBuilder valBuilder = builder
				  .startLongSlider(getDisplayName(), c.get(name), min, max)
				  .setDefaultValue(value)
				  .setSaveConsumer(saveConsumer(c))
				  .setTooltipSupplier(this::supplyTooltip)
				  .setTooltipSupplier(this::supplyTooltip)
				  .setErrorSupplier(this::supplyError);
				return Optional.of(decorate(valBuilder).build());
			}
		}
	}
	
	public static class DoubleEntry extends RangedEntry<Double, Double, Double, DoubleEntry> {
		public DoubleEntry(double value, Double min, Double max) {
			super(value,
			      min == null ? Double.NEGATIVE_INFINITY : min,
			      max == null ? Double.POSITIVE_INFINITY : max);
		}
		
		@Override protected Optional<ConfigValue<?>> buildConfigEntry(Builder builder) {
			return Optional.of(decorate(builder).defineInRange(name, value, min, max));
		}
		
		@OnlyIn(Dist.CLIENT)
		@Override protected Optional<AbstractConfigListEntry<?>> buildGUIEntry(
		  ConfigEntryBuilder builder, ISimpleConfigEntryHolder c
		) {
			if (!asSlider) {
				final DoubleFieldBuilder valBuilder = builder
				  .startDoubleField(getDisplayName(), c.get(name))
				  .setDefaultValue(value)
				  .setMin(min).setMax(max)
				  .setSaveConsumer(saveConsumer(c))
				  .setTooltipSupplier(this::supplyTooltip)
				  .setErrorSupplier(this::supplyError);
				return Optional.of(decorate(valBuilder).build());
			} else {
				/*
				final DoubleSliderBuilder valBuilder = builder
				  .startDoubleSlider(getDisplayName(), c.get(name))
				  .setDefaultValue(value)
				  .setMin(min).setMax(max)
				  .setSaveConsumer(saveConsumer(c))
				  .setTooltipSupplier(this::supplyTooltip)
				  .setErrorSupplier(this::supplyError);
				return Optional.of(decorate(valBuilder).build());
				*/
				throw new NotImplementedException(
				  "Double slider entries are not implemented");
			}
		}
	}
	
	public static class StringEntry extends Entry<String, String, String, StringEntry> {
		public StringEntry(String value) {super(value);}
		
		@Override protected Optional<ConfigValue<?>> buildConfigEntry(Builder builder) {
			return Optional.of(decorate(builder).define(name, value));
		}
		
		@OnlyIn(Dist.CLIENT)
		@Override protected Optional<AbstractConfigListEntry<?>> buildGUIEntry(
		  ConfigEntryBuilder builder, ISimpleConfigEntryHolder c
		) {
			final TextFieldBuilder valBuilder = builder
			  .startTextField(getDisplayName(), c.get(name))
			  .setDefaultValue(value)
			  .setSaveConsumer(saveConsumer(c))
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(this::supplyError);
			return Optional.of(decorate(valBuilder).build());
		}
	}
	
	public static class EnumEntry<E extends Enum<E>> extends Entry<E, E, E, EnumEntry<E>> {
		public Class<E> enumClass;
		
		public EnumEntry(E value) {
			super(value);
			enumClass = value.getDeclaringClass();
		}
		
		@Override protected Optional<ConfigValue<?>> buildConfigEntry(Builder builder) {
			return Optional.of(decorate(builder).defineEnum(name, value));
		}
		
		@Override protected void addTranslationsDebugInfo(List<ITextComponent> tooltip) {
			super.addTranslationsDebugInfo(tooltip);
			if (parent != null) {
				tooltip.add(new StringTextComponent(" + Enum translation keys:"));
				for (E elem : enumClass.getEnumConstants())
					tooltip.add(new StringTextComponent(
					  "   > " + getEnumTranslationKey(elem, parent.getRoot())));
			}
		}
		
		protected String getEnumTranslationKey(E item, SimpleConfig config) {
			return config.modId + ".config.enum." +
			CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, enumClass.getSimpleName()) +
			"." + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_UNDERSCORE, item.name());
		}
		
		@OnlyIn(Dist.CLIENT)
		protected ITextComponent enumName(E item, SimpleConfig config) {
			if (item instanceof ITranslatedEnum)
				return ((ITranslatedEnum) item).getDisplayName();
			final String key = getEnumTranslationKey(item, config);
			// if (debugTranslations()) return new StringTextComponent(key);
			if (I18n.hasKey(key))
				return new TranslationTextComponent(key);
			return new StringTextComponent(item.name());
		}
		
		@OnlyIn(Dist.CLIENT)
		@Override
		protected Optional<AbstractConfigListEntry<?>> buildGUIEntry(
		  ConfigEntryBuilder builder, ISimpleConfigEntryHolder c
		) {
			final EnumSelectorBuilder<E> valBuilder = builder
			  .startEnumSelector(getDisplayName(), enumClass, c.get(name))
			  .setDefaultValue(value)
			  .setSaveConsumer(saveConsumer(c))
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(this::supplyError);
			//noinspection unchecked
			valBuilder.setEnumNameProvider(e -> enumName((E) e, c.getRoot()));
			return Optional.of(decorate(valBuilder).build());
		}
	}
	
	public interface ITranslatedEnum {
		ITextComponent getDisplayName();
	}
	
	public static abstract class ListEntry
	  <V, Config, Gui, Self extends ListEntry<V, Config, Gui, Self>> extends Entry<List<V>, List<Config>, List<Gui>, Self> {
		public Function<V, Optional<ITextComponent>> validator = t -> Optional.empty();
		protected boolean expand;
		
		public ListEntry(@Nullable List<V> value) {
			super(value != null? value : new ArrayList<>());
		}
		
		/**
		 * Set a validator for the elements of this list entry<br>
		 * You may also use {@link ListEntry#setValidator(Function)}
		 * to provide users with more explicative error messages<br>
		 * You may also use {@link IErrorEntry#error(Function)} to
		 * validate instead the whole list
		 * @param validator Element validator. Should return true for all valid elements
		 */
		public Self setValidator(Predicate<V> validator) {
			return this.setValidator((Function<V, Optional<ITextComponent>>)
			  c -> validator.test(c)? Optional.empty() :
			       Optional.of(new TranslationTextComponent(
			    "simple-config.config.error.list_element_does_not_match_validator", c)));
		}
		
		/**
		 * Set an error message supplier for the elements of this list entry<br>
		 * You may also use {@link IErrorEntry#error(Function)} to check
		 * instead the whole list
		 * @param validator Error message supplier. Empty return values indicate
		 *                  correct values
		 */
		public Self setValidator(Function<V, Optional<ITextComponent>> validator) {
			this.validator = validator;
			return self();
		}
		
		/**
		 * Expand this list automatically in the GUI
		 */
		public Self expand() {
			return expand(true);
		}
		
		/**
		 * Expand this list automatically in the GUI
		 */
		public Self expand(boolean expand) {
			this.expand = expand;
			return self();
		}
		
		@Override protected List<Gui> forGui(List<V> list) {
			return list.stream().map(this::elemForGui).collect(Collectors.toList());
		}
		@Override protected @Nullable List<V> fromGui(@Nullable List<Gui> list) {
			if (list == null) return null;
			return list.stream().map(this::elemFromGui).collect(Collectors.toList());
		}
		@Override protected List<Config> forConfig(List<V> list) {
			return list.stream().map(this::elemForConfig).collect(Collectors.toList());
		}
		@Override protected @Nullable List<V> fromConfig(@Nullable List<Config> list) {
			if (list == null) return null;
			return list.stream().map(this::elemFromConfig).collect(Collectors.toList());
		}
		
		protected Gui elemForGui(V value) {
			//noinspection unchecked
			return (Gui) value;
		}
		protected V elemFromGui(Gui value) {
			//noinspection unchecked
			return (V) value;
		}
		protected Config elemForConfig(V value) {
			//noinspection unchecked
			return (Config) value;
		}
		protected V elemFromConfig(Config value) {
			//noinspection unchecked
			return (V) value;
		}
		
		protected boolean validateElement(Object o) {
			try {
				//noinspection unchecked
				return !validator.apply(elemFromConfig((Config) o)).isPresent();
			} catch (ClassCastException ignored) {
				return false;
			}
		}
		
		protected static ITextComponent addIndex(ITextComponent message, int index) {
			return message.copyRaw().appendString(", ").append(new TranslationTextComponent(
			  "simple-config.config.error.at_index",
			  new StringTextComponent(String.format("%d", index + 1)).mergeStyle(TextFormatting.AQUA)));
		}
		
		@Override protected Optional<ITextComponent> supplyError(List<Gui> value) {
			for (int i = 0; i < value.size(); i++) {
				Config elem = elemForConfig(elemFromGui(value.get(i)));
				final Optional<ITextComponent> error = validator.apply(elemFromConfig(elem));
				if (error.isPresent()) return Optional.of(addIndex(error.get(), i));
			}
			return super.supplyError(value);
		}
		
		@Override protected Optional<ConfigValue<?>> buildConfigEntry(
		  Builder builder
		) {
			return Optional.of(decorate(builder).defineList(name, value, this::validateElement));
		}
	}
	
	public static class EntryListEntry
	  <V, C, G, E extends Entry<V, C, G, E>>
	  extends ListEntry<V, C, G, EntryListEntry<V, C, G, E>> {
		protected static final String TOOLTIP_KEY_SUFFIX = ".help";
		protected static final String SUB_ELEMENTS_KEY_SUFFIX = ".sub";
		
		protected final Entry<V, C, G, E> entry;
		protected ListEntryEntryHolder<V, C, G, E> holder;
		
		public static class ListEntryEntryHolder<V, C, G, E extends Entry<V, C, G, E>>
		  implements ISimpleConfigEntryHolder {
			private static final Logger LOGGER = LogManager.getLogger();
			protected final Entry<V, C, G, E> entry;
			protected List<V> value = null;
			protected final List<V> buffer = new ArrayList<>();
			
			protected String nameFor(G element) {
				buffer.add(entry.fromGuiOrDefault(element));
				return String.valueOf(buffer.size() - 1);
			}
			
			protected void onDelete(int index) {
				buffer.remove(index);
			}
			
			protected void clear() {
				buffer.clear();
				value = null;
			}
			
			public ListEntryEntryHolder(Entry<V, C, G, E> entry) {
				this.entry = entry;
			}
			
			protected void setValue(List<V> value) {
				this.value = value;
			}
			
			@Override public SimpleConfig getRoot() {
				return entry.parent.getRoot();
			}
			
			@Override public void markDirty(boolean dirty) {
				entry.parent.markDirty(dirty);
			}
			
			@Override public <T> T get(String path) {
				// V must be T
				try {
					//noinspection unchecked
					return (T) buffer.get(Integer.parseInt(path));
				} catch (NumberFormatException e) {
					throw new NoSuchConfigEntryError(entry.name + "." + path);
				} catch (ClassCastException e) {
					throw new InvalidConfigValueTypeException(entry.name + "." + path, e);
				}
			}
			
			@Override public <T> void set(String path, T value) {
				// T must be V
				try {
					//noinspection unchecked
					this.buffer.set(Integer.parseInt(path), (V) value);
				} catch (NumberFormatException e) {
					throw new NoSuchConfigEntryError(entry.name + "." + path);
				} catch (ClassCastException e) {
					throw new InvalidConfigValueTypeException(entry.name + "." + path, e);
				}
			}
		}
		
		public EntryListEntry(@Nullable List<V> value, Entry<V, C, G, E> entry) {
			super(value);
			this.entry = entry.withSaver((g, c) -> {});
			if (translation != null)
				this.translate(translation);
			if (tooltip != null)
				this.tooltip(tooltip);
		}
		
		@Override protected EntryListEntry<V, C, G, E> translate(String translation) {
			super.translate(translation);
			if (translation != null)
				entry.translate(translation + SUB_ELEMENTS_KEY_SUFFIX);
			return self();
		}
		
		@Override protected EntryListEntry<V, C, G, E> tooltip(String translation) {
			super.tooltip(translation);
			if (tooltip != null)
				if (tooltip.endsWith(TOOLTIP_KEY_SUFFIX))
					entry.tooltip(tooltip.substring(0, tooltip.length() - TOOLTIP_KEY_SUFFIX.length())
					              + SUB_ELEMENTS_KEY_SUFFIX + TOOLTIP_KEY_SUFFIX);
				else entry.tooltip(tooltip + SUB_ELEMENTS_KEY_SUFFIX);
			return self();
		}
		
		@Override protected void setParent(ISimpleConfigEntryHolder config) {
			super.setParent(config);
			this.entry.setParent(config);
			this.holder = new ListEntryEntryHolder<>(entry);
		}
		
		@Override protected C elemForConfig(V value) {
			return entry.forConfig(value);
		}
		@Override protected V elemFromConfig(C value) {
			return entry.fromConfig(value);
		}
		@Override protected G elemForGui(V value) {
			return entry.forGui(value);
		}
		@Override protected V elemFromGui(G value) {
			return entry.fromGui(value);
		}
		
		@Override protected Optional<ConfigValue<?>> buildConfigEntry(Builder builder) {
			return Optional.of(decorate(builder).defineList(name, forConfig(value), v -> {
				try {
					//noinspection unchecked
					return !entry.supplyError(elemForGui(elemFromConfig((C) v))).isPresent();
				} catch (ClassCastException e) {
					return false;
				}
			}));
		}
		
		@Override protected Optional<AbstractConfigListEntry<?>> buildGUIEntry(
		  ConfigEntryBuilder builder, ISimpleConfigEntryHolder c
		) {
			holder.setValue(c.get(name));
			holder.clear();
			final NestedListEntry<G, AbstractConfigListEntry<G>> e =
			  new NestedListEntry<>(
			    getDisplayName(), forGui(c.get(name)), expand,
			    () -> this.supplyTooltip(forGui(c.get(name))),
			    saveConsumer(c).andThen(g -> holder.clear()),
			    () -> forGui(value), builder.getResetButtonKey(),
			    true, false,
			    (g, en) -> {
					 entry.name(holder.nameFor(g));
					 //noinspection unchecked
					 return (AbstractConfigListEntry<G>) entry.buildGUIEntry(
						builder, holder
					 ).orElseThrow(() -> new IllegalStateException(
						"Sub entry in list entry did not generate a GUI entry"));
				 }, holder::onDelete);
			e.setRequiresRestart(requireRestart);
			e.setTooltipSupplier(() -> this.supplyTooltip(e.getValue()));
			e.setErrorSupplier(() -> this.supplyError(e.getValue()));
			return Optional.of(e);
		}
	}
	
	public static abstract class RangedListEntry
	  <V extends Comparable<V>, Config, Gui, Self extends RangedListEntry<V, Config, Gui, Self>> extends ListEntry<V, Config, Gui, Self> {
		public V min;
		public V max;
		
		public RangedListEntry(
		  @Nullable List<V> value, @Nonnull V min, @Nonnull V max
		) {
			super(value);
			this.min = min;
			this.max = max;
		}
		
		/**
		 * Set the minimum allowed value for the elements of this list entry
		 */
		public Self min(@Nonnull V min) {
			this.min = min;
			setValidator(clamp(validator, min, max));
			return self();
		}
		
		/**
		 * Set the maximum allowed value for the elements of this list entry
		 */
		public Self max(@Nonnull V max) {
			this.max = max;
			setValidator(clamp(validator, min, max));
			return self();
		}
		
		@Override public Self setValidator(Function<V, Optional<ITextComponent>> validator) {
			return super.setValidator(clamp(validator, min, max));
		}
		
		protected Function<V, Optional<ITextComponent>> clamp(
		  @Nullable Function<V, Optional<ITextComponent>> validator, V min, V max
		) {
			return t -> {
				if (t.compareTo(min) < 0)
					return Optional.of(new TranslationTextComponent("text.cloth-config.error.too_small", min));
				if (t.compareTo(max) > 0)
					return Optional.of(new TranslationTextComponent("text.cloth-config.error.too_large", max));
				return validator != null? validator.apply(t) : Optional.empty();
			};
		}
	}
	
	public static class LongListEntry extends RangedListEntry<Long, Number, Long, LongListEntry> {
		public LongListEntry(@Nullable List<Long> value) {
			this(value, null, null);
		}
		public LongListEntry(
		  @Nullable List<Long> value, @Nullable Long min, @Nullable Long max
		) {
			super(value, min != null? min : Long.MIN_VALUE, max != null? max : Long.MAX_VALUE);
		}
		
		@Override protected Long elemFromConfig(Number value) {
			return value.longValue();
		}
		
		@Override protected List<Long> get(ConfigValue<?> spec) {
			// Sometimes Forge returns lists of subtypes, so we cast them
			//noinspection unchecked
			return ((ConfigValue<List<Number>>) spec).get().stream().map(Number::longValue)
			  .collect(Collectors.toList());
			
			// //noinspection unchecked
			// return ((List<Number>) (List<?>) super.get(spec))
			//   .stream().map(Number::longValue).collect(Collectors.toList());
		}
		
		@OnlyIn(Dist.CLIENT)
		@Override protected Optional<AbstractConfigListEntry<?>> buildGUIEntry(
		  ConfigEntryBuilder builder, ISimpleConfigEntryHolder c
		) {
			final LongListBuilder valBuilder = builder
			  .startLongList(getDisplayName(), c.get(name))
			  .setDefaultValue(value)
			  .setMin(min).setMax(max)
			  .setExpanded(expand)
			  .setSaveConsumer(saveConsumer(c))
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(this::supplyError);
			return Optional.of(decorate(valBuilder).build());
		}
	}
	
	public static class DoubleListEntry extends RangedListEntry<Double, Number, Double, DoubleListEntry> {
		public DoubleListEntry(@Nullable List<Double> value) {
			this(value, null, null);
		}
		public DoubleListEntry(
		  @Nullable List<Double> value,
		  @Nullable Double min, @Nullable Double max
		) {
			super(value, min != null? min : Double.NEGATIVE_INFINITY, max != null? max : Double.POSITIVE_INFINITY);
		}
		
		@Override protected Double elemFromConfig(Number value) {
			return value.doubleValue();
		}
		
		@Override protected List<Double> get(ConfigValue<?> spec) {
			// Sometimes forge returns lists of subtypes, so we cast them
			//noinspection unchecked
			return ((List<Number>) (List<?>) super.get(spec))
			  .stream().map(Number::doubleValue).collect(Collectors.toList());
		}
		
		@OnlyIn(Dist.CLIENT)
		@Override protected Optional<AbstractConfigListEntry<?>> buildGUIEntry(
		  ConfigEntryBuilder builder, ISimpleConfigEntryHolder c
		) {
			final DoubleListBuilder valBuilder = builder
			  .startDoubleList(getDisplayName(), c.get(name))
			  .setDefaultValue(value)
			  .setMin(min).setMax(max)
			  .setExpanded(expand)
			  .setSaveConsumer(saveConsumer(c))
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(this::supplyError);
			return Optional.of(decorate(valBuilder).build());
		}
	}
	
	public static class StringListEntry extends ListEntry<String, String, String, StringListEntry> {
		public StringListEntry(List<String> value) {
			super(value);
		}
		
		@OnlyIn(Dist.CLIENT)
		@Override protected Optional<AbstractConfigListEntry<?>> buildGUIEntry(
		  ConfigEntryBuilder builder, ISimpleConfigEntryHolder c
		) {
			final StringListBuilder valBuilder = builder
			  .startStrList(getDisplayName(), c.get(name))
			  .setDefaultValue(value)
			  .setExpanded(expand)
			  .setSaveConsumer(saveConsumer(c))
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(this::supplyError);
			return Optional.of(decorate(valBuilder).build());
		}
	}
	
	public interface ISerializableConfigEntry<T extends ISerializableConfigEntry<T>> {
		IConfigEntrySerializer<T> getConfigSerializer();
	}
	
	public interface IConfigEntrySerializer<T> {
		String serializeConfigEntry(T value);
		Optional<T> deserializeConfigEntry(String value);
	}
	
	/**
	 * Doesn't have a GUI<br>
	 * To create your custom type of entry with GUI,
	 * extend this class or {@link Entry} directly
	 *
	 * @param <V> Type of the value
	 */
	@SuppressWarnings("unused")
	public static class SerializableEntry<V> extends Entry<V, String, String, SerializableEntry<V>> {
		public Function<V, String> serializer;
		public Function<String, Optional<V>> deserializer;
		
		public SerializableEntry(
		  V value, IConfigEntrySerializer<V> serializer
		) {
			this(value, serializer::serializeConfigEntry, serializer::deserializeConfigEntry);
		}
		
		@SuppressWarnings("unused")
		public SerializableEntry(
		  V value,
		  Function<V, String> serializer,
		  Function<String, Optional<V>> deserializer
		) {
			super(value);
			this.serializer = serializer;
			this.deserializer = deserializer;
		}
		
		@Override protected String forGui(V value) {
			return serializer.apply(value);
		}
		
		@Override protected @Nullable V fromGui(@Nullable String value) {
			return value != null? deserializer.apply(value).orElse(null) : null;
		}
		
		@Override protected String forConfig(V value) {
			return serializer.apply(value);
		}
		
		@Override protected @Nullable V fromConfig(@Nullable String value) {
			return value != null? deserializer.apply(value).orElse(null) : null;
		}
		
		@Override protected Optional<ConfigValue<?>> buildConfigEntry(Builder builder) {
			return Optional.of(
			  builder.define(
			    name, serializer.apply(value),
				 s -> s instanceof String && deserializer.apply((String) s).isPresent()));
		}
		
		@Override protected Optional<AbstractConfigListEntry<?>> buildGUIEntry(
		  ConfigEntryBuilder builder, ISimpleConfigEntryHolder c
		) {
			final TextFieldBuilder valBuilder = builder
			  .startTextField(getDisplayName(), forGui(c.get(name)))
			  .setDefaultValue(forGui(value))
			  .setSaveConsumer(saveConsumer(c))
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(this::supplyError);
			return Optional.of(decorate(valBuilder).build());
		}
	}
	
	public static class SerializableConfigEntry<T extends ISerializableConfigEntry<T>> extends SerializableEntry<T> {
		public SerializableConfigEntry(T value) {
			super(value, null, null);
			final IConfigEntrySerializer<T> serializer = value.getConfigSerializer();
			this.serializer = serializer::serializeConfigEntry;
			this.deserializer = serializer::deserializeConfigEntry;
		}
	}
	
	public static class ColorEntry extends Entry<Color, String, Integer, ColorEntry> {
		public ColorEntry(Color value) {
			super(value);
		}
		
		@Override protected String forConfig(Color value) {
			return String.format("#%06X", value.getRGB() & 0xFFFFFF);
		}
		protected static final Pattern COLOR_PATTERN = Pattern.compile(
		  "\\s*(?:0x|#)(?i)(?<color>[0-9a-f]{3}|[0-9a-f]{6})\\s*");
		@Override protected @Nullable Color fromConfig(String value) {
			if (value == null)
				return null;
			final Matcher m = COLOR_PATTERN.matcher(value);
			if (m.matches()) {
				String c = m.group("color");
				if (c.length() == 3)
					c = doubleChars(c);
				return new Color((int) Long.parseLong(c.toLowerCase(), 0x10));
			}
			return null;
		}
		
		@Override protected Integer forGui(Color value) {
			return value.getRGB() & 0xFFFFFF;
		}
		@Override protected @Nullable Color fromGui(@Nullable Integer value) {
			return value != null? new Color(value) : null;
		}
		
		protected static String doubleChars(String s) {
			StringBuilder r = new StringBuilder();
			for (char ch : s.toCharArray())
				r.append(ch);
			return r.toString();
		}
		
		@Override protected Optional<ConfigValue<?>> buildConfigEntry(Builder builder) {
			return Optional.of(decorate(builder).define(
			  name, forConfig(value), s -> s instanceof String && fromConfig((String) s) != null));
		}
		
		@OnlyIn(Dist.CLIENT)
		@Override protected Optional<AbstractConfigListEntry<?>> buildGUIEntry(
		  ConfigEntryBuilder builder, ISimpleConfigEntryHolder c
		) {
			final ColorFieldBuilder valBuilder = builder
			  .startColorField(getDisplayName(), forGui(c.get(name)))
			  .setDefaultValue(forGui(value))
			  .setSaveConsumer(saveConsumer(c))
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(this::supplyError);
			return Optional.of(decorate(valBuilder).build());
		}
	}
	
	public static class AlphaColorEntry extends ColorEntry {
		public AlphaColorEntry(Color value) {
			super(value);
		}
		
		@Override protected String forConfig(Color value) {
			return String.format("#%08X", value.getRGB());
		}
		protected static final Pattern ALPHA_COLOR_PATTERN = Pattern.compile(
		  "\\s*(?:0x|#)(?i)(?<color>[0-9a-f]{3,4}|[0-9a-f]{6}|[0-9a-f]{8})\\s*");
		@Override protected @Nullable Color fromConfig(@Nullable String value) {
			if (value == null)
				return null;
			final Matcher m = ALPHA_COLOR_PATTERN.matcher(value);
			if (m.matches()) {
				String c = m.group("color");
				if (c.length() < 6)
					c = doubleChars(c);
				return new Color((int) Long.parseLong(c.toLowerCase(), 0x10), true);
			}
			return null;
		}
		
		@Override protected Integer forGui(Color value) {
			return value.getRGB();
		}
		@Override protected Color fromGui(@Nullable Integer value) {
			return value != null? new Color(value, true) : null;
		}
		
		@Override protected Optional<ConfigValue<?>> buildConfigEntry(Builder builder) {
			return Optional.of(decorate(builder).define(
			  name, forConfig(value), s -> s instanceof String && fromConfig((String) s) != null));
		}
		
		@OnlyIn(Dist.CLIENT)
		@Override protected Optional<AbstractConfigListEntry<?>> buildGUIEntry(
		  ConfigEntryBuilder builder, ISimpleConfigEntryHolder c
		) {
			final int prev = c.<Color>get(name).getRGB();
			final ColorFieldBuilder valBuilder = builder
			  .startAlphaColorField(getDisplayName(), prev)
			  .setDefaultValue(forGui(value))
			  .setSaveConsumer(saveConsumer(c))
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(this::supplyError);
			return Optional.of(decorate(valBuilder).build());
		}
	}
	
	public static class ItemEntry extends Entry<Item, String, Item, ItemEntry> {
		protected final ItemStack stack;
		protected Ingredient filter = null;
		protected ITag<Item> tag = null;
		protected Set<Item> validItems = null;
		
		public ItemEntry(@Nullable Item value) {
			super(value != null? value : Items.AIR);
			this.stack = new ItemStack(this.value);
		}
		
		public ItemEntry from(Ingredient filter) {
			this.filter = filter;
			if (filter != null) {
				if (!filter.test(stack))
					throw new IllegalArgumentException(
					  "Filter for item config entry does not match the default value");
				validItems = Arrays.stream(filter.getMatchingStacks()).map(ItemStack::getItem).collect(Collectors.toSet());
			} else validItems = null;
			return this;
		}
		
		public ItemEntry from(Item... items) {
			return from(Ingredient.fromItems(items));
		}
		
		public ItemEntry from(ITag<Item> tag) {
			this.tag = tag;
			return this;
		}
		
		protected Set<Item> getValidItems() {
			if (tag != null) {
				// Tags cannot be used until a world is loaded
				// Until a world is loaded we simply don't apply any restrictions
				try {
					filter = Ingredient.fromTag(tag);
					validItems = Arrays.stream(filter.getMatchingStacks()).map(ItemStack::getItem)
					  .collect(Collectors.toSet());
				} catch (IllegalStateException e) {
					filter = null;
					validItems = null;
				}
			}
			return validItems != null? validItems : Registry.ITEM.stream().collect(Collectors.toSet());
		}
		
		@Override protected String forConfig(Item value) {
			//noinspection ConstantConditions
			return value.getRegistryName().toString();
		}
		
		@Override protected @Nullable Item fromConfig(@Nullable String value) {
			if (value == null) return null;
			final Item i = fromId(value);
			return i != null ? i : this.value;
		}
		
		protected @Nullable Item fromId(String itemId) {
			final ResourceLocation registryName = new ResourceLocation(itemId);
			final Item item = Registry.ITEM.containsKey(registryName) ?
			                  Registry.ITEM.getOrDefault(registryName) : null;
			return getValidItems().contains(item)? item : null;
		}
		
		@Override protected Optional<ConfigValue<?>> buildConfigEntry(Builder builder) {
			if (parent.getRoot().type != net.minecraftforge.fml.config.ModConfig.Type.SERVER && tag != null)
				throw new IllegalArgumentException(
				  "Cannot use tag item filters in non-server config entry \"" + name + "\"");
			assert value.getRegistryName() != null;
			return Optional.of(decorate(builder).define(
			  name, value.getRegistryName().toString(), s ->
				 s instanceof String && fromId((String) s) != null));
		}
		
		@Override protected Optional<AbstractConfigListEntry<?>> buildGUIEntry(
		  ConfigEntryBuilder builder, ISimpleConfigEntryHolder c
		) {
			final DropdownMenuBuilder<Item> valBuilder = builder
			  .startDropdownMenu(
			    getDisplayName(), TopCellElementBuilder.ofItemObject(c.get(name)),
			    CellCreatorBuilder.ofItemObject())
			  .setDefaultValue(value)
			  .setSelections(
			    getValidItems().stream().sorted(
			      Comparator.comparing(Item::toString)
			    ).collect(Collectors.toCollection(LinkedHashSet::new)))
			  .setSaveConsumer(saveConsumer(c))
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(this::supplyError);
			return Optional.of(decorate(valBuilder).build());
		}
	}
}
