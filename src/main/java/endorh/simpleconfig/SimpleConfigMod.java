package endorh.simpleconfig;

import com.google.common.collect.Lists;
import endorh.simpleconfig.core.SimpleConfig;
import endorh.simpleconfig.core.SimpleConfigGroup;
import endorh.simpleconfig.core.annotation.Bind;
import endorh.simpleconfig.core.annotation.NotEntry;
import endorh.simpleconfig.core.entry.Builders;
import endorh.simpleconfig.core.entry.EnumEntry.ITranslatedEnum;
import endorh.simpleconfig.core.entry.StringEntry;
import endorh.simpleconfig.demo.DemoConfigCategory;
import endorh.simpleconfig.demo.DemoServerCategory;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.Util;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.util.text.event.HoverEvent.Action;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.registries.IForgeRegistry;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static endorh.simpleconfig.SimpleConfigMod.MenuButtonPosition.SPLIT_OPTIONS_BUTTON;
import static endorh.simpleconfig.core.SimpleConfig.group;
import static endorh.simpleconfig.core.entry.Builders.*;
import static net.minecraft.client.util.InputMappings.Type.KEYSYM;
import static net.minecraftforge.client.settings.KeyConflictContext.GUI;
import static net.minecraftforge.client.settings.KeyModifier.CONTROL;
import static net.minecraftforge.client.settings.KeyModifier.SHIFT;

@Mod(SimpleConfigMod.MOD_ID)
@EventBusSubscriber(value = Dist.CLIENT, modid = SimpleConfigMod.MOD_ID, bus = Bus.MOD)
@Internal public class SimpleConfigMod {
	public static final String MOD_ID = "simpleconfig";
	public static SoundEvent UI_TAP;
	public static SoundEvent UI_DOUBLE_TAP;
	
	// Storing the config instances is optional
	public static SimpleConfig CLIENT_CONFIG;
	public static SimpleConfig SERVER_CONFIG;
	
	static {
		// Trigger class loading
		Builders.bool(false);
	}
	
