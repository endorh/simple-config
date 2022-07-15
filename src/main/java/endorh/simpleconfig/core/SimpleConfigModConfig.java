package endorh.simpleconfig.core;

import endorh.simpleconfig.yaml.SimpleConfigCommentedYamlFormat;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IFutureReloadListener;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.Unit;
import net.minecraft.util.text.LanguageMap;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.config.ConfigFileTypeHandler;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.resource.SelectiveReloadStateHandler;
import net.minecraftforge.resource.VanillaResourceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class SimpleConfigModConfig extends ModConfig {
	private final ConfigFileTypeHandler handler = SimpleConfigFileTypeHandler.YAML;
	private final SimpleConfig config;
	private final SimpleConfigCommentedYamlFormat configFormat;
	
	public SimpleConfigModConfig(
	  SimpleConfig config, ModContainer container
	) {
		super(config.getType(), config.spec, container, config.getFileName());
		this.config = config;
		this.configFormat = SimpleConfigCommentedYamlFormat.forConfig(config);
	}
	
	public SimpleConfig getSimpleConfig() {
		return config;
	}
	
	@Override public ConfigFileTypeHandler getHandler() {
		return handler;
	}
	
	public SimpleConfigCommentedYamlFormat getConfigFormat() {
		return configFormat;
	}
	
	public static class LanguageReloadManager implements IFutureReloadListener {
		public static final LanguageReloadManager INSTANCE = new LanguageReloadManager();
		private static final Logger LOGGER = LogManager.getLogger();
		
		@Override public @NotNull CompletableFuture<Void> reload(
		  @NotNull IStage stage, @NotNull IResourceManager resourceManager, @NotNull IProfiler preparationsProfiler,
		  @NotNull IProfiler reloadProfiler, @NotNull Executor backgroundExecutor, @NotNull Executor gameExecutor
		) {
			if (SelectiveReloadStateHandler.INSTANCE.get().test(VanillaResourceType.LANGUAGES)) {
				LOGGER.warn("Detected I18n reload: " + LanguageMap.getInstance().getLanguageData().size());
				return stage.markCompleteAwaitingOthers(Unit.INSTANCE).thenRunAsync(() -> {
					LOGGER.warn("I18n should be reloaded now: " + LanguageMap.getInstance().getLanguageData().size());
					LOGGER.info("Reloading config file comments with loaded translations");
					SimpleConfig.updateAllFileTranslations();
				});
			}
			return stage.markCompleteAwaitingOthers(Unit.INSTANCE).thenRunAsync(() -> {});
		}
	}
}
