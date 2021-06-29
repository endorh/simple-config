package dnj.simple_config.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
// TODO: Rename as Entry
public @interface ConfigEntry {
	
	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface Long {
		long min() default java.lang.Long.MIN_VALUE;
		long max() default java.lang.Long.MAX_VALUE;
		boolean slider() default false;
	}
	
	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface Double {
		double min() default java.lang.Double.NEGATIVE_INFINITY;
		double max() default java.lang.Double.POSITIVE_INFINITY;
	}
	
	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface Color {
		boolean alpha() default false;
	}
	
	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface List {
		
		@Target(ElementType.FIELD)
		@Retention(RetentionPolicy.RUNTIME)
		@interface Long {
			long min() default java.lang.Long.MIN_VALUE;
			long max() default java.lang.Long.MAX_VALUE;
		}
		
		@Target(ElementType.FIELD)
		@Retention(RetentionPolicy.RUNTIME)
		@interface Double {
			double min() default java.lang.Double.MIN_VALUE;
			double max() default java.lang.Double.MAX_VALUE;
		}
	}
}
