package endorh.simpleconfig.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.Nullable;

import static endorh.simpleconfig.api.SimpleConfigGUIManagerProxy.getSimpleConfigGUIManager;

public interface SimpleConfigGUIManager {
	/**
	 * Prevent Simple Config from adding a button to the pause menu.<br>
	 * Mainly intended to be used by mods that modify the pause menu themselves.
	 */
	static void hideSimpleConfigPauseMenuButton() {
		getSimpleConfigGUIManager().setAddButton(false);
	}
	
	void setAddButton(boolean add);
	
	boolean hasConfigGUI(String modId);
	@Nullable Screen getConfigGUI(String modId, Screen parent);
	default @Nullable Screen getConfigGUI(String modId) {
		return getConfigGUI(modId, Minecraft.getInstance().currentScreen);
	}
	void showConfigGUI(String modId);
	void showModListGUI();
	void showConfigHotkeysGUI();
}
