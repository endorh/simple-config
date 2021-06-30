package dnj.simple_config;

import dnj.simple_config.core.SimpleConfig;
import dnj.simple_config.core.SimpleConfigSync;
import dnj.simple_config.core.annotation.ConfigEntry;
import dnj.simple_config.demo.DemoConfigCategory;
import dnj.simple_config.demo.DemoServerConfig;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static dnj.simple_config.SimpleConfigMod.MenuButtonPosition.SPLIT_OPTIONS_BUTTON;

@Mod(SimpleConfigMod.MOD_ID)
public class SimpleConfigMod {
	public static final String MOD_ID = "simple-config";
	private static final Logger LOGGER = LogManager.getLogger();
	
	public static boolean add_pause_menu_button;
	public static MenuButtonPosition menu_button_position;
	
	public SimpleConfigMod() {
		//noinspection CodeBlock2Expr
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			SimpleConfig.builder(MOD_ID, Type.CLIENT, ModConfig.class)
			  .n(DemoConfigCategory.getDemoCategory())
			  .setGUIDecorator((config, builder) -> builder.setDefaultBackgroundTexture(
			    new ResourceLocation("textures/block/bookshelf.png")))
			  .debugTranslations()
			  .buildAndRegister();
		});
		DemoServerConfig.registerServerConfig();
	}
	
	public static class ModConfig {
		@ConfigEntry public static boolean add_pause_menu_button = true;
		@ConfigEntry public static MenuButtonPosition menu_button_position = SPLIT_OPTIONS_BUTTON;
	}
	
	@SuppressWarnings("unused")
	public enum MenuButtonPosition {
		SPLIT_OPTIONS_BUTTON, LEFT_OF_OPTIONS_BUTTON
	}
	
	public static ResourceLocation prefix(String name) {
		return new ResourceLocation(MOD_ID, name);
	}
}
