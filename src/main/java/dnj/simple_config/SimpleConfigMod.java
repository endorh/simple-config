package dnj.simple_config;

import dnj.simple_config.core.SimpleConfig;
import dnj.simple_config.core.annotation.Entry;
import dnj.simple_config.demo.DemoConfigCategory;
import dnj.simple_config.demo.DemoServerConfig;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;

import static dnj.simple_config.SimpleConfigMod.MenuButtonPosition.SPLIT_OPTIONS_BUTTON;

@Mod(SimpleConfigMod.MOD_ID)
public class SimpleConfigMod {
	public static final String MOD_ID = "simple-config";
	
	public SimpleConfigMod() {
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			// Create and register the client config for the mod
			SimpleConfig.builder(MOD_ID, Type.CLIENT, ModConfig.class)
			  // Hook here the demo category
			  .n(DemoConfigCategory.getDemoCategory())
			  // Change the background texture
			  .setGUIDecorator((config, builder) -> builder.setDefaultBackgroundTexture(
			    new ResourceLocation("textures/block/bookshelf.png")))
			  .debugTranslations()
			  .buildAndRegister();
		});
		// Register the demo server config as well
		DemoServerConfig.registerServerConfig();
	}
	
	/**
	 * Config backing class
	 */
	public static class ModConfig {
		@Entry public static boolean add_pause_menu_button = true;
		@Entry public static MenuButtonPosition menu_button_position = SPLIT_OPTIONS_BUTTON;
	}
	
	@SuppressWarnings("unused")
	public enum MenuButtonPosition {
		SPLIT_OPTIONS_BUTTON, LEFT_OF_OPTIONS_BUTTON
	}
	
	public static ResourceLocation prefix(String name) {
		return new ResourceLocation(MOD_ID, name);
	}
}
