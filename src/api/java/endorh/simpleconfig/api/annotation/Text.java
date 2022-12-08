package endorh.simpleconfig.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generate a text entry in the position of this <b>static</b> field
 * within the config class<br>
 * The type of this field must be either:
 * <ul>
 *    <li>{@code Void}, mapped to a translation according to its name and
 *    path within the config</li>
 *    <li>{@code ITextComponent}, which is displayed as is.
 *    It may be a {@code TranslationTextComponent}, or even have
 *    click/hover events through {@code withStyle}</li>
 *    <li>{@code Supplier<ITextComponent>} which will be invoked
 *    every time the GUI is built and thus may be able to provide
 *    dynamic information</li>
 * </ul>
 * Making fields used for text entries non-public helps keep a cleaner
 * config API for your project<br><br>
 *
 * @see Entry
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Text {
	/**
	 * Preferred order to be inserted in the config (relative to its siblings)<br>
	 * You shouldn't need to use this.
	 */
	int value() default 0;
}
