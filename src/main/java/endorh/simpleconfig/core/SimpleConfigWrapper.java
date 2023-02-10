package endorh.simpleconfig.core;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.common.collect.Lists;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.api.*;
import endorh.simpleconfig.api.SimpleConfig.Type;
import endorh.simpleconfig.api.entry.ConfigEntrySerializer;
import endorh.simpleconfig.api.entry.ListEntryBuilder;
import endorh.simpleconfig.api.entry.RangedEntryBuilder;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons.MinecraftOptions;
import endorh.simpleconfig.config.CommonConfig.menu;
import endorh.simpleconfig.core.SimpleConfigBuilderImpl.ConfigValueBuilder;
import endorh.simpleconfig.ui.hotkey.ConfigHotKeyManager;
import endorh.simpleconfig.yaml.SimpleConfigCommentedYamlFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.ValueSpec;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static endorh.simpleconfig.api.ConfigBuilderFactoryProxy.*;
import static endorh.simpleconfig.api.SimpleConfigTextUtil.applyStyle;
import static endorh.simpleconfig.api.SimpleConfigTextUtil.splitTtc;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@EventBusSubscriber(modid=SimpleConfigMod.MOD_ID, bus = Bus.MOD)
public class SimpleConfigWrapper {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Method Range$getClazz;
	private static final Method Range$getMin;
	private static final Method Range$getMax;
	
