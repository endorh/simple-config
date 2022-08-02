package endorh.simpleconfig.core;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.fml.loading.FMLConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.FileUtils;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.nio.file.Path;

@Internal public class SimpleConfigPaths {
	public static final Path CLIENT_CONFIG_DIR = FMLPaths.CONFIGDIR.get();
	public static final Path LOCAL_PRESETS_DIR = CLIENT_CONFIG_DIR.resolve("presets");
	public static final Path DEFAULT_SERVER_CONFIG_DIR = FMLPaths.GAMEDIR.get()
	  .resolve(FMLConfig.defaultConfigPath());
	public static final Path LOCAL_HOTKEYS_DIR = CLIENT_CONFIG_DIR.resolve("saved-hotkeys");
	
	public static final FolderName SERVERCONFIG = new FolderName("serverconfig");
	
	public static Path getServerConfigPath() {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		Path serverConfig = server.func_240776_a_(SERVERCONFIG);
		return FileUtils.getOrCreateDirectory(serverConfig, SERVERCONFIG.getFileName());
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
