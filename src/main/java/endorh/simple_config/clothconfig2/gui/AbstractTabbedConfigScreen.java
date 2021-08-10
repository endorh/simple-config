package endorh.simple_config.clothconfig2.gui;

import endorh.simple_config.clothconfig2.api.ConfigCategory;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.Collection;
import java.util.List;

public abstract class AbstractTabbedConfigScreen extends AbstractConfigScreen {
	protected AbstractTabbedConfigScreen(
	  Screen parent, ITextComponent title, ResourceLocation backgroundLocation,
	  Collection<ConfigCategory> categories, Collection<ConfigCategory> serverCategories
	) {
		super(parent, title, backgroundLocation, categories, serverCategories);
	}
	
	protected List<ConfigCategory> getTabbedCategories() {
		return isSelectedCategoryServer()? sortedServerCategories : sortedClientCategories;
	}
}

