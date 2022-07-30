package endorh.simpleconfig.ui.hotkey;

public class HotKeyActionWrapper<T, A extends HotKeyAction<T>> {
	private final HotKeyActionType<T, A> type;
	private final Object value;
	
	public HotKeyActionWrapper(HotKeyActionType<T, A> type, Object value) {
		this.type = type;
		this.value = value;
	}
	
	public HotKeyActionType<T, A> getType() {
		return type;
	}
	
	public Object getValue() {
		return value;
	}
}
