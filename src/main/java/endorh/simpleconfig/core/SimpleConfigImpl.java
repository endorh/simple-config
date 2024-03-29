package endorh.simpleconfig.core;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingException;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import endorh.simpleconfig.api.ConfigBuilderFactoryProxy;
import endorh.simpleconfig.api.SimpleConfig;
import endorh.simpleconfig.api.SimpleConfigCategory;
import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.config.ServerConfig.permissions;
import endorh.simpleconfig.core.BackingField.BackingFieldBuilder;
import endorh.simpleconfig.core.SimpleConfigNetworkHandler.CSimpleConfigSyncPacket;
import endorh.simpleconfig.core.SimpleConfigNetworkHandler.SSimpleConfigServerCommonConfigPacket;
import endorh.simpleconfig.core.SimpleConfigNetworkHandler.SSimpleConfigSyncPacket;
import endorh.simpleconfig.ui.api.ConfigCategoryBuilder;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.api.ConfigScreenBuilder;
import endorh.simpleconfig.ui.api.ConfigScreenBuilder.IConfigSnapshotHandler;
import endorh.simpleconfig.ui.gui.AbstractConfigScreen;
import endorh.simpleconfig.yaml.NodeComments;
import endorh.simpleconfig.yaml.SimpleConfigCommentedYamlFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.comments.CommentLine;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static endorh.simpleconfig.api.SimpleConfigTextUtil.splitTtc;
import static endorh.simpleconfig.core.SimpleConfigPaths.LOCAL_PRESETS_DIR;
import static endorh.simpleconfig.core.SimpleConfigSnapshotHandler.failedFuture;
import static endorh.simpleconfig.yaml.SimpleConfigCommentedYamlWriter.commentLine;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

/**
 * Simple config.<br>
 * Create and register your config with {@link ConfigBuilderFactoryProxy#config(String, Type)}
 * or {@link ConfigBuilderFactoryProxy#config(String, Type, Class)}
 */
public class SimpleConfigImpl extends AbstractSimpleConfigEntryHolder implements SimpleConfig {
	private static final Map<Pair<String, Type>, SimpleConfigImpl> INSTANCES = new ConcurrentHashMap<>();
	@Internal public static final SimpleConfigImpl DUMMY = new SimpleConfigImpl(
	  "", Type.SERVER, "dummy", null, null, null);
	static {
		INSTANCES.remove(Pair.of("", Type.SERVER));
	}
	
	private static final Pattern LINE_BREAK = Pattern.compile("\\R");

	protected final String defaultTitle;
	protected final String tooltip;
	protected final @Nullable Consumer<SimpleConfigImpl> saver;
	protected final @Nullable Consumer<SimpleConfig> baker;
	protected final @Nullable Object configClass;
	private final Type type;
	private final String modId;
	protected Icon defaultCategoryIcon;
	protected int defaultCategoryColor;
	/**
	 * Should not be modified
	 */
	protected Map<String, SimpleConfigCategoryImpl> categories = null;
	/**
	 * Should not be modified
	 */
	protected Map<String, SimpleConfigGroupImpl> groups = null;
	/**
	 * Order used in the config screen
	 */
	protected List<IGUIEntry> order;
	@Nullable protected ForgeConfigSpec spec;
	@Nullable protected List<ForgeConfigSpec> extraSpecs;
	@OnlyIn(Dist.CLIENT) protected @Nullable BiConsumer<SimpleConfig, ConfigScreenBuilder> decorator;
	protected @Nullable Predicate<SimpleConfigCategory> categoryFilter;
	protected @Nullable ResourceLocation background;
	protected boolean transparent;
	@OnlyIn(Dist.CLIENT) protected @Nullable AbstractConfigScreen gui;
	protected @Nullable IConfigSnapshotHandler snapshotHandler;
	protected Set<Player> remoteListeners = new HashSet<>();
	private ModConfig modConfig;
	private Map<String, ModConfig> extraModConfigs;
	private ModContainer modContainer;
	private @Nullable LiteralArgumentBuilder<CommandSourceStack> commandRoot;
	private Map<String, NodeComments> comments = new HashMap<>();
	private final SimpleConfigCommentedYamlFormat configFormat = SimpleConfigCommentedYamlFormat.forConfig(this);
	
