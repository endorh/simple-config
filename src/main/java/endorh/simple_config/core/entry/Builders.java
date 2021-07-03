package endorh.simple_config.core.entry;

import endorh.simple_config.core.AbstractConfigEntry;
import endorh.simple_config.core.EntryListEntry;
import endorh.simple_config.core.annotation.Entry;
import endorh.simple_config.core.annotation.HasAlpha;
import endorh.simple_config.core.annotation.Slider;
import endorh.simple_config.core.entry.SerializableEntry.SerializableConfigEntry;
import net.minecraft.item.Item;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static endorh.simple_config.core.ReflectionUtil.checkType;
import static endorh.simple_config.core.SimpleConfigClassParser.*;

/**
 * Contains builder methods for all the built-in config entry types
 */
@SuppressWarnings("unused")
public class Builders {
	
	// Basic types
	public static BooleanEntry bool(boolean value) {
		return new BooleanEntry(value);
	}
	/**
	 * Generates an entry in the config GUI, but no entry in the config file<br>
	 * Modifications are only effective until restart
	 */
	public static NonPersistentBooleanEntry nonPersistentBool(boolean value) {
		return new NonPersistentBooleanEntry(value);
	}
	public static StringEntry string(String value) {
		return new StringEntry(value);
	}
	public static <E extends Enum<E>> EnumEntry<E> enum_(E value) {
		return new EnumEntry<>(value);
	}
	
	// Byte
	/**
	 * Unbound byte value
	 * @deprecated Use a bound int entry
	 */
	public static @Deprecated ByteEntry number(byte value) {
		return new ByteEntry(value, null, null);
	}
	/**
	 * Non-negative byte between 0 and {@code max} (inclusive)
	 * @deprecated Use a bound int entry
	 */
	public static @Deprecated ByteEntry number(byte value, byte max) {
		return number(value, (byte) 0, max);
	}
	/**
	 * Byte value between {@code min} and {@code max} (inclusive)
	 * @deprecated Use a bound int entry
	 */
	public static @Deprecated ByteEntry number(byte value, byte min, byte max) {
		return new ByteEntry(value, min, max);
	}
	
	// Short
	/**
	 * Unbound short value
	 * @deprecated Use a bound int entry
	 */
	public static @Deprecated ShortEntry number(short value) {
		return new ShortEntry(value, null, null);
	}
	/**
	 * Non-negative short between 0 and {@code max} (inclusive)
	 * @deprecated Use a bound int entry
	 */
	public static @Deprecated ShortEntry number(short value, short max) {
		return number(value, (short) 0, max);
	}
	/**
	 * Short value between {@code min} and {@code max} (inclusive)
	 * @deprecated Use a bound int entry
	 */
	public static @Deprecated ShortEntry number(short value, short min, short max) {
		return new ShortEntry(value, min, max);
	}
	
	// Int
	/**
	 * Unbound integer value
	 */
	public static IntegerEntry number(int value) {
		return new IntegerEntry(value, null, null);
	}
	/**
	 * Non-negative integer between 0 and {@code max} (inclusive)
	 */
	public static IntegerEntry number(int value, int max) {
		return number(value, 0, max);
	}
	/**
	 * Integer value between {@code min} and {@code max} (inclusive)
	 */
	public static IntegerEntry number(int value, int min, int max) {
		return new IntegerEntry(value, min, max);
	}
	
	// Long
	/**
	 * Unbound long value
	 */
	public static LongEntry number(long value) {
		return new LongEntry(value, null, null);
	}
	/**
	 * Non-negative long between 0 and {@code max} (inclusive)
	 */
	public static LongEntry number(long value, long max) {
		return number(value, 0L, max);
	}
	/**
	 * Long value between {@code min} and {@code max} (inclusive)
	 */
	public static LongEntry number(long value, long min, long max) {
		return new LongEntry(value, min, max);
	}
	
	// Float
	/**
	 * Unbound float value
	 */
	public static FloatEntry number(float value) {
		return new FloatEntry(value, null, null);
	}
	/**
	 * Non-negative float value between 0 and {@code max} (inclusive)
	 */
	public static FloatEntry number(float value, float max) {
		return number(value, 0F, max);
	}
	/**
	 * Float value between {@code min} and {@code max} inclusive
	 */
	public static FloatEntry number(float value, float min, float max) {
		return new FloatEntry(value, min, max);
	}
	
	// Double
	/**
	 * Unbound double value
	 */
	public static DoubleEntry number(double value) {
		return new DoubleEntry(value, null, null);
	}
	/**
	 * Non-negative double value between 0 and {@code max} (inclusive)
	 */
	public static DoubleEntry number(double value, double max) {
		return number(value, 0D, max);
	}
	/**
	 * Double value between {@code min} and {@code max} inclusive
	 */
	public static DoubleEntry number(double value, double min, double max) {
		return new DoubleEntry(value, min, max);
	}
	
