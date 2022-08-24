package endorh.simpleconfig.core;

import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.yaml.SimpleConfigCommentedYamlFormat;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.config.ConfigFileTypeHandler;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
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
	private final SimpleConfigImpl config;
	private final SimpleConfigCommentedYamlFormat configFormat;
	
	public SimpleConfigModConfig(
	  SimpleConfigImpl config, ModContainer container
	) {
		super(config.getType().asConfigType(), config.spec, container, config.getFileName());
		this.config = config;
		this.configFormat = SimpleConfigCommentedYamlFormat.forConfig(config);
		// Prevent default handling of S2CConfigData packet, which assumes Toml format for files
		FILE_MAP.remove(getFileName());
	}
	
	public SimpleConfigImpl getSimpleConfig() {
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
	
	public static class LanguageReloadManager implements PreparableReloadListener {
		public static final LanguageReloadManager INSTANCE = new LanguageReloadManager();
		
		@Override public @NotNull CompletableFuture<Void> reload(
		  @NotNull PreparationBarrier stage, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller preparationsProfiler,
		  @NotNull ProfilerFiller reloadProfiler, @NotNull Executor backgroundExecutor, @NotNull Executor gameExecutor
		) {
			return stage.wait(Unit.INSTANCE).thenRunAsync(SimpleConfigImpl::updateAllFileTranslations);
		}
	}
	
	@EventBusSubscriber(value = Dist.DEDICATED_SERVER, modid = SimpleConfigMod.MOD_ID)
	public static class ServerLanguageReloadManager {
		/**
		 * FMLServerStartingEvent is posted after server translations have been loaded.
		 */
		@SubscribeEvent public static void onServerLanguageReloaded(ServerStartingEvent event) {
			SimpleConfigImpl.updateAllFileTranslations();
		}
	}
}
