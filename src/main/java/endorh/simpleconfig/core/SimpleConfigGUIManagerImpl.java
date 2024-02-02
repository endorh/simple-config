package endorh.simpleconfig.core;

import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.api.SimpleConfig;
import endorh.simpleconfig.api.SimpleConfig.Type;
import endorh.simpleconfig.api.SimpleConfigGUIManager;
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
import net.minecraft.client.gui.screen.IngameMenuScreen;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.OptionsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.client.gui.screen.ModListScreen;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static endorh.simpleconfig.api.SimpleConfigTextUtil.splitTtc;
import static java.util.Collections.synchronizedMap;

/**
 * Handle the creation of config GUIs for the registered mods<br>
 * Mod configs are automatically registered upon building.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(value = Dist.CLIENT, modid = SimpleConfigMod.MOD_ID)
public final class SimpleConfigGUIManagerImpl implements SimpleConfigGUIManager {
	private static final Logger LOGGER = LogManager.getLogger();
	public static final SimpleConfigGUIManagerImpl INSTANCE = new SimpleConfigGUIManagerImpl();
	private static final String MINECRAFT_MOD_ID = "minecraft";
	
	// Mod loading is asynchronous
	private final Map<String, Map<Type, SimpleConfigImpl>> modConfigs = synchronizedMap(new HashMap<>());
	private final Map<String, AbstractConfigScreen> activeScreens = new HashMap<>();
	
	private boolean addButton = false;
	private boolean autoAddedButton = false;
	private Comparator<SimpleConfigImpl> typeOrder = Comparator.comparing(SimpleConfigImpl::getType);
	private ResourceLocation defaultBackground = new ResourceLocation("textures/block/oak_planks.png");
	
	// Used to evict unbound `ExtendedKeyBind`s for overlap checks to work properly
	private int guiSession;
	private final Map<String, Integer> guiSessions = new HashMap<>();
	private final Map<String, IConfigScreenGUIState> guiStates = new HashMap<>();
	
	private SimpleConfigGUIManagerImpl() {}
	
	@Internal public int getGuiSession() {
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
	@Override public void setAddButton(boolean add) {
		addButton = add;
		autoAddedButton = true;
	}
	
	/**
	 * Check if a config GUI exists for a mod.
	 */
	@Override public boolean hasConfigGUI(String modId) {
		return modConfigs.containsKey(modId);
	}
	
	/**
	 * Register a config in the GUI system
	 */
	protected void registerConfig(SimpleConfigImpl config) {
		String modId = config.getModId();
		ModContainer container = config.getModContainer();
		Optional<BiFunction<Minecraft, Screen, Screen>> ext = container.getCustomExtension(ExtensionPoint.CONFIGGUIFACTORY);
		if (config.isWrapper() && (
		  !CommonConfig.menu.shouldWrapConfig(modId)
		  || ext.isPresent()
		     && !(ext.get() instanceof ConfigGUIFactory)
		     && !CommonConfig.menu.shouldReplaceMenu(modId)
		)) return;
		if (!autoAddedButton)
			autoAddedButton = addButton = true;
		if (!modConfigs.containsKey(modId)) {
			modConfigs.computeIfAbsent(modId, s -> synchronizedMap(new HashMap<>())).put(
			  config.getType(), config);
			container.registerExtensionPoint(
			  ExtensionPoint.CONFIGGUIFACTORY, () -> new ConfigGUIFactory(modId));
		} else modConfigs.get(modId).put(config.getType(), config);
	}
	
	private void reRegisterMenus() {
		ModList.get().forEachModContainer((modId, container) -> {
			if (modConfigs.containsKey(modId)) {
				container.getCustomExtension(ExtensionPoint.CONFIGGUIFACTORY).ifPresent(f -> {
					if (!(f instanceof ConfigGUIFactory))
						container.registerExtensionPoint(
						  ExtensionPoint.CONFIGGUIFACTORY, () -> new ConfigGUIFactory(modId));
				});
			}
		});
	}
	
	/**
	 * Used for marking instead of an anonymous lambda.
	 */
	private static class ConfigGUIFactory implements BiFunction<Minecraft, Screen, Screen> {
		private final String modId;
		private ConfigGUIFactory(String modId) {
			this.modId = modId;
		}
		@Override public Screen apply(Minecraft minecraft, Screen screen) {
			Screen gui = INSTANCE.getConfigGUI(modId, screen);
			return gui != null? gui : screen;
		}
		public String getModId() {
			return modId;
		}
	}
	
	public Screen getNoServerDialogScreen(Screen parent) {
		return new DialogScreen(parent, InfoDialog.create(
		  new TranslationTextComponent("simpleconfig.error.no_server.dialog.title"),
		  splitTtc("simpleconfig.error.no_server.dialog.body"), d -> d.setConfirmText(new TranslationTextComponent("gui.ok"))
		));
	}
	
	public Screen getConfigGUIForHotKey(
	  String modId, IDialogCapableScreen parent, HotKeyListDialog hotKeyDialog, ConfigHotKey hotkey
	) {
		AbstractConfigScreen screen = activeScreens.get(modId);
		Screen parentScreen = (Screen) parent;
		if (screen != null) {
			int prevSession = guiSession;
			guiSession = guiSessions.get(modId);
			screen.setEditedConfigHotKey(hotkey, r -> {
				screen.setEditedConfigHotKey(null, null);
				Minecraft.getInstance().displayGuiScreen(parentScreen);
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
		  .sorted(typeOrder)
		  .collect(Collectors.toList());
		if (orderedConfigs.isEmpty()) return getNoServerDialogScreen(parentScreen);
		final ConfigScreenBuilder builder = ConfigScreenBuilder.create(modId)
		  .setParentScreen(parentScreen)
		  .setSavingRunnable(() -> {})
		  .setTitle(new TranslationTextComponent(
		    "simpleconfig.config.title", SimpleConfigImpl.getModNameOrId(modId)))
		  .setDefaultBackgroundTexture(defaultBackground)
		  .setPreviousGUIState(guiStates.get(modId))
		  .setEditedConfigHotKey(hotkey, r -> {
			  AbstractConfigScreen removed = activeScreens.remove(modId);
			  guiStates.put(modId, removed.saveConfigScreenGUIState());
			  guiSessions.remove(modId);
			  if (configs.containsKey(SimpleConfig.Type.COMMON)
			      && !Minecraft.getInstance().isIntegratedServerRunning()
			      && hasPermission
			  ) new CSimpleConfigReleaseServerCommonConfigPacket(modId).send();
			  for (SimpleConfigImpl c: orderedConfigs) c.removeGUI();
			  Minecraft.getInstance().displayGuiScreen(parentScreen);
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
	@Override public @Nullable Screen getConfigGUI(String modId, Screen parent) {
		Map<Type, SimpleConfigImpl> configs = modConfigs.get(modId);
		if (configs == null || configs.isEmpty())
			throw new IllegalArgumentException(
			  "No Simple Config GUI registered for mod id: \"" + modId + "\"");
		final Minecraft mc = Minecraft.getInstance();
		boolean hasPermission =
		  mc.player != null && ServerConfig.permissions.permissionFor(mc.player, modId).getLeft().canView();
		final List<SimpleConfigImpl> orderedConfigs = configs.values().stream()
		  .filter(c -> c.getType() != Type.SERVER || hasPermission)
		  .sorted(typeOrder)
		  .collect(Collectors.toList());
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
			      && !Minecraft.getInstance().isIntegratedServerRunning()
			      && hasPermission
			  ) new CSimpleConfigReleaseServerCommonConfigPacket(modId).send();
			  for (SimpleConfigImpl c: orderedConfigs) c.removeGUI();
		  }).setTitle(new TranslationTextComponent(
			 "simpleconfig.config.title", SimpleConfigImpl.getModNameOrId(modId)))
		  .setDefaultBackgroundTexture(defaultBackground)
		  .setSnapshotHandler(handler)
		  .setRemoteCommonConfigProvider(handler);
		for (SimpleConfigImpl config : orderedConfigs) {
			config.buildGUI(builder, false);
			if (config.getType() == SimpleConfig.Type.COMMON
			    && !Minecraft.getInstance().isIntegratedServerRunning()
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
	@Override public @Nullable Screen getConfigGUI(String modId) {
		return getConfigGUI(modId, Minecraft.getInstance().currentScreen);
	}
	
	/**
	 * Show the config GUI for the specified mod id, using the current screen as parent
	 */
	@Override public void showConfigGUI(String modId) {
		Screen screen = getConfigGUI(modId);
		if (screen != null) Minecraft.getInstance().displayGuiScreen(screen);
	}
	
	public void showConfigGUIForHotKey(
	  String modId, IDialogCapableScreen parent, HotKeyListDialog hotKeyDialog, ConfigHotKey hotKey
	) {
		Minecraft.getInstance().displayGuiScreen(
		  getConfigGUIForHotKey(modId, parent, hotKeyDialog, hotKey));
	}
	
	/**
	 * Show the Forge mod list GUI
	 */
	@Override public void showModListGUI() {
		final Minecraft mc = Minecraft.getInstance();
		mc.displayGuiScreen(new ModListScreen(mc.currentScreen));
	}
	
	/**
	 * Show the Config Hotkey GUI
	 */
	@Override public void showConfigHotkeysGUI() {
		Minecraft mc = Minecraft.getInstance();
		mc.displayGuiScreen(new DialogScreen(mc.currentScreen, new HotKeyListDialog(null)));
	}
	
	/**
	 * Try to find the Options button in the game menu<br>
	 * Can check its position and size before returning, so it returns
	 * empty if the button does not match the expected placement<br>
	 */
	public static Optional<Button> getOptionsButton(
	  Screen gui, List<Widget> widgets, boolean checkDimensions
	) {
		int x = gui.width / 2 - (gui instanceof MainMenuScreen? 100 : 102);
		int y =
		  gui instanceof MainMenuScreen? gui.height / 4 + 48 + 72 + 12 : gui.height / 4 + 96 - 16;
		int width = 98;
		Optional<Button> opt = widgets.stream()
		  .filter(l -> l instanceof Button).map(l -> (Button) l)
			.filter(b -> {
			  ITextComponent m = b.getMessage();
			  return m instanceof TranslationTextComponent
			         && "menu.options".equals(((TranslationTextComponent) m).getKey());
			}).findFirst()
			.filter(b -> !checkDimensions || b.x == x && b.y == y && b.getWidth() == width);
		if (!opt.isPresent())
			LOGGER.debug("Couldn't find options button in " + gui.getClass().getCanonicalName());
		return opt;
	}
	
	private Button lastOptionsButton = null;
	
	@EventBusSubscriber(value=Dist.CLIENT, modid=SimpleConfigMod.MOD_ID)
	private static class EventSubscriber {
		/**
		 * Adds a minimal button to the pause menu to open the mod list in-game.<br>
		 * This behaviour can be disabled in the config, in case it interferes with
		 * another mod
		 */
		@SubscribeEvent
		public static void onGuiInit(InitGuiEvent.Post event) {
			if ((!INSTANCE.addButton || !menu.add_pause_menu_button) && menu.options_button_behaviour == OptionsButtonBehaviour.DEFAULT)
				return;
			final Screen gui = event.getGui();
			boolean hasMinecraftOptionsGui = INSTANCE.hasConfigGUI(MINECRAFT_MOD_ID);
			if (gui instanceof IngameMenuScreen) {
				if (hasMinecraftOptionsGui)
					getOptionsButton(gui, event.getWidgetList(), false)
					  .ifPresent(b -> INSTANCE.lastOptionsButton = b);
				if (INSTANCE.addButton && menu.add_pause_menu_button) {
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
							Optional<Button> opt = getOptionsButton(gui, event.getWidgetList(), true);
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
						32, 64, p -> INSTANCE.showModListGUI());
					event.addWidget(modOptions);
				}
			} else if (gui instanceof MainMenuScreen && hasMinecraftOptionsGui) {
				getOptionsButton(gui, event.getWidgetList(), false)
				  .ifPresent(b -> INSTANCE.lastOptionsButton = b);
			} else if (
				gui instanceof OptionsScreen
				&& INSTANCE.addButton && menu.add_options_menu_button
				&& hasMinecraftOptionsGui
			) {
				MultiFunctionImageButton b = MultiFunctionImageButton.of(
				  Buttons.GEAR, ButtonAction.of(
					 () -> Minecraft.getInstance().displayGuiScreen(
					   INSTANCE.getConfigGUI(MINECRAFT_MOD_ID, gui))
				  ).tooltip(splitTtc("simpleconfig.ui.edit_with_simpleconfig")));
				switch (menu.options_menu_button_position) {
					case TOP_LEFT_CORNER:
						b.setPosition(8, 8);
						break;
					case TOP_RIGHT_CORNER:
						b.setPosition(gui.width - b.getWidth() - 8, 8);
						break;
					case BOTTOM_LEFT_CORNER:
						b.setPosition(8, gui.height - b.getHeightRealms() - 8);
						break;
					case BOTTOM_RIGHT_CORNER:
						b.setPosition(gui.width - b.getWidth() - 8, gui.height - b.getHeightRealms() - 8);
						break;
				}
				event.addWidget(b);
			}
		}
		
		@SubscribeEvent
		public static void onButtonClick(GuiScreenEvent.MouseClickedEvent.Pre event) {
			if (menu.options_button_behaviour == OptionsButtonBehaviour.DEFAULT ||
			    INSTANCE.lastOptionsButton == null) return;
			Screen screen = Minecraft.getInstance().currentScreen;
			if ((screen instanceof IngameMenuScreen || screen instanceof MainMenuScreen)
			    && INSTANCE.lastOptionsButton.isMouseOver(event.getMouseX(), event.getMouseY())) {
				if (menu.options_button_behaviour == OptionsButtonBehaviour.MAIN_CLICK
				    ? event.getButton() != 1 && !Screen.hasShiftDown()
				    : event.getButton() == 1 || Screen.hasShiftDown()) {
					if (INSTANCE.hasConfigGUI(MINECRAFT_MOD_ID)) {
						INSTANCE.showConfigGUI(MINECRAFT_MOD_ID);
						event.setCanceled(true);
					}
				}
			}
		}
	}
	
	@EventBusSubscriber(value=Dist.CLIENT, bus=Bus.MOD, modid=SimpleConfigMod.MOD_ID)
	private static class ModEventSubscriber {
		@SubscribeEvent(priority=EventPriority.LOWEST)
		public static void onLoadComplete(FMLLoadCompleteEvent event) {
			if (CommonConfig.menu.prevent_external_menu_replacement)
				event.enqueueWork(INSTANCE::reRegisterMenus);
		}
	}
}
