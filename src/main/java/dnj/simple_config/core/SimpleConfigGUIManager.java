package dnj.simple_config.core;

import dnj.simple_config.SimpleConfigMod;
import dnj.simple_config.SimpleConfigMod.MenuButtonPosition;
import dnj.simple_config.SimpleConfigMod.Config;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
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

import static dnj.simple_config.SimpleConfigMod.prefix;
import static java.util.Collections.synchronizedMap;
import static java.util.Collections.synchronizedSet;

/**
 * Handle the creation of config GUIs for the registered mods<br>
 * Mod configs are automatically registered upon building
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(value = Dist.CLIENT, modid = SimpleConfigMod.MOD_ID)
public class SimpleConfigGUIManager {
	// Mod loading is asynchronous
	protected static final Map<String, Set<SimpleConfig>> modConfigs = synchronizedMap(new HashMap<>());
	
	protected static boolean addButton = false;
	protected static boolean autoAddButton = false;
	
	/**
	 * Modify the behaviour that adds a side button to the pause menu
	 * to open the mod list screen<br>
	 * This is exposed so mods which add their own buttons to the pause
	 * menu and have already this mod as a dependency can toggle this
	 * off to avoid interferences<br>
	 * If your mod doesn't mess with the pause menu, you should not
	 * call this method
	 */
	@SuppressWarnings("unused")
	public void setAddButton(boolean add) {
		addButton = add;
		autoAddButton = true;
	}
	
	/**
	 * Register a config in the GUI system
	 */
	protected static void registerConfig(SimpleConfig config) {
		if (!autoAddButton)
			autoAddButton = addButton = true;
		final ModLoadingContext context = ModLoadingContext.get();
		String modId = context.getActiveContainer().getModId();
		if (!modConfigs.containsKey(modId)) {
			modConfigs.computeIfAbsent(modId, s -> synchronizedSet(new HashSet<>())).add(config);
			context.registerExtensionPoint(
			  ExtensionPoint.CONFIGGUIFACTORY,
			  () -> (mc, screen) -> getConfigGUI(modId, screen));
		} else {
			modConfigs.get(modId).add(config);
		}
	}
	
	/**
	 * Build a config gui for the specified mod id
	 * @param parent Parent screen to return to
	 */
	public static Screen getConfigGUI(String modId, Screen parent) {
		Set<SimpleConfig> configs = modConfigs.get(modId);
		if (configs == null || configs.isEmpty())
			throw new IllegalArgumentException(
			  "There's not any config GUI registered for mod id: \"" + modId + "\"");
		final ConfigBuilder builder = ConfigBuilder.create()
		  .setParentScreen(parent)
		  .setSavingRunnable(() -> configs.stream()
		    .filter(c -> c.dirty).forEach(SimpleConfig::save))
		  .setTitle(new TranslationTextComponent(
		    "simple-config.config.title", SimpleConfig.getModNameOrId(modId)))
		  .setDefaultBackgroundTexture(new ResourceLocation(
		  "textures/block/oak_planks.png"));
		final List<SimpleConfig> orderedConfigs = configs.stream().sorted(Comparator.comparing(c -> {
			switch (c.type) {
				case CLIENT: return 1;
				case COMMON: return 2;
				case SERVER: return 4;
				default: return 3;
			}
		})).collect(Collectors.toList());
		for (SimpleConfig config : orderedConfigs)
			config.decorate(builder);
		final Minecraft mc = Minecraft.getInstance();
		final boolean isOperator = mc.player != null && mc.player.hasPermissionLevel(2);
		for (SimpleConfig config : orderedConfigs) {
			if (config.type != Type.SERVER || isOperator) {
				config.buildGUI(builder);
			}
		}
		builder.transparentBackground();
		return builder.build();
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
		if (!addButton || !Config.add_pause_menu_button)
			return;
		final Screen gui = event.getGui();
		if (gui instanceof IngameMenuScreen) {
			// Coordinates taken from IngameMenuScreen#addButtons
			int w = 20, h = 20, x = gui.width / 2 - 102 - w - 4, y = gui.height / 4 + 96 + -16;
			
			final Optional<Button> opt = getOptionsButton(gui, event.getWidgetList());
			if (Config.menu_button_position == MenuButtonPosition.SPLIT_OPTIONS_BUTTON
			    && opt.isPresent()) {
				Button options = opt.get();
				options.setWidth(options.getWidth() - 20 - 4);
				x = gui.width / 2 - 102 + 98 - 20;
			}
			Button modOptions = new ImageButton(
			  x, y, w, h, 0, 0, 20, prefix("textures/gui/menu.png"),
			  32, 64, p -> showModListGUI()
			);
			event.addWidget(modOptions);
		}
	}
	
	/**
	 * Try to find the Options button in the game menu<br>
	 * Checks its position and size before returning, so it returns
	 * empty if the button does not match the expected placement<br>
	 * This prevents adding the mod config button if the pause menu's
	 * layout changes in future updates or due to other mods,
	 * instead of potentially breaking the pause menu
	 */
	public static Optional<Button> getOptionsButton(Screen gui, List<Widget> widgets) {
		final int x = gui.width / 2 - 102, y = gui.height / 4 + 96 + -16;
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
