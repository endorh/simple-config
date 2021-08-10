package endorh.simple_config.clothconfig2.impl;

import com.google.common.collect.Maps;
import endorh.simple_config.clothconfig2.api.ConfigBuilder;
import endorh.simple_config.clothconfig2.api.ConfigCategory;
import endorh.simple_config.clothconfig2.api.Expandable;
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
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

@OnlyIn(value = Dist.CLIENT)
@ApiStatus.Internal
public class ConfigBuilderImpl
  implements ConfigBuilder {
	private Runnable savingRunnable;
	private Screen parent;
	private ITextComponent title = new TranslationTextComponent("text.cloth-config.config");
	private boolean globalized = false;
	private boolean globalizedExpanded = true;
	private boolean editable = true;
	private boolean tabsSmoothScroll = true;
	private boolean listSmoothScroll = true;
	private boolean doesConfirmSave = true;
	private boolean transparentBackground = false;
	private ResourceLocation defaultBackground = AbstractGui.BACKGROUND_LOCATION;
	private Consumer<Screen> afterInitConsumer = screen -> {};
	private final Map<ITextComponent, ConfigCategory> categoryMap = Maps.newLinkedHashMap();
	private ITextComponent fallbackCategory = null;
	private boolean alwaysShowTabs = false;
	
	@ApiStatus.Internal
	public ConfigBuilderImpl() {
	}
	
	@Override
	public void setGlobalized(boolean globalized) {
		this.globalized = globalized;
	}
	
	@Override
	public void setGlobalizedExpanded(boolean globalizedExpanded) {
		this.globalizedExpanded = globalizedExpanded;
	}
	
	@Override
	public boolean isAlwaysShowTabs() {
		return this.alwaysShowTabs;
	}
	
	@Override
	public ConfigBuilder setAlwaysShowTabs(boolean alwaysShowTabs) {
		this.alwaysShowTabs = alwaysShowTabs;
		return this;
	}
	
	@Override
	public ConfigBuilder setTransparentBackground(boolean transparentBackground) {
		this.transparentBackground = transparentBackground;
		return this;
	}
	
	@Override
	public boolean hasTransparentBackground() {
		return this.transparentBackground;
	}
	
	@Override
	public ConfigBuilder setAfterInitConsumer(Consumer<Screen> afterInitConsumer) {
		this.afterInitConsumer = afterInitConsumer;
		return this;
	}
	
	@Override
	public ConfigBuilder setFallbackCategory(ConfigCategory fallbackCategory) {
		this.fallbackCategory = Objects.requireNonNull(fallbackCategory).getCategoryKey();
		return this;
	}
	
	@Override
	public Screen getParentScreen() {
		return this.parent;
	}
	
	@Override
	public ConfigBuilder setParentScreen(Screen parent) {
		this.parent = parent;
		return this;
	}
	
	@Override
	public ITextComponent getTitle() {
		return this.title;
	}
	
	@Override
	public ConfigBuilder setTitle(ITextComponent title) {
		this.title = title;
		return this;
	}
	
	@Override
	public boolean isEditable() {
		return this.editable;
	}
	
	@Override
	public ConfigBuilder setEditable(boolean editable) {
		this.editable = editable;
		return this;
	}
	
	@Override
	public ConfigCategory getOrCreateCategory(ITextComponent categoryKey) {
		if (this.categoryMap.containsKey(categoryKey)) {
			return this.categoryMap.get(categoryKey);
		}
		if (this.fallbackCategory == null) {
			this.fallbackCategory = categoryKey;
		}
		return this.categoryMap.computeIfAbsent(
		  categoryKey, key -> new ConfigCategoryImpl(this, categoryKey));
	}
	
	@Override
	public ConfigBuilder removeCategory(ITextComponent category) {
		if (this.categoryMap.containsKey(category) && this.fallbackCategory.equals(category)) {
			this.fallbackCategory = null;
		}
		if (!this.categoryMap.containsKey(category)) {
			throw new NullPointerException("Category doesn't exist!");
		}
		this.categoryMap.remove(category);
		return this;
	}
	
	@Override
	public ConfigBuilder removeCategoryIfExists(ITextComponent category) {
		if (this.categoryMap.containsKey(category) && this.fallbackCategory.equals(category)) {
			this.fallbackCategory = null;
		}
		this.categoryMap.remove(category);
		return this;
	}
	
	@Override
	public boolean hasCategory(ITextComponent category) {
		return this.categoryMap.containsKey(category);
	}
	
	@Override
	public ConfigBuilder setShouldTabsSmoothScroll(boolean shouldTabsSmoothScroll) {
		this.tabsSmoothScroll = shouldTabsSmoothScroll;
		return this;
	}
	
	@Override
	public boolean isTabsSmoothScrolling() {
		return this.tabsSmoothScroll;
	}
	
	@Override
	public ConfigBuilder setShouldListSmoothScroll(boolean shouldListSmoothScroll) {
		this.listSmoothScroll = shouldListSmoothScroll;
		return this;
	}
	
	@Override
	public boolean isListSmoothScrolling() {
		return this.listSmoothScroll;
	}
	
	@Override
	public ConfigBuilder setDoesConfirmSave(boolean confirmSave) {
		this.doesConfirmSave = confirmSave;
		return this;
	}
	
	@Override
	public boolean doesConfirmSave() {
		return this.doesConfirmSave;
	}
	
	@Override
	public ResourceLocation getDefaultBackgroundTexture() {
		return this.defaultBackground;
	}
	
	@Override
	public ConfigBuilder setDefaultBackgroundTexture(ResourceLocation texture) {
		this.defaultBackground = texture;
		return this;
	}
	
	@Override
	public ConfigBuilder setSavingRunnable(Runnable runnable) {
		this.savingRunnable = runnable;
		return this;
	}
	
	@Override
	public Consumer<Screen> getAfterInitConsumer() {
		return this.afterInitConsumer;
	}
	
	@Override
	public Screen build() {
		if (this.categoryMap.isEmpty() || this.fallbackCategory == null) {
			throw new NullPointerException("There cannot be no categories or fallback category!");
		}
		AbstractConfigScreen screen =
		  this.globalized ? new GlobalizedClothConfigScreen(this.parent, this.title, this.categoryMap,
		                                                    this.defaultBackground)
		                  : new ClothConfigScreen(this.parent, this.title, this.categoryMap,
		                                          this.defaultBackground);
		screen.setSavingRunnable(this.savingRunnable);
		screen.setEditable(this.editable);
		screen.setFallbackCategory(this.fallbackCategory);
		screen.setTransparentBackground(this.transparentBackground);
		screen.setAlwaysShowTabs(this.alwaysShowTabs);
		screen.setConfirmSave(this.doesConfirmSave);
		screen.setAfterInitConsumer(this.afterInitConsumer);
		if (screen instanceof Expandable) {
			((Expandable) screen).setExpanded(this.globalizedExpanded);
		}
		return screen;
	}
	
	@Override
	public Runnable getSavingRunnable() {
		return this.savingRunnable;
	}
}

