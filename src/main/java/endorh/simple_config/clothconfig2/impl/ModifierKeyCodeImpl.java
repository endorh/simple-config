package endorh.simple_config.clothconfig2.impl;

import endorh.simple_config.clothconfig2.api.Modifier;
import endorh.simple_config.clothconfig2.api.ModifierKeyCode;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(value = Dist.CLIENT)
public class ModifierKeyCodeImpl
  implements ModifierKeyCode {
	private InputMappings.Input keyCode;
	private Modifier modifier;
	
	@Override
	public InputMappings.Input getKeyCode() {
		return this.keyCode;
	}
	
	@Override
	public Modifier getModifier() {
		return this.modifier;
	}
	
	@Override
	public ModifierKeyCode setKeyCode(InputMappings.Input keyCode) {
		this.keyCode = keyCode.getType().getOrMakeInput(keyCode.getKeyCode());
		if (keyCode.equals(InputMappings.INPUT_INVALID)) {
			this.setModifier(Modifier.none());
		}
		return this;
	}
	
	@Override
	public ModifierKeyCode setModifier(Modifier modifier) {
		this.modifier = Modifier.of(modifier.getValue());
		return this;
	}
	
	@Override
	public String toString() {
		return this.getLocalizedName().getString();
	}
	
	@Override
	public ITextComponent getLocalizedName() {
		ITextComponent base = this.keyCode.func_237520_d_();
		if (this.modifier.hasShift()) {
			base = new TranslationTextComponent("modifier.cloth-config.shift", base);
		}
		if (this.modifier.hasControl()) {
			base = new TranslationTextComponent("modifier.cloth-config.ctrl", base);
		}
		if (this.modifier.hasAlt()) {
			base = new TranslationTextComponent("modifier.cloth-config.alt", base);
		}
		return base;
	}
	
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ModifierKeyCode)) {
			return false;
		}
		ModifierKeyCode that = (ModifierKeyCode) o;
		return this.keyCode.equals(that.getKeyCode()) &&
             this.modifier.equals(that.getModifier());
	}
	
	public int hashCode() {
		int result = this.keyCode != null ? this.keyCode.hashCode() : 0;
		result = 31 * result + (this.modifier != null ? this.modifier.hashCode() : 0);
		return result;
	}
}

