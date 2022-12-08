package endorh.simpleconfig.core.reflection;

import endorh.simpleconfig.api.annotation.Configure;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.util.*;

public interface EntryTypeData extends Iterable<Annotation> {
	static EntryTypeData fromField(MethodBindingContext ctx, Field field) {
		return new EntryTypeDataImpl(ctx, field.getAnnotations());
	}
	
	static EntryTypeData fromType(MethodBindingContext ctx, AnnotatedType aType) {
		return new EntryTypeDataImpl(ctx, aType.getAnnotations());
	}
	
	static EntryTypeData merge(EntryTypeData a, EntryTypeData b) {
		MethodBindingContext aCtx = a.getMethodBindingContext();
		MethodBindingContext bCtx = b.getMethodBindingContext();
		if (aCtx != bCtx) throw new IllegalArgumentException(
		  "Can't merge two EntryTypeData instances from different contexts");
		EntryTypeDataImpl set = new EntryTypeDataImpl(aCtx);
		for (Annotation an: b) set.map.put(an.annotationType(), an);
		for (Annotation an: a) set.map.put(an.annotationType(), an);
		set.entryAnnotations.putAll(b.getConfigureAnnotations());
		set.entryAnnotations.putAll(a.getConfigureAnnotations());
		return set;
	}
	
	MethodBindingContext getMethodBindingContext();
	
	default Class<?> getContextClass() {
		return getMethodBindingContext().getContextClass();
	}
	
	boolean contains(Class<? extends Annotation> cls);
	
	<A extends Annotation> Optional<A> get(Class<A> cls);
	<A extends Annotation> A getOrNull(Class<A> cls);
	
	Class<? extends Annotation>[] getUnusedAnnotations();
	
	Map<Configure, Class<? extends Annotation>> getConfigureAnnotations();
	
	class EntryTypeDataImpl implements EntryTypeData {
		private final MethodBindingContext methodBindingContext;
		private final Map<Class<? extends Annotation>, Annotation> map = new HashMap<>();
		private final Set<Class<? extends Annotation>> usedAnnotations = new HashSet<>();
		private final Map<Configure, Class<? extends Annotation>> entryAnnotations =
		  new LinkedHashMap<>();
		
		public EntryTypeDataImpl(
		  MethodBindingContext ctx, Annotation... annotations
		) {
			methodBindingContext = ctx;
			for (Annotation a: annotations) addRecursive(null, a);
		}
		
		private void addRecursive(Annotation parent, Annotation a) {
			Class<? extends Annotation> ac = a.annotationType();
			map.put(ac, a);
			if (a instanceof Configure e) entryAnnotations.put(e, parent != null? parent.annotationType() : null);
			for (Annotation sub: ac.getAnnotations()) {
				Class<? extends Annotation> sc = sub.annotationType();
				if (sc == Target.class || sc == Retention.class) continue;
				if (!map.containsKey(sc)) addRecursive(a, sub);
			}
		}
		
		@Override public MethodBindingContext getMethodBindingContext() {
			return methodBindingContext;
		}
		
		@Override public boolean contains(Class<? extends Annotation> cls) {
			return map.containsKey(cls);
		}
		
		@Override public <A extends Annotation> Optional<A> get(Class<A> cls) {
			return Optional.ofNullable(getOrNull(cls));
		}
		
		@Override public <A extends Annotation> A getOrNull(Class<A> cls) {
			usedAnnotations.add(cls);
			return cls.cast(map.get(cls));
		}
		
		@SuppressWarnings("unchecked") @Override public Class<? extends Annotation>[] getUnusedAnnotations() {
			return map.keySet().stream()
			  .filter(a -> !usedAnnotations.contains(a))
			  .toArray(Class[]::new);
		}
		
		@Override public Map<Configure, Class<? extends Annotation>> getConfigureAnnotations() {
			return entryAnnotations;
		}
		
		@NotNull @Override public Iterator<Annotation> iterator() {
			return map.values().iterator();
		}
	}
}
