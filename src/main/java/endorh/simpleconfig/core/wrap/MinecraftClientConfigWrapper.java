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
import net.minecraft.client.AbstractOption;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screen.ControlsScreen;
import net.minecraft.client.resources.Language;
import net.minecraft.client.settings.BooleanOption;
import net.minecraft.client.settings.IteratableOption;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.settings.SliderPercentageOption;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.KeyBindingMap;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static endorh.simpleconfig.api.ConfigBuilderFactoryProxy.*;
import static net.minecraft.util.math.MathHelper.clamp;

@EventBusSubscriber(value=Dist.CLIENT, modid=SimpleConfigMod.MOD_ID, bus=Bus.MOD)
@OnlyIn(Dist.CLIENT)
public class MinecraftClientConfigWrapper {
	private static final Logger LOGGER = LogManager.getLogger();
	public static final String MINECRAFT_MODID = "minecraft";
	public static final String OPTIONS_TXT = "options.txt";
	
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
			m.put("advancedItemTooltips", "field_82882_x");
			m.put("fullscreenResolution", "field_198019_u");
			m.put("glDebugVerbosity", "field_209231_W");
			m.put("heldItemTooltips", "field_92117_D");
			m.put("hideServerAddress", "field_80005_w");
			m.put("lang", "field_74363_ab");
			m.put("lastServer", "field_74332_R");
			m.put("pauseOnLostFocus", "field_82881_y");
			m.put("skipMultiplayerWarning", "field_230152_Z_");
			m.put("syncChunkWrites", "field_241568_aS_");
			m.put("tutorialStep", "field_193631_S");
			m.put("useNativeTransport", "field_181150_U");
			m.put("overrideWidth", "field_92118_B");
			m.put("overrideHeight", "field_92119_C");
			m.put("modelParts", "field_178882_aU");
			m.put("sourceVolumes", "field_186714_aM");
		});
		
		// The GameSettings instance is final and unique to the Minecraft instance
		private final GameSettings options = Minecraft.getInstance().gameSettings;
		private final GameSettings dummyOptions;
		private final SimpleConfigBuilderImpl builder =
			(SimpleConfigBuilderImpl) config(MINECRAFT_MODID, Type.CLIENT);
		private ConfigEntryHolderBuilder<?> target = builder;
		private boolean caption = false;
		private final MinecraftConfigValueBuilder vb = new MinecraftConfigValueBuilder();
		
		private MinecraftOptionsWrapperBuilder() {
			GameSettings defOptions;
			try {
				File file = Files.createTempDirectory("dummy-options").toFile();
				defOptions = new GameSettings(Minecraft.getInstance(), file);
				file.delete();
			} catch (IOException ignored) {
				LOGGER.error("Couldn't create dummy options file, using nul path");
				defOptions = new GameSettings(Minecraft.getInstance(), new File("_nul"));
			}
			dummyOptions = defOptions;
			// Restore KeyBinding maps, removing the KeyMappings from the dummyOptions
			Pair<Map<String, KeyBinding>, KeyBindingMap> maps = getKeyMappingMaps();
			Map<String, KeyBinding> KEYBIND_ARRAY = maps.getLeft();
			KeyBindingMap HASH = maps.getRight();
			Set<String> rebound = new HashSet<>();
			for (KeyBinding km: dummyOptions.keyBindings) {
				String name = km.getKeyDescription();
				if (KEYBIND_ARRAY.get(name) == km) rebound.add(name);
				HASH.removeKey(km);
			}
			for (KeyBinding km: options.keyBindings) {
				String name = km.getKeyDescription();
				if (rebound.contains(name)) KEYBIND_ARRAY.put(name, km);
			}
		}
		
		@SuppressWarnings("unchecked") private static Pair<Map<String, KeyBinding>, KeyBindingMap> getKeyMappingMaps() {
			Field KEYBIND_ARRAY_field = ObfuscationReflectionHelper.findField(KeyBinding.class, "field_74516_a");
			Field HASH_field = ObfuscationReflectionHelper.findField(KeyBinding.class, "field_74514_b");
			try {
				return Pair.of((Map<String, KeyBinding>) KEYBIND_ARRAY_field.get(null), (KeyBindingMap) HASH_field.get(null));
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
		
		private void addEntries() {
			with(category("controls").withIcon(MinecraftOptions.CONTROLS).withColor(0x808080F0), () -> {
				with(group("mouse", true), () -> {
					wrapDouble(AbstractOption.SENSITIVITY, (o, v) -> o.mouseSensitivity = v);
					wrapBool(AbstractOption.INVERT_MOUSE);
					wrapDouble(AbstractOption.MOUSE_WHEEL_SENSITIVITY, (o, v) -> o.mouseWheelSensitivity = v);
					wrapBool(AbstractOption.DISCRETE_MOUSE_SCROLL);
					wrapBool(AbstractOption.TOUCHSCREEN);
					wrapBool(AbstractOption.RAW_MOUSE_INPUT);
				});
				wrapBool(AbstractOption.SNEAK, g -> g.toggleCrouch, (g, v) -> g.toggleCrouch = v);
				wrapBool(AbstractOption.SPRINT, g -> g.toggleSprint, (g, v) -> g.toggleSprint = v);
				wrapBool(AbstractOption.AUTO_JUMP);
				target.add("keyMappings", button(() -> {
					Minecraft mc = Minecraft.getInstance();
					mc.displayGuiScreen(new ControlsScreen(mc.currentScreen, mc.gameSettings));
				}).label("simpleconfig.ui.open"));
			});
			with(category("graphics").withIcon(MinecraftOptions.GRAPHICS).withColor(0x8080F0A0), () -> {
				wrapDouble(AbstractOption.FOV, (o, v) -> o.fov = v);
				wrapOption(
				  AbstractOption.GUI_SCALE, g -> g.guiScale,
				  (s, v) -> s.guiScale = v,
				  IntStream.rangeClosed(0, Minecraft.getInstance().getMainWindow()
				    .calcGuiScale(0, Minecraft.getInstance().getForceUnicodeFont()))
				    .boxed().collect(Collectors.toList()));
				
				wrapDouble(AbstractOption.RENDER_DISTANCE, (o, v) -> o.renderDistanceChunks = v.intValue());
				wrapDouble(AbstractOption.ENTITY_DISTANCE_SCALING, (o, v) -> o.entityDistanceScaling = v.floatValue());
				
				wrapEnum(AbstractOption.GRAPHICS, s -> s.graphicFanciness);
				wrapDouble(AbstractOption.GAMMA, (o, v) -> o.gamma = v);
				wrapEnum(AbstractOption.AO, s -> s.ambientOcclusionStatus);
				wrapDouble(AbstractOption.BIOME_BLEND_RADIUS, (o, v) -> o.biomeBlendRadius = MathHelper.clamp(v.intValue(), 0, 7));
				wrapEnum(AbstractOption.RENDER_CLOUDS, s -> s.cloudOption);
				wrapEnum(AbstractOption.PARTICLES, s -> s.particles);
				wrapBool(AbstractOption.ENTITY_SHADOWS);
				wrapBool(AbstractOption.VIEW_BOBBING);
				wrapEnum(AbstractOption.ATTACK_INDICATOR, s -> s.attackIndicator);
				
				wrapDouble(AbstractOption.FRAMERATE_LIMIT, (o, v) -> o.framerateLimit = v.intValue());
				wrapBool(AbstractOption.VSYNC);
				wrapDouble(AbstractOption.MIPMAP_LEVELS, (o, v) -> o.mipmapLevels = v.intValue());
				
				wrapDouble(AbstractOption.FOV_EFFECT_SCALE_SLIDER, (o, v) -> o.fovScaleEffect = MathHelper.sqrt(v.floatValue()));
				wrapDouble(AbstractOption.SCREEN_EFFECT_SCALE_SLIDER, (o, v) -> o.screenEffectScale = MathHelper.sqrt(v.floatValue()));
				
				wrapBool(AbstractOption.FULLSCREEN);
				wrapString("fullscreenResolution", "");
				wrapInt("overrideWidth", 0);
				wrapInt("overrideHeight", 0);
			});
			with(category("sound").withIcon(MinecraftOptions.SOUND).withColor(0x80F04280), () -> {
				with(group("volume", true), true, () -> {
					Map<SoundCategory, Float> sourceLevels =
					  ObfuscationReflectionHelper.getPrivateValue(
					    GameSettings.class, options, SRG_NAMES.get("sourceVolumes"));
					for (SoundCategory source: SoundCategory.values())
						wrapMapValue(
						  source.getName(), "soundCategory." + source.getName(),
						  sourceLevels, source, volume(1F));
				});
				wrapBool(AbstractOption.SHOW_SUBTITLES);
			});
			with(category("chat").withIcon(MinecraftOptions.CHAT).withColor(0x8090F080), () -> {
				wrapEnum(AbstractOption.CHAT_VISIBILITY, s -> s.chatVisibility);
				wrapDouble(AbstractOption.CHAT_OPACITY, (o, v) -> o.chatOpacity = v);
				wrapDouble(AbstractOption.ACCESSIBILITY_TEXT_BACKGROUND_OPACITY, (o, v) -> o.accessibilityTextBackgroundOpacity = v);
				wrapBool(
				  AbstractOption.ACCESSIBILITY_TEXT_BACKGROUND,
				  s -> s.accessibilityTextBackground,
				  (s, v) -> s.accessibilityTextBackground = v);
				wrapDouble(AbstractOption.CHAT_WIDTH, (o, v) -> o.chatWidth = v);
				wrapDouble(AbstractOption.CHAT_HEIGHT_FOCUSED, (o, v) -> o.chatHeightFocused = v);
				wrapDouble(AbstractOption.CHAT_HEIGHT_UNFOCUSED, (o, v) -> o.chatHeightUnfocused = v);
				wrapDouble(AbstractOption.LINE_SPACING, (o, v) -> o.chatLineSpacing = v);
				wrapDouble(AbstractOption.CHAT_SCALE, (o, v) -> o.chatScale = v);
				wrapBool(AbstractOption.CHAT_COLOR);
				
				wrapDouble(AbstractOption.DELAY_INSTANT, (o, v) -> o.chatDelay = v);
				wrapBool(AbstractOption.AUTO_SUGGEST_COMMANDS);
				
				wrapBool(AbstractOption.CHAT_LINKS);
				wrapBool(AbstractOption.CHAT_LINKS_PROMPT);
				// wrapBool(AbstractOption.HIDE_MATCHED_NAMES);
			});
			with(category("skin").withIcon(MinecraftOptions.SKIN).withColor(0x80E0E080), () -> {
				wrapEnum(AbstractOption.MAIN_HAND, s -> s.mainHand);
				with(group("modelPart", true), () -> {
					Set<PlayerModelPart> modelParts = ObfuscationReflectionHelper.getPrivateValue(
					  GameSettings.class, options, SRG_NAMES.get("modelParts"));
					for (PlayerModelPart part: PlayerModelPart.values()) wrapSetBool(
					  part.getPartName(), "options.modelPart." + part.getName(),
					  modelParts, part, true);
				});
			});
			with(category("language").withIcon(MinecraftOptions.LANGUAGE).withColor(0x80E042E0), () -> {
				wrapString("lang", "en_us", () -> Minecraft.getInstance()
				  .getLanguageManager().getLanguages().stream()
				  .map(Language::getCode).collect(Collectors.toList()));
				wrapBool(AbstractOption.FORCE_UNICODE_FONT);
			});
			with(category("online").withIcon(MinecraftOptions.ONLINE).withColor(0x80F0A080), () -> {
				wrapBool(AbstractOption.REALMS_NOTIFICATIONS);
			});
			with(category("accessibility").withIcon(
			  MinecraftOptions.ACCESSIBILITY).withColor(0x80E0E0FF), () -> {
				wrapEnum(AbstractOption.NARRATOR, s -> s.narrator,
				         s -> NarratorChatListener.INSTANCE.isActive()
				              ? s.func_238233_b_()
				              : new TranslationTextComponent("options.narrator.notavailable"));
				// wrapBool(AbstractOption.DARK_MOJANG_STUDIOS_BACKGROUND_COLOR);
			});
			with(category("advanced").withIcon(MinecraftOptions.ADVANCED), () -> {
				wrapBool(AbstractOption.REDUCED_DEBUG_INFO);
				wrapBool("pauseOnLostFocus", true);
				wrapBool("advancedItemTooltips", false);
				wrapBool("heldItemTooltips", true);
				wrapBool("hideServerAddress", false);
				wrapBool("syncChunkWrites", Util.getOSType() == Util.OS.WINDOWS);
				wrapBool("useNativeTransport", true);
				wrapInt("glDebugVerbosity", 1);
				wrapEnum("tutorialStep", TutorialSteps.MOVEMENT);
				wrapBool("skipMultiplayerWarning", false);
				wrapString("lastServer", "");
			});
		}
		
		public SimpleConfig build() {
			try {
				addEntries();
				builder.withGUIDecorator((s, b) -> b.getCategories(EditType.CLIENT).forEach(
				  c -> c.setContainingFile(new File(
					 Minecraft.getInstance().gameDir, OPTIONS_TXT
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
		
		private void wrapDouble(SliderPercentageOption opt, BiConsumer<GameSettings, Double> effectLessSetter) {
			double min = opt.getMinValue();
			double max = opt.getMaxValue();
			DoubleEntryBuilder b = number(getInitialValue(opt), min, max)
			  .slider(getSliderLabelProvider(opt, effectLessSetter))
			  .sliderMap(
				 d -> clamp((opt.denormalizeValue(d) - min) / (max - min), 0F, 1F),
				 d -> opt.normalizeValue(clamp(min + d * (max - min), min, max)));
			add(opt, b);
		}
		
		private <E extends Enum<E>> void wrapEnum(
		  IteratableOption opt, Function<GameSettings, E> getter
		) {
			wrapEnum(opt, getter, null);
		}
		
		private <E extends Enum<E>> void wrapEnum(
		  IteratableOption opt, Function<GameSettings, E> getter,
		  @Nullable Function<E, ITextComponent> display
		) {
			E initial = getter.apply(dummyOptions);
			List<E> values = Arrays.asList(initial.getDeclaringClass().getEnumConstants());
			wrapOption(opt, getter, null, display, values);
		}
		
		private void wrapBool(IteratableOption opt,  Function<GameSettings, Boolean> getter, BiConsumer<GameSettings, Boolean> setter) {
			wrapOption(opt, getter, setter, Lists.newArrayList(false, true));
		}
		
		private <T> void wrapOption(
		  IteratableOption opt, Function<GameSettings, T> getter,
		  @Nullable BiConsumer<GameSettings, T> setter, List<T> values
		) {
			wrapOption(opt, getter, setter, null, values);
		}
		
		private <T> void wrapOption(
		  IteratableOption opt, Function<GameSettings, T> getter,
		  @Nullable BiConsumer<GameSettings, T> setter,
		  @Nullable Function<T, ITextComponent> display, List<T> values
		) {
			T initial = getter.apply(dummyOptions);
			BiFunction<GameSettings, IteratableOption, ITextComponent> getText = (s, o) -> o.getName(s);
			if (setter == null) {
				BiConsumer<GameSettings, Integer> setFromIdx = opt::setValueIndex;
				setter = (gs, e) -> {
					int i = values.indexOf(e);
					for (int j = 0; j < values.size(); j++) {
						setFromIdx.accept(gs, i);
						if (getter.apply(gs).equals(e)) return;
					}
					LOGGER.error("Failed to set option " + getName(opt) + " to " + e);
				};
			}
			if (display == null) display = getDisplayer(opt, values, setter, getText, getter);
			OptionEntryBuilder<T> b = option(initial, values).withDisplay(display);
			AbstractConfigEntryBuilder<T, ?, ?, ?, ?, ?> bb = cast(b);
			bb = bb.translation(getName(opt))
			  .withDelegate(new MinecraftIteratableOptionEntryDelegate<>(opt, getter, setter));
			target.add(getID(opt), bb);
		}
		
		private <T> Function<T, ITextComponent> getDisplayer(
		  IteratableOption opt, List<T> values, BiConsumer<GameSettings, T> setter,
		  BiFunction<GameSettings, IteratableOption, ITextComponent> getText,
		  Function<GameSettings, T> getter
		) {
			GameSettings dummy = dummyOptions;
			Map<T, ITextComponent> map = new HashMap<>();
			for (int j = 0; j < values.size(); j++) {
				T t = getter.apply(dummy);
				int i = values.indexOf(t);
				if (i < 0) throw new IllegalStateException(
				  "Unexpected value found in option: " + getName(opt) + ", (" + t + ")");
				if (!map.containsKey(t))
					map.put(t, extractValue(getText.apply(dummy, opt)));
				if (j < values.size() - 1)
					setter.accept(dummy, values.get((i + 1) % values.size()));
			}
			return t -> map.getOrDefault(t, new StringTextComponent(t.toString()));
		}
		
		private void wrapBool(BooleanOption opt) {
			BooleanEntryBuilder b = onOff(getInitialValue(opt));
			add(opt, b);
		}
		
		private static Supplier<List<IReorderingProcessor>> getValues(IteratableOption option) {
			return () -> option.getOptionValues().orElse(Lists.newArrayList());
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
			Field field = ObfuscationReflectionHelper.findField(GameSettings.class, srgName);
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
		
		private void add(SliderPercentageOption opt, ConfigEntryBuilder<Double, ?, ?, ?> entryBuilder) {
			AbstractConfigEntryBuilder<Double, ?, ?, ?, ?, ?> b = cast(entryBuilder);
			b = b.translation(getName(opt))
			  .withDelegate(new MinecraftProgressOptionEntryDelegate(opt));
			addEntry(getID(opt), b);
		}
		
		private void add(BooleanOption opt, ConfigEntryBuilder<Boolean, ?, ?, ?> entryBuilder) {
			AbstractConfigEntryBuilder<Boolean, ?, ?, ?, ?, ?> b = cast(entryBuilder);
			b = b.translation(getName(opt)).withDelegate(new MinecraftBooleanOptionEntryDelegate(opt));
			addEntry(getID(opt), b);
		}
		
		private void addEntry(String name, ConfigEntryBuilder<?, ?, ?, ?> entryBuilder) {
			if (caption) {
				if (target instanceof ConfigGroupBuilder) {
					((ConfigGroupBuilder) target).caption(name, castAtom(entryBuilder));
					caption = false;
				} else throw new IllegalStateException(
				  "Cannot add caption outside a group: " + name);
			} else target.add(name, entryBuilder);
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
		
		private static ITextComponent getCaption(AbstractOption opt) {
			return ObfuscationReflectionHelper.getPrivateValue(AbstractOption.class, opt, "field_243217_ac");
		}
		
		private static String getName(AbstractOption opt) {
			ITextComponent caption = getCaption(opt);
			return caption instanceof TranslationTextComponent
			       ? ((TranslationTextComponent) caption).getKey() : caption.getString();
		}
		
		private static String getID(AbstractOption opt) {
			return getName(opt).replace("options.", "").replace('.', '_');
		}
		
		private double getInitialValue(SliderPercentageOption opt) {
			return opt.get(dummyOptions);
		}
		
		private boolean getInitialValue(BooleanOption opt) {
			return opt.get(dummyOptions);
		}
		
		private static BiFunction<GameSettings, SliderPercentageOption, ITextComponent> getStringifier(SliderPercentageOption opt) {
			return ObfuscationReflectionHelper.getPrivateValue(
			  SliderPercentageOption.class, opt, "field_216739_V");
		}
		
		private Function<Double, ITextComponent> getSliderLabelProvider(
		  SliderPercentageOption opt, BiConsumer<GameSettings, Double> effectLessSetter
		) {
			BiFunction<GameSettings, SliderPercentageOption, ITextComponent> stringifier = getStringifier(opt);
			GameSettings options = Minecraft.getInstance().gameSettings;
			return v -> {
				double prev = opt.get(options);
				effectLessSetter.accept(options, v);
				ITextComponent c = stringifier.apply(options, opt);
				effectLessSetter.accept(options, prev);
				return extractValue(c);
			};
		}
		
		private ITextComponent extractValue(ITextComponent label) {
			if (label instanceof TranslationTextComponent) {
				TranslationTextComponent tc = (TranslationTextComponent) label;
				Object[] args = tc.getFormatArgs();
				if (args.length >= 2) {
					Object first = args[1];
					if (first instanceof ITextComponent) return (ITextComponent) first;
					return new StringTextComponent(String.valueOf(first));
				}
			}
			return label;
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
			
			@Override public ForgeConfigSpec build() {
				return null;
			}
		}
	}
	
	public static class MinecraftProgressOptionEntryDelegate implements ConfigEntryDelegate<Double> {
		private final SliderPercentageOption option;
		
		public MinecraftProgressOptionEntryDelegate(SliderPercentageOption option) {
			this.option = option;
		}
		
		@Override public Double getValue() {
			return option.get(Minecraft.getInstance().gameSettings);
		}
		
		@Override public void setValue(Double value) {
			if (getValue().equals(value)) return;
			GameSettings options = Minecraft.getInstance().gameSettings;
			option.set(options, value);
			options.saveOptions();
		}
	}
	
	public static class MinecraftIteratableOptionEntryDelegate<T> implements ConfigEntryDelegate<T> {
		private final IteratableOption option;
		private final Function<GameSettings, T> getter;
		private final BiConsumer<GameSettings, T> setter;
		
		public MinecraftIteratableOptionEntryDelegate(
		  IteratableOption opt, Function<GameSettings, T> getter,
		  BiConsumer<GameSettings, T> setter
		) {
			option = opt;
			this.getter = getter;
			this.setter = setter;
		}
		
		@Override public T getValue() {
			return getter.apply(Minecraft.getInstance().gameSettings);
		}
		
		@Override public void setValue(T value) {
			if (getValue().equals(value)) return;
			GameSettings options = Minecraft.getInstance().gameSettings;
			setter.accept(options, value);
			options.saveOptions();
		}
	}
	
	public static class MinecraftBooleanOptionEntryDelegate implements ConfigEntryDelegate<Boolean> {
		private final BooleanOption option;
		
		public MinecraftBooleanOptionEntryDelegate(BooleanOption option) {
			this.option = option;
		}
		
		@Override public Boolean getValue() {
			return option.get(Minecraft.getInstance().gameSettings);
		}
		
		@Override public void setValue(Boolean value) {
			if (getValue().equals(value)) return;
			GameSettings options = Minecraft.getInstance().gameSettings;
			option.set(options, value? "true" : "false");
			options.saveOptions();
		}
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
				return (V) field.get(Minecraft.getInstance().gameSettings);
			} catch (IllegalAccessException e) {
				return defValue;
			}
		}
		
		@Override public void setValue(V value) {
			try {
				if (getValue().equals(value)) return;
				GameSettings options = Minecraft.getInstance().gameSettings;
				field.set(options, value);
				options.saveOptions();
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
			if (value == set.contains(this.value)) return;
			if (value) {
				set.add(this.value);
			} else set.remove(this.value);
			Minecraft.getInstance().gameSettings.saveOptions();
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
			map.put(key, value);
			Minecraft.getInstance().gameSettings.saveOptions();
		}
	}
}
