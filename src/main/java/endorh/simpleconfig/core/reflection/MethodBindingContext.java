package endorh.simpleconfig.core.reflection;

import com.google.gson.internal.Primitives;
import endorh.simpleconfig.core.EntryType;
import endorh.simpleconfig.core.ReflectionUtil;
import endorh.simpleconfig.core.SimpleConfigClassParser.SimpleConfigClassParseException;
import endorh.simpleconfig.core.reflection.MethodBindingContext.MethodWrapper.AdapterMethodWrapper;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static endorh.simpleconfig.core.ReflectionUtil.getMethodName;
import static endorh.simpleconfig.core.reflection.FieldParser.invoke;
import static java.lang.Math.min;

public class MethodBindingContext {
	private static final Logger LOGGER = LogManager.getLogger();
	public final Class<?> cls;
	public final @Nullable MethodBindingContext parent;
	private @Nullable String contextName = null;
	private @Nullable Set<Method> methodSet;
	
	public static MethodBindingContext forConfigClass(
	  Class<?> cls, @Nullable MethodBindingContext parent, Set<Method> methodSet
	) {
		MethodBindingContext ctx = new MethodBindingContext(cls, parent);
		ctx.methodSet = methodSet;
		return ctx;
	}
	
	public MethodBindingContext(
	  Class<?> cls, @Nullable MethodBindingContext parent
	) {
		this.cls = cls;
		this.parent = parent;
	}
	
	public static ParametersAdapter[] oneOptionalAdapter(EntryType<?> type) {
		return new ParametersAdapter[]{
		  ParametersAdapter.of(p -> p, type),
		  ParametersAdapter.of(p -> new Object[]{})
		};
	}
	
	public static ParametersAdapter[] lastOptionalAdapter(
	  EntryType<?> type, EntryType<?>... fixed
	) {
		return new ParametersAdapter[]{
		  ParametersAdapter.of(p -> p, ArrayUtils.add(fixed, type)),
		  ParametersAdapter.of(p -> ArrayUtils.subarray(p, 0, p.length - 1), fixed)
		};
	}
	
	private void warnMissTypedMethod(String name) {
		MemberName nm = normalizeName(name);
		Arrays.stream(nm.cls.getDeclaredMethods())
		  .filter(m -> nm.name().equals(m.getName()))
		  .findFirst().ifPresent(m -> LOGGER.warn(
		    "Found method \"" + getMethodName(m) + "\" with invalid type: ",
		    m.getDeclaringClass().getName(), m.getName()));
	}
	
	public Class<?> getContextClass() {
		return cls;
	}
	
	public @Nullable String getContextName() {
		return contextName;
	}
	
	public void setContextName(@Nullable String contextName) {
		this.contextName = contextName;
	}
	
	public void setContextName(Field field) {
		setContextName(field.getName());
	}
	
	public @Nullable Method findCompatibleMethod(
	  String name, boolean widen, Class<?> returnType, Class<?>... paramTypes
	) {
		MemberName nm = normalizeName(name);
		boolean found = false;
		findMethod:for (Method m: nm.cls().getDeclaredMethods()) {
			if (!m.getName().equals(nm.name())) continue;
			Class<?>[] types = m.getParameterTypes();
			if (types.length != paramTypes.length) continue;
			for (int i = 0; i < paramTypes.length; i++)
				if (!types[i].isAssignableFrom(paramTypes[i])) continue findMethod;
			Class<?> rt = m.getReturnType();
			if (returnType.isAssignableFrom(rt)) {
				m.setAccessible(true);
				add(m);
				return m;
			}
			if (widen && rt.isAssignableFrom(returnType)) {
				m.setAccessible(true);
				add(m);
				return m;
			}
			found = true;
		}
		Method m = null;
		if (parent != null) m = parent.findCompatibleMethod(name, widen, returnType, paramTypes);
		if (m == null && found) warnMissTypedMethod(name);
		return m;
	}
	
	public @NotNull Method requireCompatibleMethod(
	  String name, boolean widen, Class<?> returnType, Class<?>... paramTypes
	) {
		Method m = findCompatibleMethod(name, widen, returnType, paramTypes);
		if (m == null) throw new MethodBindingException(
		  normalizeName(name), widen, returnType, paramTypes);
		return m;
	}
	
