package endorh.simple_config.core;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.io.IndentStyle;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.mojang.datafixers.util.Pair;
import endorh.simple_config.SimpleConfigMod;
import endorh.simple_config.SimpleConfigMod.ConfigPermission;
import endorh.simple_config.clothconfig2.api.ConfigBuilder;
import endorh.simple_config.clothconfig2.api.ConfigCategory;
import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.core.SimpleConfigBuilder.CategoryBuilder;
import endorh.simple_config.core.SimpleConfigBuilder.GroupBuilder;
import endorh.simple_config.core.SimpleConfigSync.CSimpleConfigSyncPacket;
import endorh.simple_config.core.SimpleConfigSync.SSimpleConfigSyncPacket;
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
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.Nullable;
import javax.naming.NoPermissionException;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static endorh.simple_config.core.SimpleConfigTextUtil.splitTtc;
import static java.util.Collections.*;

/**
 * Simple config class. Requires Cloth Config API (Forge) for the GUI menu<br>
 * Create and register your config with {@link SimpleConfig#builder(String, Type)}
 * or {@link SimpleConfig#builder(String, Type, Class)}
 */
public class SimpleConfig extends AbstractSimpleConfigEntryHolder {
	
	private static final Map<Pair<String, ModConfig.Type>, SimpleConfig> INSTANCES =
	  synchronizedMap(new HashMap<>());
	
	public final ModConfig.Type type;
	public final String modId;
	
