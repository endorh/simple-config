package dnj.simple_config.core;

import com.google.common.base.CaseFormat;
import dnj.simple_config.core.SimpleConfig.IGUIEntry;
import dnj.simple_config.core.SimpleConfig.InvalidConfigValueTypeException;
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
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.fml.config.ModConfig.Type;
import org.apache.commons.lang3.NotImplementedException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// TODO: Generify adding a backing and GUI type parameters
/**
 * An abstract config entry, which may or may not produce an entry in
 * the actual config and/or the config GUI<br>
 * Entries should not be accessed at all by API users after
 * their config has been registered. Doing so will result in
 * undefined behaviour.<br>
 * In particular, users can not modify the default
 * value/bounds/validators of an entry after the registering phase
 * has ended.<br>
 * Subclasses may override {@link Entry#buildConfigEntry}
 * and {@link Entry#buildGUIEntry} to generate the appropriate
 * entries in both ends
 *
 * @param <T> The type of the value held by the entry
 */
public abstract class Entry<T, E extends Entry<T, E>> implements IGUIEntry {
	protected String name = null;
	protected String translation = null;
	protected String tooltip = null;
	protected String comment = null;
	protected boolean requireRestart = false;
	@Nullable protected Function<T, Optional<ITextComponent>> errorSupplier = null;
	@Nullable protected Function<T, Optional<ITextComponent[]>> tooltipSupplier = null;
	protected final T value;
	protected @Nullable Field backingField;
	protected AbstractSimpleConfigEntryHolder parent;
	
	protected void setParent(AbstractSimpleConfigEntryHolder config) {
		this.parent = config;
	}
	
	// Builder methods
	public static class Builders {
		public static StringEntry string(String value) {
			return new StringEntry(value);
		}
		public static <E extends Enum<E>> EnumEntry<E> enum_(E value) {
			return new EnumEntry<>(value);
		}
		public static ColorEntry color(Color value) { return color(value, false); }
		public static ColorEntry color(Color value, boolean alpha) {
			return alpha? new AlphaColorEntry(value) : new ColorEntry(value);
		}
		public static BooleanEntry bool(boolean value) {
			return new BooleanEntry(value);
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
		public static <T> SerializableEntry<T> entry(T value, Function<T, String> serializer, Function<String, Optional<T>> deserializer) {
			return new SerializableEntry<>(value, serializer, deserializer);
		}
		public static <T extends ISerializableConfigEntry<T>> SerializableEntry<T> entry(T value) {
			return new SerializableConfigEntry<>(value);
		}
		
		// List entries
		public static StringListEntry list(List<String> value) {
			return new StringListEntry(value);
		}
		public static LongListEntry list(List<Long> value, Long min, Long max) {
			return new LongListEntry(value, null, min, max);
		}
		public static DoubleListEntry list(List<Double> value, Double min, Double max) {
			return new DoubleListEntry(value, null, min, max);
		}
		
		public static ItemEntry item(@Nullable Item value) {
			return new ItemEntry(value);
		}
	}
	
	public Entry(T value) {
		this.value = value;
	}
	
	@SuppressWarnings("unchecked")
	protected E castThis() {
		return (E) this;
	}
	
	protected E name(String name) {
		this.name = name;
		return castThis();
	}
	
	@SuppressWarnings("UnusedReturnValue")
	protected E translate(String translation) {
		this.translation = translation;
		return castThis();
	}
	
	@SuppressWarnings("UnusedReturnValue")
	protected E tooltip(String translation) {
		this.tooltip = translation;
		return castThis();
	}
	
	public E tooltip(Function<T, Optional<ITextComponent[]>> tooltipSupplier) {
		this.tooltipSupplier = tooltipSupplier;
		return castThis();
	}
	
	@SuppressWarnings("UnusedReturnValue")
	public E error(Function<T, Optional<ITextComponent>> errorSupplier) {
		this.errorSupplier = errorSupplier;
		return castThis();
	}
	
	public E restart(boolean requireRestart) {
		this.requireRestart = requireRestart;
		return castThis();
	}
	
	protected ITextComponent getDisplayName() {
		if (translation != null)
			return new TranslationTextComponent(translation);
		return new StringTextComponent(name);
	}
	
