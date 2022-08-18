package endorh.simpleconfig.ui.hotkey;

import com.electronwill.nightconfig.core.CommentedConfig;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.ui.hotkey.NestedHotKeyActionType.NestedHotKeyAction;
import endorh.simpleconfig.ui.icon.Icon;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class NestedHotKeyActionType<V> extends HotKeyActionType<V, NestedHotKeyAction<V>> {
	private final INestedHotKeyAction<V> action;
	
	public NestedHotKeyActionType(
	  String tagName, Icon icon, INestedHotKeyAction<V> action
	) {
		super(tagName, icon);
		this.action = action;
	}
	
	@Override
	public @Nullable <T, C, E extends AbstractConfigEntry<T, C, V>> NestedHotKeyAction<V> deserialize(
	  E entry, Object value
	) {
		if (value instanceof Map) {
			//noinspection unchecked
			return new NestedHotKeyAction<>(this, action, (Map<String, HotKeyAction<?>>) value);
		}
		return null;
	}
	
	@Override public <T, C, E extends AbstractConfigEntry<T, C, V>> Object serialize(
	  E entry, NestedHotKeyAction<V> action
	) {
		return action.getStorage();
	}
	
	public static class NestedHotKeyAction<V> extends HotKeyAction<V> {
		private final INestedHotKeyAction<V> action;
		private final Map<String, HotKeyAction<?>> storage;
		
		public NestedHotKeyAction(HotKeyActionType<V, ?> type, INestedHotKeyAction<V> action, Map<String, HotKeyAction<?>> storage) {
			super(type);
			this.action = action;
			this.storage = storage;
		}
		
		@Override public <T, C, E extends AbstractConfigEntry<T, C, V>>
		@Nullable ITextComponent apply(String path, E entry, CommentedConfig result) {
			try {
				action.apply(entry, storage);
			} catch (ClassCastException ignored) {}
			return null;
		}
		
		public INestedHotKeyAction<V> getAction() {
			return action;
		}
		
		public Map<String, HotKeyAction<?>> getStorage() {
			return storage;
		}
		
		// TODO: Equals and hashCode
	}
	
	public interface INestedHotKeyAction<V> {
		<E extends AbstractConfigEntry<?, ?, V>> void apply(E entry, Map<String, HotKeyAction<?>> storage);
	}
}