	protected final String defaultTitle;
	protected final String tooltip;
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
		return SimpleConfigSync.getConfigFilePath(this);
	}
	
	private static int TEXT_ENTRY_ID_GEN = 0;
	
	protected static String nextTextID() {
		return "_text$" + TEXT_ENTRY_ID_GEN++;
	}
	
	static {
		SimpleConfigSync.registerPackets();
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
		this.tooltip = defaultTitle + ":help";
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
	  List<IGUIEntry> order,
	  ForgeConfigSpec spec
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
	}
	
	/**
	 * Used in error messages
	 */
	@Override protected String getPath() {
		return getName();
	}
	
	@Override protected String getName() {
		return "SimpleConfig[" + modId + ", " + type.name() + "]";
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
	 * Commits any changes in the backing fields to the actual config file
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
				  "simple-config.config.msg.client_changes_require_restart"
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
		  "simple-config.config.category." + type.name().toLowerCase());
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
			final ConfigCategory category = configBuilder.getOrCreateCategory(type.name().toLowerCase());
			category.setTitle(getTitle());
			category.setIsServer(type == Type.SERVER);
			getFilePath().ifPresent(category::setContainingFile);
			category.setDescription(
			  () -> I18n.hasKey(tooltip)
			        ? Optional.of(splitTtc(tooltip).toArray(new ITextComponent[0]))
			        : Optional.empty());
			if (background != null)
				category.setBackground(background);
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
		final String prefix = type.name().toLowerCase() + "-";
		final String fileName = modId + "-preset-" + prefix + name + ".toml";
		final File dest = FMLPaths.CONFIGDIR.get().resolve(fileName).toFile();
		if (dest.isDirectory())
			return failedFuture(new FileNotFoundException(dest.getPath()));
		if (config != null) {
			final TomlWriter writer = new TomlWriter();
			writer.setIndent(IndentStyle.SPACES_2);
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			writer.write(config.unmodifiable(), os);
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
		final String prefix = type.name().toLowerCase() + "-";
		return SimpleConfigSync.saveSnapshot(modId, prefix + name, config);
	}
	
	protected CompletableFuture<CommentedConfig> getLocalPreset(String name) {
		final CompletableFuture<CommentedConfig> future = new CompletableFuture<>();
		final String prefix = type.name().toLowerCase() + "-";
		final Optional<Path> opt = getFilePath();
		if (!opt.isPresent()) {
			future.completeExceptionally(
			  new FileNotFoundException("Config file for mod " + modId));
			return future;
		}
		final File file =
		  opt.get().getParent().resolve(modId + "-preset-" + prefix + name + ".toml").toFile();
		if (!file.exists() || !file.isFile()) {
			future.completeExceptionally(new FileNotFoundException(file.getPath()));
			return future;
		}
		try {
			final CommentedConfig config = TomlFormat.instance().createParser()
			  .parse(new ByteArrayInputStream(FileUtils.readFileToByteArray(file)));
			future.complete(config);
			return future;
		} catch (IOException e) {
			future.completeExceptionally(e);
			return future;
		}
	}
	
	protected CompletableFuture<CommentedConfig> getRemotePreset(String name) {
		final String prefix = type.name().toLowerCase() + "-";
		return SimpleConfigSync.requestSnapshot(modId, prefix + name);
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
	
	public static class InvalidConfigValueTypeException extends RuntimeException {
		public InvalidConfigValueTypeException(String path) {
			super("Invalid type requested for config value \"" + path + "\"");
		}
		
		public InvalidConfigValueTypeException(String path, ClassCastException cause) {
			super("Invalid type requested for config value \"" + path + "\"", cause);
		}
		
		public InvalidConfigValueTypeException(String path, ClassCastException cause, String extra) {
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
	
	protected static class SnapshotHandler implements ConfigBuilder.SnapshotHandler {
		private final String modId;
		private final Map<ModConfig.Type, SimpleConfig> configMap;
		
		public SnapshotHandler(
		  Map<Type, SimpleConfig> configMap
		) {
			this.configMap = configMap;
			modId = configMap.values().stream().findFirst()
			  .map(c -> c.modId).orElse("");
		}
		
		@Override public CommentedConfig preserve(Type type) {
			final SimpleConfig c = configMap.get(type);
			if (c == null) throw new IllegalArgumentException("Unsupported config type: " + type);
			final CommentedConfig config =
			  CommentedConfig.of(LinkedHashMap::new, TomlFormat.instance());
			c.saveGUISnapshot(config);
			return config;
		}
		
		@Override public void restore(
		  CommentedConfig config, Type type
		) {
			final SimpleConfig c = configMap.get(type);
			if (c != null)
				c.loadGUISnapshot(config);
		}
		
		@OnlyIn(Dist.CLIENT)
		@Override public boolean canSaveRemote() {
			final Minecraft mc = Minecraft.getInstance();
			if (mc.getConnection() == null || mc.player == null) return false;
			return SimpleConfigMod.ServerConfig.permissions.permissionFor(mc.player, modId) == ConfigPermission.ALLOW;
		}
		
		@Override public CommentedConfig getLocal(String name, Type type) {
			final SimpleConfig c = configMap.get(type);
			if (c == null)
				throw new IllegalArgumentException("Missing config type");
			final CompletableFuture<CommentedConfig> future = c.getLocalPreset(name);
			if (!future.isDone())
				throw new IllegalStateException("Uncompleted future");
			return future.getNow(null);
		}
		
		@Override public CompletableFuture<CommentedConfig> getRemote(String name, Type type) {
			final SimpleConfig c = configMap.get(type);
			return c != null ? c.getRemotePreset(name) :
			       failedFuture(new IllegalArgumentException("Missing config type"));
		}
		
		@Override public Optional<Throwable> saveLocal(
		  String name, Type type, CommentedConfig config
		) {
			final SimpleConfig c = configMap.get(type);
			if (c != null) {
				return getException(c.saveLocalPreset(name, config));
			} else return Optional.empty();
		}
		
		@Override public CompletableFuture<Void> saveRemote(
		  String name, Type type, CommentedConfig config
		) {
			if (!canSaveRemote())
				return failedFuture(new NoPermissionException("Cannot save remote preset"));
			final SimpleConfig c = configMap.get(type);
			return c != null ? c.saveRemotePreset(name, config) :
			       failedFuture(new IllegalArgumentException("Missing config type"));
		}
		
		@Override public Optional<Throwable> deleteLocal(
		  String name, Type type
		) {
			return saveLocal(name, type, null);
		}
		
		@Override public CompletableFuture<Void> deleteRemote(
		  String name, Type type
		) {
			return saveRemote(name, type, null);
		}
		
		@Override public List<String> getLocalSnapshotNames() {
			final SimpleConfig c = configMap.get(Type.CLIENT);
			if (c == null) return emptyList();
			final Optional<Path> opt = c.getFilePath();
			if (!opt.isPresent()) return emptyList();
			final File dir = opt.get().getParent().toFile();
			Pattern pattern = Pattern.compile("^" + c.modId + "-preset-.+\\.toml$");
			final File[] files =
			  dir.listFiles((d, name) -> pattern.matcher(name).matches());
			return files == null ? emptyList() :
			       Arrays.stream(files).map(
						f -> {
							final String name = f.getName();
							return name.substring(
							  c.modId.length() + 8, // "-preset-".length()
							  name.length() - 5); // ".toml".length()
						}
			       ).collect(Collectors.toList());
		}
		
		@Override public CompletableFuture<List<String>> getRemoteSnapshotNames() {
			final SimpleConfig c = configMap.get(Type.SERVER);
			if (c == null) return CompletableFuture.completedFuture(emptyList());
			return SimpleConfigSync.requestSnapshotList(c.modId);
		}
	}
	
	private static <T> CompletableFuture<T> failedFuture(Throwable throwable) {
		final CompletableFuture<T> future = new CompletableFuture<>();
		future.completeExceptionally(throwable);
		return future;
	}
	
	private static Optional<Throwable> getException(CompletableFuture<?> future) {
		if (future.isCompletedExceptionally()) {
			try {
				future.getNow(null);
				return Optional.empty();
			} catch (CompletionException e) {
				return Optional.of(e.getCause());
			}
		}
		return Optional.empty();
	}
}
