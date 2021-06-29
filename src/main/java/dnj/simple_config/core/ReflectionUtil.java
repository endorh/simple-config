package dnj.simple_config.core;

import com.google.gson.internal.Primitives;
import dnj.simple_config.core.SimpleConfigClassParser.ConfigClassParseException;

import javax.annotation.Nullable;
import java.lang.reflect.*;
import java.util.Arrays;

class ReflectionUtil {
	
	/**
	 * Get the full field name, suitable for error messages
	 */
	public static String getFieldName(Field field) {
		return field.getDeclaringClass().getName() + "#" + field.getName();
	}
	
	/**
	 * Get the full method name, suitable for error messages
	 */
	public static String getMethodName(Method method) {
		return method.getDeclaringClass().getName() + "#" + method.getName();
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
	 * Try to invoke method<br>
	 * Convenient for lambdas
	 * @param errorMsg Error message added to the exception on error<br>
	 *                 Will be formatted with the method name
	 * @throws ConfigClassParseException on error
	 */
	public static <T> T invoke(Method method, Object self, String errorMsg, Object... args) {
		try {
			//noinspection unchecked
			return (T) method.invoke(self, args);
		} catch (InvocationTargetException | IllegalAccessException | ClassCastException e) {
			throw new ConfigClassParseException(
			  String.format(errorMsg, getMethodName(method)) +
			  "\n  Details: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Check if the actual first-layer type parameters of the
	 * declared class for the field match the given types
	 * @param types Expected types, where null means don't care
	 *              and the length is also checked
	 */
	public static boolean checkTypeParameters(Field field, Class<?>... types) {
		return checkTypeParameters((ParameterizedType) field.getGenericType(), types);
	}
	
	
	/**
	 * Check if the actual first-layer type parameters of the passed type
	 * match the expected
	 * @param types Expected types, where null means don't care
	 *              and the length is also checked
	 */
	public static boolean checkTypeParameters(ParameterizedType type, Class<?>... types) {
		final Type[] actualTypes = type.getActualTypeArguments();
		if (actualTypes.length != types.length) return false;
		for (int i = 0; i < types.length; i++) {
			if (types[i] != null && !types[i].equals(actualTypes[i]))
				return false;
		}
		return true;
	}
	
	/**
	 * Try to find a method with the given parameter types or their
	 * primitives
	 * @return The found method made accessible or null if not found
	 */
	protected static @Nullable Method tryGetMethod(
	  Class<?> clazz, String name, Class<?>... parameterTypes
	) { return tryGetMethod(clazz, name, null, parameterTypes); }
	
	/**
	 * Try to find a method with the given parameter types or their
	 * primitives
	 * @param suffix Appended after a '$' to the name of the method
	 * @return The found method made accessible or null if not found
	 */
	protected static @Nullable Method tryGetMethod(
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
