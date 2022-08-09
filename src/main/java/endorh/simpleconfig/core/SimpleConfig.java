package endorh.simpleconfig.core;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingException;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import endorh.simpleconfig.SimpleConfigMod.ServerConfig.permissions;
import endorh.simpleconfig.core.BackingField.BackingFieldBuilder;
import endorh.simpleconfig.core.SimpleConfigBuilder.CategoryBuilder;
import endorh.simpleconfig.core.SimpleConfigBuilder.GroupBuilder;
import endorh.simpleconfig.core.SimpleConfigNetworkHandler.CSimpleConfigSyncPacket;
import endorh.simpleconfig.core.SimpleConfigNetworkHandler.SSimpleConfigServerCommonConfigPacket;
import endorh.simpleconfig.core.SimpleConfigNetworkHandler.SSimpleConfigSyncPacket;
import endorh.simpleconfig.core.entry.Builders;
import endorh.simpleconfig.ui.ConfigCategoryBuilder;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.api.ConfigScreenBuilder;
import endorh.simpleconfig.ui.gui.AbstractConfigScreen;
import endorh.simpleconfig.ui.gui.Icon;
import endorh.simpleconfig.yaml.NodeComments;
import endorh.simpleconfig.yaml.SimpleConfigCommentedYamlFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.resources.I18n;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.comments.CommentLine;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static endorh.simpleconfig.core.SimpleConfigPaths.LOCAL_PRESETS_DIR;
import static endorh.simpleconfig.core.SimpleConfigSnapshotHandler.failedFuture;
import static endorh.simpleconfig.core.SimpleConfigTextUtil.splitTtc;
import static endorh.simpleconfig.yaml.SimpleConfigCommentedYamlWriter.commentLine;
import static java.util.Collections.synchronizedMap;
import static java.util.Collections.unmodifiableMap;

/**
 * Simple config.<br>
 * Create and register your config with {@link SimpleConfig#builder(String, Type)}
 * or {@link SimpleConfig#builder(String, Type, Class)}
 */
public class SimpleConfig extends AbstractSimpleConfigEntryHolder {
	private static final Map<Pair<String, Type>, SimpleConfig> INSTANCES =
	  synchronizedMap(new HashMap<>());
	private static final Pattern LINE_BREAK = Pattern.compile("\\R");
	private static int TEXT_ENTRY_ID_GEN = 0;

	static {
		SimpleConfigNetworkHandler.registerPackets();
		// Trigger class loading
		Builders.bool(true);
	}

	protected final String defaultTitle;
	protected final String tooltip;
	protected final @Nullable Consumer<SimpleConfig> saver;
	protected final @Nullable Consumer<SimpleConfig> baker;
	protected final @Nullable Object configClass;
	private final Type type;
	private final String modId;
	protected Icon defaultCategoryIcon;
	protected int defaultCategoryColor;
	/**
	 * Should not be modified
	 */
	protected Map<String, SimpleConfigCategory> categories = null;
	/**
	 * Should not be modified
	 */
	protected Map<String, SimpleConfigGroup> groups = null;
	/**
	 * Order used in the config screen
	 */
	protected List<IGUIEntry> order;
	protected ForgeConfigSpec spec;
	@OnlyIn(Dist.CLIENT) protected @Nullable BiConsumer<SimpleConfig, ConfigScreenBuilder> decorator;
	protected @Nullable ResourceLocation background;
	protected boolean transparent;
	@OnlyIn(Dist.CLIENT) protected @Nullable AbstractConfigScreen gui;
	protected @Nullable SimpleConfigSnapshotHandler snapshotHandler;
	protected Set<PlayerEntity> remoteListeners = new HashSet<>();
	private ModConfig modConfig;
	private ModContainer modContainer;
	private @Nullable LiteralArgumentBuilder<CommandSource> commandRoot;
	private Map<String, NodeComments> comments = new HashMap<>();
	private final SimpleConfigCommentedYamlFormat configFormat = SimpleConfigCommentedYamlFormat.forConfig(this);
	
	@Internal protected SimpleConfig(
	  String modId, Type type, String defaultTitle,
	  @Nullable Consumer<SimpleConfig> baker, @Nullable Consumer<SimpleConfig> saver,
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
		synchronized (INSTANCES) {
			if (!INSTANCES.containsKey(key))
				INSTANCES.put(key, this);
			else throw new IllegalStateException(
			  "Cannot create more than one config per type per mod");
		}
	}
	
	@Internal public static SimpleConfig getConfigOrNull(String modId, Type type) {
		return INSTANCES.get(Pair.of(modId, type));
	}
	
