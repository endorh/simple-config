package endorh.simpleconfig.core;

import com.electronwill.nightconfig.core.ConfigFormat;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileWatcher;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.ReportedException;
import net.minecraftforge.fml.config.ConfigFileTypeHandler;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import static endorh.simpleconfig.core.SimpleConfigPaths.DEFAULT_SERVER_CONFIG_DIR;

public class SimpleConfigFileTypeHandler extends ConfigFileTypeHandler {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Marker CONFIG = MarkerManager.getMarker("CONFIG");
	public static SimpleConfigFileTypeHandler YAML = new SimpleConfigFileTypeHandler();
	
	@Override public Function<ModConfig, CommentedFileConfig> reader(Path configBasePath) {
		return (config) -> {
			if (!(config instanceof SimpleConfigModConfig)) throw new IllegalArgumentException(
			  "SimpleConfigFileTypeHandler can only handle SimpleConfigModConfig");
			SimpleConfigModConfig c = (SimpleConfigModConfig) config;
			final Path configPath = configBasePath.resolve(c.getFileName());
			final CommentedFileConfig configData = CommentedFileConfig.builder(
			  configPath, c.getConfigFormat()
			  ).sync()
			  .preserveInsertionOrder()
			  .autosave()
			  .onFileNotFound((newfile, configFormat) -> setupConfigFile(config, newfile, configFormat))
			  .writingMode(WritingMode.REPLACE)
			  .build();
			LOGGER.debug(CONFIG, "Built YAML config for {}", configPath.toString());
			try {
				configData.load();
			} catch (ParsingException ex) {
				throw new ReportedException(CrashReport.forThrowable(
				  ex, "Error reading config file " + c.getFileName()));
			}
			LOGGER.debug(CONFIG, "Loaded YAML config file {}", configPath.toString());
			try {
				FileWatcher.defaultInstance().addWatch(configPath, new SimpleConfigWatcher(
				  config, configData, Thread.currentThread().getContextClassLoader()));
				LOGGER.debug(CONFIG, "Watching YAML config file {} for changes", configPath.toString());
			} catch (IOException e) {
				throw new RuntimeException("Couldn't watch config file", e);
			}
			return configData;
		};
	}
	
	@Override public void unload(Path configBasePath, ModConfig config) {
		Path configPath = configBasePath.resolve(config.getFileName());
		try {
			FileWatcher.defaultInstance().removeWatch(configBasePath.resolve(config.getFileName()));
		} catch (RuntimeException e) {
			LOGGER.error("Failed to remove config {} from tracker!", configPath.toString(), e);
		}
	}
	
	private boolean setupConfigFile(
	  final ModConfig modConfig, final Path file, final ConfigFormat<?> conf
	) throws IOException {
		Path p = DEFAULT_SERVER_CONFIG_DIR.resolve(modConfig.getFileName());
		if (Files.exists(p)) {
			LOGGER.info(CONFIG, "Loading default config file from path {}", p);
			Files.copy(p, file);
		} else {
			Files.createFile(file);
			conf.initEmptyFile(file);
		}
		return true;
	}
	
	@Internal public static class SimpleConfigWatcher implements Runnable {
		private final ModConfig modConfig;
		private final CommentedFileConfig commentedFileConfig;
		private final ClassLoader realClassLoader;
		
		SimpleConfigWatcher(
		  final ModConfig modConfig, final CommentedFileConfig commentedFileConfig,
		  final ClassLoader classLoader
		) {
			this.modConfig = modConfig;
			this.commentedFileConfig = commentedFileConfig;
			realClassLoader = classLoader;
		}
		
		@Override
		public void run() {
			// Force the regular classloader onto the special thread
			Thread.currentThread().setContextClassLoader(realClassLoader);
			if (!modConfig.getSpec().isCorrecting()) {
				try {
					commentedFileConfig.load();
					if (!modConfig.getSpec().isCorrect(commentedFileConfig)) {
						LOGGER.warn(
						  CONFIG, "Configuration file {} is not correct. Correcting",
						  commentedFileConfig.getFile().getAbsolutePath());
						ConfigFileTypeHandler.backUpConfig(commentedFileConfig);
						modConfig.getSpec().correct(commentedFileConfig);
						commentedFileConfig.save();
					}
				} catch (ParsingException ex) {
					throw new ConfigLoadingException(modConfig, ex);
				}
				LOGGER.debug(
				  CONFIG, "Config file {} changed, sending notifies", modConfig.getFileName());
				modConfig.getSpec().afterReload();
				try {
					SimpleConfigNetworkHandler.tryFireEvent(
					  modConfig, SimpleConfigNetworkHandler.newReloading(modConfig));
				} catch (RuntimeException e) {
					LOGGER.error("Error updating config from file {}", modConfig.getFileName(), e);
				}
			}
		}
	}
	
	private static class ConfigLoadingException extends RuntimeException {
		public ConfigLoadingException(ModConfig config, Exception cause) {
			super(
			  "Failed loading config file " + config.getFileName() + " of type " + config.getType() +
			  " for modid " + config.getModId(), cause);
		}
	}
}
