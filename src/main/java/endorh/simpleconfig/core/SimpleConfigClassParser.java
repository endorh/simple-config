package endorh.simpleconfig.core;

import com.google.common.collect.Sets;
import endorh.simpleconfig.api.*;
import endorh.simpleconfig.api.annotation.Error;
import endorh.simpleconfig.api.annotation.*;
import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.core.BackingField.BackingFieldBinding;
import endorh.simpleconfig.core.BackingField.BackingFieldBuilder;
import endorh.simpleconfig.core.SimpleConfigBuilderImpl.CategoryBuilder;
import endorh.simpleconfig.core.SimpleConfigBuilderImpl.GroupBuilder;
import endorh.simpleconfig.core.entry.TextEntry;
import endorh.simpleconfig.core.reflection.FieldParser;
import endorh.simpleconfig.core.reflection.MethodBindingContext;
import endorh.simpleconfig.core.reflection.MethodBindingContext.MethodWrapper;
import endorh.simpleconfig.core.reflection.MethodBindingContext.MethodWrapper.AdapterMethodWrapper;
import endorh.simpleconfig.core.reflection.MethodBindingContext.ParametersAdapter;
import endorh.simpleconfig.core.reflection.MethodBindingContext.ReturnTypeAdapter;
import net.minecraft.util.text.ITextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static endorh.simpleconfig.api.ConfigBuilderFactoryProxy.category;
import static endorh.simpleconfig.api.ConfigBuilderFactoryProxy.group;
import static endorh.simpleconfig.core.ReflectionUtil.getFieldName;
import static endorh.simpleconfig.core.ReflectionUtil.getMethodName;
import static endorh.simpleconfig.core.reflection.MethodBindingContext.ParametersAdapter.emptySignature;
import static endorh.simpleconfig.core.reflection.MethodBindingContext.oneOptionalAdapter;

public class SimpleConfigClassParser {
	private static final Logger LOGGER = LogManager.getLogger();
	@SuppressWarnings("unchecked")
	public static final Set<Class<? extends Annotation>> CONFIG_ENTRY_ANNOTATIONS = Sets.newHashSet(
	  Advanced.class, Default.class, Error.class, Experimental.class, HasAlpha.class,
	  Length.class, Linked.class, Max.class, Min.class, NonPersistent.class, Operator.class,
	  RequireRestart.class, Size.class, Slider.class, Suggest.class
	);
	
	// Used to construct exception messages
	protected static final Map<Class<?>, AbstractSimpleConfigEntryHolderBuilder<?>> builders = new ConcurrentHashMap<>();
	protected static final Map<SimpleConfigBuilderImpl, Map<Class<?>, Set<Method>>> seenMethods = new ConcurrentHashMap<>();
	
	protected static void decorateBuilder(SimpleConfigBuilderImpl root) {
		MethodBindingContext ctx = null;
		if (root.configClass != null) {
			ctx = methodBindingContext(root, root.configClass, null);
			decorateAbstractBuilder(root, ctx, root.configClass, root);
		}
		for (CategoryBuilder catBuilder : root.categories.values()) if (catBuilder.configClass != null) {
			decorateAbstractBuilder(
			  root, methodBindingContext(root, catBuilder.configClass, ctx),
			  catBuilder.configClass, catBuilder);
		}
		checkBoundMethods(root);
		seenMethods.remove(root);
	}
	
	private static MethodBindingContext methodBindingContext(
	  SimpleConfigBuilderImpl root, Class<?> cls, @Nullable MethodBindingContext parent
	) {
		Set<Method> set = new HashSet<>();
		seenMethods.computeIfAbsent(root, c -> new HashMap<>()).put(cls, set);
		return MethodBindingContext.forConfigClass(cls, parent, set);
	}
	
