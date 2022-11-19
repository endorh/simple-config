package endorh.simpleconfig.ui.impl;

import endorh.simpleconfig.api.SimpleConfig;
import endorh.simpleconfig.api.SimpleConfig.EditType;
import endorh.simpleconfig.ui.api.ConfigCategory;
import endorh.simpleconfig.ui.api.ConfigCategoryBuilder;
import endorh.simpleconfig.ui.api.ConfigScreenBuilder;
import endorh.simpleconfig.ui.gui.AbstractConfigScreen;
import endorh.simpleconfig.ui.gui.SimpleConfigScreen;
import endorh.simpleconfig.ui.hotkey.ConfigHotKey;
import endorh.simpleconfig.ui.impl.builders.ConfigCategoryBuilderImpl;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;

@OnlyIn(value = Dist.CLIENT)
@Internal public class ConfigScreenBuilderImpl implements ConfigScreenBuilder {
	protected Runnable savingRunnable;
	protected Runnable closingRunnable;
	protected String modId;
	protected Screen parent;
	protected ConfigHotKey editedConfigHotkey = null;
	protected Consumer<Boolean> hotKeySaver = null;
	protected Component title = new TranslatableComponent("simpleconfig.config.title");
	protected boolean editable = true;
	protected boolean transparentBackground = false;
	protected ResourceLocation defaultBackground = GuiComponent.BACKGROUND_LOCATION;
	protected Consumer<Screen> afterInitConsumer = screen -> {};
	protected final EnumMap<EditType, Map<String, ConfigCategoryBuilder>> categories =
	  Util.make(new EnumMap<>(EditType.class), m -> {
		  for (EditType type: SimpleConfig.EditType.values()) m.put(type, new LinkedHashMap<>());
	  });
	protected ConfigCategoryBuilder fallbackCategory = null;
	protected boolean alwaysShowTabs = false;
	protected @Nullable IConfigSnapshotHandler snapshotHandler;
	protected @Nullable ConfigScreenBuilder.IRemoteConfigProvider remoteConfigProvider;
	private ConfigCategoryBuilder selectedCategory;
	private @Nullable IConfigScreenGUIState previousGUIState = null;
	
	@Internal public ConfigScreenBuilderImpl(String modId) {
		this.modId = modId;
	}
	
	@Override public boolean isAlwaysShowTabs() {
		return alwaysShowTabs;
	}
	@Override public ConfigScreenBuilder setAlwaysShowTabs(boolean alwaysShowTabs) {
		this.alwaysShowTabs = alwaysShowTabs;
		return this;
	}
	
	@Override public ConfigScreenBuilder setTransparentBackground(boolean transparent) {
		transparentBackground = transparent;
		return this;
	}
	@Override public boolean hasTransparentBackground() {
		return transparentBackground;
	}
	
	@Override public ConfigScreenBuilder setSnapshotHandler(
	  IConfigSnapshotHandler handler
	) {
		snapshotHandler = handler;
		return this;
	}
	
	@Override public ConfigScreenBuilder setRemoteCommonConfigProvider(IRemoteConfigProvider provider) {
		remoteConfigProvider = provider;
		return this;
	}
	
	@Override public ConfigScreenBuilder setAfterInitConsumer(Consumer<Screen> afterInitConsumer) {
		this.afterInitConsumer = afterInitConsumer;
		return this;
	}
	
	@Override public ConfigScreenBuilder setFallbackCategory(ConfigCategoryBuilder fallbackCategory) {
		this.fallbackCategory = fallbackCategory;
		return this;
	}
	
	@Override public Screen getParentScreen() {
		return parent;
	}
	@Override public ConfigScreenBuilder setParentScreen(Screen parent) {
		this.parent = parent;
		return this;
	}
	
	@Override public Component getTitle() {
		return title;
	}
	@Override public ConfigScreenBuilder setTitle(Component title) {
		this.title = title;
		return this;
	}
	
	@Override public ConfigHotKey getEditedConfigHotKey() {
		return editedConfigHotkey;
	}
	
	@Override public Consumer<Boolean> getHotKeySaver() {
		return hotKeySaver;
	}
	
	@Override public ConfigScreenBuilder setEditedConfigHotKey(
	  ConfigHotKey hotkey, Consumer<Boolean> hotKeySaver
	) {
		this.editedConfigHotkey = hotkey;
		this.hotKeySaver = hotKeySaver;
		return this;
	}
	
	@Override public ConfigCategoryBuilder getSelectedCategory() {
		return selectedCategory;
	}
	
	@Override public ConfigScreenBuilder setSelectedCategory(ConfigCategoryBuilder category) {
		selectedCategory = category;
		return this;
	}
	
	@Override public @Nullable IConfigScreenGUIState getPreviousGUIState() {
		return previousGUIState;
	}
	
	@Override public ConfigScreenBuilder setPreviousGUIState(@Nullable IConfigScreenGUIState state) {
		previousGUIState = state;
		return this;
	}
	
	@Override public boolean isEditable() {
		return editable;
	}
	@Override public ConfigScreenBuilder setEditable(boolean editable) {
		this.editable = editable;
		return this;
	}
	
	@Override public List<ConfigCategoryBuilder> getCategories(EditType type) {
		return new ArrayList<>(categories.get(type).values());
	}
	
