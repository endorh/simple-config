package endorh.simpleconfig.core.wrap;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.google.common.collect.Lists;
import com.google.gson.internal.Primitives;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.api.*;
import endorh.simpleconfig.api.SimpleConfig.EditType;
import endorh.simpleconfig.api.SimpleConfig.Type;
import endorh.simpleconfig.api.entry.BooleanEntryBuilder;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons.MinecraftOptions;
import endorh.simpleconfig.core.*;
import endorh.simpleconfig.core.SimpleConfigBuilderImpl.ConfigValueBuilder;
import endorh.simpleconfig.core.wrap.MinecraftServerConfigWrapper.MinecraftGameRulesWrapperBuilder.MinecraftServerPropertyEntryDelegate;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.server.dedicated.DedicatedServerSettings;
import net.minecraft.server.dedicated.Settings;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.GameRules.IntegerValue;
import net.minecraft.world.level.GameRules.Key;
import net.minecraft.world.level.GameRules.Value;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static endorh.simpleconfig.api.ConfigBuilderFactoryProxy.*;
import static endorh.simpleconfig.api.SimpleConfigTextUtil.optSplitTtc;
import static endorh.simpleconfig.api.entry.RangedEntryBuilder.InvertibleDouble2DoubleFunction.*;

@EventBusSubscriber(bus=Bus.MOD, modid=SimpleConfigMod.MOD_ID)
public class MinecraftServerConfigWrapper {
	private static final Logger LOGGER = LogManager.getLogger();
	public static final String MINECRAFT_MOD_ID = "minecraft";
	public static final String EDIT_PROTECTED_PROPERTIES = "edit-protected-properties";
	private static SimpleConfigImpl config;
	private static @Nullable Consumer<MinecraftServer> binder = null;
	
	private static void wrapMinecraftGameRules() {
		try {
			MinecraftGameRulesWrapperBuilder builder = new MinecraftGameRulesWrapperBuilder();
			Pair<SimpleConfigImpl, Consumer<MinecraftServer>> pair = builder.build();
			config = pair.getLeft();
			binder = pair.getRight();
		} catch (RuntimeException e) {
			LOGGER.error("Error wrapping Minecraft server config", e);
		}
	}
	
	@SubscribeEvent
	public static void onLoadComplete(FMLLoadCompleteEvent event) {
		wrapMinecraftGameRules();
	}
	
	@EventBusSubscriber(value=Dist.DEDICATED_SERVER, modid=SimpleConfigMod.MOD_ID)
	public static class ServerEventSubscriber {
		@SubscribeEvent public static void onServerAboutToStart(ServerAboutToStartEvent event) {
			DedicatedServer server = (DedicatedServer) event.getServer();
			if (binder != null) binder.accept(server);
			// Add default value to file
			MinecraftServerPropertyEntryDelegate<Boolean> delegate = new MinecraftServerPropertyEntryDelegate<>(
			  EDIT_PROTECTED_PROPERTIES, boolean.class, false, null);
			delegate.bind(server);
			delegate.setValue(delegate.getValue());
			if (!delegate.getValue())
				removeProtectedProperties();
		}
	}
	
	@OnlyIn(Dist.DEDICATED_SERVER)
	public static boolean areProtectedPropertiesEditable() {
		if (config != null) return config.hasChild("properties.protected");
		DedicatedServer server = (DedicatedServer) ServerLifecycleHooks.getCurrentServer();
		DedicatedServerProperties properties = server.getProperties();
		Method Settings$get = ObfuscationReflectionHelper.findMethod(
		  Settings.class, "m_139836_", String.class, boolean.class);
		Settings$get.setAccessible(true);
		try {
			return (boolean) Settings$get.invoke(
			  properties, EDIT_PROTECTED_PROPERTIES, false);
		} catch (IllegalAccessException | InvocationTargetException ignored) {
			return false;
		}
	}
	
	@OnlyIn(Dist.DEDICATED_SERVER)
	private static void removeProtectedProperties() {
		config.removeChild("properties.protected");
	}
	
