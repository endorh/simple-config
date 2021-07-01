package dnj.simple_config.core;

import dnj.simple_config.core.SimpleConfig.InvalidConfigValueTypeException;
import dnj.simple_config.core.SimpleConfig.NoSuchConfigEntryError;

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
	 * Get a config value
	 * @param path Name or dot-separated path to the value
	 * @param <T> Expected type of the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type does not match the expected
	 * @see AbstractSimpleConfigEntryHolder#getBoolean(String)
	 * @see AbstractSimpleConfigEntryHolder#getInt(String)
	 * @see AbstractSimpleConfigEntryHolder#getLong(String)
	 * @see AbstractSimpleConfigEntryHolder#getFloat(String)
	 * @see AbstractSimpleConfigEntryHolder#getDouble(String)
	 */
	<T> T get(String path);
	
	/**
	 * Set a config value
	 * @param path Name or dot-separated path to the value
	 * @param value Value to be set
	 * @param <V> Type of the value
	 * @throws NoSuchConfigEntryError if the entry is not found
	 * @throws InvalidConfigValueTypeException if the entry's type does not match the expected
	 */
	<V> void set(String path, V value);
	
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
	 * The clean state is propagated to all of the children<br>
	 */
	void markDirty(boolean dirty);
	
	/**
	 * Get a boolean config value
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not boolean
	 * @see AbstractSimpleConfigEntryHolder#get(String)
	 */
	default boolean getBoolean(String path) {
		return this.<Boolean>get(path);
	}
	
	/**
	 * Get an int config value
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not int
	 * @see AbstractSimpleConfigEntryHolder#get(String)
	 */
	default int getInt(String path) {
		return this.<Long>get(path).intValue();
	}
	
	/**
	 * Get a long config value
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not long
	 * @see AbstractSimpleConfigEntryHolder#get(String)
	 */
	default long getLong(String path) {
		return this.<Long>get(path);
	}
	
	/**
	 * Get a float config value
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not float
	 * @see AbstractSimpleConfigEntryHolder#get(String)
	 */
	default float getFloat(String path) {
		return this.<Double>get(path).floatValue();
	}
	
	/**
	 * Get a double config value
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not double
	 * @see AbstractSimpleConfigEntryHolder#get(String)
	 */
	default double getDouble(String path) {
		return this.<Double>get(path);
	}
}
