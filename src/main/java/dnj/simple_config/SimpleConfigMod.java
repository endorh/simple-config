package dnj.simple_config;

import dnj.simple_config.core.SimpleConfig;
import dnj.simple_config.core.annotation.Entry;
import dnj.simple_config.core.annotation.Group;
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
	// Storing the config instance is optional
	public static SimpleConfig CLIENT_CONFIG;
	
	public SimpleConfigMod() {
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			// Create and register the client config for the mod
			CLIENT_CONFIG = SimpleConfig.builder(MOD_ID, Type.CLIENT, Config.class)
			  // Hook here the demo category
			  .n(DemoConfigCategory.getDemoCategory())
			  // Change the background texture
			  .setBackground("textures/block/bookshelf.png")
			  .buildAndRegister();
		});
		// Register the demo server config as well
		DemoServerConfig.registerServerConfig();
	}
	
	// Simple configs may be defined directly in the backing class,
	//   if you actually like this bothersome annotations
	// It is however encouraged to define the configs entirely
	//   in the builder, which provides more options
	// Configs defined in the builder may have backing fields as well
	//   They just need to be found in the backing class
	/**
	 * Config backing class
	 */
	public static class Config {
		@Entry public static boolean add_pause_menu_button = true;
		@Entry public static MenuButtonPosition menu_button_position = SPLIT_OPTIONS_BUTTON;
		@Group public static class advanced {
			@Entry.NonPersistent public static boolean translation_debug_mode = false;
		}
	}
	
	@SuppressWarnings("unused")
	public enum MenuButtonPosition {
		SPLIT_OPTIONS_BUTTON, LEFT_OF_OPTIONS_BUTTON
	}
	
	public static ResourceLocation prefix(String name) {
		return new ResourceLocation(MOD_ID, name);
	}
}
