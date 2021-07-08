package endorh.simple_config.core.entry;

import endorh.simple_config.core.AbstractConfigEntry;
import endorh.simple_config.core.AbstractConfigEntryBuilder;
import endorh.simple_config.core.EntryListEntry;
import endorh.simple_config.core.EntryListEntry.Builder;
import endorh.simple_config.core.StringToEntryMapEntry;
import endorh.simple_config.core.annotation.Entry;
import endorh.simple_config.core.annotation.HasAlpha;
import endorh.simple_config.core.annotation.Slider;
import net.minecraft.item.Item;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

import static endorh.simple_config.core.ReflectionUtil.checkType;
import static endorh.simple_config.core.SimpleConfigClassParser.*;

/**
 * Contains builder methods for all the built-in config entry types
 */
@SuppressWarnings("unused")
public class Builders {
	
	// Basic types
	public static BooleanEntry.Builder bool(boolean value) {
		return new BooleanEntry.Builder(value);
	}
	/**
	 * Generates an entry in the config GUI, but no entry in the config file<br>
	 * Modifications are only effective until restart
	 */
	public static NonPersistentBooleanEntry.Builder nonPersistentBool(boolean value) {
		return new NonPersistentBooleanEntry.Builder(value);
	}
	public static StringEntry.Builder string(String value) {
		return new StringEntry.Builder(value);
	}
	public static <E extends Enum<E>> EnumEntry.Builder<E> enum_(E value) {
		return new EnumEntry.Builder<>(value);
	}
	
	// Byte
	/**
	 * Unbound byte value
	 * @deprecated Use a bound int entry
	 */
	public static @Deprecated ByteEntry.Builder number(byte value) {
		return new ByteEntry.Builder(value);
	}
	/**
	 * Non-negative byte between 0 and {@code max} (inclusive)
	 * @deprecated Use a bound int entry
	 */
	public static @Deprecated ByteEntry.Builder number(byte value, byte max) {
		return number(value, (byte) 0, max);
	}
	/**
	 * Byte value between {@code min} and {@code max} (inclusive)
	 * @deprecated Use a bound int entry
	 */
	public static @Deprecated ByteEntry.Builder number(byte value, byte min, byte max) {
		return new ByteEntry.Builder(value).range(min, max);
	}
	
	// Short
	/**
	 * Unbound short value
	 * @deprecated Use a bound int entry
	 */
	public static @Deprecated ShortEntry.Builder number(short value) {
		return new ShortEntry.Builder(value);
	}
	/**
	 * Non-negative short between 0 and {@code max} (inclusive)
	 * @deprecated Use a bound int entry
	 */
	public static @Deprecated ShortEntry.Builder number(short value, short max) {
		return number(value, (short) 0, max);
	}
	/**
	 * Short value between {@code min} and {@code max} (inclusive)
	 * @deprecated Use a bound int entry
	 */
	public static @Deprecated ShortEntry.Builder number(short value, short min, short max) {
		return new ShortEntry.Builder(value).range(min, max);
	}
	
	// Int
	/**
	 * Unbound integer value
	 */
	public static IntegerEntry.Builder number(int value) {
		return new IntegerEntry.Builder(value);
	}
	/**
	 * Non-negative integer between 0 and {@code max} (inclusive)
	 */
	public static IntegerEntry.Builder number(int value, int max) {
		return number(value, 0, max);
	}
	/**
	 * Integer value between {@code min} and {@code max} (inclusive)
	 */
	public static IntegerEntry.Builder number(int value, int min, int max) {
		return new IntegerEntry.Builder(value).range(min, max);
	}
	
	// Long
	/**
	 * Unbound long value
	 */
	public static LongEntry.Builder number(long value) {
		return new LongEntry.Builder(value);
	}
	/**
	 * Non-negative long between 0 and {@code max} (inclusive)
	 */
	public static LongEntry.Builder number(long value, long max) {
		return number(value, 0L, max);
	}
	/**
	 * Long value between {@code min} and {@code max} (inclusive)
	 */
	public static LongEntry.Builder number(long value, long min, long max) {
		return new LongEntry.Builder(value).range(min, max);
	}
	
