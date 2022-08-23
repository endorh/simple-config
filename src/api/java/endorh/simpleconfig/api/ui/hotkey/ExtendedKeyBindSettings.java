package endorh.simpleconfig.api.ui.hotkey;

import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping.KeyBindActivation;
import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping.KeyBindContext;
import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping.VanillaKeyBindContext;

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
	
	KeyBindActivation activation();
	
	KeyBindContext context();
	
	boolean allowExtraKeys();
	
	boolean orderSensitive();
	
	boolean exclusive();
	
	boolean matchByChar();
	
	boolean preventFurther();
	
	ExtendedKeyBindSettings copy();
}