	protected Consumer<T> saveConsumer(AbstractSimpleConfigEntryHolder c) {
		return t -> {
			if (!t.equals(c.get(name)))
				c.markDirty().set(name, t);
		};
	}
	
	@OnlyIn(Dist.CLIENT)
	protected Optional<ITextComponent[]> supplyTooltip(T value) {
		if (tooltipSupplier != null)
			return tooltipSupplier.apply(value);
		if (tooltip != null && I18n.hasKey(tooltip))
			return Optional.of(
			  Arrays.stream(I18n.format(tooltip).split("\n"))
			    .map(StringTextComponent::new).toArray(ITextComponent[]::new));
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
	
	public Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.empty();
	}
	
	/**
	 * Generate an {@link AbstractConfigListEntry} to be added to the GUI
	 * @param builder Entry builder
	 * @param config Config holder
	 */
	@OnlyIn(Dist.CLIENT)
	public Optional<AbstractConfigListEntry<?>> buildGUIEntry(
	  ConfigEntryBuilder builder, AbstractSimpleConfigEntryHolder config
	) {
		return Optional.empty();
	}
	
	/**
	 * Add entry to the GUI<br>
	 * Subclasses should instead override {@link Entry#buildGUIEntry} in most cases
	 */
	@OnlyIn(Dist.CLIENT)
	@Override public void buildGUI(
	  ConfigCategory category, ConfigEntryBuilder entryBuilder, AbstractSimpleConfigEntryHolder config
	) {
		buildGUIEntry(entryBuilder, config).ifPresent(category::addEntry);
	}
	
	/**
	 * Get the value held by this entry
	 *
	 * @param spec Config spec to look into
	 * @throws InvalidConfigValueTypeException if the found value type does not match the expected
	 */
	public T get(ConfigValue<?> spec) {
		try {
			//noinspection unchecked
			return (T) spec.get();
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(name, e);
		}
	}
	
	public <S> void set(ConfigValue<S> spec, T value) {
		try {
			//noinspection unchecked
			spec.set((S) value);
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(name, e);
		}
	}
	
	public static class BooleanEntry extends Entry<Boolean, BooleanEntry> {
		protected Function<Boolean, ITextComponent> yesNoSupplier = null;
		
		public BooleanEntry(boolean value) {
			super(value);
		}
		
		@Override
		public Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
			return Optional.of(decorate(builder).define(name, value.booleanValue()));
		}
		
		public BooleanEntry displayAs(Function<Boolean, ITextComponent> displayAdapter) {
			this.yesNoSupplier = displayAdapter;
			return this;
		}
		
		@OnlyIn(Dist.CLIENT)
		@Override
		public Optional<AbstractConfigListEntry<?>> buildGUIEntry(
		  ConfigEntryBuilder builder, AbstractSimpleConfigEntryHolder c
		) {
			final BooleanToggleBuilder valBuilder = builder
			  .startBooleanToggle(getDisplayName(), c.get(name))
			  .setDefaultValue(value)
			  .setSaveConsumer(saveConsumer(c))
			  .setTooltipSupplier(this::supplyTooltip)
			  .setYesNoTextSupplier(yesNoSupplier)
			  .setErrorSupplier(errorSupplier);
			return Optional.of(decorate(valBuilder).build());
		}
	}
	
	public static abstract class RangedEntry<T, E extends RangedEntry<T, E>> extends Entry<T, E> {
		public T min;
		public T max;
		protected boolean asSlider = false;
		
		public RangedEntry(T value, T min, T max) {
			super(value);
			this.min = min;
			this.max = max;
		}
		
		@SuppressWarnings("unchecked")
		protected E castThis() {
			return (E) this;
		}
		
		E min(T min) {
			this.min = min;
			return castThis();
		}
		
		E max(T max) {
			this.max = max;
			return castThis();
		}
	}
	
	public static class LongEntry extends RangedEntry<Long, LongEntry> {
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
		
		@Override
		public Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
			return Optional.of(decorate(builder).defineInRange(name, value, min, max));
		}
		