	@Override public ConfigCategoryBuilder getOrCreateCategory(String name, EditType type) {
		Map<String, ConfigCategoryBuilder> categories = this.categories.get(type);
		ConfigCategoryBuilder cat = categories.computeIfAbsent(
		  name, key -> new ConfigCategoryBuilderImpl(this, name, type));
		if (fallbackCategory == null) fallbackCategory = cat;
		return cat;
	}
	
	@Override public ConfigScreenBuilder removeCategory(String name, EditType type) {
		Map<String, ConfigCategoryBuilder> categories = this.categories.get(type);
		if (!categories.containsKey(name))
			throw new IllegalArgumentException("Category " + name + " does not exist");
		if (categories.get(name) == fallbackCategory)
			fallbackCategory = null;
		categories.remove(name);
		return this;
	}
	
	@Override public ConfigScreenBuilder removeCategoryIfExists(String name, EditType type) {
		Map<String, ConfigCategoryBuilder> categories = this.categories.get(type);
		if (categories.containsKey(name))
			removeCategory(name, type);
		return this;
	}
	
	@Override public boolean hasCategory(String name, EditType type) {
		Map<String, ConfigCategoryBuilder> categories = this.categories.get(type);
		return categories.containsKey(name);
	}
	
	@Override public ResourceLocation getDefaultBackgroundTexture() {
		return defaultBackground;
	}
	
	@Override public ConfigScreenBuilder setDefaultBackgroundTexture(ResourceLocation texture) {
		defaultBackground = texture;
		return this;
	}
	
	@Override public Runnable getSavingRunnable() {
		return savingRunnable;
	}
	@Override public ConfigScreenBuilder setSavingRunnable(Runnable runnable) {
		savingRunnable = runnable;
		return this;
	}
	
	@Override public Runnable getClosingRunnable() {
		return closingRunnable;
	}
	
	@Override public ConfigScreenBuilder setClosingRunnable(Runnable runnable) {
		closingRunnable = runnable;
		return this;
	}
	
	@Override public Consumer<Screen> getAfterInitConsumer() {
		return afterInitConsumer;
	}
	
	@Override public AbstractConfigScreen build() {
		if (categories.values().stream().allMatch(Map::isEmpty) || fallbackCategory == null)
			throw new IllegalStateException("Config screen without categories or fallback category");
		ConfigCategoryBuilder selectedBuilder =
		  selectedCategory != null? selectedCategory : fallbackCategory;
		ConfigCategory selected = null;
		EnumMap<EditType, List<ConfigCategory>> builtMap = new EnumMap<>(EditType.class);
		for (Entry<EditType, Map<String, ConfigCategoryBuilder>> e: categories.entrySet()) {
			List<ConfigCategory> categories = builtMap.computeIfAbsent(e.getKey(), k -> new ArrayList<>());
			for (ConfigCategoryBuilder builder: e.getValue().values()) {
				ConfigCategory built = builder.build();
				categories.add(built);
				if (builder == selectedBuilder) selected = built;
			}
		}
		AbstractConfigScreen screen = new SimpleConfigScreen(
		  parent, modId, title, builtMap.get(SimpleConfig.EditType.CLIENT), builtMap.get(
		  SimpleConfig.EditType.COMMON),
		  builtMap.get(SimpleConfig.EditType.SERVER_COMMON), builtMap.get(SimpleConfig.EditType.SERVER),
		  defaultBackground);
		screen.setEditedConfigHotKey(editedConfigHotkey, hotKeySaver);
		screen.setSavingRunnable(savingRunnable);
		screen.setClosingRunnable(closingRunnable);
		screen.setEditable(editable);
		screen.setSelectedCategory(selected);
		screen.setTransparentBackground(transparentBackground);
		screen.setAlwaysShowTabs(alwaysShowTabs);
		screen.setAfterInitConsumer(afterInitConsumer);
		screen.setSnapshotHandler(snapshotHandler);
		screen.setRemoteCommonConfigProvider(remoteConfigProvider);
		screen.loadConfigScreenGUIState(previousGUIState);
		return screen;
	}
	
	public static class ConfigScreenGUIState implements IConfigScreenGUIState {
		private EditType editedType;
		private final Map<EditType, String> selectedCategories = new EnumMap<>(EditType.class);
		private final Map<EditType, Map<String, IConfigCategoryGUIState>> categoryStates = new EnumMap<>(EditType.class);
		
		@Override public EditType getEditedType() {
			return editedType;
		}
		public void setEditedType(EditType editedType) {
			this.editedType = editedType;
		}
		
		@Override public Map<EditType, String> getSelectedCategories() {
			return selectedCategories;
		}
		
		@Override public Map<EditType, Map<String, IConfigCategoryGUIState>> getCategoryStates() {
			return categoryStates;
		}
		
		public static class ConfigCategoryGUIState implements IConfigCategoryGUIState {
			private final Map<String, Boolean> expandStates = new HashMap<>();
			private String selectedEntry;
			private int scrollOffset;
			
			@Override public Map<String, Boolean> getExpandStates() {
				return expandStates;
			}
			
			@Override public String getSelectedEntry() {
				return selectedEntry;
			}
			public void setSelectedEntry(String selectedEntry) {
				this.selectedEntry = selectedEntry;
			}
			
			@Override public int getScrollOffset() {
				return scrollOffset;
			}
			public void setScrollOffset(int scrollOffset) {
				this.scrollOffset = scrollOffset;
			}
		}
	}
}
