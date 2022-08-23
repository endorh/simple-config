package endorh.simpleconfig.config;

import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.api.EntryTag;
import endorh.simpleconfig.api.SimpleConfig;
import endorh.simpleconfig.api.annotation.Bind;
import endorh.simpleconfig.api.entry.KeyBindEntryBuilder;
import endorh.simpleconfig.api.ui.hotkey.ExtendedKeyBind;
import endorh.simpleconfig.api.ui.hotkey.ExtendedKeyBindProvider;
import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping;
import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping.VanillaKeyBindContext;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons.Buttons;
import endorh.simpleconfig.config.CommonConfig.HotKeyLogLocation;
import endorh.simpleconfig.core.SimpleConfigGUIManager;
import endorh.simpleconfig.core.SimpleConfigImpl;
import endorh.simpleconfig.demo.DemoConfigCategory;
import net.minecraft.Util;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static endorh.simpleconfig.api.ConfigBuilderFactoryProxy.*;
import static endorh.simpleconfig.config.ClientConfig.KeyBindings.EDIT_CONFIG_HOTKEYS;
import static endorh.simpleconfig.config.ClientConfig.KeyBindings.OPEN_MOD_LIST;
import static endorh.simpleconfig.config.ClientConfig.MenuButtonPosition.SPLIT_OPTIONS_BUTTON;
import static endorh.simpleconfig.config.CommonConfig.HotKeyLogLocation.*;

/**
 * Client config backing class
 */