	protected static void decorateAbstractBuilder(
	  SimpleConfigBuilderImpl root, MethodBindingContext ctx,
	  Class<?> configClass, AbstractSimpleConfigEntryHolderBuilder<?> builder
	) {
		builders.put(configClass, builder);
		Class<?> builderTypeClass = match(builder, r -> SimpleConfigBuilder.class, c -> ConfigCategoryBuilder.class, g -> ConfigGroupBuilder.class);
		EntryType<?> builderType = EntryType.unchecked(builderTypeClass);
		MethodWrapper<?> buildMethod = ctx.findOwnMethod("build", ParametersAdapter.singleSignature(builderType), ReturnTypeAdapter.identity(builderType));
		if (buildMethod != null)
			builder = (AbstractSimpleConfigEntryHolderBuilder<?>) buildMethod.invoke(builder);
		AdapterMethodWrapper<Icon> iconGetter = ctx.findCompatibleMethod(
		  "getIcon", false, emptySignature(),
		  ReturnTypeAdapter.identity(EntryType.unchecked(Icon.class)));
		if (iconGetter != null) {
			if (builder != root && !(builder instanceof CategoryBuilder)) {
				LOGGER.warn(
				  "Config class " + configClass.getName() + " has an icon getter, but it's not a config " +
				  "filo nor a category! Only categories and the file itself can have an icon getter, " +
				  "so this method will be ignored.");
			} else try {
				Icon icon = iconGetter.invoke();
				if (icon != null) {
					if (builder == root) {
						if (root.defaultCategory.icon != null) LOGGER.warn(
						  "Config class " + configClass.getName() + " has an icon getter, but it " +
						  "already has a default category icon! Only the first icon set will be used!" +
						  "\n  Remove the `getIcon` method, or the call to `withIcon` to suppress this warning!"
						);
						else root.withIcon(icon);
					} else {
						CategoryBuilder cb = (CategoryBuilder) builder;
						if (cb.icon != null) LOGGER.warn(
						  "Config category class " + configClass.getName() + " has an icon getter, " +
						  "but it already has an icon! Only the first icon set will be used!" +
						  "\n  Remove the `getIcon` method, or the call to `withIcon` to suppress this warning!"
						);
						else cb.withIcon(icon);
					}
				} else LOGGER.warn(
				  "Icon getter for method " + iconGetter.getMethodName() + " returned a null icon!");
			} catch (RuntimeException e) {
				throw new SimpleConfigClassParseException(
				  builder, "Error creating icon for config category from method " +
				           iconGetter.getMethodName());
			}
		}
		if (configClass.isAnnotationPresent(Category.class)) {
			if (!(builder instanceof CategoryBuilder)) throw new SimpleConfigClassParseException(
			  builder, "Config class " + configClass.getName() + " is annotated with @Category, " +
			           "but is not associated with a category builder!!");
			CategoryBuilder cb = (CategoryBuilder) builder;
			Category cat = configClass.getAnnotation(Category.class);
			if (!cat.background().isEmpty()) {
				if (cb.background != null) LOGGER.warn(
				  "Config class " + configClass.getName() + " specifies a background texture in its " +
				  "@Category annotation, but already has a background!" +
				  "\n  Only the first background set will be used. Remove the background from the " +
				  "annotation to suppress this warning."
				); else cb.withBackground(cat.background());
			}
			if (cat.color() != 0) {
				if (cb.tint != 0) LOGGER.warn(
				  "Config class " + configClass.getName() + " specifies a tint color in its " +
				  "@Category annotation, but already has a tint color!" +
				  "\n  Only the first tint set will be used. Remove the color from the " +
				  "annotation to suppress this warning."
				); else cb.withColor(cat.color());
			}
		}
		if (configClass.isAnnotationPresent(ConfigClass.class)) {
			if (builder != root) throw new SimpleConfigClassParseException(
			  builder, "Config class " + configClass.getName() + " is annotated with @ConfigClass, " +
			           "but is not the root config class!!");
			ConfigClass config = configClass.getAnnotation(ConfigClass.class);
			if (!config.background().isEmpty()) {
				if (root.background != null) LOGGER.warn(
				  "Config class " + configClass.getName() + " specifies a background texture in its " +
				  "@ConfigClass annotation, but already has a background!" +
				  "\n  Only the first background set will be used. Remove the background from the " +
				  "annotation to suppress this warning."
				); else root.withBackground(config.background());
			}
			if (config.color() != 0) {
				if (root.defaultCategory.tint != 0) LOGGER.warn(
				  "Config class " + configClass.getName() + " specifies a default tint color in its " +
				  "@ConfigClass annotation, but already has a default tint color!" +
				  "\n  Only the first tint set will be used. Remove the color from the " +
				  "annotation to suppress this warning."
				); else root.withColor(config.color());
			}
		}
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
		for (Field field: configClass.getDeclaredFields()) {
			final String name = field.getName();
			if (secondaryBackingFields.contains(field)) continue;
			final String fieldTypeName = field.getGenericType().getTypeName();
			if (!Modifier.isStatic(field.getModifiers()))
				throw new SimpleConfigClassParseException(
				  builder,
				  "Config class members must be static. Found non-static field " + getFieldName(field));
			if (field.isAnnotationPresent(Group.class) || field.isAnnotationPresent(Category.class)) {
				Group group = field.getAnnotation(Group.class);
				if (field.getType() != Void.class) throw new SimpleConfigClassParseException(
				  builder, "Found " + (group != null? "group" : "category") + " marker field with non-void type: " + getFieldName(field));
				String holderName = name.endsWith("$marker")? name.substring(0, name.length() - 7) : name;
				if (group != null) {
					if (builder.groups.containsKey(holderName)) throw new SimpleConfigClassParseException(
					  builder, "Found group marker field for already registered group: " + getFieldName(field));
					boolean expand = group.expand();
					Class<?> cl = Arrays.stream(configClass.getDeclaredClasses())
					  .filter(c -> c.getSimpleName().equals(holderName))
					  .findFirst().orElseThrow(() -> new SimpleConfigClassParseException(
						  configClass, "Found group marker field without matching group class: " + getFieldName(field)));
					Group gr = cl.getAnnotation(Group.class);
					expand |= gr != null && gr.expand();
					builder.n(group(holderName, expand));
				} else {
					if (root != builder) throw new SimpleConfigClassParseException(
					  builder, "Found category marker field in non-root builder: " + getFieldName(field));
					if (root.categories.containsKey(holderName)) throw new SimpleConfigClassParseException(
					  builder, "Found category marker field for already registered category: " + getFieldName(field));
					Class<?> cl = Arrays.stream(configClass.getDeclaredClasses())
					  .filter(c -> c.getSimpleName().equals(holderName))
					  .findFirst().orElseThrow(() -> new SimpleConfigClassParseException(
						  configClass, "Found category marker field without matching category class: " + getFieldName(field)));
					root.n(category(holderName, cl));
				}
				if (hasAnyConfigAnnotation(field)) LOGGER.warn(
				  "Group/category marker field " + getFieldName(field) + " has config annotations " +
				  "that have no effect\n  Remove them to suppress this warning.");
				continue;
			}
			Object value;
			try {
				field.setAccessible(true);
				value = field.get(null);
			} catch (IllegalAccessException e) {
				throw new SimpleConfigClassParseException(
				  builder, "Couldn't access config class field " +
				           getFieldName(field) + "\n  Details: " +
				           e.getMessage(), e);
			}
			if (field.isAnnotationPresent(Text.class)) {
				TextEntry.Builder textBuilder;
				if (field.getType() == Void.class) {
					textBuilder = new TextEntry.Builder();
				} else if (value instanceof ITextComponent) {
					final ITextComponent tc = (ITextComponent) value;
					textBuilder = new TextEntry.Builder(() -> tc);
				} else if (value instanceof Supplier && EntryType.from(
				  Supplier.class, ITextComponent.class
				).equals(EntryType.fromField(field))) {
					// noinspection unchecked
					final Supplier<ITextComponent> supplier = (Supplier<ITextComponent>) value;
					textBuilder = new TextEntry.Builder(supplier);
				} else throw new SimpleConfigClassParseException(
				  builder, "Unsupported text supplier in config field " + getFieldName(field) + " of type " +
				  fieldTypeName + "\n  Should be either String (contents ignored), ITextComponent or Supplier<ITextComponent>");
				ctx.setContextName(field);
				builder.add(getOrder(field), field.getName(), FieldParser.addTooltip(ctx, textBuilder));
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
				throw new SimpleConfigClassParseException(
				  builder,
				  "Config field " + getFieldName(field) + " was annotated with @Bind but no " +
				  "matching config entry was found defined");
			if (field.isAnnotationPresent(Entry.class) || field.isAnnotationPresent(Caption.class)) {
				ConfigEntryBuilder<?, ?, ?, ?> entryBuilder = FieldParser.parseField(ctx, field);
				if (!(entryBuilder instanceof AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?>))
					throw new IllegalStateException(
					  "Entry builder " + entryBuilder.getClass().getCanonicalName() +
					  " is not an AbstractConfigEntryBuilder");
				AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?> b = (AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?>) entryBuilder;
				BackingFieldBuilder<?, ?> fieldBuilder = b.backingFieldBuilder;
				if (fieldBuilder == null) throw new SimpleConfigClassParseException(
				  builder, "Config entry generated from field does not support backing fields");
				BackingField<?, ?> backingField = fieldBuilder.build(field);
				if (field.isAnnotationPresent(Caption.class)) {
					if (builder instanceof GroupBuilder) {
						GroupBuilder gBuilder = (GroupBuilder) builder;
						if (gBuilder.captionBuilder != null) throw new SimpleConfigClassParseException(
						  builder, "Field " + getFieldName(field) + " is annotated with @Caption, but " +
						           "this group already has a caption entry: " + gBuilder.captionName);
						addCaption(gBuilder, name, entryBuilder, field);
					} else throw new SimpleConfigClassParseException(
					  builder, "Found @Caption annotation on field " + getFieldName(field) +
					  " in non-group builder: " + className);
				} else builder.add(getOrder(field), name, entryBuilder);
				builder.setBackingField(name, backingField);
				continue;
			}
			if (hasAnyConfigAnnotation(field)) throw new SimpleConfigClassParseException(
			  builder, "Unsupported config annotation/field type combination in field " +
			           getFieldName(field) + " of type " + fieldTypeName);
		}
		for (Class<?> clazz : configClass.getDeclaredClasses()) {
			final String name = clazz.getSimpleName();
			if (clazz.isAnnotationPresent(Category.class) || root.categories.containsKey(name)) {
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
				decorateAbstractBuilder(root, methodBindingContext(root, clazz, ctx), clazz, catBuilder);
			} else if (clazz.isAnnotationPresent(Group.class) || builder.groups.containsKey(name)) {
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
				decorateAbstractBuilder(root, methodBindingContext(root, clazz, ctx), clazz, gBuilder);
			} else if (clazz.isAnnotationPresent(Bind.class))
				throw new SimpleConfigClassParseException(builder,
				  "Config inner class " + clazz.getName() + " was annotated with @Bind " +
				  "but no matching config category/group was found defined");
		}
		Class<?> bakeArgClass = match(builder, c -> SimpleConfig.class, c -> SimpleConfigCategory.class, c -> SimpleConfigGroup.class);
		Consumer<? extends ConfigEntryHolder> prevBaker = match(builder, c -> c.baker, c -> c.baker, c -> c.baker);
		MethodWrapper<Void> baker = ctx.findOwnMethod(
		  "bake", oneOptionalAdapter(EntryType.unchecked(bakeArgClass)),
		  ReturnTypeAdapter.ofVoid());
		if (baker != null) {
			if (prevBaker == null) {
				match(builder, c -> c.withBaker(baker::invoke), c -> c.withBaker(baker::invoke), c -> c.withBaker(baker::invoke));
			} else LOGGER.warn(
			  "Found bake method in config class " + className + ", but this config " +
			  "file/category/group already has a configured baker\nOnly the already " +
			  "configured baker will be used\nIf the configured baker is precisely this " +
			  "method, rename it or let the reflection find it to suppress this warning.");
		}
		builders.remove(configClass);
	}
	
	@SuppressWarnings("unchecked") private static <V, C, G, B extends ConfigEntryBuilder<V, C, G, B> & AtomicEntryBuilder> void addCaption(
	  GroupBuilder builder, String name, ConfigEntryBuilder<V, C, G, ?> entryBuilder, Field field
	) {
		if (!(entryBuilder instanceof AtomicEntryBuilder)) throw new SimpleConfigClassParseException(
		  builder, "Field " + getFieldName(field) + " is annotated with @Caption, but " +
		           "its entry type builder doesn't implement AtomicEntryBuilder" +
		           "\nThis type of config entry cannot be used as caption: " +
		           EntryType.fromField(field));
		builder.caption(name, (B) entryBuilder);
	}
	
	protected static void checkBoundMethods(SimpleConfigBuilderImpl root) {
		Map<Class<?>, Set<Method>> methodSets = seenMethods.remove(root);
		if (methodSets == null) return;
		for (Map.Entry<Class<?>, Set<Method>> e: methodSets.entrySet()) {
			Class<?> cls = e.getKey();
			Set<Method> methods = e.getValue();
			Optional<Method> opt = Arrays.stream(cls.getDeclaredMethods())
			  .filter(m -> m.isAnnotationPresent(Bind.class) && !methods.contains(m))
			  .findFirst();
			if (opt.isPresent()) throw new SimpleConfigClassParseException(
			  cls, "Found unbound method in config class annotated with @Bind: " +
			       getMethodName(opt.get()) +
			       "\nMake sure the references to this method from config annotations are correct " +
			       "or remove the @Bind annotation");
		}
	}
	
	protected static <R> R match(
	  AbstractSimpleConfigEntryHolderBuilder<?> builder,
	  Function<SimpleConfigBuilderImpl, R> rootGetter,
	  Function<CategoryBuilder, R> categoryGetter,
	  Function<GroupBuilder, R> groupGetter
	) {
		if (builder instanceof SimpleConfigBuilderImpl)
			return rootGetter.apply((SimpleConfigBuilderImpl) builder);
		else if (builder instanceof CategoryBuilder)
			return categoryGetter.apply((CategoryBuilder) builder);
		else if (builder instanceof GroupBuilder)
			return groupGetter.apply((GroupBuilder) builder);
		else throw new IllegalArgumentException(
			  "Unknown builder type: " + builder.getClass().getCanonicalName());
	}
	
	protected static boolean hasAnyConfigAnnotation(Field field) {
		for (Class<? extends Annotation> cls: CONFIG_ENTRY_ANNOTATIONS)
			if (field.isAnnotationPresent(cls)) return true;
		return false;
	}
	
	protected static int getOrder(Field field) {
		if (field.isAnnotationPresent(Entry.class)) {
			return field.getAnnotation(Entry.class).value();
		} else if (field.isAnnotationPresent(Text.class)) {
			return field.getAnnotation(Text.class).value();
		} else if (field.isAnnotationPresent(Caption.class)) {
			return -1;
		} else return 0;
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
		
		public static String getExtraMessage(Class<?> parsedClass) {
			return getExtraMessage(getBuilderForClass(parsedClass));
		}
		
		public static String getExtraMessage(AbstractSimpleConfigEntryHolderBuilder<?> builder) {
			if (builder == null)
				return "\n  Could not get config building context information";
			StringBuilder r = new StringBuilder("\n");
			r.append("  Parsing config ").append(builder).append("\n");
			r.append("    Defined entries:\n");
			for (Map.Entry<String, AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?>> entry :
			  builder.entries.entrySet()) {
				final AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?> e = entry.getValue();
				r.append("      ").append(entry.getKey()).append(": ")
				  .append(Arrays.stream(e.getClass().getInterfaces())
				            .filter(ConfigEntryBuilder.class::isAssignableFrom).findFirst()
				            .map(Class::getSimpleName).orElse(e.getClass().getSimpleName() +
				                                              " [unknown builder interface!]"));
				if (e.type != null)
					r.append(" (").append(e.type).append(")");
				r.append("\n");
			}
			return r.toString();
		}
		
		public static String getEntryBuilderName(ConfigEntryBuilder<?, ?, ?, ?> builder) {
			return Arrays.stream(builder.getClass().getInterfaces())
			  .filter(ConfigEntryBuilder.class::isAssignableFrom).findFirst()
			  .map(Class::getSimpleName)
			  .orElse(builder.getClass().getSimpleName());
		}
	}
}
