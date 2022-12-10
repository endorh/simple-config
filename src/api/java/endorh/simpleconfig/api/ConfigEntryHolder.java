package endorh.simpleconfig.api;

import endorh.simpleconfig.api.SimpleConfig.*;
import endorh.simpleconfig.api.ui.ConfigScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract container for config entries<br>
 * Has a {@link SimpleConfig} as root, and can retrieve/set config values
 * and be marked as dirty
 */
public interface ConfigEntryHolder {
	
	/**
	 * Get the root config of this entry holder
	 */
	@NotNull SimpleConfig getRoot();
	
	/**
	 * Get the parent of this entry holder.
	 * Will throw {@link NoSuchConfigGroupError} if it has no parent.
	 */
	default @NotNull ConfigEntryHolder getParent() {
		throw new NoSuchConfigGroupError("");
	}
	
	/**
	 * Get the current configuration GUI of this holder, if any.
	 */
	@OnlyIn(Dist.CLIENT)
	@Nullable ConfigScreen getGUI();
	
	/**
	 * Check if this holder has an associated active configuration GUI.
	 */
	default boolean hasGUI() {
		if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) return false;
		return getGUI() != null;
	}
	
	/**
	 * Get a child entry holder<br>
	 * @param path Name or dot-separated path to the child
	 *             If null or empty, {@code this} will be returned
	 * @throws NoSuchConfigGroupError if the child is not found
	 * @return A child {@link ConfigEntryHolder},
	 *         or {@code this} if path is null or empty.
	 */
	@NotNull ConfigEntryHolder getChild(String path);
	
	/**
	 * Get a config value<br>
	 * To retrieve a numeric primitive value use instead the variant methods
	 * @param path Name or dot-separated path to the value
	 * @param <T> Expected type of the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @see #getBoolean(String)
	 * @see #getChar(String)
	 * @see #getByte(String)
	 * @see #getShort(String)
	 * @see #getInt(String)
	 * @see #getLong(String)
	 * @see #getFloat(String)
	 * @see #getDouble(String)
	 */
	<T> T get(String path);
	
	/**
	 * Set a config value in the GUI, translating it from its actual type.<br>
	 * @param path Name or dot-separated path to the value
	 * @param value Value to be set
	 * @param <V> Type of the value
	 * @throws NoSuchConfigEntryError if the entry is not found
	 * @throws InvalidConfigValueTypeException if the entry's type does not match the expected
	 */
	<V> void setForGUI(String path, V value);
	
	/**
	 * Set a config value in the GUI, translating it from its actual type<br>
	 * Numeric types are upcast as needed
	 * @param path Name or dot-separated path to the value
	 * @param number Value to be set
	 * @throws NoSuchConfigEntryError if the entry is not found
	 * @throws InvalidConfigValueTypeException if the entry's type does not match the expected
	 */
	void setForGUI(String path, Number number);
	
	/**
	 * Check if an entry has a currently active config GUI.
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 */
	boolean hasGUI(String path);
	
	/**
	 * Get the current candidate value in the config GUI, or just the value
	 * if there's no config GUI active<br>
	 * To retrieve a numeric primitive value use instead the variant methods
	 * when reading from
	 * @param path Name or dot-separated path to the value
	 * @param <G> Expected GUI type of the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @see #getGUIBoolean(String)
	 * @see #getGUIByte(String)
	 * @see #getGUIShort(String)
	 * @see #getGUIInt(String)
	 * @see #getGUILong(String)
	 * @see #getGUIFloat(String)
	 * @see #getGUIDouble(String)
	 * @see #getGUIChar(String)
	 */
	<G> G getGUI(String path);
	
	/**
	 * Get the current candidate value in the config GUI, translated to the actual
	 * type of this entry (if they're different).<br>
	 * To retrieve a numeric primitive value use instead the variant methods.
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @see #getBooleanFromGUI(String)
	 * @see #getByteFromGUI(String)
	 * @see #getShortFromGUI(String)
	 * @see #getIntFromGUI(String)
	 * @see #getLongFromGUI(String)
	 * @see #getFloatFromGUI(String)
	 * @see #getDoubleFromGUI(String)
	 * @see #getCharFromGUI(String)
	 */
	<V> V getFromGUI(String path);
	
	/**
	 * Set a config value<br>
	 * Numeric values are upcast as needed
	 * @param path Name or dot-separated path to the value
	 * @param value Value to be set
	 * @param <V> Type of the value
	 * @throws NoSuchConfigEntryError if the entry is not found
	 * @throws InvalidConfigValueTypeException if the entry's type does not match the expected
	 * @throws InvalidConfigValueException if the value is invalid for this entry
	 */
	<V> void set(String path, V value);
	
	/**
	 * Mark this entry holder as dirty<br>
	 * When the config screen is saved, only config files containing dirty entries are updated
	 * @see #isDirty
	 */
	default ConfigEntryHolder markDirty() {
		markDirty(true);
		return this;
	}
	
	/**
	 * Mark this entry holder as dirty or clean<br>
	 * The dirty state is propagated to all ancestors<br>
	 * The clean state is propagated to all descendants<br>
	 * @see #isDirty
	 */
	void markDirty(boolean dirty);
	
	/**
	 * Returns {@code true} if marked as dirty.
	 * @see #markDirty
	 */
	boolean isDirty();
	
	/**
	 * Get a boolean config value
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not boolean
	 * @see #get(String)
	 */
	default boolean getBoolean(String path) {
		try {
			return this.<Boolean>get(path);
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}
	
	
	/**
	 * Get a char config value
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not int
	 * @see #get(String)
	 */
	default char getChar(String path) {
		try {
			return this.<Character>get(path);
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}
	
	/**
	 * Get a byte config value
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not int
	 * @see #get(String)
	 */
	default byte getByte(String path) {
		try {
			return this.<Number>get(path).byteValue();
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}
	
	/**
	 * Get a short config value
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not int
	 * @see #get(String)
	 */
	default short getShort(String path) {
		try {
			return this.<Number>get(path).shortValue();
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}
	
	/**
	 * Get an int config value
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not int
	 * @see #get(String)
	 */
	default int getInt(String path) {
		try {
			return this.<Number>get(path).intValue();
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}
	
	/**
	 * Get a long config value
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not long
	 * @see #get(String)
	 */
	default long getLong(String path) {
		try {
			return this.<Number>get(path).longValue();
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}
	
	/**
	 * Get a float config value
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not float
	 * @see #get(String)
	 */
	default float getFloat(String path) {
		try {
			return this.<Number>get(path).floatValue();
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}
	
	/**
	 * Get a double config value
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not double
	 * @see #get(String)
	 */
	default double getDouble(String path) {
		try {
			return this.<Number>get(path).doubleValue();
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}
	
	
	/**
	 * Get a baked config value.<br>
	 * Different from {@link #get(String)} only if the entry defines any
	 * baking transformation.<br>
	 * To retrieve a numeric primitive value use instead the variant methods
	 *
	 * @param path Name or dot-separated path to the value
	 * @param <T> Expected type of the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @see #getBakedBoolean(String)
	 * @see #getBakedChar(String)
	 * @see #getBakedByte(String)
	 * @see #getBakedShort(String)
	 * @see #getBakedInt(String)
	 * @see #getBakedLong(String)
	 * @see #getBakedFloat(String)
	 * @see #getBakedDouble(String)
	 */
	<T> T getBaked(String path);
	
	/**
	 * Get a baked boolean config value
	 *
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not boolean
	 * @see #getBaked(String)
	 */
	default boolean getBakedBoolean(String path) {
		try {
			return this.<Boolean>getBaked(path);
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}
	
	/**
	 * Get a baked char config value
	 *
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not int
	 * @see #getBaked(String)
	 */
	default char getBakedChar(String path) {
		try {
			return this.<Character>getBaked(path);
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}
	
	/**
	 * Get a baked byte config value
	 *
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not int
	 * @see #getBaked(String)
	 */
	default byte getBakedByte(String path) {
		try {
			return this.<Number>getBaked(path).byteValue();
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}
	
	/**
	 * Get a baked short config value
	 *
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not int
	 * @see #getBaked(String)
	 */
	default short getBakedShort(String path) {
		try {
			return this.<Number>getBaked(path).shortValue();
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}
	
	/**
	 * Get a baked int config value
	 *
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not int
	 * @see #getBaked(String)
	 */
	default int getBakedInt(String path) {
		try {
			return this.<Number>getBaked(path).intValue();
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}
	
	/**
	 * Get a baked long config value
	 *
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not long
	 * @see #getBaked(String)
	 */
	default long getBakedLong(String path) {
		try {
			return this.<Number>getBaked(path).longValue();
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}
	
	/**
	 * Get a baked float config value
	 *
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not float
	 * @see #getBaked(String)
	 */
	default float getBakedFloat(String path) {
		try {
			return this.<Number>getBaked(path).floatValue();
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}
	
	/**
	 * Get a baked double config value
	 *
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not double
	 * @see #getBaked(String)
	 */
	default double getBakedDouble(String path) {
		try {
			return this.<Number>getBaked(path).doubleValue();
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}
	
	/**
	 * Set a config value in its baked domain.<br>
	 * This is only possible if the entry defines an invertible baking transformation,
	 * or no baking transformation at all.<br>
	 * Numeric values are upcast as needed
	 *
	 * @param path Name or dot-separated path to the value
	 * @param value Value to be set
	 * @param <V> Type of the value
	 * @throws NoSuchConfigEntryError if the entry is not found
	 * @throws InvalidConfigValueTypeException if the entry's type does not match the expected
	 * @throws UnInvertibleBakingTransformationException if the entry's baking transformation is not invertible
	 * @throws InvalidConfigValueException if the value is invalid for this entry
	 */
	<V> void setBaked(String path, V value);
	
	
	/**
	 * Get a candidate boolean config value from the GUI, or directly from
	 * the config if no GUI is active
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not boolean
	 * @see #getGUI(String)
	 */
	default boolean getGUIBoolean(String path) {
		try {
			return this.<Boolean>getGUI(path);
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}
	
	
	/**
	 * Get a candidate char config value from the GUI, or directly from
	 * the config if no GUI is active
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not int
	 * @see #getGUI(String)
	 */
	default char getGUIChar(String path) {
		try {
			return this.<Character>getGUI(path);
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}
	
	/**
	 * Get a candidate byte config value from the GUI, or directly from
	 * the config if no GUI is active
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not int
	 * @see #getGUI(String)
	 */
	default byte getGUIByte(String path) {
		try {
			return this.<Number>getGUI(path).byteValue();
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}
	
	/**
	 * Get a candidate short config value from the GUI, or directly from
	 * the config if no GUI is active
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not int
	 * @see #getGUI(String)
	 */
	default short getGUIShort(String path) {
		try {
			return this.<Number>getGUI(path).shortValue();
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}
	
	/**
	 * Get a candidate int config value from the GUI, or directly from
	 * the config if no GUI is active
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not int
	 * @see #getGUI(String)
	 */
	default int getGUIInt(String path) {
		try {
			return this.<Number>getGUI(path).intValue();
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}
	
	/**
	 * Get a candidate long config value from the GUI, or directly from
	 * the config if no GUI is active
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not long
	 * @see #getGUI(String)
	 */
	default long getGUILong(String path) {
		try {
			return this.<Number>getGUI(path).longValue();
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}
	
	/**
	 * Get a candidate float config value from the GUI, or directly from
	 * the config if no GUI is active
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not float
	 * @see #getGUI(String)
	 */
	default float getGUIFloat(String path) {
		try {
			return this.<Number>getGUI(path).floatValue();
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}
	
	/**
	 * Get a candidate double config value from the GUI, or directly from
	 * the config if no GUI is active
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not double
	 * @see #getGUI(String)
	 */
	default double getGUIDouble(String path) {
		try {
			return this.<Number>getGUI(path).doubleValue();
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}
	
	/**
	 * Get a candidate boolean config value from the GUI, translated to the
	 * actual type of the entry (if they're different).<br>
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not boolean
	 * @see #getFromGUI(String)
	 */
	default boolean getBooleanFromGUI(String path) {
		try {
			return this.<Boolean>getFromGUI(path);
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}
	
	/**
	 * Get a candidate byte config value from the GUI, translated to the
	 * actual type of the entry (if they're different).<br>
	  @param path Name or dot-separated path to the value
	  @throws NoSuchConfigEntryError if the value is not found
	  @throws InvalidConfigValueTypeException if the value type is not byte
	  @see #getFromGUI(String)
	 */
	default byte getByteFromGUI(String path) {
		try {
			return this.<Number>getFromGUI(path).byteValue();
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}

	/**
	 * Get a candidate short config value from the GUI, translated to the
	 * actual type of the entry (if they're different).<br>
	  @param path Name or dot-separated path to the value
	  @throws NoSuchConfigEntryError if the value is not found
	  @throws InvalidConfigValueTypeException if the value type is not short
	  @see #getFromGUI(String)
	 */
	default short getShortFromGUI(String path) {
		try {
			return this.<Number>getFromGUI(path).shortValue();
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}

	/**
	 * Get a candidate int config value from the GUI, translated to the
	 * actual type of the entry (if they're different).<br>
	  @param path Name or dot-separated path to the value
	  @throws NoSuchConfigEntryError if the value is not found
	  @throws InvalidConfigValueTypeException if the value type is not int
	  @see #getFromGUI(String)
	 */
	default int getIntFromGUI(String path) {
		try {
			return this.<Number>getFromGUI(path).intValue();
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}
	
	/**
	 * Get a candidate long config value from the GUI, translated to the
	 * actual type of the entry (if they're different).<br>
	  @param path Name or dot-separated path to the value
	  @throws NoSuchConfigEntryError if the value is not found
	  @throws InvalidConfigValueTypeException if the value type is not long
	  @see #getFromGUI(String)
	 */
	default long getLongFromGUI(String path) {
		try {
			return this.<Number>getFromGUI(path).longValue();
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}
	/**
	 * Get a candidate float config value from the GUI, translated to the
	 * actual type of the entry (if they're different).<br>
	  @param path Name or dot-separated path to the value
	  @throws NoSuchConfigEntryError if the value is not found
	  @throws InvalidConfigValueTypeException if the value type is not float
	  @see #getFromGUI(String)
	 */
	default float getFloatFromGUI(String path) {
		try {
			return this.<Number>getFromGUI(path).floatValue();
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}

	/**
	 * Get a candidate double config value from the GUI, translated to the
	 * actual type of the entry (if they're different).<br>
	  @param path Name or dot-separated path to the value
	  @throws NoSuchConfigEntryError if the value is not found
	  @throws InvalidConfigValueTypeException if the value type is not double
	  @see #getFromGUI(String)
	 */
	default double getDoubleFromGUI(String path) {
		try {
			return this.<Number>getFromGUI(path).doubleValue();
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}
	/**
	 * Get a candidate char config value from the GUI, translated to the
	 * actual type of the entry (if they're different).<br>
	  @param path Name or dot-separated path to the value
	  @throws NoSuchConfigEntryError if the value is not found
	  @throws InvalidConfigValueTypeException if the value type is not char
	  @see #getFromGUI(String)
	 */
	default char getCharFromGUI(String path) {
		try {
			return this.<Character>getFromGUI(path);
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}
	
	
	/**
	 * Set a config value in the GUI
	 * @param path Name or dot-separated path to the value
	 * @param value Value to be set
	 * @param <G> Type of the value in the GUI
	 * @throws NoSuchConfigEntryError if the entry is not found
	 * @throws InvalidConfigValueTypeException if the entry's type does not match the expected
	 */
	<G> void setGUI(String path, G value);
	
	/**
	 * Reset all entries to their default values.
	 */
	void reset();
	
	/**
	 * Reset an entry or group of entries to its default value.
	 * @param path Name or dot-separated path to the entry or group of entries
	 * @throws NoSuchConfigEntryError if the entry is not found
	 */
	void reset(String path);
	
	/**
	 * Reset an entry within the GUI, if it has one.
	 * @param path Name or dot-separated path to the value
	 * @return {@code true} if the entry has a GUI and was reset, {@code false} otherwise
	 * @throws NoSuchConfigEntryError if the entry is not found
	 */
	@OnlyIn(Dist.CLIENT) boolean resetInGUI(String path);
	
	/**
	 * Restore an entry within the GUI, if it has one.
	 * @param path Name or dot-separated path to the value
	 * @return {@code true} if the entry has a GUI and was restored, {@code false} otherwise
	 * @throws NoSuchConfigEntryError if the entry is not found
	 */
	@OnlyIn(Dist.CLIENT) boolean restoreInGUI(String path);
}