	@Internal protected SimpleConfigImpl(
	  String modId, Type type, String defaultTitle,
	  @Nullable Consumer<SimpleConfig> baker, @Nullable Consumer<SimpleConfigImpl> saver,
	  @Nullable Object configClass
	) {
		this.modId = modId;
		this.type = type;
		this.defaultTitle = defaultTitle;
		this.baker = baker;
		this.saver = saver;
		this.configClass = configClass;
		tooltip = defaultTitle + ":help";
		root = this;
		
		Pair<String, Type> key = Pair.of(modId, type);
		if (INSTANCES.put(key, this) != null) throw new IllegalStateException(
		  "Cannot create more than one config per type per mod");
	}
	
	@Internal public static SimpleConfigImpl getConfigOrNull(String modId, Type type) {
		return INSTANCES.get(Pair.of(modId, type));
	}
	
	@Internal public static SimpleConfigImpl getConfig(String modId, Type type) {
		Pair<String, Type> key = Pair.of(modId, type);
		if (!INSTANCES.containsKey(key)) throw new IllegalStateException(
		  "Attempted to get unregistered config for mod id \"" + modId + "\" of type " + type);
		return INSTANCES.get(key);
	}
	
	@Internal public static boolean hasConfig(String modId, Type type) {
		return INSTANCES.containsKey(Pair.of(modId, type));
	}
	
	@Internal public static Set<String> getConfigModIds() {
		return INSTANCES.keySet().stream().map(Pair::getLeft).collect(Collectors.toSet());
	}
	
	@Internal public static Collection<SimpleConfigImpl> getAllConfigs() {
		return INSTANCES.values();
	}
	
	/**
	 * Get the display name of the mod, or just its mod id if not found
	 */
	@Internal public static String getModNameOrId(String modId) {
		final Optional<IModInfo> first = ModList.get().getMods().stream()
		  .filter(m -> modId.equals(m.getModId())).findFirst();
		if (first.isPresent())
			return first.get().getDisplayName();
		return modId;
	}
	
	@Internal public static void updateAllFileTranslations() {
		INSTANCES.values().stream().filter(
		  c -> c.modConfig != null
		       && c.modConfig.getConfigData() != null
		       && !c.isWrapper()
		).forEach(c -> c.spec.save());
	}
	
	@Override public boolean isWrapper() {
		return !(modConfig instanceof SimpleConfigModConfig) || !extraModConfigs.isEmpty();
	}
	
	@Override public Optional<Path> getFilePath() {
		ModConfig modConfig = getModConfig();
		return modConfig != null && modConfig.getConfigData() instanceof CommentedFileConfig
		       ? Optional.of(modConfig.getFullPath()) : Optional.empty();
	}
	
	@Override public Optional<Path> getFilePath(String category) {
		ModConfig config = extraModConfigs.get(category);
		if (config == null) return getFilePath();
		return config.getConfigData() instanceof CommentedFileConfig
		       ? Optional.of(config.getFullPath()) : Optional.empty();
	}
	
	/**
	 * Set up the config
	 */
	@Internal protected void build(
	  Map<String, AbstractConfigEntry<?, ?, ?>> entries,
	  Map<String, SimpleConfigCategoryImpl> categories,
	  Map<String, SimpleConfigGroupImpl> groups,
	  List<IGUIEntry> order, @Nullable Pair<ForgeConfigSpec, List<ForgeConfigSpec>> spec,
	  Icon icon, int color, @Nullable LiteralArgumentBuilder<CommandSourceStack> commandRoot
	) {
		if (this.entries != null)
			throw new IllegalStateException("Called buildEntry() twice");
		this.entries = entries;
		this.categories = categories;
		this.groups = groups;
		this.order = order;
		this.spec = spec != null? spec.getLeft() : null;
		extraSpecs = spec != null? spec.getRight() : null;
		final Map<String, AbstractSimpleConfigEntryHolder> children = new HashMap<>();
		children.putAll(this.categories);
		children.putAll(this.groups);
		this.children = unmodifiableMap(children);
		defaultCategoryIcon = icon;
		defaultCategoryColor = color;
		this.commandRoot = commandRoot;
	}
	