	public static class MinecraftGameRulesWrapperBuilder {
		private final SimpleConfigBuilderImpl builder =
		  (SimpleConfigBuilderImpl) config(MINECRAFT_MOD_ID, Type.SERVER);
		private ConfigEntryHolderBuilder<?> target = builder;
		private boolean caption = false;
		private final MinecraftGameRuleConfigValueBuilder
		  vb = new MinecraftGameRuleConfigValueBuilder();
		private final List<MinecraftServerPropertyEntryDelegate<?>> delegates = Lists.newArrayList();
		
		public Pair<SimpleConfigImpl, @Nullable Consumer<MinecraftServer>> build() {
			try {
				with(
				  category("gamerule").withIcon(MinecraftOptions.GAMERULES).withColor(0x804242FF),
				  this::addGameRuleEntries);
				with(
				  category("properties").withIcon(
				    MinecraftOptions.PROPERTIES).withColor(0x8042FF42),
				  this::addServerPropertiesEntries);
				DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> builder.withGUIDecorator((s, b) -> {
					if (!SimpleConfigNetworkHandler.isConnectedToDedicatedServer()) {
						b.removeCategory("properties", EditType.SERVER);
					} else {
						SimpleConfigImpl config = (SimpleConfigImpl) s;
						b.getOrCreateCategory("properties", EditType.SERVER).setLoadingFuture(
						  SimpleConfigNetworkHandler.requestServerProperties().handle((p, t) -> {
							  if (p != null) {
								  CommentedConfig c = p.getRight();
								  if (c != null) {
									  boolean protectedProperties = p.getLeft();
									  config.loadSnapshot(
									    c, false, false, pp -> pp.startsWith("properties."));
									  config.loadSnapshot(
									    c, true, false, true, pp -> pp.startsWith("properties."));
									  return cc -> {
										  cc.finishLoadingEntries();
										  cc.removeEntry(protectedProperties? "protected-disclaimer" : "protected");
										  return true;
									  };
								  }
							  }
							  return cc -> false;
						  }));
					}
				}));
				SimpleConfigImpl config = builder.buildAndRegister(null, vb);
				return Pair.of(
				  config, FMLEnvironment.dist == Dist.DEDICATED_SERVER
				          ? s -> delegates.forEach(d -> d.bind((DedicatedServer) s))
				          : null);
			} catch (RuntimeException e) {
				e.printStackTrace();
				throw e;
			}
		}
		
		private static final Map<GameRules.Key<?>, GameRules.Type<?>> GAME_RULE_TYPES =
		  ObfuscationReflectionHelper.getPrivateValue(GameRules.class, null, "f_46129_");
		private static final Map<GameRules.Key<IntegerValue>, ConfigEntryBuilder<Integer, ?, ?, ?>> OVERRIDES = Util.make(new HashMap<>(), m -> {
			m.put(GameRules.RULE_MAX_ENTITY_CRAMMING, number(24).sliderRange(1, 256).sliderMap(pow(2)));
			m.put(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE, percent(100));
			m.put(GameRules.RULE_RANDOMTICKING, number(3, 0, 3000).sliderMap(expMap(8)));
			m.put(GameRules.RULE_SPAWN_RADIUS, number(10, 0, 64).sliderMap(pow(2)));
		});
		private void addGameRuleEntries() {
			if (GAME_RULE_TYPES == null) throw new IllegalStateException(
			  "Cannot access GameRules#GAME_RULE_TYPES");
			GAME_RULE_TYPES.forEach((k, t) -> {
				Value<?> rule = t.createRule();
				if (rule instanceof GameRules.BooleanValue v) {
					boolean defValue = v.get();
					add(k, yesNo(defValue));
				} else if (rule instanceof GameRules.IntegerValue v) {
					int defValue = v.get();
					ConfigEntryBuilder<Integer, ?, ?, ?> override = OVERRIDES.get(k);
					add(k, override != null? override.withValue(defValue) : number(defValue));
				}
			});
		}
		
