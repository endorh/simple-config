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
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
@Internal
public class ConfigBuilderImpl implements ConfigBuilder {
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
   private ResourceLocation defaultBackground;
   private Consumer<Screen> afterInitConsumer;
   private final Map<ITextComponent, ConfigCategory> categoryMap;
   private ITextComponent fallbackCategory;
   private boolean alwaysShowTabs;

   @Internal
   public ConfigBuilderImpl() {
      this.defaultBackground = AbstractGui.BACKGROUND_LOCATION;
      this.afterInitConsumer = (screen) -> {
      };
      this.categoryMap = Maps.newLinkedHashMap();
      this.fallbackCategory = null;
      this.alwaysShowTabs = false;
   }

   public void setGlobalized(boolean globalized) {
      this.globalized = globalized;
   }

   public void setGlobalizedExpanded(boolean globalizedExpanded) {
      this.globalizedExpanded = globalizedExpanded;
   }

   public boolean isAlwaysShowTabs() {
      return this.alwaysShowTabs;
   }

   public ConfigBuilder setAlwaysShowTabs(boolean alwaysShowTabs) {
      this.alwaysShowTabs = alwaysShowTabs;
      return this;
   }

   public ConfigBuilder setTransparentBackground(boolean transparentBackground) {
      this.transparentBackground = transparentBackground;
      return this;
   }

   public boolean hasTransparentBackground() {
      return this.transparentBackground;
   }

   public ConfigBuilder setAfterInitConsumer(Consumer<Screen> afterInitConsumer) {
      this.afterInitConsumer = afterInitConsumer;
      return this;
   }

   public ConfigBuilder setFallbackCategory(ConfigCategory fallbackCategory) {
      this.fallbackCategory = Objects.requireNonNull(fallbackCategory).getCategoryKey();
      return this;
   }

   public Screen getParentScreen() {
      return this.parent;
   }

   public ConfigBuilder setParentScreen(Screen parent) {
      this.parent = parent;
      return this;
   }

   public ITextComponent getTitle() {
      return this.title;
   }

   public ConfigBuilder setTitle(ITextComponent title) {
      this.title = title;
      return this;
   }

   public boolean isEditable() {
      return this.editable;
   }

   public ConfigBuilder setEditable(boolean editable) {
      this.editable = editable;
      return this;
   }

   public ConfigCategory getOrCreateCategory(ITextComponent categoryKey) {
      if (this.categoryMap.containsKey(categoryKey)) {
         return this.categoryMap.get(categoryKey);
      } else {
         if (this.fallbackCategory == null) {
            this.fallbackCategory = categoryKey;
         }

         return this.categoryMap.computeIfAbsent(categoryKey, (key) -> {
            return new ConfigCategoryImpl(this, categoryKey);
         });
      }
   }

   public ConfigBuilder removeCategory(ITextComponent category) {
      if (this.categoryMap.containsKey(category) && this.fallbackCategory.equals(category)) {
         this.fallbackCategory = null;
      }

      if (!this.categoryMap.containsKey(category)) {
         throw new NullPointerException("Category doesn't exist!");
      } else {
         this.categoryMap.remove(category);
         return this;
      }
   }

   public ConfigBuilder removeCategoryIfExists(ITextComponent category) {
      if (this.categoryMap.containsKey(category) && this.fallbackCategory.equals(category)) {
         this.fallbackCategory = null;
      }

      this.categoryMap.remove(category);
      return this;
   }

   public boolean hasCategory(ITextComponent category) {
      return this.categoryMap.containsKey(category);
   }

   public ConfigBuilder setShouldTabsSmoothScroll(boolean shouldTabsSmoothScroll) {
      this.tabsSmoothScroll = shouldTabsSmoothScroll;
      return this;
   }

   public boolean isTabsSmoothScrolling() {
      return this.tabsSmoothScroll;
   }

   public ConfigBuilder setShouldListSmoothScroll(boolean shouldListSmoothScroll) {
      this.listSmoothScroll = shouldListSmoothScroll;
      return this;
   }

   public boolean isListSmoothScrolling() {
      return this.listSmoothScroll;
   }

   public ConfigBuilder setDoesConfirmSave(boolean confirmSave) {
      this.doesConfirmSave = confirmSave;
      return this;
   }

   public boolean doesConfirmSave() {
      return this.doesConfirmSave;
   }

   public ResourceLocation getDefaultBackgroundTexture() {
      return this.defaultBackground;
   }

   public ConfigBuilder setDefaultBackgroundTexture(ResourceLocation texture) {
      this.defaultBackground = texture;
      return this;
   }

   public ConfigBuilder setSavingRunnable(Runnable runnable) {
      this.savingRunnable = runnable;
      return this;
   }

   public Consumer<Screen> getAfterInitConsumer() {
      return this.afterInitConsumer;
   }

   public Screen build() {
      if (!this.categoryMap.isEmpty() && this.fallbackCategory != null) {
         Object screen;
         if (this.globalized) {
            screen = new GlobalizedClothConfigScreen(this.parent, this.title, this.categoryMap, this.defaultBackground);
         } else {
            screen = new ClothConfigScreen(this.parent, this.title, this.categoryMap, this.defaultBackground);
         }

         ((AbstractConfigScreen)screen).setSavingRunnable(this.savingRunnable);
         ((AbstractConfigScreen)screen).setEditable(this.editable);
         ((AbstractConfigScreen)screen).setFallbackCategory(this.fallbackCategory);
         ((AbstractConfigScreen)screen).setTransparentBackground(this.transparentBackground);
         ((AbstractConfigScreen)screen).setAlwaysShowTabs(this.alwaysShowTabs);
         ((AbstractConfigScreen)screen).setConfirmSave(this.doesConfirmSave);
         ((AbstractConfigScreen)screen).setAfterInitConsumer(this.afterInitConsumer);
         if (screen instanceof Expandable) {
            ((Expandable)screen).setExpanded(this.globalizedExpanded);
         }

         return (Screen)screen;
      } else {
         throw new NullPointerException("There cannot be no categories or fallback category!");
      }
   }

   public Runnable getSavingRunnable() {
      return this.savingRunnable;
   }
}