	@Internal public void build(ModContainer container, ModConfig modConfig) {
		build(container, modConfig, emptyMap());
	}
	
	@Internal public void build(ModContainer container, ModConfig modConfig, Map<String, ModConfig> extraConfigs) {
		modContainer = container;
		this.modConfig = modConfig;
		this.extraModConfigs = extraConfigs;
	}
	
	/**
	 * Config relative path, for error reporting.
	 */
	@Override public String getPath() {
		return getName();
	}
	
	@Override protected String getName() {
		return "SimpleConfig[" + getModId() + ", " + getType().name() + "]";
	}
	
	@Override @Internal public String getFileName() {
		return String.format("%s-%s.yaml", getModId(), getType().getAlias());
	}
	
	@Override
	public @Nullable <T, C, Gui> AbstractConfigEntry<T, C, Gui> getEntryOrNull(String path) {
		if (path.startsWith(".")) path = path.substring(1);
		AbstractConfigEntry<?, ?, ?> entry = entries.get(path);
		if (entry == null) entry = getSubEntry(path);
		//noinspection unchecked
		return (AbstractConfigEntry<T, C, Gui>) entry;
	}
	
	@Override public @Nullable AbstractSimpleConfigEntryHolder getChildOrNull(String path) {
		String[] split = DOT.split(path, 2);
		if (split[0].isEmpty()) return super.getChildOrNull(split[1]);
		return super.getChildOrNull(path);
	}
	
	@Override protected <T> AbstractConfigEntry<T, ?, ?> getSubEntry(String path) {
		String[] split = DOT.split(path, 2);
		if (split[0].isEmpty()) {
			AbstractConfigEntry<?, ?, ?> entry = entries.get(split[1]);
			if (entry != null) //noinspection unchecked
				return (AbstractConfigEntry<T, ?, ?>) entry;
			return super.getSubEntry(split[1]);
		}
		return super.getSubEntry(path);
	}
	
	/**
	 * Bakes all the backing fields<br>
	 */
	protected void bakeFields() {
		for (SimpleConfigCategoryImpl cat : categories.values())
			cat.bakeFields();
		for (SimpleConfigGroupImpl group : groups.values())
			group.bakeFields();
		for (AbstractConfigEntry<?, ?, ?> entry : entries.values())
			entry.bakeField();
	}
	
