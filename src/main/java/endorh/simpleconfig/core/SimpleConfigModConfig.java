package endorh.simpleconfig.core;

import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.yaml.SimpleConfigCommentedYamlFormat;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IFutureReloadListener;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.Unit;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.config.ConfigFileTypeHandler;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.resource.SelectiveReloadStateHandler;
import net.minecraftforge.resource.VanillaResourceType;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class SimpleConfigModConfig extends ModConfig {
	private static final ConcurrentHashMap<String, ModConfig> FILE_MAP =
	  Objects.requireNonNull(ObfuscationReflectionHelper.getPrivateValue(
		 ConfigTracker.class, ConfigTracker.INSTANCE, "fileMap"));
	private final ConfigFileTypeHandler handler = SimpleConfigFileTypeHandler.YAML;
	private final SimpleConfig config;
	private final SimpleConfigCommentedYamlFormat configFormat;
	
	public SimpleConfigModConfig(
	  SimpleConfig config, ModContainer container
	) {
		super(config.getType(), config.spec, container, config.getFileName());
		this.config = config;
		this.configFormat = SimpleConfigCommentedYamlFormat.forConfig(config);
		// Prevent default handling of S2CConfigData packet, which assumes Toml format for files
		FILE_MAP.remove(getFileName());
	}
	
	public SimpleConfig getSimpleConfig() {
		return config;
	}
	
	@Override public ConfigFileTypeHandler getHandler() {
		return handler;
	}
	
	@Override public Path getFullPath() {
		return super.getFullPath();
	}
	
	public SimpleConfigCommentedYamlFormat getConfigFormat() {
		return configFormat;
	}
	
	public static class LanguageReloadManager implements IFutureReloadListener {
		public static final LanguageReloadManager INSTANCE = new LanguageReloadManager();
		
		@Override public @NotNull CompletableFuture<Void> reload(
		  @NotNull IStage stage, @NotNull IResourceManager resourceManager, @NotNull IProfiler preparationsProfiler,
		  @NotNull IProfiler reloadProfiler, @NotNull Executor backgroundExecutor, @NotNull Executor gameExecutor
		) {
			return stage.markCompleteAwaitingOthers(Unit.INSTANCE)
			  .thenRunAsync(
				 SelectiveReloadStateHandler.INSTANCE.get().test(VanillaResourceType.LANGUAGES)
				 ? SimpleConfig::updateAllFileTranslations
				 : () -> {}
			  );
		}
	}
	
	@EventBusSubscriber(value = Dist.DEDICATED_SERVER, modid = SimpleConfigMod.MOD_ID)
	public static class ServerLanguageReloadManager {
		/**
		 * FMLServerStartingEvent is posted after server translations have been loaded.
		 */
		@SubscribeEvent public static void onServerLanguageReloaded(FMLServerStartingEvent event) {
			SimpleConfig.updateAllFileTranslations();
		}
	}
}
