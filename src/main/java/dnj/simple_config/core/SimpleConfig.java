package dnj.simple_config.core;

import com.mojang.datafixers.util.Pair;
import dnj.simple_config.core.SimpleConfigBuilder.CategoryBuilder;
import dnj.simple_config.core.SimpleConfigBuilder.GroupBuilder;
import dnj.simple_config.core.SimpleConfigSync.CSimpleConfigSyncPacket;
import dnj.simple_config.core.SimpleConfigSync.SSimpleConfigSyncPacket;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.resources.I18n;
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
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.Collections.synchronizedMap;
import static java.util.Collections.unmodifiableMap;

/**
 * Simple config class. Requires Cloth Config API (Forge) for the GUI menu<br>
 * Create and register your config with {@link SimpleConfig#builder(String, Type)}
 * or {@link SimpleConfig#builder(String, Type, Class)}
 */
public class SimpleConfig extends AbstractSimpleConfigEntryHolder {
	
	private static final Map<Pair<String, ModConfig.Type>, SimpleConfig> INSTANCES = synchronizedMap(new HashMap<>());
	
	public final ModConfig.Type type;
	public final String modId;
	
	protected final String defaultTitle;
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
	protected BiConsumer<SimpleConfig, ConfigBuilder> decorator;
	
	protected final boolean debugTranslations;
	
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
	
	private static int TEXT_ENTRY_ID_GEN = 0;
	protected static String nextTextID() {
		return "_text$" + TEXT_ENTRY_ID_GEN++;
	}
	
	static {
		SimpleConfigSync.registerPackets();
	}
	
	/**
	 * Create a {@link SimpleConfig} builder<br>
	 * Add entries with {@link SimpleConfigBuilder#add(String, AbstractConfigEntry)}<br>
	 * Add categories and groups with {@link SimpleConfigBuilder#n(CategoryBuilder)}
	 * and {@link SimpleConfigBuilder#n(GroupBuilder)}<br>
	 * Complete the config by calling {@link SimpleConfigBuilder#buildAndRegister()}<br>
	 * @param modId Your mod id
	 * @param type A {@link ModConfig.Type}, usually either CLIENT or SERVER
	 */
	public static SimpleConfigBuilder builder(String modId, ModConfig.Type type) {
		return new SimpleConfigBuilder(modId, type);
	}
	
	/**
	 * Create a {@link SimpleConfig} builder<br>
	 * Add entries with {@link SimpleConfigBuilder#add(String, AbstractConfigEntry)}<br>
	 * Add categories and groups with {@link SimpleConfigBuilder#n(CategoryBuilder)}
	 * and {@link SimpleConfigBuilder#n(GroupBuilder)}<br>
	 * Complete the config by calling {@link SimpleConfigBuilder#buildAndRegister()}<br>
	 * @param modId Your mod id
	 * @param type A {@link ModConfig.Type}, usually either CLIENT or SERVER
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
	 * @param name Group name, suitable for the config file (without spaces)
	 */
	public static GroupBuilder group(String name) {
		return group(name, false);
	}
	
	/**
	 * Create a config group
	 * @param name Group name, suitable for the config file (without spaces)
	 * @param expand Whether or not to expand this group in the GUI automatically
	 *               (default: no)
	 */
	public static GroupBuilder group(String name, boolean expand) {
		return new GroupBuilder(name, expand);
	}
	
	/**
	 * Create a config category
	 * @param name Category name, suitable for the config file (without spaces)
	 */
	public static CategoryBuilder category(String name) {
		return new CategoryBuilder(name);
	}
	
	/**
	 * Create a config category
	 * @param name Category name, suitable for the config file (without spaces)
	 * @param configClass Backing class for the category, which will be parsed
	 *                    for static backing fields and config annotations
	 */
	public static CategoryBuilder category(String name, Class<?> configClass) {
		return new CategoryBuilder(name, configClass);
	}
	
	@Internal protected SimpleConfig(
	  String modId, ModConfig.Type type, String defaultTitle,
	  @Nullable Consumer<SimpleConfig> baker, @Nullable Consumer<SimpleConfig> saver,
	  @Nullable Object configClass, boolean debugTranslations
	) {
		this.modId = modId;
		this.type = type;
		this.defaultTitle = defaultTitle;
		this.baker = baker;
		this.saver = saver;
		this.configClass = configClass;
		this.debugTranslations = debugTranslations;
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
	  Map<String, ConfigValue<?>> specValues,
	  List<IGUIEntry> order,
	  ForgeConfigSpec spec
	) {
		if (this.entries != null)
			throw new IllegalStateException("Called build() twice");
		this.entries = entries;
		this.categories = categories;
		this.groups = groups;
		this.specValues = specValues;
		this.order = order;
		this.spec = spec;
		final Map<String, AbstractSimpleConfigEntryHolder> children = new HashMap<>();
		children.putAll(this.categories);
		children.putAll(this.groups);
		this.children = unmodifiableMap(children);
	}
	
