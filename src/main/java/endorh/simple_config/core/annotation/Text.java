package endorh.simple_config.core.annotation;

import endorh.simple_config.core.SimpleConfigBuilder;

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
 * the {@link SimpleConfigBuilder} methods
 * instead, because it provides more options and is less prone
 * to errors<br>
 * Declaring text entries in the backing class forces you to define
 * dummy fields
 *
 * @see Entry
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Text {}
