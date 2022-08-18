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
 *    <li>A {@code String}, which value is ignored<br>
 *    It is mapped to a translation according to its name and
 *    path within the config</li>
 *    <li>An {@code ITextComponent}, which is served right-away
 *    It may be a {@code TranslationTextComponent}, or even have
 *    click/hover events through {@code .modifyStyle}</li>
 *    <li>A {@code Supplier<ITextComponent>} which will be invoked
 *    every time the GUI is built and thus may be able to provide
 *    dynamic information</li>
 * </ul>
 * Making fields used for text entries non-public helps keep a cleaner
 * config API for your project<br><br>
 *
 * However, <b>you're encouraged</b> to generate your entries with
 * the {@link ISimpleConfigBuilder} methods
 * instead, because it provides more options and is less prone
 * to errors<br>
 * Declaring text entries in the backing class forces you to define
 * dummy fields
 *
 * @see Entry
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Text {
	/**
	 * Preferred order to be inserted in the config (relative to its siblings)<br>
	 * Should only be necessary when dealing with nested groups in between entries,
	 * but the Java reflection specification won't let me guarantee that
	 */
	int value() default 0;
}
