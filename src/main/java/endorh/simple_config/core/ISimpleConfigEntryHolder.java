package endorh.simple_config.core;

import endorh.simple_config.core.SimpleConfig.InvalidConfigValueTypeException;
import endorh.simple_config.core.SimpleConfig.NoSuchConfigEntryError;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * Abstract container for config entries<br>
 * Has a {@link SimpleConfig} as root, and can retrieve/set config values
 * and be marked as dirty
 */
public interface ISimpleConfigEntryHolder {
	
	/**
	 * Get the root config of this entry holder
	 */
	SimpleConfig getRoot();
	
	/**
	 * Get a config value<br>
	 * To retrieve a numeric primitive value use instead the variant methods
	 * @param path Name or dot-separated path to the value
	 * @param <T> Expected type of the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type does not match the expected
	 * @see ISimpleConfigEntryHolder#getBoolean(String)
	 * @see ISimpleConfigEntryHolder#getChar(String)
	 * @see ISimpleConfigEntryHolder#getByte(String)
	 * @see ISimpleConfigEntryHolder#getShort(String)
	 * @see ISimpleConfigEntryHolder#getInt(String)
	 * @see ISimpleConfigEntryHolder#getLong(String)
	 * @see ISimpleConfigEntryHolder#getFloat(String)
	 * @see ISimpleConfigEntryHolder#getDouble(String)
	 */
	<T> T get(String path);
	
	/**
	 * Get the current candidate value in the config GUI, or just the value
	 * if there's no config GUI active<br>
	 * To retrieve a numeric primitive value use instead the variant methods
	 * when reading from
	 * @param path Name or dot-separated path to the value
	 * @param <G> Expected GUI type of the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value does not match
	 *         the expected
	 */
	<G> G getGUI(String path);
	
	/**
	 * Internal generic setter<br>
	 * Use {@link ISimpleConfigEntryHolder#set(String, Object)} instead
	 * to benefit from a layer of primitive generics type safety
	 * @param path Name or dot-separated path to the value
	 * @param value Value to be set
	 * @param <V> The type of the value
	 * @deprecated Use {@link ISimpleConfigEntryHolder#set(String, Object)} instead
	 * to benefit from an extra layer of primitive generics type safety
	 */
	@Internal @Deprecated <V> void doSet(String path, V value);
	
	/**
	 * Set a config value
	 * @param path Name or dot-separated path to the value
	 * @param value Value to be set
	 * @param <V> Type of the value
	 * @throws NoSuchConfigEntryError if the entry is not found
	 * @throws InvalidConfigValueTypeException if the entry's type does not match the expected
	 */
	default <V> void set(String path, V value) {
		doSet(path, value);
	}
	
	/**
	 * Set a config value
	 * @param path Name or dot-separated path to the value
	 * @param value Value to be set
	 * @throws NoSuchConfigEntryError if the entry is not found
	 * @throws InvalidConfigValueTypeException if the entry's type does not match the expected
	 */
	default void set(String path, Float value) {
		try {
			doSet(path, value);
		} catch (InvalidConfigValueTypeException ignored) {
			set(path, value.doubleValue());
		}
	}
	
	/**
	 * Set a config value
	 * @param path Name or dot-separated path to the value
	 * @param value Value to be set
	 * @throws NoSuchConfigEntryError if the entry is not found
	 * @throws InvalidConfigValueTypeException if the entry's type does not match the expected
	 */
	default void set(String path, Long value) {
		try {
			doSet(path, value);
		} catch (InvalidConfigValueTypeException ignored) {
			set(path, value.doubleValue());
		}
	}
	
	/**
	 * Set a config value
	 * @param path Name or dot-separated path to the value
	 * @param value Value to be set
	 * @throws NoSuchConfigEntryError if the entry is not found
	 * @throws InvalidConfigValueTypeException if the entry's type does not match the expected
	 */
	default void set(String path, Integer value) {
		try {
			doSet(path, value);
		} catch (InvalidConfigValueTypeException ignored) {
			try {
				doSet(path, value.longValue());
			} catch (InvalidConfigValueTypeException ignored1) {
				set(path, value.floatValue());
			}
		}
	}
	
	/**
	 * Set a config value
	 * @param path Name or dot-separated path to the value
	 * @param value Value to be set
	 * @throws NoSuchConfigEntryError if the entry is not found
	 * @throws InvalidConfigValueTypeException if the entry's type does not match the expected
	 */
	default void set(String path, Short value) {
		try {
			doSet(path, value);
		} catch (InvalidConfigValueTypeException ignored) {
			set(path, value.intValue());
		}
	}
	
