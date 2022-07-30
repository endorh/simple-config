package endorh.simpleconfig.ui.hotkey;

import net.minecraftforge.client.event.InputEvent.KeyInputEvent;
import net.minecraftforge.client.event.InputEvent.MouseInputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

public class ConfigHotKeyInputHandler {
	public static final ConfigHotKeyInputHandler INSTANCE = new ConfigHotKeyInputHandler();
	private final ConfigHotKeyManager manager = ConfigHotKeyManager.INSTANCE;
	
	@SubscribeEvent public void onKeyEvent(KeyInputEvent event) {
		if (event.getAction() == GLFW.GLFW_PRESS) {
			manager.getSortedHotKeys().stream()
			  .filter(h -> h.getHotKey().matchesKey(event.getKey(), event.getScanCode()))
			  .findFirst().ifPresent(IConfigHotKey::applyHotkey);
		} else if (event.getAction() == GLFW.GLFW_RELEASE) {
			// TODO: Improve hotkeys to support more features like key releases, non-standard
			//       modifiers, optional masking, optional order sensibility, etc.
		}
	}
	
	@SubscribeEvent public void onMouseEvent(MouseInputEvent event) {
		if (event.getAction() == GLFW.GLFW_PRESS) {
			manager.getSortedHotKeys().stream()
			  .filter(h -> h.getHotKey().matchesMouse(event.getButton()))
			  .findFirst().ifPresent(IConfigHotKey::applyHotkey);
		} else if (event.getAction() == GLFW.GLFW_RELEASE) {
		
		}
	}
}
