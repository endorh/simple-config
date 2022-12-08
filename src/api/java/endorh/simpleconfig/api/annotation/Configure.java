package endorh.simpleconfig.api.annotation;

import endorh.simpleconfig.api.ConfigEntryBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used as an extension point for Simple Config's
 * declarative API.<br><br>
 *
 * It can be used to decorate the automatically generated config entry
 * builders, by linking to a method that will accept them and transform
 * them using the builder API.<br>
 *
 * While it can be applied directly to fields/inner types, the main use
 * is to apply it to a custom annotation, which can then be used on
 * many entry types.<br>
 *
 * The specified method must be static, and its path is relative to the
 * target of this annotation:
 * <ul><li>
 *    If applied to a field, the method must be in the same class
 *    as the field, and can be relativized to the field name by
 *    using {@code '$'} as a prefix (as with any other annotation
 *    that takes method references)
 * </li><li>
 *    If applied to a custom annotation, the annotation is expected to
 *    be defined within a class, where the method can be found.
 * </li></ul>
 *
 * Multiple methods with different signatures can be defined for the
 * same name. In this case, only the most specific to the entry type
 * will be used.<br>
 * Methods in containing config classes are only considered if none
 * applicable is found directly in the paths specified above, that is,
 * closeness is compared before specificity.<br><br>
 *
 * Decorator methods must have one of the following signatures:
 * <ul><li>
 *    {@code BuilderType(BuilderType, AnnotationType)}, if this annotation
 *    is being applied to a custom annotation of {@code AnnotationType} type.
 *    This makes possible to use the custom annotation's arguments
 *    in the decorator method.
 * </li><li>
 *    {@code BuilderType(BuilderType)}, if this annotation is not being
 *    applied to a custom annotation, or the annotation arguments are
 *    unneeded in the decorator method.
 * </li></ul>
 *
 * The {@code BuilderType} used above can be any subtype of {@link ConfigEntryBuilder},
 * (or {@link ConfigEntryBuilder} for completely generic decorators), and
 * it'll be used to determine which is the most specific method to use
 * for a given entry if multiple are available, or to determine if there's
 * no applicable decorator for a certain type of entry (which will produce
 * an error, as if you used a built-in annotation for a type it doesn't support).<br><br>
 *
 * <h2>Samples</h2>
 * <h3>Custom annotation with arguments</h3>
 * <pre>{@code
 *    // Class used to hold your custom annotations, along
 *    //   with their decorator methods
 *    public static class MyCustomAnnotations {
 *       @Target( {ElementType.FIELD, ElementType.TYPE_USE, ElementType.ANNOTATION_TYPE} )
 *       @Retention(RetentionPolicy.RUNTIME)
 *       @Configure("decExtraSlider")
 *       public @interface ExtraSlider {
 *          int min() default 0;
 *          int extraMin() default 0;
 *          int max() default 100;
 *          int extraMax() default 200;
 *       }
 *
 *       // Decorator methods for entry types that support the @ExtraSlider
 *       public static IntegerEntryBuilder decExtraSlider(
 *          IntegerEntryBuilder builder, ExtraSlider s
 *       ) {
 *          return builder.range(s.extraMin(), s.extraMax()).sliderRange(s.min(), s.max());
 *       }
 *
 *       // In this sample we only define decorators for int and float entries
 *       // It'd also be possible to define a single decorator for `RangedEntryBuilder`,
 *       //   however, this can make the method harder to write in a generic way
 *       public static FloatEntryBuilder decExtraSlider(
 *          FloatEntryBuilder builder, ExtraSlider s
 *       ) {
 *          return builder.range(s.extraMin(), s.extraMax()).sliderRange(s.min(), s.max());
 *       }
 *    }
 *
 *    @Category public static class config_category {
 *       // The entries generated for the below entries will be decorated by the
 *       //   methods defined above
 *       @Entry @ExtraSlider public static int int_slider;
 *       @Entry @ExtraSlider(max=1, extraMax=2) public static float float_slider;
 *    }
 * }</pre>
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Configure {
	/**
	 * Method reference to a configuring method for this entry.<br><br>
	 * The method must be static, accept a single parameter of a type
	 * to which an entry builder for this entry can be assigned,
	 * and must return the modified builder.<br>
	 */
	String value();
}
