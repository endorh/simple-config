package endorh.simpleconfig.ui.impl;

import endorh.simpleconfig.ui.api.ConfigCategory;
import endorh.simpleconfig.ui.api.ConfigScreenBuilder;
import endorh.simpleconfig.ui.gui.AbstractConfigScreen;
import endorh.simpleconfig.ui.gui.SimpleConfigScreen;
import endorh.simpleconfig.ui.hotkey.ConfigHotKey;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

@OnlyIn(value = Dist.CLIENT)
@Internal public class ConfigScreenBuilderImpl implements ConfigScreenBuilder {
	protected Runnable savingRunnable;
	protected String modId;
	protected Screen parent;
	protected ConfigHotKey editedConfigHotkey = null;
	protected Consumer<Boolean> hotKeySaver = null;
	protected ITextComponent title = new TranslationTextComponent("text.cloth-config.config");
	protected boolean editable = true;
	protected boolean transparentBackground = false;
	protected ResourceLocation defaultBackground = AbstractGui.BACKGROUND_LOCATION;
	protected Consumer<Screen> afterInitConsumer = screen -> {};
	protected final Map<String, ConfigCategory> serverCategories = new LinkedHashMap<>();
	protected final Map<String, ConfigCategory> clientCategories = new LinkedHashMap<>();
	protected ConfigCategory fallbackCategory = null;
	protected boolean alwaysShowTabs = false;
	protected @Nullable ConfigScreenBuilder.IConfigSnapshotHandler snapshotHandler;
	private ConfigCategory selectedCategory;
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
	
	@Override public ConfigScreenBuilder setAfterInitConsumer(Consumer<Screen> afterInitConsumer) {
		this.afterInitConsumer = afterInitConsumer;
		return this;
	}
	
	@Override public ConfigScreenBuilder setFallbackCategory(ConfigCategory fallbackCategory) {
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
	
	@Override public ConfigCategory getSelectedCategory() {
		return selectedCategory;
	}
	
	@Override public ConfigScreenBuilder setSelectedCategory(ConfigCategory category) {
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
	
	@Override public ConfigCategory getOrCreateCategory(String name, boolean isServer) {
		Map<String, ConfigCategory> categories = isServer? serverCategories : clientCategories;
		ConfigCategory cat = categories.computeIfAbsent(
		  name, key -> new ConfigCategoryImpl(this, name, isServer));
		if (fallbackCategory == null) fallbackCategory = cat;
		return cat;
	}
	
	@Override public ConfigScreenBuilder removeCategory(String name, boolean isServer) {
		Map<String, ConfigCategory> categories = isServer? serverCategories : clientCategories;
		if (!categories.containsKey(name))
			throw new IllegalArgumentException("Category " + name + " does not exist");
		if (categories.get(name) == fallbackCategory)
			fallbackCategory = null;
		categories.remove(name);
		return this;
	}
	
	@Override public ConfigScreenBuilder removeCategoryIfExists(String name, boolean isServer) {
		Map<String, ConfigCategory> categories = isServer? serverCategories : clientCategories;
		if (categories.containsKey(name))
			removeCategory(name, isServer);
		return this;
	}
	
	@Override public boolean hasCategory(String name, boolean isServer) {
		Map<String, ConfigCategory> categories = isServer? serverCategories : clientCategories;
		return categories.containsKey(name);
	}
	
	@Override public ResourceLocation getDefaultBackgroundTexture() {
		return defaultBackground;
	}
	
	@Override public ConfigScreenBuilder setDefaultBackgroundTexture(ResourceLocation texture) {
		defaultBackground = texture;
		return this;
	}
	
	@Override public ConfigScreenBuilder setSavingRunnable(Runnable runnable) {
		savingRunnable = runnable;
		return this;
	}
	
	@Override public Consumer<Screen> getAfterInitConsumer() {
		return afterInitConsumer;
	}
	
	@Override public AbstractConfigScreen build() {
		if (serverCategories.isEmpty() && clientCategories.isEmpty() || fallbackCategory == null)
			throw new IllegalStateException("Config screen without categories or fallback category");
		AbstractConfigScreen screen = new SimpleConfigScreen(
		  parent, modId, title, clientCategories.values(),
		  serverCategories.values(), defaultBackground);
		screen.setEditedConfigHotKey(editedConfigHotkey, hotKeySaver);
		screen.setSavingRunnable(savingRunnable);
		screen.setEditable(editable);
		screen.setSelectedCategory(selectedCategory != null? selectedCategory : fallbackCategory);
		screen.setTransparentBackground(transparentBackground);
		screen.setAlwaysShowTabs(alwaysShowTabs);
		screen.setAfterInitConsumer(afterInitConsumer);
		screen.setSnapshotHandler(snapshotHandler);
		screen.loadConfigScreenGUIState(previousGUIState);
		return screen;
	}
	
	@Override public Runnable getSavingRunnable() {
		return savingRunnable;
	}
}
