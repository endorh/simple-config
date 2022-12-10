package endorh.simpleconfig.core.reflection;

import endorh.simpleconfig.core.EntryType;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public interface FieldTypeFilter {
	static FieldTypeFilter matching(EntryType<?> type) {
		return new EntryTypeFieldTypeFilter(type);
	}
	
	static FieldTypeFilter subClasses(Class<?> superClass) {
		return new SubClassFieldTypeFilter(superClass);
	}
	
	static FieldTypeFilter annotated(Class<? extends Annotation> annotation) {
		return new AnnotationFieldTypeFilter(annotation);
	}
	
	default boolean isApplicable(Field field) {
		return isApplicable(field.getType());
	}
	boolean isApplicable(Type type);
	default boolean isApplicable(AnnotatedType type) {
		return isApplicable(type.getType());
	}
	
	class EntryTypeFieldTypeFilter implements FieldTypeFilter {
		private final EntryType<?> type;
		
		public EntryTypeFieldTypeFilter(EntryType<?> type) {
			this.type = type;
		}
		
		@Override public boolean isApplicable(Field field) {
			return type.matches(EntryType.fromField(field));
		}
		
		@Override public boolean isApplicable(Type type) {
			return this.type.matches(EntryType.fromType(type));
		}
	}
	
	class SubClassFieldTypeFilter implements FieldTypeFilter {
		private final Class<?> superClass;
		
		public SubClassFieldTypeFilter(Class<?> superClass) {
			this.superClass = superClass;
		}
		
		@Override public boolean isApplicable(Type type) {
			if (!(type instanceof Class<?>)) return false;
			return superClass.isAssignableFrom((Class<?>) type);
		}
	}
	
	class AnnotationFieldTypeFilter implements FieldTypeFilter {
		private final Class<? extends Annotation> annotation;
		
		public AnnotationFieldTypeFilter(Class<? extends Annotation> annotation) {
			this.annotation = annotation;
		}
		
		@Override public boolean isApplicable(Type type) {
			if (type instanceof ParameterizedType) type = ((ParameterizedType) type).getRawType();
			if (!(type instanceof Class<?>)) return false;
			return ((Class<?>) type).isAnnotationPresent(annotation);
		}
	}
}
