package endorh.simpleconfig.api;

import net.minecraft.Util;
import net.minecraftforge.fml.config.ModConfig;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.*;

public interface SimpleConfig extends ConfigEntryHolder {
	@Internal String MOD_ID = "simpleconfig";
	
	boolean isWrapper();
	
	/**
	 * Retrieve the actual path of this file, if found
	 */
	Optional<Path> getFilePath();
	
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
	
	boolean canEdit();
	
	Type getType();
	
	String getModId();
	
	String getModName();
	
	enum Type {
		CLIENT(ModConfig.Type.CLIENT, true, false),
		COMMON(ModConfig.Type.COMMON, true, true),
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
		
		public static Set<Type> localTypes() {
			return LOCAL_TYPES;
		}
		public static Set<Type> remoteTypes() {
			return REMOTE_TYPES;
		}
		
		public static Type fromAlias(String alias) {
			return BY_ALIAS.get(alias);
		}
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
		
		public boolean isLocal() {
			return isLocal;
		}
		
		public boolean isRemote() {
			return isRemote;
		}
		
		public @NotNull ModConfig.Type asConfigType() {
			return type;
		}
		public EditType asEditType(boolean remote) {
			return Arrays.stream(EditType.values()).filter(
			  t -> t.getType() == this && t.isOnlyRemote() == remote
			).findFirst().orElseGet(() -> Arrays.stream(EditType.values()).filter(
			  t -> t.getType() == this
			).findFirst().orElse(null));
		}
		public @NotNull String getAlias() {
			return alias;
		}
	}
	
	enum EditType {
		CLIENT(Type.CLIENT, false, false),
		COMMON(Type.COMMON, false, false),
		SERVER_COMMON(Type.COMMON, true, true),
		SERVER(Type.SERVER, true, false);
		
		private final static Map<String, EditType> BY_ALIAS = Util.make(
		  new HashMap<>(values().length), m -> {
			  for (EditType v: values()) m.put(v.getAlias(), v);
		  });
		
		private static final EditType[] LOCAL_TYPES = Arrays.stream(values())
		  .filter(editType -> !editType.isRemote()).toArray(EditType[]::new);
		private static final EditType[] REMOTE_TYPES = Arrays.stream(values())
		  .filter(EditType::isRemote).toArray(EditType[]::new);
		
		public static EditType[] localTypes() {
			return LOCAL_TYPES;
		}
		
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
		
		public @NotNull Type getType() {
			return type;
		}
		public boolean isRemote() {
			return isRemote;
		}
		public boolean isOnlyRemote() {
			return onlyRemote;
		}
		public @NotNull String getAlias() {
			return alias;
		}
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
