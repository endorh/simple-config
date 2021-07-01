package dnj.simple_config.core;

import dnj.simple_config.core.SimpleConfig.ConfigReflectiveOperationException;
import dnj.simple_config.core.SimpleConfigBuilder.CategoryBuilder;
import dnj.simple_config.core.SimpleConfigBuilder.GroupBuilder;
import dnj.simple_config.core.annotation.*;
import dnj.simple_config.core.entry.*;
import net.minecraft.util.text.ITextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.Nullable;
import java.awt.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static dnj.simple_config.core.ReflectionUtil.*;
import static java.util.Collections.synchronizedMap;

public class SimpleConfigClassParser {
	
	private static final Logger LOGGER = LogManager.getLogger();
	protected static final Map<Class<? extends Annotation>, Map<Class<?>, FieldEntryParser<?>>>
	  PARSERS = synchronizedMap(new HashMap<>());
	
	@FunctionalInterface
	public interface FieldEntryParser<T extends Annotation> {
		@Nullable
		AbstractConfigEntry<?, ?, ?, ?> tryParse(T annotation, Field field, Object value);
	}
	
	/**
	 * Technically, you may register new field parsers before you mod's config
	 * is registered, but this is discouraged. Using the builder will probably be easier.
	 */
	@Internal public static <T extends Annotation> void registerFieldParser(
	  Class<T> annotationClass, Class<?> fieldClass, FieldEntryParser<T> parser
	) {
		synchronized (PARSERS) {
			PARSERS.computeIfAbsent(annotationClass, a -> synchronizedMap(new HashMap<>())).put(fieldClass, parser);
		}
	}
	
	/**
	 * Adds a validator from a sibling method to a {@link ListEntry}
	 */
	@Internal public static @Nullable <T, E extends ListEntry<T, ?, ?, E>> E decorateListEntry(
	  E entry, Field field
	) {
		final Class<?> cl = field.getDeclaringClass();
		final Method validate = tryGetMethod(cl, field.getName(), "validate", getTypeParameter(field, 0));
		if (validate != null) {
			final String errorMsg = "Unexpected reflection error invoking config list element validator method %s";
			if (checkType(validate, Boolean.class)) {
				entry.setValidator((Predicate<T>) e -> invoke(
				  validate, null, errorMsg, e));
			} else if (checkType(validate, Optional.class, ITextComponent.class)) {
				entry.setValidator((Function<T, Optional<ITextComponent>>) e -> invoke(
				  validate, null, errorMsg, e));
			} else throw new SimpleConfigClassParseException(
			  "Unsupported return type in config list element validator method " + getMethodName(validate) +
			  "\n  Return type must be either \"boolean\" or \"Optional<ITextComponent>\"");
		}
		return entry;
	}
	
	protected static <T> void decorateEntry(
	  AbstractConfigEntry<T, ?, ?, ?> entry, Class<?> cl, Field field
	) {
		final Method m = tryGetMethod(cl, field.getName(), "error", field.getType());
		if (m != null) {
			if (!checkType(m, Optional.class, ITextComponent.class))
				throw new SimpleConfigClassParseException(
				  "Invalid return type in config field error supplier method " + getMethodName(m)
				  + ": " + getMethodTypeName(m) + "\n  Error suppliers must return Optional<ITextComponent>");
			final String errorMsg = "Reflection error invoking config element error supplier method %s";
			entry.error(v -> invoke(m, null, errorMsg, v));
		}
		addTooltip(entry, cl, field);
		if (field.isAnnotationPresent(RequireRestart.class))
			entry.restart(true);
	}
	
