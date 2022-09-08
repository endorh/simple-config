package endorh.simpleconfig.ui.hotkey;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.util.InputMappings;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraft.client.util.InputMappings.Type;
import net.minecraft.util.Util;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Greatly inspired by <a href="https://github.com/maruohon/malilib/blob/2f6d1a7e3c2e51254908a603205fd9cefad3e97c/src/main/java/fi/dy/masa/malilib/input/Keys.java">MaLiLib</a>'s great keybind system.
 */
public class Keys {
	/**
	 * Used at runtime to store keycodes corresponding to keys matched by chars.
	 * Must not be serialized. Next codes are obtained in descending order.
	 */
	public static int FIRST_UNASSIGNED_KEY = -103;
	private static final Pattern SEPARATOR_PATTERN = Pattern.compile("[\\s_-]++");
	private static final Object2IntMap<String> NAMES_TO_IDS = new Object2IntOpenHashMap<>();
	private static final Int2ObjectMap<String> IDS_TO_NAMES = new Int2ObjectOpenHashMap<>();
	private static final Map<String, String> TRANSLATION_OVERRIDES = new HashMap<>();
	private static final IntSet MODIFIER_KEYS = Util.make(new IntOpenHashSet(8), s -> {
		IntStream.of(
		  GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER,
		  GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL,
		  GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT,
		  GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT
		).forEach(s::add);
	});
	
	public static Input getInputFromName(String name) {
		name = SEPARATOR_PATTERN.matcher(name).replaceAll(".").toLowerCase();
		Input input;
		try {
			if (name.startsWith("mouse.") || name.startsWith("keyboard.")) {
				input = InputMappings.getInputByName("key." + name);
			} else if (!name.contains(".")) {
				input = InputMappings.getInputByName("key.keyboard." + name);
			} else try {
				input = InputMappings.getInputByName(name);
			} catch (IllegalArgumentException ignored) {
				input = InputMappings.getInputByName("key.keyboard." + name);
			}
			return input;
		} catch (IllegalArgumentException e) {
			return InputMappings.INPUT_INVALID;
		}
	}
	
	public static Input getInputFromKey(int key) {
		if (key >= -100 && key < -1) {
			return Type.MOUSE.getOrMakeInput(key + 100);
		} else if (key <= -300) {
			return InputMappings.getInputByCode(-1, -300 - key);
		} else if (key >= 0) return InputMappings.getInputByCode(key, -1);
		return InputMappings.INPUT_INVALID;
	}
	
	public static int getKeyFromName(String name) {
		int key = NAMES_TO_IDS.getOrDefault(name, -1);
		if (key != -1) return key;
		Input input = getInputFromName(name);
		switch (input.getType()) {
			case MOUSE:
				return input.getKeyCode() - 100;
			case SCANCODE:
				return -300 - input.getKeyCode();
			default:
				return input.getKeyCode();
		}
	}
	
	public static String getNameForKey(int key) {
		String name = IDS_TO_NAMES.get(key);
		if (name != null) return name;
		Input input = getInputFromKey(key);
		String keyName = input.getTranslationKey();
		if (keyName.startsWith("key.keyboard.")) {
			keyName = keyName.substring("key.keyboard.".length());
		} else if (keyName.startsWith("key.mouse.")) {
			keyName = keyName.substring("key.".length());
		}
		return keyName;
	}
	
	public static int getKeyFromInput(int keyCode, int scanCode) {
		Input input = InputMappings.getInputByCode(keyCode, scanCode);
		return input.getType() == Type.SCANCODE? -300 - input.getKeyCode() : input.getKeyCode();
	}
	
	public static int getKeyFromMouseInput(int button) {
		return button - 100;
	}
	
	public static int getKeyFromScroll(double scrollDelta) {
		return scrollDelta < 0? -102 : -101;
	}
	
	public static boolean isMouseKey(int key) {
		return key >= -100 && key < -1;
	}
	
	public static boolean isScrollKey(int key) {
		return key == -101 || key == -102;
	}
	
	public static boolean isKeyCode(int key) {
		return key >= 0;
	}
	
	public static boolean isScanCode(int key) {
		return key <= -300;
	}
	
