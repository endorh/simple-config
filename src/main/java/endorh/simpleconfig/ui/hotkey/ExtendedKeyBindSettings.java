package endorh.simpleconfig.ui.hotkey;

import endorh.simpleconfig.ui.hotkey.KeyBindMapping.KeyBindActivation;
import endorh.simpleconfig.ui.hotkey.KeyBindMapping.KeyBindContext;
import endorh.simpleconfig.ui.hotkey.KeyBindMapping.VanillaKeyBindContext;

public interface ExtendedKeyBindSettings {
	static ExtendedKeyBindSettingsBuilder ingame() {
		return new ExtendedKeyBindSettingsBuilder();
	}
	
	static ExtendedKeyBindSettingsBuilder menu() {
		return new ExtendedKeyBindSettingsBuilder().withContext(VanillaKeyBindContext.MENU);
	}
	
	static ExtendedKeyBindSettingsBuilder all() {
		return new ExtendedKeyBindSettingsBuilder().withContext(VanillaKeyBindContext.ALL);
	}
	
	KeyBindActivation getActivation();
	
	KeyBindContext getContext();
	
	boolean isAllowExtraKeys();
	
	boolean isOrderSensitive();
	
	boolean isExclusive();
	
	boolean isMatchByChar();
	
	boolean isPreventFurther();
	
	ExtendedKeyBindSettings copy();
}
