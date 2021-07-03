package endorh.simple_config.core;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ReflectionUtilTest extends ReflectionUtil {
	
	@Test void testCheckTypeField() throws NoSuchFieldException {
		assertTrue(checkType(getField("bool"), boolean.class));
		assertTrue(checkType(getField("bool"), Boolean.class));
		assertFalse(checkType(getField("bool"), String.class));
		
		assertTrue(checkType(getField("wrapped_bool"), boolean.class));
		assertTrue(checkType(getField("wrapped_bool"), Boolean.class));
		assertFalse(checkType(getField("wrapped_bool"), Long.class));
		
		assertTrue(checkType(getField("string"), String[].class));
		assertFalse(checkType(getField("string"), String.class));
		assertFalse(checkType(getField("string"), Number.class));
		
		assertTrue(checkType(getField("list_list_long"), List.class, List.class, Long[].class));
		assertFalse(checkType(getField("list_list_long"), List.class, List.class, Long.class));
		assertFalse(checkType(getField("list_list_long"), List.class));
		assertTrue(checkType(getField("list_list_long"), List.class, (Class<?>) null));
		assertFalse(checkType(getField("list_list_long"), List.class, List.class));
		assertFalse(checkType(getField("list_list_long"), List.class, List.class, Number.class));
		assertFalse(checkType(getField("list_list_long"), List.class, List.class, Long.class, Long.class));
		
		final String n = "pair_pair_double_list_double_map_number_set_number";
		assertTrue(checkType(
		  getField(n), Pair.class, Pair.class, Double.class, List.class, Double.class,
		  Map.class, Number.class, Set.class, Number.class));
		assertTrue(checkType(
		  getField(n), Pair.class, null,
		  Map.class, Number.class, Set.class, null));
		assertFalse(checkType(
		  getField(n), Pair.class, null,
		  Map.class, Number.class, Set.class, null, null));
		assertTrue(checkType(
		  getField(n), Pair.class, Pair.class, Double.class, null,
		  Map.class, Number.class, null));
		assertFalse(checkType(
		  getField(n), Pair.class, Pair.class, Long.class, null,
		  Map.class, Number.class, null));
	}
	
	@Test void testCheckTypeMethod() throws NoSuchMethodException {
		assertTrue(checkType(getMethod("_void"), Void.class));
		assertFalse(checkType(getMethod("_void"), Object.class));
		
		assertTrue(checkType(getMethod("bool"), boolean.class));
		assertTrue(checkType(getMethod("bool"), Boolean.class));
		assertFalse(checkType(getMethod("bool"), String.class));
		
		assertTrue(checkType(getMethod("wrapped_bool"), boolean.class));
		assertTrue(checkType(getMethod("wrapped_bool"), Boolean.class));
		assertFalse(checkType(getMethod("wrapped_bool"), Long.class));
		
		assertTrue(checkType(getMethod("string"), String[].class));
		assertFalse(checkType(getMethod("string"), String.class));
		assertFalse(checkType(getMethod("string"), Number.class));
		
		assertTrue(checkType(getMethod("list_list_long"), List.class, List.class, Long[].class));
		assertFalse(checkType(getMethod("list_list_long"), List.class, List.class, Long.class));
		assertFalse(checkType(getMethod("list_list_long"), List.class));
		assertTrue(checkType(getMethod("list_list_long"), List.class, (Class<?>) null));
		assertFalse(checkType(getMethod("list_list_long"), List.class, List.class));
		assertFalse(checkType(getMethod("list_list_long"), List.class, List.class, Number.class));
		assertFalse(checkType(getMethod("list_list_long"), List.class, List.class, Long.class, Long.class));
		
		final String n = "pair_pair_double_list_double_map_number_set_number";
		assertTrue(checkType(
		  getMethod(n), Pair.class, Pair.class, Double.class, List.class, Double.class,
		  Map.class, Number.class, Set.class, Number.class));
		assertTrue(checkType(
		  getMethod(n), Pair.class, null,
		  Map.class, Number.class, Set.class, null));
		assertFalse(checkType(
		  getMethod(n), Pair.class, null,
		  Map.class, Number.class, Set.class, null, null));
		assertTrue(checkType(
		  getMethod(n), Pair.class, Pair.class, Double.class, null,
		  Map.class, Number.class, null));
		assertFalse(checkType(
		  getMethod(n), Pair.class, Pair.class, Long.class, null,
		  Map.class, Number.class, null));
	}
	
	public static Field getField(String name) throws NoSuchFieldException {
		return ReflectionUtilTest.class.getField(name);
	}
	
	public static Method getMethod(String name) throws NoSuchMethodException {
		return ReflectionUtilTest.class.getMethod(name);
	}
	
	public static boolean bool;
	public static Boolean wrapped_bool;
	public static String[] string;
	public static List<List<Long[]>> list_list_long;
	public static List<List<?>> list_list_null;
	public static Pair<Pair<Double, List<Double>>, Map<Number, Set<Number>>>
	  pair_pair_double_list_double_map_number_set_number;
	
	public static void _void() {}
	public static boolean bool() { return false; }
	public static Boolean wrapped_bool() { return null; }
	public static String[] string() { return null; }
	public static List<List<Long[]>> list_list_long() { return null; }
	public static List<List<?>> list_list_null() { return null; }
	public static Pair<Pair<Double, List<Double>>, Map<Number, Set<Number>>>
	  pair_pair_double_list_double_map_number_set_number() { return null; }
}