	public static boolean isVirtualKey(int key) {
		return key <= FIRST_UNASSIGNED_KEY && key >= -300;
	}
	
	public static boolean isModifier(int key) {
		return MODIFIER_KEYS.contains(key);
	}
	
	public static IFormattableTextComponent getDisplayNameForKey(int key) {
		if (key == -1) return new TranslationTextComponent("key.abbrev.unknown");
		String translationKey;
		Input input = null;
		if (key == -102) {
			translationKey = "key.mouse.scroll.down";
		} else if (key == -101) {
			translationKey = "key.mouse.scroll.up";
		} else {
			input = getInputFromKey(key);
			translationKey = input.getTranslationKey();
		}
		if (TRANSLATION_OVERRIDES.containsKey(translationKey))
			return new TranslationTextComponent(TRANSLATION_OVERRIDES.get(translationKey));
		return input != null
		       ? input.func_237520_d_().deepCopy()
		       : new TranslationTextComponent(translationKey);
	}
	
	public static @Nullable String getCharFromKey(int key) {
		Input input = getInputFromKey(key);
		if (input.getType() == Type.MOUSE || input.getKeyCode() == -1) return null;
		if (input.getType() == Type.KEYSYM)
			return GLFW.glfwGetKeyName(input.getKeyCode(), -1);
		return GLFW.glfwGetKeyName(-1, input.getKeyCode());
	}
	
	public static int getKeyFromChar(String ch) {
		Optional<Input> opt = getInputMap(Type.KEYSYM).values().stream()
		  .filter(i -> i.getKeyCode() != -1 && ch.equals(GLFW.glfwGetKeyName(i.getKeyCode(), -1)))
		  .findFirst();
		if (opt.isPresent()) return opt.get().getKeyCode();
		opt = getInputMap(Type.SCANCODE).values().stream()
		  .filter(i -> ch.equals(GLFW.glfwGetKeyName(-1, i.getKeyCode())))
		  .findFirst();
		return opt.map(input -> -300 - input.getKeyCode()).orElse(-1);
	}
	
	protected static Int2ObjectMap<Input> getInputMap(Type type) {
		return ObfuscationReflectionHelper.getPrivateValue(
		  Type.class, type, "field_197951_e");
	}
	
	static {
		addAlias(-97, "mouse.back");
		addAlias(-96, "mouse.forwards");
		addAlias(-102, "mouse.scroll.down");
		addAlias(-101, "mouse.scroll.up");
		
		overrideTranslation("key.keyboard.left.control", "key.abbrev.left.control");
		overrideTranslation("key.keyboard.right.control", "key.abbrev.right.control");
		overrideTranslation("key.keyboard.left.alt", "key.abbrev.left.alt");
		overrideTranslation("key.keyboard.right.alt", "key.abbrev.right.alt");
		overrideTranslation("key.keyboard.left.shift", "key.abbrev.left.shift");
		overrideTranslation("key.keyboard.right.shift", "key.abbrev.right.shift");
		overrideTranslation("key.keyboard.left.win", "key.abbrev.left.win");
		overrideTranslation("key.keyboard.right.win", "key.abbrev.right.win");
		overrideTranslation("key.mouse.left", "key.abbrev.mouse.left");
		overrideTranslation("key.mouse.right", "key.abbrev.mouse.right");
		overrideTranslation("key.mouse.middle", "key.abbrev.mouse.middle");
		overrideTranslation("key.mouse.4", "key.abbrev.mouse.4");
		overrideTranslation("key.mouse.5", "key.abbrev.mouse.5");
		overrideTranslation("key.mouse.scroll.down", "key.abbrev.mouse.scroll.down");
		overrideTranslation("key.mouse.scroll.up", "key.abbrev.mouse.scroll.up");
		overrideTranslation("key.mouse.scroll.left", "key.abbrev.mouse.scroll.left");
		overrideTranslation("key.mouse.scroll.right", "key.abbrev.mouse.scroll.right");
	}
	
	private static void overrideTranslation(String key, String override) {
		TRANSLATION_OVERRIDES.put(key, override);
	}
	
	private static void addAlias(int keyCode, String name, String... aliases) {
		IDS_TO_NAMES.put(keyCode, name);
		NAMES_TO_IDS.put(name, keyCode);
	}
}
