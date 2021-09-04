package endorh.simple_config.clothconfig2.gui;

import com.google.common.collect.Maps;
import endorh.simple_config.clothconfig2.api.ConfigCategory;
import endorh.simple_config.clothconfig2.api.TabbedConfigScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class AbstractTabbedConfigScreen extends AbstractConfigScreen
  implements TabbedConfigScreen {
	private final Map<String, ResourceLocation> categoryBackgroundLocation = Maps.newHashMap();
	
	protected AbstractTabbedConfigScreen(
	  Screen parent, ITextComponent title, ResourceLocation backgroundLocation,
	  Map<String, ConfigCategory> categories
	) {
		super(parent, title, backgroundLocation, categories);
	}
	
	@Override public final void registerCategoryBackground(String text, ResourceLocation background) {
		this.categoryBackgroundLocation.put(text, background);
	}
	
	@Override public ResourceLocation getBackgroundLocation() {
		String cat = this.getSelectedCategory();
		if (this.categoryBackgroundLocation.containsKey(cat))
			return this.categoryBackgroundLocation.get(cat);
		return super.getBackgroundLocation();
	}
}

