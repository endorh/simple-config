package endorh.simpleconfig.api;

import endorh.simpleconfig.api.SimpleConfig.InvalidConfigValueException;
import endorh.simpleconfig.api.SimpleConfig.InvalidConfigValueTypeException;
import endorh.simpleconfig.api.SimpleConfig.NoSuchConfigEntryError;
import endorh.simpleconfig.api.SimpleConfig.NoSuchConfigGroupError;
import endorh.simpleconfig.api.ui.ConfigScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.ApiStatus.Internal;
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
	SimpleConfig getRoot();
	
	/**
	 * Get the parent of this entry holder.
	 * Will throw {@link NoSuchConfigGroupError} if it has no parent.
	 */
	default ConfigEntryHolder getParent() {
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
	 * @throws InvalidConfigValueTypeException if the value type does not match the expected
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
	 * @throws InvalidConfigValueTypeException if the value does not match
	 *         the expected
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
	 * @throws InvalidConfigValueTypeException if the value does not match the expected
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
	 * Internal generic setter<br>
	 * Use {@link ConfigEntryHolder#set(String, Object)} instead
	 * to benefit from a layer of primitive generics type safety
	 * @param path Name or dot-separated path to the value
	 * @param value Value to be set
	 * @param <V> The type of the value
	 * @deprecated Use {@link ConfigEntryHolder#set(String, Object)} instead
	 * to benefit from an extra layer of primitive generics type safety
	 */
	@SuppressWarnings("DeprecatedIsStillUsed") @Internal @Deprecated <V> void doSet(String path, V value);
	
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
	default <V> void set(String path, V value) {
		if (value instanceof Number) {
			try {
				set(path, (Number) value);
				return;
			} catch (InvalidConfigValueTypeException ignored) {}
		}
		doSet(path, value);
	}
	
	/**
	 * Set a config value<br>
	 * Numeric values are upcast as needed
	 * @param path Name or dot-separated path to the value
	 * @param number Value to be set
	 * @throws NoSuchConfigEntryError if the entry is not found
	 * @throws InvalidConfigValueTypeException if the entry's type does not match the expected
	 * @throws InvalidConfigValueException if the value is invalid for this entry
	 */
	default void set(String path, Number number) {
		boolean pre;
		//noinspection AssignmentUsedAsCondition
		if (pre = number instanceof Byte) {
			try {
				doSet(path, number.byteValue());
				return;
			} catch (InvalidConfigValueTypeException ignored) {}
		}
		if (pre |= number instanceof Short) {
			try {
				doSet(path, number.shortValue());
				return;
			} catch (InvalidConfigValueTypeException ignored) {}
		}
		if (pre |= number instanceof Integer) {
			try {
				doSet(path, number.intValue());
				return;
			} catch (InvalidConfigValueTypeException ignored) {}
		}
		if (pre || number instanceof Long) {
			try {
				doSet(path, number.longValue());
				return;
			} catch (InvalidConfigValueTypeException ignored) {}
		}
		if (number instanceof Float) {
			try {
				doSet(path, number.floatValue());
				return;
			} catch (InvalidConfigValueTypeException ignored) {}
		}
		doSet(path, number.doubleValue());
	}
	
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
		return this.<Boolean>get(path);
	}
	
	
	/**
	 * Get a char config value
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not int
	 * @see #get(String)
	 */
	default char getChar(String path) {
		return this.<Character>get(path);
	}
	
	/**
	 * Get a byte config value
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not int
	 * @see #get(String)
	 */
	default byte getByte(String path) {
		return this.<Number>get(path).byteValue();
	}
	
	/**
	 * Get a short config value
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not int
	 * @see #get(String)
	 */
	default short getShort(String path) {
		return this.<Number>get(path).shortValue();
	}
	
	/**
	 * Get an int config value
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not int
	 * @see #get(String)
	 */
	default int getInt(String path) {
		return this.<Number>get(path).intValue();
	}
	
	/**
	 * Get a long config value
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not long
	 * @see #get(String)
	 */
	default long getLong(String path) {
		return this.<Number>get(path).longValue();
	}
	
	/**
	 * Get a float config value
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not float
	 * @see #get(String)
	 */
	default float getFloat(String path) {
		return this.<Number>get(path).floatValue();
	}
	
	/**
	 * Get a double config value
	 * @param path Name or dot-separated path to the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type is not double
	 * @see #get(String)
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
	 * @see #getGUI(String)
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
	 * @see #getGUI(String)
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
	 * @see #getGUI(String)
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
	 * @see #getGUI(String)
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
	 * @see #getGUI(String)
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
	 * @see #getGUI(String)
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
	 * @see #getGUI(String)
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
	 * @see #getGUI(String)
	 */
	default double getGUIDouble(String path) {
		return this.<Number>getGUI(path).doubleValue();
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
		return this.<Boolean>getFromGUI(path);
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
		return this.<Number>getFromGUI(path).byteValue();
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
		return this.<Number>getFromGUI(path).shortValue();
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
		return this.<Number>getFromGUI(path).intValue();
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
		return this.<Number>getFromGUI(path).longValue();
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
		return this.<Number>getFromGUI(path).floatValue();
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
		return this.<Number>getFromGUI(path).doubleValue();
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
		return this.<Character>getFromGUI(path);
	}
	
	/**
	 * Internal generic GUI setter<br>
	 * Use {@link ConfigEntryHolder#setGUI(String, Object)} instead
	 * to benefit from a layer of primitive generics type safety
	 * @param path Name or dot-separated path to the value
	 * @param value Value to be set
	 * @param <G> The type of the value
	 * @deprecated Use {@link ConfigEntryHolder#setGUI(String, Object)} instead
	 * to benefit from an extra layer of primitive generics type safety
	 */
	@SuppressWarnings("DeprecatedIsStillUsed") @Internal @Deprecated <G> void doSetGUI(String path, G value);
	
	
	/**
	 * Set a config value in the GUI
	 * @param path Name or dot-separated path to the value
	 * @param value Value to be set
	 * @param <G> Type of the value in the GUI
	 * @throws NoSuchConfigEntryError if the entry is not found
	 * @throws InvalidConfigValueTypeException if the entry's type does not match the expected
	 */
	default <G> void setGUI(String path, G value) {
		if (value instanceof Number) {
			try {
				setGUI(path, (Number) value);
				return;
			} catch (InvalidConfigValueTypeException ignored) {}
		}
		doSetGUI(path, value);
	}
	
	
	/**
	 * Set a config value in the GUI<br>
	 * Numeric values are upcast as needed
	 * @param path Name or dot-separated path to the value
	 * @param number Value to be set in the GUI
	 * @throws NoSuchConfigEntryError if the entry is not found
	 * @throws InvalidConfigValueTypeException if the entry's type does not match the expected
	 */
	default void setGUI(String path, Number number) {
		boolean pre;
		//noinspection AssignmentUsedAsCondition
		if (pre = number instanceof Byte) {
			try {
				doSetGUI(path, number.byteValue());
				return;
			} catch (InvalidConfigValueTypeException ignored) {}
		}
		if (pre |= number instanceof Short) {
			try {
				doSetGUI(path, number.shortValue());
				return;
			} catch (InvalidConfigValueTypeException ignored) {}
		}
		if (pre |= number instanceof Integer) {
			try {
				doSetGUI(path, number.intValue());
				return;
			} catch (InvalidConfigValueTypeException ignored) {}
		}
		if (pre || number instanceof Long) {
			try {
				doSetGUI(path, number.longValue());
				return;
			} catch (InvalidConfigValueTypeException ignored) {}
		}
		if (number instanceof Float) {
			try {
				doSetGUI(path, number.floatValue());
				return;
			} catch (InvalidConfigValueTypeException ignored) {}
		}
		doSetGUI(path, number.doubleValue());
	}
	
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
