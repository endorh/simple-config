package endorh.simpleconfig.ui.hotkey;

import endorh.simpleconfig.config.ClientConfig.hotkey_log;
import endorh.simpleconfig.config.CommonConfig.HotKeyLogLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConfigHotKeyLogger {
	public static void logHotKey(ITextComponent title, List<ITextComponent> message) {
		if (!hotkey_log.log_hotkey_actions) message = Collections.emptyList();
		logHotKey(hotkey_log.hotkey_log_location, title, message);
	}
	
	public static void logRemoteHotKey(ITextComponent title, List<ITextComponent> message) {
		if (!hotkey_log.log_remote_hotkey_actions) message = Collections.emptyList();
		logHotKey(hotkey_log.remote_hotkey_log_location, title, message);
	}
	
	private static void logHotKey(
	  HotKeyLogLocation location,
	  ITextComponent title, List<ITextComponent> message
	) {
		int size = message.size();
		int maxSize = hotkey_log.max_logged_actions;
		if (size > maxSize) {
			message = new ArrayList<>(message.subList(0, maxSize));
			message.add(new TranslationTextComponent("simpleconfig.hotkey.more", size - maxSize));
		}
		switch (location) {
			case CHAT:
				ClientPlayerEntity player = Minecraft.getInstance().player;
				if (player != null) {
					IFormattableTextComponent msg = title.copy();
					message.forEach(l -> msg.append("\n").append(l));
					player.sendMessage(msg, Util.NIL_UUID);
				}
				break;
			case RIGHT_OVERLAY:
				ConfigHotKeyOverlay.addMessage(title, message);
				break;
			case CENTER_TOAST:
				ConfigHotKeyOverlay.addToastMessage(title);
				break;
		}
	}
}
