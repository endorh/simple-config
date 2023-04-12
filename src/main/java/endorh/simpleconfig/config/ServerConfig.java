package endorh.simpleconfig.config;

import com.google.common.collect.Lists;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.api.SimpleConfig;
import endorh.simpleconfig.api.annotation.Bind;
import endorh.simpleconfig.api.entry.StringEntryBuilder;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.HoverEvent.Action;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static endorh.simpleconfig.api.ConfigBuilderFactoryProxy.*;

/**
 * Server config backing class
 */
public class ServerConfig {
	public static SimpleConfig build() {
		// Entry builders are immutable, so they can be cached, reused and modified
		//   freely without any concern. All their methods return modified copies
		final StringEntryBuilder playerName = string("").suggest(() -> {
			MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
			final List<String> names = Lists.newArrayList();
			if (server != null) {
				final PlayerList pl = server.getPlayerList();
				final List<String> ops = Lists.newArrayList(pl.getOpNames());
				final List<String> whl = Lists.newArrayList(pl.getWhiteListNames());
				final List<String> nms = Lists.newArrayList(pl.getPlayerNamesArray());
				whl.removeAll(ops);
				nms.removeAll(ops);
				nms.removeAll(whl);
				names.addAll(ops);
				names.addAll(whl);
				names.addAll(nms);
			}
			return names;
		});
		final Supplier<List<String>> roleNameSupplier = () -> new ArrayList<>(
		  SimpleConfigMod.SERVER_CONFIG.<Map<String, List<String>>>getFromGUI("permissions.roles").keySet());
		final Supplier<List<String>> modNameSupplier = () -> ModList.get().getMods().stream()
		  .map(IModInfo::getModId).collect(Collectors.toList());
		StringEntryBuilder modName = string("").suggest(modNameSupplier);
		StringEntryBuilder roleName = string("").suggest(() -> {
			List<String> roles = roleNameSupplier.get();
			roles.add(0, "[all]");
			roles.add(1, "[op]");
			return roles;
		}).error(s -> roleNameSupplier.get().contains(s) || "[op]".equals(s) || "[all]".equals(s)
		              ? Optional.empty() : Optional.of(new TranslatableComponent(
							 "simpleconfig.config.error.unknown_role", s)));
		StringEntryBuilder modGroupOrName = string("").suggest(() -> {
			List<Pair<String, ?>> modGroups = SimpleConfigMod.SERVER_CONFIG.getGUI("permissions.mod_groups");
			List<String> names = modGroups.stream().map(Pair::getKey).collect(Collectors.toList());
			ModList.get().getMods().stream().map(IModInfo::getModId).forEachOrdered(names::add);
			names.add(0, "[all]");
			return names;
		});
		return config(SimpleConfigMod.MOD_ID, SimpleConfig.Type.SERVER, ServerConfig.class)
		  .withIcon(SimpleConfigIcons.Types.SERVER)
		  .withColor(0x648090FF)
		  .withBackground("textures/block/blackstone_bricks.png")
		  .text("desc", makeLink(
		    "simpleconfig.config.server.desc.permission_level",
		    "simpleconfig.config.server.desc.permission_level:help",
		    "https://minecraft.fandom.com/wiki/Permission_level",
		    ChatFormatting.DARK_GRAY, ChatFormatting.UNDERLINE))
		  .n(group("permissions", true)
			    .add("roles", map(
				   string("").notEmpty()
				     .error(s -> "[op]".equals(s) || "[all]".equals(s)? Optional.of(
					    new TranslatableComponent(
						   "simpleconfig.config.error.role.reserved", s)) : Optional.empty()),
				   list(playerName)))
			    .add("mod_groups", map(
				   string("").notEmpty()
				     .error(s -> "[all]".equals(s)? Optional.of(new TranslatableComponent(
					    "simpleconfig.config.error.mod_group.reserved", s)) : Optional.empty()),
				   caption(option(ListType.WHITELIST), list(modName))))
			    .add("rules", pairList(
				   roleName, caption(
				     pair(option(ConfigPermission.EDIT_SERVER_CONFIG), option(
					    PresetPermission.SAVE_PRESETS))
					    .withSplitPosition(0.4),
				     list(modGroupOrName)
				   ), Lists.newArrayList(Pair.of("[op]", Pair.of(
				     Pair.of(
					    ConfigPermission.EDIT_SERVER_CONFIG,
					    PresetPermission.SAVE_PRESETS), Lists.newArrayList("[all]"))))))
			    .add("hotkey_rules", pairList(
				   roleName, yesNo(true), Lists.newArrayList(Pair.of("[op]", true))))
		       .add("datapack_permission", option(ConfigPermission.EDIT_SERVER_CONFIG)
		         .restrict(ConfigPermission.VIEW_SERVER_CONFIG))
		       .add("broadcast_datapack_config_changes", yesNo(false))
		  ).text("end")
		  .buildAndRegister();
	}
	
	private static MutableComponent makeLink(
	  String key, @Nullable String tooltipKey, String url, ChatFormatting... styles
	) {
		return new TranslatableComponent(key).withStyle(s -> {
			s = s.applyFormats(styles);
			if (tooltipKey != null)
				s = s.withHoverEvent(new HoverEvent(
				  Action.SHOW_TEXT, new TranslatableComponent(tooltipKey)));
			return s.withClickEvent(new ClickEvent(
			  ClickEvent.Action.OPEN_URL, url));
		});
	}
	
