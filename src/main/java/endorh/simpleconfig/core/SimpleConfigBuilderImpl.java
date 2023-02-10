package endorh.simpleconfig.core;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import endorh.simpleconfig.api.*;
import endorh.simpleconfig.api.SimpleConfig.Type;
import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.core.SimpleConfigImpl.IGUIEntry;
import endorh.simpleconfig.ui.api.ConfigScreenBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.ReportedException;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Collections.*;

/**
 * Create a {@link SimpleConfigImpl} using a chained method call<br>
 * Use {@link SimpleConfigBuilder#add(String, ConfigEntryBuilder)}
 * to add entries to the config (in order)<br>
 * Use {@link SimpleConfigBuilder#n(ConfigCategoryBuilder)} to add
 * subcategories to the config, each with its own tab<br>
 * Use {@link SimpleConfigBuilder#n(ConfigGroupBuilder)} to add
 * subgroups to the config, each under a dropdown entry in the GUI.
 * Groups may contain other groups.<br><br>
 * You may create categories and groups with the
 * {@link ConfigBuilderFactoryProxy#category(String, Class)} and
 * {@link ConfigBuilderFactoryProxy#group(String, boolean)} methods<br>
 * You may create entries to add using the builder methods under
 * {@link ConfigBuilderFactoryProxy}<br>
 * Entries can be further configured with their own builder methods
 */
