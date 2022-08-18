package endorh.simpleconfig.core;

import com.google.gson.internal.Primitives;
import endorh.simpleconfig.api.SimpleConfig.InvalidConfigValueTypeException;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * Several reflection utilities used to parse config backing classes<br>
 * Mainly contains helpers to check generic types or find methods/fields<br>
 * Use at your own risk
 */
@Internal public class ReflectionUtil {
	
	/**
	 * Get the full field name, suitable for error messages
	 */
	public static String getFieldName(Field field) {
		return field.getDeclaringClass().getName() + "#" + field.getName();
	}
	
	/**
	 * Get the full field type name, suitable for error messages
	 */
	public static String getFieldTypeName(Field field) {
		return field.getGenericType().getTypeName();
	}
	
	/**
	 * Get the full method name, suitable for error messages
	 */
	public static String getMethodName(Method method) {
		return method.getDeclaringClass().getName() + "#" + method.getName();
	}
	
	/**
	 * Get the full method type name, suitable for error messages
	 */
	public static String getMethodTypeName(Method method) {
		return method.getGenericReturnType().getTypeName();
	}
	
	/**
	 * Get the actual first-layer type parameter at the given index of
	 * the declared return type for the method
	 */
	public static Class<?> getTypeParameter(Method method, int index) {
		return getTypeParameter((ParameterizedType) method.getGenericReturnType(), index);
	}
	
	/**
	 * Get the actual first-layer type parameter at the given index
	 * of the declared class for the field
	 */
	public static Class<?> getTypeParameter(Field field, int index) {
		return getTypeParameter((ParameterizedType) field.getGenericType(), index);
	}
	
	/**
	 * Get the actual type parameter of a {@link ParameterizedType}
	 */
	public static Class<?> getTypeParameter(ParameterizedType type, int index) {
		return (Class<?>) type.getActualTypeArguments()[index];
	}
	
	/**
	 * Check if the actual type parameters of the declared class
	 * for the field match the given types<br>
	 *
	 * If the type parameters have themselves more type parameters,
	 * those are checked too, in the order they would be written<br>
	 *
	 * For example calling with types
	 * <pre>{@code
	 *    checkTypeParameters(type,
	 *       Function.class, List.class, Long.class, List.class, null)
	 * }</pre>
	 * would match a type of {@code Function<List<Long>, List<?>>}
	 * @param types Expected types, where null means don't care
	 */
	public static boolean checkType(Field field, Class<?> type, Class<?>... types) {
		return Primitives.wrap(field.getType()).equals(Primitives.wrap(type))
		       && (field.getGenericType() instanceof ParameterizedType
		           ? checkTypeParameters((ParameterizedType) field.getGenericType(), types)
		           : types.length == 0);
	}
	
	/**
	 * Check if the actual type parameters of the declared return type
	 * for the method match the given types, in the order they would be written<br>
	 *
	 * If the type parameters have themselves more type parameters,
	 * those are checked too, in the order they would be written<br>
	 *
	 * For example calling with types
	 * <pre>{@code
	 *    checkTypeParameters(type,
	 *       Function.class, List.class, Long.class, List.class, null)
	 * }</pre>
	 * would match a type of {@code Function<List<Long>, List<?>>}
	 * @param types Expected types, where null means don't care
	 */
	public static boolean checkType(Method method, Class<?> type, Class<?>... types) {
		return Primitives.wrap(method.getReturnType()).equals(Primitives.wrap(type))
		       && (method.getGenericReturnType() instanceof ParameterizedType
		           ? checkTypeParameters((ParameterizedType) method.getGenericReturnType(), types)
		           : types.length == 0);
	}
	
	
	/**
	 * Check if the actual type parameters of the passed type
	 * match the expected<br>
	 *
	 * If the type parameters have themselves more type parameters,
	 * those are checked too, in the order they would be written<br>
	 *
	 * For example calling with types
	 * <pre>{@code
	 *    checkTypeParameters(type,
	 *       Function.class, List.class, Long.class, List.class, null)
	 * }</pre>
	 * would match a type of {@code Function<List<Long>, List<?>>}
	 * @param types Expected types, where null means don't care
	 */
	public static boolean checkTypeParameters(ParameterizedType type, Class<?>... types) {
		return checkTypeParameters(type, 0, types) == types.length;
	}
	
