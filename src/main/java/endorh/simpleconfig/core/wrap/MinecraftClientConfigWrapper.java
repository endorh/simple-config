package endorh.simpleconfig.core.wrap;

import com.google.common.collect.Lists;
import com.google.common.primitives.Primitives;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.api.*;
import endorh.simpleconfig.api.SimpleConfig.EditType;
import endorh.simpleconfig.api.SimpleConfig.Type;
import endorh.simpleconfig.api.entry.BooleanEntryBuilder;
import endorh.simpleconfig.api.entry.DoubleEntryBuilder;
import endorh.simpleconfig.api.entry.OptionEntryBuilder;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons.MinecraftOptions;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractConfigEntryBuilder;
import endorh.simpleconfig.core.SimpleConfigBuilderImpl;
import endorh.simpleconfig.core.SimpleConfigBuilderImpl.ConfigValueBuilder;
import endorh.simpleconfig.core.SimpleConfigImpl;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import net.minecraft.Util;
import net.minecraft.Util.OS;
import net.minecraft.client.*;
import net.minecraft.client.CycleOption.OptionSetter;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.CycleButton.Builder;
import net.minecraft.client.gui.components.CycleButton.ValueListSupplier;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.KeyBindingMap;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper.UnableToFindMethodException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static endorh.simpleconfig.api.ConfigBuilderFactoryProxy.*;
import static net.minecraft.util.Mth.clamp;

@EventBusSubscriber(value=Dist.CLIENT, modid=SimpleConfigMod.MOD_ID, bus=Bus.MOD)
@OnlyIn(Dist.CLIENT)
public class MinecraftClientConfigWrapper {
	private static final Logger LOGGER = LogManager.getLogger();
	public static final String MINECRAFT_MODID = "minecraft";
	public static final String OPTIONS_TXT = "options.txt";
	private static final String OPEN_AL_PREFIX = "OpenAL Soft on ";
	
	private static void wrapMinecraftOptions() {
		try {
			MinecraftOptionsWrapperBuilder builder = new MinecraftOptionsWrapperBuilder();
			builder.build();
		} catch (RuntimeException e) {
			LOGGER.error("Error wrapping Minecraft Client config", e);
			throw e;
		}
	}
	
	@SubscribeEvent public static void onLoadComplete(FMLLoadCompleteEvent event) {
		event.enqueueWork(MinecraftClientConfigWrapper::wrapMinecraftOptions);
	}
	
	private static class MinecraftOptionsWrapperBuilder {
		private static final Map<String, String> SRG_NAMES = Util.make(new HashMap<>(), m -> {
			m.put("advancedItemTooltips", "f_92125_");
			m.put("fullscreenResolution", "f_92123_");
			m.put("glDebugVerbosity", "f_92035_");
			m.put("heldItemTooltips", "f_92130_");
			m.put("hideBundleTutorial", "f_168405_");
			m.put("hideServerAddress", "f_92124_");
			m.put("joinedFirstServer", "f_92031_");
			m.put("lang", "f_92075_");
			m.put("lastServer", "f_92066_");
			m.put("pauseOnLostFocus", "f_92126_");
			m.put("skipMultiplayerWarning", "f_92083_");
			m.put("skipRealms32bitWarning", "f_210816_");
			m.put("syncChunkWrites", "f_92076_");
			m.put("tutorialStep", "f_92030_");
			m.put("useNativeTransport", "f_92028_");
			m.put("overrideWidth", "f_92128_");
			m.put("overrideHeight", "f_92129_");
			m.put("modelParts", "f_92108_");
			m.put("sourceVolumes", "f_92109_");
		});
		
		// The Options instance is final and unique to the Minecraft instance
		private final Options options = Minecraft.getInstance().options;
		private final Options dummyOptions;
		private final SimpleConfigBuilderImpl builder =
			(SimpleConfigBuilderImpl) config(MINECRAFT_MODID, Type.CLIENT);
		private ConfigEntryHolderBuilder<?> target = builder;
		private boolean caption = false;
		private final MinecraftConfigValueBuilder vb = new MinecraftConfigValueBuilder();
		
