package endorh.simpleconfig.core;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.WritingException;
import com.google.common.collect.Lists;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.api.SimpleConfig;
import endorh.simpleconfig.api.SimpleConfig.Type;
import endorh.simpleconfig.config.ServerConfig.permissions;
import endorh.simpleconfig.core.wrap.MinecraftServerConfigWrapper;
import endorh.simpleconfig.ui.api.ConfigScreenBuilder.IConfigSnapshotHandler.IExternalChangeHandler;
import endorh.simpleconfig.ui.gui.widget.PresetPickerWidget.Preset;
import endorh.simpleconfig.ui.hotkey.ConfigHotKeyLogger;
import endorh.simpleconfig.ui.hotkey.SavedHotKeyGroupPickerWidget.RemoteSavedHotKeyGroup;
import endorh.simpleconfig.ui.hotkey.SavedHotKeyGroupPickerWidget.SavedHotKeyGroup;
import endorh.simpleconfig.yaml.SimpleConfigCommentedYamlFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.config.IConfigEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent.Reloading;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.HandshakeHandler;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
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
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static endorh.simpleconfig.core.SimpleConfigSnapshotHandler.failedFuture;

/**
 * Handle synchronization of {@link SimpleConfigImpl} data with server config
 */
@Internal public class SimpleConfigNetworkHandler {
	private static final String MINECRAFT_MOD_ID = "minecraft";
	
