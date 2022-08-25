package endorh.simpleconfig.ui.hotkey;

import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping;

public interface IConfigHotKey {
	ExtendedKeyBindImpl getKeyBind();
	KeyBindMapping getKeyMapping();
	void applyHotkey();
}