	@Internal public static SimpleConfig getConfig(String modId, Type type) {
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
	
	@Internal public static Collection<SimpleConfig> getAllConfigs() {
		return INSTANCES.values();
	}
	
	protected static String nextTextID() {
		return "_text$" + TEXT_ENTRY_ID_GEN++;
	}
	
	/**
	 * Create a {@link SimpleConfig} builder<br>
	 * Add entries with {@link SimpleConfigBuilder#add(String, AbstractConfigEntryBuilder)}<br>
	 * Add categories and groups with {@link SimpleConfigBuilder#n(CategoryBuilder)}
	 * and {@link SimpleConfigBuilder#n(GroupBuilder)}<br>
	 * Complete the config by calling {@link SimpleConfigBuilder#buildAndRegister()}<br>
	 *
	 * @param modId Your mod id
	 * @param type  A {@link Type}, usually either CLIENT or SERVER
	 */
	public static SimpleConfigBuilder builder(String modId, Type type) {
		return new SimpleConfigBuilder(modId, type);
	}
	
	/**
	 * Create a {@link SimpleConfig} builder<br>
	 * Add entries with {@link SimpleConfigBuilder#add(String, AbstractConfigEntryBuilder)}<br>
	 * Add categories and groups with {@link SimpleConfigBuilder#n(CategoryBuilder)}
	 * and {@link SimpleConfigBuilder#n(GroupBuilder)}<br>
	 * Complete the config by calling {@link SimpleConfigBuilder#buildAndRegister()}<br>
	 *
	 * @param modId       Your mod id
	 * @param type        A {@link Type}, usually either CLIENT or SERVER
	 * @param configClass Backing class for the config. It will be parsed
	 *                    for static backing fields and config annotations
	 */
	public static SimpleConfigBuilder builder(
	  String modId, Type type, Class<?> configClass
	) {
		return new SimpleConfigBuilder(modId, type, configClass);
	}
	
	/**
	 * Create a config group
	 *
	 * @param name Group name, suitable for the config file (without spaces)
	 */
	public static GroupBuilder group(String name) {
		return group(name, false);
	}
	
	/**
	 * Create a config group
	 *
	 * @param name   Group name, suitable for the config file (without spaces)
	 * @param expand Whether to expand this group in the GUI automatically (default: no)
	 */
	public static GroupBuilder group(String name, boolean expand) {
		return new GroupBuilder(name, expand);
	}
	
	/**
	 * Create a config category
	 *
	 * @param name Category name, suitable for the config file (without spaces)
	 */
	public static CategoryBuilder category(String name) {
		return new CategoryBuilder(name);
	}
	
	/**
	 * Create a config category
	 *
	 * @param name        Category name, suitable for the config file (without spaces)
	 * @param configClass Backing class for the category, which will be parsed
	 *                    for static backing fields and config annotations
	 */
	public static CategoryBuilder category(String name, Class<?> configClass) {
		return new CategoryBuilder(name, configClass);
	}
	
	/**
	 * Get the display name of the mod, or just its mod id if not found
	 */
	@Internal public static String getModNameOrId(String modId) {
		final Optional<ModInfo> first = ModList.get().getMods().stream()
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
	
	public boolean isWrapper() {
		return !(modConfig instanceof SimpleConfigModConfig);
	}
	
	/**
	 * Retrieve the actual path of this file, if found
	 */
	public Optional<Path> getFilePath() {
		ModConfig modConfig = getModConfig();
		return modConfig.getConfigData() instanceof CommentedFileConfig? Optional.of(
		  modConfig.getFullPath()) : Optional.empty();
	}
	
	/**
	 * Setup the config
	 */
	@Internal protected void build(
	  Map<String, AbstractConfigEntry<?, ?, ?>> entries,
	  Map<String, SimpleConfigCategory> categories,
	  Map<String, SimpleConfigGroup> groups,
	  List<IGUIEntry> order, ForgeConfigSpec spec,
	  Icon icon, int color, @Nullable LiteralArgumentBuilder<CommandSource> commandRoot
	) {
		if (this.entries != null)
			throw new IllegalStateException("Called buildEntry() twice");
		this.entries = entries;
		this.categories = categories;
		this.groups = groups;
		this.order = order;
		this.spec = spec;
		final Map<String, AbstractSimpleConfigEntryHolder> children = new HashMap<>();
		children.putAll(this.categories);
		children.putAll(this.groups);
		this.children = unmodifiableMap(children);
		defaultCategoryIcon = icon;
		defaultCategoryColor = color;
		this.commandRoot = commandRoot;
	}
	
	@Internal protected void build(ModContainer container, ModConfig modConfig) {
		this.modContainer = container;
		this.modConfig = modConfig;
	}
	
	/**
	 * Config relative path, for error reporting.
	 */
	@Override protected String getPath() {
		return getName();
	}
	
	@Override protected String getName() {
		return "SimpleConfig[" + getModId() + ", " + getType().name() + "]";
	}
	
	@Internal public String getFileName() {
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
		for (SimpleConfigCategory cat : categories.values())
			cat.bakeFields();
		for (SimpleConfigGroup group : groups.values())
			group.bakeFields();
		for (AbstractConfigEntry<?, ?, ?> entry : entries.values())
			entry.bakeField();
	}
	
	/**
	 * Commits any changes in the backing fields to the actual config file.
	 * Entries with non-readable backing fields are ignored.
	 * (see {@link BackingFieldBuilder#withCommitter}
	 * @throws InvalidConfigValueException if the current value of the a field is invalid.
	 */
	@Override public void commitFields() {
		try {
			for (SimpleConfigCategory cat : categories.values())
				cat.commitFields();
			for (SimpleConfigGroup group : groups.values())
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
		for (SimpleConfigCategory cat : categories.values())
			cat.bake();
		for (SimpleConfigGroup group : groups.values())
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
	  AbstractConfigScreen gui, @Nullable SimpleConfigSnapshotHandler handler
	) {
		this.gui = gui;
		snapshotHandler = handler;
	}
	
	protected @Nullable SimpleConfigSnapshotHandler getSnapshotHandler() {
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
			final ClientPlayerEntity player = Minecraft.getInstance().player;
			if (player != null) {
				player.sendMessage(new TranslationTextComponent(
				  "simpleconfig.config.msg.client_changes_require_restart"
				).mergeStyle(TextFormatting.GOLD), Util.DUMMY_UUID);
			}
		}
	}
	
	@Internal public void sync() {
		if (FMLEnvironment.dist == Dist.CLIENT) {
			syncToServer();
		} else syncToClients();
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
	
	@Internal protected void addRemoteListener(PlayerEntity listener) {
		remoteListeners.add(listener);
	}
	
	@Internal protected void removeRemoteListener(PlayerEntity listener) {
		remoteListeners.remove(listener);
	}
	
	@Internal public CommentedConfig takeSnapshot(
	  boolean fromGUI, boolean fromRemote, @Nullable Set<String> selectedPaths
	) {
		if (selectedPaths != null) selectedPaths = selectedPaths.stream()
		  .map(p -> p.startsWith(".")? p.substring(1) : p)
		  .collect(Collectors.toSet());
		final CommentedConfig config = CommentedConfig.of(LinkedHashMap::new, SimpleConfigCommentedYamlFormat.forConfig(this));
		saveSnapshot(config, fromGUI, fromRemote, selectedPaths);
		return config;
	}
	
	@Internal @Override public void loadSnapshot(
	  CommentedConfig config, boolean intoGUI, boolean forRemote, @Nullable Set<String> selectedPaths
	) {
		if (intoGUI) {
			if (FMLEnvironment.dist != Dist.CLIENT) throw new IllegalStateException(
			  "Cannot load snapshot into GUI on server");
			AbstractConfigScreen screen = getGUI();
			if (screen != null) {
				screen.runAtomicTransparentAction(
				  () -> super.loadSnapshot(config, true, forRemote, selectedPaths));
			} else throw new IllegalStateException(
			  "Cannot load snapshot into GUI when no GUI is active");
		} else super.loadSnapshot(config, false, forRemote, selectedPaths);
	}
	
	/**
	 * Handle external config modification events
	 */
	@SubscribeEvent
	protected void onModConfigEvent(final ModConfig.ModConfigEvent event) {
		final ModConfig c = event.getConfig();
		if (c == getModConfig()) {
			bake();
			if (type == Type.SERVER || type == Type.COMMON)
				DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, () -> this::syncToClients);
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
				synchronized (this) {
					SimpleConfigSnapshotHandler handler = getSnapshotHandler();
					if (handler != null) handler.notifyExternalChanges(this);
				}
			});
		}
	}
	
