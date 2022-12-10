package endorh.simpleconfig.core.reflection;

import com.google.common.collect.Lists;
import endorh.simpleconfig.api.AtomicEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.EntryTag;
import endorh.simpleconfig.api.SimpleConfig.ConfigReflectiveOperationException;
import endorh.simpleconfig.api.SimpleConfigTextUtil;
import endorh.simpleconfig.api.annotation.Error;
import endorh.simpleconfig.api.annotation.*;
import endorh.simpleconfig.api.annotation.Slider.SliderType;
import endorh.simpleconfig.api.entry.*;
import endorh.simpleconfig.api.entry.RangedEntryBuilder.InvertibleDouble2DoubleFunction;
import endorh.simpleconfig.api.range.DoubleRange;
import endorh.simpleconfig.api.range.FloatRange;
import endorh.simpleconfig.api.range.IntRange;
import endorh.simpleconfig.api.range.LongRange;
import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractConfigEntryBuilder;
import endorh.simpleconfig.core.DummyEntryHolder;
import endorh.simpleconfig.core.EntryType;
import endorh.simpleconfig.core.SimpleConfigClassParser.SimpleConfigClassParseException;
import endorh.simpleconfig.core.reflection.BindingContext.MethodWrapper;
import endorh.simpleconfig.core.reflection.BindingContext.ParametersAdapter;
import endorh.simpleconfig.core.reflection.BindingContext.ReturnTypeAdapter;
import endorh.simpleconfig.core.reflection.FieldBuilderDecorator.AnnotationFieldDecorator;
import endorh.simpleconfig.core.reflection.FieldBuilderDecorator.FieldDecorator;
import endorh.simpleconfig.core.reflection.FieldEntryBuilder.ClassFieldEntryBuilder;
import endorh.simpleconfig.core.reflection.FieldEntryBuilder.SimpleFieldEntryBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.ByteNBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

import static endorh.simpleconfig.api.ConfigBuilderFactoryProxy.list;
import static endorh.simpleconfig.api.ConfigBuilderFactoryProxy.*;
import static endorh.simpleconfig.core.ReflectionUtil.getMethodName;
import static endorh.simpleconfig.core.SimpleConfigClassParser.CONFIG_ENTRY_ANNOTATIONS;
import static endorh.simpleconfig.core.SimpleConfigClassParser.SimpleConfigClassParseException.getEntryBuilderName;
import static endorh.simpleconfig.core.reflection.BindingContext.ParametersAdapter.lastOptionalAdapter;
import static endorh.simpleconfig.core.reflection.BindingContext.ParametersAdapter.oneOptionalAdapter;
import static java.util.Collections.*;

public class FieldParser {
	private static final List<FieldTypeParser<?>> PARSERS = synchronizedList(new ArrayList<>());
	private static final List<FieldBuilderDecorator<?>> DECORATORS = synchronizedList(new ArrayList<>());
	
	static {
		registerFieldTypeParsers();
		registerFieldTypeDecorators();
	}
	
