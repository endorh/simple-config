package endorh.simple_config.clothconfig2.impl;

import com.google.common.collect.Maps;
import endorh.simple_config.clothconfig2.api.ConfigBuilder;
import endorh.simple_config.clothconfig2.api.ConfigCategory;
import endorh.simple_config.clothconfig2.api.IExpandable;
import endorh.simple_config.clothconfig2.gui.AbstractConfigScreen;
import endorh.simple_config.clothconfig2.gui.ClothConfigScreen;
import endorh.simple_config.clothconfig2.gui.GlobalizedClothConfigScreen;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

@OnlyIn(value = Dist.CLIENT)
@Internal public class ConfigBuilderImpl implements ConfigBuilder {
	protected Runnable savingRunnable;
	protected Screen parent;
	protected ITextComponent title = new TranslationTextComponent("text.cloth-config.config");
	protected boolean globalized = false;
	protected boolean globalizedExpanded = true;
	protected boolean editable = true;
	protected boolean transparentBackground = false;
	protected ResourceLocation defaultBackground = AbstractGui.BACKGROUND_LOCATION;
	protected Consumer<Screen> afterInitConsumer = screen -> {};
	protected final Map<String, ConfigCategory> categories = Maps.newLinkedHashMap();
	protected String fallbackCategory = null;
	protected boolean alwaysShowTabs = false;
	
	@Internal public ConfigBuilderImpl() {}
	
	@Override public void setGlobalized(boolean globalized) {
		this.globalized = globalized;
	}
	
	@Override public void setGlobalizedExpanded(boolean globalizedExpanded) {
		this.globalizedExpanded = globalizedExpanded;
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
	
	@Override public ConfigBuilder setAfterInitConsumer(Consumer<Screen> afterInitConsumer) {
		this.afterInitConsumer = afterInitConsumer;
		return this;
	}
	
	@Override public ConfigBuilder setFallbackCategory(ConfigCategory fallbackCategory) {
		this.fallbackCategory = Objects.requireNonNull(fallbackCategory).getName();
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
		if (this.categories.containsKey(name)) {
			return this.categories.get(name);
		}
		if (this.fallbackCategory == null) {
			this.fallbackCategory = name;
		}
		return this.categories.computeIfAbsent(
		  name, key -> new ConfigCategoryImpl(this, name));
	}
	
	@Override public ConfigBuilder removeCategory(String name) {
		if (this.categories.containsKey(name) && this.fallbackCategory.equals(name)) {
			this.fallbackCategory = null;
		}
		if (!this.categories.containsKey(name)) {
			throw new NullPointerException("Category doesn't exist!");
		}
		this.categories.remove(name);
		return this;
	}
	
	@Override public ConfigBuilder removeCategoryIfExists(String name) {
		if (this.categories.containsKey(name) && this.fallbackCategory.equals(name)) {
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
	
	@Override public Screen build() {
		if (this.categories.isEmpty() || this.fallbackCategory == null)
			throw new NullPointerException("There cannot be no categories or fallback category!");
		AbstractConfigScreen screen =
		  this.globalized ? new GlobalizedClothConfigScreen(this.parent, this.title, this.categories, this.defaultBackground)
		                  : new ClothConfigScreen(this.parent, this.title, this.categories, this.defaultBackground);
		screen.setSavingRunnable(this.savingRunnable);
		screen.setEditable(this.editable);
		screen.setFallbackCategory(this.fallbackCategory);
		screen.setTransparentBackground(this.transparentBackground);
		screen.setAlwaysShowTabs(this.alwaysShowTabs);
		screen.setAfterInitConsumer(this.afterInitConsumer);
		if (screen instanceof IExpandable)
			((IExpandable) screen).setExpanded(this.globalizedExpanded);
		return screen;
	}
	
	@Override public Runnable getSavingRunnable() {
		return this.savingRunnable;
	}
}

