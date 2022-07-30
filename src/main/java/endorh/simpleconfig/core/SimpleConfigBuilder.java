package endorh.simpleconfig.core;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import endorh.simpleconfig.core.SimpleConfig.IGUIEntry;
import endorh.simpleconfig.core.SimpleConfig.IGUIEntryBuilder;
import endorh.simpleconfig.core.entry.Builders;
import endorh.simpleconfig.ui.api.ConfigCategory;
import endorh.simpleconfig.ui.api.ConfigScreenBuilder;
import endorh.simpleconfig.ui.gui.Icon;
import net.minecraft.command.CommandSource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

/**
 * Create a {@link SimpleConfig} using a chained method call<br>
 * Use {@link SimpleConfigBuilder#add(String, AbstractConfigEntryBuilder)}
 * to add entries to the config (in order)<br>
 * Use {@link SimpleConfigBuilder#n(CategoryBuilder)} to add
 * subcategories to the config, each with its own tab<br>
 * Use {@link SimpleConfigBuilder#n(GroupBuilder)} to add
 * subgroups to the config, each under a dropdown entry in the GUI.
 * Groups may contain other groups.<br><br>
 * You may create categories and groups with the
 * {@link SimpleConfig#category(String, Class)} and
 * {@link SimpleConfig#group(String, boolean)} methods<br>
 * You may create entries to add using the builder methods under
 * {@link Builders}<br>
 * Entries can be further configured with their own builder methods
 */