	private static void registerFieldTypeParsers() {
		reg(t(boolean.class), (d, o) -> bool(o.orElse(false)));
		reg(t(byte.class), (d, o) -> number(o.orElse((byte) 0)));
		reg(t(short.class), (d, o) -> number(o.orElse((short) 0)));
		reg(t(int.class), (d, o) -> number(o.orElse(0)));
		reg(t(long.class), (d, o) -> number(o.orElse(0L)));
		reg(t(float.class), (d, o) -> number(o.orElse(0F)));
		reg(t(double.class), (d, o) -> number(o.orElse(0D)));
		reg(t(String.class), (d, o) -> string(o.orElse("")));
		reg(FieldTypeFilter.subClasses(Enum.class), (a, c, o) -> makeEnum(
		  o.orElseGet(() -> Arrays.stream(c.getEnumConstants()).findFirst()
			 .orElseThrow(() -> new IllegalStateException("Enum class has no enum constants")))));
		reg(t(IntRange.class), (d, o) -> range(o.orElse(IntRange.UNIT)));
		reg(t(LongRange.class), (d, o) -> range(o.orElse(LongRange.UNIT)));
		reg(t(FloatRange.class), (d, o) -> range(o.orElse(FloatRange.UNIT)));
		reg(t(DoubleRange.class), (d, o) -> range(o.orElse(DoubleRange.UNIT)));
		reg(t(Color.class), (d, o) -> color(o.orElse(Color.BLACK)));
		reg(t(Pattern.class), (d, o) -> pattern(o.orElse(Pattern.compile(""))));
		reg(t(INBT.class), (d, o) -> nbtValue(o.orElse(ByteNBT.valueOf(false))));
		reg(t(CompoundNBT.class), (d, o) -> nbtTag(o.orElse(new CompoundNBT())));
		reg(t(ResourceLocation.class), (d, o) -> resource(o.orElse(new ResourceLocation(""))));
		reg(t(KeyBindMapping.class), (d, o) -> key(o.orElse(KeyBindMapping.unset())));
		reg(t(Item.class), (d, o) -> item(o.orElse(Items.AIR)));
		reg(t(Block.class), (d, o) -> block(o.orElse(Blocks.AIR)));
		reg(t(Fluid.class), (d, o) -> fluid(o.orElse(Fluids.EMPTY)));
		
		reg(EntryType.of(List.class, EntryType.unchecked(Pair.class)), (ctx, d, at, t, c, o) -> {
			AnnotatedParameterizedType apt = requireParameterized(d, at);
			AnnotatedType tt = apt.getAnnotatedActualTypeArguments()[0];
			AnnotatedParameterizedType pt = requireParameterized(d, tt);
			ConfigEntryBuilder<?, ?, ?, ?>[] args = parseSubTypes(ctx, d, pt, "k", "v");
			return makePairList(args[0], args[1], o.orElse(emptyList()));
		});
		rec(t(List.class), (ctx, d, sub, t, c, o) -> makeList(sub[0], o.orElse(emptyList())), "v");
		rec(t(Set.class), (ctx, d, sub, t, c, o) -> makeSet(sub[0], o.orElse(emptySet())), "v");
		rec(t(Map.class), (ctx, d, sub, t, c, o) -> makeMap(sub[0], sub[1], o.orElse(emptyMap())), "k", "v");
		
		// Captioned lists
		rec(EntryType.of(Pair.class, EntryType.wildcard(), EntryType.unchecked(List.class)),
		    (ctx, d, sub, t, c, o) -> makeCaptionedList(sub[0], sub[1], o.orElse(null)), "caption", "list");
		rec(EntryType.of(Pair.class, EntryType.wildcard(), EntryType.unchecked(Set.class)),
		    (ctx, d, sub, t, c, o) -> makeCaptionedSet(sub[0], sub[1], o.orElse(null)), "caption", "set");
		rec(EntryType.of(Pair.class, EntryType.wildcard(), EntryType.unchecked(Map.class)),
		    (ctx, d, sub, t, c, o) -> makeCaptionedMap(sub[0], sub[1], o.orElse(null)), "caption", "map");
		rec(EntryType.of(Pair.class, EntryType.wildcard(), EntryType.of(List.class, EntryType.unchecked(Pair.class))),
		    (ctx, d, sub, t, c, o) -> makeCaptionedPairList(sub[0], sub[1], o.orElse(null)), "caption", "list");
		rec(t(Pair.class), (ctx, d, sub, t, c, o) -> makePair(
		  sub[0], sub[1], o.orElse(Pair.of(sub[0].getValue(), sub[1].getValue()))), "l", "r");
		rec(t(Triple.class), (ctx, d, sub, t, c, o) -> makeTriple(
		  sub[0], sub[1], sub[2], o.orElse(Triple.of(sub[0].getValue(), sub[1].getValue(), sub[2].getValue()))), "l", "m", "r");
		
		reg(FieldTypeFilter.annotated(Bean.class), (a, c, o) -> {
			try {
				Object v;
				if (!o.isPresent()) {
					Constructor<?> constructor = c.getDeclaredConstructor();
					constructor.setAccessible(true);
					try {
						v = constructor.newInstance();
					} catch (RuntimeException e) {
						throw new UnsupportedConfigEntryFieldTypeException(
						  a, "Error instantiating bean of type: " + c.getCanonicalName(), e);
					}
				} else v = o.get();
				return bean(v);
			} catch (InstantiationException | IllegalAccessException | InvocationTargetException |
			         NoSuchMethodException e) {
				throw new UnsupportedConfigEntryFieldTypeException(
				  a, "Failed to instantiate bean: " + c.getCanonicalName() +
				     "\n  Ensure your bean class has an accessible default constructor", e);
			}
		});
	}
	
