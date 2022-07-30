package endorh.simpleconfig.ui.api;

import endorh.simpleconfig.ui.impl.ModifierKeyCodeImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.InputMappings;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@OnlyIn(value = Dist.CLIENT)
public interface ModifierKeyCode {
	Pattern KEY_PATTERN = Pattern.compile(
	  "(?i)\\s*(?:(\\w+)\\s*\\+\\s*)?(?:(\\w+)\\s*\\+\\s*)?(?:(\\w+)\\s*\\+\\s*)?(.*)");
	
	static ModifierKeyCode of(InputMappings.Input keyCode, Modifier modifier) {
		return new ModifierKeyCodeImpl().setKeyCodeAndModifier(keyCode, modifier);
	}
	
	static ModifierKeyCode copyOf(ModifierKeyCode code) {
		return ModifierKeyCode.of(code.getKeyCode(), code.getModifier());
	}
	
	static ModifierKeyCode unknown() {
		return ModifierKeyCode.of(InputMappings.INPUT_INVALID, Modifier.none());
	}
	
	static @NotNull ModifierKeyCode parse(@Nullable String key) {
		if (key == null)
			return unknown();
		final Matcher m = KEY_PATTERN.matcher(key.toLowerCase());
		if (m.matches()) {
			byte mods = 0;
			mods |= guessModifier(m.group(1));
			mods |= guessModifier(m.group(2));
			mods |= guessModifier(m.group(3));
			final Modifier mod = Modifier.of((mods & 2) != 0, (mods & 1) != 0, (mods & 4) != 0);
			try {
				final String name = m.group(4);
				Input in;
				if (name.startsWith("mouse.") || name.startsWith("keyboard.")) {
					in = InputMappings.getInputByName("key." + name);
				} else if (!name.contains(".")) {
					in = InputMappings.getInputByName("key.keyboard." + name);
				} else try {
					in = InputMappings.getInputByName(name);
				} catch (IllegalArgumentException ignored) {
					in = InputMappings.getInputByName("key.keyboard." + name);
				}
				return of(in, mod);
			} catch (IllegalArgumentException ignored) {
				return unknown();
			}
		} else return unknown();
	}
	
	static byte guessModifier(@Nullable String key) {
		if (key == null || key.isEmpty())
			return 0;
		switch (key.toLowerCase().charAt(0)) {
			case 'c': return 1;
			case 'a': return 2;
			case 'm': // Spanish and the like
			case 's': return 4;
			default: return 0;
		}
	}
	
	default String serializedName() {
		StringBuilder mod = new StringBuilder();
		if (getModifier().hasControl())
			mod.append("ctrl+");
		if (getModifier().hasAlt())
			mod.append("alt+");
		if (getModifier().hasShift())
			mod.append("shift+");
		return mod.append(getPrettySerializedKeyName()).toString();
	}
	
	default String getSerializedKeyName() {
		return getKeyCode().getTranslationKey();
	}
	
	default String getPrettySerializedKeyName() {
		String keyName = getSerializedKeyName();
		if (keyName.startsWith("key.keyboard."))
			keyName = keyName.substring("key.keyboard.".length());
		else if (keyName.startsWith("key.mouse."))
			keyName = keyName.substring("key.".length());
		return keyName;
	}
	
	InputMappings.Input getKeyCode();
	
	ModifierKeyCode setKeyCode(InputMappings.Input var1);
	
	default InputMappings.Type getType() {
		return this.getKeyCode().getType();
	}
	
	Modifier getModifier();
	
	ModifierKeyCode setModifier(Modifier var1);
	
	ModifierKeyCode addModifier(Modifier mod);
	
	default ModifierKeyCode copy() {
		return ModifierKeyCode.copyOf(this);
	}
	
	default boolean matchesMouse(int button) {
		return !this.isUnknown() && this.getType() == InputMappings.Type.MOUSE &&
		       this.getKeyCode().getKeyCode() == button && this.getModifier().matchesCurrent();
	}
	
	default boolean matchesKey(int keyCode, int scanCode) {
		if (this.isUnknown()) return false;
		if (keyCode == InputMappings.INPUT_INVALID.getKeyCode()) {
			return this.getType() == InputMappings.Type.SCANCODE &&
			       this.getKeyCode().getKeyCode() == scanCode && this.getModifier().matchesCurrent();
		}
		return this.getType() == InputMappings.Type.KEYSYM &&
		       this.getKeyCode().getKeyCode() == keyCode && this.getModifier().matchesCurrent();
	}
	
	default boolean matchesCurrentMouse() {
		return !this.isUnknown()
		       && this.getType() == InputMappings.Type.MOUSE
		       && this.getModifier().matchesCurrent()
		       && GLFW.glfwGetMouseButton(
					Minecraft.getInstance().getMainWindow().getHandle(),
					this.getKeyCode().getKeyCode()) == 1;
	}
	
	default boolean matchesCurrentKey() {
		return !this.isUnknown()
		       && this.getType() == InputMappings.Type.KEYSYM
		       && this.getModifier().matchesCurrent()
		       && InputMappings.isKeyDown(
               Minecraft.getInstance().getMainWindow().getHandle(),
               this.getKeyCode().getKeyCode());
	}
	
	default ModifierKeyCode setKeyCodeAndModifier(
     InputMappings.Input keyCode, Modifier modifier
   ) {
		this.setKeyCode(keyCode);
		this.setModifier(modifier);
		return this;
	}
	
	default ModifierKeyCode clearModifier() {
		return this.setModifier(Modifier.none());
	}
	
	String toString();
	
	default ITextComponent getLayoutAgnosticLocalizedName() {
		return getLayoutAgnosticLocalizedName(Style.EMPTY, Style.EMPTY);
	}
	
	default ITextComponent getLayoutAgnosticLocalizedName(TextFormatting modifiers, TextFormatting key) {
		return getLayoutAgnosticLocalizedName(Style.EMPTY.applyFormatting(modifiers), Style.EMPTY.applyFormatting(key));
	}
	
	default ITextComponent getLocalizedName() {
		return getLocalizedName(Style.EMPTY, Style.EMPTY);
	}
	
	default ITextComponent getLocalizedName(TextFormatting modifiers, TextFormatting key) {
		return getLocalizedName(Style.EMPTY.applyFormatting(modifiers), Style.EMPTY.applyFormatting(key));
	}
	
	ITextComponent getLayoutAgnosticLocalizedName(Style modifiers, Style key);
	ITextComponent getLocalizedName(Style modifiers, Style key);
	
	default boolean isUnknown() {
		return this.getKeyCode().equals(InputMappings.INPUT_INVALID);
	}
}

