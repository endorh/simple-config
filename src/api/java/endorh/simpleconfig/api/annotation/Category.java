package endorh.simpleconfig.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a <b>static</b> inner class as the backing
 * field for a generated config category<br><br>
 *
 * If the category is already defined in the config builder, this
 * annotation is unnecessary (and ignored), and the class will
 * be scanned equally.<br><br>
 *
 * If the category builder declares a different backing class
 * for the category, there mustn't be an inner class with the
 * same name under the main config's backing class, that is,
 * a category can not have its backing class defined in two places<br><br>
 *
 * You may want to define a <b>marker field</b> for this category, if you want
 * to ensure it's added in a certain position with respect to other
 * categories. A marker field is a field of {@link Void} type, annotated
 * with {@link Category}, and the same name as this category,
 * followed by '{@code $marker}'.<br>
 * @see Group
 * @see Entry
 */
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Category {
	/**
	 * The preferred order where to place this group relative to its siblings, including
	 * entries at the same level.<br>
	 * By default, it's added in random order, since Java does not
	 * allow to travel inner classes in source order.<br><br>
	 * It's recommended you use a marker field instead (see the documentation for {@link Category}).
	 */
	int value() default 0;
	
	/**
	 * Configure the background texture to use for this config {@code @Category}.<br>
	 * The texture will be tiled to fill the entire background.<br><br>
	 *
	 * It's recommended to use a block texture, such as
	 * {@code "textures/block/warped_planks.png"}, but any texture can
	 * be used.<br><br>
	 *
	 * The default texture is {@code "textures/gui/options_background.png"},
	 * which uses the classic dirt block used in Minecraft menus.
	 * @see #color()
	 */
	String background() default "";
	
	/**
	 * Configure the tint used for the tab button of this {@code @Category}.<br><br>
	 *
	 * The color must be in AARRGGBB format. The alpha channel determines how intensely does the
	 * tint obscure the underlying button texture.<br>
	 * By default, tab buttons have no tint (they are gray).
	 *
	 * @see #background()
	 */
	int color() default 0;
}