		private void addServerPropertiesEntries() {
			addFlag("enable-status", yesNo(true));
			add("motd", string("A Minecraft Server"), (s, m) -> {
				s.setMotd(m);
				s.getStatus().setDescription(new TextComponent(m));
			});
			addFlag("hide-online-players", yesNo(false));
			
			add("view-distance", number(10).sliderRange(1, 64).sliderMap(expMap(2)), (s, d) -> s.getPlayerList().setViewDistance(d));
			add("simulation-distance", number(10).sliderRange(1, 64).sliderMap(expMap(2)), (s, d) -> s.getPlayerList().setSimulationDistance(d));
			addFlag("entity-broadcast-range-percentage", percent(100).range(10, 1000).sliderMap(expMap(6)));
			
			add("pvp", yesNo(true), MinecraftServer::setPvpAllowed);
			addFlag("spawn-animals", yesNo(true));
			addFlag("spawn-npcs", yesNo(true));
			addFlag("spawn-monsters", yesNo(true));
			addFlag("allow-nether", yesNo(true));
			add("allow-flight", yesNo(false), MinecraftServer::setFlightAllowed);
			addFlag("enable-command-block", yesNo(false));
			addFlag("difficulty", option(Difficulty.EASY));
			addFlag("gamemode", option(GameType.SURVIVAL));
			addFlag("force-gamemode", yesNo(false));
			add("hardcore", yesNo(false));
			add("level-name", string("world"));
			// announcePlayerAchievements is a legacy setting
			
			addFlag("spawn-protection", number(16).slider("options.chunks").sliderRange(0, 64).sliderMap(expMap(2)));
			addFlag("op-permission-level", number(4).sliderRange(1, 4));
			add("function-permission-level", number(2).sliderRange(1, 4));
			wrap("white-list", yesNo(false), (d, b) -> d.getPlayerList().setUsingWhiteList(b));
			add("enforce-whitelist", yesNo(false));
			add("enforce-secure-profile", yesNo(true));
			
			add("max-players", number(20).sliderRange(1, 1000).sliderMap(expMap(4)));
			add("max-world-size", number(29999984).range(1, 29999984));
			add("max-tick-time", number(TimeUnit.MINUTES.toMillis(1L)));
			wrap("player-idle-timeout", number(0).sliderRange(0, 120).sliderMap(sqrt()), DedicatedServer::setPlayerIdleTimeout);
			add("max-chained-neighbor-updates", number(1000000));
			add("rate-limit", number(0));
			add("network-compression-threshold", number(256));
			
			add("use-native-transport", yesNo(true));
			add("sync-chunk-writes", yesNo(true));
			
			with(group("world-gen"), () -> {
				add("level-seed", string(""));
				add("generate-structures", yesNo(true));
				add("level-type", string("default").suggest(
				  "default", "flat", "large_biomes", "amplified",
				  "single_biome", "debug_all_bloock_states"
				));
				add("generator-settings", string("{}"));
			});
			
			with(group("resource-pack"), true, () -> {
				addFlag("resource-pack", string(""));
				addFlag("resource-pack-sha1", string(""));
				addFlag("require-resource-pack", yesNo(false));
				addFlag("resource-pack-prompt", string(""));
			});
			
			with(group("protected"), () -> {
				addProtected(EDIT_PROTECTED_PROPERTIES, enable(false));
				addProtected("online-mode", yesNo(true));
				addProtected("prevent-proxy-connections", yesNo(false));
				
				addProtected("server-ip", string(""));
				addProtected("server-port", number(25565));
				
				addProtected("enable-query", yesNo(false));
				addProtected("query.port", number(25565));
				addProtected("enable-rcon", yesNo(false));
				addProtected("rcon.port", number(25575));
				addProtected("rcon.password", string(""));
				
				addProtected("broadcast-rcon-to-ops", yesNo(true));
				addProtected("broadcast-console-to-ops", yesNo(true));
				
				addProtected("enable-jmx-monitoring", yesNo(false));
				
				addProtected("text-filtering-config", string(""));
			});
			target.text("protected-disclaimer");
		}
		
		private void with(ConfigCategoryBuilder builder, Runnable runnable) {
			ConfigEntryHolderBuilder<?> prev = target;
			if (prev != this.builder) throw new IllegalStateException(
			  "Categories must be declared at root level");
			target = builder;
			runnable.run();
			this.builder.n(builder);
			target = prev;
		}
		
