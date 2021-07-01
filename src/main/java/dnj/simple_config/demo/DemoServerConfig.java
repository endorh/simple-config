package dnj.simple_config.demo;

import dnj.simple_config.SimpleConfigMod;
import dnj.simple_config.core.SimpleConfig;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraftforge.common.Tags;
import net.minecraftforge.fml.config.ModConfig.Type;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static dnj.simple_config.core.AbstractConfigEntry.Builders.*;
import static dnj.simple_config.core.SimpleConfig.category;

public abstract class DemoServerConfig {
	private static final Logger LOGGER = LogManager.getLogger();
	
	public static void registerServerConfig() {
		SimpleConfig.builder(SimpleConfigMod.MOD_ID, Type.SERVER, DemoServerConfig.class)
		  .n(category("demo")
		       .text("greeting")
		       .add("bool", bool(true).restart())
		       .add("string", string("string"))
		       .add("magic_gem", item(Items.EMERALD).from(Tags.Items.GEMS)))
		  .setBaker(DemoServerConfig::bakeServerConfig)
		  .buildAndRegister();
	}
	
	protected static void bakeServerConfig(SimpleConfig config) {
		LOGGER.debug("Baking server config");
	}
	
	public static boolean bool;
	public static String string;
	public static Item magic_gem;
}
