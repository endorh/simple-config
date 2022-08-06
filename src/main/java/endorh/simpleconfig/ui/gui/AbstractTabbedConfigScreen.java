package endorh.simpleconfig.ui.gui;

import endorh.simpleconfig.ui.api.ConfigCategory;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.Collection;
import java.util.List;

public abstract class AbstractTabbedConfigScreen extends AbstractConfigScreen {
	protected AbstractTabbedConfigScreen(
	  Screen parent, String modId, ITextComponent title, ResourceLocation backgroundLocation,
	  Collection<ConfigCategory> clientCategories,
	  Collection<ConfigCategory> commonCategories,
	  Collection<ConfigCategory> serverCategories
	) {
		super(parent, modId, title, backgroundLocation,
		      clientCategories, commonCategories, serverCategories);
	}
	
	protected List<ConfigCategory> getTabbedCategories() {
		return getSortedTypeCategories();
	}
}

