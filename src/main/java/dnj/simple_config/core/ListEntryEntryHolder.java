package dnj.simple_config.core;

import dnj.simple_config.core.SimpleConfig.InvalidConfigValueTypeException;
import dnj.simple_config.core.SimpleConfig.NoSuchConfigEntryError;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.ArrayList;
import java.util.List;

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
	protected final AbstractConfigEntry<V, C, G, E> entry;
	protected List<V> value = null;
	protected final List<V> buffer = new ArrayList<>();
	
	public String nameFor(G element) {
		buffer.add(entry.fromGuiOrDefault(element));
		return String.valueOf(buffer.size() - 1);
	}
	
	public void onDelete(int index) {
		buffer.remove(index);
	}
	
	public void clear() {
		buffer.clear();
		value = null;
	}
	
	public ListEntryEntryHolder(AbstractConfigEntry<V, C, G, E> entry) {
		this.entry = entry;
	}
	
	public void setValue(List<V> value) {
		this.value = value;
	}
	
	@Override
	public SimpleConfig getRoot() {
		return entry.parent.getRoot();
	}
	
	@Override
	public void markDirty(boolean dirty) {
		entry.parent.markDirty(dirty);
	}
	
	@Override public <T> T get(String path) {
		// V must be T
		try {
			//noinspection unchecked
			return (T) buffer.get(Integer.parseInt(path));
		} catch (NumberFormatException e) {
			throw new NoSuchConfigEntryError(entry.getPath() + "." + path);
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
			this.buffer.set(Integer.parseInt(path), (V) value);
		} catch (NumberFormatException e) {
			throw new NoSuchConfigEntryError(entry.getPath() + "." + path);
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(entry.getPath() + "." + path, e);
		}
	}
	
	@Override public <Gui> Gui getGUI(String path) {
		return null;
	}
}
