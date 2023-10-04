package endorh.simpleconfig.core;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.loading.FMLConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Internal public class SimpleConfigPaths {
	public static final Path CLIENT_CONFIG_DIR = FMLPaths.CONFIGDIR.get();
	public static final Path LOCAL_PRESETS_DIR = CLIENT_CONFIG_DIR.resolve("presets");
	public static final Path DEFAULT_SERVER_CONFIG_DIR = FMLPaths.GAMEDIR.get().resolve(FMLConfig.defaultConfigPath());
	public static final Path LOCAL_HOTKEYS_DIR = CLIENT_CONFIG_DIR.resolve("saved-hotkeys");
	public static final Path CONFIG_HOTKEYS_FILE = CLIENT_CONFIG_DIR.resolve("simpleconfig-hotkeys.yaml");
	
	public static final LevelResource SERVERCONFIG = new LevelResource("serverconfig");
	
	public static Path getServerConfigPath() {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		Path serverConfig = server.getWorldPath(SERVERCONFIG);
		if (!Files.isDirectory(serverConfig)) try {
         Files.createDirectories(serverConfig);
      } catch (IOException e) {
         throw new RuntimeException("Problem creating directory", e);
      }
		return serverConfig;
	}
	
	public static Path getRemotePresetsDir() {
		return getServerConfigPath().resolve("presets");
	}
	
	public static Path getRemoteHotKeyGroupsDir() {
		return getServerConfigPath().resolve("saved-hotkeys");
	}
	
	public static Path relativize(Path path) {
		return FMLPaths.GAMEDIR.get().relativize(path);
	}
}
