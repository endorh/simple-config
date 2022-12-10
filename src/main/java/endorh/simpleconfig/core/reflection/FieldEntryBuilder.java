package endorh.simpleconfig.core.reflection;

import endorh.simpleconfig.api.ConfigEntryBuilder;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@FunctionalInterface
public interface FieldEntryBuilder<V> {
	ConfigEntryBuilder<?, ?, ?, ?> build(
	  BindingContext ctx, EntryTypeData data, AnnotatedType aType, Type type, Class<?> c, Optional<V> v);
	
	default ConfigEntryBuilder<?, ?, ?, ?> build(
	  BindingContext ctx, EntryTypeData data, AnnotatedType aType, Type type, Optional<V> v
	) {
		if (type instanceof Class<?> c) return build(ctx, data, aType, type, c, v);
		if (type instanceof ParameterizedType t) return build(ctx, data, aType, type, (Class<?>) t.getRawType(), v);
		throw new IllegalArgumentException("Unexpected type: " + type);
	}
	
	@FunctionalInterface
	interface ClassFieldEntryBuilder<V> extends FieldEntryBuilder<V> {
		ConfigEntryBuilder<?, ?, ?, ?> build(EntryTypeData data, Class<?> t, Optional<V> v);
		
		@Override default ConfigEntryBuilder<?, ?, ?, ?> build(
		  BindingContext ctx, EntryTypeData data, AnnotatedType aType, Type type, Class<?> c, Optional<V> v
		) {
			return build(data, c, v);
		}
	}
	
	@FunctionalInterface
	interface SimpleFieldEntryBuilder<V> extends FieldEntryBuilder<V> {
		ConfigEntryBuilder<?, ?, ?, ?> build(EntryTypeData data, Optional<V> v);
		
		@Override default ConfigEntryBuilder<?, ?, ?, ?> build(
		  BindingContext ctx, EntryTypeData data, AnnotatedType aType, Type t, Class<?> c, Optional<V> v
		) {
			return build(data, v);
		}
	}
}
