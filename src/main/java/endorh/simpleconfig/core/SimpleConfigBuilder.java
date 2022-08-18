package endorh.simpleconfig.core;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import endorh.simpleconfig.api.*;
import endorh.simpleconfig.api.ISimpleConfig.Type;
import endorh.simpleconfig.core.SimpleConfig.IGUIEntry;
import endorh.simpleconfig.ui.api.ConfigCategoryBuilder;
import endorh.simpleconfig.ui.api.ConfigScreenBuilder;
import endorh.simpleconfig.ui.icon.Icon;
import net.minecraft.command.CommandSource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

/**
 * Create a {@link SimpleConfig} using a chained method call<br>
 * Use {@link ISimpleConfigBuilder#add(String, ConfigEntryBuilder)}
 * to add entries to the config (in order)<br>
 * Use {@link ISimpleConfigBuilder#n(ICategoryBuilder)} to add
 * subcategories to the config, each with its own tab<br>
 * Use {@link ISimpleConfigBuilder#n(IGroupBuilder)} to add
 * subgroups to the config, each under a dropdown entry in the GUI.
 * Groups may contain other groups.<br><br>
 * You may create categories and groups with the
 * {@link ConfigBuilderFactoryProxy#category(String, Class)} and
 * {@link ConfigBuilderFactoryProxy#group(String, boolean)} methods<br>
 * You may create entries to add using the builder methods under
 * {@link ConfigBuilderFactoryProxy}<br>
 * Entries can be further configured with their own builder methods
 */