	@SafeVarargs
	public final @Nullable <R> AdapterMethodWrapper<R> findCompatibleMethod(
	  String name, boolean widen, ParametersAdapter[] parameters, ReturnTypeAdapter<?, R>... adapters
	) {
		MemberName nm = normalizeName(name);
		AdapterMethodWrapper<R> method = null;
		boolean found = false;
		for (ParametersAdapter p: parameters) {
			Class<?>[] paramTypes = p.getParameterTypes();
			findMethod:for (Method m: nm.cls().getDeclaredMethods()) {
				if (!m.getName().equals(nm.name())) continue;
				Class<?>[] types = m.getParameterTypes();
				if (types.length != paramTypes.length) continue;
				for (int i = 0; i < paramTypes.length; i++)
					if (!types[i].isAssignableFrom(paramTypes[i])) continue findMethod;
				Class<?> rt = m.getReturnType();
				for (ReturnTypeAdapter<?, R> a: adapters) {
					Class<?> returnType = a.getReturnType().type();
					if (returnType.isAssignableFrom(rt) || widen && rt.isAssignableFrom(returnType)) {
						m.setAccessible(true);
						checkStatic(m);
						AdapterMethodWrapper<R> fm = new AdapterMethodWrapper<>(m, p, a);
						if (method == null || fm.isMoreSpecificThan(method, true)) method = fm;
						continue findMethod;
					}
				}
				found = true;
			}
		}
		if (method != null) {
			add(method.method);
			return method;
		}
		if (parent != null) method = parent.findCompatibleMethod(name, widen, parameters, adapters);
		if (method == null && found) warnMissTypedMethod(name);
		return method;
	}
	
	@SafeVarargs public final @NotNull <R> MethodWrapper<R> requireCompatibleMethod(
	  String name, boolean widen, ParametersAdapter[] parameters, ReturnTypeAdapter<?, R>... adapters
	) {
		MethodWrapper<R> m = findCompatibleMethod(name, widen, parameters, adapters);
		if (m == null) throw new MethodBindingException(
		  normalizeName(name), true, parameters, adapters);
		return m;
	}
	
	@SafeVarargs public final <R> @Nullable MethodWrapper<R> findOwnMethod(
	  String name, ParametersAdapter[] parameters, ReturnTypeAdapter<?, R>... adapters
	) {
		MemberName nm = normalizeName(name);
		Pair<ParametersAdapter, Method> p = tryGetMethod(nm.cls(), nm.name(), parameters);
		if (p != null) {
			Method m = p.getRight();
			ParametersAdapter arg = p.getLeft();
			for (ReturnTypeAdapter<?, R> r : adapters) {
				if (r.getReturnType().equals(EntryType.fromType(m.getGenericReturnType()))) {
					add(m);
					Class<?> cls = r.getReturnType().type();
					return args -> r.castAdapt(invoke(m, null, cls, arg.adapt(args)));
				}
			}
			warnMissTypedMethod(name);
		}
		return null;
	}
	
	@SafeVarargs public final <R> @Nullable MethodWrapper<R> findMethod(
	  String name, ParametersAdapter[] parameters, ReturnTypeAdapter<?, R>... adapters
	) {
		MethodWrapper<R> own = findOwnMethod(name, parameters, adapters);
		if (own != null) return own;
		if (parent != null) return parent.findMethod(name, parameters, adapters);
		return null;
	}
	
	@SafeVarargs public final <R> @NotNull MethodWrapper<R> requireMethod(
	  String name, ParametersAdapter[] parameters, ReturnTypeAdapter<?, R>... adapters
	) {
		MethodWrapper<R> method = findMethod(name, parameters, adapters);
		if (method == null) throw new MethodBindingException(
		  normalizeName(name), parameters, adapters);
		return method;
	}
	
	public @Nullable Method findOwnMethod(String name, EntryType<?> type, EntryType<?>... args) {
		return findOwnMethod(name, MethodType.of(type, args));
	}
	