		@OnlyIn(Dist.CLIENT)
		@Override
		public Optional<AbstractConfigListEntry<?>> buildGUIEntry(
		  ConfigEntryBuilder builder, AbstractSimpleConfigEntryHolder c
		) {
			if (!asSlider) {
				final LongFieldBuilder valBuilder = builder
				  .startLongField(getDisplayName(), c.get(name))
				  .setDefaultValue(value)
				  .setMin(min).setMax(max)
				  .setSaveConsumer(saveConsumer(c))
				  .setTooltipSupplier(this::supplyTooltip)
				  .setErrorSupplier(errorSupplier);
				return Optional.of(decorate(valBuilder).build());
			} else {
				final LongSliderBuilder valBuilder = builder
				  .startLongSlider(getDisplayName(), c.get(name), min, max)
				  .setDefaultValue(value)
				  .setSaveConsumer(saveConsumer(c))
				  .setTooltipSupplier(this::supplyTooltip)
				  .setTooltipSupplier(this::supplyTooltip)
				  .setErrorSupplier(errorSupplier);
				return Optional.of(decorate(valBuilder).build());
			}
		}
	}
	
	public static class DoubleEntry extends RangedEntry<Double, DoubleEntry> {
		public DoubleEntry(double value, Double min, Double max) {
			super(value,
			      min == null ? Double.NEGATIVE_INFINITY : min,
			      max == null ? Double.POSITIVE_INFINITY : max);
		}
		
		@Override
		public Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
			return Optional.of(decorate(builder).defineInRange(name, value, min, max));
		}
		
		@OnlyIn(Dist.CLIENT)
		@Override
		public Optional<AbstractConfigListEntry<?>> buildGUIEntry(
		  ConfigEntryBuilder builder, AbstractSimpleConfigEntryHolder c
		) {
			if (!asSlider) {
				final DoubleFieldBuilder valBuilder = builder
				  .startDoubleField(getDisplayName(), c.get(name))
				  .setDefaultValue(value)
				  .setMin(min).setMax(max)
				  .setSaveConsumer(saveConsumer(c))
				  .setTooltipSupplier(this::supplyTooltip)
				  .setErrorSupplier(errorSupplier);
				return Optional.of(decorate(valBuilder).build());
			} else //noinspection CommentedOutCode
			{
				throw new NotImplementedException(
				  "Double slider entries are not implemented");
				/*
				final DoubleSliderBuilder valBuilder = builder
				  .startDoubleSlider(getDisplayName(), config.get(name), min, max)
				  .setDefaultValue(value)
				  .setSaveConsumer(saveConsumer(c))
				  .setTooltip(getTooltip())
				  .setErrorSupplier(errorSupplier);
				return decorate(valBuilder).build();
				*/
			}
		}
	}
	
	public static class StringEntry extends Entry<String, StringEntry> {
		public StringEntry(String value) {super(value);}
		
		@Override
		public Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
			return Optional.of(decorate(builder).define(name, value));
		}
		
		@OnlyIn(Dist.CLIENT)
		@Override
		public Optional<AbstractConfigListEntry<?>> buildGUIEntry(
		  ConfigEntryBuilder builder, AbstractSimpleConfigEntryHolder c
		) {
			final TextFieldBuilder valBuilder = builder
			  .startTextField(getDisplayName(), c.get(name))
			  .setDefaultValue(value)
			  .setSaveConsumer(saveConsumer(c))
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(errorSupplier);
			return Optional.of(decorate(valBuilder).build());
		}
	}
	
	public static class EnumEntry<E extends Enum<E>> extends Entry<E, EnumEntry<E>> {
		public Class<E> enumClass;
		
		public EnumEntry(E value) {
			super(value);
			enumClass = value.getDeclaringClass();
		}
		
		@Override
		public Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
			return Optional.of(decorate(builder).defineEnum(name, value));
		}
		
		@OnlyIn(Dist.CLIENT)
		public ITextComponent enumName(E item, SimpleConfig config) {
			if (item instanceof ITranslatedEnum)
				return ((ITranslatedEnum) item).getDisplayName();
			final String key =
			  config.modId + ".config.enum." +
			  CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, enumClass.getSimpleName()) +
			  "." + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_UNDERSCORE, item.name());
			if (I18n.hasKey(key))
				return new TranslationTextComponent(key);
			return new StringTextComponent(item.name());
		}
		
		@OnlyIn(Dist.CLIENT)
		@Override
		public Optional<AbstractConfigListEntry<?>> buildGUIEntry(
		  ConfigEntryBuilder builder, AbstractSimpleConfigEntryHolder c
		) {
			final EnumSelectorBuilder<E> valBuilder = builder
			  .startEnumSelector(getDisplayName(), enumClass, c.get(name))
			  .setDefaultValue(value)
			  .setSaveConsumer(saveConsumer(c))
			  .setTooltipSupplier(this::supplyTooltip);
			//noinspection unchecked
			valBuilder.setEnumNameProvider(e -> enumName((E) e, c.getRoot()));
			return Optional.of(decorate(valBuilder).build());
		}
	}
	
	public interface ITranslatedEnum {
		ITextComponent getDisplayName();
	}
	
	public static abstract class ListEntry<T, E extends ListEntry<T, E>> extends Entry<List<T>, E> {
		public Predicate<T> validator;
		protected boolean expand;
		
		public ListEntry(@Nullable List<T> value) {
			this(value, null);
		}
		
		public ListEntry(
		  @Nullable List<T> value, @Nullable Predicate<T> validator
		) {
			super(value != null? value : new ArrayList<>());
			this.validator = validator != null? validator : t -> true;
		}
		
		@SuppressWarnings("unchecked")
		protected E castThis() {
			return (E) this;
		}
		
		public E setValidator(Predicate<T> validator) {
			this.validator = validator;
			return castThis();
		}
		
		public E expand() {
			return expand(true);
		}
		public E expand(boolean expand) {
			this.expand = expand;
			return castThis();
		}
		
		protected boolean validateElement(Object o) {
			try {
				//noinspection unchecked
				T t = (T) o;
				return validator.test(t);
			} catch (ClassCastException ignored) {
				return false;
			}
		}
		
		// TODO: Make validators return Optional<ITextComponent> too
		protected Optional<ITextComponent> supplyError(List<T> value) {
			for (int i = 0; i < value.size(); i++) {
				T elem = value.get(i);
				if (!validator.test(elem))
					return Optional.of(new TranslationTextComponent(
					  "config.simple-config.error.list_element_does_not_match_validator", i));
			}
			return errorSupplier != null ? errorSupplier.apply(value) : Optional.empty();
		}
		
		@Override
		public Optional<ConfigValue<?>> buildConfigEntry(
		  ForgeConfigSpec.Builder builder
		) {
			return Optional.of(decorate(builder).defineList(
			  name, value, this::validateElement));
		}
	}
	
	public static abstract class RangedListEntry<T extends Comparable<T>, E extends RangedListEntry<T, E>> extends ListEntry<T, E> {
		public T min;
		public T max;
		
		public RangedListEntry(
		  @Nullable List<T> value, @Nullable Predicate<T> validator,
		  @Nonnull T min, @Nonnull T max
		) {
			super(value, clamp(validator, min, max));
			this.min = min;
			this.max = max;
		}
		
		public E min(@Nonnull T min) {
			this.min = min;
			setValidator(clamp(validator, min, max));
			return castThis();
		}
		
		public E max(@Nonnull T max) {
			this.max = max;
			setValidator(clamp(validator, min, max));
			return castThis();
		}
		
		public static <T extends Comparable<T>> Predicate<T> clamp(
		  @Nullable Predicate<T> validator, T min, T max
		) {
			final Predicate<T> nonNullValidator = validator != null? validator : t -> true;
			return t -> t.compareTo(min) >= 0 && t.compareTo(max) <= 0 && nonNullValidator.test(t);
		}
	}
	
	public static class LongListEntry extends RangedListEntry<Long, LongListEntry> {
		public LongListEntry(@Nullable List<Long> value) {
			this(value, null, null, null);
		}
		public LongListEntry(@Nullable List<Long> value, @Nullable Long min, @Nullable Long max) {
			this(value, null, min, max);
		}
		public LongListEntry(@Nullable List<Long> value, @Nullable Predicate<Long> validator) {
			this(value, validator, null, null);
		}
		public LongListEntry(
		  @Nullable List<Long> value, @Nullable Predicate<Long> validator,
		  @Nullable Long min, @Nullable Long max
		) {
			super(value, validator, min != null? min : Long.MIN_VALUE, max != null? max : Long.MAX_VALUE);
		}
		
		@Override public List<Long> get(ConfigValue<?> spec) {
			// Sometimes Forge returns lists of subtypes, but Cloth can't cast them
			//noinspection unchecked
			return ((List<Number>) (List<?>) super.get(spec))
			  .stream().map(Number::longValue).collect(Collectors.toList());
		}
		
		@OnlyIn(Dist.CLIENT)
		@Override
		public Optional<AbstractConfigListEntry<?>> buildGUIEntry(
		  ConfigEntryBuilder builder, AbstractSimpleConfigEntryHolder c
		) {
			final LongListBuilder valBuilder = builder
			  .startLongList(getDisplayName(), c.get(name))
			  .setDefaultValue(value)
			  .setMin(min).setMax(max)
			  .setSaveConsumer(saveConsumer(c))
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(this::supplyError);
			return Optional.of(decorate(valBuilder).build());
		}
	}
	
	public static class DoubleListEntry extends RangedListEntry<Double, DoubleListEntry> {
		public DoubleListEntry(@Nullable List<Double> value) {
			this(value, null, null, null);
		}
		public DoubleListEntry(@Nullable List<Double> value, @Nullable Double min, @Nullable Double max) {
			this(value, null, min, max);
		}
		public DoubleListEntry(@Nullable List<Double> value, @Nullable Predicate<Double> validator) {
			this(value, validator, null, null);
		}
		public DoubleListEntry(
		  @Nullable List<Double> value, @Nullable Predicate<Double> validator,
		  @Nullable Double min, @Nullable Double max
		) {
			super(value, validator, min != null? min : Double.NEGATIVE_INFINITY, max != null? max : Double.POSITIVE_INFINITY);
		}
		
		@Override
		public List<Double> get(ConfigValue<?> spec) {
			// Sometimes forge returns lists of subtypes, but Cloth can't cast them
			//noinspection unchecked
			return ((List<Number>) (List<?>) super.get(spec))
			  .stream().map(Number::doubleValue).collect(Collectors.toList());
		}
		
		@OnlyIn(Dist.CLIENT)
		@Override
		public Optional<AbstractConfigListEntry<?>> buildGUIEntry(
		  ConfigEntryBuilder builder, AbstractSimpleConfigEntryHolder c
		) {
			final DoubleListBuilder valBuilder = builder
			  .startDoubleList(getDisplayName(), c.get(name))
			  .setDefaultValue(value)
			  .setMin(min).setMax(max)
			  .setSaveConsumer(saveConsumer(c))
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(this::supplyError);
			return Optional.of(decorate(valBuilder).build());
		}
	}
	
	public static class StringListEntry extends ListEntry<String, StringListEntry> {
		public StringListEntry(List<String> value) {
			super(value);
		}
		public StringListEntry(@Nullable List<String> value, @Nullable Predicate<String> validator) {
			super(value, validator);
		}
		
		@OnlyIn(Dist.CLIENT)
		@Override
		public Optional<AbstractConfigListEntry<?>> buildGUIEntry(
		  ConfigEntryBuilder builder, AbstractSimpleConfigEntryHolder c
		) {
			final StringListBuilder valBuilder = builder
			  .startStrList(getDisplayName(), c.get(name))
			  .setDefaultValue(value)
			  .setSaveConsumer(l -> {
				  if (!l.equals(c.get(name))) c.markDirty().set(name, l);
			  })
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(this::supplyError);
			return Optional.of(decorate(valBuilder).build());
		}
	}
	
	public static class EmptyEntry extends Entry<Void, EmptyEntry> {
		public EmptyEntry() {
			super(null);
		}
	}
	
	public static class TextEntry extends EmptyEntry {
		public final Supplier<ITextComponent> translation;
		
		public TextEntry(Supplier<ITextComponent> supplier) {
			this.translation = supplier;
		}
		
		public TextEntry() {
			this.translation = () -> new TranslationTextComponent(super.translation);
		}
		
		@OnlyIn(Dist.CLIENT)
		@Override
		public Optional<AbstractConfigListEntry<?>> buildGUIEntry(
		  ConfigEntryBuilder builder, AbstractSimpleConfigEntryHolder c
		) {
			final TextDescriptionBuilder valBuilder = builder
			  .startTextDescription(translation.get());
			return Optional.of(decorate(valBuilder).build());
		}
	}
	
	/**
	 * Doesn't have a GUI<br>
	 * To create your custom type of entry with GUI,
	 * extend this class or {@link Entry} directly
	 *
	 * @param <T> Type of the value
	 */
	@SuppressWarnings("unused")
	public static class SerializableEntry<T> extends Entry<T, SerializableEntry<T>> {
		public Function<T, String> serializer;
		public Function<String, Optional<T>> deserializer;
		
		@SuppressWarnings("unused")
		public SerializableEntry(
		  T value,
		  Function<T, String> serializer,
		  Function<String, Optional<T>> deserializer
		) {
			super(value);
			this.serializer = serializer;
			this.deserializer = deserializer;
		}
		
		@Override
		public Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
			return Optional.of(
			  builder.define(
				 name, serializer.apply(value),
				 s -> deserializer.apply((String) s).isPresent()));
		}
		
		@Override
		public T get(ConfigValue<?> spec) {
			return deserializer.apply((String) spec.get()).orElse(null);
		}
		
		@Override
		public <S> void set(ConfigValue<S> spec, T value) {
			try {
				//noinspection unchecked
				final ConfigValue<String> str = (ConfigValue<String>) spec;
				str.set(serializer.apply(value));
			} catch (ClassCastException e) {
				throw new InvalidConfigValueTypeException(name, e);
			}
		}
	}
	
	public interface ISerializableConfigEntry<T extends ISerializableConfigEntry<T>> {
		IConfigEntrySerializer<T> getConfigSerializer();
	}
	
	public interface IConfigEntrySerializer<T> {
		String serialize(T value);
		Optional<T> deserialize(String value);
	}
	
	public static class SerializableConfigEntry<T extends ISerializableConfigEntry<T>> extends SerializableEntry<T> {
		public SerializableConfigEntry(T value) {
			super(value, null, null);
			final IConfigEntrySerializer<T> serializer = value.getConfigSerializer();
			this.serializer = serializer::serialize;
			this.deserializer = serializer::deserialize;
		}
	}
	
	// Colors are stored as Strings in the config, but used as Integers in the GUI
	public static class ColorEntry extends Entry<Color, ColorEntry> {
		public ColorEntry(Color value) {
			super(value);
		}
		
		@Override
		public Color get(ConfigValue<?> spec) {
			try {
				//noinspection unchecked
				final Color c = fromHex(((ConfigValue<String>) spec).get());
				return c != null? c : value;
			} catch (ClassCastException e) {
				throw new InvalidConfigValueTypeException(name, e);
			}
		}
		
		@Override
		public <S> void set(ConfigValue<S> spec, Color value) {
			try {
				//noinspection unchecked
				((ConfigValue<String>) spec).set(asHex(value));
			} catch (ClassCastException e) {
				throw new InvalidConfigValueTypeException(name, e);
			}
		}
		
		public static String asHex(Color color) {
			return String.format("#%06X", color.getRGB() & 0xFFFFFF);
		}
		
		protected static final Pattern COLOR_PATTERN = Pattern.compile(
		  "\\s*(?:0x|#)(?i)(?<color>[0-9a-f]{3}|[0-9a-f]{6})\\s*");
		public static @Nullable Color fromHex(String color) {
			final Matcher m = COLOR_PATTERN.matcher(color);
			if (m.matches()) {
				String c = m.group("color");
				if (c.length() == 3)
					c = doubleChars(c);
				return new Color((int) Long.parseLong(c.toLowerCase(), 0x10));
			}
			return null;
		}
		protected static String doubleChars(String s) {
			StringBuilder r = new StringBuilder();
			for (char ch : s.toCharArray())
				r.append(ch);
			return r.toString();
		}
		
		@Override
		public Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
			return Optional.of(decorate(builder).define(
			  name, asHex(value), s -> (s instanceof String? fromHex((String) s) : null) != null));
		}
		
		@OnlyIn(Dist.CLIENT)
		@Override
		public Optional<AbstractConfigListEntry<?>> buildGUIEntry(
		  ConfigEntryBuilder builder, AbstractSimpleConfigEntryHolder c
		) {
			final int prev = c.<Color>get(name).getRGB() & 0xFFFFFF;
			final ColorFieldBuilder valBuilder = builder
			  .startColorField(getDisplayName(), prev)
			  .setDefaultValue(value.getRGB() & 0xFFFFFF)
			  .setSaveConsumer(i -> {
			  	  if (i != prev) c.markDirty().set(name, new Color(i));
			  })
			  .setTooltipSupplier(i -> supplyTooltip(new Color(i)));
			if (errorSupplier != null)
				valBuilder.setErrorSupplier(i -> errorSupplier.apply(new Color(i)));
			return Optional.of(decorate(valBuilder).build());
		}
	}
	
	public static class AlphaColorEntry extends ColorEntry {
		public AlphaColorEntry(Color value) {
			super(value);
		}
		
		@Override
		public Color get(ConfigValue<?> spec) {
			try {
				//noinspection unchecked
				final Color c = fromHex(((ConfigValue<String>) spec).get());
				return c != null? c : value;
			} catch (ClassCastException e) {
				throw new InvalidConfigValueTypeException(name, e);
			}
		}
		
		@Override
		public <S> void set(ConfigValue<S> spec, Color value) {
			try {
				//noinspection unchecked
				((ConfigValue<String>) spec).set(asHex(value));
			} catch (ClassCastException e) {
				throw new InvalidConfigValueTypeException(name, e);
			}
		}
		
		public static String asHex(Color color) {
			return String.format("#%08X", color.getRGB());
		}
		
		protected static final Pattern ALPHA_COLOR_PATTERN = Pattern.compile(
		  "\\s*(?:0x|#)(?i)(?<color>[0-9a-f]{3,4}|[0-9a-f]{6}|[0-9a-f]{8})\\s*");
		public static @Nullable Color fromHex(String s) {
			final Matcher m = ALPHA_COLOR_PATTERN.matcher(s);
			if (m.matches()) {
				String c = m.group("color");
				if (c.length() < 6)
					c = doubleChars(c);
				return new Color((int) Long.parseLong(c.toLowerCase(), 0x10), true);
			}
			return null;
		}
		
		@Override
		public Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
			return Optional.of(decorate(builder).define(
			  name, asHex(value), s -> (s instanceof String? fromHex((String) s) : null) != null));
		}
		
		@OnlyIn(Dist.CLIENT)
		@Override
		public Optional<AbstractConfigListEntry<?>> buildGUIEntry(
		  ConfigEntryBuilder builder,
		  AbstractSimpleConfigEntryHolder c
		) {
			final int prev = c.<Color>get(name).getRGB();
			final ColorFieldBuilder valBuilder = builder
			  .startAlphaColorField(getDisplayName(), prev)
			  .setDefaultValue(value.getRGB())
			  .setSaveConsumer(i -> {
				  if (i != prev) c.markDirty().set(name, new Color(i));
			  })
			  .setTooltipSupplier(i -> supplyTooltip(new Color(i)));
			if (errorSupplier != null)
				valBuilder.setErrorSupplier(i -> errorSupplier.apply(new Color(i, true)));
			return Optional.of(decorate(valBuilder).build());
		}
	}
	
	public static class ItemEntry extends Entry<Item, ItemEntry> {
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
		
		@Override public Item get(ConfigValue<?> spec) {
			try {
				//noinspection unchecked
				final Item item = fromId(((ConfigValue<String>) spec).get());
				return item != null? item : value;
			} catch (InvalidConfigValueTypeException e) {
				return value;
			}
		}
		
		@Override public <S> void set(ConfigValue<S> spec, Item value) {
			try {
				((ConfigValue<String>) spec).set(value.getRegistryName().toString());
			} catch (InvalidConfigValueTypeException ignored) {}
		}
		
		protected @Nullable Item fromId(String itemId) {
			final ResourceLocation registryName = new ResourceLocation(itemId);
			final Item item = Registry.ITEM.containsKey(registryName) ?
			                  Registry.ITEM.getOrDefault(registryName) : null;
			return getValidItems().contains(item)? item : null;
		}
		
		@Override public Optional<ConfigValue<?>> buildConfigEntry(Builder builder) {
			if (parent.root.type != Type.SERVER && tag != null)
				throw new IllegalArgumentException(
				  "Cannot use tag item filters in non-server config entry \"" + name + "\"");
			return Optional.of(decorate(builder).define(
			  name, value.getRegistryName().toString(), s ->
				 (s instanceof String ? fromId((String) s) : null) != null));
		}
		
		@Override public Optional<AbstractConfigListEntry<?>> buildGUIEntry(
		  ConfigEntryBuilder builder, AbstractSimpleConfigEntryHolder c
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
			  .setErrorSupplier(errorSupplier);
			return Optional.of(decorate(valBuilder).build());
		}
	}
}