	/**
	 * Float value between 0 and 1 (inclusive)
	 */
	public static FloatEntry fractional(float value) {
		if (0F > value || value > 1F)
			throw new IllegalArgumentException(
			  "Fraction values must be within [0, 1], passed " + value);
		return number(value, 0F, 1F);
	}
	
	/**
	 * Double value between 0 and 1 (inclusive)
	 */
	public static DoubleEntry fractional(double value) {
		if (0D > value || value > 1D)
			throw new IllegalArgumentException(
			  "Fraction values must be within [0, 1], passed " + value);
		return number(value, 0D, 1D);
	}
	
	/**
	 * Color without alpha component
	 */
	public static ColorEntry color(Color value) {
		return color(value, false);
	}
	/**
	 * Color with or without alpha component
	 */
	public static ColorEntry color(Color value, boolean alpha) {
		return alpha ? new AlphaColorEntry(value) : new ColorEntry(value);
	}
	
	// String serializable entries
	
	/**
	 * Entry of a String serializable object
	 */
	public static <T> SerializableEntry<T> entry(
	  T value, Function<T, String> serializer, Function<String, Optional<T>> deserializer
	) {
		return new SerializableEntry<>(value, serializer, deserializer);
	}
	/**
	 * Entry of a String serializable object
	 * @param typeClass Actual type of the entry, required in the backing field
	 */
	public static <T> SerializableEntry<T> entry(
	  T value, Function<T, String> serializer, Function<String, Optional<T>> deserializer,
	  Class<?> typeClass
	) {
		return new SerializableEntry<>(value, serializer, deserializer, typeClass);
	}
	/**
	 * Entry of a String serializable object
	 */
	public static <T extends ISerializableConfigEntry<T>> SerializableEntry<T> entry(T value) {
		return new SerializableConfigEntry<>(value);
	}
	
	// Convenience Minecraft entries
	public static ItemEntry item(@Nullable Item value) {
		return new ItemEntry(value);
	}
	
	/**
	 * NBT entry that accepts any kind of NBT, either values or compounds
	 */
	public static INBTEntry nbt(INBT value) {
		return new INBTEntry(value);
	}
	
	/**
	 * NBT entry that accepts NBT compounds
	 */
	public static CompoundNBTEntry nbt(CompoundNBT value) {
		return new CompoundNBTEntry(value);
	}
	
	
	/**
	 * Generic resource location entry
	 */
	public static ResourceLocationEntry resource(String resourceName) {
		return new ResourceLocationEntry(new ResourceLocation(resourceName));
	}
	/**
	 * Generic resource location entry
	 */
	public static ResourceLocationEntry resource(ResourceLocation value) {
		return new ResourceLocationEntry(value);
	}
	
	// List entries
	
	/**
	 * String list
	 */
	public static StringListEntry list(java.util.List<String> value) {
		return new StringListEntry(value);
	}
	
	/**
	 * Byte list with elements between {@code min} and {@code max} (inclusive)<br>
	 * Null bounds are unbound
	 * @deprecated Use bound Integer lists
	 */
	@Deprecated public static ByteListEntry list(java.util.List<Byte> value, Byte min, Byte max) {
		return new ByteListEntry(value, min, max);
	}
	
	/**
	 * Short list with elements between {@code min} and {@code max} (inclusive)<br>
	 * Null bounds are unbound
	 * @deprecated Use bound Integer lists
	 */
	@Deprecated public static ShortListEntry list(java.util.List<Short> value, Short min, Short max) {
		return new ShortListEntry(value, min, max);
	}
	
	/**
	 * Integer list with elements between {@code min} and {@code max} (inclusive)<br>
	 * Null bounds are unbound
	 */
	public static IntegerListEntry list(java.util.List<Integer> value, Integer min, Integer max) {
		return new IntegerListEntry(value, min, max);
	}
	/**
	 * Long list with elements between {@code min} and {@code max} (inclusive)<br>
	 * Null bounds are unbound
	 */
	public static LongListEntry list(java.util.List<Long> value, Long min, Long max) {
		return new LongListEntry(value, min, max);
	}
	/**
	 * Float list with elements between {@code min} and {@code max} (inclusive)<br>
	 * Null bounds are unbound
	 */
	public static FloatListEntry list(java.util.List<Float> value, Float min, Float max) {
		return new FloatListEntry(value, min, max);
	}
	/**
	 * Double list with elements between {@code min} and {@code max} (inclusive)<br>
	 * Null bounds are unbound
	 */
	public static DoubleListEntry list(java.util.List<Double> value, Double min, Double max) {
		return new DoubleListEntry(value, min, max);
	}
	
	// List of other entries
	/**
	 * List of other entries. Defaults to empty list<br>
	 * Non-persistent entries cannot be nested
	 */
	public static <V, C, G, E extends AbstractConfigEntry<V, C, G, E>>
	EntryListEntry<V, C, G, E> list(AbstractConfigEntry<V, C, G, E> entry) {
		return list(entry, new ArrayList<>());
	}
	/**
	 * List of other entries<br>
	 * Non-persistent entries cannot be nested
	 */
	public static <V, C, G, E extends AbstractConfigEntry<V, C, G, E>>
	EntryListEntry<V, C, G, E> list(AbstractConfigEntry<V, C, G, E> entry, List<V> value) {
		return new EntryListEntry<>(value, entry);
	}
	