	public @Nullable Method findOwnMethod(String name, MethodType<?> type) {
		MemberName nm = normalizeName(name);
		Method method = tryGetMethod(nm.cls(), nm.name(), type.getParameterClasses());
		if (method != null && type.matches(method))
			return add(method);
		return null;
	}
	
	public @Nullable Method findMethod(String name, EntryType<?> type, EntryType<?>... args) {
		return findMethod(name, MethodType.of(type, args));
	}
	
	public @Nullable Method findMethod(String name, MethodType<?> type) {
		Method method = getMethod(name, type);
		if (method != null) return add(method);
		method = getMethodNoCheck(name, type);
		if (method != null) LOGGER.warn(
		  "Found matching method with invalid signature: "
		  + method.getDeclaringClass().getCanonicalName() + "#" + method.getName()
		  + ", expected signature: " + type);
		return null;
	}
	
	public @NotNull Method requireMethod(String name, EntryType<?> type, EntryType<?>... args) {
		return requireMethod(name, MethodType.of(type, args));
	}
	
	public @NotNull Method requireMethod(String name, MethodType<?> type) {
		Method method = getMethod(name, type);
		if (method != null) return add(method);
		method = getMethodNoCheck(name, type);
		if (method != null) throw new MethodBindingException(method, type);
		throw new MethodBindingException(normalizeName(name), type);
	}
	
	private @Nullable Method getMethod(String name, MethodType<?> type) {
		MemberName nm = normalizeName(name);
		Method method = tryGetMethod(nm.cls(), nm.name(), type.getParameterClasses());
		if (method != null && type.matches(method)) return add(method);
		if (parent != null) return parent.getMethod(name, type);
		return null;
	}
	
	private @Nullable Method getMethodNoCheck(
	  String name, MethodType<?> type
	) {
		MemberName nm = normalizeName(name);
		Method method = tryGetMethod(nm.cls(), nm.name(), type.getParameterClasses());
		if (method != null) return method;
		if (parent != null) return parent.getMethodNoCheck(name, type);
		return null;
	}
	
	private @Nullable Pair<ParametersAdapter, Method> tryGetMethod(
	  Class<?> clazz, String name, ParametersAdapter... adapters
	) {
		for (ParametersAdapter a: adapters) {
			Class<?>[] classes = a.getParameterTypes();
			try {
				Method m = checkStatic(clazz.getDeclaredMethod(name, classes));
				m.setAccessible(true);
				return Pair.of(a, m);
			} catch (NoSuchMethodException ignored) {
				// Try finding method with primitive types
				try {
					final Class<?>[] parameterTypesAsPrimitives = Arrays.stream(classes)
					  .map(Primitives::unwrap)
					  .toArray(Class<?>[]::new);
					if (Arrays.equals(parameterTypesAsPrimitives, classes))
						continue;
					final Method m = checkStatic(clazz.getDeclaredMethod(name, parameterTypesAsPrimitives));
					m.setAccessible(true);
					return Pair.of(a, m);
				} catch (NoSuchMethodException ignored1) {}
			}
		}
		return null;
	}
	
	private @Nullable Method tryGetMethod(
	  Class<?> clazz, String name, Class<?>... paramTypes
	) {
		return checkStatic(ReflectionUtil.tryGetMethod(clazz, name, paramTypes));
	}
	
	private Method checkStatic(Method method) {
		if (method == null) return null;
		if (!Modifier.isStatic(method.getModifiers())) throw new MethodBindingException(method);
		return method;
	}
	
