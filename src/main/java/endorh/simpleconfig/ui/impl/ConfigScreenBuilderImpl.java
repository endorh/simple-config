package endorh.simpleconfig.ui.impl;

import endorh.simpleconfig.core.SimpleConfig.EditType;
import endorh.simpleconfig.ui.ConfigCategoryBuilder;
import endorh.simpleconfig.ui.api.ConfigCategory;
import endorh.simpleconfig.ui.api.ConfigScreenBuilder;
import endorh.simpleconfig.ui.gui.AbstractConfigScreen;
import endorh.simpleconfig.ui.gui.SimpleConfigScreen;
import endorh.simpleconfig.ui.hotkey.ConfigHotKey;
import endorh.simpleconfig.ui.impl.builders.ConfigCategoryBuilderImpl;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
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
	protected ITextComponent title = new TranslationTextComponent("text.cloth-config.config");
	protected boolean editable = true;
	protected boolean transparentBackground = false;
	protected ResourceLocation defaultBackground = AbstractGui.BACKGROUND_LOCATION;
	protected Consumer<Screen> afterInitConsumer = screen -> {};
	protected final EnumMap<EditType, Map<String, ConfigCategoryBuilder>> categories =
	  Util.make(new EnumMap<>(EditType.class), m -> {
		  for (EditType type: EditType.values()) m.put(type, new LinkedHashMap<>());
	  });
	protected ConfigCategoryBuilder fallbackCategory = null;
	protected boolean alwaysShowTabs = false;
	protected @Nullable IConfigSnapshotHandler snapshotHandler;
	protected @Nullable ConfigScreenBuilder.IRemoteConfigProvider remoteConfigProvider;
	private ConfigCategoryBuilder selectedCategory;
	private IConfigScreenGUIState previousGUIState;
	
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
	
	@Override public ITextComponent getTitle() {
		return title;
	}
	@Override public ConfigScreenBuilder setTitle(ITextComponent title) {
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
	
	@Override public IConfigScreenGUIState getPreviousGUIState() {
		return previousGUIState;
	}
	
	@Override public ConfigScreenBuilder setPreviousGUIState(IConfigScreenGUIState state) {
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
		  parent, modId, title, builtMap.get(EditType.CLIENT), builtMap.get(EditType.COMMON),
		  builtMap.get(EditType.SERVER_COMMON), builtMap.get(EditType.SERVER),
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
}
