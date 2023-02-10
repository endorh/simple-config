package endorh.simpleconfig.api;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;

import static endorh.simpleconfig.api.SimpleConfigGUIManagerProxy.getSimpleConfigGUIManager;
import static endorh.simpleconfig.api.SimpleConfigProxy.getModNameOrId;

public interface SimpleConfig extends ConfigEntryHolder {
	@Internal String MOD_ID = "simpleconfig";
	
	/**
    * Builds and registers a config file declared using the
    * declarative Java API<br><br>
    *
	 * The mod ID is inferred from the mod thread.<br><br>
	 *
    * If the Simple Config mod is not present, this method will log
    * a warning stating that Simple Config needs to be installed to
    * edit the configuration for your mod.
	 *
	 * @param type The {@link Type} of the config file, usually either {@code CLIENT} or {@code SERVER}
	 * @param configClass The config class that defines config entries for your file using the
	 *                    declarative Java API
    */
	static boolean registerConfig(Type type, @NotNull Class<?> configClass) {
		return registerConfig(null, type, configClass);
	}
	
	/**
	 * Builds and registers a config file declared using the
	 * declarative Java API<br><br>
	 *
	 * If the Simple Config mod is not present, this method will log
	 * a warning stating that Simple Config needs to be installed to
	 * edit the configuration for your mod.
	 *
	 * @param modId Your mod ID, it will be inferred from your mod thread if omitted
	 * @param type The {@link Type} of the config file, usually either {@code CLIENT} or {@code SERVER}
	 * @param configClass The config class that defines config entries for your file using the
	 *                    declarative Java API
	 */
	static boolean registerConfig(@Nullable String modId, Type type, @NotNull Class<?> configClass) {
		if (!SimpleConfigProxy.isRuntimePresent()) {
			LogManager.getLogger().warn(
			  "Config file for mod " + getModNameOrId(modId) + "(" + modId + ")" +
			  " cannot be created because the Simple Config mod is not present." +
			  "\n  Install Simple Config to modify the configuration of this mod:" +
			  "\n    https://www.curseforge.com/minecraft/mc-mods/simple-config");
			return false;
		}
		ConfigBuilderFactoryProxy.config(modId, type, configClass).buildAndRegister();
		return true;
	}
	
	/**
	 * Whether this config is a wrapper for a mod that
	 * doesn't use the Simple Config API.
	 */
	boolean isWrapper();
	
	/**
	 * Retrieve the actual path of this file, if found
	 */
	Optional<Path> getFilePath();
	
	/**
	 * Return the actual path of the file for this category or the whole config, if found
	 */
	default Optional<Path> getFilePath(String category) {
		return getFilePath();
	}
	
	@Internal String getFileName();
	
	/**
	 * Get a config category
	 *
	 * @param name Name of the category
	 * @throws NoSuchConfigCategoryError if the category is not found
	 */
	@NotNull SimpleConfigCategory getCategory(String name);
	
	/**
	 * Get a config group
	 *
	 * @param path Name or dot-separated path to the group
	 * @throws NoSuchConfigGroupError if the group is not found
	 */
	@NotNull SimpleConfigGroup getGroup(String path);
	
	/**
	 * Whether this config can be edited by the player.<br>
	 * Will be false for remote configs for which the player doesn't
	 * have edit permission.
	 */
	boolean canEdit();
	
	/**
	 * Type of this config.
	 */
	Type getType();
	
	/**
	 * Mod ID of this config.
	 */
	String getModId();
	
	/**
	 * Mod name of this config.
	 */
	String getModName();
	
	/**
	 * Type of config files:
	 * <ul>
	 *    <li>{@link #CLIENT}</li>
	 *    <li>{@link #COMMON}</li>
	 *    <li>{@link #SERVER}</li>
	 * </ul>
	 */
	enum Type {
		/**
		 * <b>Client config</b>, exclusive for the client side.<br>
		 * Stored locally in the client's {@code config} folder.<br>
		 * Different for each player, missing in the server, shared
		 * for all worlds a player plays in.<br>
		 */
		CLIENT(ModConfig.Type.CLIENT, true, false),
		/**
		 * <b>Common config</b><br>
		 * Stored locally in the client's {@code config} folder, and
		 * in the server's {@code serverconfig} folder.<br>
		 * Different for each player and the server (each has their
		 * own version).<br>
		 * Shared for all worlds a player plays in, but each world has its
		 * own version as well.<br>
		 * All players can edit their own version, and players with permissions
		 * on a server can see/edit the server's version remotely.<br>
		 * <br>
		 * <b>Strongly discouraged</b>. The only encouraged use case is for server
		 * configuration that is needed before the world loads, as
		 * {@link #SERVER} configs are not loaded until the world is being
		 * loaded, which is sometimes too late.<br>
		 * Most mods only ever need {@link #CLIENT} and {@link #SERVER} configs.
		 */
		COMMON(ModConfig.Type.COMMON, true, true),
		/**
		 * <b>Server config</b>, exclusive for the server side, but known by clients.<br>
		 * Stored remotely in the server's {@code serverconfig} folder only.<br>
		 * A copy is sent to clients when they join the server, or when the config
		 * changes.<br>
		 * Same for all players in the server.<br>
		 * Only players with permission on the server can see/edit the config
		 * remotely.<br>
		 */
		SERVER(ModConfig.Type.SERVER, false, true);
		
