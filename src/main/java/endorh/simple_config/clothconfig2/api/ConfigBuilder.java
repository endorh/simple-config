package endorh.simple_config.clothconfig2.api;

import endorh.simple_config.clothconfig2.impl.ConfigBuilderImpl;
import endorh.simple_config.clothconfig2.impl.ConfigEntryBuilderImpl;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;

@OnlyIn(value = Dist.CLIENT)
public interface ConfigBuilder {
	static ConfigBuilder create() {
		return new ConfigBuilderImpl();
	}
	
	ConfigBuilder setFallbackCategory(ConfigCategory fallbackCategory);
	
	Screen getParentScreen();
	
	ConfigBuilder setParentScreen(Screen parent);
	
	ITextComponent getTitle();
	
	ConfigBuilder setTitle(ITextComponent title);
	
	boolean isEditable();
	
	ConfigBuilder setEditable(boolean editable);
	
	ConfigCategory getOrCreateCategory(String name);
	
	ConfigBuilder removeCategory(String name);
	
	ConfigBuilder removeCategoryIfExists(String name);
	
	boolean hasCategory(String name);
	
	ResourceLocation getDefaultBackgroundTexture();
	
	ConfigBuilder setDefaultBackgroundTexture(ResourceLocation texture);
	
	Runnable getSavingRunnable();
	
	ConfigBuilder setSavingRunnable(Runnable runnable);
	
	Consumer<Screen> getAfterInitConsumer();
	
	ConfigBuilder setAfterInitConsumer(Consumer<Screen> afterInitConsumer);
	
	default ConfigBuilder alwaysShowTabs() {
		return this.setAlwaysShowTabs(true);
	}
	
	@Deprecated void setGlobalized(boolean globalized);
	
	@Deprecated void setGlobalizedExpanded(boolean globalizedExpanded);
	
	boolean isAlwaysShowTabs();
	
	ConfigBuilder setAlwaysShowTabs(boolean alwaysShowTabs);
	
	ConfigBuilder setTransparentBackground(boolean transparentBackground);
	
	default ConfigBuilder transparentBackground() {
		return this.setTransparentBackground(true);
	}
	
	default ConfigBuilder solidBackground() {
		return this.setTransparentBackground(false);
	}
	
	default ConfigEntryBuilder entryBuilder() {
		return ConfigEntryBuilderImpl.create();
	}
	
	Screen build();
	
	boolean hasTransparentBackground();
}

