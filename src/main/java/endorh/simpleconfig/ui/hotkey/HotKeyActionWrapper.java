package endorh.simpleconfig.ui.hotkey;

public record HotKeyActionWrapper<T, A extends HotKeyAction<T>>(
  HotKeyActionType<T, A> type,
  Object value
) {}
