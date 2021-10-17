package endorh.simple_config.clothconfig2.api;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public interface TabbedConfigScreen extends ConfigScreen {
	void registerCategoryBackground(String key, ResourceLocation background);
	
	String getSelectedCategoryName();
	ConfigCategory getSelectedCategory();
}

