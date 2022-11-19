package endorh.simpleconfig.core;

import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.api.SimpleConfig;
import endorh.simpleconfig.api.SimpleConfig.Type;
import endorh.simpleconfig.api.SimpleConfigTextUtil;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons.Buttons;
import endorh.simpleconfig.config.ClientConfig.OptionsButtonBehaviour;
import endorh.simpleconfig.config.ClientConfig.menu;
import endorh.simpleconfig.config.CommonConfig;
import endorh.simpleconfig.config.ServerConfig;
import endorh.simpleconfig.core.SimpleConfigNetworkHandler.CSimpleConfigReleaseServerCommonConfigPacket;
import endorh.simpleconfig.ui.api.ConfigScreenBuilder;
import endorh.simpleconfig.ui.api.ConfigScreenBuilder.IConfigScreenGUIState;
import endorh.simpleconfig.ui.api.IDialogCapableScreen;
import endorh.simpleconfig.ui.gui.AbstractConfigScreen;
import endorh.simpleconfig.ui.gui.DialogScreen;
import endorh.simpleconfig.ui.gui.InfoDialog;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import endorh.simpleconfig.ui.hotkey.ConfigHotKey;
import endorh.simpleconfig.ui.hotkey.HotKeyListDialog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.ModListScreen;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper.UnableToAccessFieldException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;

import static java.util.Collections.synchronizedMap;

