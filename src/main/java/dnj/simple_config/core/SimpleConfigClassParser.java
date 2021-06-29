package dnj.simple_config.core;

import com.google.gson.internal.Primitives;
import dnj.simple_config.core.Entry.*;
import dnj.simple_config.core.SimpleConfigBuilder.CategoryBuilder;
import dnj.simple_config.core.SimpleConfigBuilder.GroupBuilder;
import dnj.simple_config.core.annotation.*;
import net.minecraft.util.text.ITextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.awt.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

import static dnj.simple_config.core.ReflectionUtil.checkTypeParameters;
import static dnj.simple_config.core.ReflectionUtil.getFieldName;
import static java.util.Collections.synchronizedMap;

public class SimpleConfigClassParser {
	
	private static final Logger LOGGER = LogManager.getLogger();
	protected static final Map<Class<? extends Annotation>, Map<Class<?>, FieldEntryParser<?>>>
	  PARSERS = synchronizedMap(new HashMap<>());
	
	@FunctionalInterface
	public interface FieldEntryParser<T extends Annotation> {
		@Nullable Entry<?, ?> tryParse(T annotation, Field field, Object value);
	}
	
	public static <T extends Annotation> void registerFieldParser(
	  Class<T> annotationClass, Class<?> fieldClass, FieldEntryParser<T> parser
	) {
		synchronized (PARSERS) {
			PARSERS.computeIfAbsent(annotationClass, a -> synchronizedMap(new HashMap<>())).put(fieldClass, parser);
		}
	}
	
	static {
		registerFieldParser(ConfigEntry.class, Boolean.class, (a, field, value) ->
		  new BooleanEntry(value != null? (Boolean) value : false));
		registerFieldParser(ConfigEntry.class, String.class, (a, field, value) ->
		  new StringEntry(value != null? (String) value : ""));
		registerFieldParser(ConfigEntry.class, Enum.class, (a, field, value) -> {
			if (value == null)
				value = field.getType().getEnumConstants()[0];
			//noinspection rawtypes
			return new EnumEntry((Enum) value);
		});
		registerFieldParser(ConfigEntry.Long.class, Long.class, (a, field, value) -> {
			final LongEntry e = new LongEntry((Long) value, a.min(), a.max());
			e.asSlider = a.slider();
			return e;
		});
		registerFieldParser(ConfigEntry.Double.class, Double.class, (a, field, value) ->
		  new DoubleEntry((Double) value, a.min(), a.max()));
		registerFieldParser(ConfigEntry.Color.class, Color.class, (a, field, value) ->
		  a.alpha() ? new AlphaColorEntry((Color) value) : new ColorEntry((Color) value));
		
		// Lists
		registerFieldParser(ConfigEntry.List.class, List.class, (a, field, value) ->
		  !checkTypeParameters(field, String.class) ? null :
		  decorateListEntry(new StringListEntry((List<String>) value), field));
		
		registerFieldParser(ConfigEntry.List.class, List.class, (a, field, value) ->
		  !checkTypeParameters(field, Long.class) ? null :
		  decorateListEntry(new LongListEntry((List<Long>) value), field));
		registerFieldParser(ConfigEntry.List.Long.class, List.class, (a, field, value) ->
		  !checkTypeParameters(field, Long.class) ? null :
		  decorateListEntry(new LongListEntry((List<Long>) value, a.min(), a.max()), field));
		
		registerFieldParser(ConfigEntry.List.class, List.class, (a, field, value) ->
		  !checkTypeParameters(field, Double.class) ? null :
		  decorateListEntry(new DoubleListEntry((List<Double>) value), field));
		registerFieldParser(ConfigEntry.List.Double.class, List.class, (a, field, value) ->
		  !checkTypeParameters(field, Double.class) ? null :
		  decorateListEntry(new DoubleListEntry((List<Double>) value, a.min(), a.max()), field));
	}
	
	protected static @Nullable <T, E extends ListEntry<T, E>> E decorateListEntry(
	  E entry, Field field
	) {
		final Class<?> cl = field.getDeclaringClass();
		final Method validate = ReflectionUtil
		  .tryGetMethod(cl, field.getName(), "validate",
		                ReflectionUtil.getTypeParameter(field, 0));
		if (validate != null) {
			if (!Boolean.class.isAssignableFrom(Primitives.wrap(validate.getReturnType())))
				throw new ConfigClassParseException(
				  "Non-boolean return type in config list element validator method " + cl.getName() + "#" + validate.getName());
			final String error = "Reflection error invoking config list element validator method %s";
			entry.setValidator(e -> ReflectionUtil.invoke(validate, null, error, e));
		}
		return entry;
	}
	
