package endorh.simpleconfig.core;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.SimpleConfig;
import endorh.simpleconfig.api.SimpleConfig.ConfigReflectiveOperationException;
import endorh.simpleconfig.api.SimpleConfigCategory;
import endorh.simpleconfig.api.SimpleConfigGroup;
import endorh.simpleconfig.api.annotation.*;
import endorh.simpleconfig.api.entry.ListEntryBuilder;
import endorh.simpleconfig.core.BackingField.BackingFieldBinding;
import endorh.simpleconfig.core.BackingField.BackingFieldBuilder;
import endorh.simpleconfig.core.SimpleConfigBuilderImpl.CategoryBuilder;
import endorh.simpleconfig.core.SimpleConfigBuilderImpl.GroupBuilder;
import endorh.simpleconfig.core.entry.AbstractListEntry;
import endorh.simpleconfig.core.entry.TextEntry;
import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static endorh.simpleconfig.api.ConfigBuilderFactoryProxy.*;
import static endorh.simpleconfig.core.ReflectionUtil.*;
import static java.util.Collections.synchronizedMap;

public class SimpleConfigClassParser {
	
	private static final Logger LOGGER = LogManager.getLogger();
	protected static final Map<Class<? extends Annotation>, Map<Class<?>, List<FieldEntryParser<?, ?>>>>
	  PARSERS = synchronizedMap(new HashMap<>());
	
	// Used to construct exception messages
	protected static final Map<Class<?>, AbstractSimpleConfigEntryHolderBuilder<?>> builders = new ConcurrentHashMap<>();
	
	static {
		registerFieldParsers();
	}
	