@OnlyIn(Dist.CLIENT) public class ClientConfig {
	@Internal public static SimpleConfig build() {
		KeyBindings.register();
		final Supplier<List<String>> modNameSupplier = () -> ModList.get().getMods().stream()
		  .map(IModInfo::getModId).collect(Collectors.toList());
		return config(SimpleConfigMod.MOD_ID, SimpleConfig.Type.CLIENT, ClientConfig.class)
		  .withIcon(SimpleConfigIcons.Types.CLIENT)
		  .withColor(0x6490FF80)
		  .withBackground("textures/block/bookshelf.png")
		  .n(
			 group("menu", true)
				.add("add_pause_menu_button", yesNo(true))
				.add("menu_button_position", option(SPLIT_OPTIONS_BUTTON)
				  .editable(g -> g.getGUIBoolean("add_pause_menu_button")))
		  ).n(
			 group("hotkey_log")
				.add("hotkey_log_location", option(RIGHT_OVERLAY))
				.add("log_hotkey_actions", yesNo(true))
				.add("remote_hotkey_log_location", option(CHAT).withTags(EntryTag.OPERATOR))
				.add("log_remote_hotkey_actions", yesNo(true).withTags(EntryTag.OPERATOR))
				.add("max_logged_actions", number(10).min(1))
				.n(
				  group("overlay")
					 .add("background_opacity", fraction(0.5F)
						.editable(g -> g.getParent().getGUI("hotkey_log_location") == RIGHT_OVERLAY
						               || g.getParent().getGUI("remote_hotkey_log_location") == RIGHT_OVERLAY))
					 .add("display_time", number(2.0F).min(0)
						.add_field("ms", f -> (int) (f * 1000), Integer.class)
						.editable(g -> g.getParent().getGUI("hotkey_log_location") == RIGHT_OVERLAY
						               || g.getParent().getGUI("remote_hotkey_log_location") == RIGHT_OVERLAY))
				).n(
				  group("toast")
					 .add("relative_height", fraction(0.1F)
						.editable(g -> g.getParent().getGUI("hotkey_log_location") == CENTER_TOAST
						               || g.getParent().getGUI("remote_hotkey_log_location") == CENTER_TOAST))
					 .add("background_opacity", fraction(0F)
						.editable(g -> g.getParent().getGUI("hotkey_log_location") == CENTER_TOAST
						               || g.getParent().getGUI("remote_hotkey_log_location") == CENTER_TOAST))
					 .add("display_time", number(1F).min(0)
						.add_field("ms", f -> (int) (f * 1000), Integer.class)
						.editable(g -> g.getParent().getGUI("hotkey_log_location") == CENTER_TOAST
						               || g.getParent().getGUI("remote_hotkey_log_location") == CENTER_TOAST)))
		  ).n(
			 group("confirm")
				.add("save", yesNo(true))
				.add("discard", yesNo(true))
				.add("overwrite_external", yesNo(true))
				.add("overwrite_remote", yesNo(true))
				.add("reset", yesNo(false))
				.add("group_reset", yesNo(true))
				.add("restore", yesNo(false))
				.add("group_restore", yesNo(true))
				.add("save_hotkeys", yesNo(true))
				.add("discard_hotkeys", yesNo(true))
		  ).n(
			 group("advanced")
				.add("show_ui_tips", yesNo(true))
			   .add("allow_over_scroll", yesNo(false))
				.add("tooltip_max_width", percent(60F))
				.add("prefer_combo_box", number(8))
				.add("max_options_in_config_comment", number(16).min(5).restart())
				.add("color_picker_saved_colors", map(
				  number(0), color(Color.BLACK).alpha(),
				  Util.make(new HashMap<>(), m -> {
					  // Default Minecraft palette (Java edition) (only light colors)
					  m.put(0, new Color(0xFF5555)); // Red
					  m.put(1, new Color(0xFFAA00)); // Gold
					  m.put(2, new Color(0xFFFF55)); // Yellow
					  m.put(3, new Color(0x55FF55)); // Green
					  m.put(4, new Color(0x55FFFF)); // Aqua
					  m.put(5, new Color(0x5555FF)); // Blue
					  m.put(6, new Color(0xFF55FF)); // Light Purple
					  m.put(7, new Color(0xAAAAAA)); // Gray
				  })))
			   .n(group("search")
			        .add("search_filter", yesNo(false))
			        .add("search_regex", yesNo(false))
			        .add("search_case_sensitive", yesNo(false))
			        .add("search_tooltips", yesNo(true))
			        .add("search_history", caption(number(20).min(0).max(1000), list(string("")))
				       .split_fields("size"))
			        .add("regex_search_history", caption(number(20).max(1000), list(pattern("")))
				       .split_fields("size")))
				.add("translation_debug_mode", enable(false).temp())
		  ).n(
			 category("hotkeys")
			   .withColor(0x802442FF)
			   .withIcon(Buttons.KEYBOARD)
			   .add("open_mod_list", keyBind(OPEN_MOD_LIST))
			   .add("edit_config_hotkeys", keyBind(EDIT_CONFIG_HOTKEYS))
			   .add("mod_config_hotkeys", map(
				  key().guiError(
					 m -> m.getSettings().getContext() != VanillaKeyBindContext.GAME
					      ? Optional.of(new TranslatableComponent(
							  "simpleconfig.config.error.unsupported_hotkey_context",
							  m.getSettings().getContext().serialize())) : Optional.empty()),
				  string("").suggest(modNameSupplier)).expand())
			 // Hook here the demo category
		  ).n(DemoConfigCategory.getDemoCategory())
		  .buildAndRegister();
	}
	
	public enum MenuButtonPosition {
		SPLIT_OPTIONS_BUTTON, LEFT_OF_OPTIONS_BUTTON,
		TOP_LEFT_CORNER, TOP_RIGHT_CORNER,
		BOTTOM_LEFT_CORNER, BOTTOM_RIGHT_CORNER
	}
	
	@Bind public static class menu {
		@Bind public static boolean add_pause_menu_button;
		@Bind public static MenuButtonPosition menu_button_position;
	}
	
	@Bind public static class hotkey_log {
		@Bind public static HotKeyLogLocation hotkey_log_location;
		@Bind public static boolean log_hotkey_actions;
		@Bind public static HotKeyLogLocation remote_hotkey_log_location;
		@Bind public static boolean log_remote_hotkey_actions;
		@Bind public static int max_logged_actions;
		
