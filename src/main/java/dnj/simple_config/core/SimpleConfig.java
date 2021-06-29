package dnj.simple_config.core;

import com.mojang.datafixers.util.Pair;
import dnj.simple_config.core.SimpleConfigBuilder.CategoryBuilder;
import dnj.simple_config.core.SimpleConfigBuilder.GroupBuilder;
import dnj.simple_config.core.SimpleConfigSync.CSimpleConfigSyncPacket;
import dnj.simple_config.core.SimpleConfigSync.SSimpleConfigSyncPacket;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.SubCategoryListEntry;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.Collections.unmodifiableMap;

/**
 * Simple config class. Requires Cloth Config API (Forge)
 */
public class SimpleConfig extends AbstractSimpleConfigEntryHolder {
	
	public final ModConfig.Type type;
	public final String modId;
	
	protected final String defaultTitle;
	/**
	 * Should not be modified
	 */
	protected Map<String, Category> categories = null;
	/**
	 * Should not be modified
	 */
	protected Map<String, Group> groups = null;
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
	
	private static final Map<Pair<String, ModConfig.Type>, SimpleConfig> INSTANCES = new HashMap<>();
	
	@SuppressWarnings("UnusedReturnValue")
	public static SimpleConfig getInstance(String modId, ModConfig.Type type) {
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
	
	public static GroupBuilder group(String name) {
		return group(name, false);
	}
	public static GroupBuilder group(String name, boolean expand) {
		return new GroupBuilder(name, expand);
	}
	
	public static CategoryBuilder category(String name) {
		return new CategoryBuilder(name);
	}
	
	public static CategoryBuilder category(String name, Class<?> configClass) {
		return new CategoryBuilder(name, configClass);
	}
	
	protected SimpleConfig(
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
		root = this;
		
		Pair<String, ModConfig.Type> key = Pair.of(modId, type);
		if (!INSTANCES.containsKey(key))
			INSTANCES.put(key, this);
		else throw new IllegalStateException(
		  "Cannot create more than one config per type per mod");
	}
	
	protected void build(
	  Map<String, Entry<?, ?>> entries,
	  Map<String, Category> categories,
	  Map<String, Group> groups,
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
			for (Category cat : categories.values())
				cat.bakeFields();
			for (Group group : groups.values())
				group.bakeFields();
			for (Entry<?, ?> entry : entries.values())
				if (entry.backingField != null)
					entry.backingField.set(null, get(entry.name));
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(
			  "Could not access mod config field during config bake\n  Details: " + e.getMessage(), e);
		}
	}
	
	public void bake() {
		bakeFields();
		if (baker != null)
			baker.accept(this);
		for (Category cat : categories.values())
			if (cat.baker != null)
				cat.baker.accept(cat);
	}
	
	public void save() {
		// TODO: If the change requires a restart suggest a command for operators in the chat
		//       or at least post a warning
		bake();
		if (saver != null)
			saver.accept(this);
	}
	
	@OnlyIn(Dist.CLIENT)
	public void decorate(ConfigBuilder builder) {
		if (decorator != null)
			decorator.accept(this, builder);
	}
	
	protected void syncToClients() {
		new SSimpleConfigSyncPacket(this).send();
	}
	protected void syncToServer() {
		new CSimpleConfigSyncPacket(this).send();
	}
	
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
	public Category getCategory(String name) {
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
	public Group getGroup(String path) {
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
			final ConfigCategory category = configBuilder.getOrCreateCategory(getTitle());
			for (IGUIEntry entry : order)
				entry.buildGUI(category, entryBuilder, this);
		}
		for (Category cat : categories.values()) {
			cat.buildGUI(configBuilder, entryBuilder);
		}
	}
	
	public static class Category extends AbstractSimpleConfigEntryHolder {
		public final SimpleConfig parent;
		public final String name;
		public final String title;
		protected final @Nullable Consumer<Category> baker;
		protected Map<String, Group> groups;
		protected List<IGUIEntry> order;
		
		public Category(
		  SimpleConfig parent, String name, String title, @Nullable Consumer<Category> baker
		) {
			this.parent = parent;
			this.name = name;
			this.title = title;
			this.baker = baker;
			root = parent;
		}
		
		protected void build(
		  Map<String, Entry<?, ?>> entries, Map<String, Group> groups,
		  Map<String, ConfigValue<?>> specValues,
		  List<IGUIEntry> order
		) {
			if (this.entries != null)
				throw new IllegalStateException("Called build() twice");
			this.entries = entries;
			this.groups = groups;
			children = groups;
			this.specValues = specValues;
			this.order = order;
		}
		
		@Override public void markDirty(boolean dirty) {
			super.markDirty(dirty);
			if (dirty) parent.markDirty(true);
		}
		
		@OnlyIn(Dist.CLIENT)
		public void buildGUI(ConfigBuilder builder, ConfigEntryBuilder entryBuilder) {
			ConfigCategory category = builder.getOrCreateCategory(getTitle());
			if (!order.isEmpty()) {
				for (IGUIEntry entry : order)
					entry.buildGUI(category, entryBuilder, this);
			}
		}
		
