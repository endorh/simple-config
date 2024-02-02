package endorh.simpleconfig.core;

import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.api.ConfigBuilderFactoryProxy;
import endorh.simpleconfig.api.SimpleConfig;
import endorh.simpleconfig.api.SimpleConfigBuilder;
import endorh.simpleconfig.api.annotation.ConfigClass;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.ReportedException;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLModContainer;
import net.minecraftforge.fml.loading.moddiscovery.ModAnnotation.EnumHolder;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.language.ModFileScanData;
import net.minecraftforge.forgespi.language.ModFileScanData.AnnotationData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static endorh.simpleconfig.core.SimpleConfigImpl.getModNameOrId;

@EventBusSubscriber(bus = Bus.MOD, modid = SimpleConfigMod.MOD_ID)
public class SimpleConfigDiscoverer {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Type CONFIG_CLASS_TYPE = Type.getType(ConfigClass.class);

	private static @Nullable IEventBus tryGetEventBus(@Nullable ModContainer container) {
		if (container == null) return null;
		if (container instanceof FMLModContainer)
			return ((FMLModContainer) container).getEventBus();
		// Duck typing attempt
		Class<? extends ModContainer> cls = container.getClass();
		try {
			Method method = cls.getMethod("getEventBus");
			method.setAccessible(true);
			if (IEventBus.class.isAssignableFrom(method.getReturnType())) {
				Object eventBus = method.invoke(container);
				if (eventBus instanceof IEventBus) return (IEventBus) eventBus;
			}
			LOGGER.warn("Unexpected type returned by ModContainer#getEventBus(): " + method.getReturnType().getCanonicalName());
		} catch (NoSuchMethodException ignored) {
		} catch (IllegalAccessException | InvocationTargetException ignored) {
			LOGGER.warn("Unexpected reflection error while trying to invoke ModContainer#getEventBus() for mod: " + container.getModId());
		}
		try {
			Field field = cls.getField("eventBus");
			field.setAccessible(true);
			if (IEventBus.class.isAssignableFrom(field.getType())) {
				Object eventBus = field.get(container);
				if (eventBus instanceof IEventBus) return (IEventBus) eventBus;
			}
			LOGGER.warn("Unexpected type found in ModContainer#eventBus: " + field.getType().getCanonicalName());
		} catch (NoSuchFieldException ignored) {
		} catch (IllegalAccessException e) {
			LOGGER.warn("Unexpected reflection error while trying to read ModContainer#eventBus for mod: " + container.getModId());
		}
		LOGGER.debug("Couldn't get mod event bus for mod: " + container.getModId() + " (mod container type: " + container.getClass().getCanonicalName() + "), Simple Config config class discovery may fail for this mod.");
		return null;
	}

	@Internal public static void discoverConfigs() {
		try {
			ModList modList = ModList.get();
			for (ModFileScanData scanData: modList.getAllScanData()) {
				String inferredModID = null;
				IEventBus eventBus = null;
				List<IModFileInfo> modFiles = scanData.getIModInfoData();
				if (!modFiles.isEmpty()) {
					List<IModInfo> mods = modFiles.get(0).getMods();
					if (!mods.isEmpty()) {
						IModInfo modInfo = mods.get(0);
						String modId = modInfo.getModId();
						ModContainer container = modList.getModContainerById(modId).orElseThrow(() -> new IllegalStateException(
							"Mod container for mod ID " + modId + " not found"));
						eventBus = tryGetEventBus(container);
						inferredModID = modId;
					}
				}
				LOGGER.debug("Discovering mod file for config annotations (inferred mod ID: " + inferredModID + ")");
				List<AnnotationData> ccTargets = scanData.getAnnotations().stream()
				  .filter(d -> CONFIG_CLASS_TYPE.equals(d.getAnnotationType()))
				  .collect(Collectors.toList());
				for (AnnotationData d: ccTargets) {
					Map<String, Object> args = d.getAnnotationData();
					EnumHolder typeHolder =
					  (EnumHolder) args.getOrDefault("type", new EnumHolder(null, "CLIENT"));
					SimpleConfig.Type type = SimpleConfig.Type.valueOf(typeHolder.getValue());
					String modID = (String) args.getOrDefault("modID", inferredModID);
					String className = d.getClassType().getClassName();
					if (modID == null) throw new SimpleConfigDiscoveryException(
					  "Missing mod ID in class annotated as @ConfigClass: " + className);
					try {
						Class<?> cls = Class.forName(className);
						LOGGER.debug("Discovered " + " mod config for mod: " + getModNameOrId(modID) + "(" + modID + ")");
						SimpleConfigBuilder builder = ConfigBuilderFactoryProxy.config(modID, type, cls);
						builder.buildAndRegister(eventBus != null? eventBus : FMLJavaModLoadingContext.get().getModEventBus());
					} catch (ClassNotFoundException e) {
						throw new SimpleConfigDiscoveryException(
						  "Error accessing class annotated as @ConfigClass: " + className, e);
					}
				}
			}
			LOGGER.debug("Finished discovering config classes");
		} catch (RuntimeException e) {
			throw new ReportedException(CrashReport.makeCrashReport(e, "Error discovering config classes"));
		}
	}
	
	public static class SimpleConfigDiscoveryException extends RuntimeException {
		public SimpleConfigDiscoveryException(String message) {
			super(message);
		}
		public SimpleConfigDiscoveryException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