		@Bind public static class overlay {
			@Bind public static float background_opacity;
			@Bind public static int display_time_ms;
		}
		
		@Bind public static class toast {
			@Bind public static float relative_height;
			@Bind public static float background_opacity;
			@Bind public static int display_time_ms;
		}
	}
	
	@Bind public static class confirm {
		@Bind public static boolean save;
		@Bind public static boolean discard;
		@Bind public static boolean overwrite_external;
		@Bind public static boolean overwrite_remote;
		@Bind public static boolean reset;
		@Bind public static boolean restore;
		@Bind public static boolean group_reset;
		@Bind public static boolean group_restore;
		@Bind public static boolean save_hotkeys;
		@Bind public static boolean discard_hotkeys;
	}
	
	@Bind public static class advanced {
		@Bind public static boolean show_ui_tips;
		@Bind public static boolean allow_over_scroll;
		@Bind public static float tooltip_max_width;
		@Bind public static int prefer_combo_box;
		@Bind public static int max_options_in_config_comment = 4;
		@Bind public static Map<Integer, Color> color_picker_saved_colors;
		@Bind public static class search {
			@Bind public static boolean search_filter;
			@Bind public static boolean search_regex;
			@Bind public static boolean search_case_sensitive;
			@Bind public static boolean search_tooltips;
			@Bind public static int search_history_size;
			@Bind public static List<String> search_history;
			@Bind public static int regex_search_history_size;
			@Bind public static List<Pattern> regex_search_history;
		}
		@Bind public static boolean translation_debug_mode;
	}
	
	@Bind public static class hotkeys {
		@Bind public static Map<KeyBindMapping, String> mod_config_hotkeys;
		public static List<ExtendedKeyBind> mod_config_keybinds;
		
		@Bind public static void bake() {
			mod_config_keybinds = mod_config_hotkeys.entrySet().stream().map(e -> {
				KeyBindMapping mapping = e.getKey();
				String modId = e.getValue();
				if (mapping.getSettings().getContext() != VanillaKeyBindContext.GAME) return null;
				if (!SimpleConfigImpl.getConfigModIds().contains(modId)) return null;
				return ExtendedKeyBind.of(SimpleConfigMod.MOD_ID,
				  new TranslatableComponent(
				    "simpleconfig.keybind.open_mod_config",
				    SimpleConfigImpl.getModNameOrId(modId)),
				  mapping, () -> SimpleConfigGUIManager.showConfigGUI(modId));
			}).filter(Objects::nonNull).collect(Collectors.toList());
		}
	}
	
	public static class KeyBindings {
		public static final ExtendedKeyBind
		  OPEN_MOD_LIST = keyBind(
		  "right.alt>\"m\"", SimpleConfigGUIManager::showModListGUI),
		  EDIT_CONFIG_HOTKEYS = keyBind(
		    "right.alt>\"h\"", SimpleConfigGUIManager::showConfigHotkeysGUI);
		
		public static class ModExtendedKeyBindProvider implements ExtendedKeyBindProvider {
			@Override public Iterable<ExtendedKeyBind> getActiveKeyBinds() {
				return hotkeys.mod_config_keybinds;
			}
			@Override public Iterable<ExtendedKeyBind> getAllKeyBinds() {
				return Collections.emptyList();
			}
		}
		
		private static void register() {
			ExtendedKeyBindProvider.registerProvider(new ModExtendedKeyBindProvider());
			ExtendedKeyBindProvider.registerKeyBinds(OPEN_MOD_LIST, EDIT_CONFIG_HOTKEYS);
		}
		
		@NotNull private static ExtendedKeyBind keyBind(String def, Runnable action) {
			return ExtendedKeyBind.of(SimpleConfigMod.MOD_ID, "", def, action);
		}
	}
	
	private static KeyBindEntryBuilder keyBind(ExtendedKeyBind keyBind) {
		return key(keyBind).inheritTitle();
	}
}