	// Float
	/**
	 * Unbound float value
	 */
	public static FloatEntry.Builder number(float value) {
		return new FloatEntry.Builder(value);
	}
	/**
	 * Non-negative float value between 0 and {@code max} (inclusive)
	 */
	public static FloatEntry.Builder number(float value, float max) {
		return number(value, 0F, max);
	}
	/**
	 * Float value between {@code min} and {@code max} inclusive
	 */
	public static FloatEntry.Builder number(float value, float min, float max) {
		return new FloatEntry.Builder(value).range(min, max);
	}
	
	// Double
	/**
	 * Unbound double value
	 */
	public static DoubleEntry.Builder number(double value) {
		return new DoubleEntry.Builder(value);
	}
	/**
	 * Non-negative double value between 0 and {@code max} (inclusive)
	 */
	public static DoubleEntry.Builder number(double value, double max) {
		return number(value, 0D, max);
	}
	/**
	 * Double value between {@code min} and {@code max} inclusive
	 */
	public static DoubleEntry.Builder number(double value, double min, double max) {
		return new DoubleEntry.Builder(value).range(min, max);
	}
	
	/**
	 * Float value between 0 and 1 (inclusive)
	 */
	public static FloatEntry.Builder fractional(float value) {
		if (0F > value || value > 1F)
			throw new IllegalArgumentException(
			  "Fraction values must be within [0, 1], passed " + value);
		return number(value, 0F, 1F);
	}
	
	/**
	 * Double value between 0 and 1 (inclusive)
	 */
	public static DoubleEntry.Builder fractional(double value) {
		if (0D > value || value > 1D)
			throw new IllegalArgumentException(
			  "Fraction values must be within [0, 1], passed " + value);
		return number(value, 0D, 1D);
	}
	
	/**
	 * Color
	 */
	public static ColorEntry.Builder color(Color value) {
		return new ColorEntry.Builder(value);
	}
	
	/**
	 * Regex Pattern entry<br>
	 * Will use the flags of the passed regex to compile user input<br>
	 */
	public static PatternEntry.Builder pattern(Pattern pattern) {
		return new PatternEntry.Builder(pattern);
	}
	
	/**
	 * Regex pattern entry with default flags
	 */
	public static PatternEntry.Builder pattern(String pattern) {
		return new PatternEntry.Builder(pattern);
	}
	
	/**
	 * Regex pattern
	 */
	public static PatternEntry.Builder pattern(String pattern, int flags) {
		return new PatternEntry.Builder(pattern, flags);
	}
	
	// String serializable entries
	
	/**
	 * Entry of a String serializable object
	 */
	public static <V> SerializableEntry.Builder<V> entry(
	  V value, Function<V, String> serializer, Function<String, Optional<V>> deserializer
	) {
		return new SerializableEntry.Builder<>(value, serializer, deserializer);
	}
	/**
	 * Entry of a String serializable object
	 */
	public static <V> SerializableEntry.Builder<V> entry(
	  V value, IConfigEntrySerializer<V> serializer
	) {
		return new SerializableEntry.Builder<>(value, serializer);
	}
	/**
	 * Entry of a String serializable object
	 */
	public static <V extends ISerializableConfigEntry<V>> SerializableEntry.Builder<V> entry(V value) {
		return new SerializableEntry.Builder<>(value, value.getConfigSerializer());
	}
	
	// Convenience Minecraft entries
	public static ItemEntry.Builder item(@Nullable Item value) {
		return new ItemEntry.Builder(value);
	}
	
	/**
	 * NBT entry that accepts any kind of NBT, either values or compounds
	 */
	public static INBTEntry.Builder nbtValue(INBT value) {
		return new INBTEntry.Builder(value);
	}
	
	/**
	 * NBT entry that accepts NBT compounds
	 */
	public static CompoundNBTEntry.Builder nbtTag(CompoundNBT value) {
		return new CompoundNBTEntry.Builder(value);
	}
	
	
	/**
	 * Generic resource location entry
	 */
	public static ResourceLocationEntry.Builder resource(String resourceName) {
		return new ResourceLocationEntry.Builder(new ResourceLocation(resourceName));
	}
	/**
	 * Generic resource location entry
	 */
	public static ResourceLocationEntry.Builder resource(ResourceLocation value) {
		return new ResourceLocationEntry.Builder(value);
	}
	
