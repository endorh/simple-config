package endorh.simpleconfig.ui.hotkey;

public interface IConfigHotKey {
	ExtendedKeyBind getKeyBind();
	KeyBindMapping getKeyMapping();
	void applyHotkey();
}
