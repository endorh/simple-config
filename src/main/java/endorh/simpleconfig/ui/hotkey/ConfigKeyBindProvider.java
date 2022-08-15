package endorh.simpleconfig.ui.hotkey;

import endorh.simpleconfig.ui.hotkey.ConfigHotKeyManager.ConfigHotKeyGroup;
import endorh.simpleconfig.ui.hotkey.ConfigHotKeyManager.IConfigHotKeyGroupEntry;
import endorh.simpleconfig.ui.hotkey.ExtendedKeyBindDispatcher.ExtendedKeyBindProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class ConfigKeyBindProvider implements ExtendedKeyBindProvider {
	public static final ConfigKeyBindProvider INSTANCE = new ConfigKeyBindProvider();
	private final ConfigHotKeyManager manager = ConfigHotKeyManager.INSTANCE;
	
	@Override public Iterable<ExtendedKeyBind> getActiveKeyBinds() {
		return new ConfigKeyBindIterable(manager.getHotKeys().getEntries(), false);
	}
	@Override public Iterable<ExtendedKeyBind> getAllKeyBinds() {
		return new ConfigKeyBindIterable(manager.getHotKeys().getEntries(), true);
	}
	
	public static class ConfigKeyBindIterable implements Iterable<ExtendedKeyBind> {
		private final List<IConfigHotKeyGroupEntry> hotKeys;
		private final boolean iterateAll;
		
		public ConfigKeyBindIterable(List<IConfigHotKeyGroupEntry> hotKeys, boolean iterateAll) {
			this.hotKeys = hotKeys;
			this.iterateAll = iterateAll;
		}
		
		@NotNull @Override public Iterator<ExtendedKeyBind> iterator() {
			return new ConfigKeyBindIterator(hotKeys, iterateAll);
		}
	}
	
	public static class ConfigKeyBindIterator implements Iterator<ExtendedKeyBind> {
		private final List<? extends IConfigHotKey> hotKeys;
		private final boolean iterateAll;
		private ConfigHotKeyGroup group = null;
		private ConfigKeyBindIterator sub = null;
		private int index = 0;
		
		public ConfigKeyBindIterator(List<? extends IConfigHotKey> hotKeys, boolean iterateAll) {
			this.hotKeys = hotKeys;
			this.iterateAll = iterateAll;
		}
		
		@Override public boolean hasNext() {
			return index < hotKeys.size() || sub != null && sub.hasNext() || group != null && (iterateAll || group.isEnabled()) && !group.getEntries().isEmpty();
		}
		
		@Override public ExtendedKeyBind next() {
			if (group != null && (iterateAll || group.isEnabled())) {
				sub = new ConfigKeyBindIterator(group.getEntries(), iterateAll);
				group = null;
			}
			if (sub != null) {
				if (sub.hasNext()) {
					return sub.next();
				} else sub = null;
			}
			if (index >= hotKeys.size()) throw new NoSuchElementException();
			IConfigHotKey hotKey = hotKeys.get(index++);
			if (hotKey instanceof ConfigHotKeyGroup)
				group = (ConfigHotKeyGroup) hotKey;
			return hotKey.getKeyBind();
		}
	}
}
