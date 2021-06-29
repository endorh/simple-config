package dnj.simple_config.demo;

import dnj.simple_config.SimpleConfigMod;
import dnj.simple_config.core.SimpleConfigBuilder;
import net.minecraft.item.Items;
import net.minecraftforge.common.Tags;
import net.minecraftforge.fml.config.ModConfig.Type;

import static dnj.simple_config.core.Entry.Builders.*;
import static dnj.simple_config.core.SimpleConfig.category;

public abstract class DemoServerConfig {
	public static void registerServerConfig() {
		new SimpleConfigBuilder(SimpleConfigMod.MOD_ID, Type.SERVER, DemoServerConfig.class)
		  .n(category("demo")
		       .text("greeting")
		       .add("string", string("string"))
		       .add("magic_gem", item(Items.EMERALD).from(Tags.Items.GEMS)))
		  .build();
	}
}