	public SimpleConfigMod() {
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			// Create and register the client config for the mod
			CLIENT_CONFIG = SimpleConfig.builder(MOD_ID, Type.CLIENT, ClientConfig.class)
			  .add("add_pause_menu_button", bool(true))
			  .add("menu_button_position", enum_(SPLIT_OPTIONS_BUTTON))
			  .n(group("confirm")
			       .add("save", bool(true))
			       .add("unsaved", bool(true))
			       .add("reset", bool(false))
			       .add("group_reset", bool(true))
			       .add("restore", bool(false))
			       .add("group_restore", bool(true)))
			  .n(group("advanced")
			       .add("tooltip_key", enum_(TooltipDisplayKey.NONE))
			       .add("tooltip_max_width", percent(60F))
			       .add("prefer_combo_box", number(8))
			       .add("max_options_in_config_comment", number(16).min(5))
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
			       .add("search_history", caption(
						number(20).min(0).max(1000), list(string(""))))
			       .add("regex_search_history", caption(
						number(20).max(1000), list(pattern(""))))
			       .add("translation_debug_mode", bool(false).temp()))
			  // Hook here the demo category
			  .n(DemoConfigCategory.getDemoCategory())
			  // Change the background texture
			  .setBackground("textures/block/bookshelf.png")
			  .buildAndRegister();
		});
		// Entry builders are immutable, so they can be cached, reused and modified
		//   freely without any concern. All their methods return modified copies
		final StringEntry.Builder playerName = string("").suggest(() -> {
			MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
			final List<String> names = Lists.newArrayList();
			if (server != null) {
				final PlayerList pl = server.getPlayerList();
				final List<String> ops = Arrays.asList(pl.getOpNames());
				final List<String> whl = Arrays.asList(pl.getWhiteListNames());
				final List<String> nms = Arrays.asList(pl.getPlayerNamesArray());
				whl.removeAll(ops);
				nms.removeAll(ops);
				nms.removeAll(whl);
				names.addAll(ops);
				names.addAll(whl);
				names.addAll(nms);
			}
			return names;
		});
		final Supplier<List<String>> modNameSupplier = () -> ModList.get().getMods().stream().map(ModInfo::getModId).collect(Collectors.toList());
		final Supplier<List<String>> roleNameSupplier = () ->
		  new ArrayList<>(SERVER_CONFIG.<Map<String, List<String>>>getFromGUI("permissions.roles").keySet());
		StringEntry.Builder modName = string("").suggest(modNameSupplier);
		StringEntry.Builder roleName = string("").suggest(() -> {
			  List<String> roles = roleNameSupplier.get();
			  roles.add(0, "op");
			  return roles;
		  }).error(s -> roleNameSupplier.get().contains(s) || s.equals("op")? Optional.empty() : Optional.of(
			 new TranslationTextComponent("simpleconfig.error.unknown_role")));
		StringEntry.Builder modGroupOrName = string("").suggest(() -> {
			List<Pair<String, ?>> modGroups = SERVER_CONFIG.getGUI("permissions.mod_groups");
			List<String> names = modGroups.stream().map(Pair::getKey).collect(Collectors.toList());
			ModList.get().getMods().stream().map(ModInfo::getModId).forEachOrdered(names::add);
			names.add(0, "[all]");
			return names;
		});
		SERVER_CONFIG = SimpleConfig.builder(MOD_ID, Type.SERVER, ServerConfig.class)
		  .text("desc", makeLink(
			 "simpleconfig.config.server.desc.permission_level",
			 "simpleconfig.config.server.desc.permission_level:help",
			 "https://minecraft.fandom.com/wiki/Permission_level",
			 TextFormatting.DARK_GRAY, TextFormatting.UNDERLINE))
		  .n(group("permissions", true)
		       .add("roles", map(
					string("").notEmpty()
					  .error(s -> s.equals("op")? Optional.of(new TranslationTextComponent(
						 "simpleconfig.config.error.role.reserved", "op")) : Optional.empty()),
					list(playerName)))
		       .add("mod_groups", map(
					string("").notEmpty()
					  .error(s -> s.equals("[all]")? Optional.of(new TranslationTextComponent(
						 "simpleconfig.config.error.mod_group.reserved", "[all]")) : Optional.empty()),
					caption(enum_(ListType.WHITELIST), list(modName))))
		       .add("rules", pairList(
					roleName, caption(enum_(ConfigPermission.ALLOW), list(modGroupOrName)),
					Lists.newArrayList(Pair.of("op", Pair.of(ConfigPermission.ALLOW, Lists.newArrayList("[all]")))))))
		  .text("end")
		  // Register the demo server config category as well
		  .n(DemoServerCategory.getDemoServerCategory())
		  .setBackground("minecraft:blackstone_bricks")
		  .buildAndRegister();
	}
	
	/**
	 * Client config backing class
	 */
	public static class ClientConfig {
		@Bind public static boolean add_pause_menu_button;
		@Bind public static MenuButtonPosition menu_button_position;
		@Bind public static class confirm {
			@Bind public static boolean reset;
			@Bind public static boolean restore;
			@Bind public static boolean group_reset;
			@Bind public static boolean group_restore;
			@Bind public static boolean unsaved;
			@Bind public static boolean save;
		}
		@Bind public static class advanced {
			@Bind public static TooltipDisplayKey tooltip_key;
			@Bind public static float tooltip_max_width;
			@Bind public static int prefer_combo_box;
			@Bind public static int max_options_in_config_comment;
			@Bind public static Map<Integer, Color> color_picker_saved_colors;
			public static int search_history_size;
			@NotEntry public static List<String> search_history;
			public static int regex_search_history_size;
			@NotEntry public static List<Pattern> regex_search_history;
			@Bind public static boolean translation_debug_mode;
			
			static void bake(SimpleConfigGroup g) {
				final Pair<Integer, List<String>> sh = g.get("search_history");
				search_history = sh.getValue();
				search_history_size = sh.getKey();
				final Pair<Integer, List<Pattern>> rsh = g.get("regex_search_history");
				regex_search_history = rsh.getValue();
				regex_search_history_size = rsh.getKey();
			}
		}
	}
	
	/**
	 * Server config backing class
	 */
	public static class ServerConfig {
		@Bind public static class permissions {
			// Each role contains a list of players
			//   There's a reserved role, "op", which contains all server operators
			@Bind public static Map<String, List<String>> roles;
			// Each mod group contains a list of mods, either included or excluded
			//   There's a reserved group, "[all]", which contains all mods
			@Bind public static Map<String, Pair<ListType, List<String>>> mod_groups;
			// Each pair contains the permission and a pair with a role
			//   name and a list of mod group names or just mod names
			// The permissions are applied in order. Each player is ruled
			//   by the last rule that affects them for each mod.
			// By default, op players have ALLOW permission for the [all] mod group
			@Bind public static List<Pair<String, Pair<ConfigPermission, List<String>>>> rules;
			
			public static ConfigPermission permissionFor(PlayerEntity player, String mod) {
				final Set<String> roles = permissions.roles.entrySet().stream().filter(
				  e -> e.getValue().contains(player.getScoreboardName())
				).map(Entry::getKey).collect(Collectors.toSet());
				if (player.hasPermissions(4)) // Top level admins/single-player cheats
					return ConfigPermission.ALLOW;
				if (player.hasPermissions(2)) roles.add("op");
				final Set<String> modGroups = permissions.mod_groups.entrySet().stream().filter(
				  e -> e.getValue().getKey() == ListType.BLACKLIST ^ e.getValue().getValue().contains(mod)
				).map(Entry::getKey).collect(Collectors.toSet());
				modGroups.add("[all]");
				modGroups.add(mod);
				ConfigPermission permission = ConfigPermission.DENY;
				for (Pair<String, Pair<ConfigPermission, List<String>>> rule : rules) {
					if (roles.contains(rule.getKey())
					    && rule.getValue().getValue().stream().anyMatch(modGroups::contains))
						permission = rule.getValue().getKey();
				}
				return permission;
			}
		}
	}
	
	public enum ListType {
		WHITELIST, BLACKLIST
	}
	
	public enum ConfigPermission {
		ALLOW, DENY // VIEW // SAVE_PRESET
	}
	
	public enum MenuButtonPosition {
		SPLIT_OPTIONS_BUTTON, LEFT_OF_OPTIONS_BUTTON,
		TOP_LEFT_CORNER, TOP_RIGHT_CORNER,
		BOTTOM_LEFT_CORNER, BOTTOM_RIGHT_CORNER
	}
	
	public enum TooltipDisplayKey implements ITranslatedEnum {
		NONE(() -> true),
		CTRL(Screen::hasControlDown),
		ALT(Screen::hasAltDown),
		SHIFT(Screen::hasShiftDown);
		
		private final Supplier<Boolean> shouldDisplay;
		TooltipDisplayKey(Supplier<Boolean> shouldDisplay) {
			this.shouldDisplay = shouldDisplay;
		}
		
		public boolean shouldDisplayTooltip() {
			return shouldDisplay.get();
		}
		
		@Override public IFormattableTextComponent getDisplayName() {
			return new TranslationTextComponent("simpleconfig.config.enum.tooltip_display_key." + name().toLowerCase());
		}
	}
	
	@SubscribeEvent
	protected static void onRegisterSounds(RegistryEvent.Register<SoundEvent> event) {
		final IForgeRegistry<SoundEvent> r = event.getRegistry();
		UI_TAP = regSound(r, new ResourceLocation(MOD_ID, "ui_tap"));
		UI_DOUBLE_TAP = regSound(r, new ResourceLocation(MOD_ID, "ui_double_tap"));
	}
	
	protected static SoundEvent regSound(IForgeRegistry<SoundEvent> registry, ResourceLocation name) {
		SoundEvent event = new SoundEvent(name);
		event.setRegistryName(name);
		registry.register(event);
		return event;
	}
	
	@EventBusSubscriber(value = Dist.CLIENT, bus = Bus.MOD, modid = MOD_ID)
	public static class KeyBindings {
		public static final String CATEGORY = "Simple Config";
		public static KeyBinding
		  SEARCH, PREV_PAGE, NEXT_PAGE, UNDO, REDO, PREV_ERROR, NEXT_ERROR, SAVE;
		
		@SubscribeEvent public static void register(FMLClientSetupEvent event) {
			event.enqueueWork(() -> {
				SEARCH = reg("search", CONTROL, 70);        // Ctrl + F
				PREV_PAGE = reg("prev_page", CONTROL, 266); // Ctrl + PgUp
				NEXT_PAGE = reg("next_page", CONTROL, 267); // Ctrl + PgDown
				UNDO = reg("undo", CONTROL, 90);            // Ctrl + Z
				REDO = reg("redo", CONTROL, 89);            // Ctrl + Y
				PREV_ERROR = reg("prev_error", SHIFT, 290); // Shift + F1
				NEXT_ERROR = reg("next_error", 290);        // F1
				SAVE = reg("save", CONTROL, 83);            // Ctrl + S
			});
		}
		
		private static KeyBinding reg(String name, int keyCode) {
			KeyBinding binding = new KeyBinding(MOD_ID + ".key." + name, GUI, KEYSYM.getOrCreate(keyCode), CATEGORY);
			ClientRegistry.registerKeyBinding(binding);
			return binding;
		}
		private static KeyBinding reg(String name, KeyModifier modifier, int keyCode) {
			KeyBinding binding = new KeyBinding(MOD_ID + ".key." + name, GUI, modifier, KEYSYM.getOrCreate(keyCode), CATEGORY);
			ClientRegistry.registerKeyBinding(binding);
			return binding;
		}
	}
	
	private static IFormattableTextComponent makeLink(
	  String key, @Nullable String tooltipKey, String url, TextFormatting... styles
	) {
		return new TranslationTextComponent(key).withStyle(s -> {
			s = s.applyFormats(styles);
			if (tooltipKey != null)
				s = s.withHoverEvent(new HoverEvent(
				  Action.SHOW_TEXT, new TranslationTextComponent(tooltipKey)));
			return s.withClickEvent(new ClickEvent(
			  ClickEvent.Action.OPEN_URL, url));
		});
	}
}
