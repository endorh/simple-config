package endorh.simpleconfig.ui.hotkey;

import endorh.simpleconfig.ui.api.ModifierKeyCode;

public interface IConfigHotKey {
	ModifierKeyCode getHotKey();
	void applyHotkey();
}
