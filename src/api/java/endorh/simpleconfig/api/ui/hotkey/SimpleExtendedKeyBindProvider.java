package endorh.simpleconfig.api.ui.hotkey;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Simple keybind provider, using a collection of keybinds as backend,
 * and implementing priority as a property.<br>
 */
public class SimpleExtendedKeyBindProvider implements ExtendedKeyBindProvider {
	private final Collection<ExtendedKeyBind> keyBinds;
	private int priority;
	
	public SimpleExtendedKeyBindProvider(Collection<ExtendedKeyBind> keyBinds) {
		this(0, keyBinds);
	}
	
	public SimpleExtendedKeyBindProvider(int priority, Collection<ExtendedKeyBind> keyBinds) {
		this.priority = priority;
		this.keyBinds = keyBinds;
	}
	
	@Override public @NotNull Iterable<ExtendedKeyBind> getActiveKeyBinds() {
		return keyBinds;
	}
	
	@Override public int getPriority() {
		return priority;
	}
	
	public void setPriority(int priority) {
		this.priority = priority;
	}
}
