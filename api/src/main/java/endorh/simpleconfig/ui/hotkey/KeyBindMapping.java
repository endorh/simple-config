package endorh.simpleconfig.ui.hotkey;

import endorh.simpleconfig.ui.icon.Icon;
import endorh.simpleconfig.ui.icon.KeyBindSettingsIcon;
import endorh.simpleconfig.ui.icon.SimpleConfigIcons.Hotkeys;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
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
	static KeyBindMapping unset() {
		return unset(ExtendedKeyBindSettings.ingame().build());
	}
	
	static KeyBindMapping unset(ExtendedKeyBindSettings settings) {
		return ExtendedKeyBindProxy.getFactory().unsetMapping(settings);
	}
	
	IntList getRequiredKeys();
	@Nullable Int2ObjectMap<String> getCharMap();
	ExtendedKeyBindSettings getSettings();
	boolean isUnset();
	boolean overlaps(KeyBindMapping other);
	
	default ITextComponent getDisplayName() {
		return getDisplayName(Style.EMPTY);
	}
	default ITextComponent getDisplayName(TextFormatting style) {
		return getDisplayName(Style.EMPTY.applyFormatting(style));
	}
	ITextComponent getDisplayName(Style style);
	KeyBindMapping copy();
	
	static KeyBindMapping parse(String serialized) {
		return ExtendedKeyBindProxy.getFactory().parseMapping(serialized);
	}
	String serialize();
	
	enum KeyBindActivation {
		PRESS(Hotkeys.ACTIVATION_PRESS),
		RELEASE(Hotkeys.ACTIVATION_RELEASE),
		BOTH(Hotkeys.ACTIVATION_BOTH),
		TOGGLE(Hotkeys.ACTIVATION_TOGGLE),
		TOGGLE_RELEASE(Hotkeys.ACTIVATION_TOGGLE_RELEASE),
		REPEAT(Hotkeys.ACTIVATION_REPEAT);
		
		private final Icon icon;
		
		private static final Map<String, KeyBindActivation> NAME_MAP = Util.make(new HashMap<>(4), m -> {
			for (KeyBindActivation value: values()) m.put(value.serialize(), value);
		});
		
		KeyBindActivation(Icon icon) {
			this.icon = icon;
		}
		
		public Icon getIcon() {
			return icon;
		}
		
		public static @Nullable KeyBindMapping.KeyBindActivation deserialize(String type) {
			return NAME_MAP.get(type);
		}
		
		public String serialize() {
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
		ITextComponent getDisplayName();
		Icon getIcon();
		Icon getCropIcon();
		String serialize();
	}
	
	enum VanillaKeyBindContext implements KeyBindContext {
		GAME(Hotkeys.CONTEXT_GAME, KeyBindSettingsIcon.CONTEXT_GAME),
		MENU(Hotkeys.CONTEXT_MENU, KeyBindSettingsIcon.CONTEXT_MENU),
		ALL(Hotkeys.CONTEXT_ALL, KeyBindSettingsIcon.CONTEXT_ALL);
		
		private final Icon icon;
		private final Icon cropIcon;
		private final String serialized;
		
		VanillaKeyBindContext(Icon icon, Icon cropIcon) {
			this.icon = icon;
			serialized = name().toLowerCase();
			this.cropIcon = cropIcon;
		}
		
		@Override public boolean isActive() {
			return this == ALL || (Minecraft.getInstance().currentScreen == null) == (this == GAME);
		}
		
		@Override public boolean conflictsWith(KeyBindContext other) {
			return this == ALL || this == other;
		}
		
		@Override public ITextComponent getDisplayName() {
			return new TranslationTextComponent("simpleconfig.keybind.context." + serialized);
		}
		@Override public Icon getIcon() {
			return icon;
		}
		@Override public Icon getCropIcon() {
			return cropIcon;
		}
		@Override public String serialize() {
			return serialized;
		}
	}
}