		private void with(ConfigGroupBuilder builder, Runnable runnable) {
			with(builder, false, runnable);
		}
		
		private void with(ConfigGroupBuilder builder, boolean caption, Runnable runnable) {
			ConfigEntryHolderBuilder<?> prev = target;
			this.caption = caption;
			target = builder;
			runnable.run();
			prev.n(builder);
			target = prev;
			this.caption = false;
		}
		
		private void addEntry(String name, ConfigEntryBuilder<?, ?, ?, ?> entryBuilder) {
			if (caption) {
				if (target instanceof ConfigGroupBuilder group) {
					group.caption(name, castAtom(entryBuilder));
					caption = false;
				} else throw new IllegalStateException(
				  "Cannot add caption outside a group: " + name);
			} else target.add(name, entryBuilder);
		}
		
		@SuppressWarnings("unchecked")
		private <T extends ConfigEntryBuilder<?, ?, ?, ?> & AtomicEntryBuilder> T castAtom(
		  ConfigEntryBuilder<?, ?, ?, ?> entryBuilder
		) {
			if (!(entryBuilder instanceof AtomicEntryBuilder)) throw new IllegalArgumentException(
			  "Entry builder is not atomic: " + entryBuilder.getClass().getCanonicalName());
			return (T) entryBuilder;
		}
		
		private <T> void add(String name, ConfigEntryBuilder<T, ?, ?, ?> entryBuilder) {
			add(name, entryBuilder, null);
		}
		
		private <T> void addProtected(String name, ConfigEntryBuilder<T, ?, ?, ?> entryBuilder) {
			add(name, entryBuilder, null);
		}
		
		private <T> void addFlag(
		  String name, ConfigEntryBuilder<T, ?, ?, ?> entryBuilder
		) {
			doAdd(name, entryBuilder, null);
		}
		
		private <T> void add(
		  String name, ConfigEntryBuilder<T, ?, ?, ?> entryBuilder,
		  @Nullable BiConsumer<DedicatedServer, T> applier
		) {
			if (applier == null) entryBuilder = entryBuilder.restart();
			doAdd(name, entryBuilder, applier != null? (s, t) -> {
				applier.accept(s, t);
				return true;
			} : null);
		}
		
		private <T> void wrap(
		  String name, ConfigEntryBuilder<T, ?, ?, ?> entryBuilder,
		  @Nullable BiConsumer<DedicatedServer, T> applier
		) {
			if (applier == null) entryBuilder = entryBuilder.restart();
			doAdd(name, entryBuilder, applier != null? (s, t) -> {
				applier.accept(s, t);
				return false;
			} : null);
		}
		
		private <T> void doAdd(
		  String name, ConfigEntryBuilder<T, ?, ?, ?> entryBuilder,
		  @Nullable BiFunction<DedicatedServer, T, Boolean> applier
		) {
			AbstractConfigEntryBuilder<T, ?, ?, ?, ?, ?> b = cast(entryBuilder);
			MinecraftServerPropertyEntryDelegate<T> delegate;
			Class<?> typeClass = b.getTypeClass();
			if (typeClass == Boolean.class || typeClass == Integer.class) {
				delegate = new MinecraftServerPropertyEntryDelegate<>(name, typeClass, b.getValue(), applier);
			} else if (typeClass == String.class) {
				//noinspection unchecked
				delegate = new MinecraftServerPropertyEntryDelegate<>(
				  name, s -> (T) s, t -> (String) t, b.getValue(), applier);
			} else if (typeClass == Long.class) {
				delegate = new MinecraftServerPropertyEntryDelegate<>(
				  name, wrapNumberDeserializer(Long::parseLong), Object::toString,
				  b.getValue(), applier);
			} else if (Enum.class.isAssignableFrom(typeClass)) {
				//noinspection unchecked
				delegate = new MinecraftServerPropertyEntryDelegate<>(
				  name, s -> Arrays.stream(typeClass.getEnumConstants()).map(e -> (Enum<?>) e)
				  .filter(e -> e.name().equalsIgnoreCase(s))
				  .map(e -> (T) e).findFirst().orElse(null), t -> ((Enum<?>) t).name(),
				  b.getValue(), applier);
			} else throw new IllegalArgumentException(
			  "Unsupported server property type: " + typeClass);
			delegates.add(delegate);
			addEntry(name.replace('.', '-'), b.withDelegate(delegate));
		}
		