	private static void registerFieldTypeDecorators() {
		dec(ConfigEntryBuilder.class, RequireRestart.class, (a, b) -> b.restart(a.value()));
		dec(ConfigEntryBuilder.class, NonPersistent.class, (a, b) -> b.temp(a.value()));
		dec(ConfigEntryBuilder.class, Experimental.class, (a, b) -> b.experimental(a.value()));
		dec(ConfigEntryBuilder.class, Advanced.class, (a, b) -> b.withTags(a.value(), EntryTag.ADVANCED));
		dec(ConfigEntryBuilder.class, Operator.class, (a, b) -> b.withTags(a.value(), EntryTag.OPERATOR));
		
		// @Min and @Max
		dec(RangedEntryBuilder.class, Min.class, (a, b) -> b.min(adapt(b.getClass(), a.value())));
		dec(RangedEntryBuilder.class, Max.class, (a, b) -> b.max(adapt(b.getClass(), a.value())));
		
		// @Slider
		dec(RangedEntryBuilder.class, (d, b) -> d.get(Slider.class).map(a -> {
			RangedEntryBuilder<?, ?, ?, ?> bb = a.label().isEmpty()? b.slider() : b.slider(a.label());
			Optional<InvertibleDouble2DoubleFunction> o = SliderType.fromAnnotation(a);
			if (o.isPresent()) bb = bb.sliderMap(o.get());
			Double min = Double.isNaN(a.min())? null : a.min();
			Double max = Double.isNaN(a.max())? null : a.max();
			if (min != null || max != null) {
				Class<?> c = b.getClass();
				bb = bb.sliderRange(min != null? adapt(c, min) : null, max != null? adapt(c, max) : null);
			}
			return bb;
		}).orElse(b));
		
		// @Bake.Scale
		dec(FloatEntryBuilder.class, Bake.Scale.class, (a, b) -> b.bakeScale((float) a.value()));
		dec(DoubleEntryBuilder.class, Bake.Scale.class, (a, b) -> b.bakeScale(a.value()));
		
		// @HasAlpha
		dec(ColorEntryBuilder.class, HasAlpha.class, (a, b) -> b.alpha(a.value()));
		dec(ConfigEntryBuilder.class, Default.class, (c, d, a, b) -> {
			Object v;
			Optional<ITextComponent> error;
			try {
				AbstractConfigEntry<?, ?, ?> e = DummyEntryHolder.build(b);
				v = e.fromCommand(a.value());
				error = v == null? e.getErrorFromCommand(a.value()) : Optional.empty();
			} catch (RuntimeException e) {
				throw new UnsupportedConfigEntryFieldTypeException(
				  d, "Error setting default value for type: " + b.getClass() +
				     "\n  This type may not support setting a default value with the @Default annotation", e);
			}
			if (v == null) {
				String errorText = error.map(ITextComponent::getString).orElse(
				  "Ensure it's valid YAML of the expected type!");
				throw new InvalidConfigEntryFieldAnnotationException(
				  d, "Invalid default value for entry: " + a.value() + "\n  " +
				     errorText.replace("\n", "\n  "));
			}
			return b.withValue(v);
		});
		dec(StringEntryBuilder.class, Length.class, (s, b) -> {
			if (s.min() > 0) b = b.minLength(s.min());
			if (s.max() < Integer.MAX_VALUE) b = b.maxLength(s.max());
			return b;
		});
		dec(CollectionEntryBuilder.class, Size.class, (s, b) -> {
			if (s.min() > 0) b = (CollectionEntryBuilder<?, ?, ?, ?>) b.minSize(s.min());
			if (s.max() < Integer.MAX_VALUE) b = (CollectionEntryBuilder<?, ?, ?, ?>) b.maxSize(s.max());
			return b;
		});
		dec(EntryMapEntryBuilder.class, Linked.class, (a, b) -> b.linked(a.value()));
		
		dec(ConfigEntryBuilder.class, (c, d, b) -> {
			MethodWrapper<Optional<ITextComponent>> m = c.findOwnMethod(
			  "$error", oneOptionalAdapter(getType(b)), ERROR_TYPE_ADAPTERS);
			if (m != null) return ((ConfigEntryBuilder<?, ?, ?, ?>) b).error(m::invoke);
			return b;
		});
		dec(ConfigEntryBuilder.class, (c, d, b) -> addTooltip(c, b));
		dec(ConfigEntryBuilder.class, (c, d, b) -> {
			EntryType<?> type = getType(b);
			MethodWrapper<?> m = c.findOwnMethod(
			  "$bake", oneOptionalAdapter(type), ReturnTypeAdapter.identity(type));
			if (m != null) return ((ConfigEntryBuilder<Object, ?, ?, ?>) b).baked(m::invoke);
			return b;
		});
		dec(StringEntryBuilder.class, (c, d, b) -> d.get(Suggest.class).map(s -> {
			if (s.value().length > 0) {
				return b.suggest(s.value());
			} else if (!s.method().isEmpty()) {
				Method m = c.requireMethod(s.method(), EntryType.from(List.class, String.class));
				return b.suggest(() -> invoke(m, null, List.class));
			} else return b.suggest();
		}).orElse(b));
		dec(ConfigEntryBuilder.class, (c, d, b) -> d.get(Error.class).map(s -> {
			if (s.method().isEmpty()) throw new InvalidConfigEntryFieldAnnotationException(
			  d, "@Error annotation must specify a non empty method name");
			if (cast(b).getErrorSupplier() != null) throw new InvalidConfigEntryFieldAnnotationException(
			  d, "@Error annotation used for field of entry which already has an error supplier");
			MethodWrapper<Optional<ITextComponent>> m = c.requireMethod(
			  s.method(), oneOptionalAdapter(getType(b)), ERROR_TYPE_ADAPTERS);
			ConfigEntryBuilder<?, ?, ?, ?> bb = b;
			return bb = bb.error(m::invoke);
		}).orElse(b));
		dec(ConfigEntryBuilder.class, (c, d, b) -> d.get(Bake.class).map(s -> {
			if (s.method().isEmpty()) throw new InvalidConfigEntryFieldAnnotationException(
			  d, "@Bake annotation must specify a non empty method name");
			if (cast(b).getPresentation() != null) throw new InvalidConfigEntryFieldAnnotationException(
			  d, "@Bake annotation used for field of entry which already has a baking method");
			EntryType<?> type = getType(b);
			Optional<Annotation> pOpt = d.getParent(s);
			ParametersAdapter[] pars = pOpt.map(
			  pa -> lastOptionalAdapter(EntryType.unchecked(pa.annotationType()), type)
			).orElse(ParametersAdapter.singleSignature(type));
			BindingContext ctx = c;
			if (pOpt.isPresent()) {
				Class<?> declaringClass = pOpt.get().annotationType().getDeclaringClass();
				if (declaringClass != null)
					ctx = new BindingContext(declaringClass, ctx);
			}
			MethodWrapper<?> m = ctx.requireMethod(
			  s.method(), pars, ReturnTypeAdapter.identity(type));
			ConfigEntryBuilder<Object, ?, ?, ?> bb = b;
			if (pOpt.isPresent()) {
				Annotation a = pOpt.get();
				return bb = bb.baked(v -> m.invoke(v, a));
			}
			return bb = bb.baked(m::invoke);
		}).orElse(b));
	}
	
