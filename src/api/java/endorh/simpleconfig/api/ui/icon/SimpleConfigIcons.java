package endorh.simpleconfig.api.ui.icon;

import endorh.simpleconfig.api.SimpleConfig;
import endorh.simpleconfig.api.SimpleConfig.EditType;
import endorh.simpleconfig.api.SimpleConfig.Type;
import endorh.simpleconfig.api.ui.icon.Icon.IconBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("UnusedAssignment") public class SimpleConfigIcons {
	public static final ResourceLocation TEXTURE = new ResourceLocation(
	  SimpleConfig.MOD_ID, "textures/gui/simpleconfig/config_menu.png");
	
	public static class Types {
		private static IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256)
		  .reverseOffset(true, false).offset(256, 198);
		/** Size 16×16 */
		public static final Icon
		  CLIENT = b.size(16, 16).at(0, 0),
		  SERVER = b.at(16, 0),
		  COMMON = b.at(32, 0),
		  COMMON_CLIENT = b.at(48, 0),
		  COMMON_SERVER = b.at(64, 0);
		
		public static Icon iconFor(Type type) {
			switch (type) {
				case CLIENT: return CLIENT;
				case SERVER: return SERVER;
				case COMMON: return COMMON;
				default: throw new IllegalArgumentException("Unknown type: " + type);
			}
		}
		public static Icon iconFor(EditType type) {
			switch (type) {
				case CLIENT: return CLIENT;
				case SERVER: return SERVER;
				case COMMON: return COMMON_CLIENT;
				case SERVER_COMMON: return COMMON_SERVER;
				default: throw new IllegalArgumentException("Unknown type: " + type);
			}
		}
		static { b = null; }
	}
	
	public static class Buttons {
		private static IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256).offset(0, 136);
		/** Size 20×20 */
		public static final Icon
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
		/** Size 18×18 */
		public static final Icon
		  DOWN = b.size(18, 18).at(220, 0),
		  UP = b.at(238, 0);
		/** Size 18×18 */
		public static final Icon
		  GEAR = b.twoLevel(true).offset(0, 76).size(18, 18).at(0, 0),
		  KEYBOARD = b.at(18, 0),
		  COPY = b.at(36, 0),
		  SELECT_ALL = b.at(54, 0),
		  INVERT_SELECTION = b.at(72, 0);
		/** Size 40×20 */
		public static final Icon
		  CONFIRM_DRAG_LEFT = b.twoLevel(false).offset(0, 36).size(40, 20).at(0, 0),
		  CONFIRM_DRAG_RIGHT = b.at(0, 20);
		/** Size 12×18 */
		public static final Icon
		  LEFT_TAB = b.offset(220, 0).size(12, 18).level(12, 0).at(0, 18),
		  RIGHT_TAB = b.level(-12, 0).at(24, 0);
		static { b = null; }
	}
	
	public static class Actions {
		private static IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256)
		  .offset(256, 104).reverseOffset(true, false);
		/** Size 16×16 */
		public static final Icon
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
		/** Size 18×18 */
		public static final Icon
		  SEARCH_TOOLTIPS = b.size(18, 18).at(0, 0),
		  SEARCH_REGEX = b.at(18, 0),
		  SEARCH_CASE_SENSITIVE = b.at(36, 0),
		  SEARCH_FILTER = b.at(54, 0);
		static { b = null; }
	}
	
	public static class Status {
		private static IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256).offset(66, 0);
		/** Size 15×15 */
		public static final Icon
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
		/** Size 18×18 */
		public static final Icon
		  CHECKBOX_FLAT = b.size(18, 18).at(0, 0),
		  CHECKBOX = b.at(18, 0);
		/** Size 16×16 */
		public static final Icon
		  TREE_ARROW = b.offset(180, 36).size(16, 16).at(0, 0);
		/** Size 8×16 */
		public static final Icon
		  TREE_DRAG_HANDLE = b.size(8, 16).at(16, 0);
		/** Size 20×20 */
		public static final Icon
		  TREE_ADD = b.offset(60, 112).size(20, 20).at(0, 0),
		  TREE_ADD_GROUP = b.at(20, 0),
		  TREE_REMOVE = b.at(40, 0);
		static { b = null; }
	}
	
	public static class Presets {
		private static IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256).offset(200, 214);
		/** Size 14×14 */
		public static final Icon
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
		static { b = null; }
	}
	
	public static class Hotkeys {
		private static IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256).offset(158, 76);
		/** Size 14×14 */
		public static final Icon
		  LOCAL_HOTKEY = b.size(14, 14).at(0, 0),
		  REMOTE_HOTKEY = b.at(0, 14),
		  SAVE_HOTKEY = b.at(14, 0),
		  RESOURCE_HOTKEY = b.at(14, 14);
		
		/** Size 8×9 */
		public static final Icon
		  CONTEXT_GAME = b.offset(160, 216).size(8, 9).at(1, 1),
		  CONTEXT_MENU = b.at(1, 21),
		  CONTEXT_ALL = b.at(21, 1);
		/** Size 5×9 */
		public static final Icon
		  ACTIVATION_PRESS = b.size(5, 9).at(10, 1),
		  ACTIVATION_RELEASE = b.at(10, 21),
		  ACTIVATION_BOTH = b.at(30, 1),
		  ACTIVATION_TOGGLE = b.at(30, 21),
		  ACTIVATION_TOGGLE_RELEASE = b.at(35, 21),
		  ACTIVATION_REPEAT = b.at(20, 21),
		  ACTIVATION_REPEAT_RELEASE = b.at(25, 21);
		/** Size 8×5 */
		public static final Icon
		  ORDER_INSENSITIVE = b.size(8, 5).at(1, 10),
		  ORDER_SENSITIVE = b.at(1, 30);
		/** Size 8×4 */
		public static final Icon
		  EXTRA_KEYS_ALLOW = b.size(8, 4).at(1, 15),
		  EXTRA_KEYS_BLOCK = b.at(1, 35);
		/** Size 4×9 **/
		public static final Icon
		  EXCLUSIVE_NO = b.size(4, 9).at(15, 1),
		  EXCLUSIVE_YES = b.at(15, 21);
		/** Size 5×7 */
		public static final Icon
		  MATCH_BY_CODE = b.size(5, 7).at(9, 11),
		  MATCH_BY_NAME = b.at(9, 31),
		  PREVENT_FURTHER_NO = b.at(14, 11),
		  PREVENT_FURTHER_YES = b.at(14, 31);
		
		static { b = null; }
	}
	
	public static class Lists {
		private static IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256).offset(0, 0);
		/** Size 9×9 */
		public static final Icon
		  ADD = b.size(9, 9).at(0, 0),
		  REMOVE = b.at(9, 0),
		  EXPAND = b.at(30, 0);
		/** Size 12×9 */
		public static final Icon
		  INSERT_ARROW = b.size(12, 9).at(18, 0),
		  DELETE_ARROW = b.at(18, 18);
		/** Size 7×4 */
		public static final Icon
		  UP_ARROW = b.level(9, 0).size(7, 4).at(1, 27),
		  DOWN_ARROW = b.at(1, 32);
		static { b = null; }
	}
	
	public static class Entries {
		private static IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256).offset(39, 0);
		/** Size 9×9 */
		public static final Icon
		  EXPAND = b.size(9, 9).at(0, 0),
		  TEXT_EXPAND = b.at(9, 0),
		  SLIDER_EDIT = b.at(18, 0);
		/** Size 14×14 */
		public static final Icon
		  ERROR = b.offset(256, 76).reverseOffset(true, false).size(14, 14).at(0, 0),
		  WARNING = b.at(14, 0),
		  HELP = b.at(28, 0),
		  NOT_PERSISTENT = b.at(0, 14),
		  REQUIRES_RESTART = b.at(14, 14),
		  EXPERIMENTAL = b.at(28, 14);
		/**
		 * Size 14×14<br>
		 * This icon is white, so you may get a tinted version of it using
		 * {@link Icon#withTint(int)} or {@link Icon#withTint(ChatFormatting)}.
		 */
		public static final Icon
		  COPY = b.at(42, 0),
		  TAG = b.at(56, 0),
		  WRENCH = b.at(42, 14),
		  BOOKMARK = b.at(56, 14);
		/** Size 18×18 */
		public static final Icon
		  LESS_EQUAL = b.offset(162, 36).reverseOffset(false, false).size(18, 18).at(0, 0);
		/** Size 20×20 */
		public static final Icon
		  HELP_SEARCH = b.offset(0, 112).level(0, 0).size(20, 20).at(0, 0),
		  HELP_SEARCH_MATCH = HELP_SEARCH.withTint(ChatFormatting.YELLOW),
		  HELP_SEARCH_FOCUSED_MATCH = HELP_SEARCH.withTint(ChatFormatting.GOLD),
		  MERGE = b.at(20, 0),
		  MERGE_ACCEPTED = MERGE.withTint(0xFF80F090),
		  MERGE_CONFLICT = MERGE.withTint(0xFFF0BD80),
		  CLOSE_X = b.at(40, 0);
		static { b = null; }
	}
	
	public static class ComboBox {
		private static IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256).offset(132, 36);
		/** Size 20×20 */
		public static final Icon
		  UNKNOWN = b.size(20, 20).at(0, 0),
		  ERROR = b.at(0, 20);
		/** Size 10×10 */
		public static final Icon
		  DROP_DOWN_ARROW = b.size(10, 10).at(20, 0);
		static { b = null; }
	}
	
	public static class ColorPicker {
		private static IconBuilder b = IconBuilder.ofTexture(TEXTURE, 256, 256).offset(40, 36);
		/** Size 40×40 */
		public static final Icon
		  CHESS_BOARD = b.size(40, 40).at(0, 0),
		  DIAGONAL_TEXTURE = b.at(40, 0);
		/** Size 5×7 */
		public static final Icon
		  ARROW_RIGHT = b.size(5, 7).at(80, 0),
		  ARROW_LEFT = b.at(85, 0);
		/** Size 11×11 */
		public static final Icon
		  POINTER = b.size(11, 11).at(80, 14);
		static { b = null; }
	}
	
	public static class Backgrounds {
		private static IconBuilder b = IconBuilder.ofTexture(AbstractWidget.WIDGETS_LOCATION, 256, 256);
		
		public static final NinePatchIcon
		  BUTTON_BACKGROUND = b.size(200, 20).patchSize(2, 2, 2, 3).patchAt(0, 46);
		
		static { b = null; }
	}
	
	public static final AnimatedIcon SPINNING_CUBE = AnimatedIcon.ofStripe(
	  new ResourceLocation(SimpleConfig.MOD_ID, "textures/gui/simpleconfig/cube.png"),
	  20, 20, 8, 40);
	public static final AnimatedIcon HOTKEY_RECORDING = AnimatedIcon.ofStripe(
	  new ResourceLocation(SimpleConfig.MOD_ID, "textures/gui/simpleconfig/hotkey_recording.png"),
	  18, 18, 16, 50);
}