	// List entries
	
	/**
	 * String list
	 */
	public static StringListEntry.Builder stringList(java.util.List<String> value) {
		return new StringListEntry.Builder(value);
	}
	
	/**
	 * Byte list with elements between {@code min} and {@code max} (inclusive)<br>
	 * Null bounds are unbound
	 * @deprecated Use bound Integer lists
	 */
	@Deprecated public static ByteListEntry.Builder byteList(java.util.List<Byte> value) {
		return new ByteListEntry.Builder(value);
	}
	
	/**
	 * Short list with elements between {@code min} and {@code max} (inclusive)<br>
	 * Null bounds are unbound
	 * @deprecated Use bound Integer lists
	 */
	@Deprecated public static ShortListEntry.Builder shortList(java.util.List<Short> value) {
		return new ShortListEntry.Builder(value);
	}
	
	/**
	 * Integer list with elements between {@code min} and {@code max} (inclusive)<br>
	 * Null bounds are unbound
	 */
	public static IntegerListEntry.Builder intList(java.util.List<Integer> value) {
		return new IntegerListEntry.Builder(value);
	}
	/**
	 * Long list with elements between {@code min} and {@code max} (inclusive)<br>
	 * Null bounds are unbound
	 */
	public static LongListEntry.Builder longList(java.util.List<Long> value) {
		return new LongListEntry.Builder(value);
	}
	/**
	 * Float list with elements between {@code min} and {@code max} (inclusive)<br>
	 * Null bounds are unbound
	 */
	public static FloatListEntry.Builder floatList(java.util.List<Float> value) {
		return new FloatListEntry.Builder(value);
	}
	/**
	 * Double list with elements between {@code min} and {@code max} (inclusive)<br>
	 * Null bounds are unbound
	 */
	public static DoubleListEntry.Builder doubleList(java.util.List<Double> value) {
		return new DoubleListEntry.Builder(value);
	}
	
	// List of other entries
	
	/**
	 * List of other entries. Defaults to empty list<br>
	 * Non-persistent entries cannot be nested
	 */
	public static <V, C, G, E extends AbstractConfigEntry<V, C, G, E>,
	  B extends AbstractConfigEntryBuilder<V, C, G, E, B>>
	Builder<V, C, G, E, B> list(B entry) {
		return list(entry, Collections.emptyList());
	}
	
	/**
	 * List of other entries<br>
	 * Non-persistent entries cannot be nested
	 */
	public static <V, C, G, E extends AbstractConfigEntry<V, C, G, E>,
	  B extends AbstractConfigEntryBuilder<V, C, G, E, B>>
	EntryListEntry.Builder<V, C, G, E, B> list(B entry, List<V> value) {
		return new EntryListEntry.Builder<>(value, entry);
	}
	
	public static <K, V, C, G, E extends AbstractConfigEntry<V, C, G, E>,
	  B extends AbstractConfigEntryBuilder<V, C, G, E, B>,
	  KE extends AbstractConfigEntry<K, ?, ?, KE> & IAbstractStringKeyEntry<K>,
	  KB extends AbstractConfigEntryBuilder<K, ?, ?, KE, KB>>
	StringToEntryMapEntry.Builder<K, V, C, G, E, B, KE, KB> map(
	  KB keyEntry, B entry, Map<K, V> value
	) {
		return new StringToEntryMapEntry.Builder<>(value, keyEntry, entry);
	}
	
	public static <V, C, G, E extends AbstractConfigEntry<V, C, G, E>,
	  B extends AbstractConfigEntryBuilder<V, C, G, E, B>>
	StringToEntryMapEntry.Builder<String, V, C, G, E, B, StringEntry, StringEntry.Builder> map(
	  B entry, Map<String, V> value
	) {
		return new StringToEntryMapEntry.Builder<>(value, string(""), entry);
	}
	
	// Register reflection field parsers ------------------------------------------------------------
	
