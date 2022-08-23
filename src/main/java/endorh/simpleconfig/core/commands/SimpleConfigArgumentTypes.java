package endorh.simpleconfig.core.commands;

import com.mojang.brigadier.arguments.ArgumentType;
import endorh.simpleconfig.SimpleConfigMod;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@EventBusSubscriber(modid = SimpleConfigMod.MOD_ID, bus = Bus.MOD)
public class SimpleConfigArgumentTypes {
	
	@SubscribeEvent public static void onInit(FMLCommonSetupEvent event) {
		registerArgumentTypes();
	}
	
	public static void registerArgumentTypes() {
		register("mod-id", SimpleConfigModIdArgumentType.class, new SimpleConfigModIdArgumentType.Serializer());
		register("type", SimpleConfigTypeArgumentType.class, new SimpleConfigTypeArgumentType.Serializer());
		register("key", SimpleConfigKeyArgumentType.class, new SimpleConfigKeyArgumentType.Serializer());
		register("value", SimpleConfigValueArgumentType.class, new SimpleConfigValueArgumentType.Serializer());
	}
	
	private static <T extends ArgumentType<?>> void register(
	  String name, Class<T> clazz, ArgumentSerializer<T> serializer
	) {
		ArgumentTypes.register(SimpleConfigMod.MOD_ID + ":" + name, clazz, serializer);
	}
}
