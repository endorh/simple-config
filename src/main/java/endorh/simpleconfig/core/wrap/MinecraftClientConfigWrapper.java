package endorh.simpleconfig.core.wrap;

import com.google.common.primitives.Primitives;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.api.*;
import endorh.simpleconfig.api.SimpleConfig.EditType;
import endorh.simpleconfig.api.SimpleConfig.Type;
import endorh.simpleconfig.api.entry.DoubleEntryBuilder;
import endorh.simpleconfig.api.entry.IntegerEntryBuilder;
import endorh.simpleconfig.api.entry.OptionEntryBuilder;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons.MinecraftOptions;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractConfigEntryBuilder;
import endorh.simpleconfig.core.SimpleConfigBuilderImpl;
import endorh.simpleconfig.core.SimpleConfigBuilderImpl.ConfigValueBuilder;
import endorh.simpleconfig.core.SimpleConfigImpl;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.Util.OS;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.OptionInstance.LazyEnum;
import net.minecraft.client.OptionInstance.SliderableValueSet;
import net.minecraft.client.OptionInstance.TooltipSupplier;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static endorh.simpleconfig.api.ConfigBuilderFactoryProxy.*;

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
			m.put("resourcePacks", "f_92117_");
			m.put("incompatibleResourcePacks", "f_92118_");
			m.put("lastServer", "f_92066_");
			m.put("lang", "f_92075_");
			m.put("hideServerAddress", "f_92124_");
			m.put("advancedItemTooltips", "f_92125_");
			m.put("pauseOnLostFocus", "f_92126_");
			m.put("overrideWidth", "f_92128_");
			m.put("overrideHeight", "f_92129_");
			// m.put("heldItemTooltips", "f_92130_");
			m.put("useNativeTransport", "f_92028_");
			m.put("tutorialStep", "f_92030_");
			m.put("glDebugVerbosity", "f_92035_");
			m.put("skipMultiplayerWarning", "f_92083_");
			m.put("skipRealms32bitWarning", "f_210816_");
			m.put("joinedFirstServer", "f_92031_");
			m.put("hideBundleTutorial", "f_168405_");
			m.put("syncChunkWrites", "f_92076_");
			m.put("modelParts", "f_92108_");
			m.put("sourceVolumes", "f_92109_");
		});
		
		// The Options instance is final and unique to the Minecraft instance
		private final Options options = Minecraft.getInstance().options;
		private final SimpleConfigBuilderImpl builder =
			(SimpleConfigBuilderImpl) config(MINECRAFT_MODID, Type.CLIENT);
		private ConfigEntryHolderBuilder<?> target = builder;
		private boolean caption = false;
		private final MinecraftConfigValueBuilder vb = new MinecraftConfigValueBuilder();
		
		private void addEntries() {
			with(category("controls").withIcon(MinecraftOptions.CONTROLS).withColor(0x808080F0), () -> {
				with(group("mouse", true), () -> {
					wrapDouble(options.sensitivity());
					wrapBool(options.invertYMouse());
					wrapDouble(options.mouseWheelSensitivity());
					wrapBool(options.discreteMouseScroll());
					wrapBool(options.touchscreen());
					wrapBool(options.rawMouseInput());
				});
				wrapBool(options.toggleCrouch());
				wrapBool(options.toggleSprint());
				wrapBool(options.autoJump());
				wrapBool(options.operatorItemsTab());
				target.add("keyMappings", button(() -> {
					Minecraft mc = Minecraft.getInstance();
					mc.setScreen(new KeyBindsScreen(mc.screen, mc.options));
				}).label("simpleconfig.ui.open"));
			});
			with(category("graphics").withIcon(MinecraftOptions.GRAPHICS).withColor(0x8080F0A0), () -> {
				wrapInt(options.fov());
				wrapInt(options.guiScale());
				
				wrapInt(options.renderDistance());
				wrapInt(options.simulationDistance());
				wrapDouble(options.entityDistanceScaling());
				
				wrapOption(options.graphicsMode());
				wrapDouble(options.gamma());
				wrapOption(options.ambientOcclusion());
				wrapInt(options.biomeBlendRadius());
				wrapOption(options.cloudStatus());
				wrapOption(options.particles());
				wrapBool(options.entityShadows());
				wrapBool(options.bobView());
				wrapOption(options.attackIndicator());
				wrapBool(options.showAutosaveIndicator());
				
				wrapInt(options.framerateLimit());
				wrapBool(options.enableVsync());
				wrapOption(options.prioritizeChunkUpdates());
				wrapInt(options.mipmapLevels());
				
				wrapDouble(options.fovEffectScale());
				wrapDouble(options.darknessEffectScale());
				wrapDouble(options.screenEffectScale());
				
				wrapBool(options.fullscreen());
				wrapInt("overrideWidth", 0);
				wrapInt("overrideHeight", 0);
			});
			with(category("sound").withIcon(MinecraftOptions.SOUND).withColor(0x80F04280), () -> {
				with(group("volume", true), true, () -> {
					for (SoundSource source: SoundSource.values())
						wrapVolume(options.getSoundSourceOptionInstance(source));
				});
				wrapOption(options.soundDevice());
				wrapBool(options.showSubtitles());
				wrapBool(options.directionalAudio());
			});
			with(category("chat").withIcon(MinecraftOptions.CHAT).withColor(0x8090F080), () -> {
				wrapOption(options.chatVisibility());
				wrapDouble(options.chatOpacity());
				wrapDouble(options.textBackgroundOpacity());
				wrapBool(options.backgroundForChatOnly());
				wrapDouble(options.chatWidth());
				wrapDouble(options.chatHeightFocused());
				wrapDouble(options.chatHeightUnfocused());
				wrapDouble(options.chatLineSpacing());
				wrapDouble(options.chatScale());
				wrapBool(options.chatColors());
				
				wrapDouble(options.chatDelay());
				wrapBool(options.autoSuggestions());
				
				wrapBool(options.chatLinks());
				wrapBool(options.chatLinksPrompt());
				wrapBool(options.onlyShowSecureChat());
				wrapBool(options.hideMatchedNames());
			});
			with(category("skin").withIcon(MinecraftOptions.SKIN).withColor(0x80E0E080), () -> {
				wrapOption(options.mainHand());
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
				  .getLanguageManager().getLanguages().values().stream()
				  .map(LanguageInfo::toComponent).map(Component::getString).collect(Collectors.toList()));
				wrapBool(options.forceUnicodeFont());
			});
			with(category("online").withIcon(MinecraftOptions.ONLINE).withColor(0x80F0A080), () -> {
				wrapBool(options.allowServerListing());
				wrapBool(options.realmsNotifications());
				wrapBool(options.telemetryOptInExtra());
			});
			with(category("accessibility").withIcon(
			  MinecraftOptions.ACCESSIBILITY).withColor(0x80E0E0FF), () -> {
				wrapOption(options.narrator());
				wrapBool(options.hideLightningFlash());
				wrapBool(options.darkMojangStudiosBackground());
				wrapDouble(options.panoramaSpeed());
			});
			with(category("advanced").withIcon(MinecraftOptions.ADVANCED), () -> {
				wrapBool(options.reducedDebugInfo());
				wrapBool("pauseOnLostFocus", true);
				wrapBool("advancedItemTooltips", false);
				// wrapBool("heldItemTooltips", true);
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
		
		private void wrapInt(OptionInstance<Integer> opt) {
			Object values = opt.values();
			if (values instanceof OptionInstance.IntRange range) {
				IntegerEntryBuilder b = number(getInitialValue(opt))
				  .range(range.minInclusive(), range.maxInclusive())
				  .slider(getSliderLabelProvider(opt));
				add(opt, b);
			} else if (values instanceof OptionInstance.ClampingLazyMaxIntRange range) {
				OptionEntryBuilder<Integer> bb = option(
				  0, () -> IntStream.range(0, range.maxInclusive() + 1).boxed().toList()
				).withDisplay(getSliderLabelProvider(opt));
				add(opt, bb);
			} else if (values instanceof OptionInstance.SliderableValueSet<?>) {
				//noinspection unchecked
				SliderableValueSet<Integer> set = (SliderableValueSet<Integer>) values;
				IntegerEntryBuilder b = number(getInitialValue(opt))
				  .range(set.fromSliderValue(0), set.fromSliderValue(1))
				  .slider(getSliderLabelProvider(opt));
				add(opt, b);
			}
		}

		private void wrapDouble(OptionInstance<Double> opt) {
			wrapDouble(opt, null);
		}

		private void wrapDouble(OptionInstance<Double> opt, DoubleEntryBuilder builder) {
			DoubleEntryBuilder b = builder != null? builder.withValue(getInitialValue(opt)) : number(getInitialValue(opt));
			Object values = opt.values();
			if (values instanceof OptionInstance.UnitDouble unit) {
				b = b.range(0, 1).slider(getSliderLabelProvider(opt));
			} else if (values instanceof OptionInstance.SliderableValueSet<?>) {
				//noinspection unchecked
				SliderableValueSet<Double> set = (SliderableValueSet<Double>) values;
				b = b.range(set.fromSliderValue(0), set.fromSliderValue(1))
				  .slider(getSliderLabelProvider(opt));
			}
			add(opt, b);
		}

		private void wrapVolume(OptionInstance<Double> opt) {
			wrapDouble(opt, volume(1D));
		}
		
		@SuppressWarnings("unchecked") private <T> void wrapOption(OptionInstance<T> opt) {
			Object values = opt.values();
			T initial = getInitialValue(opt);
			if (values instanceof OptionInstance.LazyEnum<?>) {
				LazyEnum<T> le = (LazyEnum<T>) values;
				OptionEntryBuilder<T> b = option(
				  initial,
				  le.valueListSupplier()::getSelectedList
				).withCodec(le.codec())
				  .withDisplay(getStringifier(opt));
				add(opt, b);
			} else if (values instanceof OptionInstance.Enum<?>) {
				OptionInstance.Enum<T> e = (OptionInstance.Enum<T>) values;
				OptionEntryBuilder<T> b = option(initial, e.values())
				  .withCodec(e.codec())
				  .withDisplay(getStringifier(opt));
				add(opt, b);
			} else if (values instanceof OptionInstance.AltEnum<?>) {
				OptionInstance.AltEnum<T> e = (OptionInstance.AltEnum<T>) values;
				OptionEntryBuilder<T> b = option(initial, e.valueListSupplier().getSelectedList())
				  .withCodec(e.codec())
				  .withDisplay(getStringifier(opt));
				add(opt, b);
			}
		}
		
		private void wrapBool(OptionInstance<Boolean> opt) {
			Function<Boolean, Component> stringifier = getStringifier(opt);
			add(opt, bool(getInitialValue(opt))
			  .text(b -> patchBoolean(stringifier.apply(b))));
		}
		
		private static Component patchBoolean(Component c) {
			if ("OFF".equals(c.getString()) && c.getStyle().isEmpty()) {
				return c.copy().withStyle(Style.EMPTY.withColor(0xFF4242));
			} else if ("ON".equals(c.getString()) && c.getStyle().isEmpty()) {
				return c.copy().withStyle(ChatFormatting.GREEN);
			} else return c;
		}
		
		private void wrapBool(String name, boolean def) {
			add(name, bool(def));
		}
		
		private void wrapInt(String name, int def) {
			add(name, number(def));
		}
		
		private void wrapDouble(String name, double def) {
			add(name, number(def));
		}
		
		private void wrapFloat(String name, float def) {
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
		
		private <V> void add(
		  String name, ConfigEntryBuilder<V, ?, ?, ?> entryBuilder
		) {
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
			b = b.withDelegate(new MinecraftFieldOptionEntryDelegate<>(options, field, entryBuilder.getValue()));
			addEntry(name, b);
		}
		
		private <V> void add(
		  OptionInstance<V> opt, ConfigEntryBuilder<V, ?, ?, ?> entryBuilder
		) {
			TooltipSupplier<V> tooltipSupplier = getTooltip(opt);
			entryBuilder = entryBuilder.tooltip(v -> Optional.ofNullable(tooltipSupplier.apply(v))
					.map(t -> t.toCharSequence(Minecraft.getInstance())).orElseGet(Collections::emptyList).stream()
					.map(SimpleConfigTextUtil::asComponent).collect(Collectors.toList()));
			AbstractConfigEntryBuilder<V, ?, ?, ?, ?, ?> b = cast(entryBuilder);
			b = b.translation(getName(opt)).withDelegate(new MinecraftOptionEntryDelegate<>(opt));
			addEntry(getID(opt), b);
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
		
		private Component getCaption(OptionInstance<?> opt) {
			return ObfuscationReflectionHelper.getPrivateValue(OptionInstance.class, opt, "f_231480_");
		}
		
		private <V> OptionInstance.TooltipSupplier<V> getTooltip(OptionInstance<V> opt) {
			return ObfuscationReflectionHelper.getPrivateValue(OptionInstance.class, opt, "f_231474_");
		}
		
		private String getName(OptionInstance<?> opt) {
			Component caption = getCaption(opt);
			return caption.getContents() instanceof TranslatableContents c
			       ? c.getKey() : caption.getString();
		}
		
		private String getID(OptionInstance<?> opt) {
			return getName(opt).replace("options.", "").replace('.', '_');
		}
		
		private static <T> T getInitialValue(OptionInstance<T> opt) {
			return ObfuscationReflectionHelper.getPrivateValue(
			  OptionInstance.class, opt, "f_231478_");
		}
		
		private static <T> Function<T, Component> getStringifier(OptionInstance<T> opt) {
			return ObfuscationReflectionHelper.getPrivateValue(
			  OptionInstance.class, opt, "f_231475_");
		}
		
		private static <T> Function<T, Component> getSliderLabelProvider(OptionInstance<T> opt) {
			Function<T, Component> str = getStringifier(opt);
			return v -> {
				Component a = str.apply(v);
				ComponentContents contents = a.getContents();
				if (contents instanceof TranslatableContents tc) {
					Object[] args = tc.getArgs();
					if (args.length >= 2) {
						Object first = args[1];
						if (first instanceof Component) return (Component) first;
						return Component.literal(String.valueOf(first));
					} else return a;
				} else return a;
			};
		}
		
		private static final Method Options$readPackList = ObfuscationReflectionHelper.findMethod(
		  Options.class, "m_168442_", String.class);
		@SuppressWarnings("unchecked") private static List<String> readPackList(String packList) {
			try {
				return (List<String>) Options$readPackList.invoke(null);
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
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
	
	public static class MinecraftOptionEntryDelegate<V> implements ConfigEntryDelegate<V> {
		private final OptionInstance<V> option;
		public MinecraftOptionEntryDelegate(OptionInstance<V> option) {
			this.option = option;
		}
		
		@Override public V getValue() {
			return option.get();
		}
		
		@Override public void setValue(V value) {
			if (Objects.equals(getValue(), value)) return;
			option.set(value);
			Minecraft.getInstance().options.save();
		}
	}
	
	public static class MinecraftFieldOptionEntryDelegate<V> implements ConfigEntryDelegate<V> {
		private final Options options;
		private final Field field;
		private final V defValue;
		
		public MinecraftFieldOptionEntryDelegate(Options options, Field field, V defValue) {
			this.options = options;
			this.field = field;
			this.defValue = defValue;
		}
		
		@SuppressWarnings("unchecked") @Override public V getValue() {
			try {
				return (V) field.get(options);
			} catch (IllegalAccessException e) {
				return defValue;
			}
		}
		
		@Override public void setValue(V value) {
			if (Objects.equals(getValue(), value)) return;
			try {
				field.set(options, value);
				Minecraft.getInstance().options.save();
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