	private static final ReturnTypeAdapter<?, Optional<ITextComponent>>[] ERROR_TYPE_ADAPTERS = new ReturnTypeAdapter[]{
	  ReturnTypeAdapter.of(EntryType.from(Optional.class, ITextComponent.class), t -> t),
	  ReturnTypeAdapter.of(EntryType.of(ITextComponent.class), Optional::ofNullable),
	  ReturnTypeAdapter.of(EntryType.of(String.class), t -> Optional.ofNullable(t).map(TranslationTextComponent::new)),
	  ReturnTypeAdapter.of(EntryType.of(boolean.class), t -> t? Optional.of(new TranslationTextComponent("simpleconfig.config.error.invalid_value_generic")) : Optional.empty())
	};
	
	private static final ReturnTypeAdapter<?, List<ITextComponent>>[] TOOLTIP_TYPE_ADAPTERS = new ReturnTypeAdapter[]{
	  ReturnTypeAdapter.of(EntryType.from(List.class, ITextComponent.class), t -> t),
	  ReturnTypeAdapter.of(EntryType.of(ITextComponent.class), Collections::singletonList),
	  ReturnTypeAdapter.of(EntryType.of(String.class), SimpleConfigTextUtil::optSplitTtc),
	};
	
	public static ConfigEntryBuilder<?, ?, ?, ?> addTooltip(
	  BindingContext c, ConfigEntryBuilder<?, ?, ?, ?> b
	) {
		MethodWrapper<List<ITextComponent>> m = c.findOwnMethod(
		  "$tooltip", oneOptionalAdapter(getType(b)), TOOLTIP_TYPE_ADAPTERS);
		if (m != null) return b.tooltip(v -> m.invoke(v));
		return b;
	}
	
	public static ConfigEntryBuilder<?, ?, ?, ?> parseInstanceField(
	  BindingContext ctx, Field field, Object instance
	) {
		try {
			ctx.setContextName(field);
			Object o = field.get(instance);
			return parseType(ctx, field.getAnnotatedType(), EntryTypeData.fromField(ctx, field), null, o);
		} catch (IllegalAccessException e) {
			throw new SimpleConfigClassParseException(
			  field.getDeclaringClass(), "Cannot access entry field " + field.getName(), e);
		}
	}
	
	public static ConfigEntryBuilder<?, ?, ?, ?> parseField(BindingContext ctx, Field field) {
		try {
			ctx.setContextName(field);
			Object o = field.get(null);
			return parseType(ctx, field.getAnnotatedType(), EntryTypeData.fromField(ctx, field), null, o);
		} catch (IllegalAccessException e) {
			throw new SimpleConfigClassParseException(
			  field.getDeclaringClass(), "Cannot access entry field " + field.getName(), e);
		}
	}
	
	public static ConfigEntryBuilder<?, ?, ?, ?> parseSubType(
	  EntryTypeData parentData, BindingContext ctx, AnnotatedType type, String name
	) {
		String prevCtx = ctx.getContextName();
		ctx.setContextName(prevCtx + "$" + name);
		ConfigEntryBuilder<?, ?, ?, ?> subType = parseType(ctx, type, null, parentData, null);
		ctx.setContextName(prevCtx);
		return subType;
	}
	
	public static ConfigEntryBuilder<?, ?, ?, ?> parseType(
	  BindingContext ctx, AnnotatedType aType,
	  @Nullable EntryTypeData data, @Nullable EntryTypeData parentData, @Nullable Object value
	) {
		Class<?> cls;
		Type type = aType.getType();
		EntryTypeData d = EntryTypeData.fromType(ctx, aType);
		if (data != null) d = EntryTypeData.merge(data, d);
		if (type instanceof Class<?>) {
			cls = (Class<?>) type;
		} else if (type instanceof ParameterizedType) {
			cls = (Class<?>) ((ParameterizedType) type).getRawType();
		} else throw new UnexpectedFieldParsingException(
		  d, "Unexpected type found introspecting config field generic type: " +
		     type.getClass().getCanonicalName());
		try {
			for (FieldTypeParser<?> parser: PARSERS) {
				if (parser.getFilter().isApplicable(aType)) {
					return decorateType(ctx, d, create(parser, d, aType, type, cls, value));
				}
			}
		} catch (RuntimeException e) {
			throw new UnsupportedConfigEntryFieldTypeException(
			  d, "Error creating config entry from type: " + EntryType.fromType(type), e);
		}
		throw new UnsupportedConfigEntryFieldTypeException(
		  d, "Unsupported config field type: " + EntryType.fromType(type) +
		     "\n  Please, refer to Simple Config's documentation/wiki for supported config field " +
		     "types in the declarative API");
	}
	
