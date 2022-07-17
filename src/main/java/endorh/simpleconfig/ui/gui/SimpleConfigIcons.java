package endorh.simpleconfig.ui.gui;

import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.ui.gui.Icon.IconBuilder;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

public class SimpleConfigIcons {
	private static final ResourceLocation TEXTURE = new ResourceLocation(
	  SimpleConfigMod.MOD_ID, "textures/gui/simple_config/config_menu.png");
	private static final IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256).offset(0, 0);
	
	public static final Icon // Size 16×16
	  CLIENT = b.size(16, 16).at(240, 76),
	  SERVER = b.at(240, 92);
	
	public static class Buttons {
		private static final IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256).offset(0, 136);
		
		public static final Icon // Size 20×20
		  RESET = b.size(20, 20).at(0, 0),
		  RESET_GROUP = b.at(20, 0),
		  RESTORE = b.at(40, 0),
		  RESTORE_GROUP = b.at(60, 0),
		  MERGE_ACCEPT = b.at(80, 0),
		  MERGE_ACCEPT_GROUP = b.at(100, 0),
		  LOAD = b.at(120, 0),
		  SAVE = b.at(140, 0),
		  SAVE_REMOTE = b.at(160, 0),
		  DELETE = b.at(180, 0),
		  EDIT_FILE = b.at(200, 0),
		  SEARCH = b.at(0, 60),
		  SEARCH_CLOSE = b.at(20, 60),
		  UNDO = b.at(40, 60),
		  REDO = b.at(60, 60),
		  ACCEPT = b.at(80, 60);
		
		public static final Icon // Size 20×20
		  DISABLED = b.size(20, 20).at(100, 60),
		  ASSIGN = b.at(120, 60),
		  MULTIPLY = b.at(140, 60),
		  DIVIDE = b.at(160, 60),
		  ADD = b.at(180, 60),
		  SUBTRACT = b.at(200, 60);
		
		public static final Icon // Size 18×18
		  DOWN = b.size(18, 18).at(220, 66),
		  UP = b.at(238, 66);
		
		public static final Icon // Size 18×18
		  GEAR = b.twoLevel(true).offset(0, 76).size(18, 18).at(0, 0),
		  KEYBOARD = b.at(18, 0),
		  COPY = b.at(36, 0),
		  SELECT_ALL = b.at(54, 0),
		  INVERT_SELECTION = b.at(72, 0);
		
		public static final Icon // Size 40×20
		  CONFIRM_DRAG_LEFT = b.twoLevel(false).offset(0, 36).size(40, 20).at(0, 0),
		  CONFIRM_DRAG_RIGHT = b.at(0, 20);
		
		public static final Icon // 12×18
		  LEFT_TAB = b.offset(220, 0).size(12, 18).level(12, 0).at(0, 18),
		  RIGHT_TAB = b.level(-12, 0).at(24, 0);
	}
	
	public static class SearchBar {
		private static final IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256).offset(112, 0);
		
		public static final Icon // Size 18×18
		  SEARCH_TOOLTIPS = b.twoLevel(true).size(18, 18).at(0, 0),
		  SEARCH_REGEX = b.at(18, 0),
		  SEARCH_CASE_SENSITIVE = b.at(36, 0),
		  SEARCH_FILTER = b.at(54, 0);
	}
	
	public static class Status {
		private static final IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256).offset(66, 0);
		public static final Icon // Size 15×15
		  ERROR = b.level(0, 0).size(15, 15).at(0, 0),
		  WARNING = b.at(15, 0),
		  INFO = b.at(0, 15),
		  CHECKMARK = b.at(15, 15),
		  H_DOTS = b.at(30, 0),
		  V_DOTS = b.at(30, 15);
	}
	
	public static class Widgets {
		private static final IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256).offset(184, 0);
		public static final Icon // Size 18×18
		  CHECKBOX_FLAT = b.size(18, 18).at(0, 0),
		  CHECKBOX = b.at(18, 0);
	}
	
	public static class Presets {
		private static final IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256).offset(198, 76);
		
		public static final Icon // Size 14×14
		  CLIENT_LOCAL = b.size(14, 14).at(0, 0),
		  CLIENT_REMOTE = b.at(14, 0),
		  CLIENT_SAVE = b.at(28, 0),
		  SERVER_LOCAL = b.at(0, 14),
		  SERVER_REMOTE = b.at(14, 14),
		  SERVER_SAVE = b.at(28, 14);
	}
	
	public static class Lists {
		private static final IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256).offset(0, 0);
		
		public static final Icon // Size 9×9
		  ADD = b.size(9, 9).at(0, 0),
		  REMOVE = b.at(9, 0),
		  EXPAND = b.at(30, 0);
		
		public static final Icon // Size 12×9
		  INSERT_ARROW = b.size(12, 9).at(18, 0),
		  DELETE_ARROW = b.at(18, 18);
		
		public static final Icon // Size 7×4
		  UP_ARROW = b.level(9, 0).size(7, 4).at(1, 27),
		  DOWN_ARROW = b.at(1, 32);
	}
	
	public static class Entries {
		private static final IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256).offset(39, 0);
		
		public static final Icon // Size 9×9
		  EXPAND = b.size(9, 9).at(0, 0),
		  TEXT_EXPAND = b.at(9, 0),
		  SLIDER_EDIT = b.at(18, 0);
		
		public static final Icon // Size 14×14
		  HELP = b.offset(170, 76).size(14, 14).at(0, 0),
		  ERROR = b.at(14, 0),
		  REQUIRES_RESTART = b.at(0, 14),
		  NOT_PERSISTENT = b.at(14, 14);
		
		public static final Icon // Size 18×18
		  LESS_EQUAL = b.offset(162, 36).size(18, 18).at(0, 0);
		
		@SuppressWarnings("ConstantConditions")
		public static final Icon // Size 20×20
		  HELP_SEARCH = b.offset(0, 112).level(0, 0).size(20, 20).at(0, 0),
		  HELP_SEARCH_MATCH = HELP_SEARCH.withTint(TextFormatting.YELLOW.getColor() | 0xFF000000),
		  HELP_SEARCH_FOCUSED_MATCH = HELP_SEARCH.withTint(TextFormatting.GOLD.getColor() | 0xFF000000),
		  MERGE = b.at(20, 0),
		  MERGE_ACCEPTED = MERGE.withTint(0xFF80F090),
		  MERGE_CONFLICT = MERGE.withTint(0xFFF0BD80),
		  CLOSE_X = b.at(40, 0);
	}
	
	public static class ComboBox {
		private static final IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256).offset(132, 36);
		
		public static final Icon // Size 20×20
		  UNKNOWN = b.size(20, 20).at(0, 0),
		  ERROR = b.at(0, 20);
		
		public static final Icon // Size
		  DROP_DOWN_ARROW = b.size(10, 10).at(20, 0);
	}
	
	public static class ColorPicker {
		private static final IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256).offset(40, 36);
		
		public static final Icon // Size 40×40
		  CHESS_BOARD = b.size(40, 40).at(0, 0),
		  DIAGONAL_TEXTURE = b.at(40, 0);
		
		public static final Icon // Size 5×7
		  ARROW_RIGHT = b.size(5, 7).at(80, 0),
		  ARROW_LEFT = b.at(85, 0);
		
		public static final Icon // Size 11×11
		  POINTER = b.size(11, 11).at(80, 14);
	}
	
	public static final AnimatedIcon SPINNING_CUBE = new AnimatedIcon(
	  new ResourceLocation(SimpleConfigMod.MOD_ID, "textures/gui/simple_config/cube.png"),
	  0, 0, 20, 20, 160, 20, 1, 8, 40);
}
