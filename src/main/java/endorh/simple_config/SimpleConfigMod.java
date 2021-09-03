package endorh.simple_config;

import com.google.common.collect.Lists;
import endorh.simple_config.core.SimpleConfig;
import endorh.simple_config.core.SimpleConfigGroup;
import endorh.simple_config.core.annotation.Bind;
import endorh.simple_config.core.annotation.NotEntry;
import endorh.simple_config.core.entry.Builders;
import endorh.simple_config.core.entry.StringEntry;
import endorh.simple_config.demo.DemoConfigCategory;
import endorh.simple_config.demo.DemoServerCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.Util;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static endorh.simple_config.SimpleConfigMod.MenuButtonPosition.SPLIT_OPTIONS_BUTTON;
import static endorh.simple_config.core.SimpleConfig.group;
import static endorh.simple_config.core.entry.Builders.*;

@Mod(SimpleConfigMod.MOD_ID)
@Internal public class SimpleConfigMod {
	public static final String MOD_ID = "simple-config";
	// Storing the config instances is optional
	public static SimpleConfig CLIENT_CONFIG;
	public static SimpleConfig SERVER_CONFIG;
	
	static {
		// Trigger class loading
		Builders.nonPersistentBool(false);
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
			       .add("translation_debug_mode", nonPersistentBool(false)))
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
				final List<String> ops = Arrays.asList(pl.getOppedPlayerNames());
				final List<String> whl = Arrays.asList(pl.getWhitelistedPlayerNames());
				final List<String> nms = Arrays.asList(pl.getOnlinePlayerNames());
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
		final Supplier<List<String>> roleNameSupplier = () -> SERVER_CONFIG.<List<Pair<String, ?>>>getGUI("permissions.roles").stream().map(Pair::getKey).collect(Collectors.toList());
		StringEntry.Builder modName = string("").suggest(modNameSupplier);
		StringEntry.Builder roleName = string("").suggest(() -> {
			  List<String> roles = roleNameSupplier.get();
			  roles.add(0, "op");
			  return roles;
		  }).error(s -> roleNameSupplier.get().contains(s) || s.equals("op")? Optional.empty() : Optional.of(
			 new TranslationTextComponent("simple-config.error.unknown_role")));
		StringEntry.Builder modGroupOrName = string("").suggest(() -> {
			List<Pair<String, ?>> modGroups = SERVER_CONFIG.getGUI("permissions.mod_groups");
			List<String> names = modGroups.stream().map(Pair::getKey).collect(Collectors.toList());
			ModList.get().getMods().stream().map(ModInfo::getModId).forEachOrdered(names::add);
			names.add(0, "[all]");
			return names;
		});
		SERVER_CONFIG = SimpleConfig.builder(MOD_ID, Type.SERVER, ServerConfig.class)
		  .text("desc")
		  .n(group("permissions", true)
		       .add("roles", map(
					string("").notEmpty()
					  .error(s -> s.equals("op")? Optional.of(new TranslationTextComponent(
						 "simple-config.config.error.role.reserved", "op")) : Optional.empty()),
					list(playerName)))
		       .add("mod_groups", map(
					string("").notEmpty()
					  .error(s -> s.equals("[all]")? Optional.of(new TranslationTextComponent(
						 "simple-config.config.error.mod_group.reserved", "[all]")) : Optional.empty()),
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
				final String name = player.getScoreboardName();
				final Set<String> roles = permissions.roles.entrySet().stream().filter(
				  e -> e.getValue().contains(name)
				).map(Entry::getKey).collect(Collectors.toSet());
				if (player.hasPermissionLevel(2)) roles.add("op");
				final Set<String> mod_groups = permissions.mod_groups.entrySet().stream().filter(
				  e -> e.getValue().getKey() == ListType.BLACKLIST
				       ^ e.getValue().getValue().contains(mod)
				).map(Entry::getKey).collect(Collectors.toSet());
				mod_groups.add("[all]");
				mod_groups.add(mod);
				ConfigPermission permission = ConfigPermission.DENY;
				for (Pair<String, Pair<ConfigPermission, List<String>>> rule : rules) {
					if (roles.contains(rule.getKey())
					    && rule.getValue().getValue().stream().anyMatch(mod_groups::contains))
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
		ALLOW, DENY // VIEW
	}
	
	@SuppressWarnings("unused") public enum MenuButtonPosition {
		SPLIT_OPTIONS_BUTTON, LEFT_OF_OPTIONS_BUTTON,
		TOP_LEFT_CORNER, TOP_RIGHT_CORNER,
		BOTTOM_LEFT_CORNER, BOTTOM_RIGHT_CORNER
	}
}