	public static ConfigEntryBuilder<?, ?, ?, ?> decorateType(
	  BindingContext ctx, EntryTypeData data, ConfigEntryBuilder<?, ?, ?, ?> builder
	) {
		Class<?> cls = builder.getClass();
		Map<Configure, Class<? extends Annotation>> entries = data.getConfigureAnnotations();
		List<Configure> rev = Lists.reverse(new ArrayList<>(entries.keySet()));
		for (Configure e: rev) {
			Class<? extends Annotation> cl = entries.get(e);
			String methodName = e.value();
			if (!methodName.isEmpty()) {
				BindingContext c = ctx;
				EntryType<?> t = EntryType.unchecked(cls);
				Object obj;
				if (cl != null) {
					// We look within the declaring class, since annotation
					//   classes cannot have static methods
					if (cl.getDeclaringClass() != null)
						c = new BindingContext(cl.getDeclaringClass(), ctx);
					MethodWrapper<?> m = c.requireCompatibleMethod(methodName, true, lastOptionalAdapter(
					  EntryType.unchecked(cl), t
					), ReturnTypeAdapter.identity(t));
					obj = m.invoke(builder, data.getOrNull(cl));
				} else {
					Method m = c.requireCompatibleMethod(methodName, true, cls, cls);
					obj = invoke(m, null, cls, builder);
				}
				if (obj.getClass() != cls) throw new SimpleConfigClassParseException(
				  c.cls, "Configure method " + methodName + " must return " + cls.getName());
				builder = (ConfigEntryBuilder<?, ?, ?, ?>) obj;
			}
		}
		for (FieldBuilderDecorator<?> decorator: DECORATORS)
			if (decorator.isApplicable(builder))
				builder = decorate(decorator, data, builder);
		Optional<Class<? extends Annotation>> opt = Arrays.stream(data.getUnusedAnnotations())
		  .filter(CONFIG_ENTRY_ANNOTATIONS::contains)
		  .findFirst();
		if (opt.isPresent()) {
			throw new InvalidConfigEntryFieldAnnotationException(
			  data,
			  "Inapplicable config entry annotation (@" + opt.get().getSimpleName() +
			  ") for entry of type " + getEntryBuilderName(builder) +
			  " [" + getType(builder) + "]");
		}
		return builder;
	}
	
	@SuppressWarnings("unchecked") private static <T> ConfigEntryBuilder<?, ?, ?, ?> create(
	  FieldTypeParser<T> parser, EntryTypeData d,
	  AnnotatedType aType, Type type, Class<?> cls, Object value
	) {
		return parser.create(d, aType, type, (Class<T>) cls, (T) value);
	}
	
	@SuppressWarnings("unchecked") private static <
	  B extends ConfigEntryBuilder<?, ?, ?, B>, FD extends FieldBuilderDecorator<B>
	> ConfigEntryBuilder<?, ?, ?, ?> decorate(
	  FieldBuilderDecorator<?> decorator, EntryTypeData d, ConfigEntryBuilder<?, ?, ?, ?> builder
	) {
		return ((FD) decorator).decorate(d, (B) builder);
	}
	
	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	@FunctionalInterface public interface RecursiveFieldTypeBuilder<T> {
		ConfigEntryBuilder<?, ?, ?, ?> build(
		  BindingContext ctx, EntryTypeData a, ConfigEntryBuilder<?, ?, ?, ?>[] args,
		  AnnotatedParameterizedType type, Class<?> cls, Optional<T> o);
	}
	
	@SuppressWarnings("unchecked")
	private static <T> AbstractConfigEntryBuilder<T, ?, ?, ?, ?, ?> cast(
	  ConfigEntryBuilder<T, ?, ?, ?> b
	) {
		return (AbstractConfigEntryBuilder<T, ?, ?, ?, ?, ?>) b;
	}
	
	private static @NotNull AnnotatedParameterizedType requireParameterized(
	  EntryTypeData d, AnnotatedType at
	) {
		if (!(at instanceof AnnotatedParameterizedType))
			throw new UnexpectedFieldParsingException(
			  d, "Unexpected type found introspecting config field type: " +
			     at.getClass().getCanonicalName());
		return (AnnotatedParameterizedType) at;
	}
	
	static EntryType<?> getType(ConfigEntryBuilder<?, ?, ?, ?> b) {
		AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?> bb = cast(b);
		return bb.getEntryType();
	}
	
	private static <T> EntryType<T> t(Class<T> cls) {
		return EntryType.unchecked(cls);
	}
	
