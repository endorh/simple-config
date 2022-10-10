package endorh.simpleconfig.api.ui.hotkey;

import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.api.ui.icon.KeyBindSettingsIcon;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons.Hotkeys;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TranslatableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/**
 * Contains a set of required keys and matching settings to use for matching an
 * {@link ExtendedKeyBind}.<br>
 * Inspired by <a href="https://github.com/maruohon/malilib">MaLiLib</a>'s great keybind system.<br>
 * <br>
 * The chars in {@link #getCharMap()} are those returned by
 * {@link GLFW#glfwGetKeyName(int, int)}, and could contain multiple characters,
 * depending on the keyboard layout.
 */
public interface KeyBindMapping {
	static @NotNull KeyBindMapping unset() {
		return unset(ExtendedKeyBindSettings.ingame().build());
	}
	
	static @NotNull KeyBindMapping unset(ExtendedKeyBindSettings settings) {
		return ExtendedKeyBindProxy.getFactory().unsetMapping(settings);
	}
	
	@NotNull IntList getRequiredKeys();
	@Nullable Int2ObjectMap<String> getCharMap();
	@NotNull ExtendedKeyBindSettings getSettings();
	
	boolean isUnset();
	boolean overlaps(KeyBindMapping other);
	
	default @NotNull Component getDisplayName() {
		return getDisplayName(Style.EMPTY);
	}
	default @NotNull Component getDisplayName(ChatFormatting style) {
		return getDisplayName(Style.EMPTY.applyFormat(style));
	}
	@NotNull Component getDisplayName(Style style);
	@NotNull KeyBindMapping copy();
	
	static @NotNull KeyBindMapping parse(String serialized) {
		return ExtendedKeyBindProxy.getFactory().parseMapping(serialized);
	}
	@NotNull String serialize();
	
	enum KeyBindActivation {
		PRESS(Hotkeys.ACTIVATION_PRESS),
		RELEASE(Hotkeys.ACTIVATION_RELEASE),
		BOTH(Hotkeys.ACTIVATION_BOTH),
		TOGGLE(Hotkeys.ACTIVATION_TOGGLE),
		TOGGLE_RELEASE(Hotkeys.ACTIVATION_TOGGLE_RELEASE),
		REPEAT(Hotkeys.ACTIVATION_REPEAT);
		
		private final @NotNull Icon icon;
		
		private static final Map<String, KeyBindActivation> NAME_MAP = Util.make(new HashMap<>(4), m -> {
			for (KeyBindActivation value: values()) m.put(value.serialize(), value);
		});
		
		KeyBindActivation(@NotNull Icon icon) {
			this.icon = icon;
		}
		
		public @NotNull Icon getIcon() {
			return icon;
		}
		
		public static @Nullable KeyBindMapping.KeyBindActivation deserialize(String type) {
			return NAME_MAP.get(type);
		}
		
		public @NotNull String serialize() {
			return name().toLowerCase();
		}
	}
	
	interface KeyBindContext {
		Map<String, KeyBindContext> NAME_MAP = Util.make(Collections.synchronizedMap(new LinkedHashMap<>()), m -> {
			for (VanillaKeyBindContext v: VanillaKeyBindContext.values()) m.put(v.serialize(), v);
		});
		static @Nullable KeyBindMapping.KeyBindContext deserialize(String context) {
			return NAME_MAP.get(context);
		}
		static void registerContext(String serialized, KeyBindContext context) {
			NAME_MAP.put(serialized, context);
		}
		static Collection<KeyBindContext> getAllContexts() {
			return NAME_MAP.values();
		}
		
		boolean isActive();
		boolean conflictsWith(KeyBindContext other);
		Component getDisplayName();
		@NotNull Icon getIcon();
		@NotNull Icon getCropIcon();
		@NotNull String serialize();
	}
	
	enum VanillaKeyBindContext implements KeyBindContext {
		GAME(Hotkeys.CONTEXT_GAME, KeyBindSettingsIcon.CONTEXT_GAME),
		MENU(Hotkeys.CONTEXT_MENU, KeyBindSettingsIcon.CONTEXT_MENU),
		ALL(Hotkeys.CONTEXT_ALL, KeyBindSettingsIcon.CONTEXT_ALL);
		
		private final @NotNull Icon icon;
		private final @NotNull Icon cropIcon;
		private final String serialized;
		
		VanillaKeyBindContext(@NotNull Icon icon, @NotNull Icon cropIcon) {
			this.icon = icon;
			serialized = name().toLowerCase();
			this.cropIcon = cropIcon;
		}
		
		@Override public boolean isActive() {
			return this == ALL || (Minecraft.getInstance().screen == null) == (this == GAME);
		}
		
		@Override public boolean conflictsWith(KeyBindContext other) {
			return this == ALL || this == other;
		}
		
		@Override public @NotNull Component getDisplayName() {
			return new TranslatableComponent("simpleconfig.keybind.context." + serialized);
		}
		@Override public @NotNull Icon getIcon() {
			return icon;
		}
		@Override public @NotNull Icon getCropIcon() {
			return cropIcon;
		}
		@Override public @NotNull String serialize() {
			return serialized;
		}
	}
}
