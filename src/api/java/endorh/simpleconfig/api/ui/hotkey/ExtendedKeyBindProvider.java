package endorh.simpleconfig.api.ui.hotkey;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;

public interface ExtendedKeyBindProvider {
	/**
	 * Register a set of keybinds directly, if you only have a finite
	 * amount of them.
	 */
	static void registerKeyBinds(ExtendedKeyBind... keyBinds) {
		registerProvider(new SimpleExtendedKeyBindProvider(Lists.newArrayList(keyBinds)));
	}
	
	/**
	 * Register a keybind provider, if you have a dynamic amount of
	 * keybinds.
	 */
	static void registerProvider(ExtendedKeyBindProvider provider) {
		ExtendedKeyBindProxy.getRegistrar().registerProvider(provider);
	}
	
	/**
	 * Unregister an already registered keybind provider.
	 */
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
	@NotNull Iterable<ExtendedKeyBind> getActiveKeyBinds();
	
	/**
	 * Provide a collection of all potential keybinds that could be
	 * returned by this provider, for the purpose of checking for conflicts.<br>
	 * <b>All returned keybinds must be instances of {@code ExtendedKeyBindImpl}</b>.
	 * Do not implement {@link ExtendedKeyBind} directly.
	 */
	default @NotNull Iterable<ExtendedKeyBind> getAllKeyBinds() {
		return getActiveKeyBinds();
	}
	
	/**
	 * Priority of this provider. Higher values receive events earlier.
	 */
	default int getPriority() {
		return 0;
	}
}
