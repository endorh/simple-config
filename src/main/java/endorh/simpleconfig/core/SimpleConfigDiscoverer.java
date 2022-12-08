package endorh.simpleconfig.core;

import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.api.ConfigBuilderFactoryProxy;
import endorh.simpleconfig.api.SimpleConfig;
import endorh.simpleconfig.api.annotation.ConfigClass;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.ReportedException;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.loading.moddiscovery.ModAnnotation.EnumHolder;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.language.ModFileScanData;
import net.minecraftforge.forgespi.language.ModFileScanData.AnnotationData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static endorh.simpleconfig.core.SimpleConfigImpl.getModNameOrId;

@EventBusSubscriber(bus = Bus.MOD, modid = SimpleConfigMod.MOD_ID)
public class SimpleConfigDiscoverer {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Type CONFIG_CLASS_TYPE = Type.getType(ConfigClass.class);
	
	@Internal public static void discoverConfigs() {
		try {
			for (ModFileScanData scanData: ModList.get().getAllScanData()) {
				String inferredModID = null;
				List<IModFileInfo> modFiles = scanData.getIModInfoData();
				if (modFiles.size() == 1) {
					List<IModInfo> mods = modFiles.get(0).getMods();
					if (mods.size() == 1) inferredModID = mods.get(0).getModId();
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
						ConfigBuilderFactoryProxy.config(modID, type, cls).buildAndRegister();
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
