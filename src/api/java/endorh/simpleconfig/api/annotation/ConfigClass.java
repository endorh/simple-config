package endorh.simpleconfig.api.annotation;

import endorh.simpleconfig.api.SimpleConfig.Type;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as defining a Simple Config file.<br><br>
 *
 * This makes it possible to declare config files without depending
 * on Simple Config at runtime, although the lack of Simple Config
 * at runtime will result in your mod lacking a config file/menu/commands.<br><br>
 *
 * @see Entry
 * @see Category
 * @see Group
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigClass {
	/**
	 * The mod ID associated to this config file.
	 */
	String modId();
	
	/**
	 * The {@link Type} of the config file, usually either {@code CLIENT} or {@code SERVER}
	 */
	Type type() default Type.CLIENT;
	
	/**
	 * Configure the background texture to use for this config file.<br>
	 * It will be used as the default background for all categories.<br>
	 * The texture will be tiled to fill the entire background.<br><br>
	 *
	 * It's recommended to use a block texture, such as
	 * {@code "textures/block/warped_planks.png"}, but any texture can
	 * be used.<br><br>
	 *
	 * The default texture is {@code "textures/gui/options_background.png"},
	 * which uses the classic dirt block used in Minecraft menus.
	 *
	 * @see #color()
	 */
	String background() default "";
	
	/**
	 * Configure the tint used for the tab button of the default category
	 * of this config.<br>
	 * It'll only affect the default category, not other categories,
	 * as {@link #background()} does.<br><br>
	 *
	 * The color must be in AARRGGBB format. The alpha channel determines how intensely does the
	 * tint obscure the underlying button texture.<br>
	 * By default, tab buttons have no tint (they are gray).
	 *
	 * @see #background()
	 */
	int color() default 0;
}
