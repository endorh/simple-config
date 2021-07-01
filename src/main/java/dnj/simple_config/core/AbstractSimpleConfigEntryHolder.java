package dnj.simple_config.core;

import dnj.simple_config.core.SimpleConfig.InvalidConfigValueTypeException;
import dnj.simple_config.core.SimpleConfig.NoSuchConfigEntryError;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

import javax.annotation.Nullable;
import java.util.Map;

public abstract class AbstractSimpleConfigEntryHolder implements ISimpleConfigEntryHolder {
	protected Map<String, ConfigValue<?>> specValues;
	protected Map<String, AbstractConfigEntry<?, ?, ?, ?>> entries;
	protected Map<String, ? extends AbstractSimpleConfigEntryHolder> children;
	
	// Configs are loaded all the time, so cyclic references aren't a problem
	protected SimpleConfig root;
	protected boolean dirty = false;
	
	/**
	 * Get the root config of this entry holder
	 */
	@Override public SimpleConfig getRoot() {
		return root;
	}
	
	/**
	 * Mark this entry holder as dirty<br>
	 * When the config screen is saved, only config files containing dirty entries are updated
	 */
	@Override public AbstractSimpleConfigEntryHolder markDirty() {
		markDirty(true);
		return this;
	}
	
	/**
	 * Mark this entry holder as dirty or clean<br>
	 * The clean state is propagated to all of the children<br>
	 * Subclasses should propagate the dirty state to their parents
	 */
	@Override public void markDirty(boolean dirty) {
		this.dirty = dirty;
		if (!dirty) {
			children.values().forEach(c -> c.markDirty(false));
			entries.values().forEach(e -> e.markDirty(false));
		}
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
			throw new InvalidConfigValueTypeException(path, e);
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
		final String[] split = path.split("\\.", 2);
		if (children.containsKey(split[0]))
			return children.get(split[0]).getSpecValue(split[1]);
		throw new NoSuchConfigEntryError(path);
	}
	
	/**
	 * Get a config entry by name or dot-separated path
	 * @param path Name or dot-separated path to the entry
	 * @param <T> Expected type of the entry
	 * @throws NoSuchConfigEntryError if the entry is not found
	 * @see AbstractSimpleConfigEntryHolder#get(String)
	 * @see AbstractSimpleConfigEntryHolder#set(String, Object)
	 */
	protected <T> AbstractConfigEntry<T, ?, ?, ?> getEntry(String path) {
		AbstractConfigEntry<?, ?, ?, ?> entry = entries.get(path);
		if (entry == null)
			entry = getSubEntry(path);
		if (entry == null) // Unnecessary, since getSubEntry already throws
			throw new NoSuchConfigEntryError(path);
		//noinspection unchecked
		return (AbstractConfigEntry<T, ?, ?, ?>) entry;
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
		final String[] split = path.split("\\.", 2);
		if (children.containsKey(split[0]))
			return children.get(split[0]).getEntry(split[1]);
		throw new NoSuchConfigEntryError(path);
	}
	
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
	@Override public <T> T get(String path) {
		try {
			return this.<T>getEntry(path).get(getSpecValue(path));
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}
	
	/**
	 * Set a config value
	 * @param path Name or dot-separated path to the value
	 * @param value Value to be set
	 * @param <V> Type of the value
	 * @throws NoSuchConfigEntryError if the entry is not found
	 * @throws InvalidConfigValueTypeException if the entry's type does not match the expected
	 */
	@Override public <V> void set(String path, V value) {
		try {
			this.<V>getEntry(path).set(getSpecValue(path), value);
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(path, e);
		}
	}
}
