package endorh.simpleconfig;

import com.mojang.blaze3d.platform.InputConstants;
import endorh.simpleconfig.api.SimpleConfig;
import endorh.simpleconfig.config.ClientConfig;
import endorh.simpleconfig.config.CommonConfig;
import endorh.simpleconfig.config.ServerConfig;
import endorh.simpleconfig.core.SimpleConfigModConfig.LanguageReloadManager;
import endorh.simpleconfig.core.SimpleConfigNetworkHandler;
import endorh.simpleconfig.core.SimpleConfigResourcePresetHandler;
import endorh.simpleconfig.core.commands.SimpleConfigArgumentTypes;
import endorh.simpleconfig.grammar.nbt.SNBTLexer;
import endorh.simpleconfig.grammar.nbt.SNBTParser;
import endorh.simpleconfig.grammar.regex.RegexLexer;
import endorh.simpleconfig.grammar.regex.RegexParser;
import endorh.simpleconfig.highlight.HighlighterManager;
import endorh.simpleconfig.highlight.HighlighterManager.LanguageHighlighter;
import endorh.simpleconfig.ui.hotkey.ExtendedKeyBindDispatcher;
import endorh.simpleconfig.ui.hotkey.ResourceConfigHotKeyGroupHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegisterEvent.RegisterHelper;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.lwjgl.glfw.GLFW;

import static net.minecraftforge.client.settings.KeyConflictContext.GUI;
import static net.minecraftforge.client.settings.KeyModifier.*;

@Mod(SimpleConfigMod.MOD_ID)
@EventBusSubscriber(value = Dist.CLIENT, modid = SimpleConfigMod.MOD_ID, bus = Bus.MOD)
@Internal public class SimpleConfigMod {
	public static final String MOD_ID = SimpleConfig.MOD_ID;
	public static SoundEvent UI_TAP;
	public static SoundEvent UI_DOUBLE_TAP;
	
	// Storing the config instances is optional
	@OnlyIn(Dist.CLIENT) public static SimpleConfig CLIENT_CONFIG;
	public static SimpleConfig COMMON_CONFIG;
	public static SimpleConfig SERVER_CONFIG;
	
	public static final HighlighterManager JSON_HIGHLIGHTER_MANAGER = HighlighterManager.INSTANCE;
	public static final SimpleConfigResourcePresetHandler RESOURCE_PRESET_HANDLER = SimpleConfigResourcePresetHandler.INSTANCE;
	public static final ResourceConfigHotKeyGroupHandler RESOURCE_HOT_KEY_GROUP_HANDLER = ResourceConfigHotKeyGroupHandler.INSTANCE;
	public static final LanguageReloadManager LANGUAGE_RELOAD_MANAGER = LanguageReloadManager.INSTANCE;
	
