package endorh.simpleconfig.ui.gui;

import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.ui.gui.Icon.IconBuilder;
import endorh.simpleconfig.ui.gui.widget.PresetPickerWidget.Preset.Location;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.config.ModConfig;

@SuppressWarnings("UnusedAssignment") public class SimpleConfigIcons {
	private static final ResourceLocation TEXTURE = new ResourceLocation(
	  SimpleConfigMod.MOD_ID, "textures/gui/simple_config/config_menu.png");
	private static IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256).offset(0, 0);
	
	public static class Types {
		private static IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256)
		  .reverseOffset(true, false).offset(256, 198);
		public static final Icon // Size 16×16
		  CLIENT = b.size(16, 16).at(0, 0),
		  SERVER = b.at(16, 0),
		  COMMON = b.at(32, 0),
		  COMMON_CLIENT = b.at(48, 0),
		  COMMON_SERVER = b.at(64, 0);
		static { b = null; }
	}
	
	public static class Buttons {
		private static IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256).offset(0, 136);
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
		public static final Icon // Size 18×18
		  DOWN = b.size(18, 18).at(220, 0),
		  UP = b.at(238, 0);
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
		static { b = null; }
	}
	
	public static class Actions {
		private static IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256)
		  .offset(256, 104).reverseOffset(true, false);
		public static final Icon // Size 16×16
		  NONE = b.size(16, 16).at(0, 0),
		  ASSIGN = b.at(0, 16),
		  MULTIPLY = b.at(16, 0),
		  DIVIDE = b.at(16, 16),
		  ADD = b.at(32, 0),
		  SUBTRACT = b.at(32, 16),
		  ADD_CYCLE = b.at(48, 0),
		  SUBTRACT_CYCLE = b.at(48, 16),
		  CYCLE = b.at(64, 0),
		  CYCLE_REVERSE = b.at(64, 16);
		static { b = null; }
	}
	
	public static class SearchBar {
		private static IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256).offset(112, 0);
		public static final Icon // Size 18×18
		  SEARCH_TOOLTIPS = b.size(18, 18).at(0, 0),
		  SEARCH_REGEX = b.at(18, 0),
		  SEARCH_CASE_SENSITIVE = b.at(36, 0),
		  SEARCH_FILTER = b.at(54, 0);
		static { b = null; }
	}
	
	public static class Status {
		private static IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256).offset(66, 0);
		public static final Icon // Size 15×15
		  ERROR = b.level(0, 0).size(15, 15).at(0, 0),
		  WARNING = b.at(15, 0),
		  INFO = b.at(0, 15),
		  CHECKMARK = b.at(15, 15),
		  H_DOTS = b.at(30, 0),
		  V_DOTS = b.at(30, 15);
		static { b = null; }
	}
	
	public static class Widgets {
		private static IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256).offset(184, 0);
		public static final Icon // Size 18×18
		  CHECKBOX_FLAT = b.size(18, 18).at(0, 0),
		  CHECKBOX = b.at(18, 0);
		public static final Icon // Size 16×16
		  TREE_ARROW = b.offset(180, 36).size(16, 16).at(0, 0);
		public static final Icon // Size 8×16
		  TREE_DRAG_HANDLE = b.size(8, 16).at(16, 0);
		public static final Icon // Size 20×20
		  TREE_ADD = b.offset(60, 112).size(20, 20).at(0, 0),
		  TREE_ADD_GROUP = b.at(20, 0),
		  TREE_REMOVE = b.at(40, 0);
		static { b = null; }
	}
	
	public static class Presets {
		private static IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256).offset(200, 214);
		public static final Icon // Size 14×14
		  CLIENT_LOCAL = b.size(14, 14).at(0, 0),
		  CLIENT_REMOTE = b.at(14, 0),
		  CLIENT_SAVE = b.at(28, 0),
		  CLIENT_RESOURCE = b.at(42, 0),
		  COMMON_LOCAL = b.at(0, 14),
		  COMMON_REMOTE = b.at(14, 14),
		  COMMON_SAVE = b.at(28, 14),
		  COMMON_RESOURCE = b.at(42, 14),
		  SERVER_LOCAL = b.at(0, 28),
		  SERVER_REMOTE = b.at(14, 28),
		  SERVER_SAVE = b.at(28, 28),
		  SERVER_RESOURCE = b.at(42, 28);
		public static Icon saveIconFor(ModConfig.Type type) {
			switch (type) {
				case CLIENT: return CLIENT_SAVE;
				case COMMON: return COMMON_SAVE;
				case SERVER: return SERVER_SAVE;
				default: return null;
			}
		}
		public static Icon iconFor(ModConfig.Type type, Location location) {
			switch (type) {
				case CLIENT: switch (location) {
					case LOCAL: return CLIENT_LOCAL;
					case REMOTE: return CLIENT_REMOTE;
					case RESOURCE: return CLIENT_RESOURCE;
					default: return null;
				}
				case COMMON: switch (location) {
					case LOCAL: return COMMON_LOCAL;
					case REMOTE: return COMMON_REMOTE;
					case RESOURCE: return COMMON_RESOURCE;
					default: return null;
				}
				case SERVER: switch (location) {
					case LOCAL: return SERVER_LOCAL;
					case REMOTE: return SERVER_REMOTE;
					case RESOURCE: return SERVER_RESOURCE;
					default: return null;
				}
				default: return null;
			}
		}
		static { b = null; }
	}
	
	public static class Hotkeys {
		private static IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256).offset(200, 76);
		public static final Icon // Size 14×14
		  LOCAL_HOTKEY = b.size(14, 14).at(0, 0),
		  REMOTE_HOTKEY = b.at(0, 14),
		  SAVE_HOTKEY = b.at(14, 0),
		  RESOURCE_HOTKEY = b.at(14, 14);
		static { b = null; }
	}
	
	public static class Lists {
		private static IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256).offset(0, 0);
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
		static { b = null; }
	}
	
	public static class Entries {
		private static IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256).offset(39, 0);
		public static final Icon // Size 9×9
		  EXPAND = b.size(9, 9).at(0, 0),
		  TEXT_EXPAND = b.at(9, 0),
		  SLIDER_EDIT = b.at(18, 0);
		public static final Icon // Size 14×14
		  ERROR = b.offset(256, 76).reverseOffset(true, false).size(14, 14).at(0, 0),
		  HELP = b.at(14, 0),
		  NOT_PERSISTENT = b.at(0, 14),
		  REQUIRES_RESTART = b.at(14, 14);
		public static final Icon // Size 18×18
		  LESS_EQUAL = b.offset(162, 36).reverseOffset(false, false).size(18, 18).at(0, 0);
		@SuppressWarnings("ConstantConditions")
		public static final Icon // Size 20×20
		  HELP_SEARCH = b.offset(0, 112).level(0, 0).size(20, 20).at(0, 0),
		  HELP_SEARCH_MATCH = HELP_SEARCH.withTint(TextFormatting.YELLOW.getColor() | 0xFF000000),
		  HELP_SEARCH_FOCUSED_MATCH = HELP_SEARCH.withTint(TextFormatting.GOLD.getColor() | 0xFF000000),
		  MERGE = b.at(20, 0),
		  MERGE_ACCEPTED = MERGE.withTint(0xFF80F090),
		  MERGE_CONFLICT = MERGE.withTint(0xFFF0BD80),
		  CLOSE_X = b.at(40, 0);
		static { b = null; }
	}
	
	public static class ComboBox {
		private static IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256).offset(132, 36);
		public static final Icon // Size 20×20
		  UNKNOWN = b.size(20, 20).at(0, 0),
		  ERROR = b.at(0, 20);
		public static final Icon // Size
		  DROP_DOWN_ARROW = b.size(10, 10).at(20, 0);
		static { b = null; }
	}
	
	public static class ColorPicker {
		private static IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256).offset(40, 36);
		public static final Icon // Size 40×40
		  CHESS_BOARD = b.size(40, 40).at(0, 0),
		  DIAGONAL_TEXTURE = b.at(40, 0);
		public static final Icon // Size 5×7
		  ARROW_RIGHT = b.size(5, 7).at(80, 0),
		  ARROW_LEFT = b.at(85, 0);
		public static final Icon // Size 11×11
		  POINTER = b.size(11, 11).at(80, 14);
		static { b = null; }
	}
	
	public static final AnimatedIcon SPINNING_CUBE = AnimatedIcon.ofStripe(
	  new ResourceLocation(SimpleConfigMod.MOD_ID, "textures/gui/simple_config/cube.png"),
	  20, 20, 8, 40);
	public static final AnimatedIcon HOTKEY_RECORDING = AnimatedIcon.ofStripe(
	  new ResourceLocation(SimpleConfigMod.MOD_ID, "textures/gui/simple_config/hotkey_recording.png"),
	  18, 18, 16, 50);
	static { b = null; }
}