	public static Style ALLOWED_UPDATE_STYLE = Style.EMPTY
	  .applyFormat(ChatFormatting.GRAY).withItalic(true);
	public static Style DENIED_UPDATE_STYLE = Style.EMPTY
	  .applyFormat(ChatFormatting.GOLD).withItalic(true);
	public static Style ERROR_UPDATE_STYLE = Style.EMPTY
	  .applyFormat(ChatFormatting.DARK_RED).withItalic(true);
	public static Style REQUIRES_RESTART_STYLE = Style.EMPTY
	  .applyFormat(ChatFormatting.DARK_PURPLE).withItalic(true);
	public static Style ALLOWED_SNAPSHOT_UPDATE_STYLE = ALLOWED_UPDATE_STYLE;
	public static Style DENIED_SNAPSHOT_UPDATE_STYLE = DENIED_UPDATE_STYLE;
	
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
		  "SimpleConfig won't be able to sync server config modifications in-game";
		boolean success = false;
		Method setConfigData = null;
		String member = null;
		Method fireEvent = null;
		Constructor<Reloading> reloading = null;
		try {
			member = "ModConfig$setConfigData method";
			setConfigData = ModConfig.class.getDeclaredMethod("setConfigData", CommentedConfig.class);
			setConfigData.setAccessible(true);
			member = "ModConfig$fireEvent method";
			fireEvent = ModConfig.class.getDeclaredMethod("fireEvent", IConfigEvent.class);
			fireEvent.setAccessible(true);
			member = "ModConfig$Reloading constructor";
			reloading = Reloading.class.getDeclaredConstructor(ModConfig.class);
			reloading.setAccessible(true);
			success = true;
		} catch (NoSuchMethodException e) {
			LOGGER.error(String.format(errorFmt, member));
		} finally {
			ModConfig$setConfigData = setConfigData;
			ModConfig$fireEvent = fireEvent;
			Reloading$$init = reloading;
			reflectionSucceeded = success;
		}
	}
	
	private static void broadcastToOperators(Component message) {
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
	
	@Internal protected static void trySetConfigData(ModConfig config, CommentedConfig configData) {
		if (!reflectionSucceeded)
			throw new ConfigUpdateReflectionError();
		try {
			ModConfig$setConfigData.invoke(config, configData);
		} catch (InvocationTargetException | IllegalAccessException e) {
			throw new ConfigUpdateReflectionError(e);
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
	  final SimpleConfigImpl config, final byte[] fileData, boolean set
	) {
		ModConfig modConfig = config.getModConfig();
		CommentedConfig sentConfig = deserializeSnapshot(config, fileData);
		if (sentConfig == null) return;
		try {
			Map<String, ModConfig> extraConfigs = config.getExtraModConfigs();
			extraConfigs.forEach((id, extra) -> {
				Object sub = sentConfig.remove(id);
				if (sub instanceof CommentedConfig subConfig)
					putOrSet(set, extra, subConfig);
				extra.getSpec().afterReload();
				
				tryFireEvent(extra, newReloading(extra));
			});
		} catch (IllegalStateException | ParsingException e) {
			LOGGER.error("Failed to parse synced server config for mod " + config.getModId(), e);
		}
		if (modConfig == null) {
			// Minecraft Gamerules Simple Config wrapper
			config.loadSnapshot(sentConfig, false, false);
		} else try {
			putOrSet(set, modConfig, sentConfig);
			modConfig.getSpec().afterReload();
			
			tryFireEvent(modConfig, newReloading(modConfig));
		} catch (IllegalStateException | ParsingException e) {
			LOGGER.error("Failed to parse synced server config for mod " + config.getModId(), e);
		}
	}
	private static void putOrSet(boolean set, ModConfig config, CommentedConfig data) {
		if (set) {
			trySetConfigData(config, data);
		} else config.getConfigData().putAll(data);
	}
	
	protected static CommentedConfig deserializeSnapshot(
	  final SimpleConfigImpl config, final byte[] fileData
	) {
		SimpleConfigCommentedYamlFormat format = config.getConfigFormat();
		try {
			return format.createParser(false).parse(new ByteArrayInputStream(fileData));
		} catch (IllegalStateException | ParsingException e) {
			LOGGER.error("Failed to parse synced server config for mod " + config.getModId(), e);
			return null;
		}
	}
	
	protected static byte[] serializeSnapshot(
	  SimpleConfigImpl config, @Nullable CommentedConfig snapshot
	) {
		try {
			if (snapshot == null) snapshot = config.takeSnapshot(false, false);
			if (snapshot instanceof CommentedFileConfig) {
				return Files.readAllBytes(((CommentedFileConfig) snapshot).getNioPath());
			} else {
				ByteArrayOutputStream arrayWriter = new ByteArrayOutputStream();
				SimpleConfigCommentedYamlFormat format = config.getConfigFormat();
				format.createWriter(false).write(snapshot, arrayWriter);
				return arrayWriter.toByteArray();
			}
		} catch (IOException error) {
			throw new RuntimeException("IO error reading config file", error);
		}
	}
	
	// Network channel ------------------------------------------------
	
	private static final String CHANNEL_PROTOCOL_VERSION = "1";
	private static final ResourceLocation CHANNEL_NAME = new ResourceLocation(
	  SimpleConfigMod.MOD_ID, "config");
	private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
	  CHANNEL_NAME, () -> CHANNEL_PROTOCOL_VERSION,
	  CHANNEL_PROTOCOL_VERSION::equals,
	  CHANNEL_PROTOCOL_VERSION::equals);
	private static int ID_COUNT = 0;
	private static int loginID = 0;
	private static int dedicatedServerLoginID = -1;
	private static boolean isConnectedToDedicatedServer = false;
	
	public static SimpleChannel getChannel() {
		return CHANNEL;
	}
	
	public static boolean isServerReady() {
		if (FMLEnvironment.dist != Dist.DEDICATED_SERVER) return false;
		return ServerLifecycleHooks.getCurrentServer() != null;
	}
	
	public static boolean isConnectedToSimpleConfigServer() {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		if (connection == null) return false;
		return getChannel().isRemotePresent(connection.getConnection());
	}
	
	public static boolean isConnectedToDedicatedServer() {
		return isConnectedToDedicatedServer;
	}
	
	@EventBusSubscriber(value = Dist.CLIENT, modid = SimpleConfigMod.MOD_ID)
	@Internal public static class LoginEventSubscriber {
		@SubscribeEvent public static void onClientLogin(ClientPlayerNetworkEvent.LoggedInEvent event) {
			// LoggingIn event occurs after SDedicatedServerLoginPacket is handled, so we need
			//   to compare a pair of login IDs to properly check if the packet was sent for this login
			isConnectedToDedicatedServer = dedicatedServerLoginID == loginID++;
		}
	}
	
	// Registering ----------------------------------------------------
	
	@Internal public static void registerPackets() {
		if (ID_COUNT != 0) throw new IllegalStateException("Packets registered twice!");
		registerServer(SSimpleConfigSyncPacket::new);
		registerServer(SSimpleConfigSavedPresetPacket::new);
		registerServer(SSimpleConfigPresetListPacket::new);
		registerServer(SSimpleConfigPresetPacket::new);
		registerServer(SSimpleConfigSavedHotKeyGroupsPacket::new);
		registerServer(SSavedHotKeyGroupPacket::new);
		registerServer(SSaveRemoteHotKeyGroupPacket::new);
		registerServer(SSimpleConfigServerCommonConfigPacket::new);
		registerServer(SSimpleConfigPatchReportPacket::new);
		registerServer(SSimpleConfigServerPropertiesPacket::new);
		
		registerClient(CSimpleConfigSyncPacket::new);
		registerClient(CSimpleConfigSavePresetPacket::new);
		registerClient(CSimpleConfigRequestPresetListPacket::new);
		registerClient(CSimpleConfigRequestPresetPacket::new);
		registerClient(CSimpleConfigRequestSavedHotKeyGroupsPacket::new);
		registerClient(CRequestSavedHotKeyGroupPacket::new);
		registerClient(CSaveRemoteHotKeyGroupPacket::new);
		registerClient(CSimpleConfigRequestServerCommonConfigPacket::new);
		registerClient(CSimpleConfigSaveServerCommonConfigPacket::new);
		registerClient(CSimpleConfigReleaseServerCommonConfigPacket::new);
		registerClient(CSimpleConfigApplyPatchPacket::new);
		registerClient(CSimpleConfigServerPropertiesRequestPacket::new);
		
		registerLogin(CAcknowledgePacket::new);
		registerLogin(SLoginConfigDataPacket::new, SimpleConfigNetworkHandler::getLoginConfigDataPackets);
		registerLogin(SDedicatedServerLoginPacket::new, isLocal -> Lists.newArrayList(
		  Pair.of(MINECRAFT_MOD_ID, new SDedicatedServerLoginPacket(!isLocal))));
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
		Class<Packet> msgClass = (Class<Packet>) msg.getClass();
		CHANNEL.messageBuilder(msgClass, ID_COUNT++, direction)
		  .encoder(AbstractPacket::write)
		  .decoder(AbstractPacket.decoder(factory))
		  .consumer(AbstractPacket::handle)
		  .add();
	}
	
	private static <Packet extends SAbstractLoginPacket> void registerLogin(
	  Supplier<Packet> factory,
	  Function<Boolean, List<Pair<String, Packet>>> packetListBuilder
	) {
		Packet msg = factory.get();
		//noinspection unchecked
		Class<Packet> msgClass = (Class<Packet>) msg.getClass();
		CHANNEL.messageBuilder(
			 msgClass, ID_COUNT++, NetworkDirection.LOGIN_TO_CLIENT)
		  .loginIndex(ILoginPacket::getLoginIndex, ILoginPacket::setLoginIndex)
		  .encoder(SAbstractLoginPacket::write)
		  .decoder(AbstractPacket.decoder(factory))
		  .consumer(HandshakeHandler.biConsumerFor(SAbstractLoginPacket::handleWithReply))
		  .buildLoginPacketList(packetListBuilder)
		  .add();
	}
	
	private static <Packet extends CAbstractLoginPacket> void registerLogin(
	  Supplier<Packet> factory
	) {
		Packet msg = factory.get();
		//noinspection unchecked
		Class<Packet> msgClass = (Class<Packet>) msg.getClass();
		CHANNEL.messageBuilder(
			 msgClass, ID_COUNT++, NetworkDirection.LOGIN_TO_SERVER)
		  .loginIndex(ILoginPacket::getLoginIndex, ILoginPacket::setLoginIndex)
		  .encoder(CAbstractLoginPacket::write)
		  .decoder(AbstractPacket.decoder(factory))
		  .consumer(HandshakeHandler.indexFirst(CAbstractLoginPacket::handle))
		  .add();
	}
	
	// Packet Utils ---------------------------------------------------
	
	/**
	 * Subclasses must have a no-arg constructor
	 */
	protected abstract static class AbstractPacket {
		protected abstract void handle(Supplier<Context> ctxSupplier);
		public abstract void write(FriendlyByteBuf buf);
		public abstract void read(FriendlyByteBuf buf);
		
		public static <Packet extends AbstractPacket> Function<FriendlyByteBuf, Packet> decoder(
		  Supplier<Packet> factory
		) {
			return buf -> {
				Packet p = factory.get();
				p.read(buf);
				return p;
			};
		}
	}
	
	protected abstract static class CAbstractPacket extends AbstractPacket {
		@Override protected final void handle(Supplier<Context> ctxSupplier) {
			Context ctx = ctxSupplier.get();
			ctx.setPacketHandled(true);
			onServer(ctx);
		}
		
		public void onServer(Context ctx) {}
		
		public void send() {
			CHANNEL.sendToServer(this);
		}
	}
	
	protected abstract static class SAbstractPacket extends AbstractPacket {
		private static final PacketDistributor<ServerPlayer> EXCEPT = new PacketDistributor<>(
		  (distributor, supplier) -> packet -> {
			  final ServerPlayer exception = supplier.get();
			  final MinecraftServer server = exception.level.getServer();
			  if (server == null) return;
			  server.getPlayerList().getPlayers()
				 .stream().filter(p -> exception != p)
				 .forEach(p -> p.connection.send(packet));
		  }, NetworkDirection.PLAY_TO_CLIENT);
		private static final PacketDistributor<ServerPlayer> OPS_EXCEPT = new PacketDistributor<>(
		  (distributor, supplier) -> packet -> {
			  final ServerPlayer exception = supplier.get();
			  final MinecraftServer server = exception.level.getServer();
			  if (server == null) return;
			  server.getPlayerList().getPlayers()
			    .stream().filter(p -> exception != p && p.hasPermissions(2))
			    .forEach(p -> p.connection.send(packet));
		  }, NetworkDirection.PLAY_TO_CLIENT);
		private static final PacketDistributor<Collection<? extends Player>> LIST =
		  new PacketDistributor<>(
		    (distributor, supplier) -> packet -> {
			    final Collection<? extends Player> players = supplier.get();
			    players.stream()
			      .filter(p -> p instanceof ServerPlayer)
			      .forEach(p -> ((ServerPlayer) p).connection.send(packet));
		    }, NetworkDirection.PLAY_TO_CLIENT);
		
		public static void sendMessage(Component message) {
			final LocalPlayer player = Minecraft.getInstance().player;
			if (player == null)
				return;
			player.sendMessage(message, Util.NIL_UUID);
		}
		
		protected void handleWithReply(Supplier<Context> ctxSupplier) {
			Context ctx = ctxSupplier.get();
			handle(ctxSupplier);
			CHANNEL.reply(new CAcknowledgePacket(), ctx);
		}
		
		@Override protected void handle(Supplier<Context> ctxSupplier) {
			Context ctx = ctxSupplier.get();
			ctx.setPacketHandled(true);
			onClient(ctx);
		}
		
		public void onClient(Context ctx) {}
		
		public void sendTo(ServerPlayer player) {
			CHANNEL.sendTo(this, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
		}
		
		public void sendTo(Collection<? extends Player> players) {
			CHANNEL.send(LIST.with(() -> Lists.newArrayList(players)), this);
		}
		
		public void sendExcept(ServerPlayer player) {
			CHANNEL.send(SAbstractPacket.EXCEPT.with(() -> player), this);
		}
		
		public void sendToOpsExcept(ServerPlayer player) {
			CHANNEL.send(SAbstractPacket.OPS_EXCEPT.with(() -> player), this);
		}
		
		public void sendToAll() {
			CHANNEL.send(PacketDistributor.ALL.noArg(), this);
		}
	}
	
	protected interface ILoginPacket extends IntSupplier {
		int getLoginIndex();
		void setLoginIndex(int index);
		@Override default int getAsInt() {
			return getLoginIndex();
		}
	}
	
	protected abstract static class SAbstractLoginPacket extends SAbstractPacket implements ILoginPacket {
		private int loginIndex;
		@Override public int getLoginIndex() {
			return loginIndex;
		}
		@Override public void setLoginIndex(int loginIndex) {
			this.loginIndex = loginIndex;
		}
		
		public void handle(HandshakeHandler handler, Supplier<Context> ctxSupplier) {
			handle(ctxSupplier);
		}
		public void handleWithReply(HandshakeHandler handler, Supplier<Context> ctxSupplier) {
			handleWithReply(ctxSupplier);
		}
		public static void handle(
		  HandshakeHandler handler, SAbstractLoginPacket packet, Supplier<Context> ctxSupplier
		) {
			packet.handle(handler, ctxSupplier);
		}
		public static void handleWithReply(
		  HandshakeHandler handler, SAbstractLoginPacket packet, Supplier<Context> ctxSupplier
		) {
			packet.handleWithReply(handler, ctxSupplier);
		}
	}
	
	protected abstract static class CAbstractLoginPacket extends CAbstractPacket implements ILoginPacket {
		private int loginIndex;
		@Override public int getLoginIndex() {
			return loginIndex;
		}
		@Override public void setLoginIndex(int loginIndex) {
			this.loginIndex = loginIndex;
		}
		
		public void handle(HandshakeHandler handler, Supplier<Context> ctxSupplier) {
			handle(ctxSupplier);
		}
		public static void handle(
		  HandshakeHandler handler, CAbstractLoginPacket packet, Supplier<Context> ctxSupplier
		) {
			packet.handle(handler, ctxSupplier);
		}
	}
	
	// Packets --------------------------------------------------------
	
	protected static class CAcknowledgePacket extends CAbstractLoginPacket {
		@Override public void onServer(Context ctx) {
			super.onServer(ctx);
		}
		
		@Override public void write(FriendlyByteBuf buf) {}
		@Override public void read(FriendlyByteBuf buf) {}
	}
	
	protected static List<Pair<String, SLoginConfigDataPacket>> getLoginConfigDataPackets(
	  boolean isLocal
	) {
		return SimpleConfigImpl.getConfigModIds().stream()
		  .map(id -> SimpleConfigImpl.hasConfig(id, SimpleConfig.Type.SERVER)
		             ? SimpleConfigImpl.getConfig(id, SimpleConfig.Type.SERVER) : null
		  ).filter(c -> c != null && !c.isWrapper())
		  .map(c -> Pair.of(
			 c.getModId(), new SLoginConfigDataPacket(c.getModId(), serializeSnapshot(c, null)))
		  ).collect(Collectors.toList());
	}
	
	public static class SLoginConfigDataPacket extends SAbstractLoginPacket {
		private String modId;
		private byte[] fileData;
		
		public SLoginConfigDataPacket() {}
		
		public SLoginConfigDataPacket(String modId, byte[] fileData) {
			this.modId = modId;
			this.fileData = fileData;
		}
		
		@Override public void onClient(Context ctx) {
			if (!Minecraft.getInstance().isLocalServer()) {
				SimpleConfigImpl config = SimpleConfigImpl.getConfig(modId, SimpleConfig.Type.SERVER);
				tryUpdateConfig(config, fileData, true);
			}
		}
		
		@Override public void write(FriendlyByteBuf buf) {
			buf.writeUtf(modId);
			buf.writeByteArray(fileData);
		}
		
		@Override public void read(FriendlyByteBuf buf) {
			modId = buf.readUtf(32767);
			fileData = buf.readByteArray();
		}
	}
	
	protected static class SDedicatedServerLoginPacket extends SAbstractLoginPacket {
		private boolean isDedicatedServer;
		
		public SDedicatedServerLoginPacket() {}
		public SDedicatedServerLoginPacket(boolean isDedicatedServer) {
			this.isDedicatedServer = isDedicatedServer;
		}
		
		@Override public void onClient(Context ctx) {
			if (isDedicatedServer) dedicatedServerLoginID = loginID;
		}
		
		@Override public void write(FriendlyByteBuf buf) {
			buf.writeBoolean(isDedicatedServer);
		}
		
		@Override public void read(FriendlyByteBuf buf) {
			isDedicatedServer = buf.readBoolean();
		}
	}
	
	protected static class CSimpleConfigSyncPacket extends CAbstractPacket {
		protected String modId;
		protected byte[] snapshot;
		protected boolean requireRestart;
		
		public CSimpleConfigSyncPacket() {}
		public CSimpleConfigSyncPacket(SimpleConfigImpl config) {
			modId = config.getModId();
			snapshot = serializeSnapshot(config, null);
			requireRestart = config.anyDirtyRequiresRestart();
		}
		
		@Override public void onServer(Context ctx) {
			final ServerPlayer sender = ctx.getSender();
			SimpleConfigImpl config = SimpleConfigImpl.getConfig(modId, SimpleConfig.Type.SERVER);
			final String modName = SimpleConfigImpl.getModNameOrId(modId);
			if (sender == null)
				throw new IllegalStateException(
				  "Received server config update from non-player source for mod \"" + modName + "\"");
			final String senderName = sender.getScoreboardName();
			if (!permissions.permissionFor(sender, modId).getLeft().canEdit()) {
				LOGGER.warn("Player \"" + senderName + "\" tried to modify " +
				            "the server config for mod \"" + modName + "\" without privileges");
				broadcastToOperators(new TranslatableComponent(
				  "simpleconfig.config.msg.tried_to_update_by", senderName, modName
				).withStyle(DENIED_UPDATE_STYLE));
				// Send back a re-sync packet
				new SSimpleConfigSyncPacket(modId, snapshot).sendTo(sender);
				return;
			}
			
			try {
				tryUpdateConfig(config, snapshot, false);
				// config.bake(); // This should happen as a consequence of the reloading event
			} catch (ConfigUpdateReflectionError e) {
				e.printStackTrace();
				LOGGER.error("Error updating server config for mod \"" + modName + "\"");
				broadcastToOperators(new TranslatableComponent(
				  "simpleconfig.config.msg.error_updating_by",
				  modName, senderName, e.getMessage()
				).withStyle(ERROR_UPDATE_STYLE));
			}
			
			LOGGER.info(
			  "Server config for mod \"" + modName + "\" " +
			  "has been updated by authorized player \"" + senderName + "\"");
			MutableComponent msg = new TranslatableComponent(
			  "simpleconfig.config.msg.updated_by",
			  modName, senderName).withStyle(ALLOWED_UPDATE_STYLE);
			if (requireRestart)
				msg = msg.append("\n").append(new TranslatableComponent(
				  "simpleconfig.config.msg.server_changes_require_restart"
				).withStyle(REQUIRES_RESTART_STYLE));
			broadcastToOperators(msg);
			new SSimpleConfigSyncPacket(modId, snapshot).sendExcept(sender);
		}
		
		@Override public void write(FriendlyByteBuf buf) {
			buf.writeUtf(modId);
			buf.writeByteArray(snapshot);
			buf.writeBoolean(requireRestart);
		}
		
		@Override public void read(FriendlyByteBuf buf) {
			modId = buf.readUtf(32767);
			snapshot = buf.readByteArray();
			requireRestart = buf.readBoolean();
		}
	}
	
	protected static class SSimpleConfigSyncPacket extends SAbstractPacket {
		protected String modId;
		protected byte[] snapshot;
		
		public SSimpleConfigSyncPacket() {}
		public SSimpleConfigSyncPacket(SimpleConfigImpl config) {
			modId = config.getModId();
			snapshot = serializeSnapshot(config, null);
		}
		
		public SSimpleConfigSyncPacket(
		  String modId, byte[] snapshot
		) {
			this.modId = modId;
			this.snapshot = snapshot;
		}
		
		@Override public void onClient(Context ctx) {
			if (!Minecraft.getInstance().isLocalServer()) {
				try {
					tryUpdateConfig(SimpleConfigImpl.getConfig(modId, SimpleConfig.Type.SERVER), snapshot, false);
				} catch (ConfigUpdateReflectionError e) {
					LOGGER.error("Error updating client config for mod \"" + modId + "\"", e);
					sendMessage(new TranslatableComponent(
					  "simpleconfig.config.msg.error_updating_from_server",
					  SimpleConfigImpl.getModNameOrId(modId), e.getMessage()));
				}
			}
		}
		
		@Override public void write(FriendlyByteBuf buf) {
			buf.writeUtf(modId);
			buf.writeByteArray(snapshot);
		}
		
		@Override public void read(FriendlyByteBuf buf) {
			modId = buf.readUtf(32767);
			snapshot = buf.readByteArray();
		}
	}
	
	protected static class CSimpleConfigRequestServerCommonConfigPacket extends CAbstractPacket {
		public static Map<String, CompletableFuture<CommentedConfig>> FUTURES = new HashMap<>();
		private String modId;
		
		public CSimpleConfigRequestServerCommonConfigPacket() {}
		public CSimpleConfigRequestServerCommonConfigPacket(String modId, CompletableFuture<CommentedConfig> future) {
			this.modId = modId;
			CompletableFuture<CommentedConfig> prev = FUTURES.get(modId);
			if (prev != null) prev.cancel(false);
			FUTURES.put(modId, future);
		}
		
		@Override public void onServer(Context ctx) {
			final ServerPlayer sender = ctx.getSender();
			final String modName = SimpleConfigImpl.getModNameOrId(modId);
			if (sender == null) throw new IllegalStateException(
			  "Received server config update from non-player source for mod \"" + modName + "\"");
			final String senderName = sender.getScoreboardName();
			if (!SimpleConfigImpl.hasConfig(modId, SimpleConfig.Type.COMMON)
			    || !permissions.permissionFor(sender, modId).getLeft().canView()
			) {
				new SSimpleConfigServerCommonConfigPacket(modId, null).sendTo(sender);
			} else {
				SimpleConfigImpl config = SimpleConfigImpl.getConfig(modId, SimpleConfig.Type.COMMON);
				config.addRemoteListener(sender);
				byte[] snapshot = serializeSnapshot(config, null);
				new SSimpleConfigServerCommonConfigPacket(modId, snapshot).sendTo(sender);
				LOGGER.info("Sending server common config for mod \"" + modName + "\" to player \"" + senderName + "\"");
			}
		}
		
		@Override public void write(FriendlyByteBuf buf) {
			buf.writeUtf(modId);
		}
		@Override public void read(FriendlyByteBuf buf) {
			modId = buf.readUtf(32767);
		}
	}
	
	protected static class SSimpleConfigServerCommonConfigPacket extends SAbstractPacket {
		private String modId;
		private byte @Nullable[] snapshot;
		
		public SSimpleConfigServerCommonConfigPacket() {}
		public SSimpleConfigServerCommonConfigPacket(SimpleConfigImpl config) {
			this(config.getModId(), serializeSnapshot(config, null));
		}
		public SSimpleConfigServerCommonConfigPacket(String modId, byte @Nullable[] snapshot) {
			this.modId = modId;
			this.snapshot = snapshot;
		}
		
		@Override public void onClient(Context ctx) {
			CompletableFuture<CommentedConfig> future = CSimpleConfigRequestServerCommonConfigPacket.FUTURES.remove(modId);
			if (future == null) {
				SimpleConfigImpl config = SimpleConfigImpl.getConfig(modId, SimpleConfig.Type.COMMON);
				SimpleConfigSnapshotHandler snapshotHandler = config.getSnapshotHandler();
				if (snapshotHandler == null) {
					LOGGER.warn("No snapshot handler for mod \"" + modId + "\"");
					sendUnsubscribeMessage();
					return;
				}
				IExternalChangeHandler handler = snapshotHandler.getExternalChangeHandler();
				if (handler == null) {
					LOGGER.warn("No external change handler for mod \"" + modId + "\"");
					sendUnsubscribeMessage();
					return;
				}
				handler.handleRemoteConfigExternalChange(SimpleConfig.EditType.SERVER_COMMON, deserializeSnapshot(config, snapshot));
			} else if (!future.isCancelled()) {
				SimpleConfigImpl config = SimpleConfigImpl.getConfig(modId, SimpleConfig.Type.COMMON);
				String modName = config.getModName();
				if (snapshot == null) {
					future.complete(null);
					LOGGER.info("Did not receive server common config for mod \"" + modName + "\"");
					return;
				}
				future.complete(deserializeSnapshot(config, snapshot));
				LOGGER.info("Received server common config for mod \"" + modName + "\"");
			}
		}
		
		protected void sendUnsubscribeMessage() {
			new CSimpleConfigReleaseServerCommonConfigPacket(modId).send();
		}
		
		@Override public void write(FriendlyByteBuf buf) {
			buf.writeUtf(modId);
			buf.writeBoolean(snapshot != null);
			if (snapshot != null) buf.writeByteArray(snapshot);
		}
		@Override public void read(FriendlyByteBuf buf) {
			modId = buf.readUtf(32767);
			snapshot = buf.readBoolean()? buf.readByteArray() : null;
		}
	}
	
	@Internal public static CompletableFuture<CommentedConfig> requestServerCommonConfig(String modId) {
		if (!isConnectedToSimpleConfigServer()) return failedFuture(
		  new IllegalStateException("Not connected to SimpleConfig server"));
		CompletableFuture<CommentedConfig> future = new CompletableFuture<>();
		new CSimpleConfigRequestServerCommonConfigPacket(modId, future).send();
		return future;
	}
	
	@Internal public static void saveServerCommonConfig(
	  String modId, SimpleConfigImpl config, boolean requiresRestart, CommentedConfig snapshot
	) {
		if (!isConnectedToSimpleConfigServer()) throw new IllegalStateException(
		  "Not connected to SimpleConfig server");
		new CSimpleConfigSaveServerCommonConfigPacket(
		  modId, requiresRestart, serializeSnapshot(config, snapshot)
		).send();
	}
	
	protected static class CSimpleConfigSaveServerCommonConfigPacket extends CAbstractPacket {
		private String modId;
		private boolean requireRestart;
		private byte @Nullable[] snapshot;
		
		public CSimpleConfigSaveServerCommonConfigPacket() {}
		public CSimpleConfigSaveServerCommonConfigPacket(
		  String modId, boolean requireRestart, byte @Nullable[] snapshot
		) {
			this.modId = modId;
			this.requireRestart = requireRestart;
			this.snapshot = snapshot;
		}
		
		@Override public void onServer(Context ctx) {
			final ServerPlayer sender = ctx.getSender();
			final String modName = SimpleConfigImpl.getModNameOrId(modId);
			if (sender == null) throw new IllegalStateException(
			  "Received server config update from non-player source for mod \"" + modName + "\"");
			final String senderName = sender.getScoreboardName();
			if (!SimpleConfigImpl.hasConfig(modId, SimpleConfig.Type.COMMON)) return;
			if (!permissions.permissionFor(sender, modId).getLeft().canEdit()) {
				LOGGER.warn("Player \"" + senderName + "\" attempted to save server common config " +
				            "for mod \"" + modName + "\"");
				broadcastToOperators(new TranslatableComponent(
				  "simpleconfig.config.msg.tried_to_update_by", senderName, modName
				).withStyle(DENIED_UPDATE_STYLE));
				return;
			}
			try {
				SimpleConfigImpl config = SimpleConfigImpl.getConfig(modId, SimpleConfig.Type.COMMON);
				tryUpdateConfig(config, snapshot, false);
			} catch (ConfigUpdateReflectionError e) {
				e.printStackTrace();
				LOGGER.error("Error updating server config for mod \"" + modName + "\"");
				broadcastToOperators(new TranslatableComponent(
				  "simpleconfig.config.msg.error_updating_by",
				  modName, senderName, e.getMessage()
				).withStyle(ERROR_UPDATE_STYLE));
				return;
			}
			LOGGER.info(
			  "Server common config for mod \"" + modName + "\" " +
			  "has been updated by authorized player \"" + senderName + "\"");
			MutableComponent msg = new TranslatableComponent(
			  "simpleconfig.config.msg.updated_by",
			  modName, senderName
			).withStyle(ALLOWED_UPDATE_STYLE);
			if (requireRestart)
				msg = msg.append("\n").append(new TranslatableComponent(
				  "simpleconfig.config.msg.server_changes_require_restart"
				).withStyle(REQUIRES_RESTART_STYLE));
			broadcastToOperators(msg);
		}
		
		@Override public void write(FriendlyByteBuf buf) {
			buf.writeUtf(modId);
			buf.writeBoolean(requireRestart);
			buf.writeBoolean(snapshot != null);
			if (snapshot != null) buf.writeByteArray(snapshot);
		}
		
		@Override public void read(FriendlyByteBuf buf) {
			modId = buf.readUtf(32767);
			requireRestart = buf.readBoolean();
			snapshot = buf.readBoolean()? buf.readByteArray() : null;
		}
	}
	
	protected static class CSimpleConfigReleaseServerCommonConfigPacket extends CAbstractPacket {
		private String modId;
		
		public CSimpleConfigReleaseServerCommonConfigPacket() {}
		public CSimpleConfigReleaseServerCommonConfigPacket(String modId) {
			this.modId = modId;
		}
		
		@Override public void onServer(Context ctx) {
			final ServerPlayer sender = ctx.getSender();
			final String modName = SimpleConfigImpl.getModNameOrId(modId);
			if (sender == null) throw new IllegalStateException(
			  "Received server config update from non-player source for mod \"" + modName + "\"");
			final String senderName = sender.getScoreboardName();
			if (!SimpleConfigImpl.hasConfig(modId, SimpleConfig.Type.COMMON)) return;
			if (!permissions.permissionFor(sender, modId).getLeft().canEdit()) {
				LOGGER.warn("Player \"" + senderName + "\" attempted to release server common config " +
				            "for mod \"" + modName + "\"");
				return;
			}
			SimpleConfigImpl config = SimpleConfigImpl.getConfig(modId, SimpleConfig.Type.COMMON);
			config.removeRemoteListener(sender);
			LOGGER.info("Released server common config for mod \"" + modName + "\" by player " + sender);
		}
		
		@Override public void write(FriendlyByteBuf buf) {
			buf.writeUtf(modId);
		}
		
		@Override public void read(FriendlyByteBuf buf) {
			modId = buf.readUtf(32767);
		}
	}
	
	protected static class CSimpleConfigApplyPatchPacket extends CAbstractPacket {
		private String modId;
		private Type type;
		private List<Component> report;
		private byte @Nullable[] snapshot;
		
		public CSimpleConfigApplyPatchPacket() {}
		public CSimpleConfigApplyPatchPacket(
		  SimpleConfigImpl config, CommentedConfig snapshot, List<Component> report
		) {
			modId = config.getModId();
			type = config.getType();
			this.snapshot = serializeSnapshot(config, snapshot);
			this.report = report;
		}
		
		@Override public void onServer(Context ctx) {
			final ServerPlayer sender = ctx.getSender();
			final String modName = SimpleConfigImpl.getModNameOrId(modId);
			if (sender == null) throw new IllegalStateException(
			  "Received server config update from non-player source for mod \"" + modName + "\"");
			final String senderName = sender.getScoreboardName();
			if (!SimpleConfigImpl.hasConfig(modId, type)) return;
			if (!permissions.permissionFor(sender, modId).getLeft().canEdit()) {
				LOGGER.warn(
				  "Player \"" + senderName + "\" attempted to apply patch to config " +
				  "for mod \"" + modName + "\"");
				return;
			}
			try {
				SimpleConfigImpl config = SimpleConfigImpl.getConfig(modId, type);
				tryUpdateConfig(config, snapshot, false);
			} catch (ConfigUpdateReflectionError e) {
				e.printStackTrace();
				LOGGER.error("Error applying snapshot to config for mod \"" + modName + "\"");
				broadcastToOperators(new TranslatableComponent(
				  "simpleconfig.config.msg.error_updating_by",
				  modName, senderName, e.getMessage()
				).withStyle(ERROR_UPDATE_STYLE));
				return;
			}
			LOGGER.info("Snapshot applied to config for mod \"" + modName + "\" by player " + sender);
			if (report.isEmpty()) {
				broadcastToOperators(new TranslatableComponent(
				  "simpleconfig.config.msg.patched_by", modName, senderName
				).withStyle(ALLOWED_UPDATE_STYLE));
			} else {
				new SSimpleConfigPatchReportPacket(new TranslatableComponent(
				  "simpleconfig.hotkey.remote", sender.getName()), report
				).sendToOpsExcept(sender);
			}
		}
		
		@Override public void write(FriendlyByteBuf buf) {
			buf.writeUtf(modId);
			buf.writeEnum(type);
			buf.writeBoolean(snapshot != null);
			if (snapshot != null) buf.writeByteArray(snapshot);
			buf.writeVarInt(report.size());
			report.forEach(buf::writeComponent);
		}
		
		@Override public void read(FriendlyByteBuf buf) {
			modId = buf.readUtf(32767);
			type = buf.readEnum(Type.class);
			snapshot = buf.readBoolean()? buf.readByteArray() : null;
			int size = buf.readVarInt();
			List<Component> report = new ArrayList<>(size);
			for (int i = 0; i < size; i++) report.add(buf.readComponent());
			this.report = report;
		}
	}
	
	protected static class SSimpleConfigPatchReportPacket extends SAbstractPacket {
		private Component title;
		private List<Component> report;
		
		public SSimpleConfigPatchReportPacket() {}
		public SSimpleConfigPatchReportPacket(Component title, List<Component> report) {
			this.title = title;
			this.report = report;
		}
		
		@Override public void onClient(Context ctx) {
			if (title != null && report != null)
				ConfigHotKeyLogger.logRemoteHotKey(title, report);
			super.onClient(ctx);
		}
		
		@Override public void write(FriendlyByteBuf buf) {
			buf.writeComponent(title);
			buf.writeVarInt(report.size());
			report.forEach(buf::writeComponent);
		}
		@Override public void read(FriendlyByteBuf buf) {
			title = buf.readComponent();
			int size = buf.readVarInt();
			List<Component> report = new ArrayList<>(size);
			for (int i = 0; i < size; i++) report.add(buf.readComponent());
			this.report = report;
		}
	}
	
	@Internal public static boolean applyRemoteSnapshot(
	  SimpleConfigImpl config, CommentedConfig snapshot, List<Component> report
	) {
		if (!isConnectedToSimpleConfigServer()
		    || !config.getType().isRemote()
		    || !permissions.permissionFor(config.getModId()).getLeft().canEdit()
		) return false;
		new CSimpleConfigApplyPatchPacket(config, snapshot, report).send();
		return true;
	}
	
	protected static class CSimpleConfigSavePresetPacket extends CAbstractPacket {
		public static Map<Triple<String, Type, String>, CompletableFuture<Void>> FUTURES = new HashMap<>();
		protected String modId;
		protected Type type;
		protected String presetName;
		protected byte @Nullable [] fileData; // Null means delete
		
		public CSimpleConfigSavePresetPacket() {}
		public CSimpleConfigSavePresetPacket(
		  String modId, Type type, String presetName, @Nullable CommentedConfig data
		) {
			this.modId = modId;
			this.type = type;
			this.presetName = presetName;
			if (data != null) {
				SimpleConfigImpl config = SimpleConfigImpl.getConfig(modId, SimpleConfig.Type.SERVER);
				SimpleConfigCommentedYamlFormat format = config.getConfigFormat();
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
			final ServerPlayer sender = ctx.getSender();
			if (sender == null) {
				LOGGER.error("Received server config preset from non-player source for mod \"" + modId + "\"");
				return;
			}
			String tt = type.getAlias();
			String presetName = tt + "-" + this.presetName;
			String fileName = modId + "-" + presetName + ".yaml";
			String action = fileData == null? "delete" : "save"; // For messages
			// Ensure the config has been registered as a SimpleConfig
			SimpleConfigImpl.getConfig(modId, SimpleConfig.Type.SERVER);
			final String modName = SimpleConfigImpl.getModNameOrId(modId);
			final String senderName = sender.getScoreboardName();
			try {
				if (!permissions.permissionFor(sender, modId).getRight().canSave())
					throw new NoPermissionException("No permission for server presets for mod " + modName);
				
				final Path dir = SimpleConfigPaths.getRemotePresetsDir();
				final File dest = dir.resolve(fileName).toFile();
				if (fileData != null) {
					if (dest.isDirectory())
						throw new IllegalStateException("File already exists and is a directory");
					FileUtils.writeByteArrayToFile(dest, fileData);
				} else {
					BasicFileAttributes attr = Files.readAttributes(dest.toPath(), BasicFileAttributes.class);
					if (attr.isDirectory())
						throw new IllegalStateException("File is a directory");
					if (!dest.exists() || !attr.isRegularFile())
						throw new IllegalArgumentException("File does not exist");
					if (!dest.delete()) throw new IllegalStateException("Unable to delete file");
				}
				
				broadcastToOperators(new TranslatableComponent(
				  "simpleconfig.config.msg.snapshot." + tt + "." + action + "d_by",
				  this.presetName, modName, senderName).withStyle(ALLOWED_SNAPSHOT_UPDATE_STYLE));
				LOGGER.info(
				  "Server config preset \"" + presetName + "\" for mod \"" + modName + "\" " +
				  "has been " + action + "d by player \"" + senderName + "\"");
				new SSimpleConfigSavedPresetPacket(modId, type, this.presetName, null).sendTo(sender);
			} catch (RuntimeException | IOException e) {
				broadcastToOperators(new TranslatableComponent(
				  "simpleconfig.config.msg.snapshot.error_updating_by",
				  this.presetName, modName, senderName, e.getMessage()
				).withStyle(ERROR_UPDATE_STYLE));
				LOGGER.error("Error " + (fileData != null? "saving" : "deleting") + " server config " +
				             "preset for mod \"" + modName + "\"");
				new SSimpleConfigSavedPresetPacket(
				  modId, type, this.presetName, e.getClass().getSimpleName() + ": " + e.getMessage()
				).sendTo(sender);
			} catch (NoPermissionException e) {
				broadcastToOperators(new TranslatableComponent(
				  "simpleconfig.config.msg.snapshot." + tt + ".tried_to_" + action,
				  senderName, this.presetName, modName
				).withStyle(DENIED_SNAPSHOT_UPDATE_STYLE));
				LOGGER.warn("Player \"" + senderName + "\" tried to " +
				            action + " a preset for the server " +
				            "config for mod \"" + modName + "\" without privileges");
				new SSimpleConfigSavedPresetPacket(
				  modId, type, this.presetName, e.getClass().getSimpleName() + ": " + e.getMessage()
				).sendTo(sender);
			}
		}
		
		@Override public void write(FriendlyByteBuf buf) {
			buf.writeUtf(modId);
			buf.writeEnum(type);
			buf.writeUtf(presetName);
			buf.writeBoolean(fileData != null);
			if (fileData != null)
				buf.writeByteArray(fileData);
		}
		
		@Override public void read(FriendlyByteBuf buf) {
			modId = buf.readUtf(32767);
			type = buf.readEnum(Type.class);
			presetName = buf.readUtf(32767);
			fileData = buf.readBoolean() ? buf.readByteArray() : null;
		}
	}
	
	protected static class SSimpleConfigSavedPresetPacket extends SAbstractPacket {
		protected String modId;
		protected Type type;
		protected String presetName;
		protected @Nullable String errorMsg;
		
		public SSimpleConfigSavedPresetPacket() {}
		public SSimpleConfigSavedPresetPacket(
		  String modId, Type type, String presetName, @Nullable String errorMsg
		) {
			this.modId = modId;
			this.type = type;
			this.presetName = presetName;
			this.errorMsg = errorMsg;
		}
		
		@Override public void onClient(Context ctx) {
			final CompletableFuture<Void> future = CSimpleConfigSavePresetPacket.FUTURES.remove(
			  Triple.of(modId, type, presetName));
			if (future == null) return;
			if (errorMsg != null) {
				future.completeExceptionally(new RemoteException(errorMsg));
			} else future.complete(null);
		}
		
		@Override public void write(FriendlyByteBuf buf) {
			buf.writeUtf(modId);
			buf.writeEnum(type);
			buf.writeUtf(presetName);
			buf.writeBoolean(errorMsg != null);
			if (errorMsg != null) buf.writeUtf(errorMsg);
		}
		
		@Override public void read(FriendlyByteBuf buf) {
			modId = buf.readUtf(32767);
			type = buf.readEnum(Type.class);
			presetName = buf.readUtf(32767);
			errorMsg = buf.readBoolean()? buf.readUtf(32767) : null;
		}
	}
	
	protected static class CSimpleConfigRequestPresetListPacket extends CAbstractPacket {
		private static final Map<String, CompletableFuture<List<Preset>>> FUTURES = new HashMap<>();
		protected String modId;
		
		public CSimpleConfigRequestPresetListPacket() {}
		public CSimpleConfigRequestPresetListPacket(
		  String modId, CompletableFuture<List<Preset>> future
		) {
			this.modId = modId;
			CompletableFuture<List<Preset>> prev = FUTURES.get(modId);
			if (prev != null) prev.cancel(true);
			FUTURES.put(modId, future);
		}
		
		@Override public void onServer(Context ctx) {
			final ServerPlayer sender = ctx.getSender();
			if (sender == null) return;
			final File dir = SimpleConfigPaths.getRemotePresetsDir().toFile();
			if (!dir.isDirectory()) return;
			final Pattern pat = Pattern.compile(
			  "^(?<file>" + Pattern.quote(modId) + "-(?<type>\\w++)-(?<name>.*)\\.yaml)$");
			final File[] files = dir.listFiles((d, name) -> pat.matcher(name).matches());
			if (files == null) return;
			final List<Preset> names = Arrays.stream(files)
			  .map(f -> {
				  Matcher m = pat.matcher(f.getName());
				  if (!m.matches()) return null;
				  return Preset.remote(m.group("name"), SimpleConfig.Type.fromAlias(m.group("type")));
			  }).filter(Objects::nonNull).collect(Collectors.toList());
			LOGGER.info("Sending server preset list for mod \"" + modId + "\" to player \"" + sender.getScoreboardName() + "\"");
			new SSimpleConfigPresetListPacket(modId, names).sendTo(sender);
		}
		
		@Override public void write(FriendlyByteBuf buf) {
			buf.writeUtf(modId);
		}
		
		@Override public void read(FriendlyByteBuf buf) {
			modId = buf.readUtf(32767);
		}
	}
	
	protected static class SSimpleConfigPresetListPacket extends SAbstractPacket {
		protected String modId;
		protected List<Preset> presets;
		
		public SSimpleConfigPresetListPacket() {}
		public SSimpleConfigPresetListPacket(
		  String modId, List<Preset> presets
		) {
			this.modId = modId;
			this.presets = presets;
		}
		
		@Override public void onClient(Context ctx) {
			LOGGER.info("Received server preset list for mod \"" + modId + "\"");
			final CompletableFuture<List<Preset>> future = CSimpleConfigRequestPresetListPacket.FUTURES.remove(modId);
			if (future != null) future.complete(presets);
		}
		
		@Override public void write(FriendlyByteBuf buf) {
			buf.writeUtf(modId);
			buf.writeVarInt(presets.size());
			for (Preset preset : presets) {
				buf.writeUtf(preset.getName());
				buf.writeEnum(preset.getType());
			}
		}
		
		@Override public void read(FriendlyByteBuf buf) {
			modId = buf.readUtf(32767);
			presets = new ArrayList<>();
			for (int i = buf.readVarInt(); i > 0; i--)
				presets.add(Preset.remote(buf.readUtf(32767), buf.readEnum(Type.class)));
		}
	}
	
	protected static class CSimpleConfigRequestPresetPacket extends CAbstractPacket {
		protected static final Map<Triple<String, Type, String>, CompletableFuture<CommentedConfig>> FUTURES = new HashMap<>();
		protected String modId;
		protected Type type;
		protected String presetName;
		
		public CSimpleConfigRequestPresetPacket() {}
		
		public CSimpleConfigRequestPresetPacket(String modId, Type type, String presetName) {
			this.modId = modId;
			this.type = type;
			this.presetName = presetName;
		}
		
		@Override public void onServer(Context ctx) {
			final ServerPlayer sender = ctx.getSender();
			if (sender == null) return;
			Path dir = SimpleConfigPaths.getRemotePresetsDir();
			String tt = type.getAlias();
			String fileName = modId + "-" + tt + "-" + presetName + ".yaml";
			File file = dir.resolve(fileName).toFile();
			if (!file.isFile())
				new SSimpleConfigPresetPacket(
				  modId, type, presetName, null, "File does not exist"
				).sendTo(sender);
			try {
				new SSimpleConfigPresetPacket(
				  modId, type, presetName, FileUtils.readFileToByteArray(file), null
				).sendTo(sender);
			} catch (IOException e) {
				new SSimpleConfigPresetPacket(modId, type, presetName, null, e.getMessage()).sendTo(sender);
			}
		}
		
		@Override public void write(FriendlyByteBuf buf) {
			buf.writeUtf(modId);
			buf.writeEnum(type);
			buf.writeUtf(presetName);
		}
		
		@Override public void read(FriendlyByteBuf buf) {
			modId = buf.readUtf(32767);
			type = buf.readEnum(Type.class);
			presetName = buf.readUtf(32767);
		}
	}
	
	protected static class SSimpleConfigPresetPacket extends SAbstractPacket {
		protected String modId;
		protected Type type;
		protected String presetName;
		protected byte @Nullable[] fileData;
		protected @Nullable String errorMsg;
		
		public SSimpleConfigPresetPacket() {}
		public SSimpleConfigPresetPacket(
		  String modId, Type type, String presetName, byte @Nullable[] fileData,
		  @Nullable String errorMsg
		) {
			this.modId = modId;
			this.type = type;
			this.presetName = presetName;
			this.fileData = fileData;
			this.errorMsg = errorMsg;
		}
		
		@Override public void onClient(Context ctx) {
			final CompletableFuture<CommentedConfig> future =
			  CSimpleConfigRequestPresetPacket.FUTURES.remove(Triple.of(modId, type, presetName));
			if (future != null) {
				if (fileData != null) {
					SimpleConfigImpl config = Arrays.stream(SimpleConfig.Type.values())
					  .filter(t -> SimpleConfigImpl.hasConfig(modId, t))
					  .findFirst().map(t -> SimpleConfigImpl.getConfig(modId, t))
					  .orElseThrow(IllegalStateException::new);
					SimpleConfigCommentedYamlFormat format = config.getConfigFormat();
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
		
		@Override public void write(FriendlyByteBuf buf) {
			buf.writeUtf(modId);
			buf.writeEnum(type);
			buf.writeUtf(presetName);
			buf.writeBoolean(fileData != null);
			if (fileData != null) buf.writeByteArray(fileData);
			buf.writeBoolean(errorMsg != null);
			if (errorMsg != null) buf.writeUtf(errorMsg);
		}
		
		@Override public void read(FriendlyByteBuf buf) {
			modId = buf.readUtf(32767);
			type = buf.readEnum(Type.class);
			presetName = buf.readUtf(32767);
			fileData = buf.readBoolean()? buf.readByteArray() : null;
			errorMsg = buf.readBoolean()? buf.readUtf(32767) : null;
		}
	}
	
	@Internal protected static CompletableFuture<List<Preset>> requestPresetList(String modId) {
		if (!isConnectedToSimpleConfigServer())
			return CompletableFuture.completedFuture(Collections.emptyList());
		final CompletableFuture<List<Preset>> future = new CompletableFuture<>();
		new CSimpleConfigRequestPresetListPacket(modId, future).send();
		return future;
	}
	
	@Internal protected static CompletableFuture<CommentedConfig> requestRemotePreset(
	  String modId, Type type, String snapshotName
	) {
		if (!isConnectedToSimpleConfigServer())
			return failedFuture(new IllegalStateException("Not connected to SimpleConfig server"));
		final CompletableFuture<CommentedConfig> future = new CompletableFuture<>();
		final Triple<String, Type, String> key = Triple.of(modId, type, snapshotName);
		final CompletableFuture<CommentedConfig> prev = CSimpleConfigRequestPresetPacket.FUTURES.get(key);
		if (prev != null) prev.cancel(false);
		CSimpleConfigRequestPresetPacket.FUTURES.put(key, future);
		new CSimpleConfigRequestPresetPacket(modId, type, snapshotName).send();
		return future;
	}
	
	// null config implies deletion
	@Internal protected static CompletableFuture<Void> saveRemotePreset(
	  String modId, Type type, String snapshotName, CommentedConfig config
	) {
		if (!isConnectedToSimpleConfigServer())
			return failedFuture(new IllegalStateException("Not connected to SimpleConfig server"));
		final CompletableFuture<Void> future = new CompletableFuture<>();
		CSimpleConfigSavePresetPacket.FUTURES.put(Triple.of(modId, type, snapshotName), future);
		try {
			new CSimpleConfigSavePresetPacket(modId, type, snapshotName, config).send();
		} catch (SimpleConfigSyncException e) {
			future.completeExceptionally(e);
		}
		return future;
	}
	
	protected static class CSimpleConfigRequestSavedHotKeyGroupsPacket extends CAbstractPacket {
		protected static CompletableFuture<List<RemoteSavedHotKeyGroup>> future = null;
		public CSimpleConfigRequestSavedHotKeyGroupsPacket() {}
		public CSimpleConfigRequestSavedHotKeyGroupsPacket(CompletableFuture<List<RemoteSavedHotKeyGroup>> future) {
			CSimpleConfigRequestSavedHotKeyGroupsPacket.future = future;
		}
		
		@Override public void onServer(Context ctx) {
			final ServerPlayer sender = ctx.getSender();
			if (sender == null) return;
			final File dir = SimpleConfigPaths.getRemoteHotKeyGroupsDir().toFile();
			if (!dir.isDirectory()) return;
			final Pattern pat = Pattern.compile("^(?<name>.*)\\.yaml$");
			final File[] files = dir.listFiles((d, name) -> pat.matcher(name).matches());
			if (files == null) return;
			final List<RemoteSavedHotKeyGroup> groups = Arrays.stream(files).map(f -> {
				Matcher m = pat.matcher(f.getName());
				if (!m.matches()) return null;
				return SavedHotKeyGroup.remote(m.group("name"));
			}).filter(Objects::nonNull).collect(Collectors.toList());
			new SSimpleConfigSavedHotKeyGroupsPacket(groups).sendTo(sender);
		}
		
		@Override public void write(FriendlyByteBuf buf) {}
		@Override public void read(FriendlyByteBuf buf) {}
	}
	
	protected static class SSimpleConfigSavedHotKeyGroupsPacket extends SAbstractPacket {
		private List<RemoteSavedHotKeyGroup> groups;
		public SSimpleConfigSavedHotKeyGroupsPacket() {}
		public SSimpleConfigSavedHotKeyGroupsPacket(List<RemoteSavedHotKeyGroup> groups) {
			this.groups = groups;
		}
		
		@Override public void onClient(Context ctx) {
			CompletableFuture<List<RemoteSavedHotKeyGroup>> future =
			  CSimpleConfigRequestSavedHotKeyGroupsPacket.future;
			if (future != null && !future.isDone()) future.complete(groups);
		}
		
		@Override public void write(FriendlyByteBuf buf) {
			buf.writeVarInt(groups.size());
			for (RemoteSavedHotKeyGroup group : groups)
				buf.writeUtf(group.getName());
		}
		
		@Override public void read(FriendlyByteBuf buf) {
			List<RemoteSavedHotKeyGroup> groups = new ArrayList<>();
			for (int i = buf.readVarInt(); i > 0; i--)
				groups.add(SavedHotKeyGroup.remote(buf.readUtf(32767)));
			this.groups = groups;
		}
	}
	
	protected static class CRequestSavedHotKeyGroupPacket extends CAbstractPacket {
		private static final Map<String, CompletableFuture<byte[]>> FUTURES = new HashMap<>();
		private String name;
		public CRequestSavedHotKeyGroupPacket() {}
		public CRequestSavedHotKeyGroupPacket(String name, CompletableFuture<byte[]> future) {
			this.name = name;
			FUTURES.put(name, future);
		}
		
		@Override public void onServer(Context ctx) {
			ServerPlayer sender = ctx.getSender();
			if (sender == null) return;
			final String fileName = name + ".yaml";
			final File file = SimpleConfigPaths.getRemoteHotKeyGroupsDir().resolve(fileName).toFile();
			if (!file.isFile())
				new SSavedHotKeyGroupPacket(name, "Cannot find hotkey group " + name).sendTo(sender);
			try {
				byte[] bytes = FileUtils.readFileToByteArray(file);
				new SSavedHotKeyGroupPacket(name, bytes).sendTo(sender);
			} catch (IOException e) {
				new SSavedHotKeyGroupPacket(name, e.getLocalizedMessage()).sendTo(sender);
			}
		}
		
		@Override public void write(FriendlyByteBuf buf) {
			buf.writeUtf(name);
		}
		
		@Override public void read(FriendlyByteBuf buf) {
			name = buf.readUtf(32767);
		}
	}
	
	protected static class SSavedHotKeyGroupPacket extends SAbstractPacket {
		private String name;
		private byte @Nullable[] data;
		private @Nullable String errorMsg;
		
		public SSavedHotKeyGroupPacket() {}
		private SSavedHotKeyGroupPacket(String name, byte @Nullable[] data, @Nullable String errorMsg) {
			this.name = name;
			this.data = data;
			this.errorMsg = errorMsg;
		}
		public SSavedHotKeyGroupPacket(String name, byte @NotNull[] data) {
			this(name, data, null);
		}
		public SSavedHotKeyGroupPacket(String name, @NotNull String errorMsg) {
			this(name, null, errorMsg);
		}
		
		@Override public void onClient(Context ctx) {
			CompletableFuture<byte[]> future = CRequestSavedHotKeyGroupPacket.FUTURES.get(name);
			if (future != null && !future.isDone()) {
				if (data == null) {
					future.completeExceptionally(new RemoteException(errorMsg != null? errorMsg : ""));
				} else future.complete(data);
			}
		}
		
		@Override public void write(FriendlyByteBuf buf) {
			buf.writeUtf(name);
			buf.writeBoolean(data != null);
			if (data != null) buf.writeByteArray(data);
			buf.writeBoolean(errorMsg != null);
			if (errorMsg != null) buf.writeUtf(errorMsg);
		}
		
		@Override public void read(FriendlyByteBuf buf) {
			name = buf.readUtf(32767);
			data = buf.readBoolean()? buf.readByteArray() : null;
			errorMsg = buf.readBoolean()? buf.readUtf(32767) : null;
		}
	}
	
	protected static class CSaveRemoteHotKeyGroupPacket extends CAbstractPacket {
		private static final Map<String, CompletableFuture<Boolean>> FUTURES = new HashMap<>();
		private String name;
		private byte @Nullable[] data;
		
		public CSaveRemoteHotKeyGroupPacket() {}
		public CSaveRemoteHotKeyGroupPacket(String name, byte @Nullable[] data, CompletableFuture<Boolean> future) {
			this.name = name;
			this.data = data;
			FUTURES.put(name, future);
		}
		
		@Override public void onServer(Context ctx) {
			ServerPlayer sender = ctx.getSender();
			if (sender == null) return;
			if (!permissions.canEditServerHotKeys(sender)) {
				LOGGER.warn(
				  "Attempt to " + (data != null? "write" : "delete") + " server saved hotkey group " +
				  "\"" + name + "\" by player " + sender.getScoreboardName() + " denied");
				new SSaveRemoteHotKeyGroupPacket(name, "No permission to write server hotkeys").sendTo(sender);
			}
			LOGGER.info(
			  (data != null? "Writing" : "Deleting") + " server saved hotkey group " +
			  "\"" + name + "\" by player " + sender.getScoreboardName());
			final String fileName = name + ".yaml";
			final File file = SimpleConfigPaths.getRemoteHotKeyGroupsDir().resolve(fileName).toFile();
			if (data == null) {
				if (!file.delete()) {
					LOGGER.warn("Failed to delete server saved hotkey group \"" + name + "\"");
					new SSaveRemoteHotKeyGroupPacket(name, "Cannot delete file " + fileName).sendTo(sender);
				}
				LOGGER.info("Successfully deleted server saved hotkey group \"" + name + "\"");
				new SSaveRemoteHotKeyGroupPacket(name).sendTo(sender);
			} else {
				try {
					FileUtils.writeByteArrayToFile(file, data);
					LOGGER.info("Successfully saved server saved hotkey group \"" + name + "\"");
					new SSaveRemoteHotKeyGroupPacket(name).sendTo(sender);
				} catch (IOException e) {
					LOGGER.warn("Error saving server saved hotkey group \"" + name + "\"", e);
					new SSaveRemoteHotKeyGroupPacket(name, e.getLocalizedMessage()).sendTo(sender);
				}
			}
		}
		
		@Override public void write(FriendlyByteBuf buf) {
			buf.writeUtf(name);
			buf.writeBoolean(data != null);
			if (data != null) buf.writeByteArray(data);
		}
		
		@Override public void read(FriendlyByteBuf buf) {
			name = buf.readUtf(32767);
			data = buf.readBoolean()? buf.readByteArray() : null;
		}
	}
	
	protected static class SSaveRemoteHotKeyGroupPacket extends SAbstractPacket {
		private String name;
		private @Nullable String errorMsg;
		
		public SSaveRemoteHotKeyGroupPacket() {}
		public SSaveRemoteHotKeyGroupPacket(String name, @Nullable String errorMsg) {
			this.name = name;
			this.errorMsg = errorMsg;
		}
		public SSaveRemoteHotKeyGroupPacket(String name) {
			this(name, null);
		}
		
		@Override public void onClient(Context ctx) {
			CompletableFuture<Boolean> future = CSaveRemoteHotKeyGroupPacket.FUTURES.get(name);
			if (future != null && !future.isDone()) {
				if (errorMsg != null) {
					future.completeExceptionally(new RemoteException(errorMsg));
				} else future.complete(true);
			}
		}
		
		@Override public void write(FriendlyByteBuf buf) {
			buf.writeUtf(name);
			buf.writeBoolean(errorMsg != null);
			if (errorMsg != null) buf.writeUtf(errorMsg);
		}
		
		@Override public void read(FriendlyByteBuf buf) {
			name = buf.readUtf(32767);
			errorMsg = buf.readBoolean()? buf.readUtf(32767) : null;
		}
	}
	
	@Internal public static CompletableFuture<List<RemoteSavedHotKeyGroup>> getRemoteSavedHotKeyGroups() {
		if (!isConnectedToSimpleConfigServer())
			return CompletableFuture.completedFuture(Collections.emptyList());
		CompletableFuture<List<RemoteSavedHotKeyGroup>> future = new CompletableFuture<>();
		new CSimpleConfigRequestSavedHotKeyGroupsPacket(future).send();
		return future;
	}
	
	@Internal public static CompletableFuture<byte[]> getRemoteSavedHotKeyGroup(String name) {
		if (!isConnectedToSimpleConfigServer())
			return failedFuture(new IllegalStateException("Not connected to SimpleConfig server"));
		CompletableFuture<byte[]> future = new CompletableFuture<>();
		new CRequestSavedHotKeyGroupPacket(name, future).send();
		return future;
	}
	
	// null data implies delete
	@Internal public static CompletableFuture<Boolean> saveRemoteHotKeyGroup(String name, byte @Nullable[] data) {
		if (!isConnectedToSimpleConfigServer())
			return failedFuture(new IllegalStateException("Not connected to SimpleConfig server"));
		CompletableFuture<Boolean> future = new CompletableFuture<>();
		new CSaveRemoteHotKeyGroupPacket(name, data, future).send();
		return future;
	}
	
	public static class SSimpleConfigServerPropertiesPacket extends SAbstractPacket {
		private boolean protectedProperties;
		private byte @Nullable[] data;
		
		public SSimpleConfigServerPropertiesPacket() {}
		public SSimpleConfigServerPropertiesPacket(boolean protectedProperties, byte @Nullable[] data) {
			this.protectedProperties = protectedProperties;
			this.data = data;
		}
		
		@Override public void onClient(Context ctx) {
			SimpleConfigImpl config = SimpleConfigImpl.getConfigOrNull(MINECRAFT_MOD_ID, Type.SERVER);
			CompletableFuture<Pair<Boolean, CommentedConfig>> future =
			  CSimpleConfigServerPropertiesRequestPacket.FUTURE;
			CSimpleConfigServerPropertiesRequestPacket.FUTURE = null;
			if (config != null && data != null) {
				CommentedConfig snapshot = deserializeSnapshot(config, data);
				if (snapshot != null) future.complete(Pair.of(protectedProperties, snapshot));
			}
			if (!future.isDone()) future.completeExceptionally(
			  new SimpleConfigPermissionException("Cannot fetch server properties"));
		}
		
		@Override public void write(FriendlyByteBuf buf) {
			buf.writeBoolean(protectedProperties);
			buf.writeBoolean(data != null);
			if (data != null) buf.writeByteArray(data);
		}
		@Override public void read(FriendlyByteBuf buf) {
			protectedProperties = buf.readBoolean();
			data = buf.readBoolean()? buf.readByteArray() : null;
		}
	}
	
	public static class CSimpleConfigServerPropertiesRequestPacket extends CAbstractPacket {
		private static CompletableFuture<Pair<Boolean, CommentedConfig>> FUTURE;
		
		@Override public void onServer(Context ctx) {
			ServerPlayer sender = ctx.getSender();
			if (sender == null) {
				LOGGER.warn("Received server properties request packet from null sender");
				return;
			}
			boolean dedicated = ServerLifecycleHooks.getCurrentServer().isDedicatedServer();
			if (dedicated && permissions.canAccessServerProperties(sender)) {
				SimpleConfigImpl config = SimpleConfigImpl.getConfigOrNull(MINECRAFT_MOD_ID, Type.SERVER);
				if (config != null) {
					CommentedConfig snapshot = config.takeSnapshot(
					  false, false, p -> p.startsWith("properties."));
					if (!snapshot.isEmpty()) {
						new SSimpleConfigServerPropertiesPacket(
						  MinecraftServerConfigWrapper.areProtectedPropertiesEditable(),
						  serializeSnapshot(config, snapshot)
						).sendTo(sender);
						return;
					} else LOGGER.warn("No server properties to serialize");
				} else LOGGER.warn(
				  "Could not serialize server properties because Minecraft config wrapper is null");
			} else if (dedicated) LOGGER.info(
			  "Server properties not sent to player " + sender.getScoreboardName() + " as " +
			  "they don't have permission to access them.");
			new SSimpleConfigServerPropertiesPacket(false, null).sendTo(sender);
		}
		
		@Override public void write(FriendlyByteBuf buf) {}
		@Override public void read(FriendlyByteBuf buf) {}
	}
	
	@Internal public static CompletableFuture<Pair<Boolean, CommentedConfig>> requestServerProperties() {
		if (!isConnectedToSimpleConfigServer())
			return failedFuture(new IllegalStateException("Not connected to SimpleConfig server"));
		CompletableFuture<Pair<Boolean, CommentedConfig>> future = new CompletableFuture<>();
		CSimpleConfigServerPropertiesRequestPacket.FUTURE = future;
		new CSimpleConfigServerPropertiesRequestPacket().send();
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
	
	public static class SimpleConfigPermissionException extends RuntimeException {
		public SimpleConfigPermissionException(String message) {
			super(message);
		}
		public SimpleConfigPermissionException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
