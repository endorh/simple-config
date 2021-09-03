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
	private final Map<ITextComponent, ResourceLocation> categoryBackgroundLocation =
	  Maps.newHashMap();
	
	protected AbstractTabbedConfigScreen(
	  Screen parent, ITextComponent title, ResourceLocation backgroundLocation,
	  Collection<ConfigCategory> categories
	) {
		super(parent, title, backgroundLocation, categories);
	}
	
	@Override
	public final void registerCategoryBackground(ITextComponent text, ResourceLocation background) {
		this.categoryBackgroundLocation.put(text, background);
	}
	
	@Override
	public ResourceLocation getBackgroundLocation() {
		ITextComponent selectedCategory = this.getSelectedCategory();
		if (this.categoryBackgroundLocation.containsKey(selectedCategory)) {
			return this.categoryBackgroundLocation.get(selectedCategory);
		}
		return super.getBackgroundLocation();
	}
}