	protected static void registerFieldParsers() {
		registerFieldParser(Entry.class, Boolean.class, (a, field, value) ->
		  bool(value != null ? value : false));
		registerFieldParser(Entry.class, String.class, (a, field, value) ->
		  string(value != null ? value : ""));
		registerFieldParser(Entry.class, Enum.class, (a, field, value) -> {
			if (value == null)
				//noinspection rawtypes
				value = (Enum) field.getType().getEnumConstants()[0];
			return option(value);
		});
		registerFieldParser(Entry.class, Byte.class, (a, field, value) ->
		  number(value, getMin(field).byteValue(), getMax(field).byteValue())
		    .slider(field.isAnnotationPresent(Slider.class)));
		registerFieldParser(Entry.class, Short.class, (a, field, value) ->
		  number(value, getMin(field).shortValue(), getMax(field).shortValue())
		    .slider(field.isAnnotationPresent(Slider.class)));
		registerFieldParser(Entry.class, Integer.class, (a, field, value) ->
		  number(value, getMin(field).intValue(), getMax(field).intValue())
		    .slider(field.isAnnotationPresent(Slider.class)));
		registerFieldParser(Entry.class, Long.class, (a, field, value) ->
		  number(value, getMin(field).longValue(), getMax(field).longValue())
		    .slider(field.isAnnotationPresent(Slider.class)));
		registerFieldParser(Entry.class, Float.class, (a, field, value) ->
		  number(value, getMin(field).floatValue(), getMax(field).floatValue())
		    .slider(field.isAnnotationPresent(Slider.class)));
		registerFieldParser(Entry.class, Double.class, (a, field, value) ->
		  number(value, getMin(field).doubleValue(), getMax(field).doubleValue())
		    .slider(field.isAnnotationPresent(Slider.class)));
		registerFieldParser(Entry.class, Color.class, (a, field, value) ->
		  color(value).alpha(field.isAnnotationPresent(HasAlpha.class)));
		
		// Lists
		//noinspection unchecked
		registerFieldParser(Entry.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, String.class) ? null :
		  decorateListEntry(stringList((List<String>) value), field));
		//noinspection unchecked
		registerFieldParser(Entry.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, Integer.class) ? null :
		  decorateListEntry(
		    intList((List<Integer>) value)
		      .min(getMin(field).intValue()).max(getMax(field).intValue()), field));
		//noinspection unchecked
		registerFieldParser(Entry.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, Long.class) ? null :
		  decorateListEntry(
		    longList((List<Long>) value)
		      .min(getMin(field).longValue()).max(getMax(field).longValue()), field));
		//noinspection unchecked
		registerFieldParser(Entry.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, Float.class) ? null :
		  decorateListEntry(
		    floatList((List<Float>) value)
		      .min(getMin(field).floatValue()).max(getMax(field).floatValue()), field));
		//noinspection unchecked
		registerFieldParser(Entry.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, Double.class) ? null :
		  decorateListEntry(
		    doubleList((List<Double>) value)
		      .min(getMin(field).doubleValue()).max(getMax(field).doubleValue()), field));
		
		//noinspection unchecked
		registerFieldParser(Entry.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, Short.class) ? null :
		  decorateListEntry(
		    shortList((List<Short>) value)
		      .min(getMin(field).shortValue()).max(getMax(field).shortValue()), field));
		//noinspection unchecked
		registerFieldParser(Entry.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, Byte.class) ? null :
		  decorateListEntry(
		    byteList((List<Byte>) value)
		      .min(getMin(field).byteValue()).max(getMax(field).byteValue()), field));
		
		// Minecraft entry types
		registerFieldParser(Entry.class, Item.class, (a, field, value) -> item(value));
		registerFieldParser(Entry.class, Block.class, (a, field, value) -> block(value));
		registerFieldParser(Entry.class, Fluid.class, (a, field, value) -> fluid(value));
		registerFieldParser(Entry.class, CompoundNBT.class, (a, field, value) -> nbtTag(value));
		registerFieldParser(Entry.class, INBT.class, (a, field, value) -> nbtValue(value));
		registerFieldParser(Entry.class, ResourceLocation.class, (a, field, value) -> resource(value));
	}
	
	@FunctionalInterface
	public interface FieldEntryParser<T extends Annotation, V> {
		@Nullable ConfigEntryBuilder<?, ?, ?, ?> tryParse(T annotation, Field field, V value);
	}
	
	/**
	 * Technically, you may register new field parsers before you mod's config
	 * is registered, but this is discouraged. Using the builder will probably be easier.
	 */
	@Internal public static <T extends Annotation, V> void registerFieldParser(
	  Class<T> annotationClass, Class<V> fieldClass, FieldEntryParser<T, V> parser
	) {
		PARSERS.computeIfAbsent(annotationClass, a -> synchronizedMap(new HashMap<>()))
		  .computeIfAbsent(fieldClass, c -> new ArrayList<>()).add(parser);
	}
	
	
	/**
	 * Adds a validator from a sibling method to a {@link AbstractListEntry}
	 */
	@Internal public static @Nullable <V, B extends ListEntryBuilder<V, ?, ?, B>> B decorateListEntry(
	  B builder, Field field
	) {
		final Class<?> cl = field.getDeclaringClass();
		final Method validate = tryGetMethod(cl, field.getName(), "elemError", getTypeParameter(field, 0));
		if (validate != null) {
			final String errorMsg = "Unexpected reflection error invoking config list element validator method %s";
			if (checkType(validate, Optional.class, ITextComponent.class)) {
				builder = builder.elemError(e -> invoke(
				  validate, null, errorMsg, Optional.class, e));
			} else throw new SimpleConfigClassParseException(cl,
			  "Unsupported return type in config list element validator method " + getMethodName(validate) +
			  "\n  Return type must be either \"boolean\" or \"Optional<ITextComponent>\"");
		}
		return builder;
	}
	
	protected static <V> ConfigEntryBuilder<V, ?, ?, ?> decorateEntry(
	  ConfigEntryBuilder<V, ?, ?, ?> builder, Class<?> cl, Field field
	) {
		final Method m = tryGetMethod(cl, field.getName(), "error", field.getType());
		if (m != null) {
			if (!checkType(m, Optional.class, ITextComponent.class))
				throw new SimpleConfigClassParseException(cl,
				  "Invalid return type in config field error supplier method " + getMethodName(m)
				  + ": " + getMethodTypeName(m) + "\n  Error suppliers must return Optional<ITextComponent>");
			final String errorMsg = "Reflection error invoking config element error supplier method %s";
			builder = builder.error(v -> invoke(m, null, errorMsg, Optional.class, v));
		}
		builder = addTooltip(builder, cl, field);
		if (field.isAnnotationPresent(RequireRestart.class))
			builder = builder.restart(true);
		if (field.isAnnotationPresent(NonPersistent.class))
			builder = builder.temp();
		return builder;
	}
	
	protected static <V> ConfigEntryBuilder<V, ?, ?, ?> addTooltip(
	  ConfigEntryBuilder<V, ?, ?, ?> builder, Class<?> cl, Field field
	) {
		boolean a = true;
		Method m = tryGetMethod(cl, field.getName(), "tooltip", field.getType());
		if (m == null) {
			m = tryGetMethod(cl, field.getName(), "tooltip");
			a = false;
		}
		if (m != null) {
			if (!checkType(m, List.class, ITextComponent.class))
				throw new SimpleConfigClassParseException(cl,
				  "Invalid return type in config field error supplier method " + getMethodName(m)
				  + ": " + getMethodTypeName(m) + "\n  Tooltip suppliers must return List<ITextComponent>");
			final String errorMsg = "Reflection error invoking config element tooltip supplier method %s";
			final Method mm = m;
			builder = builder.tooltip(
			  a ? v -> invoke(mm, null, errorMsg, List.class, v)
			    : v -> invoke(mm, null, errorMsg, List.class));
		}
		return builder;
	}
	
	/**
	 * Get the minimum annotated value in this field
	 */
	public static Number getMin(Field field) {
		return field.isAnnotationPresent(Min.class)
		       ? field.getAnnotation(Min.class).value()
		       : Double.NEGATIVE_INFINITY;
	}
	
	/**
	 * Get the maximum annotated value in this field
	 */
	public static Number getMax(Field field) {
		return field.isAnnotationPresent(Max.class)
		       ? field.getAnnotation(Max.class).value()
		       : Double.POSITIVE_INFINITY;
	}
	
	/**
	 * Try to invoke method<br>
	 * Convenient for lambdas
	 *
	 * @param errorMsg Error message added to the exception on error<br>
	 *   Will be formatted with the method name
	 * @throws SimpleConfigClassParseException on error
	 */
	public static <T> T invoke(
	  Method method, Object self, String errorMsg, Class<?> clazz, Object... args
	) {
		try {
			//noinspection unchecked
			return (T) clazz.cast(method.invoke(self, args));
		} catch (InvocationTargetException | IllegalAccessException | ClassCastException e) {
			throw new ConfigReflectiveOperationException(
			  String.format(errorMsg, getMethodName(method)) +
			  "\n  Details: " + e.getMessage(), e);
		}
	}
	
	protected static void decorateBuilder(SimpleConfigBuilderImpl root) {
		if (root.configClass != null)
			decorateAbstractBuilder(root, root.configClass, root);
		for (CategoryBuilder catBuilder : root.categories.values())
			if (catBuilder.configClass != null)
				decorateAbstractBuilder(root, catBuilder.configClass, catBuilder);
	}
	
	protected static void decorateAbstractBuilder(
	  SimpleConfigBuilderImpl root, Class<?> configClass,
	  AbstractSimpleConfigEntryHolderBuilder<?> builder
	) {
		builders.put(configClass, builder);
		Set<Field> secondaryBackingFields = new HashSet<>();
		for (String name : builder.entries.keySet()) {
			List<? extends BackingFieldBinding<?, ?>> bindings = builder.getSecondaryBackingFieldBindings(name);
			if (bindings.isEmpty()) continue;
			List<BackingField<?, ?>> backingFields = new ArrayList<>();
			for (BackingFieldBinding<?, ?> f : bindings) {
				String fieldName = f.buildName(name);
				try {
					Field field = configClass.getDeclaredField(fieldName);
					field.setAccessible(true);
					if (!Modifier.isStatic(field.getModifiers()))
						throw new SimpleConfigClassParseException(builder,
						  "Config class members must be static. Found non-static field " + getFieldName(field));
					BackingField<?, ?> backingField = f.build(field);
					backingFields.add(backingField);
					secondaryBackingFields.add(field);
					if (hasAnyConfigAnnotation(field)) LOGGER.warn(
					  "Config field " + getFieldName(field) + " has config annotations but is already " +
					  "defined by the builder. Its annotations will be ignored.");
				} catch (NoSuchFieldException e) {
					throw new SimpleConfigClassParseException(builder,
					  "Missing backing field \"" + configClass.getSimpleName() + "#" + fieldName +
					  "\" for entry \"" + name + "\"");
				}
			}
			builder.setSecondaryBackingFields(name, backingFields);
		}
		final String className = configClass.getName();
		parseFields:for (Field field : configClass.getDeclaredFields()) {
			final String name = field.getName();
			if (secondaryBackingFields.contains(field)) continue;
			final String fieldTypeName = field.getGenericType().getTypeName();
			if (!Modifier.isStatic(field.getModifiers()))
				throw new SimpleConfigClassParseException(builder,
				  "Config class members must be static. Found non-static field " + getFieldName(field));
			Object value;
			try {
				field.setAccessible(true);
				value = field.get(null);
			} catch (IllegalAccessException e) {
				throw new SimpleConfigClassParseException(builder,
				  "Couldn't access config class field " + getFieldName(field) + "\n  Details: " + e.getMessage(), e);
			}
			if (field.isAnnotationPresent(Text.class)) {
				TextEntry.Builder textBuilder;
				if (field.getType() == String.class) {
					textBuilder = new TextEntry.Builder();
				} else if (value instanceof ITextComponent) {
					final ITextComponent tc = (ITextComponent) value;
					textBuilder = new TextEntry.Builder(() -> tc);
				} else if (value instanceof Supplier && ITextComponent.class.isAssignableFrom(
				  (Class<?>) ((ParameterizedType) field.getGenericType())
				    .getActualTypeArguments()[0])) {
					//noinspection unchecked
					final Supplier<ITextComponent> supplier = (Supplier<ITextComponent>) value;
					textBuilder = new TextEntry.Builder(supplier);
				} else throw new SimpleConfigClassParseException(builder,
				  "Unsupported text supplier in config field " + getFieldName(field) + " of type " + fieldTypeName
				  + "\n  Should be either String (contents ignored), ITextComponent or Supplier<ITextComponent>");
				builder.add(getOrder(field), field.getName(), addTooltip(textBuilder, configClass, field));
				continue;
			}
			if (builder.hasEntry(name) && !field.isAnnotationPresent(NotEntry.class)) {
				BackingFieldBuilder<?, ?> fieldBuilder = builder.getEntry(name).backingFieldBuilder;
				if (fieldBuilder == null) {
					LOGGER.warn(
					  "Found config backing field " + getFieldName(field) + " for entry " +
					  "of type " + builder.getEntry(name).getClass().getSimpleName() + ", " +
					  "which does not support backing fields. The field will be ignored\n" +
					  "Rename the field or annotate it with @NotEntry to suppress this warning.");
					continue;
				}
				BackingField<?, ?> backingField = fieldBuilder.build(field);
				if (builder.backingFields.containsKey(name))
					throw new SimpleConfigClassParseException(
					  builder, "Config entry " + name + " cannot have two backing fields");
				builder.setBackingField(name, backingField);
				if (hasAnyConfigAnnotation(field))
					LOGGER.warn(
					  "Config field " + getFieldName(field) + " has config annotations but is already " +
					  "defined by the builder. Its annotations will be ignored.");
				continue;
			}
			if (field.isAnnotationPresent(Bind.class))
				throw new SimpleConfigClassParseException(builder,
				  "Config field " + getFieldName(field) + " was annotated with @Bind but no " +
				  "matching config entry was found defined");
			synchronized (PARSERS) {
				for (Class<? extends Annotation> annotationClass : PARSERS.keySet()) {
					if (field.isAnnotationPresent(annotationClass)) {
						final Map<Class<?>, List<FieldEntryParser<?, ?>>> parsers = PARSERS.get(annotationClass);
						final Class<?> fieldClass = field.getType();
						boolean match = false;
						for (Class<?> clazz : parsers.keySet()) {
							if (clazz.isInstance(value) || clazz.isAssignableFrom(fieldClass)) {
								Annotation annotation = field.getAnnotation(annotationClass);
								for (FieldEntryParser<?, ?> parser : parsers.get(clazz)) {
									//noinspection unchecked
									ConfigEntryBuilder<?, ?, ?, ?> entryBuilder =
									  ((FieldEntryParser<Annotation, Object>) parser).tryParse(annotation, field, value);
									if (entryBuilder != null) {
										if (builder.hasEntry(name) || builder.groups.containsKey(name))
											throw new SimpleConfigClassParseException(
											  builder, "Cannot create entry with name " + name + ". The name is already used.");
										entryBuilder = decorateEntry(entryBuilder, configClass, field);
										if (!(entryBuilder instanceof AbstractConfigEntryBuilder)) throw new IllegalStateException(
										  "Entry builder " + entryBuilder.getClass().getCanonicalName() + " is not an AbstractConfigEntryBuilder");
										BackingFieldBuilder<?, ?> fieldBuilder = ((AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?>) entryBuilder).backingFieldBuilder;
										if (fieldBuilder == null) throw new SimpleConfigClassParseException(builder,
										  "Config entry generated from field does not support backing fields");
										if (!fieldBuilder.matchesType(field)) {
											match = true;
											continue;
										}
										BackingField<?, ?> backingField = fieldBuilder.build(field);
										builder.add(getOrder(field), name, entryBuilder);
										builder.setBackingField(name, backingField);
										continue parseFields;
									}
								}
							}
						}
						if (match) {
							throw new SimpleConfigClassParseException(builder,
							  "Backing field for config entry generated from field \"" +
							  getFieldName(field) + "\" doesn't match its expected type.");
						} else throw new SimpleConfigClassParseException(builder,
						  "Unsupported type for Config field " + getFieldName(field) + " with " +
						  "annotation " + annotationClass.getName() + ": " + fieldTypeName);
					}
				}
			}
			if (hasAnyConfigAnnotation(field))
				throw new SimpleConfigClassParseException(builder,
				  "Unsupported config annotation/field type combination in field " + getFieldName(field) +
				  " of type " + fieldTypeName);
		}
		for (Class<?> clazz : configClass.getDeclaredClasses()) {
			final String name = clazz.getSimpleName();
			if (clazz.isAnnotationPresent(Category.class)
			    || root.categories.containsKey(name)
			) {
				if (builder != root)
					throw new SimpleConfigClassParseException(builder,
					  "Config category " + className + "." + name + " found outside of upper config level");
				CategoryBuilder catBuilder;
				if (root.categories.containsKey(name)) {
					catBuilder = root.categories.get(name);
					if (catBuilder.configClass != null)
						throw new SimpleConfigClassParseException(builder,
						  "Attempt to declare backing class for config category that already has a " +
						  "backing class: " + name + ", class: " + clazz.getName());
				} else {
					Category a = clazz.getAnnotation(Category.class);
					catBuilder = new CategoryBuilder(name);
					if (clazz.isAnnotationPresent(RequireRestart.class))
						catBuilder.restart();
					root.n(catBuilder, a.value());
				}
				decorateAbstractBuilder(root, clazz, catBuilder);
			} else if (clazz.isAnnotationPresent(Group.class)
			           || builder.groups.containsKey(name)) {
				GroupBuilder gBuilder;
				if (builder.groups.containsKey(name)) {
					gBuilder = builder.groups.get(name);
				} else {
					Group a = clazz.getAnnotation(Group.class);
					boolean expand = a.expand();
					gBuilder = new GroupBuilder(name, expand);
					if (clazz.isAnnotationPresent(RequireRestart.class))
						gBuilder.restart();
					builder.n(gBuilder, a.value());
				}
				decorateAbstractBuilder(root, clazz, gBuilder);
			} else if (clazz.isAnnotationPresent(Bind.class))
				throw new SimpleConfigClassParseException(builder,
				  "Config inner class " + clazz.getName() + " was annotated with @Bind " +
				  "but no matching config category/group was found defined");
		}
		if (builder instanceof SimpleConfigBuilderImpl) {
			final SimpleConfigBuilderImpl b = (SimpleConfigBuilderImpl) builder;
			final Method baker = tryGetMethod(configClass, "bake", SimpleConfig.class);
			if (baker != null) {
				final String errorMsg = "Reflective error invoking config baker method %s";
				if (b.baker == null) {
					if (!Modifier.isStatic(baker.getModifiers()))
						throw new SimpleConfigClassParseException(
						  builder, "Found non-static bake method in config class " + className);
					b.withBaker(c -> invoke(baker, null, errorMsg, Object.class, c));
				} else {
					LOGGER.warn(
					  "Found bake method in config class " + className + ", but the config " +
					  "already has a configured baker\nOnly the configured baker will " +
					  "be used\nIf the configured baker is precisely this method, rename it " +
					  "or let the reflection find it to suppress this warning.");
				}
			}
		} else if (builder instanceof CategoryBuilder) {
			final CategoryBuilder b = (CategoryBuilder) builder;
			boolean useArg = true;
			Method m = tryGetMethod(configClass, "bake", SimpleConfigCategory.class);
			if (m == null) {
				useArg = false;
				m = tryGetMethod(configClass, "bake");
			}
			final Method baker = m;
			if (baker != null) {
				if (!Modifier.isStatic(baker.getModifiers()))
					throw new SimpleConfigClassParseException(builder,
					  "Found non-static bake method in config category class " + className);
				final String errorMsg = "Reflective error invoking config category baker method %s";
				if (b.baker == null) {
					b.withBaker(
					  useArg? c -> invoke(baker, null, errorMsg, Object.class, c)
					        : c -> invoke(baker, null, errorMsg, Object.class));
				} else {
					LOGGER.warn(
					  "Found bake method in config category class " + className + ", but the category " +
					  "already has a configured baker.\nOnly the configured baker will " +
					  "be used.\nIf the configured baker is precisely this method, rename it " +
					  "to suppress this warning.");
				}
			} else if (
			  Arrays.stream(configClass.getDeclaredMethods()).anyMatch(mm -> mm.getName().equals("bake"))
			) {
				LOGGER.warn(
				  "Found method named \"bake\" in config category class " + className + " with an " +
				  "unsupported signature.\nRename the method to suppress this warning, or fix the bug");
			}
		} else if (builder instanceof GroupBuilder) {
			final GroupBuilder b = (GroupBuilder) builder;
			boolean useArg = true;
			Method m = tryGetMethod(configClass, "bake", SimpleConfigGroup.class);
			if (m == null) {
				useArg = false;
				m = tryGetMethod(configClass, "bake");
			}
			final Method baker = m;
			if (baker != null) {
				if (!Modifier.isStatic(baker.getModifiers()))
					throw new SimpleConfigClassParseException(builder,
					  "Found non-static bake method in config group class " + className);
				final String errorMsg = "Reflective error invoking config category baker method %s";
				if (b.baker == null) {
					b.withBaker(
					  useArg? c -> invoke(baker, null, errorMsg, Object.class, c)
					        : c -> invoke(baker, null, errorMsg, Object.class));
				} else {
					LOGGER.warn(
					  "Found bake method in config group class " + className + ", but the group " +
					  "already has a configured baker.\nOnly the configured baker will " +
					  "be used.\nIf the configured builder is precisely this method, rename it " +
					  "to suppress this warning.");
				}
			} else if (
			  Arrays.stream(configClass.getDeclaredMethods()).anyMatch(mm -> mm.getName().equals("bake"))
			) {
				LOGGER.warn(
				  "Found method named \"bake\" in config group class " + className + " with an " +
				  "unsupported signature.\nRename the method to suppress this warning, or fix the bug");
			}
		}
		builders.remove(configClass);
	}
	
	protected static boolean hasAnyConfigAnnotation(Field field) {
		synchronized (PARSERS) {
			for (Class<? extends Annotation> clazz : PARSERS.keySet())
				if (field.isAnnotationPresent(clazz))
					return false;
		}
		return hasAnyAnnotation(
		  field, Text.class, RequireRestart.class, Min.class, Max.class,
		  HasAlpha.class, Slider.class, NonPersistent.class);
	}
	
	@SafeVarargs protected static boolean hasAnyAnnotation(Field field, Class<? extends Annotation>... annotations) {
		for (Class<? extends Annotation> annotation : annotations)
			if (field.isAnnotationPresent(annotation)) return true;
		return false;
	}
	
	protected static int getOrder(Field field) {
		if (field.isAnnotationPresent(Entry.class))
			return field.getAnnotation(Entry.class).value();
		else if (field.isAnnotationPresent(Text.class))
			return field.getAnnotation(Text.class).value();
		else return 0;
	}
	
	public static AbstractSimpleConfigEntryHolderBuilder<?> getBuilderForClass(Class<?> clazz) {
		return builders.get(clazz);
	}
	
	public static class SimpleConfigClassParseException extends RuntimeException {
		public SimpleConfigClassParseException(Class<?> parsedClass, String message) {
			this(getBuilderForClass(parsedClass), message);
		}
		public SimpleConfigClassParseException(Class<?> parsedClass, String message, Exception cause) {
			this(getBuilderForClass(parsedClass), message, cause);
		}
		
		public SimpleConfigClassParseException(
		  AbstractSimpleConfigEntryHolderBuilder<?> builder, String message
		) {
			super(message + getExtraMessage(builder));
		}
		public SimpleConfigClassParseException(
		  AbstractSimpleConfigEntryHolderBuilder<?> builder, String message, Exception cause
		) {
			super(message + getExtraMessage(builder), cause);
		}
		
		protected static String getExtraMessage(AbstractSimpleConfigEntryHolderBuilder<?> builder) {
			if (builder == null)
				return "\n  Could not get parsing context information";
			StringBuilder r = new StringBuilder("\n");
			r.append("  Parsing config ").append(builder).append("\n");
			r.append("    Defined entries:\n");
			for (Map.Entry<String, AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?>> entry :
			  builder.entries.entrySet()) {
				final AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?> e = entry.getValue();
				r.append("      ").append(entry.getKey()).append(": ")
				  .append(e.getClass().getSimpleName());
				if (e.typeClass != null)
					r.append(" (").append(e.typeClass.getName()).append(")");
				r.append("\n");
			}
			return r.toString();
		}
	}
}