	protected static <T> void addTooltip(
	  AbstractConfigEntry<T, ?, ?, ?> entry, Class<?> cl, Field field
	) {
		boolean a = true;
		Method m = tryGetMethod(cl, field.getName(), "tooltip", field.getType());
		if (m == null) {
			m = tryGetMethod(cl, field.getName(), "tooltip");
			a = false;
		}
		if (m != null) {
			if (!checkType(m, Optional.class, ITextComponent[].class))
				throw new SimpleConfigClassParseException(
				  "Invalid return type in config field error supplier method " + getMethodName(m)
				  + ": " + getMethodTypeName(m) + "\n  Tooltip suppliers must return Optional<ITextComponent[]>");
			final String errorMsg = "Reflection error invoking config element tooltip supplier method %s";
			final Method mm = m;
			entry.tooltipOpt(
			  a ? v -> invoke(mm, null, errorMsg, v)
			    : v -> invoke(mm, null, errorMsg));
		}
	}
	
	
	/**
	 * Try to invoke method<br>
	 * Convenient for lambdas
	 * @param errorMsg Error message added to the exception on error<br>
	 *                 Will be formatted with the method name
	 * @throws SimpleConfigClassParseException on error
	 */
	public static <T> T invoke(Method method, Object self, String errorMsg, Object... args) {
		try {
			//noinspection unchecked
			return (T) method.invoke(self, args);
		} catch (InvocationTargetException | IllegalAccessException | ClassCastException e) {
			throw new ConfigReflectiveOperationException(
			  String.format(errorMsg, getMethodName(method)) +
			  "\n  Details: " + e.getMessage(), e);
		}
	}
	
	protected static void decorateBuilder(SimpleConfigBuilder root) {
		decorateAbstractBuilder(root, root.configClass, root);
		for (CategoryBuilder catBuilder : root.categories.values())
			if (catBuilder.configClass != null)
				decorateAbstractBuilder(root, catBuilder.configClass, catBuilder);
	}
	
