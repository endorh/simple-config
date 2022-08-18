package endorh.simpleconfig.config;

import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.api.ISimpleConfig;
import endorh.simpleconfig.api.annotation.Bind;
import endorh.simpleconfig.ui.icon.SimpleConfigIcons;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;

import java.awt.Color;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static endorh.simpleconfig.api.ConfigBuilderFactoryProxy.*;
import static java.util.Arrays.asList;

public class CommonConfig {
	public static ISimpleConfig build() {
		final Supplier<List<String>> modNameSupplier = () -> ModList.get().getMods().stream()
		  .map(ModInfo::getModId).collect(Collectors.toList());
		return config(SimpleConfigMod.MOD_ID, ISimpleConfig.Type.COMMON, CommonConfig.class)
		  .withIcon(SimpleConfigIcons.Types.COMMON)
		  .withColor(0x64FFA090)
		  .withBackground("textures/block/warped_planks.png")
		  .text("desc")
		  .n(
			 group("menu", true)
				.add("wrap_configs", yesNo(true).restart())
				.add("wrap_config_exceptions", list(
				  string("").suggest(modNameSupplier))
				  .restart())
				.add("replace_config_menus", yesNo(false).restart()
				  .editable(g -> g.getGUIBoolean("wrap_configs")
				                 || !g.<List<?>>getGUI("wrap_config_exceptions").isEmpty()))
				.add("replace_menu_exceptions", list(
				  string("").suggest(modNameSupplier)
				).restart().editable(g -> g.getGUIBoolean("wrap_configs")
				                          || !g.<List<?>>getGUI("wrap_config_exceptions").isEmpty()))
		  ).n(
			 category("demo")
				.withIcon(SimpleConfigIcons.Status.INFO)
				.withColor(0x80607080)
				.withBackground("textures/block/warped_planks.png")
				.add("bool", yesNo(true))
				.add("number", number(0).min(0))
				.add("slider", fraction(0.5))
				.add("color_list", list(color(Color.GRAY).alpha(), asList(
				  Color.RED, Color.GREEN, Color.BLUE)))
		  ).buildAndRegister();
	}
	
	public enum HotKeyLogLocation {
		CHAT, RIGHT_OVERLAY, CENTER_TOAST, NONE;
	}
	
	@Bind
	public static class menu {
		@Bind public static boolean wrap_configs;
		@Bind public static List<String> wrap_config_exceptions;
		@Bind public static boolean replace_config_menus;
		@Bind public static List<String> replace_menu_exceptions;
		
		public static boolean shouldWrapConfig(String modId) {
			return wrap_configs != wrap_config_exceptions.contains(modId);
		}
		
		public static boolean shouldReplaceMenu(String modId) {
			return shouldWrapConfig(modId) &&
			       replace_config_menus != replace_menu_exceptions.contains(modId);
		}
	}
}
