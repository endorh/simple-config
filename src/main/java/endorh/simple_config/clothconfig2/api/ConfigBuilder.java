package endorh.simple_config.clothconfig2.api;

import endorh.simple_config.clothconfig2.impl.ConfigBuilderImpl;
import endorh.simple_config.clothconfig2.impl.ConfigEntryBuilderImpl;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public interface ConfigBuilder {
   static ConfigBuilder create() {
      return new ConfigBuilderImpl();
   }

   ConfigBuilder setFallbackCategory(ConfigCategory var1);

   Screen getParentScreen();

   ConfigBuilder setParentScreen(Screen var1);

   ITextComponent getTitle();

   ConfigBuilder setTitle(ITextComponent var1);

   boolean isEditable();

   ConfigBuilder setEditable(boolean var1);

   ConfigCategory getOrCreateCategory(ITextComponent var1);

   ConfigBuilder removeCategory(ITextComponent var1);

   ConfigBuilder removeCategoryIfExists(ITextComponent var1);

   boolean hasCategory(ITextComponent var1);

   ConfigBuilder setShouldTabsSmoothScroll(boolean var1);

   boolean isTabsSmoothScrolling();

   ConfigBuilder setShouldListSmoothScroll(boolean var1);

   boolean isListSmoothScrolling();

   ConfigBuilder setDoesConfirmSave(boolean var1);

   boolean doesConfirmSave();

   /** @deprecated */
   @Deprecated
   default ConfigBuilder setDoesProcessErrors(boolean processErrors) {
      return this;
   }

   /** @deprecated */
   @Deprecated
   default boolean doesProcessErrors() {
      return false;
   }

   ResourceLocation getDefaultBackgroundTexture();

   ConfigBuilder setDefaultBackgroundTexture(ResourceLocation var1);

   Runnable getSavingRunnable();

   ConfigBuilder setSavingRunnable(Runnable var1);

   Consumer<Screen> getAfterInitConsumer();

   ConfigBuilder setAfterInitConsumer(Consumer<Screen> var1);

   default ConfigBuilder alwaysShowTabs() {
      return this.setAlwaysShowTabs(true);
   }

   void setGlobalized(boolean var1);

   void setGlobalizedExpanded(boolean var1);

   boolean isAlwaysShowTabs();

   ConfigBuilder setAlwaysShowTabs(boolean var1);

   ConfigBuilder setTransparentBackground(boolean var1);

   default ConfigBuilder transparentBackground() {
      return this.setTransparentBackground(true);
   }

   default ConfigBuilder solidBackground() {
      return this.setTransparentBackground(false);
   }

   /** @deprecated */
   @Deprecated
   default ConfigEntryBuilderImpl getEntryBuilder() {
      return (ConfigEntryBuilderImpl)this.entryBuilder();
   }

   default ConfigEntryBuilder entryBuilder() {
      return ConfigEntryBuilderImpl.create();
   }

   Screen build();

   boolean hasTransparentBackground();
}