		protected void bakeFields() throws IllegalAccessException {
			for (Group group : groups.values())
				group.bakeFields();
			for (Entry<?, ?> entry : entries.values())
				if (entry.backingField != null)
					entry.backingField.set(null, get(entry.name));
		}
		
		/**
		 * Get a config group
		 * @param path Name or dot-separated path to the group
		 * @throws NoSuchConfigGroupError if the group is not found
		 */
		@SuppressWarnings("unused")
		public Group getGroup(String path) {
			if (path.contains(".")) {
				final String[] split = path.split("\\.", 2);
				if (groups.containsKey(split[0]))
					return groups.get(split[0]).getGroup(split[1]);
			} else if (groups.containsKey(path))
				return groups.get(path);
			throw new NoSuchConfigGroupError(path);
		}
		
		public ITextComponent getTitle() {
			return new TranslationTextComponent(title);
		}
	}
	
	public static class Group extends AbstractSimpleConfigEntryHolder implements IGUIEntry {
		public final Category category;
		public final @Nullable Group parentGroup;
		public final String name;
		public final String title;
		public final String tooltip;
		protected Map<String, Group> groups;
		protected List<IGUIEntry> order;
		private final boolean expanded;
		
		public Group(
		  Group parent, String name, String title, String tooltip, boolean expanded
		) {
			this.category = parent.category;
			this.parentGroup = parent;
			this.name = name;
			this.title = title;
			this.tooltip = tooltip;
			this.expanded = expanded;
			root = category.root;
		}
		
		public Group(
		  Category parent, String name, String title, String tooltip, boolean expanded
		) {
			this.category = parent;
			this.parentGroup = null;
			this.name = name;
			this.title = title;
			this.tooltip = tooltip;
			this.expanded = expanded;
			root = category.root;
		}
		
		protected void build(
		  Map<String, Entry<?, ?>> entries, Map<String, ConfigValue<?>> specValues,
		  Map<String, Group> groups, List<IGUIEntry> guiOrder
		) {
			if (this.entries != null)
				throw new IllegalStateException("Called build() twice");
			this.entries = entries;
			this.specValues = specValues;
			this.groups = groups;
			children = groups;
			this.order = guiOrder;
		}
		
		public Category getCategory() {
			return category;
		}
		
		@Override public void markDirty(boolean dirty) {
			super.markDirty(dirty);
			if (dirty) (parentGroup != null? parentGroup : category).markDirty(true);
		}
		
		public ITextComponent getTitle() {
			return new TranslationTextComponent(title);
		}
		
		public Optional<ITextComponent[]> getTooltip() {
			if (tooltip != null && I18n.hasKey(tooltip))
				return Optional.of(
				  Arrays.stream(I18n.format(tooltip).split("\n"))
					 .map(StringTextComponent::new).toArray(ITextComponent[]::new));
			return Optional.empty();
		}
		
		@OnlyIn(Dist.CLIENT)
		public SubCategoryListEntry buildGUI(ConfigEntryBuilder entryBuilder) {
			final SubCategoryBuilder group = entryBuilder
			  .startSubCategory(getTitle())
			  .setExpanded(expanded)
			  .setTooltip(getTooltip());
			if (!order.isEmpty()) {
				for (IGUIEntry entry : order) {
					if (entry instanceof Entry) {
						((Entry<?, ?>) entry).buildGUIEntry(entryBuilder, this).ifPresent(group::add);
					} else if (entry instanceof Group) {
						group.add(((Group) entry).buildGUI(entryBuilder));
					}
				}
			}
			return group.build();
		}
		
		@OnlyIn(Dist.CLIENT)
		public void buildGUI(ConfigCategory category, ConfigEntryBuilder entryBuilder) {
			category.addEntry(buildGUI(entryBuilder));
		}
		
		@Override public void buildGUI(
		  ConfigCategory category, ConfigEntryBuilder entryBuilder, AbstractSimpleConfigEntryHolder config
		) {
			category.addEntry(buildGUI(entryBuilder));
		}
		
		protected void bakeFields() throws IllegalAccessException {
			for (Group group : groups.values())
				group.bakeFields();
			for (Entry<?, ?> entry : entries.values())
				if (entry.backingField != null)
					entry.backingField.set(null, get(entry.name));
		}
		
		/**
		 * Get a config subgroup
		 * @param path Name or dot-separated path to the group
		 * @throws NoSuchConfigGroupError if the group is not found
		 */
		public Group getGroup(String path) {
			if (path.contains(".")) {
				final String[] split = path.split("\\.", 2);
				if (groups.containsKey(split[0]))
					return groups.get(split[0]).getGroup(split[1]);
			} else if (groups.containsKey(path))
				return groups.get(path);
			throw new NoSuchConfigGroupError(path);
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
		void buildGUI(ConfigCategory category, ConfigEntryBuilder entryBuilder, AbstractSimpleConfigEntryHolder config);
	}
}
