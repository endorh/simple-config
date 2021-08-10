package endorh.simple_config.clothconfig2.api;

import endorh.simple_config.clothconfig2.impl.ModifierKeyCodeImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

@OnlyIn(value = Dist.CLIENT)
public interface ModifierKeyCode {
	static ModifierKeyCode of(InputMappings.Input keyCode, Modifier modifier) {
		return new ModifierKeyCodeImpl().setKeyCodeAndModifier(keyCode, modifier);
	}
	
	static ModifierKeyCode copyOf(ModifierKeyCode code) {
		return ModifierKeyCode.of(code.getKeyCode(), code.getModifier());
	}
	
	static ModifierKeyCode unknown() {
		return ModifierKeyCode.of(InputMappings.INPUT_INVALID, Modifier.none());
	}
	
	InputMappings.Input getKeyCode();
	
	ModifierKeyCode setKeyCode(InputMappings.Input var1);
	
	default InputMappings.Type getType() {
		return this.getKeyCode().getType();
	}
	
	Modifier getModifier();
	
	ModifierKeyCode setModifier(Modifier var1);
	
	default ModifierKeyCode copy() {
		return ModifierKeyCode.copyOf(this);
	}
	
	default boolean matchesMouse(int button) {
		return !this.isUnknown() && this.getType() == InputMappings.Type.MOUSE &&
		       this.getKeyCode().getKeyCode() == button && this.getModifier().matchesCurrent();
	}
	
	default boolean matchesKey(int keyCode, int scanCode) {
		if (this.isUnknown()) {
			return false;
		}
		if (keyCode == InputMappings.INPUT_INVALID.getKeyCode()) {
			return this.getType() == InputMappings.Type.SCANCODE &&
			       this.getKeyCode().getKeyCode() == scanCode && this.getModifier().matchesCurrent();
		}
		return this.getType() == InputMappings.Type.KEYSYM &&
		       this.getKeyCode().getKeyCode() == keyCode && this.getModifier().matchesCurrent();
	}
	
	default boolean matchesCurrentMouse() {
		if (!this.isUnknown() && this.getType() == InputMappings.Type.MOUSE &&
		    this.getModifier().matchesCurrent()) {
			return GLFW.glfwGetMouseButton(
           Minecraft.getInstance().getMainWindow().getHandle(),
           this.getKeyCode().getKeyCode()) == 1;
		}
		return false;
	}
	
	default boolean matchesCurrentKey() {
		return !this.isUnknown() && this.getType() == InputMappings.Type.KEYSYM &&
		       this.getModifier().matchesCurrent() &&
		       InputMappings.isKeyDown(
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
	
	ITextComponent getLocalizedName();
	
	default boolean isUnknown() {
		return this.getKeyCode().equals(InputMappings.INPUT_INVALID);
	}
}

