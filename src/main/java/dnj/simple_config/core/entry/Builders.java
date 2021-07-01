package dnj.simple_config.core.entry;

import dnj.simple_config.core.AbstractConfigEntry;
import dnj.simple_config.core.EntryListEntry;
import dnj.simple_config.core.annotation.Entry;
import dnj.simple_config.core.entry.SerializableEntry.SerializableConfigEntry;
import net.minecraft.item.Item;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static dnj.simple_config.core.ReflectionUtil.checkType;
import static dnj.simple_config.core.SimpleConfigClassParser.decorateListEntry;
import static dnj.simple_config.core.SimpleConfigClassParser.registerFieldParser;

/**
 * Contains builder methods for all the built-in config entry types
 */
@SuppressWarnings("unused")
public class Builders {
	
	// TODO: Key entry
	// TODO: NBT entry (?)
	// TODO: Block entry (Abstract registry entry?)
	
	// Basic types
	public static BooleanEntry bool(boolean value) {
		return new BooleanEntry(value);
	}
	public static NonPersistentBooleanEntry nonPersistentBool(boolean value) {
		return new NonPersistentBooleanEntry(value);
	}
	public static StringEntry string(String value) {
		return new StringEntry(value);
	}
	public static <E extends Enum<E>> EnumEntry<E> enum_(E value) {
		return new EnumEntry<>(value);
	}
	
	/**
	 * Unbound long value
	 */
	public static LongEntry number(long value) {
		return number(value, (Long) null, null);
	}
	/**
	 * Non-negative long between 0 and {@code max} (inclusive)
	 */
	public static LongEntry number(long value, @Nullable Integer max) {
		return number(value, 0L, max != null? max.longValue() : null);
	}
	/**
	 * Non-negative long between 0 and {@code max} (inclusive)
	 */
	public static LongEntry number(long value, @Nullable Long max) {
		return number(value, 0L, max);
	}
	/**
	 * Long value between {@code min} and {@code max} (inclusive)
	 */
	public static LongEntry number(long value, @Nullable Integer min, @Nullable Integer max) {
		return new LongEntry(value, min != null? min.longValue() : null, max != null? max.longValue() : null);
	}
	/**
	 * Long value between {@code min} and {@code max} (inclusive)
	 */
	public static LongEntry number(long value, @Nullable Long min, @Nullable Long max) {
		return new LongEntry(value, min, max);
	}
	/**
	 * Unbound double value
	 */
	public static DoubleEntry number(double value) {
		return number(value, null, null);
	}
	/**
	 * Non-negative double value between 0 and {@code max} (inclusive)
	 */
	public static DoubleEntry number(double value, @Nullable Number max) {
		return number(value, 0D, max);
	}
	/**
	 * Double value between {@code min} and {@code max} inclusive
	 */
	public static DoubleEntry number(double value, @Nullable Number min, @Nullable Number max) {
		return new DoubleEntry(value, min != null ? min.doubleValue() : null, max != null ? max.doubleValue() : null);
	}
	
	/**
	 * Double value between 0 and 1
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
	 */
	public static <T extends ISerializableConfigEntry<T>> SerializableEntry<T> entry(T value) {
		return new SerializableConfigEntry<>(value);
	}
	
	// Convenience Minecraft entries
	public static ItemEntry item(@Nullable Item value) {
		return new ItemEntry(value);
	}
	
	// List entries
	public static StringListEntry list(java.util.List<String> value) {
		return new StringListEntry(value);
	}
	/**
	 * Long list with elements between {@code min} and {@code max} (inclusive)
	 */
	public static LongListEntry list(java.util.List<Long> value, Long min, Long max) {
		return new LongListEntry(value, min, max);
	}
	/**
	 * Double list with elements between {@code min} and {@code max} (inclusive)
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
		  new BooleanEntry(value != null ? (Boolean) value : false));
		registerFieldParser(Entry.class, String.class, (a, field, value) ->
		  new StringEntry(value != null ? (String) value : ""));
		registerFieldParser(Entry.class, Enum.class, (a, field, value) -> {
			if (value == null)
				value = field.getType().getEnumConstants()[0];
			//noinspection rawtypes
			return new EnumEntry((Enum) value);
		});
		registerFieldParser(Entry.Long.class, Long.class, (a, field, value) -> {
			final LongEntry e = new LongEntry((Long) value, a.min(), a.max());
			if (a.slider())
				e.slider();
			return e;
		});
		registerFieldParser(Entry.Double.class, Double.class, (a, field, value) ->
		  new DoubleEntry((Double) value, a.min(), a.max()));
		registerFieldParser(Entry.Color.class, Color.class, (a, field, value) ->
		  a.alpha() ? new AlphaColorEntry((Color) value) : new ColorEntry((Color) value));
		
		// Lists
		registerFieldParser(Entry.List.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, String.class) ? null :
		  decorateListEntry(new StringListEntry((List<String>) value), field));
		
		registerFieldParser(Entry.List.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, Long.class) ? null :
		  decorateListEntry(new LongListEntry((List<Long>) value), field));
		registerFieldParser(Entry.List.Long.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, Long.class) ? null :
		  decorateListEntry(new LongListEntry((List<Long>) value, a.min(), a.max()), field));
		
		registerFieldParser(Entry.List.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, Double.class) ? null :
		  decorateListEntry(new DoubleListEntry((List<Double>) value), field));
		registerFieldParser(Entry.List.Double.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, Double.class) ? null :
		  decorateListEntry(new DoubleListEntry((List<Double>) value, a.min(), a.max()), field));
		
		registerFieldParser(Entry.NonPersistent.class, Boolean.class, (a, field, value) ->
		  new NonPersistentBooleanEntry(value != null? (Boolean) value : false));
	}
}
