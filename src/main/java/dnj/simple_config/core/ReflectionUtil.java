package dnj.simple_config.core;

import com.google.gson.internal.Primitives;
import dnj.simple_config.core.SimpleConfig.ConfigReflectiveOperationException;
import dnj.simple_config.core.SimpleConfigClassParser.SimpleConfigClassParseException;
import org.jetbrains.annotations.ApiStatus.Internal;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import javax.annotation.Nullable;
import java.lang.reflect.*;
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
	
	// Warning: Uses internal proprietary API ParameterizedTypeImpl,
	//          which could be subject to change in future Java versions
	private static int checkTypeParameters(ParameterizedType type, int start, Class<?>... types) {
		final Type[] actualTypes = type.getActualTypeArguments();
		int j = start;
		for (Type actual : actualTypes) {
			if (j >= types.length) return -1;
			if (types[j] != null) {
				if (actual instanceof Class<?> && !types[j].equals(actual))
					return -1;
				if (actual instanceof ParameterizedTypeImpl && !types[j].equals(((ParameterizedTypeImpl) actual).getRawType()))
					return -1;
				if (actual instanceof ParameterizedType) {
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
}