		private MinecraftOptionsWrapperBuilder() {
			Options defOptions;
			try {
				File file = Files.createTempDirectory("dummy-options").toFile();
				defOptions = new Options(Minecraft.getInstance(), file);
				file.delete();
			} catch (IOException ignored) {
				LOGGER.error("Couldn't create dummy options file, using nul path");
				defOptions = new Options(Minecraft.getInstance(), new File("_nul"));
			}
			dummyOptions = defOptions;
			// Restore KeyMapping maps, removing the KeyMappings from the dummyOptions
			Pair<Map<String, KeyMapping>, KeyBindingMap> maps = getKeyMappingMaps();
			Map<String, KeyMapping> ALL = maps.getLeft();
			KeyBindingMap MAP = maps.getRight();
			Set<String> rebound = new HashSet<>();
			for (KeyMapping km: dummyOptions.keyMappings) {
				String name = km.getName();
				if (ALL.get(name) == km) rebound.add(name);
				MAP.removeKey(km);
			}
			for (KeyMapping km: options.keyMappings) {
				String name = km.getName();
				if (rebound.contains(name)) ALL.put(name, km);
			}
		}
		
		@SuppressWarnings("unchecked") private static Pair<Map<String, KeyMapping>, KeyBindingMap> getKeyMappingMaps() {
			Field ALL_field = ObfuscationReflectionHelper.findField(KeyMapping.class, "f_90809_");
			Field MAP_field = ObfuscationReflectionHelper.findField(KeyMapping.class, "f_90810_");
			try {
				return Pair.of((Map<String, KeyMapping>) ALL_field.get(null), (KeyBindingMap) MAP_field.get(null));
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
		
		private void addEntries() {
			with(category("controls").withIcon(MinecraftOptions.CONTROLS).withColor(0x808080F0), () -> {
				with(group("mouse", true), () -> {
					wrapDouble(Option.SENSITIVITY, (o, v) -> o.sensitivity = v);
					wrapBool(Option.INVERT_MOUSE);
					wrapDouble(Option.MOUSE_WHEEL_SENSITIVITY, (o, v) -> o.mouseWheelSensitivity = v);
					wrapBool(Option.DISCRETE_MOUSE_SCROLL);
					wrapBool(Option.TOUCHSCREEN);
					wrapBool(Option.RAW_MOUSE_INPUT);
				});
				wrapBool(Option.TOGGLE_CROUCH);
				wrapBool(Option.TOGGLE_SPRINT);
				wrapBool(Option.AUTO_JUMP);
				target.add("keyMappings", button(() -> {
					Minecraft mc = Minecraft.getInstance();
					mc.setScreen(new KeyBindsScreen(mc.screen, mc.options));
				}).label("simpleconfig.ui.open"));
			});
			with(category("graphics").withIcon(MinecraftOptions.GRAPHICS).withColor(0x8080F0A0), () -> {
				wrapDouble(Option.FOV, (o, v) -> o.fov = v);
				//noinspection unchecked
				wrapOption((CycleOption<Integer>) Option.GUI_SCALE);
				
				wrapDouble(Option.RENDER_DISTANCE, (o, v) -> o.renderDistance = v.intValue());
				wrapDouble(Option.SIMULATION_DISTANCE, (o, v) -> o.simulationDistance = v.intValue());
				wrapDouble(Option.ENTITY_DISTANCE_SCALING, (o, v) -> o.entityDistanceScaling = v.floatValue());
				
				wrapEnum(Option.GRAPHICS);
				wrapDouble(Option.GAMMA, (o, v) -> o.gamma = v);
				wrapEnum(Option.AMBIENT_OCCLUSION);
				wrapDouble(Option.BIOME_BLEND_RADIUS, (o, v) -> o.biomeBlendRadius = Mth.clamp(v.intValue(), 0, 7));
				wrapEnum(Option.RENDER_CLOUDS);
				wrapEnum(Option.PARTICLES);
				wrapBool(Option.ENTITY_SHADOWS);
				wrapBool(Option.VIEW_BOBBING);
				wrapEnum(Option.ATTACK_INDICATOR);
				wrapBool(Option.AUTOSAVE_INDICATOR);
				
				wrapDouble(Option.FRAMERATE_LIMIT, (o, v) -> o.framerateLimit = v.intValue());
				wrapBool(Option.ENABLE_VSYNC);
				wrapEnum(Option.PRIORITIZE_CHUNK_UPDATES);
				wrapDouble(Option.MIPMAP_LEVELS, (o, v) -> o.mipmapLevels = v.intValue());
				
				wrapDouble(Option.FOV_EFFECTS_SCALE, (o, v) -> o.fovEffectScale = Mth.sqrt(v.floatValue()));
				wrapDouble(Option.SCREEN_EFFECTS_SCALE, (o, v) -> o.screenEffectScale = Mth.sqrt(v.floatValue()));
				
				wrapBool(Option.USE_FULLSCREEN);
				wrapString("fullscreenResolution", "");
				wrapInt("overrideWidth", 0);
				wrapInt("overrideHeight", 0);
			});
			with(category("sound").withIcon(MinecraftOptions.SOUND).withColor(0x80F04280), () -> {
				with(group("volume", true), true, () -> {
					Object2FloatMap<SoundSource> sourceVolumes =
					  ObfuscationReflectionHelper.getPrivateValue(
					    Options.class, options, SRG_NAMES.get("sourceVolumes"));
					for (SoundSource source: SoundSource.values())
						wrapMapValue(
						  source.getName(), "soundCategory." + source.getName(),
						  sourceVolumes, source, volume(1F));
				});
				add(Option.AUDIO_DEVICE, entry(
				  "", s -> s.equals("\"\"")? "" :
				           s.startsWith(OPEN_AL_PREFIX)? s.substring(OPEN_AL_PREFIX.length()) : s,
				  s -> Minecraft.getInstance().getSoundManager().getAvailableSoundDevices().stream()
				  .filter(d -> d.equals(s) || d.equals(OPEN_AL_PREFIX + s)).findFirst().or(() -> Optional.of(s))
				).suggest(() -> Minecraft.getInstance().getSoundManager().getAvailableSoundDevices()));
				wrapBool(Option.SHOW_SUBTITLES);
			});
			with(category("chat").withIcon(MinecraftOptions.CHAT).withColor(0x8090F080), () -> {
				wrapEnum(Option.CHAT_VISIBILITY);
				wrapDouble(Option.CHAT_OPACITY, (o, v) -> o.chatOpacity = v);
				wrapDouble(Option.TEXT_BACKGROUND_OPACITY, (o, v) -> o.textBackgroundOpacity = v);
				wrapBool(Option.TEXT_BACKGROUND);
				wrapDouble(Option.CHAT_WIDTH, (o, v) -> o.chatWidth = v);
				wrapDouble(Option.CHAT_HEIGHT_FOCUSED, (o, v) -> o.chatHeightFocused = v);
				wrapDouble(Option.CHAT_HEIGHT_UNFOCUSED, (o, v) -> o.chatHeightUnfocused = v);
				wrapDouble(Option.CHAT_LINE_SPACING, (o, v) -> o.chatLineSpacing = v);
				wrapDouble(Option.CHAT_SCALE, (o, v) -> o.chatScale = v);
				wrapBool(Option.CHAT_COLOR);
				
				wrapDouble(Option.CHAT_DELAY, (o, v) -> o.chatDelay = v);
				wrapBool(Option.AUTO_SUGGESTIONS);
				
				wrapBool(Option.CHAT_LINKS);
				wrapBool(Option.CHAT_LINKS_PROMPT);
				wrapBool(Option.HIDE_MATCHED_NAMES);
			});
			with(category("skin").withIcon(MinecraftOptions.SKIN).withColor(0x80E0E080), () -> {
				wrapEnum(Option.MAIN_HAND);
				with(group("modelPart", true), () -> {
					Set<PlayerModelPart> modelParts = ObfuscationReflectionHelper.getPrivateValue(
					  Options.class, options, SRG_NAMES.get("modelParts"));
					for (PlayerModelPart part: PlayerModelPart.values()) wrapSetBool(
					  part.getId(), "options.modelPart." + part.getId(),
					  modelParts, part, true);
				});
			});
			with(category("language").withIcon(MinecraftOptions.LANGUAGE).withColor(0x80E042E0), () -> {
				wrapString("lang", "en_us", () -> Minecraft.getInstance()
				  .getLanguageManager().getLanguages().stream()
				  .map(LanguageInfo::getCode).collect(Collectors.toList()));
				wrapBool(Option.FORCE_UNICODE_FONT);
			});
			with(category("online").withIcon(MinecraftOptions.ONLINE).withColor(0x80F0A080), () -> {
				wrapBool(Option.ALLOW_SERVER_LISTING);
				wrapBool(Option.REALMS_NOTIFICATIONS);
			});
			with(category("accessibility").withIcon(
			  MinecraftOptions.ACCESSIBILITY).withColor(0x80E0E0FF), () -> {
				wrapEnum(Option.NARRATOR);
				wrapBool(Option.HIDE_LIGHTNING_FLASH);
				wrapBool(Option.DARK_MOJANG_STUDIOS_BACKGROUND_COLOR);
			});
			with(category("advanced").withIcon(MinecraftOptions.ADVANCED), () -> {
				wrapBool(Option.REDUCED_DEBUG_INFO);
				wrapBool("pauseOnLostFocus", true);
				wrapBool("advancedItemTooltips", false);
				wrapBool("heldItemTooltips", true);
				wrapBool("hideServerAddress", false);
				wrapBool("syncChunkWrites", Util.getPlatform() == OS.WINDOWS);
				wrapBool("useNativeTransport", true);
				wrapInt("glDebugVerbosity", 1);
				wrapEnum("tutorialStep", TutorialSteps.MOVEMENT);
				wrapBool("hideBundleTutorial", false);
				wrapBool("skipMultiplayerWarning", false);
				wrapBool("skipRealms32bitWarning", false);
				wrapString("lastServer", "");
				wrapBool("joinedFirstServer", false);
			});
		}
		
		public SimpleConfig build() {
			try {
				addEntries();
				builder.withGUIDecorator((s, b) -> b.getCategories(EditType.CLIENT).forEach(
				  c -> c.setContainingFile(new File(
					 Minecraft.getInstance().gameDirectory, OPTIONS_TXT
				  ).toPath().normalize())));
				return builder.buildAndRegister(null, vb);
			} catch (RuntimeException e) {
				e.printStackTrace();
				throw e;
			}
		}
		
		private void with(ConfigCategoryBuilder builder, Runnable runnable) {
			ConfigEntryHolderBuilder<?> prev = target;
			if (prev != this.builder) throw new IllegalStateException(
			  "Categories must be declared at root level");
			caption = false;
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
		
		private void wrapDouble(ProgressOption opt, BiConsumer<Options, Double> effectLessSetter) {
			double min = opt.getMinValue();
			double max = opt.getMaxValue();
			DoubleEntryBuilder b = number(getInitialValue(opt), min, max)
			  .slider(getSliderLabelProvider(opt, effectLessSetter))
			  .sliderMap(
				 d -> clamp((opt.toValue(d) - min) / (max - min), 0F, 1F),
				 d -> opt.toPct(clamp(min + d * (max - min), min, max)));
			add(opt, b);
		}
		
		private <E extends Enum<E>> void wrapEnum(CycleOption<E> opt) {
			E initial = getInitialValue(opt);
			Function<E, Component> displayer = getDisplayName(opt);
			if (displayer != null) {
				List<E> values = Arrays.stream(initial.getDeclaringClass().getEnumConstants()).toList();
				add(opt, option(initial, values).withDisplay(displayer));
			} else {
				add(opt, option(initial));
			}
		}
		
		private <T> void wrapOption(CycleOption<T> opt) {
			OptionEntryBuilder<T> b = option(getInitialValue(opt), getValues(opt));
			add(opt, b);
		}
		
		private void wrapBool(CycleOption<Boolean> opt) {
			BooleanEntryBuilder b = onOff(getInitialValue(opt));
			add(opt, b);
		}
		
		private static <T> Supplier<List<T>> getValues(CycleOption<T> option) {
			CycleButton.Builder<T> button = getButtonSetup(option).get();
			ValueListSupplier<T> values = getValues(button);
			return values::getSelectedList;
		}
		
		private static <T> CycleButton.ValueListSupplier<T> getValues(CycleButton.Builder<T> builder) {
			return ObfuscationReflectionHelper.getPrivateValue(Builder.class, builder, "f_168925_");
		}
		
		private void wrapBool(String name, boolean def) {
			add(name, onOff(def));
		}
		
		private void wrapInt(String name, int def) {
			add(name, number(def));
		}
		
		private void wrapString(String name, String def) {
			add(name, string(def));
		}
		
		private void wrapString(String name, String def, Supplier<List<String>> suggestions) {
			add(name, string(def).suggest(suggestions));
		}
		
		private <V extends Enum<V>> void wrapEnum(String name, V val) {
			add(name, option(val));
		}
		
		private <V> void wrapSetBool(String name, Set<V> set, V value, boolean defValue) {
			wrapSetBool(name, null, set, value, defValue);
		}
		
		private <V> void wrapSetBool(
		  String name, @Nullable String translation, Set<V> set, V value, boolean defValue
		) {
			AbstractConfigEntryBuilder<Boolean, ?, ?, ?, ?, ?> b = cast(bool(defValue));
			b = b.withDelegate(new MinecraftSetBoolDelegate<>(set, value));
			if (translation != null) b = b.translation(translation);
			addEntry(name, b);
		}
		
		private <K, V> void wrapMapValue(
		  String name,  Map<K, V> map, K key, ConfigEntryBuilder<V, ?, ?, ?> entryBuilder
		) {
			wrapMapValue(name, null, map, key, entryBuilder);
		}
		
		private <K, V> void wrapMapValue(
		  String name, @Nullable String translation, Map<K, V> map, K key,
		  ConfigEntryBuilder<V, ?, ?, ?> entryBuilder
		) {
			AbstractConfigEntryBuilder<V, ?, ?, ?, ?, ?> b = cast(entryBuilder);
			b = b.withDelegate(new MinecraftMapValueDelegate<>(map, key, b.getValue()));
			if (translation != null) b = b.translation(translation);
			addEntry(name, b);
		}
		
		private <V> void add(String name, ConfigEntryBuilder<V, ?, ?, ?> entryBuilder) {
			String srgName = SRG_NAMES.get(name);
			if (srgName == null) throw new IllegalArgumentException(
			  "No SRG name for Minecraft option field: " + name);
			Field field = ObfuscationReflectionHelper.findField(Options.class, srgName);
			field.setAccessible(true);
			if (Modifier.isStatic(field.getModifiers())) throw new IllegalArgumentException(
			  "Minecraft option field cannot be static: " + name);
			if (!Primitives.wrap(field.getType()).isInstance(entryBuilder.getValue()))
				throw new IllegalArgumentException(
				  "Minecraft option field type does not match: " + name + ": " + field.getType() +
				  " </- " + entryBuilder.getValue().getClass());
			AbstractConfigEntryBuilder<V, ?, ?, ?, ?, ?> b = cast(entryBuilder);
			b = b.withDelegate(new MinecraftFieldOptionEntryDelegate<>(field, entryBuilder.getValue()));
			addEntry(name, b);
		}
		
		private void add(ProgressOption opt, ConfigEntryBuilder<Double, ?, ?, ?> entryBuilder) {
			Function<Minecraft, List<FormattedCharSequence>> tooltip = getTooltip(opt);
			entryBuilder = entryBuilder.tooltip(
			  () -> tooltip.apply(Minecraft.getInstance()).stream()
			    .map(SimpleConfigTextUtil::asComponent).collect(Collectors.toList()));
			AbstractConfigEntryBuilder<Double, ?, ?, ?, ?, ?> b = cast(entryBuilder);
			b = b.translation(getName(opt))
			  .withDelegate(new MinecraftProgressOptionEntryDelegate(opt));
			addEntry(getID(opt), b);
		}
		
		private <V> void add(CycleOption<V> opt, ConfigEntryBuilder<V, ?, ?, ?> entryBuilder) {
			Function<Minecraft, CycleButton.TooltipSupplier<V>> tooltip = getTooltip(opt);
			entryBuilder = entryBuilder.tooltip(
			  v -> tooltip.apply(Minecraft.getInstance()).apply(v).stream()
			    .map(SimpleConfigTextUtil::asComponent).collect(Collectors.toList()));
			AbstractConfigEntryBuilder<V, ?, ?, ?, ?, ?> b = cast(entryBuilder);
			b = b.translation(getName(opt)).withDelegate(new MinecraftCycleOptionEntryDelegate<>(opt));
			addEntry(getID(opt), b);
		}
		
		private void addEntry(String name, ConfigEntryBuilder<?, ?, ?, ?> entryBuilder) {
			try {
				if (caption) {
					if (target instanceof ConfigGroupBuilder group) {
						group.caption(name, castAtom(entryBuilder));
						caption = false;
					} else throw new IllegalStateException(
						"Cannot add caption outside a group: " + name);
				} else target.add(name, entryBuilder);
			} catch (RuntimeException e) {
				LOGGER.error("Error wrapping Minecraft option: " + name, e);
			}
		}
		
		@SuppressWarnings("unchecked") private <T extends ConfigEntryBuilder<?, ?, ?, ?> & AtomicEntryBuilder> T castAtom(
		  ConfigEntryBuilder<?, ?, ?, ?> entryBuilder
		) {
			if (!(entryBuilder instanceof AtomicEntryBuilder)) throw new IllegalArgumentException(
			  "Entry builder is not atomic: " + entryBuilder.getClass().getCanonicalName());
			return (T) entryBuilder;
		}
		
		@SuppressWarnings("unchecked") private static <V> AbstractConfigEntryBuilder<V, ?, ?, ?, ?, ?> cast(
		  ConfigEntryBuilder<V, ?, ?, ?> builder
		) {
			return (AbstractConfigEntryBuilder<V, ?, ?, ?, ?, ?>) builder;
		}
		
		private static Component getCaption(Option opt) {
			return ObfuscationReflectionHelper.getPrivateValue(Option.class, opt, "f_91658_");
		}
		
		private static String getName(Option opt) {
			Component caption = getCaption(opt);
			return caption instanceof TranslatableComponent c? c.getKey() : caption.getString();
		}
		
		private static String getID(Option opt) {
			return getName(opt).replace("options.", "").replace('.', '_');
		}
		
		private static final List<String> keyMethodNames = Lists.newArrayList(
		  "m_90776_", "m_90511_", "m_120639_", "m_20829_", "m_35968_",
		  "m_90489_", "m_90666_", "m_19036_", "m_92195_", "m_91621_");
		private <E extends Enum<E>> @Nullable Function<E, Component> getDisplayName(CycleOption<E> opt) {
			E initial = getInitialValue(opt);
			if (initial != null) {
				Class<?> cls = initial.getClass();
				Method keyGetter = null;
				for (String name: keyMethodNames) try {
					keyGetter = ObfuscationReflectionHelper.findMethod(cls, name);
					break;
				} catch (UnableToFindMethodException ignored) {}
				if (keyGetter != null) {
					keyGetter.setAccessible(true);
					final Method kg = keyGetter;
					try {
						Object o = kg.invoke(initial);
						if (o instanceof Component) {
							return e -> {
								try {
									return (Component) kg.invoke(e);
								} catch (InvocationTargetException | IllegalAccessException ex) {
									return new TextComponent(e.toString());
								}
							};
						} else if (o instanceof String) {
							return e -> {
								try {
									return new TranslatableComponent((String) kg.invoke(e));
								} catch (InvocationTargetException | IllegalAccessException ex) {
									return new TextComponent(e.toString());
								}
							};
						}
					} catch (IllegalAccessException | InvocationTargetException ignored) {}
				}
			}
			return null;
		}
		
		private double getInitialValue(ProgressOption opt) {
			return opt.get(dummyOptions);
		}
		
		private <T> T getInitialValue(CycleOption<T> opt) {
			Function<Options, T> getter = getGetter(opt);
			return getter.apply(dummyOptions);
		}
		
		private static float getStep(ProgressOption opt) {
			return Objects.requireNonNull(ObfuscationReflectionHelper.getPrivateValue(
			  ProgressOption.class, opt, "f_92204_"));
		}
		
		private static BiFunction<Options, ProgressOption, Component> getStringifier(ProgressOption opt) {
			return ObfuscationReflectionHelper.getPrivateValue(
			  ProgressOption.class, opt, "f_92209_");
		}
		
		private static Function<Minecraft, List<FormattedCharSequence>> getTooltip(ProgressOption opt) {
			return ObfuscationReflectionHelper.getPrivateValue(
			  ProgressOption.class, opt, "f_168538_");
		}
		
		private static <T> Function<Minecraft, CycleButton.TooltipSupplier<T>> getTooltip(CycleOption<T> option) {
			return ObfuscationReflectionHelper.getPrivateValue(
			  CycleOption.class, option, "f_167715_");
		}
		
		private static <T> Supplier<CycleButton.Builder<T>> getButtonSetup(CycleOption<T> opt) {
			return ObfuscationReflectionHelper.getPrivateValue(
			  CycleOption.class, opt, "f_167714_");
		}
		
		private Function<Double, Component> getSliderLabelProvider(
		  ProgressOption opt, BiConsumer<Options, Double> effectLessSetter
		) {
			BiFunction<Options, ProgressOption, Component> stringifier = getStringifier(opt);
			Options options = Minecraft.getInstance().options;
			return v -> {
				double prev = opt.get(options);
				effectLessSetter.accept(options, v);
				Component c = stringifier.apply(options, opt);
				effectLessSetter.accept(options, prev);
				if (c instanceof TranslatableComponent tc) {
					Object[] args = tc.getArgs();
					if (args.length >= 2) {
						Object first = args[1];
						if (first instanceof Component) return (Component) first;
						return new TextComponent(String.valueOf(first));
					}
				}
				return c;
			};
		}
		
		public static class MinecraftConfigValueBuilder extends ConfigValueBuilder {
			private final ModContainer modContainer = ModList.get().getModContainerById(MINECRAFT_MODID)
			  .orElseThrow(() -> new IllegalStateException("Minecraft mod not found"));
			
			@Override public void buildModConfig(SimpleConfigImpl config) {
				config.build(modContainer, null);
			}
			
			@Override public void build(
			  AbstractConfigEntryBuilder<?, ?, ?, ?, ?, ?> entryBuilder,
			  AbstractConfigEntry<?, ?, ?> entry
			) {}
			
			@Override public Pair<ForgeConfigSpec, List<ForgeConfigSpec>> build() {
				return null;
			}
		}
	}
	
	public static class MinecraftProgressOptionEntryDelegate implements ConfigEntryDelegate<Double> {
		private final ProgressOption option;
		
		public MinecraftProgressOptionEntryDelegate(ProgressOption option) {
			this.option = option;
		}
		
		@Override public Double getValue() {
			return option.get(Minecraft.getInstance().options);
		}
		
		@Override public void setValue(Double value) {
			if (Objects.equals(getValue(), value)) return;
			Options options = Minecraft.getInstance().options;
			option.set(options, value);
			options.save();
		}
	}
	
	public static class MinecraftCycleOptionEntryDelegate<T> implements ConfigEntryDelegate<T> {
		private final CycleOption<T> option;
		private final Function<Options, T> getter;
		private final OptionSetter<T> setter;
		
		public MinecraftCycleOptionEntryDelegate(CycleOption<T> opt) {
			option = opt;
			getter = getGetter(opt);
			setter = getSetter(opt);
		}
		
		@Override public T getValue() {
			return getter.apply(Minecraft.getInstance().options);
		}
		
		@Override public void setValue(T value) {
			if (Objects.equals(getValue(), value)) return;
			Options options = Minecraft.getInstance().options;
			setter.accept(options, option, value);
			options.save();
		}
	}
	
	private static <T> Function<Options, T> getGetter(CycleOption<T> opt) {
		return ObfuscationReflectionHelper.getPrivateValue(
		  CycleOption.class, opt, "f_167713_");
	}
	
	private static <T> CycleOption.OptionSetter<T> getSetter(CycleOption<T> opt) {
		return ObfuscationReflectionHelper.getPrivateValue(
		  CycleOption.class, opt, "f_90678_");
	}
	
	public static class MinecraftFieldOptionEntryDelegate<V> implements ConfigEntryDelegate<V> {
		private final Field field;
		private final V defValue;
		
		public MinecraftFieldOptionEntryDelegate(Field field, V defValue) {
			this.field = field;
			this.defValue = defValue;
		}
		
		@SuppressWarnings("unchecked") @Override public V getValue() {
			try {
				return (V) field.get(Minecraft.getInstance().options);
			} catch (IllegalAccessException e) {
				return defValue;
			}
		}
		
		@Override public void setValue(V value) {
			if (Objects.equals(getValue(), value)) return;
			try {
				Options options = Minecraft.getInstance().options;
				field.set(options, value);
				options.save();
			} catch (IllegalAccessException ignored) {}
		}
	}
	
	public static class MinecraftSetBoolDelegate<V> implements ConfigEntryDelegate<Boolean> {
		private final Set<V> set;
		private final V value;
		
		public MinecraftSetBoolDelegate(Set<V> set, V value) {
			this.set = set;
			this.value = value;
		}
		
		@Override public Boolean getValue() {
			return set.contains(value);
		}
		
		@Override public void setValue(Boolean value) {
			if (set.contains(this.value) == value) return;
			if (value) {
				set.add(this.value);
			} else set.remove(this.value);
			Minecraft.getInstance().options.save();
		}
	}
	
	public static class MinecraftMapValueDelegate<K, V> implements ConfigEntryDelegate<V> {
		private final Map<K, V> map;
		private final K key;
		private final V defValue;
		
		public MinecraftMapValueDelegate(Map<K, V> map, K key, V defValue) {
			this.map = map;
			this.key = key;
			this.defValue = defValue;
		}
		
		@Override public V getValue() {
			return map.getOrDefault(key, defValue);
		}
		
		@Override public void setValue(V value) {
			if (Objects.equals(map.get(key), value)) return;
			map.put(key, value);
			Minecraft.getInstance().options.save();
		}
	}
}
