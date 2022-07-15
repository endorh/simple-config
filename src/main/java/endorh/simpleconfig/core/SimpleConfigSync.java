package endorh.simpleconfig.core;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.WritingException;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.SimpleConfigMod.ConfigPermission;
import endorh.simpleconfig.SimpleConfigMod.ServerConfig;
import endorh.simpleconfig.yaml.SimpleConfigCommentedYamlFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.text.*;
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
import org.jetbrains.annotations.Nullable;

import javax.naming.NoPermissionException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Handle synchronization of {@link SimpleConfig} data with server config
 */
@Internal public class SimpleConfigSync {
	private static final Pattern HYPHEN = Pattern.compile("-");
	public static Style ALLOWED_UPDATE_STYLE = Style.EMPTY
	  .applyFormatting(TextFormatting.GRAY).setItalic(true);
	public static Style DENIED_UPDATE_STYLE = Style.EMPTY
	  .applyFormatting(TextFormatting.GOLD).setItalic(true);
	public static Style ERROR_UPDATE_STYLE = Style.EMPTY
	  .applyFormatting(TextFormatting.DARK_RED).setItalic(true);
	public static Style REQUIRES_RESTART_STYLE = Style.EMPTY
	  .applyFormatting(TextFormatting.DARK_PURPLE).setItalic(true);
	public static Style ALLOWED_SNAPSHOT_UPDATE_STYLE = ALLOWED_UPDATE_STYLE;
	public static Style DENIED_SNAPSHOT_UPDATE_STYLE = DENIED_UPDATE_STYLE;
	
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
		  "SimpleConfig won't be able to sync server config modifications in-game";
		boolean success = false;
		String member = null;
		Method fireEvent = null;
		Constructor<Reloading> reloading = null;
		Method serverConfig = null;
		try {
			member = "ModConfig$fireEvent method";
			fireEvent = ModConfig.class.getDeclaredMethod("fireEvent", ModConfigEvent.class);
			fireEvent.setAccessible(true);
			member = "ModConfig$Reloading constructor";
			reloading = Reloading.class.getDeclaredConstructor(ModConfig.class);
			reloading.setAccessible(true);
			serverConfig = ServerLifecycleHooks.class.getDeclaredMethod("getServerConfigPath", MinecraftServer.class);
			serverConfig.setAccessible(true);
			success = true;
		} catch (NoSuchMethodException e) {
			LOGGER.error(String.format(errorFmt, member));
		} finally {
			ModConfig$fireEvent = fireEvent;
			Reloading$$init = reloading;
			ServerLifecycleHooks$getServerConfigPath = serverConfig;
			reflectionSucceeded = success;
		}
	}
	
	private static void broadcastToOperators(ITextComponent message) {
		ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().stream()
		  .filter(p -> p.hasPermissionLevel(2))
		  .forEach(p -> p.sendMessage(message, Util.DUMMY_UUID));
	}
	
	public static class ConfigUpdateReflectionError extends RuntimeException {
		private ConfigUpdateReflectionError(Throwable cause) {
			super("Something went wrong updating the server configs", cause);
		}
		private ConfigUpdateReflectionError() {
			super("Something went wrong updating the server configs");
		}
	}
	
	@Internal protected static void tryFireEvent(final ModConfig config, final ModConfigEvent event) {
		if (!reflectionSucceeded)
			throw new ConfigUpdateReflectionError();
		try {
			ModConfig$fireEvent.invoke(config, event);
		} catch (InvocationTargetException | IllegalAccessException e) {
			throw new ConfigUpdateReflectionError(e);
		}
	}
	@Internal protected static Reloading newReloading(final ModConfig config) {
		if (!reflectionSucceeded)
			throw new ConfigUpdateReflectionError();
		try {
			return Reloading$$init.newInstance(config);
		} catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
			throw new ConfigUpdateReflectionError(e);
		}
	}
	private static void tryUpdateConfig(
	  final SimpleConfig config, final byte[] fileData
	) {
		SimpleConfigModConfig modConfig = config.getModConfig();
		
		SimpleConfigCommentedYamlFormat format = modConfig.getConfigFormat();
		try {
			// The entry sets should match between clients and server
			CommentedConfig sentConfig = format.createParser(false)
			  .parse(new ByteArrayInputStream(fileData));
			
			modConfig.getConfigData().putAll(sentConfig);
			modConfig.getSpec().afterReload();
			
			tryFireEvent(modConfig, newReloading(modConfig));
		} catch (ParsingException e) {
			LOGGER.error("Failed to parse synced server config for mod " + config.getModId(), e);
		}
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
	protected static byte[] serializeSnapshot(SimpleConfig config) {
		try {
			CommentedConfig snapshot = config.takeSnapshot(false);
			if (snapshot instanceof CommentedFileConfig) {
				return Files.readAllBytes(((CommentedFileConfig) snapshot).getNioPath());
			} else {
				ByteArrayOutputStream arrayWriter = new ByteArrayOutputStream();
				SimpleConfigCommentedYamlFormat format = config.getModConfig().getConfigFormat();
				format.createWriter(false).write(snapshot, arrayWriter);
				return arrayWriter.toByteArray();
			}
		} catch (IOException error) {
			throw new RuntimeException("IO error reading config file", error);
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
				 final MinecraftServer server = exception.world.getServer();
				 if (server == null)
					 return;
				 server.getPlayerList().getPlayers()
					.stream().filter(p -> exception != p)
					.forEach(p -> p.connection.sendPacket(packet));
			 }, NetworkDirection.PLAY_TO_CLIENT
		  );
		
		public static void sendMessage(ITextComponent message) {
			final ClientPlayerEntity player = Minecraft.getInstance().player;
			if (player == null)
				return;
			player.sendMessage(message, Util.DUMMY_UUID);
		}
		
		@Override protected void handle(Context ctx) {
			ctx.setPacketHandled(true);
			onClient(ctx);
		}
		
		public void onClient(Context ctx) {}
		
		public void sendTo(ServerPlayerEntity player) {
			CHANNEL.sendTo(this, player.connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
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
		protected byte[] fileData;
		protected boolean requireRestart;
		
		public CSimpleConfigSyncPacket() {}
		public CSimpleConfigSyncPacket(SimpleConfig config) {
			modId = config.getModId();
			fileData = serializeSnapshot(config);
			requireRestart = config.anyDirtyRequiresRestart();
		}
		
		@Override public void onServer(Context ctx) {
			final ServerPlayerEntity sender = ctx.getSender();
			// Ensure the config has been registered as a SimpleConfig
			SimpleConfig config = SimpleConfig.getInstance(modId, Type.SERVER);
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
				  senderName, modName).mergeStyle(DENIED_UPDATE_STYLE));
				// Send back a re-sync packet
				new SSimpleConfigSyncPacket(modId, fileData).sendTo(sender);
				return;
			}
			
			try {
				tryUpdateConfig(config, fileData);
				// config.bake(); // This should happen as a consequence of the reloading event
			} catch (ConfigUpdateReflectionError e) {
				e.printStackTrace();
				LOGGER.error("Error updating server config for mod \"" + modName + "\"");
				broadcastToOperators(new TranslationTextComponent(
				  "simpleconfig.config.msg.error_updating_by",
				  modName, senderName, e.getMessage()).mergeStyle(ERROR_UPDATE_STYLE));
			}
			
			LOGGER.info(
			  "Server config for mod \"" + modName + "\" " +
			  "has been updated by authorized player \"" + senderName + "\"");
			IFormattableTextComponent msg = new TranslationTextComponent(
			  "simpleconfig.config.msg.updated_by",
			  modName, senderName).mergeStyle(ALLOWED_UPDATE_STYLE);
			if (requireRestart)
				msg = msg.appendString("\n").append(new TranslationTextComponent(
				  "simpleconfig.config.msg.server_changes_require_restart"
				).mergeStyle(REQUIRES_RESTART_STYLE));
			broadcastToOperators(msg);
			new SSimpleConfigSyncPacket(modId, fileData).sendExcept(sender);
		}
		
		@Override public void write(PacketBuffer buf) {
			buf.writeString(modId);
			buf.writeByteArray(fileData);
			buf.writeBoolean(requireRestart);
		}
		
		@Override public void read(PacketBuffer buf) {
			modId = buf.readString(32767);
			fileData = buf.readByteArray();
			requireRestart = buf.readBoolean();
		}
	}
	
	protected static class SSimpleConfigSyncPacket extends SAbstractPacket {
		protected String modId;
		protected byte[] fileData;
		
		public SSimpleConfigSyncPacket() {}
		public SSimpleConfigSyncPacket(SimpleConfig config) {
			modId = config.getModId();
			fileData = serializeSnapshot(config);
		}
		
		public SSimpleConfigSyncPacket(
		  String modId, byte[] fileData
		) {
			this.modId = modId;
			this.fileData = fileData;
		}
		
		@Override public void onClient(Context ctx) {
			if (!Minecraft.getInstance().isSingleplayer()) {
				SimpleConfig config = SimpleConfig.getInstance(modId, Type.SERVER);
				final String modName = SimpleConfig.getModNameOrId(modId);
				try {
					tryUpdateConfig(config, fileData);
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
			buf.writeString(modId);
			buf.writeByteArray(fileData);
		}
		
		@Override public void read(PacketBuffer buf) {
			modId = buf.readString(32767);
			fileData = buf.readByteArray();
		}
	}
	
	protected static class CSimpleConfigSaveSnapshotPacket extends CAbstractPacket {
		public static Map<Pair<String, String>, CompletableFuture<Void>> FUTURES = new HashMap<>();
		protected String modId;
		protected String snapshotName;
		protected byte @Nullable [] fileData; // Null means delete
		
		public CSimpleConfigSaveSnapshotPacket() {}
		public CSimpleConfigSaveSnapshotPacket(
		  String modId, String snapshotName, @Nullable CommentedConfig data
		) {
			this.modId = modId;
			this.snapshotName = snapshotName;
			if (data != null) {
				SimpleConfig config = SimpleConfig.getInstance(modId, Type.SERVER);
				SimpleConfigCommentedYamlFormat format = config.getModConfig().getConfigFormat();
				final ByteArrayOutputStream os = new ByteArrayOutputStream();
				try {
					format.createWriter(false).write(data, os);
				} catch (WritingException e) {
					throw new SimpleConfigSyncException("Error writing config snapshot", e);
				}
				fileData = os.toByteArray();
			} else fileData = null;
		}
		
		@Override public void onServer(Context ctx) {
			final ServerPlayerEntity sender = ctx.getSender();
			if (sender == null) {
				LOGGER.error("Received server config preset from non-player source for mod \"" + modId + "\"");
				return;
			}
			String[] split = HYPHEN.split(snapshotName, 2);
			String name = split.length == 2? split[1] : snapshotName;
			String type = split[0].equals("client")? "client" : "server";
			String action = fileData == null? "delete" : "save";
			// Ensure the config has been registered as a SimpleConfig
			SimpleConfig.getInstance(modId, Type.SERVER);
			final String modName = SimpleConfig.getModNameOrId(modId);
			final String senderName = sender.getScoreboardName();
			try {
				if (ServerConfig.permissions.permissionFor(sender, modId) != ConfigPermission.ALLOW)
					throw new NoPermissionException("No permission for server config for mod " + modName);
				
				if (fileData != null) {
					final Path dir = tryGetConfigDir();
					final File dest = dir.resolve(modId + "-preset-" + snapshotName + ".yaml").toFile();
					if (dest.isDirectory())
						throw new IllegalStateException("File already exists and is a directory");
					FileUtils.writeByteArrayToFile(dest, fileData);
				} else {
					final Path dir = tryGetConfigDir();
					final File dest = dir.resolve(modId + "-preset-" + snapshotName + ".yaml").toFile();
					BasicFileAttributes attr = Files.readAttributes(dest.toPath(), BasicFileAttributes.class);
					if (attr.isDirectory())
						throw new IllegalStateException("File is a directory");
					if (!dest.exists() || !attr.isRegularFile())
						throw new IllegalArgumentException("File does not exist");
					if (!dest.delete()) throw new IllegalStateException("Unable to delete file");
				}
				
				broadcastToOperators(new TranslationTextComponent(
				  "simpleconfig.config.msg.snapshot." + type + "." + action + "d_by",
				  name, modName, senderName).mergeStyle(ALLOWED_SNAPSHOT_UPDATE_STYLE));
				LOGGER.info(
				  "Server config preset \"" + snapshotName + "\" for mod \"" + modName + "\" " +
				  "has been " + action + "d by player \"" + senderName + "\"");
				new SSimpleConfigSavedSnapshotPacket(modId, snapshotName, null).sendTo(sender);
			} catch (RuntimeException | IOException e) {
				broadcastToOperators(new TranslationTextComponent(
				  "simpleconfig.config.msg.snapshot.error_updating_by",
				  name, modName, senderName, e.getMessage()
				).mergeStyle(ERROR_UPDATE_STYLE));
				LOGGER.error("Error " + (fileData != null? "saving" : "deleting") + " server config " +
				             "preset for mod \"" + modName + "\"");
				new SSimpleConfigSavedSnapshotPacket(
				  modId, snapshotName, e.getClass().getSimpleName() + ": " + e.getMessage()).sendTo(sender);
			} catch (NoPermissionException e) {
				broadcastToOperators(new TranslationTextComponent(
				  "simpleconfig.config.msg.snapshot." + type + ".tried_to_" + action,
				  senderName, name, modName
				).mergeStyle(DENIED_SNAPSHOT_UPDATE_STYLE));
				LOGGER.warn("Player \"" + senderName + "\" tried to " +
				            action + " a preset for the server " +
				            "config for mod \"" + modName + "\" without privileges");
				new SSimpleConfigSavedSnapshotPacket(
				  modId, snapshotName, e.getClass().getSimpleName() + ": " + e.getMessage()).sendTo(sender);
			}
		}
		
		@Override public void write(PacketBuffer buf) {
			buf.writeString(modId);
			buf.writeString(snapshotName);
			buf.writeBoolean(fileData != null);
			if (fileData != null)
				buf.writeByteArray(fileData);
		}
		
		@Override public void read(PacketBuffer buf) {
			modId = buf.readString(32767);
			snapshotName = buf.readString(32767);
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
			buf.writeString(modId);
			buf.writeString(snapshotName);
			buf.writeBoolean(errorMsg != null);
			if (errorMsg != null)
				buf.writeString(errorMsg);
		}
		
		@Override public void read(PacketBuffer buf) {
			modId = buf.readString(32767);
			snapshotName = buf.readString(32767);
			errorMsg = buf.readBoolean()? buf.readString(32767) : null;
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
			final Pattern pat = Pattern.compile(
			  "^(?<file>" + Pattern.quote(modId) + "-preset-(?<preset>.*)\\.yaml)$");
			final File[] files = dir.listFiles((d, name) -> pat.matcher(name).matches());
			if (files == null) return;
			final List<String> names = Arrays.stream(files)
			  .map(f -> {
				  Matcher m = pat.matcher(f.getName());
				  if (!m.matches()) return null;
				  return m.group("preset");
			  }).filter(Objects::nonNull).collect(Collectors.toList());
			new SSimpleConfigSnapshotListPacket(modId, names).sendTo(sender);
		}
		
		@Override public void write(PacketBuffer buf) {
			buf.writeString(modId);
		}
		
		@Override public void read(PacketBuffer buf) {
			modId = buf.readString(32767);
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
			buf.writeString(modId);
			buf.writeVarInt(names.size());
			for (String name : names)
				buf.writeString(name);
		}
		
		@Override public void read(PacketBuffer buf) {
			modId = buf.readString(32767);
			names = new ArrayList<>();
			for (int i = buf.readVarInt(); i > 0; i--)
				names.add(buf.readString(32767));
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
			Path dir = tryGetConfigDir();
			File file = dir.resolve(modId + "-preset-" + snapshotName + ".yaml").toFile();
			if (!file.isFile())
				new SSimpleConfigSnapshotPacket(
				  modId, snapshotName, null, "File does not exist"
				).sendTo(sender);
			try {
				new SSimpleConfigSnapshotPacket(
				  modId, snapshotName, FileUtils.readFileToByteArray(file), null
				).sendTo(sender);
			} catch (IOException e) {
				new SSimpleConfigSnapshotPacket(modId, snapshotName, null, e.getMessage()).sendTo(sender);
			}
		}
		
		@Override public void write(PacketBuffer buf) {
			buf.writeString(modId);
			buf.writeString(snapshotName);
		}
		
		@Override public void read(PacketBuffer buf) {
			modId = buf.readString(32767);
			snapshotName = buf.readString(32767);
		}
	}
	
	protected static class SSimpleConfigSnapshotPacket extends SAbstractPacket {
		protected String modId;
		protected String snapshotName;
		protected byte @Nullable[] fileData;
		protected @Nullable String errorMsg;
		
		public SSimpleConfigSnapshotPacket() {}
		public SSimpleConfigSnapshotPacket(
		  String modId, String snapshotName, byte @Nullable[] fileData, @Nullable String errorMsg
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
					SimpleConfig config = SimpleConfig.getInstance(modId, Type.SERVER);
					SimpleConfigCommentedYamlFormat format = config.getModConfig().getConfigFormat();
					try {
						CommentedConfig snapshot = format.createParser(false)
						  .parse(new ByteArrayInputStream(fileData));
						future.complete(snapshot);
						return;
					} catch (ParsingException parseException) {
						errorMsg = "Error parsing server snapshot:\n" + parseException.getMessage();
					}
				}
				future.completeExceptionally(
				  errorMsg != null? new RemoteException(errorMsg) : new RemoteException());
			}
		}
		
		@Override public void write(PacketBuffer buf) {
			buf.writeString(modId);
			buf.writeString(snapshotName);
			buf.writeBoolean(fileData != null);
			if (fileData != null)
				buf.writeByteArray(fileData);
			buf.writeBoolean(errorMsg != null);
			if (errorMsg != null)
				buf.writeString(errorMsg);
		}
		
		@Override public void read(PacketBuffer buf) {
			modId = buf.readString(32767);
			snapshotName = buf.readString(32767);
			fileData = buf.readBoolean()? buf.readByteArray() : null;
			errorMsg = buf.readBoolean()? buf.readString(32767) : null;
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
		try {
			new CSimpleConfigSaveSnapshotPacket(modId, snapshotName, config).send();
		} catch (SimpleConfigSyncException e) {
			future.completeExceptionally(e);
		}
		return future;
	}
	
	public static class SimpleConfigSyncException extends RuntimeException {
		public SimpleConfigSyncException(String message) {
			super(message);
		}
		
		public SimpleConfigSyncException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
