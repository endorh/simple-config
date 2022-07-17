package endorh.simpleconfig.core;

import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.SimpleConfigMod.ClientConfig;
import endorh.simpleconfig.SimpleConfigMod.ConfigPermission;
import endorh.simpleconfig.SimpleConfigMod.ServerConfig;
import endorh.simpleconfig.ui.api.ConfigBuilder;
import endorh.simpleconfig.ui.gui.AbstractConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.IngameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.gui.screen.ModListScreen;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.config.ModConfig.Type;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.synchronizedMap;

/**
 * Handle the creation of config GUIs for the registered mods<br>
 * Mod configs are automatically registered upon building.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(value = Dist.CLIENT, modid = SimpleConfigMod.MOD_ID)
public class SimpleConfigGUIManager {
	// Mod loading is asynchronous
	protected static final Map<String, Map<Type, SimpleConfig>> modConfigs = synchronizedMap(new HashMap<>());
	
	protected static boolean addButton = false;
	protected static boolean autoAddedButton = false;
	
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
	 * Register a config in the GUI system
	 */
	protected static void registerConfig(SimpleConfig config) {
		if (!autoAddedButton)
			autoAddedButton = addButton = true;
		final ModLoadingContext context = ModLoadingContext.get();
		String modId = context.getActiveContainer().getModId();
		if (!modConfigs.containsKey(modId)) {
			modConfigs.computeIfAbsent(modId, s -> synchronizedMap(new HashMap<>())).put(
			  config.getType(), config);
			context.registerExtensionPoint(
			  ExtensionPoint.CONFIGGUIFACTORY,
			  () -> (mc, screen) -> getConfigGUI(modId, screen));
		} else {
			modConfigs.get(modId).put(config.getType(), config);
		}
	}
	
	/**
	 * Build a config gui for the specified mod id
	 * @param parent Parent screen to return to
	 */
	public static Screen getConfigGUI(String modId, Screen parent) {
		Map<Type, SimpleConfig> configs = modConfigs.get(modId);
		if (configs == null || configs.isEmpty())
			throw new IllegalArgumentException(
			  "There's not any config GUI registered for mod id: \"" + modId + "\"");
		final Minecraft mc = Minecraft.getInstance();
		boolean hasPermission =
		  mc.player != null && ServerConfig.permissions.permissionFor(mc.player, modId) == ConfigPermission.ALLOW;
		final List<SimpleConfig> orderedConfigs = configs.values().stream()
		  .filter(c -> c.getType() != Type.SERVER || hasPermission)
		  .sorted(Comparator.comparing(c -> {
			switch (c.getType()) {
				case CLIENT: return 1;
				case COMMON: return 2;
				case SERVER: return 4;
				default: return 3;
			}
		})).collect(Collectors.toList());
		final SimpleConfigSnapshotHandler handler = new SimpleConfigSnapshotHandler(configs);
		final ConfigBuilder builder = ConfigBuilder.create(modId)
		  .setParentScreen(parent)
		  .setSavingRunnable(() -> orderedConfigs.forEach(c -> {
			  if (c.isDirty()) c.save();
			  c.removeGUI();
		  })).setTitle(new TranslationTextComponent(
		    "simpleconfig.config.title", SimpleConfig.getModNameOrId(modId)))
		  .setDefaultBackgroundTexture(new ResourceLocation(
		  "textures/block/oak_planks.png"))
		  .setSnapshotHandler(handler);
		for (SimpleConfig config : orderedConfigs)
			config.buildGUI(builder);
		final AbstractConfigScreen gui = builder.build();
		for (SimpleConfig config : orderedConfigs)
			config.setGUI(gui, handler);
		return gui;
	}
	
	/**
	 * Build a config GUI for the specified mod id, using the current screen as parent
	 */
	public static Screen getConfigGUI(String modId) {
		return getConfigGUI(modId, Minecraft.getInstance().currentScreen);
	}
	
	/**
	 * Show the config GUI for the specified mod id, using the current screen as parent
	 */
	@SuppressWarnings("unused")
	public static void showConfigGUI(String modId) {
		Minecraft.getInstance().displayGuiScreen(getConfigGUI(modId));
	}
	
	/**
	 * Show the Forge mod list GUI
	 */
	public static void showModListGUI() {
		final Minecraft mc = Minecraft.getInstance();
		mc.displayGuiScreen(new ModListScreen(mc.currentScreen));
	}
	
	/**
	 * Adds a minimal button to the pause menu to open the mod list ingame.<br>
	 * This behaviour can be disabled in the config, in case it interferes with
	 * another mod
	 */
	@SubscribeEvent
	public static void onGuiInit(InitGuiEvent.Post event) {
		if (!addButton || !ClientConfig.add_pause_menu_button)
			return;
		final Screen gui = event.getGui();
		if (gui instanceof IngameMenuScreen) {
			// Coordinates taken from IngameMenuScreen#addButtons
			int w = 20, h = 20, x, y;
			switch (ClientConfig.menu_button_position) {
				case TOP_LEFT_CORNER:
					x = 8; y = 8; break;
				case TOP_RIGHT_CORNER:
					x = gui.width - 28; y = 8; break;
				case BOTTOM_LEFT_CORNER:
					x = 8; y = gui.height - 28; break;
				case BOTTOM_RIGHT_CORNER:
					x = gui.width - 28; y = gui.height - 28; break;
				case SPLIT_OPTIONS_BUTTON:
					Optional<Button> opt = getOptionsButton(gui, event.getWidgetList());
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
			  new ResourceLocation(SimpleConfigMod.MOD_ID, "textures/gui/simple_config/menu.png"),
			  32, 64, p -> showModListGUI()
			);
			event.addWidget(modOptions);
		}
	}
	
	/**
	 * Try to find the Options button in the game menu<br>
	 * Checks its position and size before returning, so it returns
	 * empty if the button does not match the expected placement<br>
	 */
	public static Optional<Button> getOptionsButton(Screen gui, List<Widget> widgets) {
		final int x = gui.width / 2 - 102, y = gui.height / 4 + 96 - 16;
		for (Widget widget : widgets) {
			if (widget instanceof Button) {
				Button but = (Button) widget;
				if (but.getMessage().getString().equals(I18n.format("menu.options"))) {
					if (but.x == x && but.y == y && but.getWidth() == 98) {
						return Optional.of(but);
					}
				}
			}
		}
		return Optional.empty();
	}
}
