package endorh.simple_config.core;

import endorh.simple_config.core.SimpleConfig.InvalidConfigValueTypeException;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.HashMap;
import java.util.Map;

/**
 * A <i>fake</i> entry holder used by {@link EntryListEntry} to
 * trick nested entries into reading their values from a list
 * @param <V> The type of the elements of the list
 * @param <C> The type of the elements of the list facing the Config
 * @param <G> The type of the elements of the list facing the GUI
 * @param <E> The type of the entry being wrapped in the list
 */
public class ListEntryEntryHolder<V, C, G, E extends AbstractConfigEntry<V, C, G, E>>
  implements ISimpleConfigEntryHolder {
	protected final SimpleConfig root;
	protected AbstractConfigEntry<V, C, G, E> entry;
	protected final Map<String, V> values = new HashMap<>();
	
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
			//noinspection unchecked
			values.put(path, (V) value);
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