	private static <T> void rec(
	  EntryType<T> cls, RecursiveFieldTypeBuilder<T> builder, String... names
	) {
		reg(cls, (ctx, a, at, t, c, o) -> {
			if (at instanceof AnnotatedParameterizedType) {
				AnnotatedParameterizedType p = (AnnotatedParameterizedType) at;
				ConfigEntryBuilder<?, ?, ?, ?>[] args = parseSubTypes(ctx, a, p, names);
				return builder.build(ctx, a, args, p, c, o);
			}
			throw new UnexpectedFieldParsingException(
			  a, "Unexpected type found introspecting config field type: " + at.getClass().getCanonicalName());
		});
	}
	
	private static ConfigEntryBuilder<?, ?, ?, ?>[] parseSubTypes(
	  BindingContext ctx, EntryTypeData d, AnnotatedParameterizedType type, String... names
	) {
		AnnotatedType[] subTypes = type.getAnnotatedActualTypeArguments();
		if (subTypes.length != names.length) throw new UnexpectedFieldParsingException(
		  d, "Unexpected number of subtypes (" + subTypes.length + ", expected: " + names.length +
		     ") found introspecting config field generic type: " + type.getType());
		ConfigEntryBuilder<?, ?, ?, ?>[] builders = new ConfigEntryBuilder[names.length];
		for (int i = 0; i < names.length; i++)
			builders[i] = parseSubType(d, ctx, subTypes[i], names[i]);
		return builders;
	}
	
	@SuppressWarnings("unchecked") private static <E extends Enum<E>> ConfigEntryBuilder<?, ?, ?, ?> makeEnum(Object v) {
		return option((E) v);
	}
	
	@SuppressWarnings("unchecked") private static <T, C, G, B extends ConfigEntryBuilder<T, C, G, B>>
	ConfigEntryBuilder<?, ?, ?, ?> makeList(ConfigEntryBuilder<T, ?, ?, ?> b, List<?> l) {
		return list((B) b, (List<T>) l);
	}
	
	@SuppressWarnings("unchecked") private static <T, C, G, B extends ConfigEntryBuilder<T, C, G, B>>
	ConfigEntryBuilder<?, ?, ?, ?> makeSet(ConfigEntryBuilder<T, ?, ?, ?> b, Set<?> l) {
		return set((B) b, (Set<T>) l);
	}
	
	@SuppressWarnings("unchecked") private static <
	  K, KC, KG, KB extends ConfigEntryBuilder<K, KC, KG, KB> & AtomicEntryBuilder,
	  V, VC, VG, VB extends ConfigEntryBuilder<V, VC, VG, VB>
	> ConfigEntryBuilder<?, ?, ?, ?> makeMap(
	  ConfigEntryBuilder<K, ?, ?, ?> kb, ConfigEntryBuilder<V, ?, ?, ?> b, Map<?, ?> l
	) {
		if (!(kb instanceof AtomicEntryBuilder)) throw new IllegalArgumentException(
		  "Key type in map is not atomic: " + kb.getClass().getName());
		return map((KB) kb, (VB) b, (Map<K, V>) l);
	}
	
	@SuppressWarnings("unchecked") private static <
	  K, KC, KG, KB extends ConfigEntryBuilder<K, KC, KG, KB> & AtomicEntryBuilder,
	  V, VC, VG, VB extends ConfigEntryBuilder<V, VC, VG, VB>
	> ConfigEntryBuilder<?, ?, ?, ?> makePairList(
	  ConfigEntryBuilder<K, ?, ?, ?> kb, ConfigEntryBuilder<V, ?, ?, ?> b, List<?> l
	) {
		if (!(kb instanceof AtomicEntryBuilder)) throw new IllegalArgumentException(
		  "Key type in map is not atomic: " + kb.getClass().getName());
		return pairList((KB) kb, (VB) b, (List<Pair<K, V>>) l);
	}
	
	@SuppressWarnings("unchecked") @Internal public static <
	  C, CC, CG, CB extends ConfigEntryBuilder<C, CC, CG, CB> & AtomicEntryBuilder,
	  L, LC, LG, LB extends ListEntryBuilder<L, LC, LG, LB>
	> ConfigEntryBuilder<?, ?, ?, ?> makeCaptionedList(
	  ConfigEntryBuilder<C, ?, ?, ?> cb, ConfigEntryBuilder<?, ?, ?, ?> b, @Nullable Pair<?, ?> v
	) {
		CaptionedCollectionEntryBuilder<@NotNull List<@NotNull L>, List<LC>, LG, LB, C, CC, CG, CB>
		  e = caption((CB) atomic(cb), (LB) b);
		if (v != null) e = e.withValue((Pair<C, List<L>>) v);
		return e;
	}
	
	@SuppressWarnings("unchecked") @Internal public static <
	  CV, CC, CG, CB extends ConfigEntryBuilder<CV, CC, CG, CB> & AtomicEntryBuilder,
	  V, C, G, B extends ConfigEntryBuilder<V, C, G, B>,
	  SB extends EntrySetEntryBuilder<V, C, G, B>
	> ConfigEntryBuilder<?, ?, ?, ?> makeCaptionedSet(
	  ConfigEntryBuilder<CV, ?, ?, ?> cb, ConfigEntryBuilder<V, ?, ?, ?> b, @Nullable Pair<?, ?> v
	) {
		CaptionedCollectionEntryBuilder<@NotNull Set<@NotNull V>, Set<C>, G, EntrySetEntryBuilder<V, C, G, B>, CV, CC, CG, CB>
		  e = caption((CB) atomic(cb), (SB) b);
		if (v != null) e = e.withValue((Pair<CV, Set<V>>) v);
		return e;
	}
	