	public SimpleConfigMod() {
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			// Create and register the client config for the mod
			CLIENT_CONFIG = ClientConfig.build();
		});
		COMMON_CONFIG = CommonConfig.build();
		SERVER_CONFIG = ServerConfig.build();
		SimpleConfigArgumentTypes.registerArgumentTypes();
		SimpleConfigNetworkHandler.registerPackets();
	}
	
	@SubscribeEvent
	public static void registerReloadListener(RegisterParticleProvidersEvent event) {
		ReloadableResourceManager manager = (ReloadableResourceManager) Minecraft.getInstance().getResourceManager();
		manager.registerReloadListener(JSON_HIGHLIGHTER_MANAGER);
		manager.registerReloadListener(RESOURCE_PRESET_HANDLER);
		manager.registerReloadListener(RESOURCE_HOT_KEY_GROUP_HANDLER);
		JSON_HIGHLIGHTER_MANAGER.registerHighlighter(new LanguageHighlighter<>(
		  "regex", RegexLexer::new, RegexParser::new, RegexParser::root));
		JSON_HIGHLIGHTER_MANAGER.registerHighlighter(new LanguageHighlighter<>(
		  "snbt", SNBTLexer::new, SNBTParser::new, SNBTParser::root));
		manager.registerReloadListener(LANGUAGE_RELOAD_MANAGER);
	}
	
	@SubscribeEvent
	protected static void onRegisterSounds(RegisterEvent event) {
		event.register(ForgeRegistries.SOUND_EVENTS.getRegistryKey(), r -> {
			UI_TAP = regSound(r, new ResourceLocation(MOD_ID, "ui_tap"));
			UI_DOUBLE_TAP = regSound(r, new ResourceLocation(MOD_ID, "ui_double_tap"));
		});
	}
	
	protected static SoundEvent regSound(RegisterHelper<SoundEvent> registry, ResourceLocation name) {
		SoundEvent event = new SoundEvent(name);
		registry.register(name, event);
		return event;
	}
	
	@EventBusSubscriber(value = Dist.CLIENT, bus = Bus.MOD, modid = MOD_ID)
	public static class KeyBindings {
		public static final String CATEGORY = "Simple Config";
		public static KeyMapping
		  SEARCH,
		  PREV_TYPE, NEXT_TYPE,
		  PREV_PAGE, NEXT_PAGE,
		  UNDO, REDO,
		  PREV_ERROR, NEXT_ERROR,
		  SAVE, RESET_RESTORE, HOTKEY, HELP;
		
		@SubscribeEvent public static void register(RegisterKeyMappingsEvent e) {
			SEARCH = reg(e, "search", CONTROL, GLFW.GLFW_KEY_F);
			PREV_TYPE = reg(e, "prev_type", ALT, GLFW.GLFW_KEY_PAGE_UP);
			NEXT_TYPE = reg(e, "next_type", ALT, GLFW.GLFW_KEY_PAGE_DOWN);
			PREV_PAGE = reg(e, "prev_page", CONTROL, GLFW.GLFW_KEY_PAGE_UP);
			NEXT_PAGE = reg(e, "next_page", CONTROL, GLFW.GLFW_KEY_PAGE_DOWN);
			UNDO = reg(e, "undo", CONTROL, GLFW.GLFW_KEY_Z);
			REDO = reg(e, "redo", CONTROL, GLFW.GLFW_KEY_Y);
			PREV_ERROR = reg(e, "prev_error", SHIFT, GLFW.GLFW_KEY_F1);
			NEXT_ERROR = reg(e, "next_error", GLFW.GLFW_KEY_F1);
			SAVE = reg(e, "save", CONTROL, GLFW.GLFW_KEY_S);
			RESET_RESTORE = reg(e, "reset_restore", CONTROL, GLFW.GLFW_KEY_R);
			HOTKEY = reg(e, "hotkey", CONTROL, GLFW.GLFW_KEY_H);
			HELP = reg(e, "help", CONTROL, GLFW.GLFW_KEY_Q);
		}
		
		@SubscribeEvent public static void register(FMLClientSetupEvent event) {
			event.enqueueWork(() -> MinecraftForge.EVENT_BUS.register(ExtendedKeyBindDispatcher.INSTANCE));
		}
		
		@OnlyIn(Dist.CLIENT) private static KeyMapping reg(
		  RegisterKeyMappingsEvent event, String name, int keyCode
		) {
			KeyMapping binding = new KeyMapping(MOD_ID + ".key." + name, GUI, InputConstants.Type.KEYSYM.getOrCreate(keyCode), CATEGORY);
			event.register(binding);
			return binding;
		}
		
		@OnlyIn(Dist.CLIENT) private static KeyMapping reg(
		  RegisterKeyMappingsEvent event, String name, KeyModifier modifier, int keyCode
		) {
			KeyMapping binding = new KeyMapping(MOD_ID + ".key." + name, GUI, modifier, InputConstants.Type.KEYSYM.getOrCreate(keyCode), CATEGORY);
			event.register(binding);
			return binding;
		}
	}
}