		private static final Map<String, Type> BY_ALIAS = Util.make(
		  new HashMap<>(values().length), m -> {
			  for (Type v: values()) m.put(v.getAlias(), v);
		  });
		private static final Map<ModConfig.Type, Type> BY_CONFIG_TYPE = Util.make(
		  new HashMap<>(values().length), m -> {
			  for (Type v: values()) m.put(v.asConfigType(), v);
		  });
		private static final Set<Type> LOCAL_TYPES = Util.make(
		  Collections.newSetFromMap(new EnumMap<>(Type.class)),
		  s -> Arrays.stream(values()).filter(Type::isLocal).forEach(s::add));
		private static final Set<Type> REMOTE_TYPES = Util.make(
		  Collections.newSetFromMap(new EnumMap<>(Type.class)),
		  s -> Arrays.stream(values()).filter(Type::isRemote).forEach(s::add));
		
		/**
		 * {@link #CLIENT} and {@link #COMMON}
		 */
		public static Set<Type> localTypes() {
			return LOCAL_TYPES;
		}
		/**
		 * {@link #COMMON} and {@link #SERVER}
		 */
		public static Set<Type> remoteTypes() {
			return REMOTE_TYPES;
		}
		
		public static Type fromAlias(String alias) {
			return BY_ALIAS.get(alias);
		}
		
		/**
		 * From equivalent Forge config type.
		 */
		public static Type fromConfigType(ModConfig.Type type) {
			return BY_CONFIG_TYPE.get(type);
		}
		
		private final @NotNull ModConfig.Type type;
		private final boolean isLocal;
		private final boolean isRemote;
		private final @NotNull String alias;
		
		Type(@NotNull ModConfig.Type type, boolean isLocal, boolean isRemote) {
			this.type = type;
			this.isLocal = isLocal;
			this.isRemote = isRemote;
			alias = type.extension();
		}
		
		/**
		 * Whether this type has a version stored locally.
		 */
		public boolean isLocal() {
			return isLocal;
		}
		
		/**
		 * Whether this type has a version stored remotely.
		 */
		public boolean isRemote() {
			return isRemote;
		}
		
		/**
		 * Equivalent Forge config type.
		 */
		public @NotNull ModConfig.Type asConfigType() {
			return type;
		}
		
		/**
		 * {@link EditType} for remote/local origin as specified, or null
		 * if this type doesn't have a version for that origin.
		 */
		public EditType asEditType(boolean remote) {
			return Arrays.stream(EditType.values()).filter(
			  t -> t.getType() == this && t.isOnlyRemote() == remote
			).findFirst().orElseGet(() -> Arrays.stream(EditType.values()).filter(
			  t -> t.getType() == this
			).findFirst().orElse(null));
		}
		
		/**
		 * Alias used for file names (lowercase name).
		 */
		public @NotNull String getAlias() {
			return alias;
		}
	}
	
	/**
	 * Type of edited config from the player perspective:<br>
	 * <ul>
	 *    <li>{@link #CLIENT} - local client config</li>
	 *    <li>{@link #COMMON} - local common config</li>
	 *    <li>{@link #SERVER_COMMON} - remote common config</li>
	 *    <li>{@link #SERVER} - remote server config</li>
	 * </ul>
	 */
	enum EditType {
		/**
		 * Local client config
		 */
		CLIENT(Type.CLIENT, false, false),
		/**
		 * Local common config
		 */
		COMMON(Type.COMMON, false, false),
		/**
		 * Remote common config
		 */
		SERVER_COMMON(Type.COMMON, true, true),
		/**
		 * Remote server config
		 */
		SERVER(Type.SERVER, true, false);
		
		private final static Map<String, EditType> BY_ALIAS = Util.make(
		  new HashMap<>(values().length), m -> {
			  for (EditType v: values()) m.put(v.getAlias(), v);
		  });
		
		private static final EditType[] LOCAL_TYPES = Arrays.stream(values())
		  .filter(editType -> !editType.isRemote()).toArray(EditType[]::new);
		private static final EditType[] REMOTE_TYPES = Arrays.stream(values())
		  .filter(EditType::isRemote).toArray(EditType[]::new);
		