	// Register reflection field parsers ------------------------------------------------------------
	
	static {
		registerFieldParser(Entry.class, Boolean.class, (a, field, value) ->
		  new BooleanEntry(value != null ? value : false));
		registerFieldParser(Entry.class, String.class, (a, field, value) ->
		  new StringEntry(value != null ? value : ""));
		registerFieldParser(Entry.class, Enum.class, (a, field, value) -> {
			if (value == null)
				//noinspection rawtypes
				value = (Enum) field.getType().getEnumConstants()[0];
			//noinspection rawtypes
			return new EnumEntry(value);
		});
		registerFieldParser(Entry.class, Byte.class, (a, field, value) -> {
			//noinspection deprecation
			final ByteEntry e = new ByteEntry(value, getMin(field).byteValue(), getMax(field).byteValue());
			if (field.isAnnotationPresent(Slider.class))
				e.slider();
			return e;
		});
		registerFieldParser(Entry.class, Short.class, (a, field, value) -> {
			//noinspection deprecation
			final ShortEntry e = new ShortEntry(value, getMin(field).shortValue(), getMax(field).shortValue());
			if (field.isAnnotationPresent(Slider.class))
				e.slider();
			return e;
		});
		registerFieldParser(Entry.class, Integer.class, (a, field, value) -> {
			final IntegerEntry e = new IntegerEntry(value, getMin(field).intValue(), getMax(field).intValue());
			if (field.isAnnotationPresent(Slider.class))
				e.slider();
			return e;
		});
		registerFieldParser(Entry.class, Long.class, (a, field, value) -> {
			final LongEntry e = new LongEntry(value, getMin(field).longValue(), getMax(field).longValue());
			if (field.isAnnotationPresent(Slider.class))
				e.slider();
			return e;
		});
		registerFieldParser(Entry.class, Float.class, (a, field, value) -> {
			final FloatEntry e = new FloatEntry(value, getMin(field).floatValue(), getMax(field).floatValue());
			if (field.isAnnotationPresent(Slider.class))
				e.slider();
			return e;
		});
		registerFieldParser(Entry.class, Double.class, (a, field, value) -> {
			final DoubleEntry e = new DoubleEntry(value, getMin(field).doubleValue(), getMax(field).doubleValue());
			if (field.isAnnotationPresent(Slider.class))
				e.slider();
			return e;
		});
		registerFieldParser(Entry.class, Color.class, (a, field, value) ->
		  field.isAnnotationPresent(HasAlpha.class)
		  ? new AlphaColorEntry(value) : new ColorEntry(value));
		
		// Lists
		//noinspection unchecked
		registerFieldParser(Entry.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, String.class) ? null :
		  decorateListEntry(new StringListEntry((List<String>) value), field));
		//noinspection unchecked
		registerFieldParser(Entry.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, Integer.class) ? null :
		  decorateListEntry(new IntegerListEntry((List<Integer>) value, getMin(field).intValue(), getMax(field).intValue()), field));
		//noinspection unchecked
		registerFieldParser(Entry.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, Long.class) ? null :
		  decorateListEntry(new LongListEntry((List<Long>) value, getMin(field).longValue(), getMax(field).longValue()), field));
		//noinspection unchecked
		registerFieldParser(Entry.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, Float.class) ? null :
		  decorateListEntry(new FloatListEntry((List<Float>) value, getMin(field).floatValue(), getMax(field).floatValue()), field));
		//noinspection unchecked
		registerFieldParser(Entry.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, Double.class) ? null :
		  decorateListEntry(new DoubleListEntry((List<Double>) value, getMin(field).doubleValue(), getMax(field).doubleValue()), field));
		
		//noinspection unchecked
		registerFieldParser(Entry.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, Short.class) ? null :
		  decorateListEntry(new ShortListEntry((List<Short>) value), field));
		//noinspection unchecked
		registerFieldParser(Entry.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, Byte.class) ? null :
		  decorateListEntry(new ByteListEntry((List<Byte>) value), field));
		
		registerFieldParser(Entry.NonPersistent.class, Boolean.class, (a, field, value) ->
		  new NonPersistentBooleanEntry(value != null ? value : false));
		
		// Minecraft entry types
		registerFieldParser(Entry.class, Item.class, (a, field, value) ->
		  new ItemEntry(value));
		registerFieldParser(Entry.class, INBT.class, (a, field, value) ->
		  new INBTEntry(value));
		registerFieldParser(Entry.class, CompoundNBT.class, (a, field, value) ->
		  new CompoundNBTEntry(value));
		registerFieldParser(Entry.class, ResourceLocation.class, (a, field, value) ->
		  new ResourceLocationEntry(value));
	}
}