	/**
	 * Commits any changes in the backing fields to the actual config file.
	 * Entries with non-readable backing fields are ignored.
	 * (see {@link BackingFieldBuilder#withCommitter}
	 * @throws InvalidConfigValueException if the current value of the field is invalid.
	 */
	@Override public void commitFields() {
		try {
			for (SimpleConfigCategoryImpl cat : categories.values())
				cat.commitFields();
			for (SimpleConfigGroupImpl group : groups.values())
				group.commitFields();
			for (AbstractConfigEntry<?, ?, ?> entry : entries.values())
				entry.commitField();
		} catch (IllegalAccessException e) {
			throw new ConfigReflectiveOperationException(
			  "Could not access mod config field during config commit\n  Details: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Bake the fields and run the bakers
	 */
	@Override public void bake() {
		bakeFields();
		for (SimpleConfigCategoryImpl cat : categories.values())
			cat.bake();
		for (SimpleConfigGroupImpl group : groups.values())
			group.bake();
		if (baker != null)
			baker.accept(this);
	}
	
	@OnlyIn(Dist.CLIENT) @Override protected void removeGUI() {
		// Synchronization is needed, because the file watcher thread may trigger
		// a reload, which could start loading external changes using the snapshot
		// handler, while some entries get their GUI references removed
		synchronized (this) {
			gui = null;
			snapshotHandler = null;
		}
		super.removeGUI();
	}
	
	@OnlyIn(Dist.CLIENT) protected void setGUI(
	  AbstractConfigScreen gui, @Nullable IConfigSnapshotHandler handler
	) {
		this.gui = gui;
		snapshotHandler = handler;
	}
	
	protected @Nullable IConfigSnapshotHandler getSnapshotHandler() {
		return snapshotHandler;
	}
	
	/**
	 * Bake the config and save it, which performs different actions
	 * depending on the type of the config.
	 */
	public void save() {
		if (!canEdit()) return;
		bake();
		if (saver != null)
			saver.accept(this);
		markDirty(false);
	}
	
	/**
	 * Notify the user if they're in a world and just
	 * changed entries flagged as requiring a restart
	 */
	@OnlyIn(Dist.CLIENT)
	protected void checkRestart() {
		if (anyDirtyRequiresRestart()) {
			final LocalPlayer player = Minecraft.getInstance().player;
			if (player != null) {
				player.sendSystemMessage(
				  Component.translatable("simpleconfig.config.msg.client_changes_require_restart")
					 .withStyle(ChatFormatting.GOLD));
			}
		}
	}
	
	@Internal public void sync() {
		if (type == Type.SERVER) {
			if (FMLEnvironment.dist == Dist.CLIENT) {
				syncToServer();
			} else syncToClients();
		}
	}
	
	@Internal public void update() {
		if (spec != null) spec.save();
		sync();
	}
	
	@Internal public void syncToClients() {
		if (type == Type.SERVER) {
			new SSimpleConfigSyncPacket(this).sendToAll();
		} else if (type == Type.COMMON) {
			new SSimpleConfigServerCommonConfigPacket(this).sendTo(remoteListeners);
		}
	}
	
	@Internal public void syncToServer() {
		if (SimpleConfigNetworkHandler.isConnectedToSimpleConfigServer())
			new CSimpleConfigSyncPacket(this).send();
	}
	
	@Internal protected void addRemoteListener(Player listener) {
		remoteListeners.add(listener);
	}
	
	@Internal protected void removeRemoteListener(Player listener) {
		remoteListeners.remove(listener);
	}
	
	@Internal public CommentedConfig takeSnapshot(boolean fromGUI, boolean fromRemote) {
		return takeSnapshot(fromGUI, fromRemote, (Predicate<String>) null);
	}
	
	@Internal public CommentedConfig takeSnapshot(
	  boolean fromGUI, boolean fromRemote, @Nullable Set<String> selectedPaths
	) {
		if (selectedPaths != null) selectedPaths = selectedPaths.stream()
		  .map(p -> p.startsWith(".")? p.substring(1) : p)
		  .collect(Collectors.toSet());
		return takeSnapshot(fromGUI, fromRemote, selectedPaths != null? selectedPaths::contains : null);
	}
	
	@Internal public CommentedConfig takeSnapshot(
	  boolean fromGUI, boolean fromRemote, @Nullable Predicate<String> selectedPaths
	) {
		final CommentedConfig config = CommentedConfig.of(LinkedHashMap::new, SimpleConfigCommentedYamlFormat.forConfig(this));
		saveSnapshot(config, fromGUI, fromRemote, selectedPaths);
		return config;
	}
	
	@Internal public void loadSnapshot(CommentedConfig config, boolean intoGUI, boolean forRemote) {
		loadSnapshot(config, intoGUI, forRemote, (Predicate<String>) null);
	}
	
	@Internal public void loadSnapshot(
	  CommentedConfig config, boolean intoGUI, boolean forRemote, @Nullable Set<String> selectedPaths
	) {
		loadSnapshot(
		  config, intoGUI, forRemote, selectedPaths != null? selectedPaths::contains : null);
	}
	
	@Internal @Override public void loadSnapshot(
	  CommentedConfig config, boolean intoGUI, boolean forRemote,
	  @Nullable Predicate<String> selectedPaths
	) {
		loadSnapshot(config, intoGUI, forRemote, false, selectedPaths);
	}
	
	@Internal public void loadSnapshot(
	  CommentedConfig config, boolean intoGUI, boolean forRemote,
	  boolean opaque, @Nullable Predicate<String> selectedPaths
	) {
		if (intoGUI) {
			if (FMLEnvironment.dist != Dist.CLIENT) throw new IllegalStateException(
			  "Cannot load snapshot into GUI on server");
			AbstractConfigScreen screen = getGUI();
			if (screen != null) {
				if (opaque) {
					screen.runUnrecordedAction(
					  () -> super.loadSnapshot(config, true, forRemote, selectedPaths));
				} else screen.runAtomicTransparentAction(
				  () -> super.loadSnapshot(config, true, forRemote, selectedPaths));
			} else throw new IllegalStateException(
			  "Cannot load snapshot into GUI when no GUI is active");
		} else super.loadSnapshot(config, false, forRemote, selectedPaths);
	}
	
	/**
	 * Handle external config modification events
	 */
	@SubscribeEvent
	protected void onModConfigEvent(final ModConfigEvent event) {
		if (event instanceof ModConfigEvent.Unloading) return;
		final ModConfig c = event.getConfig();
		if (c == getModConfig() || getExtraModConfigs().containsValue(c)) {
			bake();
			if (type == Type.SERVER || type == Type.COMMON)
				DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, () -> this::syncToClients);
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
				synchronized (this) {
					IConfigSnapshotHandler handler = getSnapshotHandler();
					if (handler != null) handler.notifyExternalChanges(this);
				}
			});
		}
	}
	