		/**
		 * {@link #CLIENT} and {@link #COMMON}
		 */
		public static EditType[] localTypes() {
			return LOCAL_TYPES;
		}
		
		/**
		 * {@link #SERVER_COMMON} and {@link #SERVER}
		 */
		public static EditType[] remoteTypes() {
			return REMOTE_TYPES;
		}
		
		public static EditType fromAlias(String extension) {
			return BY_ALIAS.get(extension);
		}
		
		private final @NotNull Type type;
		private final boolean isRemote;
		private final boolean onlyRemote;
		private final @NotNull String alias;
		EditType(@NotNull Type type, boolean isRemote, boolean onlyRemote) {
			this.type = type;
			this.isRemote = isRemote;
			this.onlyRemote = onlyRemote;
			alias = name().toLowerCase().replace('_', '-');
		}
		
		/**
		 * Associated {@link Type}.
		 */
		public @NotNull Type getType() {
			return type;
		}
		
		/**
		 * Whether this type is stored remotely.
		 */
		public boolean isRemote() {
			return isRemote;
		}
		
		/**
		 * Whether this type is only stored remotely.
		 */
		public boolean isOnlyRemote() {
			return onlyRemote;
		}
		
		/**
		 * Alias used for serialization (hyphenated lowercase name).
		 */
		public @NotNull String getAlias() {
			return alias;
		}
	}
	
	// SimpleConfigGUIManager methods
	
	/**
	 * Check if a config GUI exists for a mod
	 */
	@OnlyIn(Dist.CLIENT)
	static boolean hasConfigGUI(String modId) {
		return getSimpleConfigGUIManager().hasConfigGUI(modId);
	}
	
	/**
	 * Build a config GUI for the specified mod id
	 * @param parent Parent screen to return to
	 */
	@OnlyIn(Dist.CLIENT)
	static @Nullable Screen getConfigGUI(String modId, Screen parent) {
		return getSimpleConfigGUIManager().getConfigGUI(modId, parent);
	}
	
	/**
	 * Build a config GUI for the specified mod id, using the current screen as parent
	 */
	@OnlyIn(Dist.CLIENT)
	static @Nullable Screen getConfigGUI(String modId) {
		return getSimpleConfigGUIManager().getConfigGUI(modId);
	}
	
	/**
	 * Show the config GUI for the specified mod id, using the current screen as parent
	 */
	@OnlyIn(Dist.CLIENT)
	static void showConfigGUI(String modId) {
		getSimpleConfigGUIManager().showConfigGUI(modId);
	}
	
	/**
	 * Show the Forge mod list GUI
	 */
	@OnlyIn(Dist.CLIENT)
	static void showModListGUI() {
		getSimpleConfigGUIManager().showModListGUI();
	}
	
	/**
	 * Show the Config Hotkey GUI
	 */
	@OnlyIn(Dist.CLIENT)
	static void showConfigHotkeysGUI() {
		getSimpleConfigGUIManager().showConfigHotkeysGUI();
	}
	
	class NoSuchConfigEntryError extends RuntimeException {
		public NoSuchConfigEntryError(String path) {
			super("Cannot find config entry \"" + path + "\"");
		}
	}
	
	class NoSuchConfigCategoryError extends RuntimeException {
		public NoSuchConfigCategoryError(String path) {
			super("Cannot find config category \"" + path + "\"");
		}
	}
	
	class NoSuchConfigGroupError extends RuntimeException {
		public NoSuchConfigGroupError(String path) {
			super("Cannot find config group \"" + path + "\"");
		}
	}
	
	class InvalidConfigValueException extends RuntimeException {
		public InvalidConfigValueException(String path, Object value) {
			super("Invalid config value set for config entry \"" + path + "\": " + value);
		}
	}
	
	class InvalidDefaultConfigValueException extends RuntimeException {
		public InvalidDefaultConfigValueException(String path, Object value) {
			super("Invalid default config value set for config entry \"" + path + "\": " + value);
		}
	}
	
	class UnInvertibleBakingTransformationException extends RuntimeException {
		public UnInvertibleBakingTransformationException(String path) {
			super("Baking transformation of config entry \"" + path + "\" does not support write access");
		}
	}
	
	class InvalidConfigValueTypeException extends RuntimeException {
		public InvalidConfigValueTypeException(String path) {
			super("Invalid type requested for config value \"" + path + "\"");
		}
		
		public InvalidConfigValueTypeException(String path, Throwable cause) {
			super("Invalid type requested for config value \"" + path + "\"", cause);
		}
		
		public InvalidConfigValueTypeException(String path, Throwable cause, String extra) {
			super("Invalid type requested for config value \"" + path + "\"\n  " + extra, cause);
		}
	}
	
	class ConfigReflectiveOperationException extends RuntimeException {
		public ConfigReflectiveOperationException(String message, Exception cause) {
			super(message, cause);
		}
	}
}
