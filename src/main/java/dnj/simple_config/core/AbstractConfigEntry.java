package dnj.simple_config.core;

import dnj.simple_config.core.SimpleConfig.IGUIEntry;
import dnj.simple_config.core.entry.*;
import dnj.simple_config.core.entry.SerializableEntry.SerializableConfigEntry;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.*;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.util.text.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

import javax.annotation.Nullable;
import java.awt.Color;
import java.lang.reflect.Field;
import java.util.List;
import java.util.*;
import java.util.function.*;

/**
 * An abstract config entry, which may or may not produce an entry in
 * the actual config and/or the config GUI<br>
 * Entries should not be accessed by API users after
 * their config has been registered. Doing so will result in
 * undefined behaviour.<br>
 * In particular, users can not modify the default
 * value/bounds/validators of an entry after the registering phase
 * has ended.<br>
 * Subclasses may override {@link AbstractConfigEntry#buildConfigEntry}
 * and {@link AbstractConfigEntry#buildGUIEntry} to generate the appropriate
 * entries in both ends
 *
 * @param <V> The type of the value held by the entry
 * @param <Config> The type of the associated config entry
 * @param <Gui> The type of the associated GUI config entry
 * @param <Self> The actual subtype of this entry to be
 *              returned by builder-like methods
 */
public abstract class AbstractConfigEntry<V, Config, Gui, Self extends AbstractConfigEntry<V, Config, Gui, Self>>
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
		public static <V, C, G, E extends AbstractConfigEntry<V, C, G, E>>
		EntryListEntry<V, C, G, E> list(AbstractConfigEntry<V, C, G, E> entry) {
			return list(entry, new ArrayList<>());
		}
		public static <V, C, G, E extends AbstractConfigEntry<V, C, G, E>>
		EntryListEntry<V, C, G, E> list(AbstractConfigEntry<V, C, G, E> entry, List<V> value) {
			return new EntryListEntry<>(value, entry);
		}
	}
	
	protected AbstractConfigEntry(V value) {
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
	 * Subclasses should instead override {@link AbstractConfigEntry#buildGUIEntry} in most cases
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
	protected void addTranslationsDebugSuffix(List<ITextComponent> tooltip) {
		tooltip.add(new StringTextComponent(" "));
		tooltip.add(new StringTextComponent(" ⚠ Translation debug mode active").mergeStyle(TextFormatting.GOLD));
		tooltip.add(new StringTextComponent("     Remember to remove the call to .debugTranslations()").mergeStyle(TextFormatting.GOLD));
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