	@SuppressWarnings("unchecked") @Internal public static <
	  CV, CC, CG, CB extends ConfigEntryBuilder<CV, CC, CG, CB> & AtomicEntryBuilder,
	  K, KC, KG, KB extends ConfigEntryBuilder<K, KC, KG, KB> & AtomicEntryBuilder,
	  V, C, G, B extends ConfigEntryBuilder<V, C, G, B>,
	  MB extends EntryMapEntryBuilder<K, V, KC, C, KG, G, B, KB>
	> ConfigEntryBuilder<?, ?, ?, ?> makeCaptionedMap(
	  ConfigEntryBuilder<CV, ?, ?, ?> cb, ConfigEntryBuilder<?, ?, ?, ?> mb, @Nullable Pair<?, ?> v
	) {
		CaptionedCollectionEntryBuilder<@NotNull Map<@NotNull K, @NotNull V>, Map<KC, C>, Pair<KG, G>, EntryMapEntryBuilder<K, V, KC, C, KG, G, B, KB>, CV, CC, CG, CB>
		  e = caption((CB) atomic(cb), (MB) mb);
		if (v != null) e = e.withValue((Pair<CV, Map<K, V>>) v);
		return e;
	}
	
	@SuppressWarnings("unchecked") @Internal public static <
	  CV, CC, CG, CB extends ConfigEntryBuilder<CV, CC, CG, CB> & AtomicEntryBuilder,
	  K, KC, KG, KB extends ConfigEntryBuilder<K, KC, KG, KB> & AtomicEntryBuilder,
	  V, C, G, B extends ConfigEntryBuilder<V, C, G, B>,
	  PLB extends EntryPairListEntryBuilder<K, V, KC, C, KG, G, B, KB>
	> ConfigEntryBuilder<?, ?, ?, ?> makeCaptionedPairList(
	  ConfigEntryBuilder<CV, ?, ?, ?> cb, ConfigEntryBuilder<?, ?, ?, ?> b, @Nullable Pair<?, ?> v
	) {
		CaptionedCollectionEntryBuilder<@NotNull List<@NotNull Pair<@NotNull K, @NotNull V>>, List<Pair<KC, C>>, Pair<KG, G>, EntryPairListEntryBuilder<K, V, KC, C, KG, G, B, KB>, CV, CC, CG, CB>
		  e = caption((CB) atomic(cb), (PLB) b);
		if (v != null) e = e.withValue((Pair<CV, List<Pair<K, V>>>) v);
		return e;
	}
	
	@SuppressWarnings("unchecked") private static <
	  L, LC, LG, LB extends ConfigEntryBuilder<L, LC, LG, LB> & AtomicEntryBuilder,
	  R, RC, RG, RB extends ConfigEntryBuilder<R, RC, RG, RB> & AtomicEntryBuilder
	> ConfigEntryBuilder<?, ?, ?, ?> makePair(
	  ConfigEntryBuilder<L, ?, ?, ?> lb, ConfigEntryBuilder<R, ?, ?, ?> rb, Pair<?, ?> p
	) {
		return pair((LB) atomic(lb), (RB) atomic(rb), (Pair<L, R>) p);
	}
	
	@SuppressWarnings("unchecked") private static <
	  L, LC, LG, LB extends ConfigEntryBuilder<L, LC, LG, LB> & AtomicEntryBuilder,
	  M, MC, MG, MB extends ConfigEntryBuilder<M, MC, MG, MB> & AtomicEntryBuilder,
	  R, RC, RG, RB extends ConfigEntryBuilder<R, RC, RG, RB> & AtomicEntryBuilder
	  > ConfigEntryBuilder<?, ?, ?, ?> makeTriple(
	  ConfigEntryBuilder<L, ?, ?, ?> lb, ConfigEntryBuilder<M, ?, ?, ?> mb,
	  ConfigEntryBuilder<R, ?, ?, ?> rb, Triple<?, ?, ?> p
	) {
		return triple((LB) atomic(lb), (MB) atomic(mb), (RB) atomic(rb), (Triple<L, M, R>) p);
	}
	
	@SuppressWarnings("unchecked") private static <
	  V, C, G, B extends ConfigEntryBuilder<V, C, G, B> & AtomicEntryBuilder
	> B atomic(ConfigEntryBuilder<?, ?, ?, ?> b) {
		if (!(b instanceof AtomicEntryBuilder)) throw new IllegalArgumentException(
		  "Type is not atomic: " + b.getClass().getName());
		return (B) b;
	}
	
