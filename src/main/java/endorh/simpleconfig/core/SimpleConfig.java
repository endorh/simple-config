package endorh.simpleconfig.core;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingException;
import com.mojang.datafixers.util.Pair;
import endorh.simpleconfig.core.BackingField.BackingFieldBuilder;
import endorh.simpleconfig.core.SimpleConfigBuilder.CategoryBuilder;
import endorh.simpleconfig.core.SimpleConfigBuilder.GroupBuilder;
import endorh.simpleconfig.core.SimpleConfigSync.CSimpleConfigSyncPacket;
import endorh.simpleconfig.core.SimpleConfigSync.SSimpleConfigSyncPacket;
import endorh.simpleconfig.core.entry.Builders;
import endorh.simpleconfig.ui.api.ConfigBuilder;
import endorh.simpleconfig.ui.api.ConfigCategory;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.gui.AbstractConfigScreen;
import endorh.simpleconfig.ui.gui.Icon;
import endorh.simpleconfig.yaml.NodeComments;
import endorh.simpleconfig.yaml.SimpleConfigCommentedYamlFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.resources.I18n;
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
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import org.apache.commons.io.FileUtils;
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

import static endorh.simpleconfig.core.SimpleConfigSnapshotHandler.failedFuture;
import static endorh.simpleconfig.core.SimpleConfigTextUtil.splitTtc;
import static endorh.simpleconfig.yaml.SimpleConfigCommentedYamlWriter.blankLine;
import static endorh.simpleconfig.yaml.SimpleConfigCommentedYamlWriter.commentLine;
import static java.util.Collections.synchronizedMap;
import static java.util.Collections.unmodifiableMap;

/**
 * Simple config.<br>
 * Create and register your config with {@link SimpleConfig#builder(String, Type)}
 * or {@link SimpleConfig#builder(String, Type, Class)}
 */
public class SimpleConfig extends AbstractSimpleConfigEntryHolder {
	private static final Map<Pair<String, ModConfig.Type>, SimpleConfig> INSTANCES =
	  synchronizedMap(new HashMap<>());
	private static final Pattern LINE_BREAK = Pattern.compile("\\R");
	
	private final ModConfig.Type type;
	private final String modId;
	private SimpleConfigModConfig modConfig;
	
	protected final String defaultTitle;
	protected final String tooltip;
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
	
	protected final @Nullable Consumer<SimpleConfig> saver;
	protected final @Nullable Consumer<SimpleConfig> baker;
	protected final @Nullable Object configClass;
	@OnlyIn(Dist.CLIENT)
	protected @Nullable BiConsumer<SimpleConfig, ConfigBuilder> decorator;
	protected @Nullable ResourceLocation background;
	protected boolean transparent;
	@OnlyIn(Dist.CLIENT)
	protected @Nullable AbstractConfigScreen gui;
	protected @Nullable SimpleConfigSnapshotHandler snapshotHandler;
	private Map<String, NodeComments> comments = new HashMap<>();
	
	@SuppressWarnings("UnusedReturnValue")
	protected static SimpleConfig getInstance(
	  String modId, @SuppressWarnings("SameParameterValue") ModConfig.Type type
	) {
		Pair<String, ModConfig.Type> key = Pair.of(modId, type);
		if (!INSTANCES.containsKey(key)) {
			throw new IllegalStateException(
			  "Attempted to get unregistered config for mod id \"" + modId + "\" of type " + type);
		}
		return INSTANCES.get(key);
	}
	
	/**
	 * Retrieve the actual path of this file, if found
	 */
	public Optional<Path> getFilePath() {
		SimpleConfigModConfig modConfig = getModConfig();
		return modConfig.getConfigData() instanceof CommentedFileConfig? Optional.of(
		  modConfig.getFullPath()) : Optional.empty();
	}
	
	private static int TEXT_ENTRY_ID_GEN = 0;
	
	protected static String nextTextID() {
		return "_text$" + TEXT_ENTRY_ID_GEN++;
	}
	
	static {
		SimpleConfigSync.registerPackets();
		// Trigger class loading
		Builders.bool(true);
	}
	