	/**
	 * Get the display name of the mod, or just its mod id if not found
	 */
	public static String getModNameOrId(String modId) {
		final Optional<ModInfo> first = ModList.get().getMods().stream()
		  .filter(m -> modId.equals(m.getModId())).findFirst();
		if (first.isPresent())
			return first.get().getDisplayName();
		return modId;
	}
	
	/**
	 * Bakes all the backing fields
	 */
	protected void bakeFields() {
		try {
			for (SimpleConfigCategory cat : categories.values())
				cat.bakeFields();
			for (SimpleConfigGroup group : groups.values())
				group.bakeFields();
			for (AbstractConfigEntry<?, ?, ?, ?> entry : entries.values())
				if (entry.backingField != null)
					entry.backingField.set(null, get(entry.name));
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(
			  "Could not access mod config field during config bake\n  Details: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Run the baker, and then the categories' bakers
	 */
	public void bake() {
		bakeFields();
		if (baker != null)
			baker.accept(this);
		for (SimpleConfigCategory cat : categories.values())
			if (cat.baker != null)
				cat.baker.accept(cat);
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
	 * Decorate a GUI builder
	 */
	@OnlyIn(Dist.CLIENT)
	public void decorate(ConfigBuilder builder) {
		if (decorator != null)
			decorator.accept(this, builder);
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
		new SSimpleConfigSyncPacket(this).send();
	}
	protected void syncToServer() {
		new CSimpleConfigSyncPacket(this).send();
	}
	
	/**
	 * Handle external config modification events
	 */
	@SubscribeEvent
	public void onModConfigEvent(final ModConfig.ModConfigEvent event) {
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
	 * @param name Name of the category
	 * @throws NoSuchConfigCategoryError if the category is not found
	 */
	@SuppressWarnings("unused")
	public SimpleConfigCategory getCategory(String name) {
		if (!categories.containsKey(name))
			throw new NoSuchConfigCategoryError(name);
		return categories.get(name);
	}
	
	/**
	 * Get a config group
	 * @param path Name or dot-separated path to the group
	 * @throws NoSuchConfigGroupError if the group is not found
	 */
	@SuppressWarnings("unused")
	public SimpleConfigGroup getGroup(String path) {
		if (path.contains(".")) {
			final String[] split = path.split("\\.", 2);
			if (groups.containsKey(split[0]))
				return groups.get(split[1]).getGroup(split[1]);
		} else if (groups.containsKey(path))
			return groups.get(path);
		throw new NoSuchConfigGroupError(path);
	}
	
	public ITextComponent getTitle() {
		if (I18n.hasKey(defaultTitle))
			return new TranslationTextComponent(defaultTitle);
		return new TranslationTextComponent("simple-config.config.category." + type.name().toLowerCase());
	}
	
	@OnlyIn(Dist.CLIENT)
	public void buildGUI(ConfigBuilder configBuilder) {
		ConfigEntryBuilder entryBuilder = configBuilder.entryBuilder();
		if (!order.isEmpty()) {
			final me.shedaniel.clothconfig2.api.ConfigCategory category = configBuilder.getOrCreateCategory(getTitle());
			for (IGUIEntry entry : order)
				entry.buildGUI(category, entryBuilder, this);
		}
		for (SimpleConfigCategory cat : categories.values()) {
			cat.buildGUI(configBuilder, entryBuilder);
		}
	}
	
	public static class NoSuchConfigEntryError extends RuntimeException {
		public NoSuchConfigEntryError(String name) {
			super("Cannot find config entry with name \"" + name + "\"");
		}
	}
	
	public static class NoSuchConfigCategoryError extends RuntimeException {
		public NoSuchConfigCategoryError(String name) {
			super("Cannot find config category with name \"" + name + "\"");
		}
	}
	
	public static class NoSuchConfigGroupError extends RuntimeException {
		public NoSuchConfigGroupError(String name) {
			super("Cannot find config group with name \"" + name + "\"");
		}
	}
	
	public static class InvalidConfigValueTypeException extends RuntimeException {
		public InvalidConfigValueTypeException(String name, ClassCastException cause) {
			super("Invalid type requested for config value \"" + name + "\"", cause);
		}
	}
	
	public interface IAbstractGUIEntry {}
	public interface IGUIEntry extends IAbstractGUIEntry {
		void buildGUI(me.shedaniel.clothconfig2.api.ConfigCategory category, ConfigEntryBuilder entryBuilder, ISimpleConfigEntryHolder config);
	}
}
