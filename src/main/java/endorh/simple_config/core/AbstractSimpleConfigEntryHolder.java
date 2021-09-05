package endorh.simple_config.core;

import endorh.simple_config.clothconfig2.gui.entries.SubCategoryListEntry;
import endorh.simple_config.core.SimpleConfig.InvalidConfigValueTypeException;
import endorh.simple_config.core.SimpleConfig.NoSuchConfigEntryError;
import endorh.simple_config.core.SimpleConfig.NoSuchConfigGroupError;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class AbstractSimpleConfigEntryHolder implements ISimpleConfigEntryHolder {
	protected static final Pattern DOT = Pattern.compile("\\.");
	protected Map<String, ConfigValue<?>> specValues;
	protected Map<String, AbstractConfigEntry<?, ?, ?, ?>> entries;
	protected Map<String, ? extends AbstractSimpleConfigEntryHolder> children;
	protected SimpleConfig root;
	protected boolean dirty = false;
	
	/**
	 * Get the root config of this entry holder
	 */
	@Override public SimpleConfig getRoot() {
		return root;
	}
	
	/**
	 * Used in error messages
	 */
	protected abstract String getPath();
	
	/**
	 * Mark this entry holder as dirty<br>
	 * When the config screen is saved, only config files containing dirty entries are updated
	 */
	@Override public AbstractSimpleConfigEntryHolder markDirty() {
		markDirty(true);
		return this;
	}
	
	protected abstract void bake();
	
	/**
	 * Mark this entry holder as dirty or clean<br>
	 * The clean state is propagated to all of the children<br>
	 * Subclasses should propagate the dirty state to their parents
	 */
	@Override public void markDirty(boolean dirty) {
		this.dirty = dirty;
		if (!dirty) {
			children.values().forEach(c -> c.markDirty(false));
			entries.values().forEach(e -> e.dirty(false));
		}
	}
	
	/**
	 * Workaround {@link SubCategoryListEntry#isRequiresRestart()} reporting false positives<br>
	 * Instead, we mark all entries as not requiring restart initially, and mark them all as
	 * requiring restart if our own computation is correct.
	 */
	@OnlyIn(Dist.CLIENT) protected void markGUIRestart() {
		entries.values().stream().filter(e -> e.guiEntry != null)
		  .forEach(e -> e.guiEntry.setRequiresRestart(true));
		children.values().forEach(AbstractSimpleConfigEntryHolder::markGUIRestart);
	}
	
	/**
	 * Remove GUI bindings after saving a GUI
	 */
	@OnlyIn(Dist.CLIENT) protected void removeGUI() {
		entries.values().forEach(e -> e.guiEntry = null);
		children.values().forEach(AbstractSimpleConfigEntryHolder::removeGUI);
	}
	
	/**
	 * Check if any dirty entry requires a restart
	 */
	public boolean anyDirtyRequiresRestart() {
		return entries.values().stream().anyMatch(e -> e.dirty && e.requireRestart)
		       || children.values().stream().anyMatch(
		         AbstractSimpleConfigEntryHolder::anyDirtyRequiresRestart);
	}
	
	/**
	 * Get a child entry holder<br>
	 * @param path Name or dot-separated path to the child
	 *             If null or empty, {@code this} will be returned
	 * @throws NoSuchConfigGroupError if the child is not found
	 * @return A child {@link AbstractSimpleConfigEntryHolder},
	 *         or {@code this} if path is null or empty.
	 */
	public AbstractSimpleConfigEntryHolder getChild(String path) {
		if (path == null || path.isEmpty())
			return this;
		final String[] split = DOT.split(path, 2);
		if (split.length < 2) {
			if (children.containsKey(path))
				return children.get(path);
			throw new NoSuchConfigGroupError(getPath() + "." + path);
		} else if (children.containsKey(split[0]))
			return children.get(split[0]).getChild(split[1]);
		throw new NoSuchConfigGroupError(getPath() + "." + path);
	}
	
	/**
	 * Get the {@link ConfigValue} associated with an entry
	 * @param path Name or dot-separated path to the entry
	 * @param <T> Expected type of the entry
	 * @throws NoSuchConfigEntryError if the entry is not found
	 */
	protected @Nullable <T> ConfigValue<T> getSpecValue(String path) {
		try {
			if (specValues.containsKey(path))
				//noinspection unchecked
				return (ConfigValue<T>) specValues.get(path);
			else return getSubSpecValue(path);
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(getPath() + "." + path, e);
		}
	}
	
	/**
	 * Get the {@link ConfigValue} from a path containing at least one level<br>
	 * This method is not intended to be used directly, instead use the
	 * more general {@link AbstractSimpleConfigEntryHolder#getSpecValue(String)}
	 * @param path Dot-separated path containing at least one dot
	 * @param <T> Expected type of the entry
	 * @throws NoSuchConfigEntryError if the entry is not found
	 */
	protected <T> ConfigValue<T> getSubSpecValue(String path) {
		final String[] split = DOT.split(path, 2);
		if (split.length < 2)
			throw new NoSuchConfigEntryError(getPath() + "." + path);
		if (children.containsKey(split[0]))
			return children.get(split[0]).getSpecValue(split[1]);
		throw new NoSuchConfigEntryError(getPath() + "." + path);
	}
	
	/**
	 * Get a config entry by name or dot-separated path
	 * @param path Name or dot-separated path to the entry
	 * @param <T> Expected type of the entry
	 * @param <Gui> Expected GUI type of the entry
	 * @throws NoSuchConfigEntryError if the entry is not found
	 * @see AbstractSimpleConfigEntryHolder#get(String)
	 * @see AbstractSimpleConfigEntryHolder#set(String, Object)
	 */
	protected <T, Gui> AbstractConfigEntry<T, ?, Gui, ?> getEntry(String path) {
		AbstractConfigEntry<?, ?, ?, ?> entry = entries.get(path);
		if (entry == null) {
			entry = getSubEntry(path);
			if (entry == null) // Unnecessary, since getSubEntry already throws
				throw new NoSuchConfigEntryError(getPath() + "." + path);
		}
		//noinspection unchecked
		return (AbstractConfigEntry<T, ?, Gui, ?>) entry;
	}
	
	/**
	 * Get a config entry by path<br>
	 * The path must at least contain one level<br>
	 * This method is not intended to be called directly, instead use
	 * the more general {@link AbstractSimpleConfigEntryHolder#getEntry(String)}
	 * @param path Dot-separated path, containing at least one dot
	 * @param <T> Expected type of the entry
	 * @throws NoSuchConfigEntryError if the entry is not found
	 */
	protected <T> AbstractConfigEntry<T, ?, ?, ?> getSubEntry(String path) {
		final String[] split = DOT.split(path, 2);
		if (split.length < 2)
			throw new NoSuchConfigEntryError(getPath() + "." + path);
		if (children.containsKey(split[0]))
			return children.get(split[0]).getEntry(split[1]);
		throw new NoSuchConfigEntryError(getPath() + "." + path);
	}
	
	protected abstract void commitFields();
	
	protected void commitFields(String path) {
		if (path == null || path.isEmpty())
			commitFields();
		final String[] split = DOT.split(path, 2);
		if (children.containsKey(split[0])) {
			if (split.length < 2) children.get(split[0]).commitFields();
			else children.get(split[0]).commitFields(split[1]);
		} else throw new NoSuchConfigGroupError(getPath() + "." + path);
	}
	
	/**
	 * Get a config value<br>
	 * To retrieve a numeric primitive value use instead the variant methods,
	 * or pass {@link Number} as the type parameter to prevent illegal casts.
	 * @param path Name or dot-separated path to the value
	 * @param <T> Expected type of the value
	 * @throws NoSuchConfigEntryError if the value is not found
	 * @throws InvalidConfigValueTypeException if the value type does not match the expected
	 * @see AbstractSimpleConfigEntryHolder#getBoolean(String)
	 * @see AbstractSimpleConfigEntryHolder#getChar(String)
	 * @see AbstractSimpleConfigEntryHolder#getByte(String)
	 * @see AbstractSimpleConfigEntryHolder#getShort(String)
	 * @see AbstractSimpleConfigEntryHolder#getInt(String)
	 * @see AbstractSimpleConfigEntryHolder#getLong(String)
	 * @see AbstractSimpleConfigEntryHolder#getFloat(String)
	 * @see AbstractSimpleConfigEntryHolder#getDouble(String)
	 */
	@Override public <T> T get(String path) {
		try {
			return this.<T, Object>getEntry(path).get(getSpecValue(path));
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(getPath() + "." + path, e);
		}
	}
	
	/**
	 * Set a config value<br>
	 * Use {@link AbstractSimpleConfigEntryHolder#set(String, Object)} instead
	 * to benefit from an extra layer of primitive generics type safety
	 * @param path Name or dot-separated path to the value
	 * @param value Value to be set
	 * @param <V> Type of the value
	 * @throws NoSuchConfigEntryError if the entry is not found
	 * @throws InvalidConfigValueTypeException if the entry's type does not match the expected
	 * @deprecated Use {@link AbstractSimpleConfigEntryHolder#set(String, Object)} instead
	 * to benefit from an extra layer of primitive generics type safety
	 */
	@Internal @Deprecated @Override public <V> void doSet(String path, V value) {
		try {
			AbstractConfigEntry<V, ?, Object, ?> entry = this.getEntry(path);
			if (!entry.typeClass.isInstance(value))
				throw new InvalidConfigValueTypeException(getPath() + "." + path);
			entry.set(getSpecValue(path), value);
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(getPath() + "." + path, e);
		}
	}
	
	/**
	 * Set a config value in the GUI<br>
	 * Use {@link AbstractSimpleConfigEntryHolder#setGUI(String, Object)} instead
	 * to benefit from an extra layer of primitive generics type safety
	 * @param path Name or dot-separated path to the value
	 * @param value Value to be set
	 * @param <G> Type of the value
	 * @throws NoSuchConfigEntryError if the entry is not found
	 * @throws InvalidConfigValueTypeException if the entry's type does not match the expected
	 * @deprecated Use {@link AbstractSimpleConfigEntryHolder#setGUI(String, Object)} instead
	 * to benefit from an extra layer of primitive generics type safety
	 */
	@Internal @Deprecated @Override public <G> void doSetGUI(String path, G value) {
		try {
			this.<Object, G>getEntry(path).setGUI(value);
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(getPath() + "." + path, e);
		}
	}
	
	protected <V> void doSetForGUI(String path, V value) {
		try {
			getEntry(path).setForGUI(value);
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(getPath() + "." + path, e);
		}
	}
	
	/**
	 * Set a config value in the GUI, translating it from its actual type<br>
	 * @param path Name or dot-separated path to the value
	 * @param value Value to be set
	 * @param <V> Type of the value
	 * @throws NoSuchConfigEntryError if the entry is not found
	 * @throws InvalidConfigValueTypeException if the entry's type does not match the expected
	 */
	public <V> void setForGUI(String path, V value) {
		if (value instanceof Number) {
			try {
				setForGUI(path, (Number) value);
				return;
			} catch (InvalidConfigValueTypeException ignored) {}
		}
		doSetForGUI(path, value);
	}
	
	/**
	 * Set a config value in the GUI, translating it from its actual type<br>
	 * Numeric types are upcast as needed
	 * @param path Name or dot-separated path to the value
	 * @param number Value to be set
	 * @throws NoSuchConfigEntryError if the entry is not found
	 * @throws InvalidConfigValueTypeException if the entry's type does not match the expected
	 */
	public void setForGUI(String path, Number number) {
		boolean pre;
		//noinspection AssignmentUsedAsCondition
		if (pre = number instanceof Byte) {
			try {
				doSetForGUI(path, number.byteValue());
				return;
			} catch (InvalidConfigValueTypeException ignored) {}
		}
		if (pre |= number instanceof Short) {
			try {
				doSetForGUI(path, number.shortValue());
				return;
			} catch (InvalidConfigValueTypeException ignored) {}
		}
		if (pre |= number instanceof Integer) {
			try {
				doSetForGUI(path, number.intValue());
				return;
			} catch (InvalidConfigValueTypeException ignored) {}
		}
		if (pre || number instanceof Long) {
			try {
				doSetForGUI(path, number.longValue());
				return;
			} catch (InvalidConfigValueTypeException ignored) {}
		}
		if (number instanceof Float) {
			try {
				doSetForGUI(path, number.floatValue());
				return;
			} catch (InvalidConfigValueTypeException ignored) {}
		}
		doSetForGUI(path, number.doubleValue());
	}
	
	@Override public <G> G getGUI(String path) {
		try {
			return this.<Object, G>getEntry(path).getGUI();
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(getPath() + "." + path, e);
		}
	}
	
	public <V> V getFromGUI(String path) {
		try {
			return this.<V, Object>getEntry(path).getFromGUI();
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(getPath() + "." + path, e);
		}
	}
	
	public boolean getBooleanFromGUI(String path) {
		return this.<Boolean>getFromGUI(path);
	}
	
	public byte getByteFromGUI(String path) {
		return this.<Number>getFromGUI(path).byteValue();
	}
	
	public short getShortFromGUI(String path) {
		return this.<Number>getFromGUI(path).shortValue();
	}
	
	public int getIntFromGUI(String path) {
		return this.<Number>getFromGUI(path).intValue();
	}
	
	public long getLongFromGUI(String path) {
		return this.<Number>getFromGUI(path).longValue();
	}
	
	public float getFloatFromGUI(String path) {
		return this.<Number>getFromGUI(path).floatValue();
	}
	
	public double getDoubleFromGUI(String path) {
		return this.<Number>getFromGUI(path).doubleValue();
	}
	
	public char getCharFromGUI(String path) {
		return this.<Character>getFromGUI(path);
	}
}
