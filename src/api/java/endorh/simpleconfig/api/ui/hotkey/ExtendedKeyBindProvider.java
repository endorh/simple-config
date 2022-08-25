package endorh.simpleconfig.api.ui.hotkey;

import com.google.common.collect.Lists;

public interface ExtendedKeyBindProvider {
	static void registerKeyBinds(ExtendedKeyBind... keyBinds) {
		registerProvider(new SimpleExtendedKeyBindProvider(Lists.newArrayList(keyBinds)));
	}
	
	static void registerProvider(ExtendedKeyBindProvider provider) {
		ExtendedKeyBindProxy.getRegistrar().registerProvider(provider);
	}
	
	static void unregisterProvider(ExtendedKeyBindProvider provider) {
		ExtendedKeyBindProxy.getRegistrar().unregisterProvider(provider);
	}
	
	/**
	 * Provide active keybinds.<br>
	 * This doesn't need to check for keybind context, it merely allows providers to
	 * add another layer of enable-ability to keybinds.<br>
	 * <b>All returned keybinds must be instances of {@code ExtendedKeyBindImpl}</b>.
	 * Do not implement {@link ExtendedKeyBind} directly.
	 */
	Iterable<ExtendedKeyBind> getActiveKeyBinds();
	
	/**
	 * Provide a collection of all potential keybinds that could be
	 * returned by this provider, for the purpose of checking for conflicts.<br>
	 * <b>All returned keybinds must be instances of {@code ExtendedKeyBindImpl}</b>.
	 * Do not implement {@link ExtendedKeyBind} directly.
	 */
	default Iterable<ExtendedKeyBind> getAllKeyBinds() {
		return getActiveKeyBinds();
	}
	
	/**
	 * Priority of this provider. Higher values receive events earlier.
	 */
	default int getPriority() {
		return 0;
	}
}