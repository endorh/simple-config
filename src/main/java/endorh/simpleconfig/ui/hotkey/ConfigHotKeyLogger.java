package endorh.simpleconfig.ui.hotkey;

import endorh.simpleconfig.config.ClientConfig.hotkey_log;
import endorh.simpleconfig.config.CommonConfig.HotKeyLogLocation;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConfigHotKeyLogger {
	public static void logHotKey(Component title, List<Component> message) {
		if (!hotkey_log.log_hotkey_actions) message = Collections.emptyList();
		logHotKey(hotkey_log.hotkey_log_location, title, message);
	}
	
	public static void logRemoteHotKey(Component title, List<Component> message) {
		if (!hotkey_log.log_remote_hotkey_actions) message = Collections.emptyList();
		logHotKey(hotkey_log.remote_hotkey_log_location, title, message);
	}
	
	private static void logHotKey(
	  HotKeyLogLocation location,
	  Component title, List<Component> message
	) {
		int size = message.size();
		int maxSize = hotkey_log.max_logged_actions;
		if (size > maxSize) {
			message = new ArrayList<>(message.subList(0, maxSize));
			message.add(new TranslatableComponent("simpleconfig.hotkey.more", size - maxSize));
		}
		switch (location) {
			case CHAT -> {
				LocalPlayer player = Minecraft.getInstance().player;
				if (player != null) {
					MutableComponent msg = title.copy();
					message.forEach(l -> msg.append("\n").append(l));
					player.sendMessage(msg, Util.NIL_UUID);
				}
			}
			case RIGHT_OVERLAY -> ConfigHotKeyOverlay.addMessage(title, message);
			case CENTER_TOAST -> ConfigHotKeyOverlay.addToastMessage(title);
		}
	}
}