	/**
	 * Create a {@link SimpleConfig} builder<br>
	 * Add entries with {@link SimpleConfigBuilder#add(String, AbstractConfigEntryBuilder)}<br>
	 * Add categories and groups with {@link SimpleConfigBuilder#n(CategoryBuilder)}
	 * and {@link SimpleConfigBuilder#n(GroupBuilder)}<br>
	 * Complete the config by calling {@link SimpleConfigBuilder#buildAndRegister()}<br>
	 *
	 * @param modId Your mod id
	 * @param type  A {@link ModConfig.Type}, usually either CLIENT or SERVER
	 */
	public static SimpleConfigBuilder builder(String modId, ModConfig.Type type) {
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
	 * @param type        A {@link ModConfig.Type}, usually either CLIENT or SERVER
	 * @param configClass Backing class for the config. It will be parsed
	 *                    for static backing fields and config annotations
	 */
	public static SimpleConfigBuilder builder(
	  String modId, ModConfig.Type type, Class<?> configClass
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
	
	@Internal protected SimpleConfig(
	  String modId, ModConfig.Type type, String defaultTitle,
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
		
		Pair<String, ModConfig.Type> key = Pair.of(modId, type);
		synchronized (INSTANCES) {
			if (!INSTANCES.containsKey(key))
				INSTANCES.put(key, this);
			else throw new IllegalStateException(
			  "Cannot create more than one config per type per mod");
		}
	}
	
	/**
	 * Setup the config
	 */
	@Internal protected void build(
	  Map<String, AbstractConfigEntry<?, ?, ?, ?>> entries,
	  Map<String, SimpleConfigCategory> categories,
	  Map<String, SimpleConfigGroup> groups,
	  List<IGUIEntry> order, ForgeConfigSpec spec,
	  Icon icon, int color
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
	}
	
	@Internal protected void build(SimpleConfigModConfig modConfig) {
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
		return String.format("%s-%s.yaml", getModId(), getType().extension());
	}
	
	/**
	 * Get the display name of the mod, or just its mod id if not found
	 */
	protected static String getModNameOrId(String modId) {
		final Optional<ModInfo> first = ModList.get().getMods().stream()
		  .filter(m -> modId.equals(m.getModId())).findFirst();
		if (first.isPresent())
			return first.get().getDisplayName();
		return modId;
	}
	
	/**
	 * Bakes all the backing fields<br>
	 */
	protected void bakeFields() {
		for (SimpleConfigCategory cat : categories.values())
			cat.bakeFields();
		for (SimpleConfigGroup group : groups.values())
			group.bakeFields();
		for (AbstractConfigEntry<?, ?, ?, ?> entry : entries.values())
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
			for (AbstractConfigEntry<?, ?, ?, ?> entry : entries.values())
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
	
	@Override protected void removeGUI() {
		super.removeGUI();
		gui = null;
		snapshotHandler = null;
	}
	
	protected void setGUI(AbstractConfigScreen gui, SimpleConfigSnapshotHandler handler) {
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
	
	protected void syncToClients() {
		new SSimpleConfigSyncPacket(this).sendToAll();
	}
	
	protected void syncToServer() {
		new CSimpleConfigSyncPacket(this).send();
	}
	
	protected CommentedConfig takeSnapshot(boolean fromGUI) {
		return takeSnapshot(fromGUI, null);
	}
	
	protected CommentedConfig takeSnapshot(boolean fromGUI, @Nullable Set<String> selectedPaths) {
		final CommentedConfig config = CommentedConfig.of(LinkedHashMap::new, getModConfig().getConfigFormat());
		saveSnapshot(config, fromGUI, selectedPaths);
		return config;
	}
	
	@Override protected void loadSnapshot(
	  CommentedConfig config, boolean intoGUI, @Nullable Set<String> selectedPaths
	) {
		if (intoGUI) {
			AbstractConfigScreen screen = getGUI();
			if (screen != null) {
				screen.runAtomicTransparentAction(
				  () -> super.loadSnapshot(config, true, selectedPaths));
			} else throw new IllegalStateException(
			  "Cannot load snapshot into GUI when no GUI is active");
		} else super.loadSnapshot(config, false, selectedPaths);
	}
	
	protected void reload() {
		bake();
		if (getType() == Type.SERVER)
			DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, () -> this::syncToClients);
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			SimpleConfigSnapshotHandler handler = getSnapshotHandler();
			if (handler != null) handler.notifyExternalChanges(this);
		});
	}
	
	/**
	 * Handle external config modification events
	 */
	@SubscribeEvent
	protected void onModConfigEvent(final ModConfig.ModConfigEvent event) {
		final ModConfig c = event.getConfig();
		final ModConfig.Type type = c.getType();
		final Pair<String, ModConfig.Type> key = Pair.of(c.getModId(), type);
		if (INSTANCES.containsKey(key)) {
			final SimpleConfig config = INSTANCES.get(key);
			config.bake();
			if (type == ModConfig.Type.SERVER)
				DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, () -> config::syncToClients);
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
				SimpleConfigSnapshotHandler handler = config.getSnapshotHandler();
				if (handler != null) handler.notifyExternalChanges(config);
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
	
	@Internal public static void updateAllFileTranslations() {
		for (SimpleConfig config : INSTANCES.values()) {
			if (config.modConfig.getConfigData() != null) {
				config.spec.save();
			}
		}
	}
	
	protected String getHeaderComment() {
		String type = this.getType().extension();
		type = Character.toUpperCase(type.charAt(0)) + type.substring(1);
		return getModNameOrId(getModId()) + " - " + type + " config\n";
	}
	
	@Internal public Map<String, NodeComments> getComments() {
		updateComments(comments);
		String headerComment = getHeaderComment().trim();
		List<CommentLine> headerLines = Arrays.stream(LINE_BREAK.split(headerComment))
		  .map(l -> commentLine(" " + l))
		  .collect(Collectors.toList());
		headerLines.add(blankLine());
		comments.put("", Util.make(new NodeComments(), c -> c.setBlockComments(headerLines)));
		return comments;
	}
	
	@Internal public void loadComments(Map<String, NodeComments> comments) {
		this.comments = comments;
	}
	
	protected ConfigSpec buildConfigSpec() {
		final ConfigSpec spec = new ConfigSpec();
		for (AbstractConfigEntry<?, ?, ?, ?> e : entries.values())
			e.buildSpec(spec, "");
		for (AbstractSimpleConfigEntryHolder child : children.values())
			child.buildConfigSpec(spec, "");
		return spec;
	}
	
	@OnlyIn(Dist.CLIENT)
	protected void buildGUI(ConfigBuilder configBuilder) {
		if (background != null)
			configBuilder.setDefaultBackgroundTexture(background);
		configBuilder.setTransparentBackground(transparent);
		ConfigEntryBuilder entryBuilder = configBuilder.entryBuilder();
		if (!order.isEmpty()) {
			final ConfigCategory category = configBuilder.getOrCreateCategory(getType().name().toLowerCase(), getType() == Type.SERVER);
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
				entry.buildGUI(category, entryBuilder);
		}
		for (SimpleConfigCategory cat : categories.values()) {
			cat.buildGUI(configBuilder, entryBuilder);
		}
		if (decorator != null)
			decorator.accept(this, configBuilder);
	}
	
	// null config implies deletion
	protected CompletableFuture<Void> saveLocalPreset(String name, @Nullable CommentedConfig config) {
		final String prefix = getType().name().toLowerCase() + "-";
		final String fileName = getModId() + "-preset-" + prefix + name + ".yaml";
		final File dest = FMLPaths.CONFIGDIR.get().resolve(fileName).toFile();
		if (dest.isDirectory())
			return failedFuture(new FileNotFoundException(dest.getPath()));
		if (config != null) {
			SimpleConfigCommentedYamlFormat format = getModConfig().getConfigFormat();
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
			if (!dest.exists() || !dest.isFile())
				return failedFuture(new FileNotFoundException(dest.getPath()));
			if (!dest.delete())
				return failedFuture(new IOException("Could not delete file " + dest.getPath()));
			return CompletableFuture.completedFuture(null);
		}
	}
	
	protected CompletableFuture<Void> saveRemotePreset(String name, CommentedConfig config) {
		final String prefix = getType().name().toLowerCase() + "-";
		return SimpleConfigSync.saveSnapshot(getModId(), prefix + name, config);
	}
	
	protected CompletableFuture<CommentedConfig> getLocalPreset(String name) {
		final CompletableFuture<CommentedConfig> future = new CompletableFuture<>();
		final String prefix = getType().name().toLowerCase() + "-";
		final Optional<Path> opt = getFilePath();
		if (!opt.isPresent()) {
			future.completeExceptionally(
			  new FileNotFoundException("Config file for mod " + getModId()));
			return future;
		}
		final File file = opt.get().getParent()
		  .resolve(getModId() + "-preset-" + prefix + name + ".yaml").toFile();
		if (!file.exists() || !file.isFile()) {
			future.completeExceptionally(new FileNotFoundException(file.getPath()));
			return future;
		}
		try {
			SimpleConfigCommentedYamlFormat format = getModConfig().getConfigFormat();
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
		final String prefix = getType().name().toLowerCase() + "-";
		return SimpleConfigSync.requestSnapshot(getModId(), prefix + name);
	}
	
	public Type getType() {
		return type;
	}
	
	public String getModId() {
		return modId;
	}
	
	public SimpleConfigModConfig getModConfig() {
		return modConfig;
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
	
	protected interface IGUIEntryBuilder {}
	
	protected interface IGUIEntry extends IGUIEntryBuilder {
		@Internal void buildGUI(
		  ConfigCategory category, ConfigEntryBuilder entryBuilder
		);
	}
}
