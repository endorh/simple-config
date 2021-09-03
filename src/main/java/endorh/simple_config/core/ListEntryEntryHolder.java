package endorh.simple_config.core;

import endorh.simple_config.core.SimpleConfig.InvalidConfigValueTypeException;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.HashMap;
import java.util.Map;

/**
 * An entry holder used by {@link EntryListEntry}
 */
public class ListEntryEntryHolder
  implements ISimpleConfigEntryHolder {
	protected final SimpleConfig root;
	protected AbstractConfigEntry<?, ?, ?, ?> entry;
	protected final Map<String, Object> values = new HashMap<>();
	
	protected int id_gen = 0;
	
	public String nextName() {
		return String.valueOf(id_gen++);
	}
	
	public void clear() {
		values.clear();
		id_gen = 0;
	}
	
	public ListEntryEntryHolder(SimpleConfig root) {
		this.root = root;
	}
	
	@Override
	public SimpleConfig getRoot() {
		return root;
	}
	
	@Override
	public void markDirty(boolean dirty) {
		entry.parent.markDirty(dirty);
	}
	
	@Override public <T> T get(String path) {
		// V must be T
		try {
			//noinspection unchecked
			return (T) values.get(path);
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(entry.getPath() + "." + path, e);
		}
	}
	
	/**
	 * @deprecated Use {@link ListEntryEntryHolder#set(String, Object)} instead
	 * to benefit from an extra layer of primitive generics type safety
	 */
	@Internal @Deprecated @Override public <T> void doSet(String path, T value) {
		// T must be V
		try {
			values.put(path, value);
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(entry.getPath() + "." + path, e);
		}
	}
	
	// Should never be called
	@Override public <Gui> Gui getGUI(String path) {
		throw new UnsupportedOperationException();
	}
	@Override public <T> void doSetGUI(String path, T value) {
		throw new UnsupportedOperationException();
	}
}