	public enum ListType {
		WHITELIST, BLACKLIST
	}
	
	public enum ConfigPermission {
		INHERIT(false, false),
		DENY(false, false),
		VIEW_SERVER_CONFIG(true, false),
		EDIT_SERVER_CONFIG(true, true);
		
		private final boolean canView;
		private final boolean canEdit;
		
		ConfigPermission(boolean canView, boolean canEdit) {
			this.canView = canView;
			this.canEdit = canEdit;
		}
		
		public boolean canView() {
			return canView;
		}
		public boolean canEdit() {
			return canEdit;
		}
	}
	
	public enum PresetPermission {
		INHERIT(false, false),
		LOAD_PRESETS(true, false),
		SAVE_PRESETS(true, true);
		
		private final boolean canLoad;
		private final boolean canSave;
		
		PresetPermission(boolean canLoad, boolean canSave) {
			this.canLoad = canLoad;
			this.canSave = canSave;
		}
		
		public boolean canLoad() {
			return canLoad;
		}
		public boolean canSave() {
			return canSave;
		}
	}
	
	@Bind
	public static class permissions {
		private static final String MINECRAFT_MOD_ID = "minecraft";
		// Each role contains a list of players
		//   The roles "[op]" and "[all]" are reserved
		@Bind public static Map<String, List<String>> roles;
		// Each mod group contains a list of mods, either included or excluded
		//   There's a reserved group, "[all]", which contains all mods
		@Bind public static Map<String, Pair<ListType, List<String>>> mod_groups;
		// Each pair contains the permission and a pair with a role
		//   name and a list of mod group names or just mod names
		// The permissions are applied in order. Each player is ruled
		//   by the last rule that affects them for each mod.
		// By default, op players have ALLOW permission for the [all] mod group
		@Bind public static List<Pair<String, Pair<Pair<ConfigPermission, PresetPermission>, List<String>>>> rules;
		@Bind public static List<Pair<String, Boolean>> hotkey_rules;
		
		// Permission for datapacks
		@Bind public static ConfigPermission datapack_permission;
		@Bind public static boolean broadcast_datapack_config_changes;
		
		@OnlyIn(Dist.CLIENT)
		public static Pair<ConfigPermission, PresetPermission> permissionFor(String mod) {
			LocalPlayer player = Minecraft.getInstance().player;
			return player == null? Pair.of(ConfigPermission.DENY, PresetPermission.LOAD_PRESETS)
			                     : permissionFor(player, mod);
		}
		
		public static Pair<ConfigPermission, PresetPermission> permissionFor(
		  Player player, String mod
		) {
			final Set<String> roles = permissions.roles.entrySet().stream().filter(
			  e -> e.getValue().contains(player.getScoreboardName())
			).map(Entry::getKey).collect(Collectors.toSet());
			if (player.hasPermissions(4)) // Top level admins/single-player cheats
				return Pair.of(ConfigPermission.EDIT_SERVER_CONFIG, PresetPermission.SAVE_PRESETS);
			if (player.hasPermissions(2)) roles.add("[op]");
			roles.add("[all]");
			final Set<String> modGroups = permissions.mod_groups.entrySet().stream().filter(
			  e -> e.getValue().getKey() == ListType.BLACKLIST ^ e.getValue().getValue().contains(mod)
			).map(Entry::getKey).collect(Collectors.toSet());
			modGroups.add("[all]");
			modGroups.add(mod);
			ConfigPermission config = ConfigPermission.DENY;
			PresetPermission preset = PresetPermission.LOAD_PRESETS;
			for (Pair<String, Pair<Pair<ConfigPermission, PresetPermission>, List<String>>> rule: rules) {
				if (roles.contains(rule.getKey()) &&
				    rule.getValue().getValue().stream().anyMatch(modGroups::contains)) {
					Pair<ConfigPermission, PresetPermission> pair = rule.getValue().getKey();
					if (pair.getKey() != ConfigPermission.INHERIT) config = pair.getKey();
					if (pair.getValue() != PresetPermission.INHERIT) preset = pair.getValue();
				}
			}
			return Pair.of(config, preset);
		}
		
		@OnlyIn(Dist.CLIENT) public static boolean canEditServerHotKeys() {
			LocalPlayer player = Minecraft.getInstance().player;
			return player != null && canEditServerHotKeys(player);
		}
		
		public static boolean canAccessServerProperties(Player player) {
			return DistExecutor.unsafeRunForDist(() -> () -> false, () -> () -> {
				MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
				return server.isDedicatedServer() && permissionFor(player, MINECRAFT_MOD_ID).getLeft().canEdit();
			});
		}
		
		public static boolean canEditServerHotKeys(Player player) {
			final Set<String> roles = permissions.roles.entrySet().stream().filter(
			  e -> e.getValue().contains(player.getScoreboardName())
			).map(Entry::getKey).collect(Collectors.toSet());
			if (player.hasPermissions(4)) // Top level admins/single-player cheats
				return true;
			if (player.hasPermissions(2)) roles.add("[op]");
			roles.add("[all]");
			for (Pair<String, Boolean> rule: Lists.reverse(hotkey_rules))
				if (roles.contains(rule.getKey())) return rule.getValue();
			return false;
		}
	}
}