	/**
	 * Set a config value
	 * @param path Name or dot-separated path to the value
	 * @param value Value to be set
	 * @throws NoSuchConfigEntryError if the entry is not found
	 * @throws InvalidConfigValueTypeException if the entry's type does not match the expected
	 */
	default void set(String path, Byte value) {
		try {
			doSet(path, value);
		} catch (InvalidConfigValueTypeException ignored) {
			set(path, value.shortValue());
		}
	}
	
	/**
	 * Mark this entry holder as dirty<br>
	 * When the config screen is saved, only config files containing dirty entries are updated
	 */
	default ISimpleConfigEntryHolder markDirty() {
		markDirty(true);
		return this;
	}
	
	/**
	 * Mark this entry holder as dirty or clean<br>
	 * The dirty state is propagated to all ancestors<br>
	 * The clean state is propagated to all descendants<br>
	 */
	void markDirty(boolean dirty);
	
	/**
	 * Get a boolean config value
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not boolean
	 * @see ISimpleConfigEntryHolder#get(String)
	 */
	default boolean getBoolean(String path) {
		return this.<Boolean>get(path);
	}
	
	
	/**
	 * Get a char config value
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not int
	 * @see ISimpleConfigEntryHolder#get(String)
	 */
	default char getChar(String path) {
		return this.<Character>get(path);
	}
	
	/**
	 * Get a byte config value
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not int
	 * @see ISimpleConfigEntryHolder#get(String)
	 */
	default byte getByte(String path) {
		return this.<Number>get(path).byteValue();
	}
	
	/**
	 * Get a short config value
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not int
	 * @see ISimpleConfigEntryHolder#get(String)
	 */
	default short getShort(String path) {
		return this.<Number>get(path).shortValue();
	}
	
	/**
	 * Get an int config value
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not int
	 * @see ISimpleConfigEntryHolder#get(String)
	 */
	default int getInt(String path) {
		return this.<Number>get(path).intValue();
	}
	
	/**
	 * Get a long config value
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not long
	 * @see ISimpleConfigEntryHolder#get(String)
	 */
	default long getLong(String path) {
		return this.<Number>get(path).longValue();
	}
	
	/**
	 * Get a float config value
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not float
	 * @see ISimpleConfigEntryHolder#get(String)
	 */
	default float getFloat(String path) {
		return this.<Number>get(path).floatValue();
	}
	
	/**
	 * Get a double config value
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not double
	 * @see ISimpleConfigEntryHolder#get(String)
	 */
	default double getDouble(String path) {
		return this.<Number>get(path).doubleValue();
	}
	
	
	/**
	 * Get a candidate boolean config value from the GUI, or directly from
	 * the config if no GUI is active
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not boolean
	 * @see ISimpleConfigEntryHolder#getGUI(String)
	 */
	default boolean getGUIBoolean(String path) {
		return this.<Boolean>getGUI(path);
	}
	
	
	/**
	 * Get a candidate char config value from the GUI, or directly from
	 * the config if no GUI is active
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not int
	 * @see ISimpleConfigEntryHolder#getGUI(String)
	 */
	default char getGUIChar(String path) {
		return this.<Character>getGUI(path);
	}
	
	/**
	 * Get a candidate byte config value from the GUI, or directly from
	 * the config if no GUI is active
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not int
	 * @see ISimpleConfigEntryHolder#getGUI(String)
	 */
	default byte getGUIByte(String path) {
		return this.<Number>getGUI(path).byteValue();
	}
	
	/**
	 * Get a candidate short config value from the GUI, or directly from
	 * the config if no GUI is active
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not int
	 * @see ISimpleConfigEntryHolder#getGUI(String)
	 */
	default short getGUIShort(String path) {
		return this.<Number>get(path).shortValue();
	}
	
	/**
	 * Get an candidate int config value from the GUI, or directly from
	 * the config if no GUI is active
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not int
	 * @see ISimpleConfigEntryHolder#getGUI(String)
	 */
	default int getGUIInt(String path) {
		return this.<Number>getGUI(path).intValue();
	}
	
	/**
	 * Get a candidate long config value from the GUI, or directly from
	 * the config if no GUI is active
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not long
	 * @see ISimpleConfigEntryHolder#getGUI(String)
	 */
	default long getGUILong(String path) {
		return this.<Number>getGUI(path).longValue();
	}
	
	/**
	 * Get a candidate float config value from the GUI, or directly from
	 * the config if no GUI is active
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not float
	 * @see ISimpleConfigEntryHolder#getGUI(String)
	 */
	default float getGUIFloat(String path) {
		return this.<Number>getGUI(path).floatValue();
	}
	
	/**
	 * Get a candidate double config value from the GUI, or directly from
	 * the config if no GUI is active
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not double
	 * @see ISimpleConfigEntryHolder#getGUI(String)
	 */
	default double getGUIDouble(String path) {
		return this.<Number>getGUI(path).doubleValue();
	}
}
