package endorh.simpleconfig.clothconfig2.gui;

import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.clothconfig2.gui.Icon.IconBuilder;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

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
	  CLOSE_SEARCH = b.create(120, 128),
	  SEARCH = b.create(140, 128),
	  RESET = b.create(0, 128),
	  RESET_GROUP = b.create(20, 128),
	  RESTORE = b.create(40, 128),
	  RESTORE_GROUP = b.create(60, 128),
	  CLIENT = b.create(0, 188),
	  SERVER = b.create(20, 188),
	  EDIT_FILE = b.create(80, 188),
	  MERGE_ACCEPT = b.create(100, 188),
	  MERGE_ACCEPT_GROUP = b.create(120, 188),
	  CHESS_BOARD = b.create(0, 64),
	  // Size 15×15
	  ERROR = b.withLevelOffset(0, 0).withSize(15, 15).create(24, 36),
	  WARNING = b.create(39, 36),
	  INFO = b.create(54, 36),
	  CHECKMARK = b.create(69, 36),
	  ELLIPSIS = b.create(84, 36),
	  // Size 18×18
	  UP = b.withLevelOffset(null, null).withSize(18, 18).create(238, 128),
	  DOWN = b.create(220, 128),
	  CHECKBOX = b.create(238, 64),
	  CHECKBOX_FLAT = b.create(220, 64),
	  SEARCH_TOOLTIPS = b.create(202, 188),
	  SEARCH_CASE_SENSITIVE = b.create(238, 188),
	  SEARCH_REGEX = b.create(220, 188),
	  SEARCH_FILTER = b.create(184, 188),
	  // Size 20×20
	  MERGE = b.withLevelOffset(0, 0).withSize(20, 20).create(112, 20),
	  MERGE_ACCEPTED = MERGE.withTint(0xFF80F090),
	  MERGE_CONFLICT = MERGE.withTint(0xFFF0BD80),
	  CLOSE_X = b.create(112, 40),
	  // Size 16×16
	  CLIENT_ICON = b.withLevelOffset(null, null).withSize(16, 16).create(174, 0),
	  SERVER_ICON = b.create(174, 16),
	  // Size 14×14
	  PRESET_CLIENT_LOCAL = b.withSize(14, 14).create(132, 0),
	  PRESET_CLIENT_REMOTE = b.create(132, 14),
	  PRESET_CLIENT_SAVE = b.create(132, 28),
	  PRESET_SERVER_LOCAL = b.create(146, 0),
	  PRESET_SERVER_REMOTE = b.create(146, 14),
	  PRESET_SERVER_SAVE = b.create(146, 28),
	  // Size 12×18
	  LEFT_TAB = b.withSize(12, 18).create(12, 0),
	  RIGHT_TAB = b.create(0, 0),
	  // Size 40×20
	  CONFIRM_DRAG_LEFT = b.withSize(40, 20).create(216, 0),
	  CONFIRM_DRAG_RIGHT = b.create(216, 0),
	  // Size 10×10
	  DROP_DOWN_ARROW = b.withSize(10, 10).create(116, 64),
	  // Two level
	  // Size 18×18
	  COPY = b.twoLevel(true).withSize(18, 18).create(128, 64),
	  KEYBOARD = b.create(146, 64),
	  GEAR = b.create(164, 64),
	  SELECT_ALL = b.create(182, 64),
	  INVERT_SELECTION = b.create(200, 64);
	
	public static class Lists {
		private static final IconBuilder b = IconBuilder.ofTexture(new ResourceLocation(
		  SimpleConfigMod.MOD_ID, "textures/gui/cloth_config.png"), 256, 256);
		
		public static final Icon
		  // Size 9×9
		  LIST_CARET = b.withSize(9, 9).create(93, 0),
		  UP_DOWN_ARROWS = b.create(84, 0),
		  ADD = b.create(42, 0),
		  REMOVE = b.create(51, 0),
		  // Size 12×9
		  INSERT_ARROW = b.withSize(12, 9).create(60, 0),
		  DELETE_ARROW = b.create(72, 0),
		  // Size 7×4
		  UP_ARROW = b.withLevelOffset(0, 9).withSize(7, 4).create(85, 0),
		  DOWN_ARROW = b.create(85, 5);
	}
	
	public static class Entries {
		private static final IconBuilder b = IconBuilder.ofTexture(new ResourceLocation(
		  SimpleConfigMod.MOD_ID, "textures/gui/cloth_config.png"), 256, 256);
		
		@SuppressWarnings("ConstantConditions")
		public static final Icon
		  // Size 9×9
		  GROUP_ARROW = b.withSize(9, 9).create(33, 0),
		  GROUP_PLUS = b.create(24, 0),
		  TEXT_EXPAND = b.create(93, 0),
		  SLIDER_EDIT = b.create(102, 0),
		  // Size 14×14
		  HELP = b.withSize(14, 14).create(160, 0),
		  REQUIRES_RESTART = b.create(160, 14),
		  NOT_PERSISTENT = b.create(160, 28),
		  // Size 20×20
		  HELP_SEARCH = b.withLevelOffset(0, 0).withSize(20, 20).create(112, 0),
		  HELP_SEARCH_MATCH = HELP_SEARCH.withTint(TextFormatting.YELLOW.getColor() | 0xFF000000),
		  HELP_SEARCH_FOCUSED_MATCH = HELP_SEARCH.withTint(TextFormatting.GOLD.getColor() | 0xFF000000),
		  // Size 18×18
		  LESS_EQUAL = b.withLevelOffset(null, null).withSize(18, 18).create(190, 0);
	}
	
	public static class ColorPicker {
		private static final IconBuilder b = IconBuilder.ofTexture(new ResourceLocation(
		  SimpleConfigMod.MOD_ID, "textures/gui/cloth_config.png"), 256, 256);
		
		public static final Icon
		  // Size 40×40
		  CHESS_BOARD = b.withSize(40, 40).create(0, 64),
		  DIAGONAL_TEXTURE = b.create(40, 64),
		  // Size 5×7
		  ARROW_RIGHT = b.withSize(5, 7).create(80, 64),
		  ARROW_LEFT = b.create(85, 64),
		  // Size 11×11
		  POINTER = b.withSize(11, 11).create(80, 78);
	}
	
	public static final AnimatedIcon SPINNING_CUBE = new AnimatedIcon(
	  new ResourceLocation(SimpleConfigMod.MOD_ID, "textures/gui/cube.png"),
	  0, 0, 20, 20, 160, 20, 1, 8, 40);
}