	private static int checkTypeParameters(ParameterizedType type, int start, Class<?>... types) {
		final Type[] actualTypes = type.getActualTypeArguments();
		int j = start;
		for (Type actual : actualTypes) {
			if (j >= types.length) return -1;
			if (types[j] != null) {
				if (actual instanceof Class<?> && !types[j].equals(actual))
					return -1;
				if (actual instanceof ParameterizedType) {
					if (!types[j].equals(((ParameterizedType) actual).getRawType()))
						return -1;
					j = checkTypeParameters((ParameterizedType) actual, j + 1, types) - 1;
					if (j < 0) return -1;
				}
			}
			j++;
		}
		return j;
	}
	
	/**
	 * Try to find a method with the given parameter types or their
	 * primitives
	 * @return The found method made accessible or null if not found
	 */
	public static @Nullable Method tryGetMethod(
	  Class<?> clazz, String name, Class<?>... parameterTypes
	) { return tryGetMethod(clazz, name, null, parameterTypes); }
	
	/**
	 * Try to find a method with the given parameter types or their
	 * primitives
	 * @param suffix Appended after a '$' to the name of the method
	 * @return The found method made accessible or null if not found
	 */
	public static @Nullable Method tryGetMethod(
	  Class<?> clazz, String name, String suffix, Class<?>... parameterTypes
	) {
		final String methodName = suffix != null ? name + "$" + suffix : name;
		try {
			final Method m = clazz.getDeclaredMethod(methodName, parameterTypes);
			m.setAccessible(true);
			return m;
		} catch (NoSuchMethodException ignored) {
			// Try finding method with primitive types
			try {
				final Class<?>[] parameterTypesAsPrimitives = Arrays.stream(parameterTypes)
				  .map(Primitives::unwrap).toArray(Class<?>[]::new);
				if (Arrays.equals(parameterTypesAsPrimitives, parameterTypes))
					return null;
				final Method m = clazz.getDeclaredMethod(methodName, parameterTypesAsPrimitives);
				m.setAccessible(true);
				return m;
			} catch (NoSuchMethodException ignored1) {}
			return null;
		}
	}
	
	/**
	 * Attempt to set a field to a value, casting numeric primitive values if necessary
	 */
	protected static <V> void setBackingField(Field field, V value) throws IllegalAccessException {
		final Class<?> type = Primitives.unwrap(field.getType());
		try {
			if (type.isPrimitive()) {
				if (type == boolean.class) {
					field.set(null, value);
				} else if (type == char.class) {
					field.set(null, value);
				} else {
					Number n = (Number) value;
					if (type == byte.class) {
						field.set(null, n.byteValue());
					} else if (type == short.class) {
						field.set(null, n.shortValue());
					} else if (type == int.class) {
						field.set(null, n.intValue());
					} else if (type == long.class) {
						field.set(null, n.longValue());
					} else if (type == float.class) {
						field.set(null, n.floatValue());
					} else if (type == double.class) {
						field.set(null, n.doubleValue());
					} else throw new IllegalStateException("Unknown primitive type: " + type.getTypeName());
				}
			} else field.set(null, value);
		} catch (ClassCastException e) {
			throw new InvalidConfigValueTypeException(field.getName(), e);
		}
	}
	
	protected static Field getFieldOrNull(Class<?> clazz, String name) {
		try {
			final Field f = clazz.getDeclaredField(name);
			f.setAccessible(true);
			return f;
		} catch (NoSuchFieldException e) {
			return null;
		}
	}
	
	protected static <T> T getFieldValueOrNull(Object object, Field field) {
		if (field == null || object == null)
			return null;
		try {
			//noinspection unchecked
			return (T) field.get(object);
		} catch (IllegalAccessException | ClassCastException e) {
			return null;
		}
	}
}
