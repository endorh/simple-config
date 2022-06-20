package endorh.simpleconfig.core;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.IndentStyle;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlWriter;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.SimpleConfigMod.ConfigPermission;
import endorh.simpleconfig.SimpleConfigMod.ServerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.text.*;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.config.ModConfig.ModConfigEvent;
import net.minecraftforge.fml.config.ModConfig.Reloading;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent.Context;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.Nullable;
import javax.naming.NoPermissionException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Handle synchronization of {@link SimpleConfig} data with server config
 */
@Internal public class SimpleConfigSync {
	
	private static final Field ConfigTracker$fileMap;
	private static final Field ConfigTracker$configSets;
	private static final Method ModConfig$fireEvent;
	private static final Constructor<Reloading> Reloading$$init;
	private static final Method ServerLifecycleHooks$getServerConfigPath;
	private static final boolean reflectionSucceeded;
	private static final Logger LOGGER = LogManager.getLogger();
	
	// Ugly reflection section ----------------------------------------
	
	static {
		// Get all required private members
		final String errorFmt =
		  "Could not access %s by reflection\n" +
		  "SimpleConfig won't be able to sync server config modifications ingame";
		boolean success = false;
		String member = null;
		Field fileMap = null;
		Field configSets = null;
		Method fireEvent = null;
		Constructor<Reloading> reloading = null;
		Method serverConfig = null;
		try {
			member = "ConfigTracker$fileMap field";
			fileMap = ConfigTracker.class.getDeclaredField("fileMap");
			fileMap.setAccessible(true);
			member = "ConfigTracker$configSets field";
			configSets = ConfigTracker.class.getDeclaredField("configSets");
			configSets.setAccessible(true);
			member = "ModConfig$fireEvent method";
			fireEvent = ModConfig.class.getDeclaredMethod("fireEvent", ModConfigEvent.class);
			fireEvent.setAccessible(true);
			member = "ModConfig$Reloading constructor";
			reloading = Reloading.class.getDeclaredConstructor(ModConfig.class);
			reloading.setAccessible(true);
			serverConfig = ServerLifecycleHooks.class.getDeclaredMethod("getServerConfigPath", MinecraftServer.class);
			serverConfig.setAccessible(true);
			success = true;
		} catch (NoSuchFieldException | NoSuchMethodException e) {
			LOGGER.error(String.format(errorFmt, member));
		} finally {
			ConfigTracker$fileMap = fileMap;
			ConfigTracker$configSets = configSets;
			ModConfig$fireEvent = fireEvent;
			Reloading$$init = reloading;
			ServerLifecycleHooks$getServerConfigPath = serverConfig;
			reflectionSucceeded = success;
		}
	}
	
	private static void broadcastToOperators(ITextComponent message) {
		ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().stream()
		  .filter(p -> p.hasPermissions(2))
		  .forEach(p -> p.sendMessage(message, Util.NIL_UUID));
	}
	
	public static class ConfigUpdateReflectionError extends RuntimeException {
		private ConfigUpdateReflectionError(Throwable cause) {
			super("Something went wrong updating the server configs", cause);
		}
		private ConfigUpdateReflectionError() {
			super("Something went wrong updating the server configs");
		}
	}
	
