package endorh.simpleconfig.api;

import endorh.simpleconfig.api.ui.icon.Icon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface ConfigCategoryBuilder extends ConfigEntryHolderBuilder<ConfigCategoryBuilder> {
	/**
	 * Set the baker method for this config category<br>
	 * You may also define a '{@code bake}' static method
	 * in the backing class accepting a {@link SimpleConfig}
	 * and it will be set automatically as the baker (but you
	 * may not define it and also call this method)
	 */
	@Contract("_ -> this") @NotNull ConfigCategoryBuilder withBaker(Consumer<SimpleConfigCategory> baker);
	
	/**
	 * Set the background texture to be used
	 *
	 * @see #withBackground(ResourceLocation)
	 * @see #withIcon(Icon)
	 * @see #withColor(int)
	 */
	@Contract("_ -> this") @NotNull ConfigCategoryBuilder withBackground(String resourceName);
	
	/**
	 * Set the background texture to be used
	 *
	 * @see #withBackground(String)
	 * @see #withIcon(Icon)
	 * @see #withColor(int)
	 */
	@Contract("_ -> this") @NotNull ConfigCategoryBuilder withBackground(ResourceLocation background);
	
	/**
	 * Set the icon of this category.<br>
	 * Icons are displayed in the tab buttons when more than one category is present.<br>
	 * Use {@link Icon#EMPTY} to disable the icon (default).
	 *
	 * @see #withColor(int)
	 * @see #withBackground(ResourceLocation)
	 */
	@Contract("_ -> this") @NotNull ConfigCategoryBuilder withIcon(Icon icon);
	
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
	@Contract("_ -> this") @NotNull ConfigCategoryBuilder withColor(int tint);
	
	/**
	 * Add a config group at the given index.<br>
	 * You shouldn't need to specify the index under normal circumstances.
	 * @param index Index used to sort entries in the menu. Lower values,
	 * untied by addition order are displayed first.
	 * @see ConfigEntryHolderBuilder#n(ConfigGroupBuilder)
	 */
	@Contract("_, _ -> this") @NotNull ConfigCategoryBuilder n(ConfigGroupBuilder group, int index);
	
	/**
	 * Set the description for this config category.<br>
	 * <br>
	 * Category descriptions are mapped automatically from translation keys.
	 * This method exists only for wrapped categories.
	 */
	@Internal @Contract("_ -> this") @NotNull ConfigCategoryBuilder withDescription(Supplier<List<ITextComponent>> description);
}