	/**
	 * Get a config category
	 *
	 * @param name Name of the category
	 * @throws NoSuchConfigCategoryError if the category is not found
	 */
	public SimpleConfigCategory getCategory(String name) {
		if (!categories.containsKey(name))
			throw new NoSuchConfigCategoryError(getPath() + "." + name);
		return categories.get(name);
	}
	
	/**
	 * Get a config group
	 *
	 * @param path Name or dot-separated path to the group
	 * @throws NoSuchConfigGroupError if the group is not found
	 */
	public SimpleConfigGroup getGroup(String path) {
		if (path.contains(".")) {
			final String[] split = path.split("\\.", 2);
			if (groups.containsKey(split[0]))
				return groups.get(split[1]).getGroup(split[1]);
		} else if (groups.containsKey(path))
			return groups.get(path);
		throw new NoSuchConfigGroupError(path);
	}
	
	protected ITextComponent getTitle() {
		if (I18n.hasKey(defaultTitle))
			return new TranslationTextComponent(defaultTitle);
		return new TranslationTextComponent(
		  "simpleconfig.config.category." + getType().name().toLowerCase());
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
	
	protected boolean canEdit() {
		return getType() != Type.SERVER
		       || FMLEnvironment.dist == Dist.DEDICATED_SERVER
		       || permissions.permissionFor(modId).getLeft().canEdit();
	}
	
	@OnlyIn(Dist.CLIENT)
	protected void buildGUI(ConfigScreenBuilder configBuilder, boolean forRemote) {
		if (background != null)
			configBuilder.setDefaultBackgroundTexture(background);
		configBuilder.setTransparentBackground(transparent);
		ConfigEntryBuilder entryBuilder = configBuilder.entryBuilder();
		if (!order.isEmpty()) {
			final ConfigCategoryBuilder category = configBuilder.getOrCreateCategory(
			  "", type.asEditType(forRemote));
			category.setEditable(canEdit());
			category.setTitle(getTitle());
			getFilePath().ifPresent(category::setContainingFile);
			category.setDescription(
			  () -> I18n.hasKey(tooltip)
			        ? Optional.of(splitTtc(tooltip).toArray(new ITextComponent[0]))
			        : Optional.empty());
			if (background != null)
				category.setBackground(background);
			category.setIcon(defaultCategoryIcon);
			category.setColor(defaultCategoryColor);
			for (IGUIEntry entry : order)
				entry.buildGUI(category, entryBuilder, forRemote);
		}
		for (SimpleConfigCategory cat : categories.values())
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
	
	public Type getType() {
		return type;
	}
	
	public String getModId() {
		return modId;
	}
	
	public String getModName() {
		return getModNameOrId(modId);
	}
	
	@Internal public SimpleConfigCommentedYamlFormat getConfigFormat() {
		return configFormat;
	}
	
	@Internal public ModConfig getModConfig() {
		return modConfig;
	}
	
	@Internal public ModContainer getModContainer() {
		return modContainer;
	}
	
	@Internal public LiteralArgumentBuilder<CommandSource> getCommandRoot() {
		return commandRoot;
	}
	
	protected interface IGUIEntryBuilder {}
	
	protected interface IGUIEntry extends IGUIEntryBuilder {
		@Internal void buildGUI(ConfigCategoryBuilder category, ConfigEntryBuilder entryBuilder, boolean forRemote);
	}
	
	public static class NoSuchConfigEntryError extends RuntimeException {
		public NoSuchConfigEntryError(String path) {
			super("Cannot find config entry \"" + path + "\"");
		}
	}
	
	public static class NoSuchConfigCategoryError extends RuntimeException {
		public NoSuchConfigCategoryError(String path) {
			super("Cannot find config category \"" + path + "\"");
		}
	}
	
	public static class NoSuchConfigGroupError extends RuntimeException {
		public NoSuchConfigGroupError(String path) {
			super("Cannot find config group \"" + path + "\"");
		}
	}
	
	public static class InvalidConfigValueException extends RuntimeException {
		public InvalidConfigValueException(String path, Object value) {
			super("Invalid config value set for config entry \"" + path + "\": " + value);
		}
	}
	
	public static class InvalidDefaultConfigValueException extends RuntimeException {
		public InvalidDefaultConfigValueException(String path, Object value) {
			super("Invalid default config value set for config entry \"" + path + "\": " + value);
		}
	}
	
	public static class InvalidConfigValueTypeException extends RuntimeException {
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
	
	public static class ConfigReflectiveOperationException extends RuntimeException {
		public ConfigReflectiveOperationException(String message, Exception cause) {
			super(message, cause);
		}
	}
	
	public enum Type {
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
		
		private final ModConfig.Type type;
		private final boolean isLocal;
		private final boolean isRemote;
		private final String alias;
		
		Type(ModConfig.Type type, boolean isLocal, boolean isRemote) {
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
		
		public ModConfig.Type asConfigType() {
			return type;
		}
		public EditType asEditType(boolean remote) {
			return Arrays.stream(EditType.values()).filter(
			  t -> t.getType() == this && t.isOnlyRemote() == remote
			).findFirst().orElseGet(() -> Arrays.stream(EditType.values()).filter(
			  t -> t.getType() == this
			).findFirst().orElse(null));
		}
		public String getAlias() {
			return alias;
		}
	}
	
	public enum EditType {
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
		
		private final Type type;
		private final boolean isRemote;
		private final boolean onlyRemote;
		private final String alias;
		EditType(Type type, boolean isRemote, boolean onlyRemote) {
			this.type = type;
			this.isRemote = isRemote;
			this.onlyRemote = onlyRemote;
			alias = name().toLowerCase().replace('_', '-');
		}
		
		public Type getType() {
			return type;
		}
		public boolean isRemote() {
			return isRemote;
		}
		public boolean isOnlyRemote() {
			return onlyRemote;
		}
		public String getAlias() {
			return alias;
		}
	}
}
