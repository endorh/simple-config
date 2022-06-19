package endorh.simpleconfig.clothconfig2.gui;

import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.clothconfig2.gui.Icon.IconBuilder;
import net.minecraft.util.ResourceLocation;

public class SimpleConfigIcons {
	private static final IconBuilder b = IconBuilder.ofTexture(new ResourceLocation(
	  SimpleConfigMod.MOD_ID, "textures/gui/cloth_config.png"), 256, 256);
	
	// The order matters, since the builder is mutable
	public static final Icon
	  // Size 20×20
	  UNDO = b.withSize(20, 20).create(80, 128),
	  REDO = b.create(100, 128),
	  ACCEPT = b.create(160, 128),
	  LOAD = b.create(180, 128),
	  SAVE = b.create(200, 128),
	  SAVE_REMOTE = b.create(40, 188),
	  DELETE = b.create(60, 188),
	  CLOSE = b.create(120, 128),
	  SEARCH = b.create(140, 128),
	  RESET = b.create(0, 128),
	  RESET_GROUP = b.create(20, 128),
	  RESTORE = b.create(40, 128),
	  RESTORE_GROUP = b.create(60, 128),
	  CLIENT = b.create(0, 188),
	  SERVER = b.create(20, 188),
	  EDIT_FILE = b.create(80, 188),
	  CHESS_BOARD = b.create(0, 64),
	  // Size 18×18
	  UP = b.withSize(18, 18).create(238, 128),
	  DOWN = b.create(220, 128),
	  CHECKBOX = b.create(238, 64),
	  SEARCH_TOOLTIPS = b.create(202, 188),
	  SEARCH_CASE_SENSITIVE = b.create(238, 188),
	  SEARCH_REGEX = b.create(220, 188),
	  // Size 14×14
	  PRESET_CLIENT_LOCAL = b.withSize(14, 14).create(138, 0),
	  PRESET_CLIENT_REMOTE = b.create(138, 14),
	  PRESET_CLIENT_SAVE = b.create(138, 28),
	  PRESET_SERVER_LOCAL = b.create(152, 0),
	  PRESET_SERVER_REMOTE = b.create(152, 14),
	  PRESET_SERVER_SAVE = b.create(152, 28),
	  // Size 12×18
	  LEFT_TAB = b.withSize(12, 18).create(12, 0),
	  RIGHT_TAB = b.create(0, 0),
	  // Two level
	  // Size 18×18
	  COPY = b.twoLevel(true).withSize(18, 18).create(128, 64),
	  KEYBOARD = b.create(146, 64);
	
	public static final AnimatedIcon SPINNING_CUBE = new AnimatedIcon(
	  new ResourceLocation(SimpleConfigMod.MOD_ID, "textures/gui/cube.png"),
	  0, 0, 20, 20, 160, 20, 1, 8, 40);
}
