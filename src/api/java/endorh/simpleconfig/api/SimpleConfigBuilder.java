package endorh.simpleconfig.api;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import endorh.simpleconfig.api.ui.icon.Icon;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public interface SimpleConfigBuilder extends ConfigEntryHolderBuilder<SimpleConfigBuilder> {
	/**
	 * Set the baker method for this config<br>
	 * You may also define a '{@code bake}' static method
	 * in the config class accepting a {@link SimpleConfig}
	 * and it will be set automatically as the baker (but you
	 * may not define it and also call this method)
	 */
	@Contract("_ -> this") @NotNull SimpleConfigBuilder withBaker(Consumer<SimpleConfig> baker);
	
	/**
	 * Set the default background for all categories
	 *
	 * @see #withBackground(ResourceLocation)
	 */
	@Contract("_ -> this") @NotNull SimpleConfigBuilder withBackground(String resourceName);
	
	/**
	 * Set the default background for all categories
	 *
	 * @see #withBackground(String)
	 * @see #withIcon(Icon)
	 * @see #withColor(int)
	 */
	@Contract("_ -> this") @NotNull SimpleConfigBuilder withBackground(ResourceLocation background);
	
	/**
	 * Set the icon for the default category.<br>
	 * Doesn't affect other categories.<br>
	 * The icon is displayed in the tab button for the category, when more than
	 * one category is present.
	 *
	 * @param icon Icon to display. Use {@link Icon#EMPTY} to display no icon (default).
	 * @see #withColor(int)
	 * @see #withBackground(ResourceLocation)
	 */
	@Contract("_ -> this") @NotNull SimpleConfigBuilder withIcon(Icon icon);
	
	/**
	 * Set the color for the default category.<br>
	 * Doesn't affect other categories.<br>
	 * The color affects the tint applied to the tab button for the category,
	 * visible when more than one category is present.
	 *
	 * @param tint Color tint to use, in ARGB format. It's recommended
	 *   a transparency of 0x80, so the button background is
	 *   visible behind the color. A value of 0 means no tint.
	 * @see #withColor(int)
	 * @see #withBackground(ResourceLocation)
	 */
	@Contract("_ -> this") @NotNull SimpleConfigBuilder withColor(int tint);
	
	/**
	 * Use the solid background too when in-game<br>
	 * By default, config GUIs are transparent when in-game
	 */
	@Contract("-> this") @NotNull SimpleConfigBuilder withSolidInGameBackground();
	
	/**
	 * Register the config command at the given command root<br>
	 * The config command will still be accessible at {@code /config ⟨sub⟩ ⟨modid⟩}<br>
	 */
	@Contract("_ -> this") @NotNull SimpleConfigBuilder withCommandRoot(
	  LiteralArgumentBuilder<CommandSourceStack> root);
	
	@Contract("-> this") @NotNull @Override SimpleConfigBuilder restart();
	
	@Contract("_ -> this") @NotNull SimpleConfigBuilder n(ConfigCategoryBuilder cat);
	
	@Contract("_, _ -> this") @NotNull SimpleConfigBuilder n(ConfigCategoryBuilder cat, int index);
	
	@Contract("_, _ -> this") @NotNull SimpleConfigBuilder n(ConfigGroupBuilder group, int index);
	
	/**
	 * Build the actual config and register it within the Forge system<br><br>
	 * <b>If your mod uses a different language than Java</b> you will need to
	 * also pass in your mod event bus as an argument to
	 * {@link #buildAndRegister(IEventBus)}
	 *
	 * @return The built config, which is also received by the baker
	 */
	@NotNull SimpleConfig buildAndRegister();
	
	/**
	 * Build the actual config and register it within the Forge system<br><br>
	 * <i>If your mod uses Java as its language</i> you don't need to pass
	 * the mod event bus
	 *
	 * @param modEventBus Your mod's language provider's mod event bus
	 * @return The built config, which is also received by the baker
	 */
	@NotNull SimpleConfig buildAndRegister(@NotNull IEventBus modEventBus);
}
