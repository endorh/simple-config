package endorh.simpleconfig.ui.hotkey;

import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.api.ui.icon.Icon;
import org.jetbrains.annotations.Nullable;

public class StorageLessHotKeyActionType<V> extends SimpleHotKeyActionType<V, Void> {
	private final IStorageLessHotKeyAction<V> action;
	
	public StorageLessHotKeyActionType(
	  String tagName, Icon icon, IStorageLessHotKeyAction<V> action
	) {
		super(tagName, icon, action);
		this.action = action;
	}
	
	@Override
	public @Nullable <T, C, E extends AbstractConfigEntry<T, C, V>> SimpleHotKeyAction<V, Void> deserialize(
	  E entry, Object value
	) {
		return new StorageLessHotKeyAction<>(this, entry, action);
	}
	
	public static class StorageLessHotKeyAction<V> extends SimpleHotKeyAction<V, Void> {
		public StorageLessHotKeyAction(
		  StorageLessHotKeyActionType<V> type, AbstractConfigEntry<?, ?, V> entry,
		  IStorageLessHotKeyAction<V> action
		) {
			super(type, entry, action, null);
		}
		
		@Override public StorageLessHotKeyActionType<V> getType() {
			return (StorageLessHotKeyActionType<V>) super.getType();
		}
	}
	
	@FunctionalInterface public interface IStorageLessHotKeyAction<V> extends ISimpleHotKeyAction<V, Void> {
		@Nullable V applyValue(AbstractConfigEntry<?, ?, V> entry, V value);
		
		@Override default @Nullable V applyValue(
		  AbstractConfigEntry<?, ?, V> entry, V value, Void serialized
		) {
			return applyValue(entry, value);
		}
	}
}
