package endorh.simpleconfig.api;

import endorh.simpleconfig.api.ui.icon.Icon;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Contract;

import java.util.function.Consumer;

public interface ConfigCategoryBuilder extends ConfigEntryHolderBuilder<ConfigCategoryBuilder> {
	/**
	 * Set the baker method for this config category<br>
	 * You may also define a '{@code bake}' static method
	 * in the backing class accepting a {@link SimpleConfig}
	 * and it will be set automatically as the baker (but you
	 * may not define it and also call this method)
	 */
	@Contract("_ -> this") ConfigCategoryBuilder withBaker(Consumer<SimpleConfigCategory> baker);
	
	/**
	 * Set the background texture to be used
	 *
	 * @see #withBackground(ResourceLocation)
	 * @see #withIcon(Icon)
	 * @see #withColor(int)
	 */
	@Contract("_ -> this") ConfigCategoryBuilder withBackground(String resourceName);
	
	/**
	 * Set the background texture to be used
	 *
	 * @see #withBackground(String)
	 * @see #withIcon(Icon)
	 * @see #withColor(int)
	 */
	@Contract("_ -> this") ConfigCategoryBuilder withBackground(ResourceLocation background);
	
	/**
	 * Set the icon of this category.<br>
	 * Icons are displayed in the tab buttons when more than one category is present.<br>
	 * Use {@link Icon#EMPTY} to disable the icon (default).
	 *
	 * @see #withColor(int)
	 * @see #withBackground(ResourceLocation)
	 */
	@Contract("_ -> this") ConfigCategoryBuilder withIcon(Icon icon);
	
	/**
	 * Set the color of this category.<br>
	 * Affects the tint applied to the tab button for this category,
	 * which will be visible when multiple categories are present.<br>
	 *
	 * @param tint Color tint to use, in ARGB format. It's recommended
	 *   a transparency of 0x80, so the button background is
	 *   visible behind the color. A value of 0 means no tint.
	 * @see #withIcon(Icon)
	 * @see #withBackground(ResourceLocation)
	 */
	@Contract("_ -> this") ConfigCategoryBuilder withColor(int tint);
	
	@Contract("_, _ -> this") ConfigCategoryBuilder n(ConfigGroupBuilder group, int index);
}