/**
 * Handle the creation of config GUIs for the registered mods<br>
 * Mod configs are automatically registered upon building.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(value = Dist.CLIENT, modid = SimpleConfigMod.MOD_ID)
public class SimpleConfigGUIManager {
	// Mod loading is asynchronous
	protected static final Map<String, Map<Type, SimpleConfigImpl>> modConfigs = synchronizedMap(new HashMap<>());
	private static final Map<String, AbstractConfigScreen> activeScreens = new HashMap<>();
	private static final Logger LOGGER = LogManager.getLogger();
	
	protected static boolean addButton = false;
	protected static boolean autoAddedButton = false;
	protected static Comparator<SimpleConfigImpl> typeOrder = Comparator.comparing(SimpleConfigImpl::getType);
	protected static ResourceLocation defaultBackground = new ResourceLocation("textures/block/oak_planks.png");
	
	// Used to evict unbound `ExtendedKeyBind`s for overlap checks to work properly
	private static int guiSession;
	private static final Map<String, Integer> guiSessions = new HashMap<>();
	private static final Map<String, IConfigScreenGUIState> guiStates = new HashMap<>();
	private static final String MINECRAFT_MOD_ID = "minecraft";
	
	@Internal public static int getGuiSession() {
		return guiSession;
	}
	
	/**
	 * Modify the behaviour that adds a side button to the pause menu
	 * to open the mod list screen<br>
	 * This is exposed so mods which add their own buttons to the pause
	 * menu and have already this mod as a dependency can toggle this
	 * off to avoid interferences<br>
	 * If your mod doesn't mess with the pause menu, you should not
	 * call this method
	 */
	@SuppressWarnings("unused") public void setAddButton(boolean add) {
		addButton = add;
		autoAddedButton = true;
	}
	
	/**
	 * Check if a config GUI exists for a mod.
	 */
	@Internal public static boolean hasConfigGUI(String modId) {
		return modConfigs.containsKey(modId);
	}
	
	/**
	 * Register a config in the GUI system
	 */
	protected static void registerConfig(SimpleConfigImpl config) {
		String modId = config.getModId();
		ModContainer container = config.getModContainer();
		Optional<BiFunction<Minecraft, Screen, Screen>> ext =
		  ConfigScreenHandler.getScreenFactoryFor(container.getModInfo());
		if (config.isWrapper() && (
		  !CommonConfig.menu.shouldWrapConfig(modId)
		  || ext.isPresent()
		     && !(ext.get() instanceof SimpleConfigGuiFactory)
		     && !CommonConfig.menu.shouldReplaceMenu(modId)
		)) return;
		if (!autoAddedButton)
			autoAddedButton = addButton = true;
		if (!modConfigs.containsKey(modId)) {
			modConfigs.computeIfAbsent(modId, s -> synchronizedMap(new HashMap<>())).put(
			  config.getType(), config);
			
			container.registerExtensionPoint(
			  ConfigScreenFactory.class,
			  () -> new ConfigScreenFactory(new SimpleConfigGuiFactory(modId)));
		} else modConfigs.get(modId).put(config.getType(), config);
	}
	
	private static void reRegisterMenus() {
		ModList.get().forEachModContainer((modId, container) -> {
			if (modConfigs.containsKey(modId)) {
				container.getCustomExtension(ConfigScreenFactory.class).ifPresent(f -> {
					if (!(f.screenFunction() instanceof SimpleConfigGuiFactory))
						container.registerExtensionPoint(
						  ConfigScreenFactory.class,
						  () -> new ConfigScreenFactory(new SimpleConfigGuiFactory(modId)));
				});
			}
		});
	}
	
	@EventBusSubscriber(value=Dist.CLIENT, bus=Bus.MOD, modid=SimpleConfigMod.MOD_ID)
	private static class ModEventSubscriber {
		@SubscribeEvent(priority=EventPriority.LOWEST)
		public static void onLoadComplete(FMLLoadCompleteEvent event) {
			if (CommonConfig.menu.prevent_external_menu_replacement)
				event.enqueueWork(SimpleConfigGUIManager::reRegisterMenus);
		}
	}
	
	/**
	 * Used for marking instead of an anonymous lambda.
	 */
	public record SimpleConfigGuiFactory(String modId) implements BiFunction<Minecraft, Screen, Screen> {
		@Override public Screen apply(Minecraft minecraft, Screen screen) {
			Screen gui = getConfigGUI(modId, screen);
			return gui != null? gui : screen;
		}
	}
	
	public static Screen getNoServerDialogScreen(Screen parent) {
		return new DialogScreen(parent, InfoDialog.create(
		  Component.translatable("simpleconfig.error.no_server.dialog.title"),
		  SimpleConfigTextUtil.splitTtc("simpleconfig.error.no_server.dialog.body"), d -> {
			  d.setConfirmText(Component.translatable("gui.ok"));
		  }
		));
	}
	
	public static Screen getConfigGUIForHotKey(
	  String modId, IDialogCapableScreen parent, HotKeyListDialog hotKeyDialog, ConfigHotKey hotkey
	) {
		AbstractConfigScreen screen = activeScreens.get(modId);
		Screen parentScreen = (Screen) parent;
		if (screen != null) {
			int prevSession = guiSession;
			guiSession = guiSessions.get(modId);
			screen.setEditedConfigHotKey(hotkey, r -> {
				screen.setEditedConfigHotKey(null, null);
				Minecraft.getInstance().setScreen(parentScreen);
				if (hotKeyDialog != null) parent.addDialog(hotKeyDialog);
				guiSession = prevSession;
			});
			return screen;
		}
		Map<Type, SimpleConfigImpl> configs = modConfigs.get(modId);
		if (configs == null || configs.isEmpty())
			throw new IllegalArgumentException(
			  "No Simple Config GUI registered for mod id: \"" + modId + "\"");
		final Minecraft mc = Minecraft.getInstance();
		boolean hasPermission =
		  mc.player != null && ServerConfig.permissions.permissionFor(mc.player, modId).getLeft().canView();
		final List<SimpleConfigImpl> orderedConfigs = configs.values().stream()
		  .filter(c -> c.getType() != Type.SERVER || hasPermission)
		  .sorted(typeOrder).toList();
		if (orderedConfigs.isEmpty()) return getNoServerDialogScreen(parentScreen);
		final ConfigScreenBuilder builder = ConfigScreenBuilder.create(modId)
		  .setParentScreen(parentScreen)
		  .setSavingRunnable(() -> {})
		  .setTitle(Component.translatable("simpleconfig.config.title", SimpleConfigImpl.getModNameOrId(modId)))
		  .setDefaultBackgroundTexture(defaultBackground)
		  .setPreviousGUIState(guiStates.get(modId))
		  .setEditedConfigHotKey(hotkey, r -> {
			  AbstractConfigScreen removed = activeScreens.remove(modId);
			  guiStates.put(modId, removed.saveConfigScreenGUIState());
			  guiSessions.remove(modId);
			  if (configs.containsKey(SimpleConfig.Type.COMMON)
			      && !Minecraft.getInstance().isLocalServer()
			      && hasPermission
			  ) new CSimpleConfigReleaseServerCommonConfigPacket(modId).send();
			  for (SimpleConfigImpl c: orderedConfigs) c.removeGUI();
			  Minecraft.getInstance().setScreen(parentScreen);
			  if (hotKeyDialog != null) parent.addDialog(hotKeyDialog);
		  }); //.setClosingRunnable(() -> activeScreens.remove(modId));
		for (SimpleConfigImpl config : orderedConfigs) config.buildGUI(builder, false);
		guiSessions.put(modId, ++guiSession);
		final AbstractConfigScreen gui = builder.build();
		activeScreens.put(modId, gui);
		for (SimpleConfigImpl config : orderedConfigs) config.setGUI(gui, null);
		return gui;
	}
	
	/**
	 * Build a config gui for the specified mod id
	 * @param parent Parent screen to return to
	 */
	public static @Nullable Screen getConfigGUI(String modId, Screen parent) {
		Map<Type, SimpleConfigImpl> configs = modConfigs.get(modId);
		if (configs == null || configs.isEmpty())
			throw new IllegalArgumentException(
			  "No Simple Config GUI registered for mod id: \"" + modId + "\"");
		final Minecraft mc = Minecraft.getInstance();
		boolean hasPermission =
		  mc.player != null && ServerConfig.permissions.permissionFor(mc.player, modId).getLeft().canView();
		final List<SimpleConfigImpl> orderedConfigs = configs.values().stream()
		  .filter(c -> c.getType() != Type.SERVER || hasPermission)
		  .sorted(typeOrder).toList();
		if (orderedConfigs.isEmpty()) return getNoServerDialogScreen(parent);
		final SimpleConfigSnapshotHandler handler = new SimpleConfigSnapshotHandler(configs);
		final ConfigScreenBuilder builder = ConfigScreenBuilder.create(modId)
		  .setParentScreen(parent)
		  .setSavingRunnable(() -> {
			  for (SimpleConfigImpl c: orderedConfigs)
				  if (c.isDirty()) c.save();
		  }).setPreviousGUIState(guiStates.get(modId))
		  .setClosingRunnable(() -> {
			  AbstractConfigScreen removed = activeScreens.remove(modId);
			  guiStates.put(modId, removed.saveConfigScreenGUIState());
			  if (configs.containsKey(SimpleConfig.Type.COMMON)
			      && !Minecraft.getInstance().isLocalServer()
			      && hasPermission
			  ) new CSimpleConfigReleaseServerCommonConfigPacket(modId).send();
			  for (SimpleConfigImpl c: orderedConfigs) c.removeGUI();
		  }).setTitle(Component.translatable("simpleconfig.config.title", SimpleConfigImpl.getModNameOrId(modId)))
		  .setDefaultBackgroundTexture(defaultBackground)
		  .setSnapshotHandler(handler)
		  .setRemoteCommonConfigProvider(handler);
		for (SimpleConfigImpl config : orderedConfigs) {
			config.buildGUI(builder, false);
			if (config.getType() == SimpleConfig.Type.COMMON
			    && !Minecraft.getInstance().isLocalServer()
			    && hasPermission
			) config.buildGUI(builder, true);
		}
		guiSessions.put(modId, ++guiSession);
		final AbstractConfigScreen gui = builder.build();
		activeScreens.put(modId, gui);
		for (SimpleConfigImpl config : orderedConfigs) config.setGUI(gui, handler);
		return gui;
	}
	
	/**
	 * Build a config GUI for the specified mod id, using the current screen as parent
	 */
	public static @Nullable Screen getConfigGUI(String modId) {
		return getConfigGUI(modId, Minecraft.getInstance().screen);
	}
	
	/**
	 * Show the config GUI for the specified mod id, using the current screen as parent
	 */
	@SuppressWarnings("unused")
	public static void showConfigGUI(String modId) {
		Screen screen = getConfigGUI(modId);
		if (screen != null) Minecraft.getInstance().setScreen(screen);
	}
	
	public static void showConfigGUIForHotKey(
	  String modId, IDialogCapableScreen parent, HotKeyListDialog hotKeyDialog, ConfigHotKey hotKey
	) {
		Minecraft.getInstance().setScreen(
		  getConfigGUIForHotKey(modId, parent, hotKeyDialog, hotKey));
	}
	
	/**
	 * Show the Forge mod list GUI
	 */
	public static void showModListGUI() {
		final Minecraft mc = Minecraft.getInstance();
		mc.setScreen(new ModListScreen(mc.screen));
	}
	
	/**
	 * Show the Config Hotkey GUI
	 */
	public static void showConfigHotkeysGUI() {
		Minecraft mc = Minecraft.getInstance();
		mc.setScreen(new DialogScreen(mc.screen, new HotKeyListDialog(null)));
	}
	
	/**
	 * Adds a minimal button to the pause menu to open the mod list in-game.<br>
	 * This behaviour can be disabled in the config, in case it interferes with
	 * another mod
	 */
	@SubscribeEvent
	public static void onGuiInit(ScreenEvent.Init.Post event) {
		if (!addButton || !menu.add_pause_menu_button)
			return;
		final Screen gui = event.getScreen();
		if (gui instanceof PauseScreen) {
			if (hasConfigGUI(MINECRAFT_MOD_ID))
				getOptionsButton(gui, event.getListenersList(), false)
				  .ifPresent(b -> lastOptionsButton = b);
			// Coordinates taken from IngameMenuScreen#addButtons
			int w = 20, h = 20, x, y;
			switch (menu.menu_button_position) {
				case TOP_LEFT_CORNER:
					x = 8;
					y = 8;
					break;
				case TOP_RIGHT_CORNER:
					x = gui.width - 28;
					y = 8;
					break;
				case BOTTOM_LEFT_CORNER:
					x = 8;
					y = gui.height - 28;
					break;
				case BOTTOM_RIGHT_CORNER:
					x = gui.width - 28;
					y = gui.height - 28;
					break;
				case SPLIT_OPTIONS_BUTTON:
					Optional<Button> opt = getOptionsButton(gui, event.getListenersList(), true);
					if (opt.isPresent()) {
						Button options = opt.get();
						options.setWidth(options.getWidth() - 20 - 4);
						// Coordinates taken from IngameMenuScreen#addButtons
						x = gui.width / 2 - 102 + 98 - 20;
						y = gui.height / 4 + 96 - 16;
						break;
					} // else fallthrough
				case LEFT_OF_OPTIONS_BUTTON:
				default:
					// Coordinates taken from IngameMenuScreen#addButtons
					x = gui.width / 2 - 102 - w - 4;
					y = gui.height / 4 + 96 - 16;
			}
			
			Button modOptions = new ImageButton(
			  x, y, w, h, 0, 0, 20,
			  new ResourceLocation(SimpleConfigMod.MOD_ID, "textures/gui/simpleconfig/menu.png"),
			  32, 64, p -> showModListGUI());
			event.addListener(modOptions);
		} else if (gui instanceof TitleScreen && hasConfigGUI(MINECRAFT_MOD_ID)) {
			getOptionsButton(gui, event.getListenersList(), false)
			  .ifPresent(b -> lastOptionsButton = b);
		} else if (gui instanceof OptionsScreen os && hasConfigGUI(MINECRAFT_MOD_ID)) {
			Screen last = getLastScreen(os).orElse(os);
			MultiFunctionImageButton b = MultiFunctionImageButton.of(
			  Buttons.GEAR, ButtonAction.of(
				 () -> Minecraft.getInstance().setScreen(
					getConfigGUI(MINECRAFT_MOD_ID, last))));
			b.setPosition(10, gui.height - 10 - b.getHeight());
			event.addListener(b);
		}
	}
	
	private static Optional<Screen> getLastScreen(OptionsScreen gui) {
		try {
			return Optional.ofNullable(ObfuscationReflectionHelper.getPrivateValue(
			  OptionsScreen.class, gui, "f_96235_"));
		} catch (UnableToAccessFieldException e) {
			LOGGER.error("Couldn't access field OptionsScreen#lastScreen", e);
			return Optional.empty();
		}
	}
	
	/**
	 * Try to find the Options button in the game menu<br>
	 * Can check its position and size before returning, so it returns
	 * empty if the button does not match the expected placement<br>
	 */
	public static Optional<Button> getOptionsButton(
	  Screen gui, List<GuiEventListener> widgets, boolean checkDimensions
	) {
		int x = gui.width / 2 - (gui instanceof TitleScreen? 100 : 102);
		int y = gui instanceof TitleScreen? gui.height / 4 + 48 + 72 + 12 : gui.height / 4 + 96 - 16;
		int width = 98;
		return widgets.stream()
		  .filter(l -> l instanceof Button).map(l -> (Button) l)
		  .filter(b -> {
			  ComponentContents contents = b.getMessage().getContents();
			  return contents instanceof TranslatableContents tc
			         && "menu.options".equals(tc.getKey());
		  }).findFirst()
		  .filter(b -> !checkDimensions || b.x == x && b.y == y && b.getWidth() == width);
	}
	
	private static Button lastOptionsButton = null;
	@SubscribeEvent
	public static void onButtonClick(ScreenEvent.MouseButtonPressed.Pre event) {
		if (menu.options_button_behaviour == OptionsButtonBehaviour.DEFAULT || lastOptionsButton == null) return;
		Screen screen = Minecraft.getInstance().screen;
		if ((screen instanceof PauseScreen || screen instanceof TitleScreen)
		    && lastOptionsButton.isMouseOver(event.getMouseX(), event.getMouseY())) {
			if (menu.options_button_behaviour == OptionsButtonBehaviour.MAIN_CLICK
			    ? event.getButton() != 1 && !Screen.hasShiftDown()
			    : event.getButton() == 1 || Screen.hasShiftDown()) {
				if (hasConfigGUI(MINECRAFT_MOD_ID)) {
					showConfigGUI(MINECRAFT_MOD_ID);
					event.setCanceled(true);
				}
			}
		}
	}
}