public class SimpleConfigBuilder
  extends AbstractSimpleConfigEntryHolderBuilder<SimpleConfigBuilder> {
	protected final String modId;
	protected final ModConfig.Type type;
	protected @Nullable LiteralArgumentBuilder<CommandSource> commandRoot = null;
	
	protected final String title;
	
	protected final Map<String, CategoryBuilder> categories = new LinkedHashMap<>();
	protected final Map<CategoryBuilder, Integer> categoryOrder = new HashMap<>();
	protected final CategoryBuilder defaultCategory;
	
	protected String path;
	
	protected final @Nullable Class<?> configClass;
	protected @Nullable Consumer<SimpleConfig> baker = null;
	protected @Nullable Consumer<SimpleConfig> saver = null;
	protected @Nullable BiConsumer<SimpleConfig, ConfigScreenBuilder> decorator = null;
	protected @Nullable ResourceLocation background = null;
	protected boolean transparent = true;
	
	protected SimpleConfigBuilder(String modId, Type type) { this(modId, type, null); }
	
	protected SimpleConfigBuilder(String modId, Type type, @Nullable Class<?> configClass) {
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
	
	/**
	 * Set the baker method for this config<br>
	 * You may also define a '{@code bake}' static method
	 * in the config class accepting a {@link SimpleConfig}
	 * and it will be set automatically as the baker (but you
	 * may not define it and also call this method)
	 */
	public SimpleConfigBuilder withBaker(Consumer<SimpleConfig> baker) {
		this.baker = baker;
		return this;
	}
	
	/**
	 * Set the default background for all categories
	 * @see #withBackground(ResourceLocation)
	 * @see #withGUIDecorator(BiConsumer)
	 */
	public SimpleConfigBuilder withBackground(String resourceName) {
		return withBackground(new ResourceLocation(resourceName));
	}
	
	/**
	 * Set the default background for all categories
	 * @see #withBackground(String)
	 * @see #withIcon(Icon)
	 * @see #withColor(int)
	 * @see #withGUIDecorator(BiConsumer)
	 */
	public SimpleConfigBuilder withBackground(ResourceLocation background) {
		this.background = background;
		return this;
	}
	
	/**
	 * Set the icon for the default category.<br>
	 * Doesn't affect other categories.<br>
	 * The icon is displayed in the tab button for the category, when more than
	 * one category is present.
	 * @param icon Icon to display. Use {@link Icon#EMPTY} to display no icon (default).
	 * @see #withColor(int)
	 * @see #withBackground(ResourceLocation)
	 * @see #withGUIDecorator(BiConsumer)
	 */
	public SimpleConfigBuilder withIcon(Icon icon) {
		defaultCategory.withIcon(icon);
		return this;
	}
	
	/**
	 * Set the color for the default category.<br>
	 * Doesn't affect other categories.<br>
	 * The color affects the tint applied to the tab button for the category,
	 * visible when more than one category is present.
	 * @param tint Color tint to use, in ARGB format. It's recommended
	 *             a transparency of 0x80, so the button background is
	 *             visible behind the color. A value of 0 means no tint.
	 * @see #withColor(int)
	 * @see #withBackground(ResourceLocation)
	 * @see #withGUIDecorator(BiConsumer)
	 */
	public SimpleConfigBuilder withColor(int tint) {
		defaultCategory.withColor(tint);
		return this;
	}
	
	/**
	 * Use the solid background too when ingame<br>
	 * By default, config GUIs are transparent when ingame
	 */
	public SimpleConfigBuilder withSolidInGameBackground() {
		this.transparent = false;
		return this;
	}
	
	/**
	 * Configure a decorator to modify the Cloth Config API's {@link ConfigScreenBuilder}
	 * just when a config GUI is being built<br>
	 * @see SimpleConfigBuilder#withBackground(ResourceLocation)
	 */
	@OnlyIn(Dist.CLIENT)
	public SimpleConfigBuilder withGUIDecorator(BiConsumer<SimpleConfig, ConfigScreenBuilder> decorator) {
		this.decorator = decorator;
		return this;
	}
	
	/**
	 * Register the config command at the given command root<br>
	 * The config command will still be accessible at {@code /config ⟨sub⟩ ⟨modid⟩}<br>
	 */
	public SimpleConfigBuilder withCommandRoot(LiteralArgumentBuilder<CommandSource> root) {
		commandRoot = root;
		return this;
	}
	
	@Override
	protected void checkName(String name) {
		super.checkName(name);
		if (categories.containsKey(name) || type.name().toLowerCase().equals(name))
			throw new IllegalArgumentException("Duplicate config entry name: " + name);
	}
	
	@Override public SimpleConfigBuilder restart() {
		super.restart();
		categories.values().forEach(CategoryBuilder::restart);
		return this;
	}
	
	@Override protected void addEntry(
	  int order, String name, AbstractConfigEntryBuilder<?, ?, ?, ?, ?> entry
	) {
		checkName(name);
		if (entries.containsKey(name) || groups.containsKey(name) || categories.containsKey(name))
			throw new IllegalArgumentException("Duplicate name for entry: " + name);
		entries.put(name, entry);
		if (requireRestart)
			entry.restart();
		guiOrder.put(name, order);
	}
	
	@Override protected AbstractConfigEntryBuilder<?, ?, ?, ?, ?> getEntry(String name) {
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
	
	@Override protected void translate(AbstractConfigEntry<?, ?, ?, ?> entry) {
		entry.setTranslation(translation(entry.name));
		entry.setTooltip(tooltip(entry.name));
	}
	
	public SimpleConfigBuilder n(CategoryBuilder cat) {
		return n(cat, 0);
	}
	
	public SimpleConfigBuilder n(CategoryBuilder cat, int index) {
		if (categories.containsKey(cat.name) || groups.containsKey(cat.name))
			throw new IllegalArgumentException("Duplicated config category: \"" + cat.name + "\"");
		categories.put(cat.name, cat);
		categoryOrder.put(cat, index);
		cat.setParent(this);
		if (requireRestart)
			cat.restart();
		return this;
	}
	
	@Override public SimpleConfigBuilder n(GroupBuilder group, int index) {
		if (groups.containsKey(group.name) || categories.containsKey(group.name) || entries.containsKey(group.name))
			throw new IllegalArgumentException("Duplicated name for group: \"" + group.name + "\"");
		groups.put(group.name, group);
		guiOrder.put(group.name, index);
		group.setParent(defaultCategory);
		if (requireRestart)
			group.restart();
		return this;
	}
	
	/**
	 * Builder for a {@link SimpleConfigCategory}<br>
	 * Use {@link CategoryBuilder#add(String, AbstractConfigEntryBuilder)}
	 * to add new entries to the category<br>
	 * Use {@link CategoryBuilder#n(GroupBuilder)} to add
	 * subgroups to the category, which may contain further groups<br><br>
	 * Create subgroups using {@link SimpleConfig#group(String, boolean)},
	 * and entries with the methods under {@link Builders}
	 */
	public static class CategoryBuilder
	  extends AbstractSimpleConfigEntryHolderBuilder<CategoryBuilder> {
		
		protected SimpleConfigBuilder parent;
		protected final String name;
		protected String title;
		protected Icon icon = Icon.EMPTY;
		protected int tint = 0;
		protected Class<?> configClass;
		
		protected @Nullable Consumer<SimpleConfigCategory> baker = null;
		
		protected String path;
		
		protected @Nullable BiConsumer<SimpleConfigCategory, ConfigCategory> decorator;
		protected @Nullable ResourceLocation background;
		
		protected CategoryBuilder(String name) {
			this(name, null);
		}
		
		protected CategoryBuilder(String name, Class<?> configClass) {
			this.name = name;
			this.path = name;
			this.configClass = configClass;
		}
		
		protected void setParent(SimpleConfigBuilder parent) {
			this.parent = parent;
			this.path = parent.path + "." + name;
			this.title = parent.modId + ".config." + path;
			
			for (GroupBuilder group : groups.values())
				group.setParent(this);
		}
		
		/**
		 * Set the baker method for this config category<br>
		 * You may also define a '{@code bake}' static method
		 * in the backing class accepting a {@link SimpleConfig}
		 * and it will be set automatically as the baker (but you
		 * may not define it and also call this method)
		 */
		public CategoryBuilder withBaker(Consumer<SimpleConfigCategory> baker) {
			this.baker = baker;
			return this;
		}
		
		/**
		 * Set the background texture to be used
		 * @see #withBackground(ResourceLocation)
		 * @see #withIcon(Icon)
		 * @see #withColor(int)
		 * @see #withGUIDecorator(BiConsumer)
		 */
		public CategoryBuilder withBackground(String resourceName) {
			return withBackground(new ResourceLocation(resourceName));
		}
		
		/**
		 * Set the background texture to be used
		 * @see #withBackground(String)
		 * @see #withIcon(Icon)
		 * @see #withColor(int)
		 * @see #withGUIDecorator(BiConsumer)
		 */
		public CategoryBuilder withBackground(ResourceLocation background) {
			this.background = background;
			return this;
		}
		
		/**
		 * Set the icon of this category.<br>
		 * Icons are displayed in the tab buttons when more than one category is present.<br>
		 * Use {@link Icon#EMPTY} to disable the icon (default).
		 * @see #withColor(int)
		 * @see #withBackground(ResourceLocation)
		 * @see #withGUIDecorator(BiConsumer)
		 */
		public CategoryBuilder withIcon(Icon icon) {
			this.icon = icon;
			return this;
		}
		
		/**
		 * Set the color of this category.<br>
		 * Affects the tint applied to the tab button for this category,
		 * which will be visible when multiple categories are present.<br>
		 * @param tint Color tint to use, in ARGB format. It's recommended
		 *             a transparency of 0x80, so the button background is
		 *             visible behind the color. A value of 0 means no tint.
		 * @see #withIcon(Icon)
		 * @see #withBackground(ResourceLocation)
		 * @see #withGUIDecorator(BiConsumer)
		 */
		public CategoryBuilder withColor(int tint) {
			this.tint = tint;
			return this;
		}
		
		/**
		 * Set a decorator that will run when creating the category GUI<br>
		 * @see #withBackground(ResourceLocation)
		 * @see #withIcon(Icon)
		 * @see #withColor(int)
		 */
		@OnlyIn(Dist.CLIENT)
		public CategoryBuilder withGUIDecorator(BiConsumer<SimpleConfigCategory, ConfigCategory> decorator) {
			this.decorator = decorator;
			return this;
		}
		
		@Override protected void addEntry(
		  int order, String name, AbstractConfigEntryBuilder<?, ?, ?, ?, ?> entry
		) {
			checkName(name);
			if (entries.containsKey(name) || groups.containsKey(name))
				throw new IllegalArgumentException("Duplicate name for entry: " + name);
			entries.put(name, entry);
			if (requireRestart)
				entry.restart();
			guiOrder.put(name, order);
		}
		
		@Override protected AbstractConfigEntryBuilder<?, ?, ?, ?, ?> getEntry(String name) {
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
		
		protected void translate(AbstractConfigEntry<?, ?, ?, ?> entry) {
			entry.setTranslation(translation(entry.name));
			entry.setTooltip(tooltip(entry.name));
		}
		
		@Override public CategoryBuilder n(GroupBuilder group, int index) {
			if (groups.containsKey(group.name))
				throw new IllegalArgumentException("Duplicated config group: \"" + group.name + "\"");
			groups.put(group.name, group);
			guiOrder.put(group.name, index);
			if (parent != null)
				group.setParent(this);
			if (requireRestart)
				group.restart();
			return this;
		}
		
		protected SimpleConfigCategory build(SimpleConfig parent, ForgeConfigSpec.Builder specBuilder) {
			specBuilder.push(name);
			final SimpleConfigCategory cat = new SimpleConfigCategory(parent, name, title, baker);
			final Map<String, SimpleConfigGroup> groups = new LinkedHashMap<>();
			final Map<String, AbstractConfigEntry<?, ?, ?, ?>> entriesByName = new LinkedHashMap<>();
			entries.forEach((name, value) -> {
				final AbstractConfigEntry<?, ?, ?, ?> entry = value.build(cat, name);
				entriesByName.put(name, entry);
				translate(entry);
				entry.backingField = getBackingField(name);
				entry.secondaryBackingFields = getSecondaryBackingFields(name);
				entry.buildConfig(specBuilder);
			});
			for (GroupBuilder group : this.groups.values()) {
				final SimpleConfigGroup g = group.build(cat, specBuilder);
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
			specBuilder.pop();
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
	 * Use {@link GroupBuilder#add(String, AbstractConfigEntryBuilder)}
	 * to add new entries to the group<br>
	 * Use {@link GroupBuilder#n(GroupBuilder)} to add
	 * subgroups to this group<br><br>
	 *
	 * You may create new entries with the methods under
	 * {@link Builders}
	 */
	public static class GroupBuilder extends AbstractSimpleConfigEntryHolderBuilder<GroupBuilder>
	  implements IGUIEntryBuilder {
		protected CategoryBuilder category;
		protected final String name;
		
		protected String title;
		protected String tooltip;
		protected final boolean expanded;
		protected @Nullable Consumer<SimpleConfigGroup> baker = null;
		
		protected String path;
		protected String heldEntryName;
		protected AbstractConfigEntryBuilder<?, ?, ?, ?, ?> heldEntryBuilder = null;
		
		protected GroupBuilder(String name, boolean expanded) {
			this.name = name;
			this.path = name;
			this.expanded = expanded;
		}
		
		protected void setParent(CategoryBuilder parent) {
			this.category = parent;
			this.path = parent.path + "." + name;
			final String modId = parent.parent.modId;
			this.title = modId + ".config." + path;
			this.tooltip = title + ":help";
			
			for (GroupBuilder group : groups.values())
				group.setParent(this);
		}
		
		protected void setParent(GroupBuilder parent) {
			this.category = parent.category;
			this.path = parent.path + "." + name;
			final String modId = parent.category.parent.modId;
			this.title = modId + ".config." + path;
			this.tooltip = title + ":help";
			
			for (GroupBuilder group : groups.values())
				group.setParent(this);
		}
		
		@Override public GroupBuilder n(GroupBuilder nested, int index) {
			if (groups.containsKey(nested.name))
				throw new IllegalArgumentException("Duplicated config group: \"" + nested.name + "\"");
			if (category != null)
				nested.setParent(this);
			groups.put(nested.name, nested);
			guiOrder.put(nested.name, index);
			if (requireRestart)
				nested.restart();
			return this;
		}
		
		public GroupBuilder withBaker(Consumer<SimpleConfigGroup> baker) {
			this.baker = baker;
			return this;
		}
		
		public <
		  V, C, G, E extends AbstractConfigEntry<V, C, G, E> & IKeyEntry<G>,
		  B extends AbstractConfigEntryBuilder<V, C, G, E, B>
		> GroupBuilder caption(String name, B entry) {
			if (heldEntryBuilder != null)
				throw new IllegalArgumentException("Attempt to declare two caption entries for the same config group: " + path);
			this.heldEntryBuilder = entry;
			this.heldEntryName = name;
			addEntry(0, name, entry);
			guiOrder.remove(name);
			return this;
		}
		
		@Override protected void addEntry(
		  int order, String name, AbstractConfigEntryBuilder<?, ?, ?, ?, ?> entry
		) {
			checkName(name);
			if (entries.containsKey(name))
				throw new IllegalArgumentException("Duplicate config entry name: " + name);
			entries.put(name, entry);
			if (requireRestart)
				entry.restart();
			guiOrder.put(name, order);
		}
		
		@Override protected AbstractConfigEntryBuilder<?, ?, ?, ?, ?> getEntry(String name) {
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
		
		protected void translate(AbstractConfigEntry<?, ?, ?, ?> entry) {
			entry.setTranslation(translation(entry.name));
			entry.setTooltip(tooltip(entry.name));
		}
		
		protected SimpleConfigGroup build(SimpleConfigGroup parent, ForgeConfigSpec.Builder specBuilder) {
			return build(null, parent, specBuilder);
		}
		
		protected SimpleConfigGroup build(SimpleConfigCategory parent, ForgeConfigSpec.Builder specBuilder) {
			return build(parent, null, specBuilder);
		}
		
		private SimpleConfigGroup build(
		  @Nullable SimpleConfigCategory parent, @Nullable SimpleConfigGroup groupParent,
		  ForgeConfigSpec.Builder specBuilder
		) {
			assert parent != null || groupParent != null;
			specBuilder.push(name);
			final SimpleConfigGroup group;
			if (parent != null)
				group = new SimpleConfigGroup(parent, name, title, tooltip, expanded, baker);
			else group = new SimpleConfigGroup(groupParent, name, title, tooltip, expanded, baker);
			final Map<String, SimpleConfigGroup> groupMap = new LinkedHashMap<>();
			final Map<String, AbstractConfigEntry<?, ?, ?, ?>> entriesByName = new LinkedHashMap<>();
			final AbstractConfigEntry<?, ?, ?, ?> heldEntry =
			  heldEntryBuilder != null ? heldEntryBuilder.build(group, heldEntryName) : null;
			if (heldEntry != null) {
				entriesByName.put(heldEntryName, heldEntry);
				translate(heldEntry);
				heldEntry.backingField = getBackingField(heldEntryName);
				heldEntry.secondaryBackingFields = getSecondaryBackingFields(heldEntryName);
				heldEntry.buildConfig(specBuilder);
			}
			entries.forEach((name, builder) -> {
				if (builder == heldEntryBuilder) return;
				final AbstractConfigEntry<?, ?, ?, ?> entry = builder.build(group, name);
				entriesByName.put(name, entry);
				translate(entry);
				entry.backingField = getBackingField(name);
				entry.secondaryBackingFields = getSecondaryBackingFields(name);
				entry.buildConfig(specBuilder);
			});
			for (String name : groups.keySet()) {
				GroupBuilder builder = groups.get(name);
				SimpleConfigGroup subGroup = builder.build(group, specBuilder);
				groupMap.put(name, subGroup);
			}
			final List<IGUIEntry> builtOrder = guiOrder.keySet().stream().sorted(
			  Comparator.comparing(a -> guiOrder.getOrDefault(a, 0))
			).map(
			  n -> groupMap.containsKey(n)? groupMap.get(n) : entriesByName.get(n)
			).collect(Collectors.toList());
			group.build(
			  unmodifiableMap(entriesByName), unmodifiableMap(groupMap),
			  unmodifiableList(builtOrder), heldEntry);
			specBuilder.pop();
			return group;
		}
		
		@Override
		public String toString() {
			return "Group[" + path + "]";
		}
	}
	
	/**
	 * Build the actual config and register it within the Forge system<br><br>
	 * <b>If your mod uses a different language than Java</b> you will need to
	 * also pass in your mod event bus as an argument to
	 * {@link #buildAndRegister(IEventBus)}
	 * @return The built config, which is also received by the baker
	 */
	public SimpleConfig buildAndRegister() {
		try {
			return buildAndRegister(FMLJavaModLoadingContext.get().getModEventBus());
		} catch (ClassCastException e) {
			throw new IllegalArgumentException(
			  "Cannot call SimpleConfigBuilder#buildAndRegister in non-Java mod without passing " +
			  "the mod event bus. Pass your mod event bus to buildAndRegister.");
		}
	}
	
	/**
	 * Applies the final decorations before the config building<br>
	 * Parses the backing classes
	 */
	protected void preBuildHook() {
		SimpleConfigClassParser.decorateBuilder(this);
	}
	
	/**
	 * Build the actual config and register it within the Forge system<br><br>
	 * <i>If your mod uses Java as its language</i> you don't need to pass
	 * the mod event bus
	 * @param modEventBus Your mod's language provider's mod event bus
	 * @return The built config, which is also received by the baker
	 */
	@SuppressWarnings("UnusedReturnValue")
	public SimpleConfig buildAndRegister(IEventBus modEventBus) {
		preBuildHook();
		if (type == Type.SERVER) {
			saver = FMLEnvironment.dist == Dist.DEDICATED_SERVER
			        ? (SimpleConfig::syncToClients)
			        : (SimpleConfig::syncToServer);
		} else {
			saver = SimpleConfig::checkRestart;
		}
		final SimpleConfig config = new SimpleConfig(modId, type, title, baker, saver, configClass);
		final ForgeConfigSpec.Builder specBuilder = new ForgeConfigSpec.Builder();
		final Map<String, AbstractConfigEntry<?, ?, ?, ?>> entriesByName = new LinkedHashMap<>();
		final Map<String, SimpleConfigCategory> categoryMap = new LinkedHashMap<>();
		final Map<String, SimpleConfigGroup> groupMap = new LinkedHashMap<>();
		entries.forEach((name, value) -> {
			final AbstractConfigEntry<?, ?, ?, ?> entry = value.build(config, name);
			entriesByName.put(name, entry);
			translate(entry);
			entry.backingField = getBackingField(name);
			entry.secondaryBackingFields = getSecondaryBackingFields(name);
			entry.buildConfig(specBuilder);
		});
		SimpleConfigCategory defaultCategory = this.defaultCategory.build(config, specBuilder);
		for (GroupBuilder group : groups.values()) {
			final SimpleConfigGroup g = group.build(defaultCategory, specBuilder);
			groupMap.put(group.name, g);
		}
		categories.values().stream().sorted(
		  Comparator.comparing(c -> categoryOrder.getOrDefault(c, 0))
		).forEachOrdered(c -> categoryMap.put(c.name, c.build(config, specBuilder)));
		final List<IGUIEntry> order = guiOrder.keySet().stream().sorted(
		  Comparator.comparing(a -> guiOrder.getOrDefault(a, 0))
		).map(
		  n -> groupMap.containsKey(n)? groupMap.get(n) : entriesByName.get(n)
		).collect(Collectors.toList());
		config.build(
		  unmodifiableMap(entriesByName), unmodifiableMap(categoryMap),
		  unmodifiableMap(groupMap), unmodifiableList(order), specBuilder.build(),
		  defaultCategory.icon, defaultCategory.color, commandRoot);
		ModContainer modContainer = ModLoadingContext.get().getActiveContainer();
		SimpleConfigModConfig modConfig = new SimpleConfigModConfig(config, modContainer);
		config.build(modConfig);
		modContainer.addConfig(modConfig);
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			config.decorator = decorator;
			config.background = background;
			config.transparent = transparent;
			SimpleConfigGUIManager.registerConfig(config);
		});
		modEventBus.register(config);
		return config;
	}
	
	@Override
	public String toString() {
		return "SimpleConfig[" + path + "]";
	}
}
