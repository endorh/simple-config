package endorh.simpleconfig.core.commands;

import com.mojang.brigadier.arguments.ArgumentType;
import endorh.simpleconfig.SimpleConfigMod;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class SimpleConfigArgumentTypes {
	private static final DeferredRegister<ArgumentTypeInfo<?, ?>>
	  TYPE_INFOS = DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, SimpleConfigMod.MOD_ID);
	
	public static void registerArgumentTypes() {
		reg("mod-id", SimpleConfigModIdArgumentType.class, SimpleConfigModIdArgumentType.Info::new);
		reg("type", SimpleConfigTypeArgumentType.class, SimpleConfigTypeArgumentType.Info::new);
		reg("key", SimpleConfigKeyArgumentType.class, SimpleConfigKeyArgumentType.Info::new);
		reg("value", SimpleConfigValueArgumentType.class, SimpleConfigValueArgumentType.Info::new);
		TYPE_INFOS.register(FMLJavaModLoadingContext.get().getModEventBus());
	}
	
	private static <T extends ArgumentType<?>, TT extends ArgumentTypeInfo.Template<T>> void reg(
	  String name, Class<T> clazz, Supplier<ArgumentTypeInfo<T, TT>> info
	) {
		ArgumentTypeInfo<T, TT> i = info.get();
		ArgumentTypeInfos.registerByClass(clazz, i);
		TYPE_INFOS.register(name, () -> i);
	}
}
