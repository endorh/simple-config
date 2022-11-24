package endorh.simpleconfig.api;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import endorh.simpleconfig.api.ui.icon.Icon;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Predicate;

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
	 * Restrict dynamically which categories are displayed in the config GUI.<br>
	 * Intended to hide config categories only relevant in certain environments,
	 * or for certain players. Do not abuse it as a permission system.<br><br>
	 * Keep in mind that this will only hide the category from the GUI, but players
	 * will still be able to access it via commands or the config files.
	 */
	@Contract("_ -> this") @NotNull SimpleConfigBuilder withDynamicGUICategoryFilter(Predicate<SimpleConfigCategory> filter);
	
	/**
	 * Register the config command at the given command root<br>
	 * The config command will still be accessible at {@code /config ⟨sub⟩ ⟨modid⟩}<br>
	 */
	@Contract("_ -> this") @NotNull SimpleConfigBuilder withCommandRoot(
	  LiteralArgumentBuilder<CommandSourceStack> root);
	
	/**
	 * Add a config category<br>
	 * Create a category with {@link ConfigBuilderFactoryProxy#category}.<br>
	 * Categories have their own tab in the config menu.
	 */
	@Contract("_ -> this") @NotNull SimpleConfigBuilder n(ConfigCategoryBuilder cat);
	
	/**
	 * Add a config category at the given index.<br>
	 * Create a category with {@link ConfigBuilderFactoryProxy#category}.<br>
	 * Categories have their own tab in the config menu.<br><br>
	 * You shouldn't need to specify the index under normal circumstances.
	 */
	@Contract("_, _ -> this") @NotNull SimpleConfigBuilder n(ConfigCategoryBuilder cat, int index);
	
	/**
	 * Add a config group at the given index.<br>
	 * Create a group with {@link ConfigBuilderFactoryProxy#group}.<br>
	 * Groups are displayed as collapsible lists of entries in the menu.
	 */
	@Contract("_, _ -> this") @NotNull SimpleConfigBuilder n(ConfigGroupBuilder group, int index);
	
	/**
	 * Build the actual config and register it within the Forge system<br><br>
	 * <b>If your mod uses a different language than Java</b> you will need to
	 * also pass in your mod event bus as an argument to
	 * {@link #buildAndRegister(IEventBus)}<br><br>
	 * <b>If your mod uses Kotlin</b>, you should be using the Simple Konfig API
	 * instead.
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