	protected static @Nullable <T> Entry<T, ?> decorateEntry(
	  Entry<T, ?> entry, Class<?> cl, Field field
	) {
		final Method error = ReflectionUtil.tryGetMethod(cl, field.getName(), "error", field.getType());
		if (error != null) {
			final Class<?> ret = error.getReturnType();
			if (!Optional.class.isAssignableFrom(ret)
			    || !ITextComponent.class.isAssignableFrom(ReflectionUtil.getTypeParameter(error, 0)))
				throw new ConfigClassParseException(
				  "Invalid return type in config field error supplier method " + ReflectionUtil
					 .getMethodName(error)
				  + ": " + ret.getTypeName() + "\n  Error suppliers must return Optional<ITextComponent>");
			final String errorMsg = "Reflection error invoking config element error supplier method %s";
			entry.errorSupplier = v -> ReflectionUtil.invoke(error, null, errorMsg, v);
		}
		if (field.isAnnotationPresent(RequireRestart.class))
			entry.restart(true);
		return entry;
	}
	
	protected static void decorateBuilder(SimpleConfigBuilder root) {
		decorateAbstractBuilder(root, root.configClass, root);
		for (CategoryBuilder catBuilder : root.categories.values())
			if (catBuilder.configClass != null)
				decorateAbstractBuilder(root, catBuilder.configClass, catBuilder);
	}
	
	protected static void decorateAbstractBuilder(
	  SimpleConfigBuilder root, Class<?> configClass,
	  AbstractSimpleConfigComponentBuilder<?> builder
	) {
		final String className = configClass.getName();
		parseFields:for (Field field : configClass.getDeclaredFields()) {
			final String name = field.getName();
			final String fieldTypeName = field.getGenericType().getTypeName();
			if (!Modifier.isStatic(field.getModifiers()))
				throw new ConfigClassParseException(
				  "Config class members must be static. Found non-static field " + getFieldName(field));
			Object value;
			try {
				field.setAccessible(true);
				value = field.get(null);
			} catch (IllegalAccessException e) {
				throw new ConfigClassParseException(
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
				} else throw new ConfigClassParseException(
				  "Unsupported text supplier in config field " + getFieldName(field) + " of type " + fieldTypeName
				  + "\n  Should be either ITextComponent or Supplier<ITextComponent>");
				continue;
			}
			if (builder.hasEntry(name)) {
				final Class<?> entryClass = builder.getEntry(name).value.getClass();
				final Class<?> fieldClass = field.getType();
				if (entryClass != fieldClass && (!fieldClass.isPrimitive() || !entryClass.isInstance(value)))
					throw new ConfigClassParseException(
					  "Config field " + getFieldName(field) + " of type " + fieldClass.getTypeName() + " does not " +
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
								final Entry<?, ?> entry =
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
						throw new ConfigClassParseException(
						  "Unsupported type for Config field " + getFieldName(field) + " with " +
						  "annotation " + annotationClass.getName() + ": " + fieldTypeName);
					}
				}
			}
			if (hasAnyConfigAnnotation(field))
				throw new ConfigClassParseException(
				  "Unsupported config field type/annotations: in field " + getFieldName(field) + "(" + fieldTypeName + ")");
		}
		for (Class<?> clazz : configClass.getDeclaredClasses()) {
			final String name = clazz.getSimpleName();
			if (clazz.isAnnotationPresent(ConfigCateg.class)
			    || root.categories.containsKey(name)
			) {
				if (builder != root)
					throw new ConfigClassParseException(
					  "Config category " + className + "." + name + " found outside of upper config level");
				CategoryBuilder catBuilder;
				if (root.categories.containsKey(name)) {
					catBuilder = root.categories.get(name);
					if (catBuilder.configClass != null)
						throw new ConfigClassParseException(
						  "Attempt to declare backing class for config category that already has a " +
						  "backing class: " + name);
				} else {
					catBuilder = new CategoryBuilder(name);
					root.n(catBuilder);
				}
				decorateAbstractBuilder(root, clazz, catBuilder);
			} else if (clazz.isAnnotationPresent(ConfigGroup.class)
			           || builder.groups.containsKey(name)) {
				GroupBuilder gBuilder;
				if (builder.groups.containsKey(name)) {
					gBuilder = builder.groups.get(name);
				} else {
					ConfigGroup a = clazz.getAnnotation(ConfigGroup.class);
					boolean expand = a != null && a.expand();
					gBuilder = new GroupBuilder(name, expand);
					builder.n(gBuilder);
				}
				decorateAbstractBuilder(root, clazz, gBuilder);
			}
		}
	}
	
	protected static boolean hasAnyConfigAnnotation(Field field) {
		for (Class<? extends Annotation> clazz : PARSERS.keySet())
			if (field.isAnnotationPresent(clazz))
				return false;
		return field.isAnnotationPresent(Text.class) || field.isAnnotationPresent(RequireRestart.class);
	}
	
	public static class ConfigClassParseException extends RuntimeException {
		public ConfigClassParseException(String message) {
			super(message);
		}
		public ConfigClassParseException(String message, Exception cause) {
			super(message, cause);
		}
	}
}
