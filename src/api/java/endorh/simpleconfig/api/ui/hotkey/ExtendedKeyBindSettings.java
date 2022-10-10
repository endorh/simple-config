package endorh.simpleconfig.api.ui.hotkey;

import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping.KeyBindActivation;
import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping.KeyBindContext;
import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping.VanillaKeyBindContext;
import org.jetbrains.annotations.NotNull;

public interface ExtendedKeyBindSettings {
	static @NotNull ExtendedKeyBindSettingsBuilder ingame() {
		return new ExtendedKeyBindSettingsBuilder();
	}
	
	static @NotNull ExtendedKeyBindSettingsBuilder menu() {
		return new ExtendedKeyBindSettingsBuilder().withContext(VanillaKeyBindContext.MENU);
	}
	
	static @NotNull ExtendedKeyBindSettingsBuilder all() {
		return new ExtendedKeyBindSettingsBuilder().withContext(VanillaKeyBindContext.ALL);
	}
	
	@NotNull KeyBindActivation activation();
	@NotNull KeyBindContext context();
	
	boolean allowExtraKeys();
	boolean orderSensitive();
	boolean exclusive();
	boolean matchByChar();
	boolean preventFurther();
	
	@NotNull ExtendedKeyBindSettings copy();
}