	@SuppressWarnings("unchecked") private static <T, B extends ConfigEntryBuilder<T, ?, ?, ?>> T adapt(
	  Class<?> cls, double v
	) {
		if (ByteEntryBuilder.class.isAssignableFrom(cls)) {
			return (T) (Byte) ((Double) v).byteValue();
		} else if (ShortEntryBuilder.class.isAssignableFrom(cls)) {
			return (T) (Short) ((Double) v).shortValue();
		} else if (IntegerEntryBuilder.class.isAssignableFrom(cls)) {
			return (T) (Integer) ((Double) v).intValue();
		} else if (LongEntryBuilder.class.isAssignableFrom(cls)) {
			return (T) (Long) ((Double) v).longValue();
		} else if (FloatEntryBuilder.class.isAssignableFrom(cls)) {
			return (T) (Float) ((Double) v).floatValue();
		} else if (DoubleEntryBuilder.class.isAssignableFrom(cls)) {
			return (T) (Double) v;
		} else throw new IllegalArgumentException("Unexpected numeric builder type: " + cls);
	}
	
	private static <V> void reg(EntryType<V> type, FieldEntryBuilder<V> parser) {
		reg(FieldTypeFilter.matching(type), parser);
	}
	private static <V> void reg(EntryType<V> type, ClassFieldEntryBuilder<V> parser) {
		reg(FieldTypeFilter.matching(type), parser);
	}
	private static <V> void reg(EntryType<V> type, SimpleFieldEntryBuilder<V> parser) {
		reg(FieldTypeFilter.matching(type), parser);
	}
	
	private static <V> void reg(FieldTypeFilter filter, FieldEntryBuilder<V> parser) {
		reg(FieldTypeParser.of(filter, parser));
	}
	private static <V> void reg(FieldTypeFilter filter, ClassFieldEntryBuilder<V> parser) {
		reg(FieldTypeParser.of(filter, parser));
	}
	private static <V> void reg(FieldTypeFilter filter, SimpleFieldEntryBuilder<V> parser) {
		reg(FieldTypeParser.of(filter, parser));
	}
	
	private static <V> void reg(FieldTypeParser<V> parser) {
		PARSERS.add(parser);
	}
	
	@SafeVarargs
	private static <T, B extends ConfigEntryBuilder<T, ?, ?, BB>, BB extends B> void dec(
	  Class<B> cls, BiFunction<EntryTypeData, B, B>... parsers
	) {
		for (BiFunction<EntryTypeData, B, B> p: parsers)
			DECORATORS.add(FieldBuilderDecorator.of(cls, p));
	}
	
	private static <B extends ConfigEntryBuilder> void dec(
	  Class<B> cls, FieldDecorator<B> parser
	) {
		DECORATORS.add(FieldBuilderDecorator.of(cls, parser));
	}
	
	private static <T, A extends Annotation, B extends ConfigEntryBuilder<T, ?, ?, BB>, BB extends B> void dec(
	  Class<B> cls, Class<A> a, BiFunction<A, B, B> transform
	) {
		dec(cls, (as, b) -> as.get(a).map(aa -> transform.apply(aa, b)).orElse(b));
	}
	
	private static <A extends Annotation, B extends ConfigEntryBuilder> void dec(
	  Class<B> cls, Class<A> a, AnnotationFieldDecorator<A, B> decorator
	) {
		dec(cls, (c, d, b) -> d.get(a).map(aa -> decorator.decorate(c, d, aa, b)).orElse(b));
	}
	
	private static <T, A extends Annotation, B extends ConfigEntryBuilder<T, ?, ?, BB>, BB extends B> void dec(
	  Class<B> cls, Class<A> a, Function<B, B> transform
	) {
		dec(cls, a, (aa, b) -> transform.apply(b));
	}
	
	static <T> T invoke(
	  Method method, Object self, Class<T> type, Object... args
	) {
		try {
			return type.cast(method.invoke(self, args));
		} catch (InvocationTargetException | IllegalAccessException | ClassCastException e) {
			throw new ConfigReflectiveOperationException(
			  "Error accessing config method: " + getMethodName(method) +
			  "\n  Details: " + e.getMessage(), e);
		}
	}
	
	public static class UnexpectedFieldParsingException extends SimpleConfigClassParseException {
		public UnexpectedFieldParsingException(EntryTypeData data, String message) {
			super(data.getContextClass(), message);
		}
		
		public UnexpectedFieldParsingException(EntryTypeData data, String message, Exception cause) {
			super(data.getContextClass(), message, cause);
		}
	}
	
	public static class InvalidConfigEntryFieldAnnotationException extends SimpleConfigClassParseException {
		public InvalidConfigEntryFieldAnnotationException(EntryTypeData data, String message) {
			super(data.getContextClass(), message);
		}
		
		public InvalidConfigEntryFieldAnnotationException(EntryTypeData data, String message, Exception cause) {
			super(data.getContextClass(), message, cause);
		}
	}
	
	public static class UnsupportedConfigEntryFieldTypeException extends SimpleConfigClassParseException {
		public UnsupportedConfigEntryFieldTypeException(EntryTypeData data, String message) {
			super(data.getContextClass(), message);
		}
		public UnsupportedConfigEntryFieldTypeException(EntryTypeData data, String message, Exception cause) {
			super(data.getContextClass(), message, cause);
		}
	}
}
