package endorh.simpleconfig.ui.impl;

import endorh.simpleconfig.ui.api.ConfigBuilder;
import endorh.simpleconfig.ui.api.ConfigCategory;
import endorh.simpleconfig.ui.gui.AbstractConfigScreen;
import endorh.simpleconfig.ui.gui.SimpleConfigScreen;
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
@Internal public class ConfigBuilderImpl implements ConfigBuilder {
	protected Runnable savingRunnable;
	protected String modId;
	protected Screen parent;
	protected ITextComponent title = new TranslationTextComponent("text.cloth-config.config");
	protected boolean editable = true;
	protected boolean transparentBackground = false;
	protected ResourceLocation defaultBackground = AbstractGui.BACKGROUND_LOCATION;
	protected Consumer<Screen> afterInitConsumer = screen -> {};
	protected final Map<String, ConfigCategory> serverCategories = new LinkedHashMap<>();
	protected final Map<String, ConfigCategory> clientCategories = new LinkedHashMap<>();
	protected ConfigCategory fallbackCategory = null;
	protected boolean alwaysShowTabs = false;
	protected @Nullable ConfigBuilder.IConfigSnapshotHandler snapshotHandler;
	
	@Internal public ConfigBuilderImpl(String modId) {
		this.modId = modId;
	}
	
	@Override public boolean isAlwaysShowTabs() {
		return alwaysShowTabs;
	}
	
	@Override public ConfigBuilder setAlwaysShowTabs(boolean alwaysShowTabs) {
		this.alwaysShowTabs = alwaysShowTabs;
		return this;
	}
	
	@Override public ConfigBuilder setTransparentBackground(boolean transparent) {
		transparentBackground = transparent;
		return this;
	}
	
	@Override public boolean hasTransparentBackground() {
		return transparentBackground;
	}
	
	@Override public ConfigBuilder setSnapshotHandler(
	  IConfigSnapshotHandler handler
	) {
		snapshotHandler = handler;
		return this;
	}
	
	@Override public ConfigBuilder setAfterInitConsumer(Consumer<Screen> afterInitConsumer) {
		this.afterInitConsumer = afterInitConsumer;
		return this;
	}
	
	@Override public ConfigBuilder setFallbackCategory(ConfigCategory fallbackCategory) {
		this.fallbackCategory = fallbackCategory;
		return this;
	}
	
	@Override public Screen getParentScreen() {
		return parent;
	}
	
	@Override public ConfigBuilder setParentScreen(Screen parent) {
		this.parent = parent;
		return this;
	}
	
	@Override public ITextComponent getTitle() {
		return title;
	}
	
	@Override public ConfigBuilder setTitle(ITextComponent title) {
		this.title = title;
		return this;
	}
	
	@Override public boolean isEditable() {
		return editable;
	}
	
	@Override public ConfigBuilder setEditable(boolean editable) {
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
	
	@Override public ConfigBuilder removeCategory(String name, boolean isServer) {
		Map<String, ConfigCategory> categories = isServer? serverCategories : clientCategories;
		if (!categories.containsKey(name))
			throw new IllegalArgumentException("Category " + name + " does not exist");
		if (categories.get(name) == fallbackCategory)
			fallbackCategory = null;
		categories.remove(name);
		return this;
	}
	
	@Override public ConfigBuilder removeCategoryIfExists(String name, boolean isServer) {
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
	
	@Override public ConfigBuilder setDefaultBackgroundTexture(ResourceLocation texture) {
		defaultBackground = texture;
		return this;
	}
	
	@Override public ConfigBuilder setSavingRunnable(Runnable runnable) {
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
		screen.setSavingRunnable(savingRunnable);
		screen.setEditable(editable);
		screen.setFallbackCategory(fallbackCategory);
		screen.setTransparentBackground(transparentBackground);
		screen.setAlwaysShowTabs(alwaysShowTabs);
		screen.setAfterInitConsumer(afterInitConsumer);
		screen.setSnapshotHandler(snapshotHandler);
		return screen;
	}
	
	@Override public Runnable getSavingRunnable() {
		return savingRunnable;
	}
}
