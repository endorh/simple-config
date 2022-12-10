package endorh.simpleconfig.core;

import com.google.gson.internal.Primitives;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;

public record EntryType<T>(
  @NotNull Class<T> type, EntryType<?> @Nullable[] args, boolean subClasses
) {
	public EntryType {
		type = Primitives.wrap(type);
		if (args != null) {
			int expected = type.getTypeParameters().length;
			if (args.length != expected) throw argumentCountMismatch(type, expected, args.length);
		}
	}
	
	public static <T> EntryType<T> of(Class<T> type, EntryType<?>... args) {
		return new EntryType<>(type, args, false);
	}
	
	public static <T> EntryType<T> unchecked(Class<T> type) {
		return new EntryType<>(type, null, false);
	}
	
	public static <T> EntryType<T> uncheckedSubClasses(Class<T> type) {
		return new EntryType<>(type, null, true);
	}
	
	public static EntryType<Object> wildcard() {
		return uncheckedSubClasses(Object.class);
	}
	
	public static <T> EntryType<T> from(Class<T> type, Class<?>... args) {
		int n = type.getTypeParameters().length;
		if (n > args.length) throw argumentCountMismatch(type, n, args.length);
		EntryType<?>[] a = new EntryType<?>[n];
		int j = 0;
		for (int i = 0; i < n; i++) {
			if (j > args.length) throw argumentCountMismatch(type, n, args.length);
			Class<?> next = args[j];
			a[i] = from(next, ArrayUtils.subarray(args, i + 1, args.length));
			j += 1 + a[i].parameterCount();
		}
		if (j != args.length) throw argumentCountMismatch(type, n, args.length);
		return EntryType.of(type, a);
	}
	
	public static @Nullable EntryType<?> fromField(Field field) {
		return fromType(field.getGenericType());
	}
	
	public static @Nullable EntryType<?> fromMethod(Method method) {
		return fromType(method.getGenericReturnType());
	}
	
	public static @Nullable EntryType<?> fromType(Type type) {
		if (type instanceof Class<?> cls) return EntryType.of(cls);
		if (type instanceof ParameterizedType t) {
			if (t.getOwnerType() != null) return null;
			EntryType<?>[] args = Arrays.stream(t.getActualTypeArguments())
			  .map(EntryType::fromType)
			  .toArray(EntryType<?>[]::new);
			return EntryType.of((Class<?>) t.getRawType(), args);
		} else return null;
	}
	
	public int parameterCount() {
		return args != null? args.length : 0;
	}
	
	public boolean matches(EntryType<?> other) {
		return (subClasses? type.isAssignableFrom(other.type) : type.equals(other.type))
		       && (args == null || other.args == null
		           || args.length == other.args.length
		              && IntStream.range(0, args.length)
		                .allMatch(i -> args[i].matches(other.args[i])));
	}
	
	@Override public String toString() {
		if (args == null || args.length == 0) return type.getSimpleName();
		return type.getSimpleName() + "<" + String.join(
		  ", ", Arrays.stream(args).map(EntryType::toString)
			 .toArray(String[]::new)) + ">";
	}
	
	private static IllegalArgumentException argumentCountMismatch(
	  Class<?> type, int expected, int actual
	) {
		return new IllegalArgumentException(
		  "Wrong number of type parameters (" + actual + ") for type " +
		  type.getCanonicalName() + " (expected " + expected + ")");
	}
	
	@Override public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EntryType<?> entryType = (EntryType<?>) o;
		return type.equals(entryType.type) && Arrays.equals(args, entryType.args);
	}
	
	@Override public int hashCode() {
		int result = Objects.hash(type);
		result = 31 * result + Arrays.hashCode(args);
		return result;
	}
}