		@SuppressWarnings("unchecked")
		private static <T, V extends Number> Function<String, T> wrapNumberDeserializer(Function<String, V> deserializer) {
			return s -> {
				try {
					return (T) deserializer.apply(s);
				} catch (NumberFormatException numberformatexception) {
					return (T) null;
				}
			};
		}
		
		@SuppressWarnings("unchecked") private void add(Key<?> key, BooleanEntryBuilder entryBuilder) {
			AbstractConfigEntryBuilder<Boolean, ?, ?, ?, ?, ?> b = patch(key, entryBuilder);
			addEntry(
			  key.getId(),
			  b.withDelegate(MinecraftGameRuleEntryDelegate.bool((Key<BooleanValue>) key)));
		}
		
		@SuppressWarnings("unchecked") private void add(Key<?> key, ConfigEntryBuilder<Integer, ?, ?, ?> entryBuilder) {
			AbstractConfigEntryBuilder<Integer, ?, ?, ?, ?, ?> b = patch(key, entryBuilder);
			addEntry(
			  key.getId(),
			  b.withDelegate(MinecraftGameRuleEntryDelegate.integer((Key<GameRules.IntegerValue>) key)));
		}
		
		private <T> AbstractConfigEntryBuilder<T, ?, ?, ?, ?, ?> patch(
		  Key<?> key, ConfigEntryBuilder<T, ?, ?, ?> entryBuilder
		) {
			entryBuilder = entryBuilder.tooltip(() -> Stream.concat(
			  optSplitTtc(key.getDescriptionId() + ".description").stream(),
			  Stream.of(new TextComponent("/gamerule " + key.getId()).withStyle(ChatFormatting.GRAY))
			).collect(Collectors.toList()));
			AbstractConfigEntryBuilder<T, ?, ?, ?, ?, ?> b = cast(entryBuilder);
			return b.translation(key.getDescriptionId());
		}
		
		@SuppressWarnings("unchecked") private static <T> AbstractConfigEntryBuilder<T, ?, ?, ?, ?, ?> cast(
		  ConfigEntryBuilder<T, ?, ?, ?> entryBuilder
		) {
			return (AbstractConfigEntryBuilder<T, ?, ?, ?, ?, ?>) entryBuilder;
		}
		
		public static class MinecraftGameRuleEntryDelegate<T, V extends GameRules.Value<V>>
		  implements ConfigEntryDelegate<T> {
			public static MinecraftGameRuleEntryDelegate<Boolean, GameRules.BooleanValue> bool(
			  GameRules.Key<GameRules.BooleanValue> key
			) {
				return new MinecraftGameRuleEntryDelegate<>(
				  key, BooleanValue::get,
				  (v, t) -> v.set(t, ServerLifecycleHooks.getCurrentServer()));
			}
			
			public static MinecraftGameRuleEntryDelegate<Integer, GameRules.IntegerValue> integer(
			  GameRules.Key<GameRules.IntegerValue> key
			) {
				return new MinecraftGameRuleEntryDelegate<>(
				  key, GameRules.IntegerValue::get,
				  (v, t) -> v.set(t, ServerLifecycleHooks.getCurrentServer()));
			}
			
			private final GameRules.Key<V> key;
			private final Function<V, T> getter;
			private final BiConsumer<V, T> setter;
			private T value;
			
			public MinecraftGameRuleEntryDelegate(
			  Key<V> key, Function<V, T> getter, BiConsumer<V, T> setter
			) {
				this.key = key;
				this.getter = getter;
				this.setter = setter;
			}
			
			@Override public T getValue() {
				return DistExecutor.unsafeRunForDist(() -> () -> value, () -> () -> {
					GameRules rules = ServerLifecycleHooks.getCurrentServer().getGameRules();
					return getter.apply(rules.getRule(key));
				});
			}
			
			@Override public void setValue(T value) {
				DistExecutor.unsafeRunForDist(() -> () -> {
					this.value = value;
					return null;
				}, () -> () -> {
					if (Objects.equals(getValue(), value)) return null;
					MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
					GameRules rules = server.getGameRules();
					V rule = rules.getRule(key);
					setter.accept(rule, value);
					return null;
				});
			}
		}
		