	private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z_$][a-zA-Z\\d_$]*$");
	protected String checkName(String name) {
		if (!NAME_PATTERN.matcher(name).matches())
			throw new NameBindingException("Invalid Java identifier: " + name);
		return name;
	}
	
	protected MemberName normalizeName(String name) {
		if (name.startsWith("$")) {
			if (contextName != null) return new MemberName(cls, checkName(contextName + name));
			throw new NameBindingException(
			  "Cannot use local name without a naming context: " + name);
		}
		if (name.contains("#")) {
			String[] split = name.split("#", 2);
			try {
				Class<?> cls = Class.forName(checkName(split[0]));
				return new MemberName(cls, checkName(split[1]));
			} catch (ClassNotFoundException e) {
				throw new NameBindingException("Class not found: " + split[0]);
			}
		}
		return new MemberName(cls, checkName(name));
	}
	
	private Method add(Method method) {
		if (methodSet != null) methodSet.add(method);
		return method;
	}
	
	public interface ParametersAdapter {
		static ParametersAdapter of(Function<Object[], Object[]> adapter, EntryType<?>... types) {
			Class<?>[] classes = Arrays.stream(types).map(EntryType::type).toArray(Class<?>[]::new);
			return new ParametersAdapter() {
				@Override public EntryType<?>[] getGenericParameterTypes() {
					return types;
				}
				@Override public Class<?>[] getParameterTypes() {
					return classes;
				}
				@Override public Object[] adapt(Object[] args) {
					return adapter.apply(args);
				}
			};
		}
		
		static ParametersAdapter empty() {
			return of(args -> args);
		}
		
		static ParametersAdapter[] emptySignature() {
			return new ParametersAdapter[] { empty() };
		}
		
		static ParametersAdapter[] singleSignature(EntryType<?>... type) {
			return new ParametersAdapter[] { of(args -> args, type) };
		}
		
		EntryType<?>[] getGenericParameterTypes();
		Class<?>[] getParameterTypes();
		Object[] adapt(Object[] args);
	}
	
	public interface ReturnTypeAdapter<T, R> {
		static <T, R> ReturnTypeAdapter<T, R> of(EntryType<T> type, Function<T, R> adapter) {
			return new ReturnTypeAdapter<T, R>() {
				@Override public EntryType<T> getReturnType() {
					return type;
				}
				@Override public R adapt(T value) {
					return adapter.apply(value);
				}
			};
		}
		static <T> ReturnTypeAdapter<T, T> identity(EntryType<T> type) {
			return of(type, Function.identity());
		}
		static ReturnTypeAdapter<Void, Void> ofVoid() {
			return identity(EntryType.of(void.class));
		}
		
		EntryType<T> getReturnType();
		
		R adapt(T result);
		@SuppressWarnings("unchecked") default R castAdapt(Object result) {
			return adapt((T) result);
		}
	}
	
	public interface MethodWrapper<R> {
		R invoke(Object... args);
		
		public static class AdapterMethodWrapper<R> implements MethodWrapper<R> {
			public final Method method;
			public final ParametersAdapter paramsAdapter;
			public final ReturnTypeAdapter<?, R> returnTypeAdapter;
			public final @NotNull Class<?> cls;
			
			public AdapterMethodWrapper(
			  Method method, ParametersAdapter paramsAdapter, ReturnTypeAdapter<?, R> returnTypeAdapter
			) {
				this.paramsAdapter = paramsAdapter;
				this.returnTypeAdapter = returnTypeAdapter;
				this.method = method;
				cls = returnTypeAdapter.getReturnType().type();
			}
			
			@Override public R invoke(Object... args) {
				return returnTypeAdapter.castAdapt(
				  FieldParser.invoke(method, null, cls, paramsAdapter.adapt(args)));
			}
			
			public String getMethodName() {
				return ReflectionUtil.getMethodName(method);
			}
			
			public boolean isMoreSpecificThan(
			  AdapterMethodWrapper<R> other, boolean ignoreExtraParameters
			) {
				Class<?>[] tTypes = paramsAdapter.getParameterTypes();
				Class<?>[] oTypes = other.paramsAdapter.getParameterTypes();
				if (tTypes.length != oTypes.length && !ignoreExtraParameters) return false;
				int l = min(tTypes.length, oTypes.length);
				for (int i = 0; i < l; i++)
					if (!oTypes[i].isAssignableFrom(tTypes[i]))
						return false;
				return true;
			}
		}
	}
	
	public static class MethodType<R> {
		private final EntryType<R> returnType;
		private final EntryType<?>[] args;
		
		private MethodType(EntryType<R> returnType, EntryType<?>[] args) {
			this.returnType = returnType;
			this.args = args;
		}
		
		public static <R> MethodType<R> of (EntryType<R> returnType, EntryType<?>... args) {
			return new MethodType<>(returnType, args);
		}
		
		public static MethodType<?> fromMethod(Method method) {
			return new MethodType<>(
			  EntryType.fromMethod(method),
			  Arrays.stream(method.getGenericParameterTypes())
			    .map(EntryType::fromType)
			    .toArray(EntryType<?>[]::new));
		}
		
		public boolean matches(Method method) {
			if (!returnType.equals(EntryType.fromType(method.getGenericReturnType())))
				return false;
			if (args.length != method.getParameterCount())
				return false;
			Type[] a = method.getGenericParameterTypes();
			for (int i = 0; i < args.length; i++)
				if (!args[i].equals(EntryType.fromType(a[i])))
					return false;
			return true;
		}
		
		@Override public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			MethodType<?> that = (MethodType<?>) o;
			return returnType.equals(that.returnType) && Arrays.equals(args, that.args);
		}
		
		@Override public int hashCode() {
			int result = Objects.hash(returnType);
			result = 31 * result + Arrays.hashCode(args);
			return result;
		}
		
		public Class<?>[] getParameterClasses() {
			return Arrays.stream(args).map(EntryType::type).toArray(Class<?>[]::new);
		}
		
		@Override public String toString() {
			return returnType + "(" + StringUtils.join(args, ", ") + ")";
		}
	}
	
	public static class MemberName {
		private final Class<?> cls;
		private final String name;
		
		public MemberName(Class<?> cls, String name) {
			this.cls = cls;
			this.name = name;
		}
		
		public Class<?> cls() {
			return cls;
		}
		
		public String name() {
			return name;
		}
		
		@Override public String toString() {
			return cls.getCanonicalName() + "#" + name;
		}
		
		@Override public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			MemberName that = (MemberName) o;
			return cls.equals(that.cls) && name.equals(that.name);
		}
		
		@Override public int hashCode() {
			return Objects.hash(cls, name);
		}
	}
	
	private class NameBindingException extends SimpleConfigClassParseException {
		private NameBindingException(String message) {
			super(cls, message);
		}
	}
	
	private class MethodBindingException extends SimpleConfigClassParseException {
		private MethodBindingException(Method method) {
			super(
			  cls,
			  "Found matching non-static config entry method: " + getMethodName(method) +
			  "\n  Make this method static, or use a different name if this match is not intended");
		}
		
		private MethodBindingException(Method method, MethodType<?> type) {
			super(
			  cls,
			  "Config entry method " + getMethodName(method) + " has the wrong type: " +
			  MethodType.fromMethod(method) + ", expected: " + type);
		}
		
		private MethodBindingException(MemberName name, MethodType<?> type) {
			super(
			  cls, "Config entry method not found: " + name +
			       "\n  Expected method type: " + type);
		}
		
		private MethodBindingException(
		  MemberName name, ParametersAdapter[] argAdapters, ReturnTypeAdapter<?, ?>... adapters
		) {
			this(name, false, argAdapters, adapters);
		}
		
		private MethodBindingException(
		  MemberName name, boolean widen, ParametersAdapter[] argAdapters, ReturnTypeAdapter<?, ?>... adapters
		) {
			super(
			  cls,
			  "Config entry method not found: " + name +
			  "\n  Expected method should accept any of the following signatures" +
			  (widen? " (or broader):" : ":") +
			  Arrays.stream(argAdapters)
			    .map(a -> "\n    (" + StringUtils.join(a.getGenericParameterTypes(), ", ") + ")")
			    .collect(Collectors.joining()) +
			  "\n  and any of the following return types:" +
			  Arrays.stream(adapters)
			    .map(a -> "\n    " + a.getReturnType())
			    .collect(Collectors.joining()));
		}
		
		private MethodBindingException(
		  MemberName name, boolean widen, Class<?> returnType, Class<?>[] paramTypes
		) {
			super(
			  cls,
			  "Config entry method not found: " + name +
			  "\n  Expected method should have the following signature or broader:" +
			  "\n    (" + Arrays.stream(paramTypes)
			    .map(Class::getCanonicalName)
			    .collect(Collectors.joining(", ")) + ")" +
			  "\n  and return type assignable to " +
			  (widen? "(or implementing): " : ": ") + returnType.getCanonicalName());
		}
	}
}