	@Override public @NotNull SimpleConfigCategoryImpl getCategory(String name) {
		if (!categories.containsKey(name))
			throw new NoSuchConfigCategoryError(getPath() + "." + name);
		return categories.get(name);
	}
	
	@Override public @NotNull SimpleConfigGroupImpl getGroup(String path) {
		if (path.contains(".")) {
			final String[] split = path.split("\\.", 2);
			if (groups.containsKey(split[0]))
				return groups.get(split[1]).getGroup(split[1]);
		} else if (groups.containsKey(path))
			return groups.get(path);
		throw new NoSuchConfigGroupError(path);
	}
	
	protected Component getTitle() {
		if (I18n.exists(defaultTitle))
			return Component.translatable(defaultTitle);
		return Component.translatable("simpleconfig.config.category." + getType().name().toLowerCase());
	}
	
	protected String getHeaderComment() {
		String type = getType().getAlias();
		type = Character.toUpperCase(type.charAt(0)) + type.substring(1);
		boolean loaded = ServerI18n.hasKey("simpleconfig.config.title");
		String lang = loaded? ServerI18n.getCurrentLanguage() : "<not loaded yet...>";
		String comment =
		  loaded? "\n" + ServerI18n.format("simpleconfig.config.header")
		        : "\nComments starting with 2 hash symbols (##) are documentation comments and will be reset if modified.";
		return getModNameOrId(getModId()) + " - " + type + " config\nLang: " + lang + comment;
	}
	
	@Internal public Map<String, NodeComments> getComments() {
		updateComments(comments);
		String headerComment = getHeaderComment().trim();
		List<CommentLine> headerLines = Arrays.stream(LINE_BREAK.split(headerComment))
		  .map(l -> commentLine("# " + l))
		  .collect(Collectors.toList());
		comments.put("", Util.make(new NodeComments(), c -> c.setBlockComments(headerLines)));
		return comments;
	}
	
	@Internal public void loadComments(Map<String, NodeComments> comments) {
		this.comments = comments;
	}
	
	protected ConfigSpec buildConfigSpec() {
		final ConfigSpec spec = new ConfigSpec();
		for (AbstractConfigEntry<?, ?, ?> e : entries.values())
			e.buildSpec(spec, "");
		for (AbstractSimpleConfigEntryHolder child : children.values())
			child.buildConfigSpec(spec, "");
		return spec;
	}
	
	@Override public boolean canEdit() {
		return getType() != Type.SERVER
		       || FMLEnvironment.dist == Dist.DEDICATED_SERVER
		       || permissions.permissionFor(modId).getLeft().canEdit();
	}
	