		public static class MinecraftServerPropertyEntryDelegate<T> implements ConfigEntryDelegate<T> {
			private DedicatedServer server = null;
			private DedicatedServerSettings settings = null;
			private Method Settings$getMutable = null;
			private final String name;
			private final @Nullable Class<?> type;
			private final @Nullable Function<String, T> deserializer;
			private final @Nullable Function<T, String> serializer;
			private final T defValue;
			private final BiFunction<DedicatedServer, T, Boolean> applier;
			private T value;
			
			public MinecraftServerPropertyEntryDelegate(
			  String name, @NotNull Class<?> type, T defValue,
			  @Nullable BiFunction<DedicatedServer, T, Boolean> applier
			) {
				this.name = name;
				this.type = type;
				deserializer = null;
				serializer = null;
				this.defValue = defValue;
				this.applier = applier;
				value = defValue;
			}
			
			public MinecraftServerPropertyEntryDelegate(
			  String name, @NotNull Function<String, T> deserializer,
			  @NotNull Function<T, String> serializer,
			  T defValue, @Nullable BiFunction<DedicatedServer, T, Boolean> applier
			) {
				this.name = name;
				this.deserializer = deserializer;
				this.serializer = serializer;
				type = null;
				this.defValue = defValue;
				this.applier = applier;
				value = defValue;
			}
			
			@OnlyIn(Dist.DEDICATED_SERVER)
			public void bind(DedicatedServer server) {
				this.server = server;
				settings = ObfuscationReflectionHelper.getPrivateValue(
				  DedicatedServer.class, server, "f_139604_");
			}
			
			@Override public T getValue() {
				return server != null? getAccessor().get() : value;
			}
			
			@Override public void setValue(T value) {
				if (server != null) {
					if (Objects.equals(getValue(), value)) return;
					if (applier == null || applier.apply(server, value))
						settings.update(p -> getAccessor().update(server.registryAccess(), value));
				} else this.value = value;
			}
			
			@SuppressWarnings("unchecked") private Settings<DedicatedServerProperties>.MutableValue<T> getAccessor() {
				if (Settings$getMutable == null) {
					Settings$getMutable = type == null? ObfuscationReflectionHelper.findMethod(
					  Settings.class, "m_139868_",
					  String.class, Function.class, Function.class, Object.class
					) :  ObfuscationReflectionHelper.findMethod(
					  Settings.class, "m_139873_",
					  String.class, Primitives.unwrap(type));
					Settings$getMutable.setAccessible(true);
				}
				try {
					return (Settings<DedicatedServerProperties>.MutableValue<T>)
					  (type == null? Settings$getMutable.invoke(settings.getProperties(), name, deserializer, serializer, defValue)
					               : Settings$getMutable.invoke(settings.getProperties(), name, defValue));
				} catch (IllegalAccessException | InvocationTargetException e) {
					throw new RuntimeException(e);
				}
			}
		}
		
		public static class MinecraftGameRuleConfigValueBuilder extends ConfigValueBuilder {
			private final ModContainer modContainer =
			  ModList.get().getModContainerById(MINECRAFT_MOD_ID)
				 .orElseThrow(() -> new IllegalStateException("Minecraft mod not found"));
			
			@Override public void buildModConfig(SimpleConfigImpl config) {
				config.build(modContainer, null);
			}
			
			@Override public void build(
			  AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?> entryBuilder,
			  AbstractConfigEntry<?, ?, ?> entry
			) {}
			
			@Override public ForgeConfigSpec build() {
				return null;
			}
		}
	}
}