public class SimpleConfigBuilderImpl
  extends AbstractSimpleConfigEntryHolderBuilder<SimpleConfigBuilder>
  implements SimpleConfigBuilder {
	protected final String modId;
	protected final Type type;
	protected @Nullable LiteralArgumentBuilder<CommandSource> commandRoot = null;
	
	protected final String title;
	
	protected final Map<String, CategoryBuilder> categories = new LinkedHashMap<>();
	protected final Map<CategoryBuilder, Integer> categoryOrder = new HashMap<>();
	protected final CategoryBuilder defaultCategory;
	
	protected String path;
	
	protected final @Nullable Class<?> configClass;
	protected @Nullable Consumer<SimpleConfig> baker = null;
	protected @Nullable Consumer<SimpleConfigImpl> saver = null;
	protected @Nullable BiConsumer<SimpleConfig, ConfigScreenBuilder> decorator = null;
	protected @Nullable Predicate<SimpleConfigCategory> categoryFilter = null;
	protected @Nullable ResourceLocation background = null;
	protected boolean transparent = true;
	protected boolean isWrapper;
	
	protected SimpleConfigBuilderImpl(String modId, Type type) { this(modId, type, null); }
	
	protected SimpleConfigBuilderImpl(String modId, Type type, @Nullable Class<?> configClass) {
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
	
	@Override @Contract("_ -> this") public @NotNull SimpleConfigBuilderImpl withBaker(
	  Consumer<SimpleConfig> baker
	) {
		this.baker = baker;
		return this;
	}
	
	@Override @Contract("_ -> this") public @NotNull SimpleConfigBuilderImpl withBackground(String resourceName) {
		return withBackground(new ResourceLocation(resourceName));
	}
	
	@Override @Contract("_ -> this") public @NotNull SimpleConfigBuilderImpl withBackground(
	  ResourceLocation background
	) {
		this.background = background;
		return this;
	}
	
	@Override @Contract("_ -> this") public @NotNull SimpleConfigBuilderImpl withIcon(Icon icon) {
		defaultCategory.withIcon(icon);
		return this;
	}
	
	@Override @Contract("_ -> this") public @NotNull SimpleConfigBuilderImpl withColor(int tint) {
		defaultCategory.withColor(tint);
		return this;
	}
	
	@Override @Contract("-> this") public @NotNull SimpleConfigBuilderImpl withSolidInGameBackground() {
		transparent = false;
		return this;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Contract("_ -> this") public SimpleConfigBuilderImpl withGUIDecorator(
	  BiConsumer<SimpleConfig, ConfigScreenBuilder> decorator
	) {
		this.decorator = decorator;
		return this;
	}
	
	@Contract("_ -> this") @Override public @NotNull SimpleConfigBuilderImpl withDynamicGUICategoryFilter(
	  Predicate<SimpleConfigCategory> categoryFilter
	) {
		this.categoryFilter = categoryFilter;
		return this;
	}
	
	@Override @Contract("_ -> this") public @NotNull SimpleConfigBuilderImpl withCommandRoot(
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
	
	@Contract("-> this") @Override public @NotNull SimpleConfigBuilderImpl restart() {
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
	
	@Override @Contract("_ -> this") public @NotNull SimpleConfigBuilderImpl n(ConfigCategoryBuilder cat) {
		return n(cat, 0);
	}
	
	@Override @Contract("_, _ -> this") public @NotNull SimpleConfigBuilderImpl n(ConfigCategoryBuilder cat, int index) {
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
	@Override public @NotNull SimpleConfigBuilderImpl n(ConfigGroupBuilder group, int index) {
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
	 * Builder for a {@link SimpleConfigCategoryImpl}<br>
	 * Use {@link ConfigCategoryBuilder#add(String, ConfigEntryBuilder)}
	 * to add new entries to the category<br>
	 * Use {@link ConfigCategoryBuilder#n(ConfigGroupBuilder)} to add
	 * subgroups to the category, which may contain further groups<br><br>
	 * Create subgroups using {@link ConfigBuilderFactoryProxy#group(String, boolean)},
	 * and entries with the methods under {@link ConfigBuilderFactoryProxy}
	 */
	public static class CategoryBuilder
	  extends AbstractSimpleConfigEntryHolderBuilder<ConfigCategoryBuilder>
	  implements ConfigCategoryBuilder {
		protected SimpleConfigBuilderImpl parent;
		protected final String name;
		protected String title;
		protected @Nullable Supplier<List<ITextComponent>> description = null;
		protected Icon icon = Icon.EMPTY;
		protected int tint = 0;
		protected Class<?> configClass;
		
		protected @Nullable Consumer<SimpleConfigCategory> baker = null;
		
		protected String path;
		
		protected @Nullable BiConsumer<SimpleConfigCategory, endorh.simpleconfig.ui.api.ConfigCategoryBuilder> decorator;
		protected @Nullable ResourceLocation background;
		
		protected CategoryBuilder(String name) {
			this(name, null);
		}
		
		protected CategoryBuilder(String name, Class<?> configClass) {
			this.name = name;
			path = name;
			this.configClass = configClass;
		}
		
		protected void setParent(SimpleConfigBuilderImpl parent) {
			this.parent = parent;
			path = parent.path + "." + name;
			title = parent.modId + ".config." + path;
			
			for (GroupBuilder group : groups.values())
				group.setParent(this);
		}
		
		@Override @Contract("_ -> this") public @NotNull CategoryBuilder withDescription(
		  Supplier<List<ITextComponent>> description
		) {
			this.description = description;
			return this;
		}
		
		@Override @Contract("_ -> this") public @NotNull CategoryBuilder withBaker(
		  Consumer<SimpleConfigCategory> baker
		) {
			this.baker = baker;
			return this;
		}
		
		@Override @Contract("_ -> this") public @NotNull CategoryBuilder withBackground(String resourceName) {
			return withBackground(new ResourceLocation(resourceName));
		}
		
		@Override @Contract("_ -> this") public @NotNull CategoryBuilder withBackground(
		  ResourceLocation background
		) {
			this.background = background;
			return this;
		}
		
		@Override @Contract("_ -> this") public @NotNull CategoryBuilder withIcon(Icon icon) {
			this.icon = icon;
			return this;
		}
		
		@Override @Contract("_ -> this") public @NotNull CategoryBuilder withColor(int tint) {
			this.tint = tint;
			return this;
		}
		
		@OnlyIn(Dist.CLIENT)
		@Contract("_ -> this") public CategoryBuilder withGUIDecorator(
		  BiConsumer<SimpleConfigCategory, endorh.simpleconfig.ui.api.ConfigCategoryBuilder> decorator
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
		@Override public @NotNull CategoryBuilder n(ConfigGroupBuilder group, int index) {
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
		
		protected SimpleConfigCategoryImpl build(
		  SimpleConfigImpl parent, ConfigValueBuilder builder, boolean isRoot
		) {
			if (!isRoot) builder.enterSection(name);
			final SimpleConfigCategoryImpl
			  cat = new SimpleConfigCategoryImpl(parent, name, title, isRoot, baker);
			final Map<String, SimpleConfigGroupImpl> groups = new LinkedHashMap<>();
			final Map<String, AbstractConfigEntry<?, ?, ?>> entriesByName = new LinkedHashMap<>();
			entries.forEach((name, value) -> {
				if (builder.canBuildEntry(name)) {
					final AbstractConfigEntry<?, ?, ?> entry = value.build(cat, name);
					entriesByName.put(name, entry);
					buildTranslations(entry);
					entry.backingField = getBackingField(name);
					entry.secondaryBackingFields = getSecondaryBackingFields(name);
					builder.build(value, entry);
				}
			});
			boolean forceExpanded = this.groups.size() == 1 && entries.isEmpty();
			for (GroupBuilder group : this.groups.values()) {
				if (forceExpanded) group.expanded = true;
				final SimpleConfigGroupImpl g = group.build(cat, builder);
				groups.put(group.name, g);
			}
			final List<IGUIEntry> order = guiOrder.keySet().stream().sorted(
			  Comparator.comparing(a -> guiOrder.getOrDefault(a, 0))
			).map(
			  n -> groups.containsKey(n)? groups.get(n) : entriesByName.get(n)
			).collect(Collectors.toList());
			cat.build(
			  entriesByName, groups,
			  order, description, icon, tint);
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
	 * Builder for a {@link SimpleConfigGroupImpl}<br>
	 * Use {@link ConfigGroupBuilder#add(String, ConfigEntryBuilder)}
	 * to add new entries to the group<br>
	 * Use {@link ConfigGroupBuilder#n(ConfigGroupBuilder)} to add
	 * subgroups to this group<br><br>
	 *
	 * You may create new entries with the methods under
	 * {@link ConfigBuilderFactoryProxy}
	 */
	public static class GroupBuilder
	  extends AbstractSimpleConfigEntryHolderBuilder<ConfigGroupBuilder>
	  implements ConfigGroupBuilder {
		protected CategoryBuilder category;
		protected final String name;
		
		protected String title;
		protected String tooltip;
		protected boolean expanded;
		protected @Nullable Consumer<SimpleConfigGroup> baker = null;
		
		protected String path;
		protected String captionName;
		protected AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?> captionBuilder = null;
		
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
		@Override public @NotNull GroupBuilder n(ConfigGroupBuilder nested, int index) {
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
		
		@Override @Contract("_ -> this") public @NotNull GroupBuilder withBaker(
		  Consumer<SimpleConfigGroup> baker
		) {
			this.baker = baker;
			return this;
		}
		
		@Override @Contract("_, _ -> this")
		public <
		  V, C, G,
		  B extends ConfigEntryBuilder<V, C, G, B> & AtomicEntryBuilder
		> @NotNull GroupBuilder caption(String name, B entry) {
			if (captionBuilder != null)
				throw new IllegalArgumentException("Attempt to declare two caption entries for the same config group: " + path);
			if (!(entry instanceof AbstractConfigEntryBuilder)) throw new IllegalArgumentException(
			  "ConfigEntryBuilder not instance of AbstractConfigEntryBuilder");
			captionBuilder = (AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?>) entry;
			captionName = name;
			addEntry(0, name, captionBuilder);
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
		
		@Internal public SimpleConfigGroupImpl build(SimpleConfigGroupImpl parent, ConfigValueBuilder builder) {
			return build(null, null, parent, builder);
		}
		
		@Internal public SimpleConfigGroupImpl build(SimpleConfigCategoryImpl parent, ConfigValueBuilder builder) {
			return build(null, parent, null, builder);
		}
		
		@Internal public SimpleConfigGroupImpl build(SimpleConfigImpl root, ConfigValueBuilder builder) {
			return build(root, null, null, builder);
		}
		
		@Internal public SimpleConfigGroupImpl build(ConfigEntryHolder holder, ConfigValueBuilder builder) {
			if (holder instanceof SimpleConfigImpl) return build((SimpleConfigImpl) holder, builder);
			if (holder instanceof SimpleConfigCategoryImpl) return build((SimpleConfigCategoryImpl) holder, builder);
			if (holder instanceof SimpleConfigGroupImpl) return build((SimpleConfigGroupImpl) holder, builder);
			throw new IllegalArgumentException(
			  "ConfigEntryHolder is not instance of SimpleConfigImpl, SimpleConfigCategoryImpl, or SimpleConfigGroupImpl");
		}
		
		private SimpleConfigGroupImpl build(
		  @Nullable SimpleConfigImpl root, @Nullable SimpleConfigCategoryImpl category,
		  @Nullable SimpleConfigGroupImpl groupParent, ConfigValueBuilder builder
		) {
			assert root != null || category != null || groupParent != null;
			builder.enterSection(name);
			final SimpleConfigGroupImpl group;
			if (category != null) {
				group = new SimpleConfigGroupImpl(category, name, title, tooltip, expanded, baker);
			} else if (groupParent != null) {
				group = new SimpleConfigGroupImpl(groupParent, name, title, tooltip, expanded, baker);
			} else group = new SimpleConfigGroupImpl(root, name, title, tooltip, expanded, baker);
			final Map<String, SimpleConfigGroupImpl> groupMap = new LinkedHashMap<>();
			final Map<String, AbstractConfigEntry<?, ?, ?>> entriesByName = new LinkedHashMap<>();
			final AbstractConfigEntry<?, ?, ?> heldEntry =
			  captionBuilder != null? captionBuilder.build(group, captionName) : null;
			if (heldEntry != null && builder.canBuildEntry(captionName)) {
				entriesByName.put(captionName, heldEntry);
				buildTranslations(heldEntry);
				heldEntry.backingField = getBackingField(captionName);
				heldEntry.secondaryBackingFields = getSecondaryBackingFields(captionName);
				builder.build(captionBuilder, heldEntry);
			}
			entries.forEach((name, b) -> {
				if (b == captionBuilder || !builder.canBuildEntry(name)) return;
				final AbstractConfigEntry<?, ?, ?> entry = b.build(group, name);
				entriesByName.put(name, entry);
				buildTranslations(entry);
				entry.backingField = getBackingField(name);
				entry.secondaryBackingFields = getSecondaryBackingFields(name);
				builder.build(b, entry);
			});
			boolean forceExpanded = groups.size() == 1 && entries.isEmpty();
			for (String name : groups.keySet()) {
				GroupBuilder b = groups.get(name);
				if (forceExpanded) b.expanded = true;
				if (builder.canBuildSection(name)) {
					SimpleConfigGroupImpl subGroup = b.build(group, builder);
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
	
	@Override public @NotNull SimpleConfigImpl buildAndRegister() {
		try {
			return buildAndRegister(FMLJavaModLoadingContext.get().getModEventBus());
		} catch (ClassCastException e) {
			throw new IllegalArgumentException(
			  "Cannot call SimpleConfigBuilder#buildAndRegister in non-Java mod without passing " +
			  "the mod event bus. Pass your mod event bus to buildAndRegister.");
		}
	}
	
	@Internal public @NotNull SimpleConfigBuilderImpl markAsWrapper() {
		this.isWrapper = true;
		return this;
	}
	
	@Override public @NotNull SimpleConfigImpl buildAndRegister(@NotNull IEventBus modEventBus) {
		return buildAndRegister(modEventBus, new ForgeConfigSpecConfigValueBuilder());
	}
	
	@Internal public SimpleConfigImpl buildAndRegister(
	  IEventBus modEventBus, ConfigValueBuilder builder
	) {
		try {
			preBuildHook();
			if (type == SimpleConfig.Type.SERVER) {
				saver = FMLEnvironment.dist == Dist.DEDICATED_SERVER
				        ? SimpleConfigImpl::syncToClients
				        : SimpleConfigImpl::syncToServer;
			} else if (type == SimpleConfig.Type.COMMON) {
				saver = FMLEnvironment.dist == Dist.DEDICATED_SERVER
				        ? SimpleConfigImpl::syncToClients
				        : SimpleConfigImpl::checkRestart;
			} else if (FMLEnvironment.dist != Dist.DEDICATED_SERVER)
				saver = SimpleConfigImpl::checkRestart;
			final SimpleConfigImpl
			  config = new SimpleConfigImpl(modId, type, title, baker, saver, configClass);
			final Map<String, AbstractConfigEntry<?, ?, ?>> entriesByName = new LinkedHashMap<>();
			final Map<String, SimpleConfigCategoryImpl> categoryMap = new LinkedHashMap<>();
			final Map<String, SimpleConfigGroupImpl> groupMap = new LinkedHashMap<>();
			entries.forEach((name, value) -> {
				if (builder.canBuildEntry(name)) {
					final AbstractConfigEntry<?, ?, ?> entry = value.build(config, name);
					entriesByName.put(name, entry);
					buildTranslations(entry);
					entry.backingField = getBackingField(name);
					entry.secondaryBackingFields = getSecondaryBackingFields(name);
					builder.build(value, entry);
				}
			});
			SimpleConfigCategoryImpl defaultCategory =
			  this.defaultCategory.build(config, builder, true);
			for (GroupBuilder group: groups.values()) {
				if (builder.canBuildSection(group.name)) {
					final SimpleConfigGroupImpl g = group.build(defaultCategory, builder);
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
				config.categoryFilter = categoryFilter;
				config.background = background;
				config.transparent = transparent;
				SimpleConfigGUIManagerImpl.INSTANCE.registerConfig(config);
			});
			if (modEventBus != null) modEventBus.register(config);
			return config;
		} catch (RuntimeException e) {
			throw new ReportedException(CrashReport.makeCrashReport(
			  e, "Building config for mod " + modId));
		}
	}
	
	@Internal public static abstract class ConfigValueBuilder {
		public abstract void buildModConfig(SimpleConfigImpl config);
		public boolean canBuildEntry(String name) {
			return true;
		}
		public boolean canBuildSection(String name) {
			return true;
		}
		public abstract void build(
		  AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?> entryBuilder,
		  AbstractConfigEntry<?, ?, ?> entry);
		public void enterSection(String name) {}
		public void exitSection() {}
		public abstract Pair<ForgeConfigSpec, List<ForgeConfigSpec>> build();
	}
	
	protected static class ForgeConfigSpecConfigValueBuilder extends ConfigValueBuilder {
		private final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		
		@Override public void buildModConfig(SimpleConfigImpl config) {
			ModContainer modContainer =
			  ModList.get().getModContainerById(config.getModId()).orElseThrow(
				 () -> new IllegalStateException("Missing mod ID for config: " + config.getModId()));
			SimpleConfigModConfig modConfig = new SimpleConfigModConfig(config, modContainer);
			config.build(modContainer, modConfig);
			modContainer.addConfig(modConfig);
		}
		
		@Override public void build(
		  AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?> entryBuilder,
		  AbstractConfigEntry<?, ?, ?> entry
		) {
			entry.buildConfig(builder);
		}
		@Override public void enterSection(String name) {
			builder.push(name);
		}
		@Override public void exitSection() {
			builder.pop();
		}
		@Override public Pair<ForgeConfigSpec, List<ForgeConfigSpec>> build() {
			return Pair.of(builder.build(), emptyList());
		}
	}
	
	@Override
	public String toString() {
		return "SimpleConfig[" + path + "]";
	}
}