	private static ConcurrentHashMap<String, ModConfig> getFileMap() {
		if (!reflectionSucceeded)
			throw new ConfigUpdateReflectionError();
		try {
			//noinspection unchecked
			return (ConcurrentHashMap<String, ModConfig>) ConfigTracker$fileMap.get(ConfigTracker.INSTANCE);
		} catch (IllegalAccessException | ClassCastException e) {
			throw new ConfigUpdateReflectionError(e);
		}
	}
	private static EnumMap<Type, Set<ModConfig>> getConfigSets() {
		if (!reflectionSucceeded)
			throw new ConfigUpdateReflectionError();
		try {
			//noinspection unchecked
			return (EnumMap<Type, Set<ModConfig>>) ConfigTracker$configSets.get(ConfigTracker.INSTANCE);
		} catch (IllegalAccessException | ClassCastException e) {
			throw new ConfigUpdateReflectionError(e);
		}
	}
	private static void tryFireEvent(final ModConfig config, final ModConfigEvent event) {
		if (!reflectionSucceeded)
			throw new ConfigUpdateReflectionError();
		try {
			ModConfig$fireEvent.invoke(config, event);
		} catch (InvocationTargetException | IllegalAccessException e) {
			throw new ConfigUpdateReflectionError(e);
		}
	}
	private static Reloading newReloading(final ModConfig config) {
		if (!reflectionSucceeded)
			throw new ConfigUpdateReflectionError();
		try {
			return Reloading$$init.newInstance(config);
		} catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
			throw new ConfigUpdateReflectionError(e);
		}
	}
	private static void tryUpdateConfig(final String fileName, final byte[] fileData) {
		final ModConfig modConfig = getFileMap().get(fileName);
		if (modConfig == null)
			return;
		
		// The entry sets should match between clients and server
		final CommentedConfig sentConfig = TomlFormat.instance().createParser()
		  .parse(new ByteArrayInputStream(fileData));
		
		modConfig.getConfigData().putAll(sentConfig);
		modConfig.getSpec().afterReload();
		
		tryFireEvent(modConfig, newReloading(modConfig));
	}
	private static Path serverConfigDir = null;
	private static Path tryGetConfigDir() {
		if (serverConfigDir != null) return serverConfigDir;
		if (!reflectionSucceeded)
			throw new ConfigUpdateReflectionError();
		final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		try {
			return serverConfigDir = (Path) ServerLifecycleHooks$getServerConfigPath.invoke(null, server);
		} catch (InvocationTargetException | IllegalAccessException | ClassCastException e) {
			throw new ConfigUpdateReflectionError(e);
		}
	}
	
	// Network channel ------------------------------------------------
	
	private static final String CHANNEL_PROTOCOL_VERSION = "1";
	private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
	  new ResourceLocation(SimpleConfigMod.MOD_ID, "config"),
	  () -> CHANNEL_PROTOCOL_VERSION,
	  CHANNEL_PROTOCOL_VERSION::equals,
	  CHANNEL_PROTOCOL_VERSION::equals
	);
	private static int ID_COUNT = 0;
	
	protected static void registerPackets() {
		if (ID_COUNT != 0) throw new IllegalStateException("Packets registered twice!");
		registerServer(SSimpleConfigSyncPacket::new);
		registerServer(SSimpleConfigSavedSnapshotPacket::new);
		registerServer(SSimpleConfigSnapshotListPacket::new);
		registerServer(SSimpleConfigSnapshotPacket::new);
		
		registerClient(CSimpleConfigSyncPacket::new);
		registerClient(CSimpleConfigSaveSnapshotPacket::new);
		registerClient(CSimpleConfigRequestSnapshotListPacket::new);
		registerClient(CSimpleConfigRequestSnapshotPacket::new);
	}
	
	private static <Packet extends CAbstractPacket> void registerClient(Supplier<Packet> factory) {
		registerMessage(factory, NetworkDirection.PLAY_TO_SERVER);
	}
	
	private static <Packet extends SAbstractPacket> void registerServer(Supplier<Packet> factory) {
		registerMessage(factory, NetworkDirection.PLAY_TO_CLIENT);
	}
	
	private static <Packet extends AbstractPacket> void registerMessage(
	  Supplier<Packet> factory, @Nullable NetworkDirection direction
	) {
		final Packet msg = factory.get();
		//noinspection unchecked
		CHANNEL.registerMessage(
		  ID_COUNT++, ((Class<Packet>) msg.getClass()),
		  AbstractPacket::write,
		  b -> {
			  final Packet p = factory.get();
			  p.read(b);
			  return p;
		  }, (p, ctxSupplier) -> p.handle(ctxSupplier.get()),
		  Optional.ofNullable(direction));
	}
	
	// Packets
	private static Pair<String, byte[]> tryGetConfigData(String modId) {
		Optional<ModConfig> opt = getConfigSets().get(Type.SERVER)
		  .stream().filter(mc -> modId.equals(mc.getModId())).findFirst();
		if (!opt.isPresent())
			throw new IllegalArgumentException(
			  "Could not find mod config for simple config of mod \"" + SimpleConfig
				 .getModNameOrId(modId) + "\"");
		final ModConfig modConfig = opt.get();
		final String fileName = modConfig.getFileName();
		try {
			final CommentedConfig configData = modConfig.getConfigData();
			if (configData instanceof CommentedFileConfig) {
				final byte[] fileData =
				  Files.readAllBytes(((CommentedFileConfig) configData).getNioPath());
				return Pair.of(fileName, fileData);
			} else {
				final TomlWriter writer = TomlFormat.instance().createWriter();
				ByteArrayOutputStream arrayWriter = new ByteArrayOutputStream();
				writer.write(
				  configData.unmodifiable(), arrayWriter);
				final byte[] fileData = arrayWriter.toByteArray();
				return Pair.of(fileName, fileData);
			}
		} catch (IOException e) {
			throw new RuntimeException("Error reading server config file");
		}
	}
	
	protected static Optional<Path> getConfigFilePath(SimpleConfig config) {
		try {
			return getConfigSets().get(config.type).stream().filter(
			  mc -> config.modId.equals(mc.getModId())
			        && mc.getConfigData() instanceof CommentedFileConfig
			).findFirst().map(ModConfig::getFullPath);
		} catch (ConfigUpdateReflectionError e) {
			return Optional.empty();
		}
	}
	
	/**
	 * Subclasses must have a constructor with no parameters
	 */
	protected static abstract class AbstractPacket {
		protected abstract void handle(Context ctx);
		public abstract void write(PacketBuffer buf);
		public abstract void read(PacketBuffer buf);
	}
	
	protected static abstract class CAbstractPacket extends AbstractPacket {
		@Override protected final void handle(Context ctx) {
			ctx.setPacketHandled(true);
			onServer(ctx);
		}
		
		public void onServer(Context ctx) {}
		
		public void send() {
			CHANNEL.sendToServer(this);
		}
	}
	
	protected static abstract class SAbstractPacket extends AbstractPacket {
		private static final PacketDistributor<ServerPlayerEntity> EXCEPT =
		  new PacketDistributor<>(
			 (distributor, supplier) -> packet -> {
				 final ServerPlayerEntity exception = supplier.get();
				 final MinecraftServer server = exception.level.getServer();
				 if (server == null)
					 return;
				 server.getPlayerList().getPlayers()
					.stream().filter(p -> exception != p)
					.forEach(p -> p.connection.send(packet));
			 }, NetworkDirection.PLAY_TO_CLIENT
		  );
		
		public static void sendMessage(ITextComponent message) {
			final ClientPlayerEntity player = Minecraft.getInstance().player;
			if (player == null)
				return;
			player.sendMessage(message, Util.NIL_UUID);
		}
		
		@Override protected void handle(Context ctx) {
			ctx.setPacketHandled(true);
			onClient(ctx);
		}
		
		public void onClient(Context ctx) {}
		
		public void sendTo(ServerPlayerEntity player) {
			CHANNEL.sendTo(this, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
		}
		
		public void sendExcept(ServerPlayerEntity player) {
			CHANNEL.send(SAbstractPacket.EXCEPT.with(() -> player), this);
		}
		
		public void sendToAll() {
			CHANNEL.send(PacketDistributor.ALL.noArg(), this);
		}
	}
	
	protected static class CSimpleConfigSyncPacket extends CAbstractPacket {
		protected String modId;
		protected String fileName;
		protected byte[] fileData;
		protected boolean requireRestart;
		
		public CSimpleConfigSyncPacket() {}
		public CSimpleConfigSyncPacket(SimpleConfig config) {
			modId = config.modId;
			final Pair<String, byte[]> pair = tryGetConfigData(modId);
			fileName = pair.getLeft();
			fileData = pair.getRight();
			requireRestart = config.anyDirtyRequiresRestart();
		}
		
		@Override public void onServer(Context ctx) {
			final ServerPlayerEntity sender = ctx.getSender();
			// Ensure the config has been registered as a SimpleConfig
			SimpleConfig.getInstance(modId, Type.SERVER);
			final String modName = SimpleConfig.getModNameOrId(modId);
			if (sender == null)
				throw new IllegalStateException(
				  "Received server config update from non-player source for mod \"" + modName + "\"");
			final String senderName = sender.getScoreboardName();
			if (ServerConfig.permissions.permissionFor(sender, modId) != ConfigPermission.ALLOW) {
				LOGGER.warn("Player \"" + senderName + "\" tried to modify " +
				            "the server config for mod \"" + modName + "\" without privileges");
				broadcastToOperators(new TranslationTextComponent(
				  "simpleconfig.config.msg.tried_to_update_by",
				  senderName, modName).withStyle(TextFormatting.RED));
				// Send back a re-sync packet
				new SSimpleConfigSyncPacket(modId, fileName, fileData).sendTo(sender);
				return;
			}
			
			try {
				
				tryUpdateConfig(fileName, fileData);
				// config.bake(); // This should happen as a consequence of the reloading event
			} catch (ConfigUpdateReflectionError e) {
				e.printStackTrace();
				LOGGER.error("Error updating server config for mod \"" + modName + "\"");
				broadcastToOperators(new TranslationTextComponent(
				  "simpleconfig.config.msg.error_updating_by_operator",
				  modName, senderName, e.getMessage()).withStyle(TextFormatting.DARK_RED));
			}
			
			LOGGER.warn("Server config for mod \"" + modName + "\" " +
			            "has been updated by operator \"" + senderName + "\"");
			IFormattableTextComponent msg = new TranslationTextComponent(
			  "simpleconfig.config.msg.updated_by",
			  modName, senderName).withStyle(TextFormatting.GOLD);
			if (requireRestart)
				msg = msg.append("\n").append(new TranslationTextComponent(
				  "simpleconfig.config.msg.server_changes_require_restart"
				).withStyle(TextFormatting.GOLD));
			broadcastToOperators(msg);
			new SSimpleConfigSyncPacket(modId, fileName, fileData).sendExcept(sender);
		}
		
		@Override public void write(PacketBuffer buf) {
			buf.writeUtf(modId);
			buf.writeUtf(fileName);
			buf.writeByteArray(fileData);
			buf.writeBoolean(requireRestart);
		}
		
		@Override public void read(PacketBuffer buf) {
			modId = buf.readUtf(32767);
			fileName = buf.readUtf(32767);
			fileData = buf.readByteArray();
			requireRestart = buf.readBoolean();
		}
	}
	
	protected static class SSimpleConfigSyncPacket extends SAbstractPacket {
		protected String modId;
		protected String fileName;
		protected byte[] fileData;
		
		public SSimpleConfigSyncPacket() {}
		public SSimpleConfigSyncPacket(SimpleConfig config) {
			modId = config.modId;
			final Pair<String, byte[]> pair = tryGetConfigData(modId);
			fileName = pair.getLeft();
			fileData = pair.getRight();
		}
		
		public SSimpleConfigSyncPacket(
		  String modId, String fileName, byte[] fileData
		) {
			this.modId = modId;
			this.fileName = fileName;
			this.fileData = fileData;
		}
		
		@Override public void onClient(Context ctx) {
			if (!Minecraft.getInstance().hasSingleplayerServer()) {
				// Ensure the config has been registered as a SimpleConfig
				SimpleConfig.getInstance(modId, Type.SERVER);
				final String modName = SimpleConfig.getModNameOrId(modId);
				try {
					tryUpdateConfig(fileName, fileData);
					// config.bake(); // This should happen as a consequence of the reloading event
				} catch (ConfigUpdateReflectionError e) {
					e.printStackTrace();
					sendMessage(new TranslationTextComponent(
					  "simpleconfig.config.msg.error_updating_from_server",
					  modName, e.getMessage()));
				}
			}
		}
		
		@Override public void write(PacketBuffer buf) {
			buf.writeUtf(modId);
			buf.writeUtf(fileName);
			buf.writeByteArray(fileData);
		}
		
		@Override public void read(PacketBuffer buf) {
			modId = buf.readUtf(32767);
			fileName = buf.readUtf(32767);
			fileData = buf.readByteArray();
		}
	}
	
	protected static class CSimpleConfigSaveSnapshotPacket extends CAbstractPacket {
		public static Map<Pair<String, String>, CompletableFuture<Void>> FUTURES = new HashMap<>();
		protected String modId;
		protected String snapshotName;
		protected @Nullable byte[] fileData; // Null means delete
		
		public CSimpleConfigSaveSnapshotPacket() {}
		public CSimpleConfigSaveSnapshotPacket(
		  String modId, String snapshotName, @Nullable CommentedConfig data
		) {
			this.modId = modId;
			this.snapshotName = snapshotName;
			if (data != null) {
				final TomlWriter writer = new TomlWriter();
				writer.setIndent(IndentStyle.SPACES_2);
				final ByteArrayOutputStream os = new ByteArrayOutputStream();
				writer.write(data.unmodifiable(), os);
				fileData = os.toByteArray();
			} else fileData = null;
		}
		
		@Override public void onServer(Context ctx) {
			final ServerPlayerEntity sender = ctx.getSender();
			if (sender == null) {
				LOGGER.warn("Received server config preset from non-player source for mod \"" + modId + "\"");
				return;
			}
			try {
				// Ensure the config has been registered as a SimpleConfig
				SimpleConfig.getInstance(modId, Type.SERVER);
				final String modName = SimpleConfig.getModNameOrId(modId);
				final String senderName = sender.getScoreboardName();
				if (ServerConfig.permissions.permissionFor(sender, modId) != ConfigPermission.ALLOW) {
					LOGGER.warn("Player \"" + senderName + "\" tried to " +
					            (fileData == null? "delete" : "save") + " a preset for the server " +
					            "config for mod \"" + modName + "\" without privileges");
					throw new NoPermissionException("No permission for server config for mod " + modName);
				}
				
				if (fileData != null) {
					try {
						final Path dir = tryGetConfigDir();
						final File dest = dir.resolve(modId + "-preset-" + snapshotName + ".toml").toFile();
						if (dest.isDirectory())
							throw new IllegalStateException("File already exists and is a directory");
						FileUtils.writeByteArrayToFile(dest, fileData);
					} catch (RuntimeException | IOException e) {
						LOGGER.error("Error saving server config preset for mod \"" + modName + "\"");
						throw e;
					}
				} else {
					try {
						final Path dir = tryGetConfigDir();
						final File dest = dir.resolve(modId + "-preset-" + snapshotName + ".toml").toFile();
						if (dest.isDirectory())
							throw new IllegalStateException("File is a directory");
						if (!dest.exists() || !dest.isFile())
							throw new IllegalArgumentException("File does not exist");
						if (!dest.delete()) throw new IllegalStateException("Unable to delete file");
					} catch (RuntimeException e) {
						LOGGER.error("Error deleting server config preset for mod \"" + modName + "\"");
						throw e;
					}
				}
				
				LOGGER.warn(
				  "Server config preset \"" + snapshotName + "\" for mod \"" + modName + "\" " +
				  "has been " + (fileData == null? "deleted" : "saved") + " by player \"" + senderName + "\"");
				
				new SSimpleConfigSavedSnapshotPacket(modId, snapshotName, null).sendTo(sender);
			} catch (RuntimeException | IOException | NoPermissionException e) {
				new SSimpleConfigSavedSnapshotPacket(
				  modId, snapshotName, e.getClass().getSimpleName() + ": " + e.getMessage()).sendTo(sender);
			}
		}
		
		@Override public void write(PacketBuffer buf) {
			buf.writeUtf(modId);
			buf.writeUtf(snapshotName);
			buf.writeBoolean(fileData != null);
			if (fileData != null)
				buf.writeByteArray(fileData);
		}
		
		@Override public void read(PacketBuffer buf) {
			modId = buf.readUtf(32767);
			snapshotName = buf.readUtf(32767);
			fileData = buf.readBoolean() ? buf.readByteArray() : null;
		}
	}
	
	protected static class SSimpleConfigSavedSnapshotPacket extends SAbstractPacket {
		protected String modId;
		protected String snapshotName;
		protected @Nullable String errorMsg;
		
		
		public SSimpleConfigSavedSnapshotPacket() {}
		public SSimpleConfigSavedSnapshotPacket(
		  String modId, String snapshotName, @Nullable String errorMsg
		) {
			this.modId = modId;
			this.snapshotName = snapshotName;
			this.errorMsg = errorMsg;
		}
		
		@Override public void onClient(Context ctx) {
			final CompletableFuture<Void> future = CSimpleConfigSaveSnapshotPacket.FUTURES.remove(Pair.of(modId, snapshotName));
			if (future == null) return;
			if (errorMsg != null) {
				future.completeExceptionally(new RemoteException(errorMsg));
			} else future.complete(null);
		}
		
		@Override public void write(PacketBuffer buf) {
			buf.writeUtf(modId);
			buf.writeUtf(snapshotName);
			buf.writeBoolean(errorMsg != null);
			if (errorMsg != null)
				buf.writeUtf(errorMsg);
		}
		
		@Override public void read(PacketBuffer buf) {
			modId = buf.readUtf(32767);
			snapshotName = buf.readUtf(32767);
			errorMsg = buf.readBoolean()? buf.readUtf(32767) : null;
		}
	}
	
	protected static class CSimpleConfigRequestSnapshotListPacket extends CAbstractPacket {
		private static final Map<String, CompletableFuture<List<String>>> FUTURES = new HashMap<>();
		protected String modId;
		
		public CSimpleConfigRequestSnapshotListPacket() {}
		public CSimpleConfigRequestSnapshotListPacket(String modId) {
			this.modId = modId;
		}
		
		@Override public void onServer(Context ctx) {
			final ServerPlayerEntity sender = ctx.getSender();
			if (sender == null || ServerConfig.permissions.permissionFor(sender, modId) != ConfigPermission.ALLOW)
				return;
			final File dir = tryGetConfigDir().toFile();
			if (!dir.exists() || !dir.isDirectory()) return;
			final Pattern pat = Pattern.compile("^" + Pattern.quote(modId) + "-preset.*\\.toml$");
			final File[] files = dir.listFiles((d, name) -> pat.matcher(name).matches());
			if (files == null) return;
			final List<String> names = Arrays.stream(files)
			  .map(
			    f -> {
				    final String name = f.getName();
				    return name.substring(
					   modId.length() + 8, // "-preset-".length()
					   name.length() - 5); // ".toml".length()
			    }
			  ).collect(Collectors.toList());
			new SSimpleConfigSnapshotListPacket(modId, names).sendTo(sender);
		}
		
		@Override public void write(PacketBuffer buf) {
			buf.writeUtf(modId);
		}
		
		@Override public void read(PacketBuffer buf) {
			modId = buf.readUtf(32767);
		}
	}
	
	protected static class SSimpleConfigSnapshotListPacket extends SAbstractPacket {
		protected String modId;
		protected List<String> names;
		
		public SSimpleConfigSnapshotListPacket() {}
		
		public SSimpleConfigSnapshotListPacket(
		  String modId, List<String> names
		) {
			this.modId = modId;
			this.names = names;
		}
		
		@Override public void onClient(Context ctx) {
			final CompletableFuture<List<String>> future =
			  CSimpleConfigRequestSnapshotListPacket.FUTURES.remove(modId);
			if (future != null) future.complete(names);
		}
		
		@Override public void write(PacketBuffer buf) {
			buf.writeUtf(modId);
			buf.writeVarInt(names.size());
			for (String name : names)
				buf.writeUtf(name);
		}
		
		@Override public void read(PacketBuffer buf) {
			modId = buf.readUtf(32767);
			names = new ArrayList<>();
			for (int i = buf.readVarInt(); i > 0; i--)
				names.add(buf.readUtf(32767));
		}
	}
	
	protected static class CSimpleConfigRequestSnapshotPacket extends CAbstractPacket {
		protected static final Map<Pair<String, String>, CompletableFuture<CommentedConfig>> FUTURES = new HashMap<>();
		protected String modId;
		protected String snapshotName;
		
		public CSimpleConfigRequestSnapshotPacket() {}
		
		public CSimpleConfigRequestSnapshotPacket(String modId, String snapshotName) {
			this.modId = modId;
			this.snapshotName = snapshotName;
		}
		
		@Override public void onServer(Context ctx) {
			final ServerPlayerEntity sender = ctx.getSender();
			if (sender == null || ServerConfig.permissions.permissionFor(sender, modId) != ConfigPermission.ALLOW)
				return;
			final File file = tryGetConfigDir().resolve(modId + "-preset-" + snapshotName + ".toml").toFile();
			if (!file.exists() || !file.isFile())
				new SSimpleConfigSnapshotPacket(modId, snapshotName, null, "File does not exist").sendTo(sender);
			try {
				final byte[] fileData = FileUtils.readFileToByteArray(file);
				try {
					Thread.sleep(4000L);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				new SSimpleConfigSnapshotPacket(modId, snapshotName, fileData, null).sendTo(sender);
			} catch (IOException e) {
				new SSimpleConfigSnapshotPacket(modId, snapshotName, null, e.getMessage()).sendTo(sender);
			}
		}
		
		@Override public void write(PacketBuffer buf) {
			buf.writeUtf(modId);
			buf.writeUtf(snapshotName);
		}
		
		@Override public void read(PacketBuffer buf) {
			modId = buf.readUtf(32767);
			snapshotName = buf.readUtf(32767);
		}
	}
	
	protected static class SSimpleConfigSnapshotPacket extends SAbstractPacket {
		protected String modId;
		protected String snapshotName;
		protected @Nullable byte[] fileData;
		protected @Nullable String errorMsg;
		
		public SSimpleConfigSnapshotPacket() {}
		public SSimpleConfigSnapshotPacket(
		  String modId, String snapshotName, @Nullable byte[] fileData, @Nullable String errorMsg
		) {
			this.modId = modId;
			this.snapshotName = snapshotName;
			this.fileData = fileData;
			this.errorMsg = errorMsg;
		}
		
		@Override public void onClient(Context ctx) {
			final CompletableFuture<CommentedConfig> future =
			  CSimpleConfigRequestSnapshotPacket.FUTURES.remove(Pair.of(modId, snapshotName));
			if (future != null) {
				if (fileData != null) {
					final CommentedConfig config = TomlFormat.instance().createParser().parse(
					  new ByteArrayInputStream(fileData));
					future.complete(config);
				} else {
					future.completeExceptionally(
					  errorMsg != null? new RemoteException(errorMsg) : new RemoteException());
				}
			}
		}
		
		@Override public void write(PacketBuffer buf) {
			buf.writeUtf(modId);
			buf.writeUtf(snapshotName);
			buf.writeBoolean(fileData != null);
			if (fileData != null)
				buf.writeByteArray(fileData);
			buf.writeBoolean(errorMsg != null);
			if (errorMsg != null)
				buf.writeUtf(errorMsg);
		}
		
		@Override public void read(PacketBuffer buf) {
			modId = buf.readUtf(32767);
			snapshotName = buf.readUtf(32767);
			fileData = buf.readBoolean()? buf.readByteArray() : null;
			errorMsg = buf.readBoolean()? buf.readUtf(32767) : null;
		}
	}
	
	@Internal protected static CompletableFuture<List<String>> requestSnapshotList(String modId) {
		if (Minecraft.getInstance().getConnection() == null)
			return CompletableFuture.completedFuture(Collections.emptyList());
		final CompletableFuture<List<String>> future = new CompletableFuture<>();
		final CompletableFuture<List<String>> prev = CSimpleConfigRequestSnapshotListPacket.FUTURES.get(modId);
		if (prev != null) prev.cancel(false);
		CSimpleConfigRequestSnapshotListPacket.FUTURES.put(modId, future);
		new CSimpleConfigRequestSnapshotListPacket(modId).send();
		return future;
	}
	
	@Internal protected static CompletableFuture<CommentedConfig> requestSnapshot(
	  String modId, String snapshotName
	) {
		if (Minecraft.getInstance().getConnection() == null)
			throw new IllegalStateException("Cannot request server snapshot when disconnected");
		final CompletableFuture<CommentedConfig> future = new CompletableFuture<>();
		final Pair<String, String> key = Pair.of(modId, snapshotName);
		final CompletableFuture<CommentedConfig> prev = CSimpleConfigRequestSnapshotPacket.FUTURES.get(key);
		if (prev != null) prev.cancel(false);
		CSimpleConfigRequestSnapshotPacket.FUTURES.put(key, future);
		new CSimpleConfigRequestSnapshotPacket(modId, snapshotName).send();
		return future;
	}
	
	// null config implies deletion
	@Internal protected static CompletableFuture<Void> saveSnapshot(
	  String modId, String snapshotName, CommentedConfig config
	) {
		final CompletableFuture<Void> future = new CompletableFuture<>();
		CSimpleConfigSaveSnapshotPacket.FUTURES.put(Pair.of(modId, snapshotName), future);
		new CSimpleConfigSaveSnapshotPacket(modId, snapshotName, config).send();
		return future;
	}
}