	static {
		registerFieldParser(Entry.class, Boolean.class, (a, field, value) ->
		  bool(value != null ? value : false));
		registerFieldParser(Entry.class, String.class, (a, field, value) ->
		  string(value != null ? value : ""));
		registerFieldParser(Entry.class, Enum.class, (a, field, value) -> {
			if (value == null)
				//noinspection rawtypes
				value = (Enum) field.getType().getEnumConstants()[0];
			return enum_(value);
		});
		registerFieldParser(Entry.class, Byte.class, (a, field, value) ->
		  number(value, getMin(field).byteValue(), getMax(field).byteValue())
		    .slider(field.isAnnotationPresent(Slider.class)));
		registerFieldParser(Entry.class, Short.class, (a, field, value) ->
		  number(value, getMin(field).shortValue(), getMax(field).shortValue())
		    .slider(field.isAnnotationPresent(Slider.class)));
		registerFieldParser(Entry.class, Integer.class, (a, field, value) ->
		  number(value, getMin(field).intValue(), getMax(field).intValue())
		    .slider(field.isAnnotationPresent(Slider.class)));
		registerFieldParser(Entry.class, Long.class, (a, field, value) ->
		  number(value, getMin(field).longValue(), getMax(field).longValue())
		    .slider(field.isAnnotationPresent(Slider.class)));
		registerFieldParser(Entry.class, Float.class, (a, field, value) ->
		  number(value, getMin(field).floatValue(), getMax(field).floatValue())
		    .slider(field.isAnnotationPresent(Slider.class)));
		registerFieldParser(Entry.class, Double.class, (a, field, value) ->
		  number(value, getMin(field).doubleValue(), getMax(field).doubleValue())
		    .slider(field.isAnnotationPresent(Slider.class)));
		registerFieldParser(Entry.class, Color.class, (a, field, value) ->
		  color(value).alpha(field.isAnnotationPresent(HasAlpha.class)));
		
		// Lists
		//noinspection unchecked
		registerFieldParser(Entry.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, String.class) ? null :
		  decorateListEntry(stringList((List<String>) value), field));
		//noinspection unchecked
		registerFieldParser(Entry.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, Integer.class) ? null :
		  decorateListEntry(
		    intList((List<Integer>) value)
		      .min(getMin(field).intValue()).max(getMax(field).intValue()), field));
		//noinspection unchecked
		registerFieldParser(Entry.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, Long.class) ? null :
		  decorateListEntry(
		    longList((List<Long>) value)
		      .min(getMin(field).longValue()).max(getMax(field).longValue()), field));
		//noinspection unchecked
		registerFieldParser(Entry.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, Float.class) ? null :
		  decorateListEntry(
		    floatList((List<Float>) value)
		      .min(getMin(field).floatValue()).max(getMax(field).floatValue()), field));
		//noinspection unchecked
		registerFieldParser(Entry.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, Double.class) ? null :
		  decorateListEntry(
		    doubleList((List<Double>) value)
		      .min(getMin(field).doubleValue()).max(getMax(field).doubleValue()), field));
		
		//noinspection unchecked
		registerFieldParser(Entry.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, Short.class) ? null :
		  decorateListEntry(
		    shortList((List<Short>) value)
		      .min(getMin(field).shortValue()).max(getMax(field).shortValue()), field));
		//noinspection unchecked
		registerFieldParser(Entry.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, Byte.class) ? null :
		  decorateListEntry(
		    byteList((List<Byte>) value)
		      .min(getMin(field).byteValue()).max(getMax(field).byteValue()), field));
		
		registerFieldParser(Entry.NonPersistent.class, Boolean.class, (a, field, value) ->
		  nonPersistentBool(value != null ? value : false));
		
		// Minecraft entry types
		registerFieldParser(Entry.class, Item.class, (a, field, value) -> item(value));
		registerFieldParser(Entry.class, INBT.class, (a, field, value) -> nbtValue(value));
		registerFieldParser(Entry.class, CompoundNBT.class, (a, field, value) -> nbtTag(value));
		registerFieldParser(Entry.class, ResourceLocation.class, (a, field, value) -> resource(value));
	}
}
