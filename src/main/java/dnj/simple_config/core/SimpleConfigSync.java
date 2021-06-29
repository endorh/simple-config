package dnj.simple_config.core;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlWriter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.EnumMap;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static dnj.simple_config.SimpleConfigMod.prefix;

/**
 * Handle synchronization of {@link SimpleConfig} data with server config
 */
public class SimpleConfigSync {
	
	private static final Field ConfigTracker$fileMap;
	private static final Field ConfigTracker$configSets;
	private static final Method ModConfig$setConfigData;
	private static final Method ModConfig$fireEvent;
	private static final Constructor<Reloading> Reloading$$init;
	private static final boolean reflectionSucceeded;
	private static final Logger LOGGER = LogManager.getLogger();
	
	// Ugly reflection section ----------------------------------------
	
	static {
		// Get all required private members
		final String errorFmt =
		  "Could not access %s by reflection\n" +
		  "SimpleConfig won't be able to sync server config updates ingame";
		boolean success = false;
		String member = null;
		Field fileMap = null;
		Field configSets = null;
		Method setConfigData = null;
		Method fireEvent = null;
		Constructor<Reloading> reloading = null;
		try {
			member = "ConfigTracker$fileMap field";
			fileMap = ConfigTracker.class.getDeclaredField("fileMap");
			fileMap.setAccessible(true);
			member = "ConfigTracker$configSets field";
			configSets = ConfigTracker.class.getDeclaredField("configSets");
			configSets.setAccessible(true);
			member = "ModConfig$setConfigData method";
			setConfigData = ModConfig.class.getDeclaredMethod("setConfigData", CommentedConfig.class);
			setConfigData.setAccessible(true);
			member = "ModConfig$fireEvent method";
			fireEvent = ModConfig.class.getDeclaredMethod("fireEvent", ModConfigEvent.class);
			fireEvent.setAccessible(true);
			member = "ModConfig$Reloading constructor";
			reloading = Reloading.class.getDeclaredConstructor(ModConfig.class);
			reloading.setAccessible(true);
			success = true;
		} catch (NoSuchFieldException | NoSuchMethodException e) {
			LOGGER.error(String.format(errorFmt, member));
		} finally {
			ConfigTracker$fileMap = fileMap;
			ConfigTracker$configSets = configSets;
			ModConfig$setConfigData = setConfigData;
			ModConfig$fireEvent = fireEvent;
			Reloading$$init = reloading;
			reflectionSucceeded = success;
		}
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
	private static void trySetConfigData(final ModConfig config, final CommentedConfig configData) {
		if (!reflectionSucceeded)
			throw new ConfigUpdateReflectionError();
		try {
			ModConfig$setConfigData.invoke(config, configData);
		} catch (InvocationTargetException | IllegalAccessException e) {
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
	public static void tryUpdateConfig(final String fileName, final byte[] fileData) {
		final ModConfig modConfig = getFileMap().get(fileName);
		if (modConfig == null)
			return;
		trySetConfigData(modConfig, TomlFormat.instance().createParser().parse(
		  new ByteArrayInputStream(fileData)));
		tryFireEvent(modConfig, newReloading(modConfig));
	}
	
	// Network channel ------------------------------------------------
	
	private static final String CHANNEL_PROTOCOL_VERSION = "1";
	static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
	  prefix("config"),
	  () -> CHANNEL_PROTOCOL_VERSION,
	  CHANNEL_PROTOCOL_VERSION::equals,
	  CHANNEL_PROTOCOL_VERSION::equals
	);
	static int ID_COUNT = 0;
	
	public static void registerPackets() {
		registerMessage(
		  SSimpleConfigSyncPacket.class,
		  SSimpleConfigSyncPacket::write,
		  SSimpleConfigSyncPacket::read,
		  SSimpleConfigSyncPacket::onClient,
		  NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(
		  CSimpleConfigSyncPacket.class,
		  CSimpleConfigSyncPacket::write,
		  CSimpleConfigSyncPacket::read,
		  CSimpleConfigSyncPacket::onServer,
		  NetworkDirection.PLAY_TO_SERVER);
	}
	
	private static <T> void registerMessage(
	  Class<T> msgClass, BiConsumer<T, PacketBuffer> serializer,
	  Function<PacketBuffer, T> deserializer, BiConsumer<T, Context> handler,
	  @Nullable NetworkDirection direction
	) {
		CHANNEL.registerMessage(
		  ID_COUNT++, msgClass,
		  serializer, deserializer,
		  (packet, ctxSupplier) -> {
			  final Context ctx = ctxSupplier.get();
			  ctx.enqueueWork(() -> {
				  handler.accept(packet, ctx);
				  ctx.setPacketHandled(true);
			  });
		  },
		  Optional.ofNullable(direction)
		);
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
	
	public static class CSimpleConfigSyncPacket {
		public final String modId;
		public final String fileName;
		public final byte[] fileData;
		
		public CSimpleConfigSyncPacket(
		  String modId, String fileName, byte[] fileData
		) {
			this.modId = modId;
			this.fileName = fileName;
			this.fileData = fileData;
		}
		
		public CSimpleConfigSyncPacket(SimpleConfig config) {
			modId = config.modId;
			final Pair<String, byte[]> pair = tryGetConfigData(modId);
			fileName = pair.getLeft();
			fileData = pair.getRight();
		}
		
		public void onServer(Context ctx) {
			final ServerPlayerEntity sender = ctx.getSender();
			// Ensure the config has been registered as a SimpleConfig
			SimpleConfig.getInstance(modId, Type.SERVER);
			final String modName = SimpleConfig.getModNameOrId(modId);
			if (sender == null)
				throw new IllegalStateException(
				  "Received server config update from non-player source for mod \"" + modName + "\"");
			final String senderName = sender.getScoreboardName();
			if (!sender.hasPermissionLevel(2)) {
				LOGGER.warn("Player \"" + senderName + "\" tried to modify " +
				            "the server config for mod \"" + modName + "\" " +
				            "without operator privileges");
				broadcastToOperators(new TranslationTextComponent(
				  "simple-config.config.msg.tried_to_update_by_non_operator",
				  senderName, modName).mergeStyle(TextFormatting.RED));
				return;
			}
			
			try {
				tryUpdateConfig(fileName, fileData);
				// config.bake(); // This should happen as a consequence of the reloading event
			} catch (ConfigUpdateReflectionError e) {
				e.printStackTrace();
				LOGGER.error("Error updating server config for mod \"" + modName + "\"");
				broadcastToOperators(new TranslationTextComponent(
				  "simple-config.config.msg.error_updating_by_operator",
				  modName, senderName, e.getMessage()).mergeStyle(TextFormatting.DARK_RED));
			}
			
			LOGGER.warn("Server config for mod \"" + modName + "\" " +
			            "has been updated by operator \"" + senderName + "\"");
			broadcastToOperators(new TranslationTextComponent(
			  "simple-config.config.msg.updated_by_operator",
			  modName, senderName).mergeStyle(TextFormatting.GOLD));
			new SSimpleConfigSyncPacket(modId, fileName, fileData).sendExcept(sender);
		}
		
		public static void broadcastToOperators(ITextComponent message) {
			ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().stream()
			  .filter(p -> p.hasPermissionLevel(2))
			  .forEach(p -> p.sendMessage(message, Util.DUMMY_UUID));
		}
		
		public void write(PacketBuffer buf) {
			buf.writeString(modId);
			buf.writeString(fileName);
			buf.writeByteArray(fileData);
		}
		
		public static CSimpleConfigSyncPacket read(PacketBuffer buf) {
			final String modId = buf.readString(32767);
			final String fileName = buf.readString(32767);
			final byte[] fileData = buf.readByteArray();
			return new CSimpleConfigSyncPacket(modId, fileName, fileData);
		}
		
		public void send() {
			CHANNEL.sendToServer(this);
		}
	}
	
	public static class SSimpleConfigSyncPacket {
		public final String modId;
		public final String fileName;
		public final byte[] fileData;
		
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
		
		public void onClient(Context ctx) {
			if (!Minecraft.getInstance().isSingleplayer()) {
				// Ensure the config has been registered as a SimpleConfig
				SimpleConfig.getInstance(modId, Type.SERVER);
				final String modName = SimpleConfig.getModNameOrId(modId);
				try {
					tryUpdateConfig(fileName, fileData);
					// config.bake(); // This should happen as a consequence of the reloading event
				} catch (ConfigUpdateReflectionError e) {
					e.printStackTrace();
					sendMessage(new TranslationTextComponent(
					  "simple-config.config.msg.error_updating_from_server",
					  modName, e.getMessage()));
				}
			}
		}
		
		public static void sendMessage(ITextComponent message) {
			final ClientPlayerEntity player = Minecraft.getInstance().player;
			if (player == null)
				return;
			player.sendMessage(message, Util.DUMMY_UUID);
		}
		
		public void write(PacketBuffer buf) {
			buf.writeString(modId);
			buf.writeString(fileName);
			buf.writeByteArray(fileData);
		}
		
		public static SSimpleConfigSyncPacket read(PacketBuffer buf) {
			final String modId = buf.readString(32767);
			final String fileName = buf.readString(32767);
			final byte[] fileData = buf.readByteArray();
			return new SSimpleConfigSyncPacket(modId, fileName, fileData);
		}
		
		public void send() {
			CHANNEL.send(PacketDistributor.ALL.noArg(), this);
		}
		
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
		
		public void sendExcept(ServerPlayerEntity player) {
			CHANNEL.send(EXCEPT.with(() -> player), this);
		}
	}
}