public class SimpleConfigBuilder
  extends AbstractSimpleConfigEntryHolderBuilder<ISimpleConfigBuilder>
  implements ISimpleConfigBuilder {
	protected final String modId;
	protected final Type type;
	protected @Nullable LiteralArgumentBuilder<CommandSource> commandRoot = null;
	
	protected final String title;
	
	protected final Map<String, CategoryBuilder> categories = new LinkedHashMap<>();
	protected final Map<CategoryBuilder, Integer> categoryOrder = new HashMap<>();
	protected final CategoryBuilder defaultCategory;
	
	protected String path;
	
	protected final @Nullable Class<?> configClass;
	protected @Nullable Consumer<ISimpleConfig> baker = null;
	protected @Nullable Consumer<SimpleConfig> saver = null;
	protected @Nullable BiConsumer<ISimpleConfig, ConfigScreenBuilder> decorator = null;
	protected @Nullable ResourceLocation background = null;
	protected boolean transparent = true;
	
	protected SimpleConfigBuilder(String modId, Type type) { this(modId, type, null); }
	
	protected SimpleConfigBuilder(String modId, Type type, @Nullable Class<?> configClass) {
		this.modId = modId;
		this.type = type;
		this.configClass = configClass;
		String classifier = type.name().toLowerCase();
		title = modId + ".config.category." + classifier;
		path = classifier;
		
		defaultCategory = new CategoryBuilder(classifier);
		defaultCategory.setParent(this);
		defaultCategory.path = path;
		defaultCategory.title = modId + ".config.category." + path;
	}
	
	@Override @Contract("_ -> this") public SimpleConfigBuilder withBaker(
	  Consumer<ISimpleConfig> baker
	) {
		this.baker = baker;
		return this;
	}
	
	@Override @Contract("_ -> this") public SimpleConfigBuilder withBackground(String resourceName) {
		return withBackground(new ResourceLocation(resourceName));
	}
	
	@Override @Contract("_ -> this") public SimpleConfigBuilder withBackground(
	  ResourceLocation background
	) {
		this.background = background;
		return this;
	}
	
	@Override @Contract("_ -> this") public SimpleConfigBuilder withIcon(Icon icon) {
		defaultCategory.withIcon(icon);
		return this;
	}
	
	@Override @Contract("_ -> this") public SimpleConfigBuilder withColor(int tint) {
		defaultCategory.withColor(tint);
		return this;
	}
	
	@Override @Contract("-> this") public SimpleConfigBuilder withSolidInGameBackground() {
		transparent = false;
		return this;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Contract("_ -> this") public SimpleConfigBuilder withGUIDecorator(
	  BiConsumer<ISimpleConfig, ConfigScreenBuilder> decorator
	) {
		this.decorator = decorator;
		return this;
	}
	
	@Override @Contract("_ -> this") public SimpleConfigBuilder withCommandRoot(
	  LiteralArgumentBuilder<CommandSource> root
	) {
		commandRoot = root;
		return this;
	}
	
	@Override protected void checkName(String name) {
		super.checkName(name);
		if (categories.containsKey(name)) throw new IllegalArgumentException(
		  "Duplicate config entry name: " + name);
	}
	
	@Contract("-> this") @Override public SimpleConfigBuilder restart() {
		super.restart();
		categories.values().forEach(CategoryBuilder::restart);
		return this;
	}
	
	@Override protected void addEntry(
	  int order, String name, AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?> entry
	) {
		checkName(name);
		if (entries.containsKey(name) || groups.containsKey(name) || categories.containsKey(name))
			throw new IllegalArgumentException("Duplicate name for entry: " + name);
		if (requireRestart) entry = (AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?>) entry.restart();
		entries.put(name, entry);
		guiOrder.put(name, order);
	}
	
	@Override protected AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?> getEntry(String name) {
		return entries.get(name);
	}
	
	@Override protected boolean hasEntry(String name) {
		return entries.containsKey(name);
	}
	
	protected String translation(String name) {
		return modId + ".config." + path + "." + name;
	}
	
	protected String tooltip(String name) {
		return translation(name) + ":help";
	}
	
	@Override protected void buildTranslations(AbstractConfigEntry<?, ?, ?> entry) {
		if (entry.getTranslation() == null)
			entry.setTranslation(translation(entry.name));
		if (entry.getTooltipKey() == null)
			entry.setTooltipKey(tooltip(entry.name));
	}
	
	@Override @Contract("_ -> this") public SimpleConfigBuilder n(ICategoryBuilder cat) {
		return n(cat, 0);
	}
	
	@Override @Contract("_, _ -> this") public SimpleConfigBuilder n(ICategoryBuilder cat, int index) {
		if (!(cat instanceof CategoryBuilder)) throw new IllegalArgumentException(
		  "Category must be a CategoryBuilder");
		CategoryBuilder c = (CategoryBuilder) cat;
		checkName(c.name);
		categories.put(c.name, c);
		categoryOrder.put(c, index);
		c.setParent(this);
		if (requireRestart) c.restart();
		return this;
	}
	
	@Contract("_, _ -> this")
	@Override public SimpleConfigBuilder n(IGroupBuilder group, int index) {
		if (!(group instanceof GroupBuilder)) throw new IllegalArgumentException(
		  "Group must be a GroupBuilder");
		GroupBuilder g = (GroupBuilder) group;
		checkName(g.name);
		groups.put(g.name, g);
		guiOrder.put(g.name, index);
		g.setParent(defaultCategory);
		if (requireRestart)
			g.restart();
		return this;
	}
	
	
	
	/**
	 * Builder for a {@link SimpleConfigCategory}<br>
	 * Use {@link ICategoryBuilder#add(String, ConfigEntryBuilder)}
	 * to add new entries to the category<br>
	 * Use {@link ICategoryBuilder#n(IGroupBuilder)} to add
	 * subgroups to the category, which may contain further groups<br><br>
	 * Create subgroups using {@link ConfigBuilderFactoryProxy#group(String, boolean)},
	 * and entries with the methods under {@link ConfigBuilderFactoryProxy}
	 */
	public static class CategoryBuilder
	  extends AbstractSimpleConfigEntryHolderBuilder<ICategoryBuilder>
	  implements ICategoryBuilder {
		protected SimpleConfigBuilder parent;
		protected final String name;
		protected String title;
		protected Icon icon = Icon.EMPTY;
		protected int tint = 0;
		protected Class<?> configClass;
		
		protected @Nullable Consumer<ISimpleConfigCategory> baker = null;
		
		protected String path;
		
		protected @Nullable BiConsumer<ISimpleConfigCategory, ConfigCategoryBuilder> decorator;
		protected @Nullable ResourceLocation background;
		
		protected CategoryBuilder(String name) {
			this(name, null);
		}
		
		protected CategoryBuilder(String name, Class<?> configClass) {
			this.name = name;
			path = name;
			this.configClass = configClass;
		}
		
		protected void setParent(SimpleConfigBuilder parent) {
			this.parent = parent;
			path = parent.path + "." + name;
			title = parent.modId + ".config." + path;
			
			for (GroupBuilder group : groups.values())
				group.setParent(this);
		}
		
		@Override @Contract("_ -> this") public CategoryBuilder withBaker(
		  Consumer<ISimpleConfigCategory> baker
		) {
			this.baker = baker;
			return this;
		}
		
		@Override @Contract("_ -> this") public CategoryBuilder withBackground(String resourceName) {
			return withBackground(new ResourceLocation(resourceName));
		}
		
		@Override @Contract("_ -> this") public CategoryBuilder withBackground(
		  ResourceLocation background
		) {
			this.background = background;
			return this;
		}
		
		@Override @Contract("_ -> this") public CategoryBuilder withIcon(Icon icon) {
			this.icon = icon;
			return this;
		}
		
		@Override @Contract("_ -> this") public CategoryBuilder withColor(int tint) {
			this.tint = tint;
			return this;
		}
		
		@OnlyIn(Dist.CLIENT)
		@Contract("_ -> this") public CategoryBuilder withGUIDecorator(
		  BiConsumer<ISimpleConfigCategory, ConfigCategoryBuilder> decorator
		) {
			this.decorator = decorator;
			return this;
		}
		
		@Override protected void addEntry(
		  int order, String name, AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?> entry
		) {
			checkName(name);
			if (entries.containsKey(name) || groups.containsKey(name))
				throw new IllegalArgumentException("Duplicate name for entry: " + name);
			if (requireRestart) entry = (AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?>) entry.restart();
			entries.put(name, entry);
			guiOrder.put(name, order);
		}
		
		@Override protected AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?> getEntry(String name) {
			return entries.get(name);
		}
		
		@Override protected boolean hasEntry(String name) {
			return entries.containsKey(name);
		}
		
		protected String translation(String name) {
			return parent.modId + ".config." + path + "." + name;
		}
		
		protected String tooltip(String name) {
			return translation(name) + ":help";
		}
		
		@Override protected void buildTranslations(AbstractConfigEntry<?, ?, ?> entry) {
			if (entry.getTranslation() == null)
				entry.setTranslation(translation(entry.name));
			if (entry.getTooltipKey() == null)
				entry.setTooltipKey(tooltip(entry.name));
		}
		
		@Contract("_, _ -> this")
		@Override public CategoryBuilder n(IGroupBuilder group, int index) {
			if (!(group instanceof GroupBuilder)) throw new IllegalArgumentException(
			  "Group must be a GroupBuilder");
			GroupBuilder g = (GroupBuilder) group;
			if (groups.containsKey(g.name))
				throw new IllegalArgumentException("Duplicated config group: \"" + g.name + "\"");
			groups.put(g.name, g);
			guiOrder.put(g.name, index);
			if (parent != null)
				g.setParent(this);
			if (requireRestart)
				g.restart();
			return this;
		}
		
		protected SimpleConfigCategory build(
		  SimpleConfig parent, ConfigValueBuilder builder, boolean isRoot
		) {
			if (!isRoot) builder.enterSection(name);
			final SimpleConfigCategory cat = new SimpleConfigCategory(parent, name, title, isRoot, baker);
			final Map<String, SimpleConfigGroup> groups = new LinkedHashMap<>();
			final Map<String, AbstractConfigEntry<?, ?, ?>> entriesByName = new LinkedHashMap<>();
			entries.forEach((name, value) -> {
				if (builder.canBuildEntry(name)) {
					final AbstractConfigEntry<?, ?, ?> entry = value.build(cat, name);
					entriesByName.put(name, entry);
					buildTranslations(entry);
					entry.backingField = getBackingField(name);
					entry.secondaryBackingFields = getSecondaryBackingFields(name);
					builder.build(entry);
				}
			});
			for (GroupBuilder group : this.groups.values()) {
				final SimpleConfigGroup g = group.build(cat, builder);
				groups.put(group.name, g);
			}
			final List<IGUIEntry> order = guiOrder.keySet().stream().sorted(
			  Comparator.comparing(a -> guiOrder.getOrDefault(a, 0))
			).map(
			  n -> groups.containsKey(n)? groups.get(n) : entriesByName.get(n)
			).collect(Collectors.toList());
			cat.build(
			  unmodifiableMap(entriesByName), unmodifiableMap(groups),
			  unmodifiableList(order), icon, tint);
			if (!isRoot) builder.exitSection();
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
				cat.decorator = decorator;
				cat.background = background;
			});
			return cat;
		}
		
		@Override
		public String toString() {
			return "Category[" + path + "]";
		}
	}
	
	/**
	 * Builder for a {@link SimpleConfigGroup}<br>
	 * Use {@link IGroupBuilder#add(String, ConfigEntryBuilder)}
	 * to add new entries to the group<br>
	 * Use {@link IGroupBuilder#n(IGroupBuilder)} to add
	 * subgroups to this group<br><br>
	 *
	 * You may create new entries with the methods under
	 * {@link ConfigBuilderFactoryProxy}
	 */
	public static class GroupBuilder
	  extends AbstractSimpleConfigEntryHolderBuilder<IGroupBuilder>
	  implements IGroupBuilder {
		protected CategoryBuilder category;
		protected final String name;
		
		protected String title;
		protected String tooltip;
		protected final boolean expanded;
		protected @Nullable Consumer<ISimpleConfigGroup> baker = null;
		
		protected String path;
		protected String heldEntryName;
		protected AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?> heldEntryBuilder = null;
		
		protected GroupBuilder(String name, boolean expanded) {
			this.name = name;
			path = name;
			this.expanded = expanded;
		}
		
		protected void setParent(CategoryBuilder parent) {
			category = parent;
			path = parent.path + "." + name;
			final String modId = parent.parent.modId;
			title = modId + ".config." + path;
			tooltip = title + ":help";
			
			for (GroupBuilder group : groups.values())
				group.setParent(this);
		}
		
		protected void setParent(GroupBuilder parent) {
			category = parent.category;
			path = parent.path + "." + name;
			final String modId = parent.category.parent.modId;
			title = modId + ".config." + path;
			tooltip = title + ":help";
			
			for (GroupBuilder group : groups.values())
				group.setParent(this);
		}
		
		@Contract("_, _ -> this")
		@Override public GroupBuilder n(IGroupBuilder nested, int index) {
			if (!(nested instanceof GroupBuilder)) throw new IllegalArgumentException(
			  "Group must be a GroupBuilder");
			GroupBuilder n = (GroupBuilder) nested;
			if (groups.containsKey(n.name))
				throw new IllegalArgumentException("Duplicated config group: \"" + n.name + "\"");
			if (category != null)
				n.setParent(this);
			groups.put(n.name, n);
			guiOrder.put(n.name, index);
			if (requireRestart)
				n.restart();
			return this;
		}
		
		@Override @Contract("_ -> this") public GroupBuilder withBaker(
		  Consumer<ISimpleConfigGroup> baker
		) {
			this.baker = baker;
			return this;
		}
		
		@Override @Contract("_, _ -> this")
		public <
		  V, C, G,
		  B extends ConfigEntryBuilder<V, C, G, B>
		> GroupBuilder caption(String name, B entry) {
			if (heldEntryBuilder != null)
				throw new IllegalArgumentException("Attempt to declare two caption entries for the same config group: " + path);
			if (!(entry instanceof AbstractConfigEntryBuilder)) throw new IllegalArgumentException(
			  "ConfigEntryBuilder not instance of AbstractConfigEntryBuilder");
			heldEntryBuilder = (AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?>) entry;
			heldEntryName = name;
			addEntry(0, name, heldEntryBuilder);
			guiOrder.remove(name);
			return this;
		}
		
		@Override protected void addEntry(
		  int order, String name, AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?> entry
		) {
			checkName(name);
			if (entries.containsKey(name))
				throw new IllegalArgumentException("Duplicate config entry name: " + name);
			if (requireRestart) entry = (AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?>) entry.restart();
			entries.put(name, entry);
			guiOrder.put(name, order);
		}
		
		@Override protected AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?> getEntry(String name) {
			return entries.get(name);
		}
		
		@Override protected boolean hasEntry(String name) {
			return entries.containsKey(name);
		}
		
		protected String translation(String name) {
			return category.parent.modId + ".config." + path + "." + name;
		}
		
		protected String tooltip(String name) {
			return translation(name) + ":help";
		}
		
		@Override protected void buildTranslations(AbstractConfigEntry<?, ?, ?> entry) {
			if (entry.getTranslation() == null)
				entry.setTranslation(translation(entry.name));
			if (entry.getTooltipKey() == null)
				entry.setTooltipKey(tooltip(entry.name));
		}
		
		protected SimpleConfigGroup build(SimpleConfigGroup parent, ConfigValueBuilder builder) {
			return build(null, parent, builder);
		}
		
		protected SimpleConfigGroup build(SimpleConfigCategory parent, ConfigValueBuilder builder) {
			return build(parent, null, builder);
		}
		
		private SimpleConfigGroup build(
		  @Nullable SimpleConfigCategory parent, @Nullable SimpleConfigGroup groupParent,
		  ConfigValueBuilder builder
		) {
			assert parent != null || groupParent != null;
			builder.enterSection(name);
			final SimpleConfigGroup group;
			if (parent != null)
				group = new SimpleConfigGroup(parent, name, title, tooltip, expanded, baker);
			else group = new SimpleConfigGroup(groupParent, name, title, tooltip, expanded, baker);
			final Map<String, SimpleConfigGroup> groupMap = new LinkedHashMap<>();
			final Map<String, AbstractConfigEntry<?, ?, ?>> entriesByName = new LinkedHashMap<>();
			final AbstractConfigEntry<?, ?, ?> heldEntry =
			  heldEntryBuilder != null ? heldEntryBuilder.build(group, heldEntryName) : null;
			if (heldEntry != null && builder.canBuildEntry(heldEntryName)) {
				entriesByName.put(heldEntryName, heldEntry);
				buildTranslations(heldEntry);
				heldEntry.backingField = getBackingField(heldEntryName);
				heldEntry.secondaryBackingFields = getSecondaryBackingFields(heldEntryName);
				builder.build(heldEntry);
			}
			entries.forEach((name, b) -> {
				if (b == heldEntryBuilder || !builder.canBuildEntry(name)) return;
				final AbstractConfigEntry<?, ?, ?> entry = b.build(group, name);
				entriesByName.put(name, entry);
				buildTranslations(entry);
				entry.backingField = getBackingField(name);
				entry.secondaryBackingFields = getSecondaryBackingFields(name);
				builder.build(entry);
			});
			for (String name : groups.keySet()) {
				GroupBuilder b = groups.get(name);
				if (builder.canBuildSection(name)) {
					SimpleConfigGroup subGroup = b.build(group, builder);
					groupMap.put(name, subGroup);
				}
			}
			final List<IGUIEntry> builtOrder = guiOrder.keySet().stream().sorted(
			  Comparator.comparing(a -> guiOrder.getOrDefault(a, 0))
			).map(
			  n -> groupMap.containsKey(n)? groupMap.get(n) : entriesByName.get(n)
			).collect(Collectors.toList());
			group.build(
			  unmodifiableMap(entriesByName), unmodifiableMap(groupMap),
			  unmodifiableList(builtOrder), heldEntry);
			builder.exitSection();
			return group;
		}
		
		@Override
		public String toString() {
			return "Group[" + path + "]";
		}
	}
	
	/**
	 * Applies the final decorations before the config building<br>
	 * Parses the backing classes
	 */
	protected void preBuildHook() {
		SimpleConfigClassParser.decorateBuilder(this);
	}
	
	@Override public SimpleConfig buildAndRegister() {
		try {
			return buildAndRegister(FMLJavaModLoadingContext.get().getModEventBus());
		} catch (ClassCastException e) {
			throw new IllegalArgumentException(
			  "Cannot call SimpleConfigBuilder#buildAndRegister in non-Java mod without passing " +
			  "the mod event bus. Pass your mod event bus to buildAndRegister.");
		}
	}
	
	@Override public SimpleConfig buildAndRegister(@NotNull IEventBus modEventBus) {
		return buildAndRegister(modEventBus, new ForgeConfigSpecConfigValueBuilder());
	}
	
	@Internal protected SimpleConfig buildAndRegister(IEventBus modEventBus, ConfigValueBuilder builder) {
		preBuildHook();
		if (type == ISimpleConfig.Type.SERVER) {
			saver = FMLEnvironment.dist == Dist.DEDICATED_SERVER
			        ? SimpleConfig::syncToClients
			        : SimpleConfig::syncToServer;
		} else if (type == ISimpleConfig.Type.COMMON) {
			saver = FMLEnvironment.dist == Dist.DEDICATED_SERVER
			        ? SimpleConfig::syncToClients
			        : SimpleConfig::checkRestart;
		} else if (FMLEnvironment.dist != Dist.DEDICATED_SERVER)
			saver = SimpleConfig::checkRestart;
		final SimpleConfig config = new SimpleConfig(modId, type, title, baker, saver, configClass);
		final Map<String, AbstractConfigEntry<?, ?, ?>> entriesByName = new LinkedHashMap<>();
		final Map<String, SimpleConfigCategory> categoryMap = new LinkedHashMap<>();
		final Map<String, SimpleConfigGroup> groupMap = new LinkedHashMap<>();
		entries.forEach((name, value) -> {
			if (builder.canBuildEntry(name)) {
				final AbstractConfigEntry<?, ?, ?> entry = value.build(config, name);
				entriesByName.put(name, entry);
				buildTranslations(entry);
				entry.backingField = getBackingField(name);
				entry.secondaryBackingFields = getSecondaryBackingFields(name);
				builder.build(entry);
			}
		});
		SimpleConfigCategory defaultCategory = this.defaultCategory.build(config, builder, true);
		for (GroupBuilder group : groups.values()) {
			if (builder.canBuildSection(group.name)) {
				final SimpleConfigGroup g = group.build(defaultCategory, builder);
				groupMap.put(group.name, g);
			}
		}
		categories.values().stream().sorted(
		  Comparator.comparing(c -> categoryOrder.getOrDefault(c, 0))
		).forEachOrdered(c -> {
			if (builder.canBuildSection(c.name))
				categoryMap.put(c.name, c.build(config, builder, false));
		});
		final List<IGUIEntry> order = guiOrder.keySet().stream().sorted(
		  Comparator.comparing(a -> guiOrder.getOrDefault(a, 0))
		).map(
		  n -> groupMap.containsKey(n)? groupMap.get(n) : entriesByName.get(n)
		).collect(Collectors.toList());
		config.build(
		  unmodifiableMap(entriesByName), unmodifiableMap(categoryMap),
		  unmodifiableMap(groupMap), unmodifiableList(order), builder.build(),
		  defaultCategory.icon, defaultCategory.color, commandRoot);
		builder.buildModConfig(config);
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			config.decorator = decorator;
			config.background = background;
			config.transparent = transparent;
			SimpleConfigGUIManager.registerConfig(config);
		});
		if (modEventBus != null) modEventBus.register(config);
		return config;
	}
	
	protected static abstract class ConfigValueBuilder {
		abstract void buildModConfig(SimpleConfig config);
		boolean canBuildEntry(String name) {
			return true;
		}
		boolean canBuildSection(String name) {
			return true;
		}
		abstract void build(AbstractConfigEntry<?, ?, ?> entry);
		void enterSection(String name) {}
		void exitSection() {}
		abstract ForgeConfigSpec build();
	}
	
	protected static class ForgeConfigSpecConfigValueBuilder extends ConfigValueBuilder {
		private final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		
		@Override void buildModConfig(SimpleConfig config) {
			ModContainer modContainer = ModLoadingContext.get().getActiveContainer();
			SimpleConfigModConfig modConfig = new SimpleConfigModConfig(config, modContainer);
			config.build(modContainer, modConfig);
			modContainer.addConfig(modConfig);
		}
		
		@Override void build(AbstractConfigEntry<?, ?, ?> entry) {
			entry.buildConfig(builder);
		}
		
		@Override void enterSection(String name) {
			builder.push(name);
		}
		
		@Override void exitSection() {
			builder.pop();
		}
		
		@Override ForgeConfigSpec build() {
			return builder.build();
		}
	}
	
	@Override
	public String toString() {
		return "SimpleConfig[" + path + "]";
	}
}
