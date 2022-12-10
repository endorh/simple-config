package endorh.simpleconfig.api.annotation;

import endorh.simpleconfig.api.entry.FloatEntryBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify a reference to a baking method, which will transform the entry
 * value before being saved in the field. This transformation must preserve the type.<br><br>
 *
 * The method reference is relative, similar to that of {@link Configure}, and one
 * of the main use cases is to apply this annotation to a custom annotation, which
 * can then apply this baking method to many entry fields.<br><br>
 *
 * The method must have one of the following signatures:
 * <ul><li>
 *    {@code ValueType(ValueType, AnnotationType)}, if this annotation is applied
 *    to a custom annotation of {@code AnnotationType} type. This makes possible
 *    to use the custom annotation's arguments in the baking method.
 * </li><li>
 *    {@code ValueType(ValueType)}, if this annotation is not applied to a custom
 *    annotation, or the annotation arguments are unneeded in the baking method.
 * </li></ul>
 * where {@code ValueType} is the entry's type.<br><br>
 *
 * Unlike the {@link Configure} annotation, this annotation can only be applied once
 * to an entry. If multiple annotations applied to an entry are annotated with
 * {@link Bake}, only the first one found is used.
 *
 * <h2>Samples</h2>
 * <h3>Custom annotation with arguments</h3>
 * <pre>{@code
 *    // Class used to hold your custom annotations, along
 *    //   with their decorator methods
 *    public static class MyCustomAnnotations {
 *       @Target( {ElementType.FIELD, ElementType.TYPE_USE, ElementType.ANNOTATION_TYPE} )
 *       @Retention(RetentionPolicy.RUNTIME)
 *       @Bake(method="map")
 *       public @interface Mapped {
 *          float inputMin() default 0;
 *          float inputMax() default 1;
 *          float outputMin() default 0;
 *          float outputMax() default 10;
 *       }
 *
 *       // Decorator methods for entry types that support the @Mapped annotation
 *       //   It's possible to use primitive types in the method signature
 *       public static float map(float value, Mapped s) {
 *          return (value - s.inputMin()) / (s.inputMax() - s.inputMin())
 *                 * (s.outputMax() - s.outputMin()) + s.outputMin();
 *       }
 *
 *       // This sample annotation only supports float and double entries
 *       public static double map(double value, Mapped s) {
 *          return (value - s.inputMin()) / (s.inputMax() - s.inputMin())
 *                 * (s.outputMax() - s.outputMin()) + s.outputMin();
 *       }
 *    }
 *
 *    @Category public static class config_category {
 *       // The entries generated for the below entries will be decorated by the
 *       //   methods defined above
 *       @Entry @Mapped public static float mapped_float;
 *       @Entry @Mapped(outputMin=10, outputMax=5) public static double mapped_double;
 *    }
 * }</pre>
 *
 * @see Configure
 */
@Target({ElementType.FIELD, ElementType.TYPE_USE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Bake {
	/**
	 * Baking method reference.
	 */
	public String method();
	
	/**
	 * Apply a scale to float/double entries before baking them.<br><br>
	 * The scale is used to multiply the value before saving it in the field.<br>
	 *
	 * Equivalent to {@link FloatEntryBuilder#bakeScale}.
	 */
	public @interface Scale {
		/**
		 * Scale used to multiply the value before saving it in the field.
		 */
		double value();
	}
}
