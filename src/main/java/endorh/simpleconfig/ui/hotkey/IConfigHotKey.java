package endorh.simpleconfig.ui.hotkey;

public interface IConfigHotKey {
	ExtendedKeyBindImpl getKeyBind();
	KeyBindMapping getKeyMapping();
	void applyHotkey();
}