	protected static void decorateAbstractBuilder(
	  SimpleConfigBuilder root, Class<?> configClass,
	  AbstractSimpleConfigEntryHolderBuilder<?> builder
	) {
		final String className = configClass.getName();
		parseFields:for (Field field : configClass.getDeclaredFields()) {
			final String name = field.getName();
			final String fieldTypeName = field.getGenericType().getTypeName();
			if (!Modifier.isStatic(field.getModifiers()))
				throw new SimpleConfigClassParseException(
				  "Config class members must be static. Found non-static field " + getFieldName(field));
			Object value;
			try {
				field.setAccessible(true);
				value = field.get(null);
			} catch (IllegalAccessException e) {
				throw new SimpleConfigClassParseException(
				  "Couldn't access config class field " + getFieldName(field) + "\n  Details: " + e.getMessage(), e);
			}
			if (field.isAnnotationPresent(Text.class)) {
				if (field.getType() == String.class) {
					builder.text(field.getName());
				} else if (value instanceof ITextComponent) {
					final ITextComponent tc = (ITextComponent) value;
					builder.text(tc);
				} else if (value instanceof Supplier && ITextComponent.class.isAssignableFrom(
				  ((Class<?>) ((ParameterizedType) field.getGenericType())
				    .getActualTypeArguments()[0]))) {
					//noinspection unchecked
					final Supplier<ITextComponent> supplier = (Supplier<ITextComponent>) value;
					builder.text(supplier);
				} else throw new SimpleConfigClassParseException(
				  "Unsupported text supplier in config field " + getFieldName(field) + " of type " + fieldTypeName
				  + "\n  Should be either String (contents ignored), ITextComponent or Supplier<ITextComponent>");
				addTooltip(builder.last, configClass, field);
				continue;
			}
			if (builder.hasEntry(name)) {
				final Class<?> entryClass = builder.getEntry(name).value.getClass();
				final Class<?> fieldClass = field.getType();
				if (entryClass != fieldClass && (!fieldClass.isPrimitive() || !entryClass.isInstance(value)))
					throw new SimpleConfigClassParseException(
					  "Config field " + getFieldName(field) + " of type " + fieldTypeName + " does not " +
					  "match its entry's type: " + entryClass.getTypeName());
				builder.getEntry(name).backingField = field;
				if (hasAnyConfigAnnotation(field))
					LOGGER.warn(
					  "Config field " + getFieldName(field) + " has config annotations but is already " +
					  "defined by the builder. Its annotations will be ignored.");
				continue;
			}
			synchronized (PARSERS) {
				for (Class<? extends Annotation> annotationClass : PARSERS.keySet()) {
					if (field.isAnnotationPresent(annotationClass)) {
						final Map<Class<?>, FieldEntryParser<?>> parsers = PARSERS.get(annotationClass);
						final Class<?> fieldClass = field.getType();
						for (Class<?> clazz : parsers.keySet()) {
							if (clazz.isInstance(value) || clazz.isAssignableFrom(fieldClass)) {
								Annotation annotation = field.getAnnotation(annotationClass);
								//noinspection unchecked
								final AbstractConfigEntry<?, ?, ?, ?> entry =
								  ((FieldEntryParser<Annotation>) parsers.get(clazz))
								    .tryParse(annotation, field, value);
								if (entry != null) {
									entry.backingField = field;
									decorateEntry(entry, configClass, field);
									builder.add(field.getName(), entry);
									continue parseFields;
								}
							}
						}
						throw new SimpleConfigClassParseException(
						  "Unsupported type for Config field " + getFieldName(field) + " with " +
						  "annotation " + annotationClass.getName() + ": " + fieldTypeName);
					}
				}
			}
			if (hasAnyConfigAnnotation(field))
				throw new SimpleConfigClassParseException(
				  "Unsupported config annotation/field type combination in field " + getFieldName(field) +
				  " of type " + fieldTypeName);
		}
		for (Class<?> clazz : configClass.getDeclaredClasses()) {
			final String name = clazz.getSimpleName();
			if (clazz.isAnnotationPresent(Category.class)
			    || root.categories.containsKey(name)
			) {
				if (builder != root)
					throw new SimpleConfigClassParseException(
					  "Config category " + className + "." + name + " found outside of upper config level");
				CategoryBuilder catBuilder;
				if (root.categories.containsKey(name)) {
					catBuilder = root.categories.get(name);
					if (catBuilder.configClass != null)
						throw new SimpleConfigClassParseException(
						  "Attempt to declare backing class for config category that already has a " +
						  "backing class: " + name + ", class: " + clazz.getName());
				} else {
					catBuilder = new CategoryBuilder(name);
					root.n(catBuilder);
				}
				decorateAbstractBuilder(root, clazz, catBuilder);
			} else if (clazz.isAnnotationPresent(Group.class)
			           || builder.groups.containsKey(name)) {
				GroupBuilder gBuilder;
				if (builder.groups.containsKey(name)) {
					gBuilder = builder.groups.get(name);
				} else {
					Group a = clazz.getAnnotation(Group.class);
					boolean expand = a != null && a.expand();
					gBuilder = new GroupBuilder(name, expand);
					builder.n(gBuilder);
				}
				decorateAbstractBuilder(root, clazz, gBuilder);
			}
		}
		if (builder instanceof SimpleConfigBuilder) {
			final SimpleConfigBuilder b = (SimpleConfigBuilder) builder;
			final Method baker = tryGetMethod(configClass, "bake", SimpleConfig.class);
			if (baker != null) {
				if (!Modifier.isStatic(baker.getModifiers()))
					throw new SimpleConfigClassParseException(
					  "Found non-static bake method in config class " + className);
				final String errorMsg = "Reflective error invoking config baker method %s";
				if (b.baker == null) {
					b.setBaker(c -> invoke(baker, null, errorMsg, c));
				} else {
					LOGGER.warn(
					  "Found bake method in config class " + className + ", but the config " +
					  "already has a configured baker\nOnly the configured builder will " +
					  "be used\nIf the configured builder is precisely this method, rename it " +
					  "to suppress this warning.");
				}
			}
		} else if (builder instanceof CategoryBuilder) {
			final CategoryBuilder b = (CategoryBuilder) builder;
			final Method baker = tryGetMethod(configClass, "bake", SimpleConfigCategory.class);
			if (baker != null) {
				if (!Modifier.isStatic(baker.getModifiers()))
					throw new SimpleConfigClassParseException(
					  "Found non-static bake method in config category class " + className);
				final String errorMsg = "Reflective error invoking config category baker method %s";
				if (b.baker == null) {
					b.setBaker(c -> invoke(baker, null, errorMsg, c));
				} else {
					LOGGER.warn(
					  "Found bake method in config category class " + className + ", but the config " +
					  "already has a configured baker.\nOnly the configured builder will " +
					  "be used.\nIf the configured builder is precisely this method, rename it " +
					  "to suppress this warning.");
				}
			}
		}
	}
	
	protected static boolean hasAnyConfigAnnotation(Field field) {
		synchronized (PARSERS) {
			for (Class<? extends Annotation> clazz : PARSERS.keySet())
				if (field.isAnnotationPresent(clazz))
					return false;
		}
		return field.isAnnotationPresent(Text.class)
		       || field.isAnnotationPresent(RequireRestart.class);
	}
	
	public static class SimpleConfigClassParseException extends RuntimeException {
		public SimpleConfigClassParseException(String message) {
			super(message);
		}
		public SimpleConfigClassParseException(String message, Exception cause) {
			super(message, cause);
		}
	}
}
