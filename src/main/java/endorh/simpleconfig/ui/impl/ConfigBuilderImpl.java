package endorh.simpleconfig.ui.impl;

import com.google.common.collect.Maps;
import endorh.simpleconfig.ui.api.ConfigBuilder;
import endorh.simpleconfig.ui.api.ConfigCategory;
import endorh.simpleconfig.ui.gui.AbstractConfigScreen;
import endorh.simpleconfig.ui.gui.ClothConfigScreen;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
	protected final Map<String, ConfigCategory> categories = Maps.newLinkedHashMap();
	protected ConfigCategory fallbackCategory = null;
	protected boolean alwaysShowTabs = false;
	protected @Nullable ConfigBuilder.IConfigSnapshotHandler snapshotHandler;
	
	@Internal public ConfigBuilderImpl(String modId) {
		this.modId = modId;
	}
	
	@Override public boolean isAlwaysShowTabs() {
		return this.alwaysShowTabs;
	}
	
	@Override public ConfigBuilder setAlwaysShowTabs(boolean alwaysShowTabs) {
		this.alwaysShowTabs = alwaysShowTabs;
		return this;
	}
	
	@Override public ConfigBuilder setTransparentBackground(boolean transparent) {
		this.transparentBackground = transparent;
		return this;
	}
	
	@Override public boolean hasTransparentBackground() {
		return this.transparentBackground;
	}
	
	@Override public ConfigBuilder setSnapshotHandler(
	  IConfigSnapshotHandler handler
	) {
		this.snapshotHandler = handler;
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
		return this.parent;
	}
	
	@Override public ConfigBuilder setParentScreen(Screen parent) {
		this.parent = parent;
		return this;
	}
	
	@Override public ITextComponent getTitle() {
		return this.title;
	}
	
	@Override public ConfigBuilder setTitle(ITextComponent title) {
		this.title = title;
		return this;
	}
	
	@Override public boolean isEditable() {
		return this.editable;
	}
	
	@Override public ConfigBuilder setEditable(boolean editable) {
		this.editable = editable;
		return this;
	}
	
	@Override public ConfigCategory getOrCreateCategory(String name) {
		final ConfigCategory cat = this.categories.computeIfAbsent(
		  name, key -> new ConfigCategoryImpl(this, name));
		if (this.fallbackCategory == null) this.fallbackCategory = cat;
		return cat;
	}
	
	@Override public ConfigBuilder removeCategory(String name) {
		if (this.categories.containsKey(name) && this.fallbackCategory.getName().equals(name)) {
			this.fallbackCategory = null;
		}
		if (!this.categories.containsKey(name)) {
			throw new NullPointerException("Category doesn't exist!");
		}
		this.categories.remove(name);
		return this;
	}
	
	@Override public ConfigBuilder removeCategoryIfExists(String name) {
		if (this.categories.containsKey(name) && this.fallbackCategory.getName().equals(name)) {
			this.fallbackCategory = null;
		}
		this.categories.remove(name);
		return this;
	}
	
	@Override public boolean hasCategory(String name) {
		return this.categories.containsKey(name);
	}
	
	@Override public ResourceLocation getDefaultBackgroundTexture() {
		return this.defaultBackground;
	}
	
	@Override public ConfigBuilder setDefaultBackgroundTexture(ResourceLocation texture) {
		this.defaultBackground = texture;
		return this;
	}
	
	@Override public ConfigBuilder setSavingRunnable(Runnable runnable) {
		this.savingRunnable = runnable;
		return this;
	}
	
	@Override public Consumer<Screen> getAfterInitConsumer() {
		return this.afterInitConsumer;
	}
	
	@Override public AbstractConfigScreen build() {
		if (this.categories.isEmpty() || this.fallbackCategory == null)
			throw new NullPointerException("There cannot be no categories or fallback category!");
		final Map<Boolean, Collection<ConfigCategory>> categorySets = categories.values().stream()
		  .collect(Collectors.groupingBy(ConfigCategory::isServer, Collectors.toCollection(LinkedHashSet::new)));
		AbstractConfigScreen screen = new ClothConfigScreen(
		  this.parent, this.modId, this.title, categorySets.getOrDefault(false, Collections.emptyList()),
		  categorySets.getOrDefault(true, Collections.emptyList()), this.defaultBackground);
		screen.setSavingRunnable(this.savingRunnable);
		screen.setEditable(this.editable);
		screen.setFallbackCategory(this.fallbackCategory);
		screen.setTransparentBackground(this.transparentBackground);
		screen.setAlwaysShowTabs(this.alwaysShowTabs);
		screen.setAfterInitConsumer(this.afterInitConsumer);
		screen.setSnapshotHandler(this.snapshotHandler);
		return screen;
	}
	
	@Override public Runnable getSavingRunnable() {
		return this.savingRunnable;
	}
}
