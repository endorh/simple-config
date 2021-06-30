package dnj.simple_config.core;

import dnj.simple_config.core.SimpleConfig.Category;
import dnj.simple_config.core.SimpleConfig.Group;
import dnj.simple_config.core.SimpleConfig.IAbstractGUIEntry;
import dnj.simple_config.core.SimpleConfig.IGUIEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

public class SimpleConfigBuilder
  extends AbstractSimpleConfigEntryHolderBuilder<SimpleConfigBuilder> {
	protected final String modId;
	protected final ModConfig.Type type;
	
	protected final String title;
	
	protected final Map<String, CategoryBuilder> categories = new LinkedHashMap<>();
	protected final CategoryBuilder defaultCategory;
	
	protected Consumer<SimpleConfig> baker = null;
	protected Consumer<SimpleConfig> saver = null;
	protected BiConsumer<SimpleConfig, ConfigBuilder> decorator = null;
	protected final Class<?> configClass;
	protected String path;
	
	protected boolean debugTranslations = false;
	
	public SimpleConfigBuilder(String modId, Type type) { this(modId, type, null); }
	
	public SimpleConfigBuilder(String modId, Type type, @Nullable Class<?> configClass) {
		this.modId = modId;
		this.type = type;
		this.configClass = configClass;
		String classifier = type.name().toLowerCase();
		this.title = modId + ".config.category." + classifier;
		path = classifier;
		
		defaultCategory = new CategoryBuilder(classifier);
		defaultCategory.setParent(this);
		defaultCategory.path = path;
		defaultCategory.title = modId + ".config.category." + path;
	}
	
	public SimpleConfigBuilder setBaker(Consumer<SimpleConfig> baker) {
		this.baker = baker;
		return this;
	}
	
	@OnlyIn(Dist.CLIENT)
	public SimpleConfigBuilder setGUIDecorator(BiConsumer<SimpleConfig, ConfigBuilder> decorator) {
		this.decorator = decorator;
		return this;
	}
	
	public static void checkName(String name) {
		if (name.contains("."))
			throw new IllegalArgumentException("Config entry names cannot contain dots");
	}
	
	@Override public SimpleConfigBuilder restart() {
		super.restart();
		categories.values().forEach(CategoryBuilder::restart);
		return this;
	}
	
	@Override public void addEntry(Entry<?, ?, ?, ?> entry) {
		checkName(entry.name);
		if (entries.containsKey(entry.name))
			throw new IllegalArgumentException("Duplicate config value: " + entry.name);
		entries.put(entry.name, entry);
		if (requireRestart)
			entry.restart();
		last = entry;
		guiOrder.add(entry);
	}
	
	@Override public Entry<?, ?, ?, ?> getEntry(String name) {
		return entries.get(name);
	}
	
	@Override public boolean hasEntry(String name) {
		return entries.containsKey(name);
	}
	
	protected String translation(String name) {
		return modId + ".config." + path + "." + name;
	}
	
	protected String tooltip(String name) {
		return translation(name) + ".help";
	}
	
	@Override protected void translate(Entry<?, ?, ?, ?> entry) {
		entry.translate(translation(entry.name));
		entry.tooltip(tooltip(entry.name));
	}
	
	public SimpleConfigBuilder n(CategoryBuilder cat) {
		if (categories.containsKey(cat.name) || groups.containsKey(cat.name))
			throw new IllegalArgumentException("Duplicated config category: \"" + cat.name + "\"");
		categories.put(cat.name, cat);
		cat.setParent(this);
		if (requireRestart)
			cat.restart();
		return this;
	}
	
	@Override public SimpleConfigBuilder n(GroupBuilder group) {
		if (groups.containsKey(group.name) || categories.containsKey(group.name))
			throw new IllegalArgumentException("Duplicated config group: \"" + group.name + "\"");
		groups.put(group.name, group);
		guiOrder.add(group);
		group.setParent(defaultCategory);
		if (requireRestart)
			group.restart();
		return this;
	}
	
	/**
	 * Displays all translation keys in the Config GUI
	 * to aid in writing them<br>
	 * @deprecated Not deprecated, it's just a reminder to
	 * remove the method before releasing
	 */
	@Deprecated
	public SimpleConfigBuilder debugTranslations() {
		debugTranslations = true;
		return this;
	}
	
	public static class CategoryBuilder
	  extends AbstractSimpleConfigEntryHolderBuilder<CategoryBuilder> {
		
		protected SimpleConfigBuilder parent;
		protected final String name;
		protected String title;
		protected Class<?> configClass;
		
		protected @Nullable Consumer<Category> baker = null;
		
		protected String path;
		
		protected CategoryBuilder(String name) {
			this(name, null);
		}
		
		protected CategoryBuilder(String name, Class<?> configClass) {
			this.name = name;
			this.configClass = configClass;
		}
		
		protected void setParent(SimpleConfigBuilder parent) {
			this.parent = parent;
			this.path = parent.path + "." + name;
			this.title = parent.modId + ".config.category." + path;
			
			for (GroupBuilder group : groups.values())
				group.setParent(this);
		}
		
		public CategoryBuilder setBaker(Consumer<Category> baker) {
			this.baker = baker;
			return this;
		}
		
		@Override public void addEntry(Entry<?, ?, ?, ?> entry) {
			checkName(entry.name);
			if (entries.containsKey(entry.name))
				throw new IllegalArgumentException("Duplicate config value: " + entry.name);
			entries.put(entry.name, entry);
			if (requireRestart)
				entry.restart();
			last = entry;
			guiOrder.add(entry);
		}
		
		@Override public Entry<?, ?, ?, ?> getEntry(String name) {
			return entries.get(name);
		}
		
		@Override public boolean hasEntry(String name) {
			return entries.containsKey(name);
		}
		
		protected String translation(String name) {
			return parent.modId + ".config." + path + "." + name;
		}
		
		protected String tooltip(String name) {
			return translation(name) + ".help";
		}
		
		protected void translate(Entry<?, ?, ?, ?> entry) {
			entry.translate(translation(entry.name));
			entry.tooltip(tooltip(entry.name));
		}
		
		@Override public CategoryBuilder n(GroupBuilder group) {
			if (groups.containsKey(group.name))
				throw new IllegalArgumentException("Duplicated config group: \"" + group.name + "\"");
			groups.put(group.name, group);
			guiOrder.add(group);
			if (parent != null)
				group.setParent(this);
			if (requireRestart)
				group.restart();
			return this;
		}
		
		public Category build(SimpleConfig parent, ForgeConfigSpec.Builder specBuilder) {
			specBuilder.push(name);
			final Category cat = new Category(parent, name, title, baker);
			final Map<GroupBuilder, Group> built = new HashMap<>();
			final Map<String, Group> groups = new LinkedHashMap<>();
			final Map<String, ConfigValue<?>> specValues = new LinkedHashMap<>();
			for (Entry<?, ?, ?, ?> entry : entries.values()) {
				translate(entry);
				entry.setParent(cat);
				entry.buildConfigEntry(specBuilder).ifPresent(e -> specValues.put(entry.name, e));
			}
			final List<IGUIEntry> order = new ArrayList<>();
			for (GroupBuilder group : this.groups.values()) {
				final Group g = group.build(cat, specBuilder);
				built.put(group, g);
				groups.put(group.name, g);
			}
			guiOrder.stream().map(e -> e instanceof GroupBuilder ? built.get(e) : (IGUIEntry) e)
			  .forEachOrdered(order::add);
			cat.build(
			  unmodifiableMap(entries), unmodifiableMap(groups),
			  unmodifiableMap(specValues), unmodifiableList(order));
			specBuilder.pop();
			return cat;
		}
	}
	
	public static class GroupBuilder extends AbstractSimpleConfigEntryHolderBuilder<GroupBuilder>
	  implements IAbstractGUIEntry {
		protected CategoryBuilder category;
		protected final String name;
		
		protected String title;
		protected String tooltip;
		protected final boolean expanded;
		
		protected String path;
		
		public GroupBuilder(String name, boolean expanded) {
			this.name = name;
			this.expanded = expanded;
		}
		
		public void setParent(CategoryBuilder parent) {
			this.category = parent;
			this.path = parent.path + "." + name;
			final String modId = parent.parent.modId;
			this.title = modId + ".config.group." + path;
			this.tooltip = title + ".help";
			
			for (GroupBuilder group : groups.values())
				group.setParent(this);
		}
		
		public void setParent(GroupBuilder parent) {
			this.category = parent.category;
			this.path = parent.path + "." + name;
			final String modId = parent.category.parent.modId;
			this.title = modId + ".config.group." + path;
			this.tooltip = title + ".help";
			
			for (GroupBuilder group : groups.values())
				group.setParent(this);
		}
		
		@Override public GroupBuilder n(GroupBuilder nested) {
			if (groups.containsKey(nested.name))
				throw new IllegalArgumentException("Duplicated config group: \"" + nested.name + "\"");
			if (category != null)
				nested.setParent(this);
			groups.put(nested.name, nested);
			guiOrder.add(nested);
			if (requireRestart)
				nested.restart();
			return this;
		}
		
		@Override public void addEntry(Entry<?, ?, ?, ?> entry) {
			checkName(entry.name);
			if (entries.containsKey(entry.name))
				throw new IllegalArgumentException("Duplicate config value: " + entry.name);
			entries.put(entry.name, entry);
			if (requireRestart)
				entry.restart();
			last = entry;
			guiOrder.add(entry);
		}
		
		@Override public Entry<?, ?, ?, ?> getEntry(String name) {
			return entries.get(name);
		}
		
		@Override public boolean hasEntry(String name) {
			return entries.containsKey(name);
		}
		
		protected String translation(String name) {
			return category.parent.modId + ".config." + path + "." + name;
		}
		
		protected String tooltip(String name) {
			return translation(name) + ".help";
		}
		
		protected void translate(Entry<?, ?, ?, ?> entry) {
			entry.translate(translation(entry.name));
			entry.tooltip(tooltip(entry.name));
		}
		
		protected Group build(Group parent, ForgeConfigSpec.Builder specBuilder) {
			return build(null, parent, specBuilder);
		}
		
		protected Group build(Category parent, ForgeConfigSpec.Builder specBuilder) {
			return build(parent, null, specBuilder);
		}
		
		private Group build(
		  @Nullable Category parent, @Nullable Group groupParent,
		  ForgeConfigSpec.Builder specBuilder
		) {
			assert parent != null || groupParent != null;
			specBuilder.push(name);
			final Group group;
			if (parent != null)
				group = new Group(parent, name, title, tooltip, expanded);
			else group = new Group(groupParent, name, title, tooltip, expanded);
			final Map<GroupBuilder, Group> builtGroups = new HashMap<>();
			final Map<String, Group> groupMap = new LinkedHashMap<>();
			final Map<String, ConfigValue<?>> specValues = new LinkedHashMap<>();
			for (Entry<?, ?, ?, ?> entry : entries.values()) {
				translate(entry);
				entry.setParent(group);
				entry.buildConfigEntry(specBuilder).ifPresent(e -> specValues.put(entry.name, e));
			}
			for (String name : groups.keySet()) {
				GroupBuilder builder = groups.get(name);
				Group subGroup = builder.build(group, specBuilder);
				groupMap.put(name, subGroup);
				builtGroups.put(builder, subGroup);
			}
			final List<IGUIEntry> builtOrder = guiOrder.stream()
			  .map(e -> e instanceof GroupBuilder ? builtGroups.get(e) : (IGUIEntry) e)
			  .collect(Collectors.toList());
			group.build(
			  unmodifiableMap(entries), unmodifiableMap(specValues),
			  unmodifiableMap(groupMap), unmodifiableList(builtOrder));
			specBuilder.pop();
			return group;
		}
	}
	
	@SuppressWarnings("UnusedReturnValue")
	public SimpleConfig buildAndRegister() { return buildAndRegister(FMLJavaModLoadingContext.get().getModEventBus()); }
	
	protected void preBuildHook() {
		SimpleConfigClassParser.decorateBuilder(this);
	}
	
	@SuppressWarnings("UnusedReturnValue")
	public SimpleConfig buildAndRegister(IEventBus eventBus) {
		preBuildHook();
		if (type == Type.SERVER) {
			saver = FMLEnvironment.dist == Dist.DEDICATED_SERVER
			        ? (SimpleConfig::syncToClients)
			        : (SimpleConfig::syncToServer);
		} else {
			saver = SimpleConfig::checkRestart;
		}
		final SimpleConfig config = new SimpleConfig(
		  modId, type, title, baker, saver, configClass, debugTranslations);
		final Map<GroupBuilder, Group> built = new HashMap<>();
		final ForgeConfigSpec.Builder specBuilder = new ForgeConfigSpec.Builder();
		final Map<String, ConfigValue<?>> specValues = new LinkedHashMap<>();
		for (Entry<?, ?, ?, ?> entry : entries.values()) {
			translate(entry);
			entry.setParent(config);
			entry.buildConfigEntry(specBuilder).ifPresent(e -> specValues.put(entry.name, e));
		}
		final Map<String, Category> categoryMap = new LinkedHashMap<>();
		final Map<String, Group> groupMap = new LinkedHashMap<>();
		final List<IGUIEntry> order = new ArrayList<>();
		for (CategoryBuilder cat : categories.values())
			categoryMap.put(cat.name, cat.build(config, specBuilder));
		Category defaultCategory = this.defaultCategory.build(config, specBuilder);
		for (GroupBuilder group : groups.values()) {
			final Group g = group.build(defaultCategory, specBuilder);
			built.put(group, g);
			groupMap.put(group.name, g);
		}
		guiOrder.stream().map(e -> e instanceof GroupBuilder ? built.get(e) : (IGUIEntry) e)
		  .forEachOrdered(order::add);
		
		config.build(
		  unmodifiableMap(entries), unmodifiableMap(categoryMap),
		  unmodifiableMap(groupMap), unmodifiableMap(specValues),
		  unmodifiableList(order), specBuilder.build());
		ModLoadingContext.get().registerConfig(type, config.spec);
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			config.decorator = decorator;
			SimpleConfigGUI.registerConfig(config);
		});
		eventBus.register(config);
		return config;
	}
}