	@OnlyIn(Dist.CLIENT)
	protected void buildGUI(ConfigScreenBuilder configBuilder, boolean forRemote) {
		if (background != null)
			configBuilder.setDefaultBackgroundTexture(background);
		configBuilder.setTransparentBackground(transparent);
		ConfigFieldBuilder entryBuilder = configBuilder.entryBuilder();
		if (!order.isEmpty()) {
			final ConfigCategoryBuilder category = configBuilder.getOrCreateCategory(
			  "", type.asEditType(forRemote));
			category.setEditable(canEdit());
			category.setTitle(getTitle());
			getFilePath().ifPresent(category::setContainingFile);
			category.setDescription(
			  () -> I18n.exists(tooltip)
			        ? Optional.of(splitTtc(tooltip).toArray(new Component[0]))
			        : Optional.empty());
			if (background != null)
				category.setBackground(background);
			category.setIcon(defaultCategoryIcon);
			category.setColor(defaultCategoryColor);
			for (IGUIEntry entry : order)
				entry.buildGUI(category, entryBuilder, forRemote);
		}
		for (SimpleConfigCategoryImpl cat : categories.values())
			if (categoryFilter == null || categoryFilter.test(cat))
				cat.buildGUI(configBuilder, entryBuilder, forRemote);
		if (decorator != null)
			decorator.accept(this, configBuilder);
	}
	
	// null config implies deletion
	protected CompletableFuture<Void> saveLocalPreset(String name, @Nullable CommentedConfig config) {
		final String typePrefix = "-" + getType().getAlias() + "-";
		final String fileName = getModId() + typePrefix + name + ".yaml";
		final File dest = LOCAL_PRESETS_DIR.resolve(fileName).toFile();
		if (dest.isDirectory())
			return failedFuture(new FileNotFoundException(dest.getPath()));
		if (config != null) {
			SimpleConfigCommentedYamlFormat format = getConfigFormat();
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				format.createWriter(false).write(config, os);
			} catch (WritingException e) {
				return failedFuture(e);
			}
			final byte[] bytes = os.toByteArray();
			try {
				FileUtils.writeByteArrayToFile(dest, bytes);
				return CompletableFuture.completedFuture(null);
			} catch (IOException e) {
				return failedFuture(e);
			}
		} else {
			if (!dest.isFile())
				return failedFuture(new FileNotFoundException(dest.getPath()));
			if (!dest.delete())
				return failedFuture(new IOException("Could not delete file " + dest.getPath()));
			return CompletableFuture.completedFuture(null);
		}
	}
	
	protected CompletableFuture<Void> saveRemotePreset(String name, CommentedConfig config) {
		return SimpleConfigNetworkHandler.saveRemotePreset(getModId(), type, name, config);
	}
	
	protected CompletableFuture<CommentedConfig> getLocalPreset(String name) {
		final CompletableFuture<CommentedConfig> future = new CompletableFuture<>();
		final String prefix = "-" + getType().getAlias() + "-";
		final File file = LOCAL_PRESETS_DIR.resolve(getModId() + prefix + name + ".yaml").toFile();
		if (!file.isFile()) {
			future.completeExceptionally(new FileNotFoundException(file.getPath()));
			return future;
		}
		try {
			SimpleConfigCommentedYamlFormat format = getConfigFormat();
			final CommentedConfig config = format.createParser(false)
			  .parse(new ByteArrayInputStream(FileUtils.readFileToByteArray(file)));
			future.complete(config);
			return future;
		} catch (IOException e) {
			future.completeExceptionally(e);
			return future;
		}
	}
	
	protected CompletableFuture<CommentedConfig> getRemotePreset(String name) {
		return SimpleConfigNetworkHandler.requestRemotePreset(getModId(), type, name);
	}
	
	@Override public Type getType() {
		return type;
	}
	
	@Override public String getModId() {
		return modId;
	}
	
	@Override public String getModName() {
		return getModNameOrId(modId);
	}
	
	@Internal public SimpleConfigCommentedYamlFormat getConfigFormat() {
		return configFormat;
	}
	
	@Internal public ModConfig getModConfig() {
		return modConfig;
	}
	
	@Internal public Map<String, ModConfig> getExtraModConfigs() {
		return extraModConfigs;
	}
	
	@Internal public ModContainer getModContainer() {
		return modContainer;
	}
	
	@Internal public @Nullable LiteralArgumentBuilder<CommandSourceStack> getCommandRoot() {
		return commandRoot;
	}
	
	@Override public @NotNull AbstractSimpleConfigEntryHolder getParent() {
		throw new NoSuchConfigGroupError(getGlobalPath());
	}
	
	public interface IGUIEntry {
		@Internal void buildGUI(ConfigCategoryBuilder category, ConfigFieldBuilder entryBuilder, boolean forRemote);
	}
}