	static {
		Class<?> cls;
		Method getClazz = null;
		Method getMin = null;
		Method getMax = null;
		try {
			cls = Class.forName("net.minecraftforge.common.ForgeConfigSpec$Range");
			getClazz = cls.getMethod("getClazz");
			getClazz.setAccessible(true);
			getMin = cls.getMethod("getMin");
			getMin.setAccessible(true);
			getMax = cls.getMethod("getMax");
			getMax.setAccessible(true);
		} catch (ClassNotFoundException | NoSuchMethodException e) {
			LOGGER.error("Failed access ForgeConfigSpec.Range class", e);
		} finally {
			Range$getClazz = getClazz;
			Range$getMin = getMin;
			Range$getMax = getMax;
		}
	}
	
	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onLoadComplete(FMLLoadCompleteEvent event) {
		event.enqueueWork(() -> {
			wrapConfigs();
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ConfigHotKeyManager::initHotKeyManager);
		});
	}
	
	public static void wrapConfigs() {
		ModList.get().forEachModContainer((modId, container) -> {
			if (!menu.shouldWrapConfig(modId)) return;
			EnumMap<ModConfig.Type, ModConfig> configs = getConfigs(container);
			if (!configs.isEmpty()) {
				String displayName = container.getModInfo().getDisplayName();
				String logName = displayName + " (" + modId + ")";
				for (Type type: SimpleConfig.Type.values()) {
					ModConfig.Type configType = type.asConfigType();
					String tt = type.getAlias();
					try {
						if (configs.containsKey(configType)) {
							ModConfig config = configs.get(configType);
							List<ModConfig> extraConfigs = collectExtraConfigFiles(container, config, type);
							if (!(config instanceof SimpleConfigModConfig)) {
								LOGGER.info("Wrapping " + tt + " config for mod {}", modId);
								IConfigSpec<?> s = config.getSpec();
								if (!(s instanceof ForgeConfigSpec spec)) throw new IllegalArgumentException(
								  "Config spec for mod " + container.getModInfo().getDisplayName() + " (" +
								  container.getModId() + ") is not a ForgeConfigSpec");
								if (extraConfigs.size() == 1) {
									extraConfigs = emptyList();
								} else if (!extraConfigs.isEmpty()) {
									spec = null;
								}
								Pair<SimpleConfigBuilder, Map<String, ForgeConfigSpec>> pair = wrap(
								  container, config, spec, extraConfigs);
								SimpleConfigBuilderImpl builder =
								  pair != null? (SimpleConfigBuilderImpl) pair.getLeft() : null;
								if (builder == null
								    || builder.entries.isEmpty()
								       && builder.categories.isEmpty() && builder.groups.isEmpty()) {
									LOGGER.warn("Unable to wrap " + tt + " config for mod {}: " +
									            "Wrapped config is empty", modId);
								} else builder.buildAndRegister(
								  null, new WrappingConfigValueBuilder(
									 container, config, extraConfigs, spec, pair.getRight()));
							}
						}
						LOGGER.info("Wrapped " + tt + " config for mod " + logName);
					} catch (RuntimeException e) {
						LOGGER.error(
						  "Error wrapping " + tt + " config for mod " + logName + "\n" +
						  "You may report this error at the Simple Config issue tracker", e);
					}
				}
			}
		});
	}
	
	private static List<ModConfig> collectExtraConfigFiles(ModContainer container, ModConfig config, Type type) {
		Set<ModConfig> configs = ConfigTracker.INSTANCE.configSets().get(type.asConfigType());
		return configs.stream()
		  .filter(c -> c.getModId().equals(container.getModId()))
		  .filter(c -> !(c instanceof SimpleConfigModConfig))
		  .collect(Collectors.toList());
	}
	
	private static Pair<SimpleConfigBuilder, Map<String, ForgeConfigSpec>> wrap(
	  ModContainer container, ModConfig config, @Nullable ForgeConfigSpec spec, List<ModConfig> extraFiles
	) {
		if (config instanceof SimpleConfigModConfig) return null;
		Type type = SimpleConfig.Type.fromConfigType(config.getType());
		SimpleConfigBuilderImpl builder = (SimpleConfigBuilderImpl) config(config.getModId(), type);
		
		if (spec != null) wrapConfig(builder, spec.getValues(), spec.getSpec(), "");
		Map<String, ForgeConfigSpec> extraSpecs = new HashMap<>();
		for (ModConfig extra: extraFiles) {
			if (!(extra.getSpec() instanceof ForgeConfigSpec extraSpec)) throw new IllegalArgumentException(
			  "Config spec for mod " + container.getModInfo().getDisplayName() + " (" +
			  container.getModId() + ") is not a ForgeConfigSpec");
			String key = extractExtraFileName(extra);
			if (builder.categories.containsKey(key)) {
				LOGGER.warn("Extra config file {} for mod {} has the same name as an existing config category",
				            extra.getFileName(), container.getModId());
				key = "file/" + key;
				if (builder.categories.containsKey(key)) {
					LOGGER.warn(
					  "Extra config file {} for mod {} could not be wrapped, because it's fallback " +
					  "category name: {} is already in use",
					  extra.getFileName(), container.getModId(), key);
					continue;
				}
			}
			extraSpecs.put(key, extraSpec);
			
			ConfigCategoryBuilder categoryBuilder = category(key)
			  .withIcon(MinecraftOptions.FILE)
			  .withColor(0x80424242)
			  .withDescription(() -> splitTtc(
				 "simpleconfig.ui.config.file",
				 new TextComponent(extra.getFileName()).withStyle(ChatFormatting.DARK_AQUA)));
			wrapConfig(categoryBuilder, extraSpec.getValues(), extraSpec.getSpec(), key);
			builder.n(categoryBuilder);
		}
		return Pair.of(builder, extraSpecs);
	}
	
	private static final Pattern EXTRA_FILE_NAME = Pattern.compile(
	  "(?:.*?[/\\\\])?(?!.*[/\\\\])(?<name>[^.]*+)\\.\\w+$");
	private static String extractExtraFileName(ModConfig config) {
		Matcher matcher = EXTRA_FILE_NAME.matcher(config.getFileName());
		if (matcher.matches()) return matcher.group("name").replace('.', ' ');
		return config.getFileName();
	}
	
	protected static class WrappingConfigValueBuilder extends ConfigValueBuilder {
		private final ModContainer container;
		private final ModConfig modConfig;
		private final Map<String, ModConfig> extraConfigs;
		private final ForgeConfigSpec spec;
		private final Map<String, ForgeConfigSpec> extraSpecs;
		private final Stack<UnmodifiableConfig> stack = new Stack<>();
		private final Stack<String> path = new Stack<>();
		
		protected WrappingConfigValueBuilder(
		  ModContainer container, ModConfig modConfig, List<ModConfig> extraConfigs,
		  ForgeConfigSpec spec, Map<String, ForgeConfigSpec> extraSpecs
		) {
			this.container = container;
			this.modConfig = modConfig;
			this.extraConfigs = extraConfigs.stream().collect(Collectors.toMap(c -> {
				IConfigSpec<?> s = c.getSpec();
				return extraSpecs.entrySet().stream()
				  .filter(ss -> ss.getValue() == s).findFirst()
				  .map(Entry::getKey).orElseThrow();
			}, Function.identity()));
			this.spec = spec;
			this.extraSpecs = extraSpecs;
			stack.push(spec != null? this.spec.getValues() : CommentedConfig.inMemory());
		}
		
		@Override public void buildModConfig(SimpleConfigImpl config) {
			config.build(container, modConfig, extraConfigs);
		}
		
		@Override public void build(
		  AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?> entryBuilder,
		  AbstractConfigEntry<?, ?, ?> entry
		) {
			Object o = getConfigValues().get(entry.name);
			if (o instanceof ConfigValue) {
				entry.setConfigValue((ConfigValue<?>) o);
			} else throw new IllegalStateException("Cannot wrap entry: " + entry.name);
		}
		
		protected UnmodifiableConfig getConfigValues() {
			return stack.peek();
		}
		
		protected String getPath() {
			return String.join(".", path);
		}
		
		protected String getPath(String name) {
			String path = getPath();
			return path.isEmpty()? name : path + "." + name;
		}
		
		@Override public boolean canBuildEntry(String name) {
			boolean r = getConfigValues().get(name) instanceof ConfigValue;
			if (!r) LOGGER.warn("Unexpected entry in wrapped config: " + getPath(name));
			return r;
		}
		
		@Override public boolean canBuildSection(String name) {
			boolean r = getConfigValues().get(name) instanceof UnmodifiableConfig
			            || extraSpecs.containsKey(name);
			if (!r) LOGGER.warn("Unexpected section in wrapped config: " + getPath(name));
			return r;
		}
		
		@Override public void enterSection(String name) {
			Object o = getConfigValues().get(name);
			if (o instanceof UnmodifiableConfig) {
				stack.push((UnmodifiableConfig) o);
			} else if (extraSpecs.containsKey(name)) {
				stack.push(extraSpecs.get(name).getValues());
			} else throw new IllegalStateException("Cannot wrap config section: " + name);
		}
		
		@Override public void exitSection() {
			stack.pop();
		}
		
		@Override public Pair<ForgeConfigSpec, List<ForgeConfigSpec>> build() {
			return Pair.of(spec, new ArrayList<>(extraSpecs.values()));
		}
	}
	
	private static void wrapConfig(
	  ConfigEntryHolderBuilder<?> builder, UnmodifiableConfig values,
	  UnmodifiableConfig spec, String path
	) {
		Map<String, Object> specMap = values.valueMap();
		for (Map.Entry<String, Object> specEntry: specMap.entrySet()) {
			final String key = specEntry.getKey();
			String entryPath = path.isEmpty()? key : path + "." + key;
			final Object specValue = specEntry.getValue();
			if (specValue instanceof UnmodifiableConfig subSpec) {
				if (builder instanceof SimpleConfigBuilder configBuilder && menu.wrap_top_level_groups_as_categories) {
					ConfigCategoryBuilder categoryBuilder = category(key);
					wrapConfig(categoryBuilder, subSpec, spec, entryPath);
					configBuilder.n(categoryBuilder);
				} else {
					ConfigGroupBuilder groupBuilder = group(key, true);
					wrapConfig(groupBuilder, subSpec, spec, entryPath);
					builder.n(groupBuilder);
				}
			} else if (specValue instanceof ConfigValue<?> configValue) {
				Object o = spec.get(configValue.getPath());
				if (o instanceof ValueSpec s) {
					Optional<ConfigEntryBuilder<?, ?, ?, ?>> opt = wrapValue(s);
					if (opt.isPresent()) {
						builder.add(key, opt.get());
					} else LOGGER.warn(
					  "Could not wrap config value: " + entryPath + " of type "
					  + configValue.getClass().getCanonicalName());
				} else LOGGER.warn(
				  "Invalid value spec: " + o + " for entry " + entryPath);
			}
		}
	}
	
	private static Optional<ConfigEntryBuilder<?, ?, ?, ?>> wrapValue(ValueSpec spec) {
		Class<?> clazz = guessValueClass(spec);
		for (ValueSpecAdapter<?, ?> adapter: ADAPTERS) {
			if (adapter.getClazz().isAssignableFrom(clazz))
				return Optional.ofNullable(adapter.createBuilder(spec));
		}
		return Optional.empty();
	}
	
	private static Class<?> guessValueClass(ValueSpec spec) {
		Class<?> clazz = spec.getClazz();
		if (clazz != Object.class) return clazz;
		clazz = tryGetClassFromRange(spec);
		if (clazz != null) return clazz;
		return spec.getDefault().getClass();
	}
	
	private static @Nullable Class<?> tryGetClassFromRange(ValueSpec spec) {
		if (Range$getClazz == null) return null;
		Object range = spec.getRange();
		if (range == null) return null;
		try {
			Object clazz = Range$getClazz.invoke(range);
			return clazz instanceof Class? (Class<?>) clazz : null;
		} catch (InvocationTargetException | IllegalAccessException ignored) {
			return null;
		}
	}
	
	private static <T> @Nullable Pair<T, T> tryGetRange(ValueSpec spec, Class<T> clazz) {
		if (Range$getMin == null) return null;
		Object range = spec.getRange();
		if (range == null) return null;
		try {
			Object min = Range$getMin.invoke(range);
			Object max = Range$getMax.invoke(range);
			return clazz.isInstance(min) && clazz.isInstance(max)
			       ? Pair.of(clazz.cast(min), clazz.cast(max)) : null;
		} catch (InvocationTargetException | IllegalAccessException ignored) {
			return null;
		}
	}
	
	private static final Pattern INDENT = Pattern.compile("^\\s*+");
	private static final Pattern LINE_BREAK = Pattern.compile("\\R");
	private static final Pattern EXPERIMENTAL = Pattern.compile(
	  "EXPERIMENTAL", Pattern.CASE_INSENSITIVE);
	private static <B extends ConfigEntryBuilder<?, ?, ?, B>> B decorateBuilder(
	  ConfigEntryBuilder<?, ?, ?, ?> builder, ValueSpec spec
	) {
		builder = builder.restart(spec.needsWorldRestart());
		String translation = spec.getTranslationKey();
		if (translation != null)
			builder = ((AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?>) builder).translation(translation);
		String comment = spec.getComment();
		if (comment != null) {
			List<Component> tooltip = new ArrayList<>();
			String[] lines = LINE_BREAK.split(comment);
			int commonIndent = Arrays.stream(lines).mapToInt(l -> {
				Matcher m = INDENT.matcher(l);
				if (!m.matches()) return 0;
				return m.end();
			}).min().orElse(0);
			for (String l: lines) {
				l = l.substring(commonIndent);
				MutableComponent ll = new TextComponent(l)
				  .withStyle(ChatFormatting.GRAY);
				Matcher m = EXPERIMENTAL.matcher(l);
				if (m.find()) {
					builder = builder.withTags(EntryTag.EXPERIMENTAL);
					if (FMLEnvironment.dist == Dist.CLIENT)
						ll = applyStyle(ll, ChatFormatting.GOLD, m.start(), m.end());
				}
				tooltip.add(ll);
			}
			builder = builder.tooltip(tooltip);
		}
		builder = builder.configError(t -> !spec.test(t)? Optional.of(new TranslatableComponent(
		  "simpleconfig.config.error.invalid_value_generic")) : Optional.empty());
		//noinspection unchecked
		return (B) builder;
	}
	
	@FunctionalInterface interface IValueSpecAdapter<V, C> {
		ConfigEntryBuilder<V, C, ?, ?> createBuilder(ValueSpec spec, V defValue);
		@SuppressWarnings("unchecked") default <B extends ConfigEntryBuilder<?, ?, ?, B>> B
		createCastBuilder(ValueSpec spec, V defValue) {
			return (B) createBuilder(spec, defValue);
		}
	}
	
	@FunctionalInterface interface IRangedValueSpecAdapter<
	  V extends Comparable<V>, C
	> {
		RangedEntryBuilder<V, C, ?, ?> createBuilder(ValueSpec spec, V defValue);
	}
	
	public static class ValueSpecAdapter<V, C> {
		private final Class<? extends C> clazz;
		private final Function<C, V> transform;
		private final IValueSpecAdapter<V, C> adapter;
		
		public ValueSpecAdapter(Class<? extends C> clazz, Function<C, V> transform, IValueSpecAdapter<V, C> adapter) {
			this.clazz = clazz;
			this.transform = transform;
			this.adapter = adapter;
		}
		
		public @Nullable ConfigEntryBuilder<?, ?, ?, ?> createBuilder(ValueSpec spec) {
			Object defValue = spec.getDefault();
			C def = clazz.cast(defValue);
			V value = transform.apply(def);
			return decorateBuilder(adapter.createCastBuilder(spec, value), spec);
		}
		
		public Class<? extends C> getClazz() {
			return clazz;
		}
	}
	
	private static <V> void reg(
	  Class<V> clazz, IValueSpecAdapter<V, V> adapter
	) {
		reg(clazz, Function.identity(), adapter);
	}
	
	private static <V extends Comparable<V>, CC, C extends CC> void regRanged(
	  Class<C> clazz, Function<CC, V> transform, IRangedValueSpecAdapter<V, CC> adapter
	) {
		reg(clazz, transform, (ValueSpec s, V v) -> {
			RangedEntryBuilder<V, CC, ?, ?> builder = adapter.createBuilder(s, v);
			Pair<? extends C, ? extends C> p = tryGetRange(s, clazz);
			if (p != null) {
				V min = transform.apply(p.getLeft());
				V max = transform.apply(p.getRight());
				// Apparently, using Double.MIN_VALUE instead of Double.NEGATIVE_INFINITY or
				//   -Double.MAX_VALUE is a frequent enough mistake, so we just ignore invalid bounds
				if (min.compareTo(v) <= 0 && max.compareTo(v) >= 0) builder = builder.range(
				  min, transform.apply(p.getRight()));
			}
			
			return builder;
		});
	}
	
	private static final List<ValueSpecAdapter<?, ?>> ADAPTERS = new ArrayList<>();
	private static <V, CC, C extends CC> void reg(
	  Class<C> clazz, Function<CC, V> transform, IValueSpecAdapter<V, CC> adapter
	) {
		ADAPTERS.add(new ValueSpecAdapter<>(clazz, transform, adapter));
	}
	
	static {
		reg(Boolean.class, (s, v) -> bool(false));
		regRanged(Byte.class, Number::byteValue, (s, v) -> number(v));
		regRanged(Short.class, Number::shortValue, (s, v) -> number(v));
		regRanged(Integer.class, Number::intValue, (s, v) -> number(v));
		regRanged(Long.class, Number::longValue, (s, v) -> number(v));
		regRanged(Float.class, Number::floatValue, (s, v) -> number(v));
		regRanged(Double.class, Number::doubleValue, (s, v) -> number(v));
		reg(String.class, (s, v) -> string(v));
		//noinspection unchecked
		reg(Enum.class, (s, v) -> option(v));
		//noinspection unchecked
		reg((Class<List<?>>) (Class<?>) List.class, Function.identity(), (s, v) -> {
			Object defValue = s.getDefault();
			if (!(defValue instanceof List<?> def)) return null;
			//noinspection unchecked
			return (ConfigEntryBuilder<List<?>, List<?>, ?, ?>) (ConfigEntryBuilder<?, ?, ?, ?>)
			  guessListType(def, s::test);
		});
	}
	
	private static ListEntryBuilder<?, ?, ?, ?> guessListType(
	  List<?> defValue, Predicate<Object> validator
	) {
		ListEntryBuilder<?, ?, ?, ?> b;
		if (defValue.isEmpty()) {
			b = guessListTypeFromValidator(validator);
		} else {
			if (allInstance(defValue, Boolean.class)) {
				b = list(bool(false), castList(defValue));
			} else if (allInstance(defValue, Integer.class)) {
				b = list(number(!validator.test(singletonList(0))? 1 : 0), castList(defValue));
			} else if (allInstance(defValue, Long.class)) {
				b = list(number(!validator.test(singletonList(0L))? 1L : 0L), castList(defValue));
			} else if (allInstance(defValue, Float.class)) {
				b = list(number(!validator.test(singletonList(0F))? 1F : 0F), castList(defValue));
			} else if (allInstance(defValue, Double.class)) {
				b = list(number(!validator.test(singletonList(0D))? 1D : 0D), castList(defValue));
			} else if (allInstance(defValue, Byte.class)) {
				b = list(number(!validator.test(singletonList((byte) 0))? (byte) 1 : (byte) 0), castList(defValue));
			} else if (allInstance(defValue, Short.class)) {
				b = list(number(!validator.test(singletonList((short) 0))? (short) 1 : (short) 0), castList(defValue));
			} else if (allInstance(defValue, String.class)) {
				b = list(string(""), castList(defValue));
			} else if (allInstance(defValue, Enum.class)) {
				//noinspection unchecked,rawtypes
				b = list(option((Enum) defValue.get(0)), castList(defValue));
			} else if (allInstance(defValue, List.class)) {
				Predicate<Object> subValidator = o -> validator.test(singletonList(o));
				Optional<? extends List<?>> opt = defValue.stream()
				  .map(e -> (List<?>) e)
				  .filter(e -> !e.isEmpty())
				  .findFirst();
				ListEntryBuilder<?, ?, ?, ?> sub;
				if (opt.isPresent()) {
					sub = guessListType(opt.get(), subValidator);
				} else sub = guessListTypeFromValidator(subValidator);
				b = wrapSubList(sub, defValue);
			} else b = list(entry("", new YamlConfigSerializer()), defValue);
		}
		if (!validator.test(Lists.newArrayList()))
			b = b.minSize(1);
		return b;
	}
	
	@SuppressWarnings("unchecked") private static <
	  V, C, G, B extends ListEntryBuilder<V, C, G, B>
	> ListEntryBuilder<?, ?, ?, ?> wrapSubList(
	  ListEntryBuilder<?, ?, ?, ?> sub, List<?> subList
	) {
		return list((B) sub, (List<List<V>>) subList);
	}
	
	private static boolean allInstance(List<?> list, Class<?> clazz) {
		return list.stream().allMatch(clazz::isInstance);
	}
	
	@SuppressWarnings("unchecked") private static <T> List<T> castList(List<?> list) {
		return (List<T>) list;
	}
	
	private static ListEntryBuilder<?, ?, ?, ?> guessListTypeFromValidator(Predicate<Object> validator) {
		if (validator.test(singletonList("s"))) {
			return list(string(""));
		} else if (validator.test(singletonList(true))) {
			return list(bool(false));
		} else if (validator.test(singletonList(0))) {
			return list(number(0));
		} else if (validator.test(singletonList(1))) {
			return list(number(1));
		} else if (validator.test(singletonList(0L))) {
			return list(number(0L));
		} else if (validator.test(singletonList(1L))) {
			return list(number(1L));
		} else if (validator.test(singletonList(0F))) {
			return list(number(0F));
		} else if (validator.test(singletonList(1F))) {
			return list(number(1F));
		} else if (validator.test(singletonList(0D))) {
			return list(number(0D));
		} else if (validator.test(singletonList(1D))) {
			return list(number(1D));
		} else if (validator.test(singletonList((byte) 0))) {
			return list(number((byte) 0));
		} else if (validator.test(singletonList((byte) 1))) {
			return list(number((byte) 1));
		} else if (validator.test(singletonList((short) 0))) {
			return list(number((short) 0));
		} else if (validator.test(singletonList((short) 1))) {
			return list(number((short) 1));
		} else return list(entry("", new YamlConfigSerializer()));
	}
	
	public static class YamlConfigSerializer implements ConfigEntrySerializer<Object> {
		@Override public String serializeConfigEntry(Object value) {
			Yaml yaml = SimpleConfigCommentedYamlFormat.getDefaultYaml();
			try {
				return yaml.dumpAs(value, null, FlowStyle.FLOW);
			} catch (YAMLException e) {
				return "";
			}
		}
		
		@Override public Optional<Object> deserializeConfigEntry(String value) {
			Yaml yaml = SimpleConfigCommentedYamlFormat.getDefaultYaml();
			try {
				return Optional.of(yaml.load(value));
			} catch (YAMLException e) {
				return Optional.empty();
			}
		}
	}
	
	@Internal public static EnumMap<ModConfig.Type, ModConfig> getConfigs(ModContainer container) {
		return ObfuscationReflectionHelper.getPrivateValue(
		  ModContainer.class, container, "configs");
	}
}